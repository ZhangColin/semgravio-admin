package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformAgentConfigAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformEdgeModelUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformEdgeModelConfigResponse;
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
 * AI 平台智能体配置控制器——技能线 T5 智能体运营配置读写＋变更留痕读面（issue #84）＋
 * 边缘调用点档位三端点（issue #112 B3，aiplatform #366 边缘四点收编、#367 档位键即时
 * 校验——spec #101 B 批），镜像 aiplatform #251／#366。独立 controller 面（与技能库面
 * 零代码交集）。
 *
 * <p>北向 {@code /api/admin/aiplatform/agent-configs/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/agent-configs/**}（机机签名出站）。三座端点寻址键 {@code agentKey}
 * 为字符串路径参数——admin 透传不解析不预校验（spec #79），未知键由 provider 判 404
 * PRJ_033（数字业务码 4033——配置面只有 main=主智能体 / executor=run 执行体 /
 * designer=设计执行体 三座；边缘调用点走 /edge/{edgeKey} 档位端点）经
 * {@code AiplatformUpstreamErrorAdvice} 原样还原。边缘端点寻址键 {@code edgeKey} 四把
 * （naming=项目取名 / compaction=上下文压缩 / classification=入口分类 / precheck=技能预检
 * ——一次性轻调用，值面仅档位键无 prompt），未知键 404 PRJ_064（数字业务码 4064）。
 * 读面/留痕均无分页（provider 单对象/List 直出——同形不造分页）。</p>
 *
 * <p>modelId 语义＝<strong>模型档位键</strong>（ADR-0031 语义键，如 flash/pro/
 * doubao-flagship——档位表 {@code GET /model-tiers} 的 enabled=true 键集即值域正本与改档
 * 下拉数据源；V44 存量已从裸模型名刷档，非裸名手输）。写口档位键即时校验（aiplatform
 * #367）：非空须在模型档位面有行且启用——缺行 400 PRJ_074（4074）、停用 400 PRJ_075
 * （4075），错值当场拦截不再靠留痕回滚兜底；null＝清空回落缺省不校验。routingStrongModelId
 * （路由强档——#354 升档落点，主档/强档同范式管理）为 B3 新增字段对：读面三件（生效值/
 * 覆盖标记/代码缺省预览）、命令一件、留痕读面 old/new 两件。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码之末两码；edge 三端点复用——issue #112 B3
 * 零新码）：读 {@code agent-config:read} 一码挂三座读面/留痕读面＋边缘读面/留痕读面四端
 * 点；写 {@code agent-config:update} 独立成码（三座全量写与边缘档位写共用）。写口操作者
 * 经框架 {@code OpenApiClient} 自动透传 {@code X-User-Id/X-User-Name} 出站头（identity
 * 同款），provider 落痕（配置变更必留痕——值面有动必落前后全量值快照）——缺头由 provider
 * 域面守卫 400 PRJ_034（数字业务码 4034）拦截，admin 不拦。全量写经框架
 * {@code OpenApiClient.put} 出站——HTTP PUT 动词（PUT 全量语义，请求体即覆盖态终态）。</p>
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
    // provider 错误码目录（忠实透传、零加戏——spec #79）；错误码以 description 文档化（PRJ_033/034/064/074/075 等）。
    @GetMapping("/{agentKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:read",
            name = "AI 平台 / 智能体配置查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "智能体配置读面（生效值＋覆盖标记＋默认预览）——透传 aiplatform",
            description = "该智能体生效配置：systemPrompt／模型档位生效值（库覆盖值或枚举默认"
                    + "——装配实取值；modelId 语义＝档位键 ADR-0031 语义键如 flash/pro，"
                    + "非裸模型名）＋覆盖标记（systemPromptOverridden/modelIdOverridden，"
                    + "运营要看到「现在跑的是覆盖还是默认」）＋枚举默认预览（defaultSystemPrompt/"
                    + "defaultModelId——清空覆盖即落此值，恢复默认前可先看落点）＋增强工具开关"
                    + "存储态（webSearchEnabled/fetchUrlEnabled——工具面清单与窄幅开关另见 "
                    + "/agent-tools 两端点）＋路由强档三件（routingStrongModelId 生效值——"
                    + "#354 升档落点、routingStrongModelIdOverridden、defaultRoutingStrongModelId"
                    + " 代码缺省 pro——主档/强档同范式管理）＋最近写者。无覆盖行＝全回落（覆盖"
                    + "标记 false、写者 null）。寻址稳定键三把：main=主智能体 / executor=run "
                    + "执行体 / designer=设计执行体；边缘调用点（naming/compaction/"
                    + "classification/precheck）走 /edge/{edgeKey} 档位端点——三座端点对边缘"
                    + "键 404 PRJ_033（数字业务码 4033）。")
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
            description = "PUT 全量语义：请求体即该智能体覆盖态终态——systemPrompt/modelId/"
                    + "routingStrongModelId 全文替换（null/缺省/纯空白＝清空覆盖，装配回落"
                    + "枚举/代码默认——调整工作协议、换模型不发版）；webSearchEnabled/"
                    + "fetchUrlEnabled 工具开关缺省 true＝开（关即退出槽位装配面、开即回归"
                    + "——工具面窄幅写口另见 PUT /agent-tools/{toolName}）。档位键即时校验"
                    + "（aiplatform #367）：modelId 与 routingStrongModelId 非空时必须在模型"
                    + "档位面（GET /model-tiers，enabled=true 键集即放行值域与改档下拉数据源）"
                    + "有行且启用——缺行 400 PRJ_074（数字业务码 4074）、停用 400 PRJ_075"
                    + "（4075），错值写不进去不再靠留痕回滚兜底；null＝清空回落缺省不校验。"
                    + "装配生效语义：动态查库，变更后下一轮命令构建自然取新值（智能体工厂缓存"
                    + "键含 sysPrompt、模型串与工具面规格——新值即新实例，进行中 run 不定格）。"
                    + "变更留痕 append-only：值面有动必落痕（变更前后全量值快照＋操作者，"
                    + "traces 读面可查）；同值重写＝幂等回执不落痕；回滚＝把留痕旧值快照写回"
                    + "（即一次新变更、留新痕，不做版本树）。X-User-Id/X-User-Name 透传头必"
                    + "留痕（缺头 400 PRJ_034＝数字业务码 4034）。寻址同 GET（未知键 404 "
                    + "PRJ_033＝4033）。回执与 GET 同形。HTTP <strong>PUT</strong> 动词。")
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
                    + "（同值幂等重写不落痕）：变更前后全量值快照（old*/new* 对——prompt/档位/"
                    + "路由强档三件可空、开关四件恒在场；旧值快照即回滚写回的依据——含强档，"
                    + "读面可改就必须可回滚）＋操作者＋时刻。append-only：留痕不随任何动作删除，"
                    + "历史事实不随配置行消失。回滚＝把某痕旧值快照 PUT 回写（即一次新变更、"
                    + "留新痕）。寻址同 GET（未知键 404 PRJ_033＝数字业务码 4033）。")
    public ApiResponse<List<AiplatformAgentConfigTraceResponse>> traces(@PathVariable String agentKey) {
        return ApiResponse.ok(agentConfigAppService.listTraces(agentKey));
    }

    @GetMapping("/edge/{edgeKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:read",
            name = "AI 平台 / 智能体配置查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "边缘调用点档位读面（生效值＋覆盖标记＋默认预览）——透传 aiplatform",
            description = "边缘调用点（一次性轻调用）的模型档位生效值：库覆盖值或代码缺省档位"
                    + "键（#366 同表收编——值面仅模型档位，无 prompt/工具开关/路由件；字段名"
                    + "对齐三座面，前端同一渲染件可复用）。寻址稳定键四把：naming=项目取名 / "
                    + "compaction=上下文压缩 / classification=入口分类 / precheck=技能预检——"
                    + "缺省档位键全部 flash（轻调用不烧重档）。modelId 语义＝档位键（ADR-0031 "
                    + "语义键——档位面 GET /model-tiers 为值域正本）。改档下一轮命令构建即生效"
                    + "（装配动态查库，进行中 run 不定格）。未知键 404 PRJ_064（数字业务码 "
                    + "4064——档位配置面只有四点；三座智能体键对本端点同 404）。")
    public ApiResponse<AiplatformEdgeModelConfigResponse> edgeConfig(@PathVariable String edgeKey) {
        return ApiResponse.ok(agentConfigAppService.getEdgeModelConfig(edgeKey));
    }

    @PutMapping("/edge/{edgeKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:update",
            name = "AI 平台 / 智能体配置维护",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "边缘调用点档位写（窄值面＋变更留痕）——透传 aiplatform",
            description = "PUT 全量语义、值面仅模型档位：请求体 {modelId} 即该边缘点覆盖态"
                    + "终态——null/缺省/纯空白＝清空覆盖回落代码缺省 flash；换档不发版"
                    + "（装配库值优先，下一轮命令构建即生效）。档位键即时校验与三座全量写同"
                    + "口径（aiplatform #367）：非空须在模型档位面有行且启用——缺行 400 "
                    + "PRJ_074（数字业务码 4074）、停用 400 PRJ_075（4075）；null＝清空回落"
                    + "缺省不校验。留痕与三座全量写同机制：值面有动必落痕（前后快照＋操作者），"
                    + "同值幂等重写不落痕，回滚＝旧值快照的 modelId 写回（一次新变更、留新痕，"
                    + "不做版本树）。X-User-Id/X-User-Name 透传头必留痕（缺头 400 PRJ_034＝"
                    + "4034）。寻址同 GET（未知键 404 PRJ_064＝4064）。回执与 GET 同形。"
                    + "HTTP <strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformEdgeModelConfigResponse> updateEdgeModel(
            @PathVariable String edgeKey, @RequestBody AiplatformEdgeModelUpdateCommand command) {
        return ApiResponse.ok(agentConfigAppService.updateEdgeModel(edgeKey, command));
    }

    @GetMapping("/edge/{edgeKey}/traces")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:agent-config:read",
            name = "AI 平台 / 智能体配置查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "边缘调用点档位留痕读面（历史可查，回滚依据）——透传 aiplatform",
            description = "该边缘点全部配置变更留痕，按时间倒序（最近先）——快照列与三座同形"
                    + "（边缘行档位列之外的快照件恒缺省态：prompt null、开关 true、路由件"
                    + "缺省态——provider 边缘写把非档位覆盖列结构性归缺省）。回滚＝把某痕旧值"
                    + "快照的 modelId PUT 回写（即一次新变更、留新痕，不做版本树）。寻址同"
                    + "GET（未知键 404 PRJ_064＝数字业务码 4064）。")
    public ApiResponse<List<AiplatformAgentConfigTraceResponse>> edgeTraces(@PathVariable String edgeKey) {
        return ApiResponse.ok(agentConfigAppService.listEdgeModelTraces(edgeKey));
    }
}
