package com.semgravio.admin.aiplatform.endpoints.controller;

import com.semgravio.admin.aiplatform.application.AiplatformSedimentDraftAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRelabelCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformSedimentDraftQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftSummaryResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台「数据标注」控制器——沉淀草稿标注队列＋激活/拒绝/改标签三键（issue #124，镜像
 * aiplatform #464 后台「数据标注」面——数据飞轮采集层沉淀草稿的人审审核口，照 admin#122
 * 镜像先例）。
 *
 * <p>北向 {@code /api/admin/aiplatform/sediment-drafts/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/sediment-drafts/**}（机机签名出站）——查询参数名、缺省值、分页
 * 基准（page 1-based，缺省 1/20）与 provider controller 同形。人审是知识入库的主质量闸
 * （未审核草稿不参与任何检索）；三键终态语义照技能草稿审核先例（ADR-0022）：CAS、
 * 已审结拒、拒绝理由必填、操作者必留痕。错误经 {@code AiplatformUpstreamErrorAdvice}
 * 原样透传（KNW 域码 2×1000＋序号：KNW_006→2006、KNW_008→2008、KNW_009→2009、
 * KNW_010→2010、KNW_011→2011、KNW_012→2012）。</p>
 *
 * <p>权限码四枚（照 skill-draft 三码先例——连字符子域、每写操作一码）：读
 * {@code sediment-draft:read} 一码挂队列/详情两读端点（读写不串）；写码
 * {@code sediment-draft:activate|reject|relabel} 各自独立成码逐端点挂载（最小授权——
 * 激活权/拒绝权/分诊权独立授受，如只给分诊员配改标签而不给终态两键）。写口操作者经
 * 框架 {@code OpenApiClient} 自动透传 {@code X-User-Id/X-User-Name} 出站头，provider
 * 落痕——标注动作必留痕，缺头由 provider 域面守卫 400 KNW_006（数字业务码 2006）拦截，
 * admin 不拦。三键一律要求草稿<strong>在途</strong>（已审结重按一律 409 KNW_009）——
 * 前端按钮态应由行 status 驱动（status=1 才可按）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/sediment-drafts")
@Validated
@Tag(name = "Admin Aiplatform Sediment Draft", description = "AI 平台 · 数据标注")
public class AiplatformSedimentDraftController {

    private final AiplatformSedimentDraftAppService sedimentDraftAppService;

    public AiplatformSedimentDraftController(AiplatformSedimentDraftAppService sedimentDraftAppService) {
        this.sedimentDraftAppService = sedimentDraftAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——同六域先例）；错误码以 description 文档化（KNW_011 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:sediment-draft:read",
            name = "AI 平台 / 数据标注查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "标注队列（按项目/类别/状态过滤，分页）——透传 aiplatform",
            description = "「数据标注」人审队列（#464）：采集层自荐的沉淀草稿，三过滤"
                    + "维度可组合、均可缺省（缺省＝全量<strong>含终态行</strong>——终态"
                    + "留档可查；待办视角须显式 status=1）：① projectId 来源项目 id 精确"
                    + "（<strong>宽松口径</strong>：畸形值＝provider 缺省不过滤返回全量，"
                    + "与 kind 非法即 400 有意不同）；② kind 类别单选（CASE/MODULE/RULE，"
                    + "大小写不敏感——非法 400 KNW_011＝数字业务码 2011 带合法取值表）；"
                    + "③ status 状态单选 Integer code（1=在途 2=已激活 3=已拒绝；非整数"
                    + "在本服务绑定层 404 框架类型不匹配口径；合法整数但未知 code 由 "
                    + "provider 绑定层骑框架 400 带合法取值表）。排序服务端定死＝自荐时间"
                    + "倒序（id 倒序稳定）。行字段：类别/标题/摘要＋血统三件（来源项目/"
                    + "run/槽位）＋扫描判定（SAFE/CAUTION——DANGEROUS 写入口已拒收不落库）"
                    + "＋在途序号 inflightSlot 1-3（每项目每 kind 在途上限 3 的结构性承载"
                    + "——队列拥挤度可视化锚）＋状态＋自荐时刻；正文与扫描 findings 在详情。"
                    + "page 1 基（缺省 1）、size 缺省 20（provider 上界 100），clamp 由 "
                    + "provider 执行、回显 provider 回报值。")
    public ApiResponse<PageResponse<AiplatformSedimentDraftSummaryResponse>> queue(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(sedimentDraftAppService.list(
                new AiplatformSedimentDraftQuery(projectId, kind, status), page, size));
    }

    // 不挂 @ErrorCodes（同上）：KNW_008 以 description 文档化。
    @GetMapping("/{id}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:sediment-draft:read",
            name = "AI 平台 / 数据标注查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "标注详情（正文＋载荷＋扫描回执＋终态留痕，审核面）——透传 aiplatform",
            description = "草稿全文按数据标注口径判读（#464）：队列行同形字段＋正文全文"
                    + "（激活<strong>原样升格不改稿</strong>——审核面所见即入库内容）＋"
                    + "载荷 payload（一期恒空面；kind 开放生长面，untyped 逐字透传不做"
                    + "形状断言）＋扫描 findings 逐条留档（patternId/severity/category/"
                    + "file/line/matchText/description，空列表＝无发现——untyped 透传）＋"
                    + "终态留痕字段（操作者/时刻/拒绝理由——已激活/已拒绝回填，在途恒 "
                    + "null）。<strong>任意状态可查</strong>（终态留档）。id 取清单行原值"
                    + "（TSID 十进制串）；草稿不存在（含未寻址/畸形 TSID）404 KNW_008"
                    + "（数字业务码 2008）。语义陷阱：「已激活」≠「已入库」——激活只翻"
                    + "审结终态，升格 knw_materials 随激活链，素材检索在升格落地前不命中"
                    + "已激活沉淀（标注面文案不得暗示「激活即可被检索」）。")
    public ApiResponse<AiplatformSedimentDraftDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.ok(sedimentDraftAppService.getDetail(id));
    }

    // 不挂 @ErrorCodes（同上）：KNW_006/008/009 以 description 文档化。
    @PostMapping("/{id}/activate")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:sediment-draft:activate",
            name = "AI 平台 / 数据标注激活",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "激活草稿（人审采纳，原样升格不改稿）——透传 aiplatform",
            description = "「数据标注」三键之一（#464）：在途翻已激活＋操作者留痕"
                    + "（CAS——并发审结零行命中 409 KNW_009＝数字业务码 2009）。激活即"
                    + "出标注队列、释放在途名额，并升格 knw_materials（kind 随草稿、正文"
                    + "段落级分块入库、与来源项目脱钩成孤儿沉淀）。<strong>语义陷阱</strong>："
                    + "「已激活」≠「已入库」——升格落地前素材检索不命中已激活沉淀，标注面"
                    + "文案不得暗示「激活即可被检索」。草稿须在途（已审结 409 KNW_009——"
                    + "终态不可再审，重提为新草稿；前端按钮态由行 status 驱动，status=1 "
                    + "才可按）；草稿不存在 404 KNW_008（2008）；缺操作者 400 KNW_006"
                    + "（数字业务码 2006——X-User-Id/X-User-Name 透传头，标注动作必留痕，"
                    + "admin 不拦）。<strong>无请求体</strong>。回执＝终态详情（留痕形状"
                    + "直读，与 GET 详情同形）。")
    public ApiResponse<AiplatformSedimentDraftDetailResponse> activate(@PathVariable String id) {
        return ApiResponse.ok(sedimentDraftAppService.activate(id));
    }

    // 不挂 @ErrorCodes（同上）：KNW_006/008/009/010 以 description 文档化。
    @PostMapping("/{id}/reject")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:sediment-draft:reject",
            name = "AI 平台 / 数据标注拒绝",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "拒绝草稿（终态留档，理由必填）——透传 aiplatform",
            description = "「数据标注」三键之一（#464）：理由必填（空白 400 KNW_010＝"
                    + "数字业务码 2010——终态留档拒绝须有据，库级 CHECK 双锁，由 provider "
                    + "裁决、BFF 可选绑定不前置拦截）＋在途翻已拒绝＋操作者留痕（CAS 同"
                    + "激活）。拒绝即终态：不再列标注队列、详情仍可查（留档）；不改稿不"
                    + "复活——重提只能靠未来会话产生新草稿。草稿须在途（已审结 409 "
                    + "KNW_009＝2009）；草稿不存在 404 KNW_008（2008）；缺操作者 400 "
                    + "KNW_006（2006，口径同激活）。body 仅 reason 单字段。回执＝终态"
                    + "详情（留痕形状直读，与 GET 详情同形）。")
    public ApiResponse<AiplatformSedimentDraftDetailResponse> reject(
            @PathVariable String id,
            @RequestBody(required = false) AiplatformSedimentDraftRejectCommand command) {
        return ApiResponse.ok(sedimentDraftAppService.reject(id, command));
    }

    // 不挂 @ErrorCodes（同上）：KNW_006/008/009/011/012 以 description 文档化。
    @PostMapping("/{id}/relabel")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:sediment-draft:relabel",
            name = "AI 平台 / 数据标注改标签",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "改标签（在途内调整类别，占目标类别在途名额）——透传 aiplatform",
            description = "「数据标注」三键之一（#464）：在途草稿调整类别（一期合法取值 "
                    + "CASE/MODULE/RULE——非法 400 KNW_011＝数字业务码 2011 带合法取值表；"
                    + "同类别幂等 no-op）；<strong>占目标类别在途名额</strong>（跨类别占"
                    + "目标类别在途序号最低空位——每项目每 kind 上限 3 的结构性约束跨类别"
                    + "生效），目标类别满 409 KNW_012＝数字业务码 2012（前端提示「目标类别"
                    + "在途已满，先审结该类别草稿」）。不翻终态、不动正文——纯队列分诊修正。"
                    + "草稿须在途（已审结 409 KNW_009＝2009）；草稿不存在 404 KNW_008"
                    + "（2008）；缺操作者 400 KNW_006（2006，标注动作必留痕）。body 仅 "
                    + "kind 单字段。回执＝详情（新类别直读）。")
    public ApiResponse<AiplatformSedimentDraftDetailResponse> relabel(
            @PathVariable String id,
            @RequestBody(required = false) AiplatformSedimentDraftRelabelCommand command) {
        return ApiResponse.ok(sedimentDraftAppService.relabel(id, command));
    }
}
