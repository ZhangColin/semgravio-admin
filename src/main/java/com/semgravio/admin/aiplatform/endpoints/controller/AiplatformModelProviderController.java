package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformModelProviderAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformRemoteModelCatalogResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台模型供应商控制器——BFF 模型供应商域七端点（issue #110 B1，spec #101 B 批，镜像
 * provider aiplatform #367 通道 CRUD＋留痕 / #368 远端清单拉取的
 * {@code BackofficeModelProviderController}）。
 *
 * <p>北向路径逐字镜像 aiplatform {@code /api/backoffice/model-providers/**}（前缀换
 * {@code /api/admin/aiplatform}）。通道语义（键名＝协议路由前缀、掩码单点、缺省肢＝
 * 保持现值、删除引用拦截、留痕 append-only）全由 provider 裁决，BFF 零推导零校验。
 * <strong>apiKey 全链只写不读明文</strong>：写口收全量原样出站；读面与留痕快照 provider
 * 已掩码（尾 4 位）——admin 忠实透传掩码值，不二次处理不尝试解掩。错误经
 * {@code AiplatformUpstreamErrorAdvice} 原样透传（如 404 PRJ_065→4065、409 PRJ_067→
 * 4067、400 PRJ_069/034/076→4069/4034/4076、409 PRJ_072→4072、502 PRJ_077→4077）。</p>
 *
 * <p>权限码（spec #101 B 批，V23 已一次记账——本票落通道四码）：读
 * {@code model-provider:read} 一码挂读面四端点（清单/单行/远端清单拉取/留痕读面——
 * remote-models 是只读探测不动库，勾选入库复用档位创建写口、权限在彼处自然拦截；
 * traces 复用 read）；写 {@code model-provider:create/update/delete} 三码——读写分权、
 * 每写操作一码惯例（最小授权）。写口操作者身份由框架 {@code OpenApiClient} 自动透传
 * {@code X-User-Id/X-User-Name} 出站头（写操作必留痕，缺头由 provider 域面守卫
 * PRJ_034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/model-providers")
@Validated
@Tag(name = "Admin Aiplatform Model Provider", description = "AI 平台 · 模型供应商通道维护（CRUD＋apiKey 掩码＋远端清单拉取＋变更留痕）")
public class AiplatformModelProviderController {

    private final AiplatformModelProviderAppService modelProviderAppService;

    public AiplatformModelProviderController(AiplatformModelProviderAppService modelProviderAppService) {
        this.modelProviderAppService = modelProviderAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #101）；错误码以 description 文档化（PRJ_065 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:read",
            name = "AI 平台 / 模型供应商查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道清单读面（apiKey 一律掩码）——透传 aiplatform",
            description = "全部通道行按键序；apiKey 只出掩码形（尾 4 位，null＝"
                    + "未配置）——provider 已掩码、admin 忠实透传不二次处理不尝试"
                    + "解掩，接口回显面零明文 key。种子两通道：volcengine 主用"
                    + "（火山方舟）＋deepseek 官方备胎。换通道、换 key、换 baseUrl "
                    + "皆不发版不重启——下一轮命令构建即生效（模型实例缓存键含"
                    + "通道指纹）。")
    public ApiResponse<List<AiplatformModelProviderResponse>> list() {
        return ApiResponse.ok(modelProviderAppService.listProviders());
    }

    @GetMapping("/{providerKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:read",
            name = "AI 平台 / 模型供应商查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道单行读面（掩码形）——透传 aiplatform",
            description = "该通道行（掩码形同清单）。寻址键即协议路由前缀"
                    + "（volcengine/deepseek 等——透传不解析不预校验）。通道不存在"
                    + " 404 <strong>PRJ_065</strong>（数字业务码 4065）。")
    public ApiResponse<AiplatformModelProviderResponse> detail(@PathVariable String providerKey) {
        return ApiResponse.ok(modelProviderAppService.getProvider(providerKey));
    }

    @PostMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:create",
            name = "AI 平台 / 模型供应商创建",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道创建（apiKey 收全量值，创建即留痕）——透传 aiplatform",
            description = "新建接入通道：键名（＝协议路由前缀，OpenAI 兼容统一接、"
                    + "SDK 内置前缀走原生 provider）随体、已存在 409 <strong>PRJ_067"
                    + "</strong>（数字业务码 4067）；baseUrl 必填（400 <strong>PRJ_069"
                    + "</strong>＝4069）；apiKey 收<strong>全量值</strong>（写面唯一"
                    + "明文入口——null/空白＝暂不配置，装配按 PRJ_063〔HTTP 503，数字业务码 "
                    + "4063〕明确报错）；"
                    + "enabled 缺省 true（Boolean 装箱——null 出站在场＝provider 缺省"
                    + " true）。回执 apiKey 已掩码（尾 4 位忠实透传）。创建即落痕"
                    + "（old 全 null）。新通道接入＝建通道行＋档位行两步（档位面 "
                    + "POST /model-tiers），不动结构。X-User-Id/X-User-Name 透传头"
                    + "必留痕（缺头 400 <strong>PRJ_034</strong>＝4034）。")
    public ApiResponse<AiplatformModelProviderResponse> create(
            @RequestBody AiplatformModelProviderCreateCommand command) {
        return ApiResponse.ok(modelProviderAppService.createProvider(command));
    }

    @PutMapping("/{providerKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:update",
            name = "AI 平台 / 模型供应商维护",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道更新（换 key/换地址/停启用；留痕）——透传 aiplatform",
            description = "PUT 全量语义：<strong>baseUrl 终态替换</strong>；apiKey 与 "
                    + "enabled <strong>缺省＝保持现值</strong>（apiKey 读面只出掩码、"
                    + "缺省清空＝读一次即丢 key；enabled 是有意状态、缺省翻转会把停用"
                    + "通道在换 key 时意外复启——两肢皆显式写才动；缺省肢在 wire 请求体"
                    + "上保真——可空肢 null 在场＝保持现值，不臆造默认值）。换 key 携"
                    + "全量新值（写面唯一明文入口；携现值掩码形如 ****abcd 同判保持"
                    + "＝round-trip 安全）；清除 key 无写面（下线通道走 enabled=false"
                    + "——停用＝引用它的装配明确报错，不静默换道）。<strong>换 key/换 "
                    + "baseUrl 不重启</strong>：下一轮命令构建即生效（模型实例缓存键含"
                    + "通道指纹——同模型串换 key 换地址＝新实例，进行中 run 不定格）。"
                    + "值面无动＝幂等回执不落痕；回滚＝留痕旧值快照写回（换 key 回滚＝"
                    + "操作者重输旧 key 全量值——留痕只出掩码，不作密钥找回面）。通道"
                    + "不存在 404 <strong>PRJ_065</strong>（4065）；baseUrl 缺 400 "
                    + "<strong>PRJ_069</strong>（4069）；X-User-Id/X-User-Name 必留痕"
                    + "（缺头 400 <strong>PRJ_034</strong>＝4034）。回执与 GET 同形。"
                    + "HTTP <strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformModelProviderResponse> update(
            @PathVariable String providerKey, @RequestBody AiplatformModelProviderUpdateCommand command) {
        return ApiResponse.ok(modelProviderAppService.updateProvider(providerKey, command));
    }

    @DeleteMapping("/{providerKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:delete",
            name = "AI 平台 / 模型供应商删除",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道删除（被档位引用即拒）——透传 aiplatform",
            description = "删除通道行；仍被档位行引用即 409 <strong>PRJ_072</strong>"
                    + "（数字业务码 4072——先删改挂其下的档位行，悬空引用不留静默坑）。"
                    + "删成落痕（new 全 null，留痕跨行存活）。通道不存在 404 "
                    + "<strong>PRJ_065</strong>（4065）；X-User-Id/X-User-Name 必留痕"
                    + "（缺头 400 <strong>PRJ_034</strong>＝4034）。回执 data＝null"
                    + "（信封即回执）。HTTP <strong>DELETE</strong> 动词。")
    public ApiResponse<Void> delete(@PathVariable String providerKey) {
        modelProviderAppService.deleteProvider(providerKey);
        return ApiResponse.ok();
    }

    @PostMapping("/{providerKey}/remote-models")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:read",
            name = "AI 平台 / 模型供应商查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "远端模型清单手动拉取（对照库内档位）——透传 aiplatform",
            description = "按通道行真拉远端模型清单（OpenAI 兼容 GET {baseUrl}/models，"
                    + "方舟端点实测同形状）并与库内档位对照：条目 trackedTierKeys 空＝"
                    + "<strong>远端有库里无（建议入库）</strong>；staleTiers＝<strong>库内"
                    + "指向远端已无（提示过时）</strong>。每次全量真拉无缓存（手动同步"
                    + "——自动定时同步明确不做）。条目 status 如实转述上游（方舟：无 "
                    + "status＝活跃、Shutdown/Retiring＝下架/退场中——建议入库时结合"
                    + "判断，接口不代拍）。<strong>只读探测不动库</strong>：勾选入库复用"
                    + "档位创建写口（POST /model-tiers，modelName 填所选条目 modelId、"
                    + "语义键人工命名——权限在彼处自然拦截）。上游不可用明确报错不静默"
                    + "（502 <strong>PRJ_077</strong>＝数字业务码 4077 带理由）；通道 "
                    + "key 空 400 <strong>PRJ_076</strong>（4076——无凭证不打上游）；"
                    + "停用通道可拉（巡检语义）；通道不存在 404 <strong>PRJ_065</strong>"
                    + "（4065）。纯读动作零留痕（挂 model-provider:read）。HTTP "
                    + "<strong>POST</strong> 动词（拉取动作非资源创建，provider 路由"
                    + "原形）。")
    public ApiResponse<AiplatformRemoteModelCatalogResponse> pullRemoteModels(
            @PathVariable String providerKey) {
        return ApiResponse.ok(modelProviderAppService.pullRemoteModels(providerKey));
    }

    @GetMapping("/{providerKey}/traces")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-provider:read",
            name = "AI 平台 / 模型供应商查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "通道变更留痕读面（历史可查；key 快照掩码）——透传 aiplatform",
            description = "该通道全部变更留痕按时间倒序（最近先）。每痕＝一次实际变更"
                    + "（幂等重写不落痕）：前后全量值快照（apiKey 快照读面一律<strong>掩码"
                    + "</strong>——尾 4 位足以比对哪次换了哪把 key，不作密钥找回面；admin "
                    + "忠实透传不二次处理）＋操作者＋时刻。创建痕 old 全 null、删除痕 "
                    + "new 全 null；append-only 跨行存活——行删除后留痕仍可读（空清单＝"
                    + "从未有行，行读面负责 404 判别）。通道不存在 404 <strong>PRJ_065"
                    + "</strong>（数字业务码 4065）。")
    public ApiResponse<List<AiplatformModelProviderTraceResponse>> traces(
            @PathVariable String providerKey) {
        return ApiResponse.ok(modelProviderAppService.listTraces(providerKey));
    }
}
