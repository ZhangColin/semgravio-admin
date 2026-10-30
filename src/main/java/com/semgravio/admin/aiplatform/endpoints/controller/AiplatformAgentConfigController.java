package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformAgentConfigAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
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
 * AI 平台智能体配置控制器——技能线 T5 智能体运营配置读写＋变更留痕读面（issue #84），
 * 镜像 aiplatform #251（#246 技能线 T5）。独立 controller 面（与技能库面零代码交集）。
 *
 * <p>北向 {@code /api/admin/aiplatform/agent-configs/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/agent-configs/**}（机机签名出站）。寻址键 {@code agentKey} 为字符串
 * 路径参数——admin 透传不解析不预校验（spec #79），未知键由 provider 判 404 PRJ_033
 * （数字业务码 4033——运营配置面只有 main/executor 两座智能体，classify/naming 等一次性
 * 判定不属本面）经 {@code AiplatformUpstreamErrorAdvice} 原样还原。读面/留痕均无分页
 * （provider 单对象/List 直出——同形不造分页）。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码之末两码——高敏感独立授权面，改 prompt 等操作
 * 可单独授予窄角色）：读 {@code agent-config:read} 一码挂读面/留痕读面两端点；写
 * {@code agent-config:update} 独立成码。写口操作者经框架 {@code OpenApiClient} 自动透传
 * {@code X-User-Id/X-User-Name} 出站头（identity 同款），provider 落痕（配置变更必留痕——
 * 值面有动必落前后全量值快照）——缺头由 provider 域面守卫 400 PRJ_034（数字业务码 4034）
 * 拦截，admin 不拦。全量写经框架 {@code OpenApiClient.put} 出站——HTTP PUT 动词（PUT 全量
 * 语义，请求体即覆盖态终态）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/agent-configs")
@Validated
@Tag(name = "Admin Aiplatform Agent Config", description = "AI 平台 · 智能体配置")
public class AiplatformAgentConfigController {

    private final AiplatformAgentConfigAppService agentConfigAppService;

    public AiplatformAgentConfigController(AiplatformAgentConfigAppService agentConfigAppService) {
        this.agentConfigAppService = agentConfigAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #79）；错误码以 description 文档化（PRJ_033/034 等）。
    @GetMapping("/{agentKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:read",
            name = "AI 平台 / 智能体配置查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "智能体配置读面（生效值＋覆盖标记＋默认预览）——透传 aiplatform",
            description = "该智能体生效配置：systemPrompt／模型档位生效值（库覆盖值或枚举默认"
                    + "——装配实取值）＋覆盖标记（systemPromptOverridden/modelIdOverridden，"
                    + "运营要看到「现在跑的是覆盖还是默认」）＋枚举默认预览（defaultSystemPrompt/"
                    + "defaultModelId——清空覆盖即落此值，恢复默认前可先看落点）＋增强工具开关"
                    + "存储态（webSearchEnabled/fetchUrlEnabled——工具面清单与窄幅开关另见 "
                    + "/agent-tools 两端点）＋最近写者。无覆盖行＝全回落（覆盖标记 false、"
                    + "写者 null）。寻址稳定键两把：main=主智能体 / executor=run 执行体；"
                    + "classify/naming 等一次性判定不属配置面——未知键 404 PRJ_033（数字业务码"
                    + " 4033）。")
    public ApiResponse<AiplatformAgentConfigResponse> config(@PathVariable String agentKey) {
        return ApiResponse.ok(agentConfigAppService.getConfig(agentKey));
    }

    @PutMapping("/{agentKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:update",
            name = "AI 平台 / 智能体配置维护",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "智能体配置全量写（覆盖态终态＋变更留痕）——透传 aiplatform",
            description = "PUT 全量语义：请求体即该智能体覆盖态终态——systemPrompt/modelId "
                    + "全文替换（null/缺省/纯空白＝清空覆盖，装配回落枚举默认——调整工作协议、"
                    + "换模型不发版）；webSearchEnabled/fetchUrlEnabled 工具开关缺省 true＝开"
                    + "（关即退出槽位装配面、开即回归——工具面窄幅写口另见 PUT "
                    + "/agent-tools/{toolName}）。装配生效语义：动态查库，变更后下一轮命令构建"
                    + "自然取新值（智能体工厂缓存键含 sysPrompt、模型串与工具面规格——新值即新"
                    + "实例，进行中 run 不定格）。变更留痕 append-only：值面有动必落痕（变更"
                    + "前后全量值快照＋操作者，traces 读面可查）；同值重写＝幂等回执不落痕；"
                    + "回滚＝把留痕旧值快照写回（即一次新变更、留新痕，不做版本树）。档位名不做"
                    + "白名单校验（错值致新会话失败经留痕写回可回滚）。X-User-Id/X-User-Name "
                    + "透传头必留痕（缺头 400 PRJ_034＝数字业务码 4034）。寻址同 GET（未知键 "
                    + "404 PRJ_033＝4033）。回执与 GET 同形。HTTP <strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformAgentConfigResponse> update(
            @PathVariable String agentKey, @RequestBody AiplatformAgentConfigUpdateCommand command) {
        return ApiResponse.ok(agentConfigAppService.updateConfig(agentKey, command));
    }

    @GetMapping("/{agentKey}/traces")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:read",
            name = "AI 平台 / 智能体配置查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "配置变更留痕读面（历史可查，回滚依据）——透传 aiplatform",
            description = "该智能体全部配置变更留痕，按时间倒序（最近先）。每痕＝一次实际变更"
                    + "（同值幂等重写不落痕）：变更前后全量值快照（old*/new* 四件对——旧值快照"
                    + "即回滚写回的依据）＋操作者＋时刻。append-only：留痕不随任何动作删除，"
                    + "历史事实不随配置行消失。回滚＝把某痕旧值快照 PUT 回写（即一次新变更、"
                    + "留新痕）。寻址同 GET（未知键 404 PRJ_033＝数字业务码 4033）。")
    public ApiResponse<List<AiplatformAgentConfigTraceResponse>> traces(@PathVariable String agentKey) {
        return ApiResponse.ok(agentConfigAppService.listTraces(agentKey));
    }
}
