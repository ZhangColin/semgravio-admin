package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRelabelCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformSedimentDraftQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformSedimentDraftDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformSedimentDraftListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformSedimentDraftRejectWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformSedimentDraftRelabelWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformSedimentDraftSummaryWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import com.cartisan.web.response.PageResponse;
import org.springframework.stereotype.Service;

/**
 * aiplatform 后台「数据标注」域 BFF 应用服务——沉淀草稿标注队列＋三键（issue #124，
 * 镜像 aiplatform #464：数据飞轮采集层沉淀草稿的人审审核口，承载数据标注面五端点）。
 * 沉淀草稿归 aiplatform base.knowledge 上下文——admin 不持业务逻辑、不持草稿数据。
 *
 * <p>admin 作为 BFF 忠实透传（零加戏）：队列三过滤维度逐字镜像（{@code projectId}
 * <strong>宽松</strong>——畸形值 provider {@code Tsid.parseOrNull} 归 null 即缺省不过滤，
 * 与 {@code kind} 非法即 400 KNW_011 <strong>有意不同</strong>；{@code status} Integer
 * code 透传，未知 code 由 provider 绑定层骑框架 400 带合法取值表）；{@code id} 为
 * TSID 十进制串透传不解析不预校验，畸形/未寻址由 provider 判 404 KNW_008（数字业务码
 * 2008）。三键终态语义照技能草稿审核先例（ADR-0022）：CAS 条件更新、已审结拒 409
 * KNW_009（2009）、操作者必留痕（缺头 400 KNW_006＝2006）。分页 1-based 直传零换算
 * （clamp 归 provider），回显 provider 回报原值。</p>
 *
 * <p>语义陷阱两则（#124 票面钉死，前端文案不得越界）：① <strong>「已激活」≠「已入库」</strong>
 * ——激活只翻审结终态，升格 knw_materials 归激活链；素材检索在升格落地前不命中已激活
 * 沉淀；② 三键一律要求草稿<strong>在途</strong>——已审结（已激活/已拒绝）重按一律
 * 409 KNW_009，按钮态应由行 status 驱动（status=1 才可按）。改标签占目标类别在途
 * 名额（每项目每 kind 上限 3），目标类别满 409 KNW_012（2012）。操作者经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（AppService 不
 * 经手），错误经 {@link AiplatformClient} 统一翻译为 {@link AiplatformUpstreamException}
 * 原样透传，本层不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformSedimentDraftAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformSedimentDraftAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 标注队列（透传 aiplatform）——「数据标注」人审队列分诊面：采集层自荐的沉淀草稿，
     * 三过滤维度可组合、均可缺省（缺省＝全量<strong>含终态行</strong>——终态留档可查；
     * 待办视角须显式 status=1）。排序服务端定死＝自荐时间倒序（id 倒序稳定）；
     * {@code projectId} 宽松（畸形＝provider 缺省不过滤）、{@code kind} 非法 400 KNW_011
     * （2011）、未知 status code 骑 provider 框架 400 带合法取值表——均 provider 裁决
     * 原样透传，admin 不预校验。分页 1-based 直传（缺省 1/20 上界 100，clamp 归
     * provider），回显 provider 回报值。
     */
    public PageResponse<AiplatformSedimentDraftSummaryResponse> list(
            AiplatformSedimentDraftQuery query, int page, int size) {
        PageResponse<AiplatformSedimentDraftSummaryWireResponse> wirePage =
                aiplatformClient.listSedimentDrafts(
                        new AiplatformSedimentDraftListWireRequest(
                                query.projectId(), query.kind(), query.status()),
                        page, size);
        List<AiplatformSedimentDraftSummaryResponse> items = wirePage.items().stream()
                .map(AiplatformSedimentDraftAppService::toSummary)
                .toList();
        // 回显 provider 的 page/size 原值（1-based，含 clamp 后值）——不是北向入参回声
        return new PageResponse<>(items, wirePage.total(), wirePage.page(), wirePage.size());
    }

    /**
     * 标注详情（透传 aiplatform）——队列行同形字段＋正文全文（激活原样升格不改稿——
     * 审核面所见即入库内容）＋载荷（untyped，一期恒空面）＋扫描 findings 逐条留档
     * （untyped 透传不造 typed record）＋终态留痕四字段（在途恒 null）。<strong>任意
     * 状态可查</strong>（终态留档）；{@code id} 取清单行原值原样出站（TSID 十进制串
     * 透传不解析不预校验），畸形/未寻址由 provider 判 404 KNW_008（数字业务码 2008）。
     */
    public AiplatformSedimentDraftDetailResponse getDetail(String id) {
        return toDetail(aiplatformClient.getSedimentDraft(id));
    }

    /**
     * 激活草稿（透传 aiplatform，「数据标注」三键之一）——人审采纳：在途翻已激活＋
     * 操作者留痕（CAS——并发审结零行命中 409 KNW_009）。激活即出标注队列、释放在途
     * 名额；升格 knw_materials 归激活链（「已激活」≠「已入库」——素材检索在升格落地
     * 前不命中已激活沉淀）。<strong>无命令体</strong>；操作者经框架透传头落痕（缺头
     * 由 provider 域面守卫 KNW_006 拦，admin 不拦）。回执＝终态详情（留痕形状直读）。
     */
    public AiplatformSedimentDraftDetailResponse activate(String id) {
        return toDetail(aiplatformClient.activateSedimentDraft(id));
    }

    /**
     * 拒绝草稿（透传 aiplatform，「数据标注」三键之一）——人审不采纳：理由必填
     * （空白 400 KNW_010——终态留档拒绝须有据，库级 CHECK 双锁）＋在途翻已拒绝＋
     * 操作者留痕（CAS 同激活）。拒绝即终态：不再列标注队列、详情仍可查（留档）；
     * 不改稿不复活——重提只能靠未来会话产生新草稿。{@code reason} 保持 provider
     * 形态——单字段命令体原样出站（admin 不在绑定层前置拦截），空白由 provider
     * 手工校验出 400 KNW_010（数字业务码 2010）原样透传。回执＝终态详情。
     */
    public AiplatformSedimentDraftDetailResponse reject(
            String id, AiplatformSedimentDraftRejectCommand command) {
        return toDetail(aiplatformClient.rejectSedimentDraft(id,
                new AiplatformSedimentDraftRejectWireRequest(command == null ? null : command.reason())));
    }

    /**
     * 改标签（透传 aiplatform，「数据标注」三键之一）——在途草稿调整类别（纯队列分诊
     * 修正：不翻终态、不动正文）。{@code kind} 一期合法取值 CASE/MODULE/RULE（非法
     * 400 KNW_011；同类别幂等 no-op）；<strong>占目标类别在途名额</strong>——跨类别
     * 占目标类别在途序号最低空位（每项目每 kind 上限 3 的结构性约束跨类别生效），
     * 目标类别满 409 KNW_012（数字业务码 2012）。{@code kind} 保持 provider 形态原样
     * 出站（admin 不预校验），回执＝详情（新类别直读）。
     */
    public AiplatformSedimentDraftDetailResponse relabel(
            String id, AiplatformSedimentDraftRelabelCommand command) {
        return toDetail(aiplatformClient.relabelSedimentDraft(id,
                new AiplatformSedimentDraftRelabelWireRequest(command == null ? null : command.kind())));
    }

    private static AiplatformSedimentDraftSummaryResponse toSummary(
            AiplatformSedimentDraftSummaryWireResponse wire) {
        return new AiplatformSedimentDraftSummaryResponse(
                wire.id(), wire.kind(), wire.title(), wire.summary(),
                wire.slot(), wire.projectId(), wire.runId(), wire.scanVerdict(),
                wire.inflightSlot(), wire.status(), wire.statusName(), wire.createdAt());
    }

    private static AiplatformSedimentDraftDetailResponse toDetail(
            AiplatformSedimentDraftDetailWireResponse wire) {
        return new AiplatformSedimentDraftDetailResponse(
                wire.id(), wire.kind(), wire.title(), wire.summary(),
                wire.slot(), wire.projectId(), wire.runId(), wire.scanVerdict(),
                wire.inflightSlot(), wire.status(), wire.statusName(), wire.createdAt(),
                wire.content(), wire.payload(), wire.scanFindings(),
                wire.operatorId(), wire.operatorName(), wire.reviewedAt(), wire.rejectReason());
    }
}
