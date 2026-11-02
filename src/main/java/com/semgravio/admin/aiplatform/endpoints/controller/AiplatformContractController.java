package com.semgravio.admin.aiplatform.endpoints.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformContractAppService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台合同控制器——BFF 合同域读面＋创建重建（issue #104，spec #102 A 批 A1，镜像
 * provider aiplatform #336 合同域六态承接的 {@code BackofficeContractController} 前三端点；
 * advance/fail/withdraw/void/sign/record 六推进写口归 A2，issue #105）。
 *
 * <p>北向 {@code /api/admin/aiplatform/contracts/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/contracts/**}——查询参数名、缺省值、分页基准（page 1-based，
 * 缺省 1/20）与 provider controller 同形；六态语义、「需更新」派生标志、冻结集取现值、
 * 活跃唯一前置门全由 provider 裁决，BFF 零推导零校验零状态机复算。错误经
 * {@code AiplatformUpstreamErrorAdvice} 原样透传（如 404 ORD_020→5020、409 ORD_024→5024）。
 * 六态推进各写动作（advance/fail/withdraw/void/sign/record）不在本票——A2（#105）逐票镜像。</p>
 *
 * <p>权限码（spec #102 A 批）：读 {@code contract:read} 一码（清单/详情两端点）＋写
 * {@code contract:create} 一码（创建重建）——读写分权、每写操作一码惯例（后续推进六写口
 * 各自独立成码，A2 落）。操作者身份由框架 {@code OpenApiClient} 自动透传
 * {@code X-User-Id/X-User-Name} 出站头（provider 合同动作以操作者为审计正身、缺头 ORD_035
 * 拒——北向无操作者字段，前端无法伪造留痕）。</p>
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
}
