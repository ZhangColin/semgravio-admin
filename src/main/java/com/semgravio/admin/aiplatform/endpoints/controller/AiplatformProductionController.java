package com.semgravio.admin.aiplatform.endpoints.controller;

import com.semgravio.admin.aiplatform.application.AiplatformProductionAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionSupplementCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProductionHandoffResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台生产承接控制器——BFF 生产承接域四端点（issue #107 A4，spec #102 A 批，镜像
 * provider aiplatform #338 交付承接的 {@code BackofficeProductionController}）。
 *
 * <p>北向路径逐字镜像 aiplatform {@code /api/backoffice/**}（前缀换 {@code /api/admin/
 * aiplatform}）：四端点同挂 {@code projects/{projectId}/production-handoff} 下（承接
 * <strong>挂项目唯一</strong>，按项目寻址）——admin-web 从项目详情进入（无独立工作台
 * 页，spec #102 Further Notes）。两态推进边、依据必填/超长、整组替换的清空语义、盖帽
 * 置位语义全由 provider 裁决，BFF 零推导零校验零状态机复算。错误经
 * {@code AiplatformUpstreamErrorAdvice} 原样透传（如 404 PRJ_055→4055、409 PRJ_056→
 * 4056、400 PRJ_057/058/059→4057/4058/4059、404 PRJ_001→4001）。</p>
 *
 * <p>项目唯一约束（如实文档化）：承接挂项目<strong>只建一次</strong>（全生命周期一行，
 * 无终态概念——承接是交付过程的状态承接页；区别于合同域的部分唯一索引——那里终态
 * 放行重建有语义、这里没有）、<strong>创建无人工口</strong>（开工判定系统触发建档，
 * 创建留痕行操作者＝system/开工判定）、无删除/重建面——admin 四端点只读面＋信息维护
 * ＋推进＋盖帽。</p>
 *
 * <p>权限码（spec #102 A 批，V22 已一次记账零迁移）：读 {@code production:read} 一码
 * ＋写 {@code production:update/advance/supplement} 三码——读写分权、每写操作一码惯例
 * （最小授权：角色可按动作粒度授受）。推进/更新操作者身份由框架 {@code OpenApiClient}
 * 自动透传 {@code X-User-Id/X-User-Name} 出站头（provider 承接动作以操作者为审计正身、
 * 缺头 PRJ_059 拒——北向无操作者字段，前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform")
@Validated
@Tag(name = "Admin Aiplatform Production", description = "AI 平台 · 生产承接（交付承接两态与进展维护）")
public class AiplatformProductionController {

    private final AiplatformProductionAppService productionAppService;

    public AiplatformProductionController(AiplatformProductionAppService productionAppService) {
        this.productionAppService = productionAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #102）；错误码以 description 文档化（PRJ_055 等）。
    @GetMapping("/projects/{projectId}/production-handoff")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:production:read",
            name = "AI 平台 / 生产承接查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "生产承接详情（承接全量面：两态/留痕时间线）——透传 aiplatform",
            description = "按项目寻址（承接<strong>挂项目唯一</strong>）：承接字段（交付联系人/"
                    + "下一步说明/进展更新/预计开始时间——可空段如实 null，呈现层「仅真实值显示」）"
                    + "＋<strong>两态</strong>（1=待安排生产 2=生产中——单向推进无终态概念，"
                    + "承接是交付过程的状态承接页）＋动作留痕时间线（新 → 旧，append-only 全量"
                    + "——动作/两侧状态/依据/操作者/时间；<strong>创建行＝开工判定系统操作者"
                    + "〔operatorId=system〕——承接创建无人工口</strong>，开工判定〔合同签署完成"
                    + "且开工门节点全部到账〕触发建档；非迁移类〔创建/信息更新〕留痕行 from=to"
                    + "在场快照）。承接<strong>挂项目只建一次</strong>（全生命周期一行）。无承接"
                    + "记录 404 <strong>PRJ_055</strong>（数字业务码 4055——开工判定未触发，"
                    + "前端缺席＝不渲染面板）；项目不存在（含畸形 id）404 PRJ_001（4001）。")
    public ApiResponse<AiplatformProductionHandoffResponse> detail(@PathVariable String projectId) {
        return ApiResponse.ok(productionAppService.getHandoff(projectId));
    }

    // 不挂 @ErrorCodes 同上：PRJ_055/057/058/059 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/projects/{projectId}/production-handoff/update")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:production:update",
            name = "AI 平台 / 生产承接信息更新",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "信息整组更新（联系人次/下一步/进展/预计开始，null 即清空，依据必填留痕）——透传 aiplatform",
            description = "「生产承接更新」之字段面：交付联系人（至多 200 字）/下一步说明"
                    + "（人工措辞，至多 1000 字）/进展更新（人工措辞、不上工程术语，至多 2000 字）"
                    + "/预计开始时间<strong>整组替换</strong>——不收半组，每次全量提交；未录入段"
                    + "传 <strong>null 即清空</strong>（预计开始时间清空＝回待安排呈现，「仅真实值"
                    + "显示」）。状态不动（留痕行 from=to 在场快照）。依据必填 400 <strong>PRJ_057"
                    + "</strong>（4057）；字段超长 400 <strong>PRJ_058</strong>（4058）；操作者"
                    + "必携（X-User-Id/X-User-Name 透传头）400 <strong>PRJ_059</strong>（4059——"
                    + "承接动作以操作者为审计正身，缺头拒不留无名痕）；无承接记录 404 "
                    + "<strong>PRJ_055</strong>（4055）；项目不存在 404 PRJ_001（4001）。"
                    + "命令体逐字镜像 {contact, nextStep, progressNote, estimatedStartAt, basis}。"
                    + "回执＝承接现值（更新后全量面）。")
    public ApiResponse<AiplatformProductionHandoffResponse> update(@PathVariable String projectId,
            @RequestBody AiplatformProductionUpdateCommand command) {
        return ApiResponse.ok(productionAppService.update(projectId, command));
    }

    // 不挂 @ErrorCodes 同上：PRJ_055/056/057/059 等码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/projects/{projectId}/production-handoff/advance")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:production:advance",
            name = "AI 平台 / 生产承接推进",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "推进（待安排生产 → 生产中，两态唯一边，依据必填留痕）——透传 aiplatform",
            description = "<strong>两态唯一迁移边</strong>（1=待安排生产 → 2=生产中——生产中是"
                    + "稳态，<strong>再推进 409 PRJ_056</strong>〔4056〕）。依据必填 400 "
                    + "<strong>PRJ_057</strong>（4057——推进依据是审计正身，BFF 不前置拦截）；"
                    + "操作者必携（X-User-Id/X-User-Name 透传头）400 <strong>PRJ_059</strong>"
                    + "（4059）；无承接记录 404 <strong>PRJ_055</strong>（4055——开工判定未"
                    + "触发的项目无从推进）；项目不存在 404 PRJ_001（4001）。命令体逐字镜像 "
                    + "{basis}。回执＝承接现值（推进后全量面——两态第二实值 2=生产中）。")
    public ApiResponse<AiplatformProductionHandoffResponse> advance(@PathVariable String projectId,
            @RequestBody AiplatformProductionActionCommand command) {
        return ApiResponse.ok(productionAppService.advance(projectId, command));
    }

    // 不挂 @ErrorCodes 同上：PRJ_001 码以 description 文档化（忠实透传、零加戏）
    @PostMapping("/projects/{projectId}/production-handoff/needs-user-supplement")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:production:supplement",
            name = "AI 平台 / 需用户补充置位",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "「需用户补充」置位/清零（前台六态盖帽标志）——透传 aiplatform",
            description = "生产承接期间需用户补资料/确认事项时<strong>置位</strong>"
                    + "（required=true——前台交付状态六态盖帽「<strong>需用户补充</strong>」"
                    + "〔第 6 态，优先于承接两态呈现——卡片双行呈现盖帽态与标志〕，处理入口在"
                    + "对话区沟通）；处理完<strong>清零</strong>（required=false——回承接两态"
                    + "呈现）。<strong>盖帽语义归后台人工判定</strong>（何时需要用户补充、补充"
                    + "什么——对话区沟通承载），平台只落项目级标志。命令体逐字镜像 "
                    + "{required}；回执 data＝null（ApiResponse&lt;Void&gt;——无回读面，回执即"
                    + "信封）。项目不存在（含畸形 id）404 <strong>PRJ_001</strong>（数字业务码"
                    + " 4001）。")
    public ApiResponse<Void> setNeedsUserSupplement(@PathVariable String projectId,
            @RequestBody AiplatformProductionSupplementCommand command) {
        productionAppService.setNeedsUserSupplement(projectId, command);
        return ApiResponse.ok();
    }
}
