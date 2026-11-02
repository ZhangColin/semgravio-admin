package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.query.AiplatformContractQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractTransitionResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractSummaryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractTransitionWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import com.cartisan.web.response.PageResponse;
import org.springframework.stereotype.Service;

/**
 * aiplatform 合同域 BFF 应用服务（issue #104，spec #102 A 批 A1——provider aiplatform
 * #336 合同域六态承接）：清单四维检索 / 详情全量面 / 创建重建三端点。
 *
 * <p>admin 作为 BFF：调接口 + DTO 转换，不持业务逻辑、不持合同数据、不记业务审计
 * （append-only 动作留痕归 provider）。下游错误已由 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException}（provider 信封原样透传），本层不再 try/catch。六态
 * 人工推进状态机、「需更新」派生标志、冻结集取现值等语义全部由 provider 承担，本层不解释
 * 不预判。操作者身份经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传
 * （AppService 不经手），provider 落痕动作时间线。</p>
 *
 * <p>分页（spec #62 平台分页统一决议）：北向 1-based 直传零换算；回显取 provider 回报的
 * page/size 原值（含 clamp 后值），BFF 不重复夹取。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformContractAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformContractAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 合同清单（四维检索，透传 aiplatform）——状态多选/创建时间区间/订单号精确/合同号精确，
     * 新合同在前（provider 定死 TSID 倒序）。
     */
    public PageResponse<AiplatformContractSummaryResponse> list(AiplatformContractQuery query, int page, int size) {
        PageResponse<AiplatformContractSummaryWireResponse> wirePage = aiplatformClient.listContracts(
                new AiplatformContractListWireRequest(query.status(), query.createdFrom(), query.createdTo(),
                        query.orderId(), query.contractNo()),
                page, size);
        List<AiplatformContractSummaryResponse> items = wirePage.items().stream()
                .map(AiplatformContractAppService::toSummary)
                .toList();
        // 回显 provider 的 page/size 原值（1-based，含 clamp 后值）——不是北向入参回声
        return new PageResponse<>(items, wirePage.total(), wirePage.page(), wirePage.size());
    }

    /**
     * 合同详情（透传 aiplatform）——后台承接全量面：全字段（含「需更新」派生标志与范围
     * 快照冻结面）＋PRD 确认版全文冗余拷贝＋动作留痕时间线（新→旧，append-only 全量）。
     */
    public AiplatformContractDetailResponse getDetail(String id) {
        return toDetail(aiplatformClient.getContract(id));
    }

    /**
     * 为订单创建/重建合同（透传 aiplatform）——撤回/作废后的重建口（新号新合同、旧合同
     * 终态保留），与用户「确认范围并准备合同」共用同一创建正本：冻结集取现值、起步准备中。
     * 前置门（报价先在/范围已录/活跃唯一/已签关门）全在 provider 裁决。
     */
    public AiplatformContractResponse createForOrder(String orderId) {
        return toContract(aiplatformClient.createContractForOrder(orderId));
    }

    private static AiplatformContractSummaryResponse toSummary(AiplatformContractSummaryWireResponse wire) {
        return new AiplatformContractSummaryResponse(
                wire.id(), wire.contractNo(), wire.orderId(),
                wire.status(), wire.statusName(),
                wire.version(), wire.amount(), wire.currency(), wire.createdAt());
    }

    /** wire 三段 → 北向三段：contract 全字段逐位直通＋PRD 全文＋留痕行逐位直通。 */
    private static AiplatformContractDetailResponse toDetail(AiplatformContractDetailWireResponse wire) {
        return new AiplatformContractDetailResponse(
                toContract(wire.contract()),
                wire.prdSnapshot(),
                wire.transitions() == null ? null : wire.transitions().stream()
                        .map(AiplatformContractAppService::toTransition)
                        .toList());
    }

    private static AiplatformContractResponse toContract(AiplatformContractWireResponse wire) {
        return new AiplatformContractResponse(
                wire.id(), wire.contractNo(), wire.orderId(),
                wire.status(), wire.statusName(),
                wire.version(), wire.needsUpdate(),
                wire.partyA(), wire.partyB(), wire.projectScope(), wire.paymentPlan(),
                wire.scopeDeliverables(), wire.scopeAcceptance(), wire.scopeTimeline(),
                wire.amount(), wire.currency(),
                wire.prdVersionNo(), wire.demoVersionNo(),
                wire.partyASigned(), wire.partyASignedAt(),
                wire.partyBSigned(), wire.partyBSignedAt(),
                wire.createdAt());
    }

    private static AiplatformContractTransitionResponse toTransition(AiplatformContractTransitionWireResponse wire) {
        return new AiplatformContractTransitionResponse(
                wire.action(), wire.actionName(),
                wire.fromStatus(), wire.fromStatusName(),
                wire.toStatus(), wire.toStatusName(),
                wire.basis(), wire.operatorId(), wire.operatorName(), wire.createdAt());
    }
}
