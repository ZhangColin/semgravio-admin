package com.semgravio.admin.aiplatform.endpoints.controller;

import com.semgravio.admin.aiplatform.application.AiplatformPaymentAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentNodesCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentVerifyCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentVoucherContentResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台付款核验控制器——BFF 付款核验域五端点（issue #106 A3，spec #102 A 批，镜像
 * provider aiplatform #337 凭证核验承接的 {@code BackofficePaymentController}）。
 *
 * <p><strong>命名注意</strong>：本控制器是 aiplatform 付款核验域（合同冻结后的付款计划
 * 人工承接——财务运营闭环），与既有支付服务 BFF 上下文（{@code com.semgravio.admin.payment}
 * 的 {@code PaymentController}，调 semgravio-payment 支付服务）<strong>无关联</strong>
 * ——两处 payment 词同形不同域，勿混读（spec #102 Further Notes）。</p>
 *
 * <p>北向路径逐字镜像 aiplatform {@code /api/backoffice/**}（前缀换 {@code /api/admin/
 * aiplatform}）：计划详情/节点录入挂 {@code orders/{orderId}/payment-plan**}、核验/打回挂
 * {@code orders/{orderId}/payment-nodes/{nodeId}/*}、凭证内容挂 {@code payment-vouchers/
 * {voucherId}/content}——付款域无统一顶层路径（照 provider 路由原形），admin-web 从订单
 * 详情进入（无独立工作台页）。节点五态、全额判定、打回守卫、重录窗口全由 provider 裁决，
 * BFF 零推导零校验零状态机复算。错误经 {@code AiplatformUpstreamErrorAdvice} 原样透传
 * （如 404 ORD_036→5036、409 ORD_044→5044、400 ORD_042→5042）。</p>
 *
 * <p>权限码（spec #102 A 批，V22 已一次记账零迁移）：读 {@code payment:read} 一码（计划
 * 详情＋凭证直出共用）＋写 {@code payment:update-plan/verify/reject} 三码——读写分权、
 * 每写操作一码惯例（最小授权：角色可按动作粒度授受）。核验/打回操作者身份由框架
 * {@code OpenApiClient} 自动透传 {@code X-User-Id/X-User-Name} 出站头（provider 核验
 * 留痕以操作者为审计正身、缺头 ORD_043 拒——北向无操作者字段，前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform")
@Validated
@Tag(name = "Admin Aiplatform Payment", description = "AI 平台 · 付款核验（凭证核验承接）")
public class AiplatformPaymentController {

    private final AiplatformPaymentAppService paymentAppService;

    public AiplatformPaymentController(AiplatformPaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #102）；错误码以 description 文档化（ORD_036 等）。
    @GetMapping("/orders/{orderId}/payment-plan")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:payment:read",
            name = "AI 平台 / 付款核验查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "付款计划详情（核验承接全量面：节点五态/凭证/留痕时间线/历史行）——透传 aiplatform",
            description = "订单名下<strong>活跃计划</strong>（最新行——撤回重建/范围重冻后即新行）全量："
                    + "① plan 计划与节点清单——节点<strong>五态</strong>（1=待支付 2=支付处理中 "
                    + "3=支付失败或关闭 4=线下转账待核验 5=已到账；v1 只走凭证核验路径 1→4→5，"
                    + "2/3 渠道真接后启用）/金额（分，绝对额）＋占比（展示换算现算不落库）/"
                    + "附言码（<strong>&lt;合同号&gt;-K&lt;序&gt;</strong>，转账回执对账关联位）/"
                    + "<strong>开工门标志</strong>（开工判定取本标志为 true 的节点集合）/渠道接口位"
                    + "（类型 1=线下转账 2=扫码〔占位〕＋<strong>渠道流水号</strong>可空——核验录入时"
                    + "落值）/累计已核到账＋还需支付推导（remainingAmount＝amount−settled，不足留痕"
                    + "后仍累积）＋凭证清单（上传序，<strong>追加保留不覆盖</strong>——上传仅代表已"
                    + "提交，核验结论在留痕行）；② trails 核验留痕时间线（新 → 旧，跨节点全量，"
                    + "append-only——到账核验〔action=1：实到金额/实到时间/关联凭证/依据〕与打回"
                    + "〔action=2：依据必填、实到两肢 null〕各一行，操作者两肢随行落）；③ history "
                    + "历史计划行摘要（新 → 旧，不含活跃行——旧计划随旧合同留痕，撤回重建/范围重冻后"
                    + "旧行定格可见）。订单不存在（含畸形 id）404 ORD_001（数字业务码 5001）；该订单"
                    + "名下无付款计划（尚无合同即无计划——前端缺席＝不渲染面板）404 <strong>ORD_036"
                    + "</strong>（5036）。")
    public ApiResponse<AiplatformPaymentPlanDetailResponse> planDetail(@PathVariable String orderId) {
        return ApiResponse.ok(paymentAppService.getPlanDetail(orderId));
    }

    // 不挂 @ErrorCodes 同上：ORD_001/036/039/044 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/orders/{orderId}/payment-plan/nodes")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:payment:update-plan",
            name = "AI 平台 / 付款节点录入",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "付款节点录入（整组替换，含开工门设定）——透传 aiplatform",
            description = "「付款节点录入/开工门设定」后台承接口：<strong>整组替换</strong>——不收"
                    + "半组，每次全量提交。每条＝名称/金额（分，绝对额〔非百分比〕）/<strong>开工门"
                    + "标志</strong>（布尔、录入时定；开工款≠默认全款——#338 开工判定按本标志取节点"
                    + "集合）。序号按列表序服务端派生（<strong>录入序即展示序</strong>）、附言码自"
                    + "合同号定格（<strong>&lt;合同号&gt;-K&lt;序&gt;</strong>）、渠道起步线下转账"
                    + "（v1 唯一走通路径，扫码占位）。<strong>支付活动未开始</strong>（全部待支付"
                    + "且无凭证无留痕）是<strong>重录唯一窗口</strong>——已开始变动走合同侧撤回重建。"
                    + "命令体逐字镜像 {nodes:[{name, amount, kickoffGate}]}。回执＝PaymentPlanResponse"
                    + "（录入后计划现值——占比现算随行）。订单不存在 404 ORD_001（5001）；无付款计划"
                    + " 404 ORD_036（5036）；录入无效（<strong>至少一条、名称必填、金额为正</strong>）"
                    + " 400 <strong>ORD_039</strong>（5039）；已开始支付 409 <strong>ORD_044</strong>"
                    + "（5044——聚合腿与凭证腿两腿同译）。")
    public ApiResponse<AiplatformPaymentPlanResponse> enterNodes(
            @PathVariable String orderId, @RequestBody AiplatformPaymentNodesCommand command) {
        return ApiResponse.ok(paymentAppService.enterNodes(orderId, command));
    }

    // 不挂 @ErrorCodes 同上：ORD_036/037/038/041/043/045 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/orders/{orderId}/payment-nodes/{nodeId}/verify")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:payment:verify",
            name = "AI 平台 / 付款到账核验",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "到账核验录入（全额判定，依据留痕）——透传 aiplatform",
            description = "凭证核验路径第二跳（后台人工录入）：实到金额（分，正数）/实到时间"
                    + "（ISO-8601 业务时间）/关联凭证（可空，须属该节点——404 <strong>ORD_045</strong>〔5045〕）/"
                    + "<strong>渠道流水号</strong>（可空落值——渠道接口位）/备注（可空，至多 1000 字）"
                    + "随行 append-only 留痕。<strong>全额判定归 provider</strong>＝累计核到账 ≥ 节点"
                    + "金额：足额自动转「已到账」（receivedAt＝实到时间）；<strong>不足留痕＋回「待"
                    + "支付」</strong>（累计已核到账不清零，前台 remainingAmount 推导还需支付差额）"
                    + "——补传补核累计到全额即转。核验落库后即触发 provider 开工判定（付款腿触发点，"
                    + "BFF 不感知）。仅<strong>线下转账待核验</strong>态可达（409 <strong>ORD_038</strong>"
                    + "〔5038〕）；录入无效（实到金额须为正、实到时间必填）400 <strong>ORD_041</strong>"
                    + "（5041）；操作者必携（X-User-Id/X-User-Name 透传头，400 <strong>ORD_043</strong>"
                    + "〔5043〕——核验动作以"
                    + "操作者为审计正身，缺头拒不留无名痕）。命令体逐字镜像 {actualAmount, arrivedAt, "
                    + "voucherId, channelRefNo, basis}。回执＝PaymentPlanResponse（动作后计划现值）。"
                    + "订单/计划不存在 404 ORD_001/ORD_036；节点不存在或不属活跃计划 404 ORD_037"
                    + "（5037）。")
    public ApiResponse<AiplatformPaymentPlanResponse> verify(
            @PathVariable String orderId, @PathVariable String nodeId,
            @RequestBody AiplatformPaymentVerifyCommand command) {
        return ApiResponse.ok(paymentAppService.verify(orderId, nodeId, command));
    }

    // 不挂 @ErrorCodes 同上：ORD_036/037/038/042/043/045 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/orders/{orderId}/payment-nodes/{nodeId}/reject")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:payment:reject",
            name = "AI 平台 / 付款打回",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "打回（依据必填留痕，回待支付）——透传 aiplatform",
            description = "凭证主张被否（回单不清、对不上账）：回「待支付」＋<strong>依据必填留痕"
                    + "</strong>（400 <strong>ORD_042</strong>〔5042〕——打回依据是审计正身，BFF 不前置"
                    + "拦截）；关联凭证可空（同核验校验，404 <strong>ORD_045</strong>〔5045〕）。"
                    + "<strong>打回不动累计已核到账</strong>（核到账只经核验录入累积）；打回后用户可再传"
                    + "凭证（回待支付＝用户可再动作态）。仅「线下转账待核验」态可达（409 "
                    + "<strong>ORD_038</strong>〔5038〕）；操作者必携（<strong>ORD_043</strong>〔5043〕）。"
                    + "命令体逐字镜像 {voucherId, basis}。回执＝PaymentPlanResponse（回待支付后的计划"
                    + "现值）。订单/计划不存在 404 ORD_001/ORD_036；节点不存在 404 ORD_037（5037）。")
    public ApiResponse<AiplatformPaymentPlanResponse> reject(
            @PathVariable String orderId, @PathVariable String nodeId,
            @RequestBody AiplatformPaymentRejectCommand command) {
        return ApiResponse.ok(paymentAppService.reject(orderId, nodeId, command));
    }

    // 不挂 @ErrorCodes 同上：ORD_045 码以 description 文档化（忠实透传、零加戏）
    @GetMapping("/payment-vouchers/{voucherId}/content")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:payment:read",
            name = "AI 平台 / 付款核验查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "付款凭证内容（后台核验看证，二进制直出，无 ApiResponse 信封）——透传 aiplatform",
            description = "凭证文件字节直出——后台核验前看证的唯一触字节口（元数据行上的"
                    + "fileName/contentType 随行进响应头）。响应为二进制文件流（<strong>不走 "
                    + "ApiResponse JSON 信封</strong>，先例＝源码包/设计资产包端点）：Content-Type"
                    + "（真实媒体类型 application/pdf | image/png | image/jpeg）+ Content-Disposition "
                    + "attachment 文件名均取 <strong>provider 原值透传</strong>（不打解析、不重构"
                    + "文件名）；字节全量缓冲内存中转不落盘。凭证不存在（含畸形 id／伴行字节缺行）"
                    + " 404 <strong>ORD_045</strong>（5045，数字业务码 JSON 信封——错误路径不走二进制）。"
                    + "权限复用 payment:read（计划详情＋凭证直出共用读码）。")
    public ResponseEntity<byte[]> voucherContent(@PathVariable String voucherId) {
        AiplatformPaymentVoucherContentResponse content = paymentAppService.getVoucherContent(voucherId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, content.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, content.contentDisposition())
                .body(content.content());
    }
}
