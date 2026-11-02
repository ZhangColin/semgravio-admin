package com.semgravio.admin.aiplatform.endpoints.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformContractAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractRecordCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractSignCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformContractQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractSummaryResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台合同控制器——BFF 合同域读面＋创建重建＋六态推进写口（issue #104 A1 三端点＋
 * issue #105 A2 六写口，spec #102 A 批，镜像 provider aiplatform #336 合同域六态承接的
 * {@code BackofficeContractController} 全九端点）。
 *
 * <p>北向 {@code /api/admin/aiplatform/contracts/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/contracts/**}——查询参数名、缺省值、分页基准（page 1-based，
 * 缺省 1/20）与 provider controller 同形；六态语义、「需更新」派生标志、冻结集取现值、
 * 活跃唯一前置门、六态推进守卫（推进边/双方均签/重录窗口/签署位幂等）全由 provider 裁决，
 * BFF 零推导零校验零状态机复算。错误经 {@code AiplatformUpstreamErrorAdvice} 原样透传
 * （如 404 ORD_020→5020、409 ORD_024→5024、400 ORD_022→5022）。</p>
 *
 * <p>权限码（spec #102 A 批）：读 {@code contract:read} 一码（清单/详情两端点）＋写
 * {@code contract:create/advance/fail/withdraw/void/sign/record} 七码——读写分权、每写
 * 操作一码惯例（最小授权：角色可按动作粒度授受）。操作者身份由框架 {@code OpenApiClient}
 * 自动透传 {@code X-User-Id/X-User-Name} 出站头（provider 合同动作以操作者为审计正身、
 * 缺头 ORD_035 拒——北向无操作者字段，前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/contracts")
@Validated
@Tag(name = "Admin Aiplatform Contract", description = "AI 平台 · 合同（六态人工承接）")
public class AiplatformContractController {

    private final AiplatformContractAppService contractAppService;

    public AiplatformContractController(AiplatformContractAppService contractAppService) {
        this.contractAppService = contractAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #102）；错误码以 description 文档化（ORD_020 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:read",
            name = "AI 平台 / 合同查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "合同清单（四维检索，分页 1-based）——透传 aiplatform",
            description = "运营工作清单：新合同在前（TSID 倒序，provider 定死）。四维可组合、均可缺省"
                    + "（缺省＝全量）：① status 状态多选，Integer code 逗号分隔单值（如 status=2,4；"
                    + "<strong>六态</strong>：1=准备中 2=待核对与签署 3=签署进行中 4=签署完成 "
                    + "5=签署失败或过期 6=已撤回或作废——人工推进单向 1→2→3→4，末边须双方均签；"
                    + "5/6 旁落终态）；② createdFrom/createdTo 创建时间区间（ISO-8601，含两端，"
                    + "如 2026-10-01T00:00:00）；③ orderId 所属订单精确（TSID 十进制，非数值→"
                    + "空清单 200）；④ contractNo 合同号精确（SG- 串）。行带编号/状态/版本/冻结金额"
                    + "（version 草稿同号重录 +1、重建从 1 起）。page 1 基（缺省 1）、size 缺省 20"
                    + "（provider 上界 100），clamp 由 provider 执行、回显 provider 回报值；排序服务端"
                    + "定死不开放。绑定裁决两段：非整数 code/非法时间在本服务绑定层 400（框架信封）。")
    public ApiResponse<PageResponse<AiplatformContractSummaryResponse>> contracts(
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String contractNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(contractAppService.list(
                new AiplatformContractQuery(status, createdFrom, createdTo, orderId, contractNo), page, size));
    }

    @GetMapping("/{id}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:read",
            name = "AI 平台 / 合同查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "合同详情（承接全量面：六态全字段＋PRD 全文冗余拷贝＋动作时间线）——透传 aiplatform",
            description = "推进决定前的核对全量，三段：① contract 全字段——六态、<strong>「需更新」"
                    + "派生标志</strong>（草稿态且冻结集与订单现值不一致——改价/改范围后待后台重录"
                    + "重新冻结并清零；非草稿态恒 false）、双方主体/项目范围/付款计划四个后台录入"
                    + "预览字段（未录入 null）、<strong>范围快照冻结面</strong>三段（交付清单/验收"
                    + "方式/工期口径——创建/重录时取订单范围文本工作副本定格，此后不漂移）、冻结金额"
                    + "（分）、PRD/Demo 双版本引用、双方签署位＋落签时点（签署标记不自动迁移状态，"
                    + "「签署完成」由 advance 显式推进）；② prdSnapshot——PRD 确认版全文冗余拷贝"
                    + "（合同自含，报价/录内容的依据面）；③ transitions——动作留痕时间线（新 → 旧，"
                    + "append-only 全量：动作〔1=推进 2=撤回 3=作废 4=签署标记 5=草稿重录 "
                    + "6=失败或过期；非迁移动作 from=to〕/两侧状态/依据/操作者两肢/时间——合同动作"
                    + "操作者必填）。合同不存在（含畸形 id）404 ORD_020（数字业务码 5020）。")
    public ApiResponse<AiplatformContractDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.ok(contractAppService.getDetail(id));
    }

    // 不挂 @ErrorCodes 同上：ORD_001/021/024/025/026 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/orders/{orderId}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:create",
            name = "AI 平台 / 合同创建重建",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "创建/重建合同（确认范围冻结集取现值）——透传 aiplatform",
            description = "<strong>撤回/作废后的重建口</strong>（新号新合同、旧合同终态保留——终态"
                    + " 4/5/6 永不物理删除，库侧部分唯一索引同谓词放行重建）——与用户「确认范围并"
                    + "准备合同」共用同一创建正本：冻结集取现值（最新价目行＋范围文本工作副本＋"
                    + "PRD/Demo 双版本引用冗余），起步准备中（version 1、四个后台录入预览字段 null、"
                    + "双方签署位未落）。<strong>无请求体</strong>（冻结集由 provider 取现值落）。"
                    + "前置门全在 provider（BFF 不复算活跃判定）：订单不存在（含畸形 id）404 ORD_001"
                    + "（数字业务码 5001）；尚无报价 409 <strong>ORD_025</strong>（5025）；范围文本"
                    + "未录入 409 <strong>ORD_026</strong>（5026——scope 录入端点归 A5）；已有活跃"
                    + "合同（预检或并发撞部分唯一索引同译）409 <strong>ORD_024</strong>（5024）；"
                    + "最近合同已签署完成/订单非已报价态 409 ORD_021（5021——已签走变更沟通不碰"
                    + "原合同）。X-User-Id/X-User-Name 透传头自动落痕（provider 合同动作操作者必填）。")
    public ApiResponse<AiplatformContractResponse> createForOrder(@PathVariable String orderId) {
        return ApiResponse.ok(contractAppService.createForOrder(orderId));
    }

    // 不挂 @ErrorCodes 同上：ORD_020/021/022/023/027/035 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/{id}/advance")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:advance",
            name = "AI 平台 / 合同推进",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "推进合同（线性单步，依据必填留痕）——透传 aiplatform",
            description = "六态人工推进正路：准备中 → 待核对与签署 → 签署进行中 → 签署完成"
                    + "（<strong>末边 3→4 须双方签署位均已落签</strong>——sign 先行）。每次推进"
                    + " append-only 留痕；推进到「签署完成」即触发 provider 侧开工判定"
                    + "（付款腿未满足则订单留等待窗口——BFF 不感知不复算）。命令体逐字镜像"
                    + " {basis}（依据必填，至多 1000 字）。回执＝ContractResponse（动作后合同"
                    + "现值；advance 不动版本——版本递增归 record）。合同不存在（含畸形 id）"
                    + " 404 ORD_020（数字业务码 5020）；无推进边（终态）409 ORD_021（5021）；"
                    + "双方未均签 409 <strong>ORD_027</strong>（5027）；依据缺失 400 ORD_022"
                    + "（5022）；依据超长 400 ORD_023（5023）。X-User-Id/X-User-Name 透传头"
                    + "自动落痕（provider 合同动作操作者必填、缺头 ORD_035 拒）。")
    public ApiResponse<AiplatformContractResponse> advance(
            @PathVariable String id, @RequestBody AiplatformContractActionCommand command) {
        return ApiResponse.ok(contractAppService.advance(id, command));
    }

    @PostMapping("/{id}/fail")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:fail",
            name = "AI 平台 / 合同失败或过期",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "合同旁落「签署失败或过期」终态（依据必填留痕）——透传 aiplatform",
            description = "失败/过期的如实终局：自<strong>待核对与签署/签署进行中</strong>可达"
                    + "（用户放弃核对或超期未签）；其余态 409 ORD_021（数字业务码 5021）。"
                    + "命令体逐字镜像 {basis}（依据必填，至多 1000 字）。回执＝ContractResponse"
                    + "（终态现值——已落签署位如实定格保留）。合同不存在 404 ORD_020（5020）；"
                    + "依据缺失/超长 400 ORD_022/023（5022/5023）。X-User-Id/X-User-Name 透传头"
                    + "自动落痕。")
    public ApiResponse<AiplatformContractResponse> fail(
            @PathVariable String id, @RequestBody AiplatformContractActionCommand command) {
        return ApiResponse.ok(contractAppService.fail(id, command));
    }

    @PostMapping("/{id}/withdraw")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:withdraw",
            name = "AI 平台 / 合同撤回",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "撤回合同（旁落终态，依据必填留痕）——透传 aiplatform",
            description = "自<strong>任何非终态</strong>可达——此后换新号重建（POST "
                    + "contracts/orders/{orderId}，旧合同终态保留）。终态再动 409 ORD_021"
                    + "（数字业务码 5021）。命令体逐字镜像 {basis}（依据必填，至多 1000 字）。"
                    + "回执＝ContractResponse（终态现值）。合同不存在 404 ORD_020（5020）；"
                    + "依据缺失/超长 400 ORD_022/023（5022/5023）。X-User-Id/X-User-Name 透传头"
                    + "自动留痕。")
    public ApiResponse<AiplatformContractResponse> withdraw(
            @PathVariable String id, @RequestBody AiplatformContractActionCommand command) {
        return ApiResponse.ok(contractAppService.withdraw(id, command));
    }

    @PostMapping("/{id}/void")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:void",
            name = "AI 平台 / 合同作废",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "作废合同（旁落终态，依据必填留痕）——透传 aiplatform",
            description = "守卫与去向<strong>同撤回</strong>（同一终态「已撤回或作废」、自任何"
                    + "非终态可达），动作语义分立——区别只在留痕行 action（2=撤回 3=作废），"
                    + "终态处置可审计。命令体逐字镜像 {basis}（依据必填，至多 1000 字）。回执＝"
                    + "ContractResponse（终态现值，与 withdraw 共形）。错误族同撤回：合同不存在"
                    + " 404 ORD_020（5020）；终态再动 409 ORD_021（5021）；依据缺失/超长 400 "
                    + "ORD_022/023（5022/5023）。X-User-Id/X-User-Name 透传头自动留痕。")
    public ApiResponse<AiplatformContractResponse> voidContract(
            @PathVariable String id, @RequestBody AiplatformContractActionCommand command) {
        return ApiResponse.ok(contractAppService.voidContract(id, command));
    }

    @PostMapping("/{id}/sign")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:sign",
            name = "AI 平台 / 合同签署标记",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "合同签署标记（落一方签署位，依据必填留痕）——透传 aiplatform",
            description = "电子签渠道真接前的后台承接口径：仅<strong>签署进行中</strong>态可落签，"
                    + "落一方签署位＋时点；<strong>不自动迁移状态</strong>——「签署完成」由 advance "
                    + "显式推进（双方均签守卫 ORD_027）。已落签的签署位幂等拒 409 "
                    + "<strong>ORD_028</strong>（5028）。命令体逐字镜像 {party, basis}：party "
                    + "Integer code（1=甲方〔客户方〕 2=乙方〔服务方〕——非法值 provider 侧 400 "
                    + "框架信封）；basis 依据必填（至多 1000 字）。回执＝ContractResponse（签署位"
                    + "落定后的合同现值）。合同不存在 404 ORD_020（5020）；非签署进行中 409 "
                    + "ORD_021（5021）；依据缺失/超长 400 ORD_022/023（5022/5023）。"
                    + "X-User-Id/X-User-Name 透传头自动留痕。")
    public ApiResponse<AiplatformContractResponse> sign(
            @PathVariable String id, @RequestBody AiplatformContractSignCommand command) {
        return ApiResponse.ok(contractAppService.sign(id, command));
    }

    @PostMapping("/{id}/record")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:contract:record",
            name = "AI 平台 / 合同草稿重录",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "合同草稿重录（同号重新冻结＋版本递增，依据必填留痕）——透传 aiplatform",
            description = "<strong>「需更新」处置正路</strong>：改价/改范围后对草稿（准备中/"
                    + "待核对与签署）<strong>同号重录</strong>——预览字段集整组录入（四个后台录入"
                    + "字段可空）＋冻结集取订单现值重新定格＋合同版本 +1＋「需更新」清零。签署"
                    + "进行中起 409 <strong>ORD_031</strong>（5031——变动走撤回/作废重建）；内容"
                    + "超长 400 <strong>ORD_032</strong>（5032——主体至多 200 字、其余至多 2000 字）；"
                    + "重冻前置门（尚无报价 ORD_025/范围未录 ORD_026）。命令体逐字镜像 "
                    + "{partyA, partyB, projectScope, paymentPlan, basis}（依据必填，至多 1000 字）。"
                    + "回执＝ContractResponse（重录后现值——version +1、needsUpdate false）。合同"
                    + "不存在 404 ORD_020（5020）；依据缺失/超长 400 ORD_022/023（5022/5023）。"
                    + "X-User-Id/X-User-Name 透传头自动留痕。")
    public ApiResponse<AiplatformContractResponse> record(
            @PathVariable String id, @RequestBody AiplatformContractRecordCommand command) {
        return ApiResponse.ok(contractAppService.record(id, command));
    }
}
