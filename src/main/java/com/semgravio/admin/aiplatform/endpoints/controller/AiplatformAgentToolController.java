package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformAgentToolAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentToolToggleCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentToolResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentToolSlotResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台工具面控制器——技能线 T6 工具面清单＋增强工具开关（issue #85），镜像
 * aiplatform #252（#246 技能线 T6）。独立 controller 面（与技能库/智能体配置面零代码
 * 交集）；UI 并入智能体配置页区块呈现（spec #79——不单独种菜单叶）。
 *
 * <p>北向 {@code /api/admin/aiplatform/agent-tools/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/agent-tools/**}（机机签名出站）。寻址键 {@code toolName} 为
 * 字符串路径参数——admin 透传不解析不预校验（spec #79），未知工具名由 provider 判 404
 * PRJ_035（数字业务码 4035——清单读面即全集）、骨架/harness 内建锁死 403 PRJ_036
 * （数字业务码 4036——ADR-0021 编排权不下放配置）、缺目标态 400 PRJ_037（数字业务码
 * 4037）经 {@code AiplatformUpstreamErrorAdvice} 原样还原。清单三槽位有界 List 直出
 * （不造分页）。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码之末两码）：读 {@code agent-tool:read}
 * 挂清单读面；写 {@code agent-tool:toggle} 独立成码（开关虽窄幅但动装配面——与
 * agent-config:update 分权，可单独授予窄角色）。写口操作者经框架 {@code OpenApiClient}
 * 自动透传 {@code X-User-Id/X-User-Name} 出站头（identity 同款），provider 落痕（开关
 * 写必留痕——与智能体配置共表共留痕机制）——缺头由 provider 域面守卫 400 PRJ_034
 * （数字业务码 4034）拦截，admin 不拦。开关经框架 {@code OpenApiClient.put} 出站——
 * HTTP PUT 动词（provider 窄幅写口形制）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/agent-tools")
@Validated
@Tag(name = "Admin Aiplatform Agent Tool", description = "AI 平台 · 工具面")
public class AiplatformAgentToolController {

    private final AiplatformAgentToolAppService agentToolAppService;

    public AiplatformAgentToolController(AiplatformAgentToolAppService agentToolAppService) {
        this.agentToolAppService = agentToolAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #79）；错误码以 description 文档化（PRJ_035/036/037/034 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-tool:read",
            name = "AI 平台 / 工具面查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "工具面清单（按职能槽位列当前挂载工具）——透传 aiplatform",
            description = "排障/审计面：按职能槽位（main=主智能体 / executor=run 执行体 / "
                    + "subagent=子智能体，与技能槽位同键）列当前挂载工具。三呈现源：平台资产"
                    + "（骨架=编排链路＋项目事实只读件；增强=联网搜索/网页抓取，enabled 按运营"
                    + "配置生效态——false＝在册但退出装配面）、harness 内建编码工具（executor 槽，"
                    + "框架注册自省的呈现口径，不可开关；main 只读面结构性无）、子智能体声明工具面"
                    + "（self-test 哨兵 allowlist）。骨架与 harness 内建结构性锁死（ADR-0021 编排权"
                    + "不下放配置），开关面只收增强两件（清单读面即全集——PUT 开关按注册名寻址）。"
                    + "三槽位有界 List 直出（不造分页）。")
    public ApiResponse<List<AiplatformAgentToolSlotResponse>> inventory() {
        return ApiResponse.ok(agentToolAppService.listToolSlots());
    }

    @PutMapping("/{toolName}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-tool:toggle",
            name = "AI 平台 / 工具面开关",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "增强工具开关（窄幅：关即退出装配面、开即回归）——透传 aiplatform",
            description = "按工具注册名窄幅开关，仅增强工具（web_search=联网搜索 / "
                    + "fetch_url=网页抓取）接受：enabled=false 该工具退出对应槽位装配面"
                    + "（模型不可见），true 回归——动态生效：下一轮命令构建即新装配（工厂缓存键"
                    + "含工具面规格，进行中 run 不定格）。变更走配置留痕（与智能体配置同机制："
                    + "值面有动才落痕、幂等回执零写入、回滚＝旧值写回）。骨架工具（问答/PRD 落库/"
                    + "计划/收口信号等编排链路件）与 harness 内建编码工具结构性锁死——接口层明确"
                    + "拒绝（403 PRJ_036＝数字业务码 4036，编排权不下放配置）；未知工具名 404 "
                    + "PRJ_035＝数字业务码 4035（清单读面即全集）。enabled 必填（无缺省翻转语义，"
                    + "缺即 400 PRJ_037＝数字业务码 4037——Boolean 装箱缺省 null 透传，provider "
                    + "裁决）。X-User-Id/X-User-Name 透传头必留痕（缺头 400 PRJ_034＝数字业务码 "
                    + "4034）。回执＝开关后工具行（与清单嵌套行同形）。HTTP <strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformAgentToolResponse> toggle(
            @PathVariable String toolName, @RequestBody(required = false) AiplatformAgentToolToggleCommand command) {
        return ApiResponse.ok(agentToolAppService.toggleTool(toolName, command));
    }
}
