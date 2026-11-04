package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformRemoteModelCatalogResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelProviderCreateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelProviderTraceWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelProviderUpdateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelProviderWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformRemoteModelCatalogWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 模型供应商域 BFF 应用服务——通道全生命周期管理（issue #110 B1，spec
 * #101 B 批，镜像 aiplatform #367 通道 CRUD＋留痕 / #368 远端清单拉取）：模型供应商
 * （通道）行的清单/单行/创建/更新/删除/远端模型清单手动拉取/变更留痕读面。通道配置归
 * aiplatform business.project 上下文——admin 不持业务逻辑、不持通道数据（与档位域
 * 独立 controller 面，通道/档位分权）。
 *
 * <p>admin 作为 BFF 忠实透传（spec #101）：寻址键 {@code providerKey} 为字符串路径
 * 参数——透传不解析不预校验，未知键由 provider 判 404 PRJ_065（数字业务码 4065）。
 * <strong>apiKey 全链只写不读明文</strong>：写口收全量原样出站（创建/换 key 的唯一
 * 明文入口）；读面与留痕快照 provider 已掩码（尾 4 位）——admin 忠实透传掩码值，不
 * 二次处理不尝试解掩。PUT 缺省肢（apiKey/enabled null＝保持现值）在 wire 请求体上
 * 保真（可空肢不臆造默认值）。删除被档位引用 409 PRJ_072、上游不可用 502 PRJ_077
 * 等错误经 {@link AiplatformClient} 统一翻译为 {@link AiplatformUpstreamException}
 * 原样透传，本层不 try/catch；remote-models 为只读探测不动库（挂 read 码——勾选入库
 * 复用档位创建写口，权限在彼处自然拦截）。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformModelProviderAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformModelProviderAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 通道清单读面（透传 aiplatform）——全部通道行按键序；apiKey 只出掩码形（尾 4 位，
     * null＝未配置），admin 忠实透传掩码值不二次处理。种子两通道：volcengine 主用
     * （火山方舟）＋deepseek 官方备胎。
     */
    public List<AiplatformModelProviderResponse> listProviders() {
        return aiplatformClient.listModelProviders().stream()
                .map(AiplatformModelProviderAppService::toProvider)
                .toList();
    }

    /**
     * 通道单行读面（透传 aiplatform）——掩码形同清单。{@code providerKey} 字符串路径
     * 参数原样出站（键名即协议路由前缀——volcengine/deepseek 等，不解析不预校验），
     * 未知键由 provider 判 404 PRJ_065（数字业务码 4065）。
     */
    public AiplatformModelProviderResponse getProvider(String providerKey) {
        return toProvider(aiplatformClient.getModelProvider(providerKey));
    }

    /**
     * 通道创建（透传 aiplatform）——新建接入通道：键名（＝协议路由前缀，OpenAI 兼容
     * 统一接、SDK 内置前缀走原生 provider）随体、已存在 409 PRJ_067；baseUrl 必填
     * （PRJ_069）；apiKey 收全量值（null/空白＝暂不配置——装配按 PRJ_063 明确报错）；
     * enabled 缺省 true。回执 apiKey 已掩码（尾 4 位忠实透传）。创建即落痕（old 全
     * null）。新通道接入＝建通道行＋档位行两步（档位面 POST /model-tiers），不动结构。
     */
    public AiplatformModelProviderResponse createProvider(AiplatformModelProviderCreateCommand command) {
        return toProvider(aiplatformClient.createModelProvider(
                new AiplatformModelProviderCreateWireRequest(
                        command.providerKey(), command.baseUrl(), command.apiKey(), command.enabled())));
    }

    /**
     * 通道更新（透传 aiplatform）——PUT 全量语义：baseUrl 终态替换；apiKey 与 enabled
     * 缺省＝保持现值（apiKey 读面只出掩码、缺省清空＝读一次即丢 key；enabled 缺省翻转
     * 会把停用通道在换 key 时意外复启——两肢皆显式写才动）。缺省肢在 wire 请求体上保真
     * （可空肢 null 在场＝保持现值）。换 key 携全量新值（写面唯一明文入口）；清除 key
     * 无写面（下线通道走 enabled=false）。值面无动＝幂等回执不落痕；回滚＝留痕旧值快照
     * 写回（换 key 回滚＝操作者重输旧 key 全量值——留痕只出掩码，不作密钥找回面）。换
     * key/换地址下一轮命令构建即生效（模型实例缓存键含通道指纹——进行中 run 不定格）。
     */
    public AiplatformModelProviderResponse updateProvider(
            String providerKey, AiplatformModelProviderUpdateCommand command) {
        return toProvider(aiplatformClient.updateModelProvider(providerKey,
                new AiplatformModelProviderUpdateWireRequest(
                        command.baseUrl(), command.apiKey(), command.enabled())));
    }

    /**
     * 通道删除（透传 aiplatform）——删除通道行；仍被档位行引用即 409 PRJ_072（先删改
     * 挂其下的档位行——悬空引用不留静默坑）。删成落痕（new 全 null，留痕跨行存活）。
     * 操作者经框架透传头落痕（缺头由 provider 域面守卫 PRJ_034 拦，admin 不拦）。
     */
    public void deleteProvider(String providerKey) {
        aiplatformClient.deleteModelProvider(providerKey);
    }

    /**
     * 远端模型清单手动拉取（透传 aiplatform）——按通道行真拉远端模型清单（OpenAI 兼容
     * GET {baseUrl}/models）并与库内档位对照：条目 trackedTierKeys 空＝远端有库里无
     * （建议入库）；staleTiers＝库内指向远端已无（提示过时）。每次全量真拉无缓存（手动
     * 同步——自动定时同步明确不做）。条目 status 如实转述上游（方舟：无 status＝活跃、
     * Shutdown/Retiring＝下架/退场中）。上游不可用明确报错不静默（502 PRJ_077 带理由）；
     * 通道 key 空 400 PRJ_076；停用通道可拉（巡检语义）。纯读动作零留痕——「勾选入库」
     * 不经此响应回写，复用档位创建写口。
     */
    public AiplatformRemoteModelCatalogResponse pullRemoteModels(String providerKey) {
        return toCatalog(aiplatformClient.pullRemoteModels(providerKey));
    }

    /**
     * 通道变更留痕读面（透传 aiplatform）——该通道全部变更留痕按时间倒序（最近先）。
     * 每痕＝一次实际变更（幂等重写不落痕）：前后全量值快照（apiKey 快照读面一律掩码
     * ——尾 4 位足以比对哪次换了哪把 key，不作密钥找回面）＋操作者＋时刻。创建痕 old
     * 全 null、删除痕 new 全 null；append-only 跨行存活——行删除后留痕仍可读（空清单
     * ＝从未有行，行读面负责 404 判别）。
     */
    public List<AiplatformModelProviderTraceResponse> listTraces(String providerKey) {
        return aiplatformClient.listModelProviderTraces(providerKey).stream()
                .map(AiplatformModelProviderAppService::toTrace)
                .toList();
    }

    /** wire 行 → 北向行（掩码值忠实透传——admin 不二次处理不尝试解掩）。 */
    private static AiplatformModelProviderResponse toProvider(AiplatformModelProviderWireResponse wire) {
        return new AiplatformModelProviderResponse(
                wire.providerKey(), wire.baseUrl(), wire.apiKeyMasked(), wire.enabled(),
                wire.operatorId(), wire.operatorName(), wire.updatedAt());
    }

    /** wire 留痕行 → 北向留痕行（apiKey 快照掩码值忠实透传）。 */
    private static AiplatformModelProviderTraceResponse toTrace(AiplatformModelProviderTraceWireResponse wire) {
        return new AiplatformModelProviderTraceResponse(
                wire.id(),
                wire.oldBaseUrl(), wire.oldApiKeyMasked(), wire.oldEnabled(),
                wire.newBaseUrl(), wire.newApiKeyMasked(), wire.newEnabled(),
                wire.operatorId(), wire.operatorName(), wire.operatedAt());
    }

    /** wire 拉取回执 → 北向回执（对照结构逐字镜像：条目与过时档位嵌套行同形）。 */
    private static AiplatformRemoteModelCatalogResponse toCatalog(AiplatformRemoteModelCatalogWireResponse wire) {
        return new AiplatformRemoteModelCatalogResponse(
                wire.providerKey(), wire.pulledAt(),
                wire.remoteModels().stream()
                        .map(entry -> new AiplatformRemoteModelCatalogResponse.RemoteModelEntry(
                                entry.modelId(), entry.name(), entry.status(), entry.domain(),
                                entry.trackedTierKeys()))
                        .toList(),
                wire.staleTiers().stream()
                        .map(tier -> new AiplatformRemoteModelCatalogResponse.StaleTier(
                                tier.tierKey(), tier.modelName(), tier.displayName(), tier.enabled()))
                        .toList());
    }
}
