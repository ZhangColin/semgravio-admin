package com.semgravio.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelProviderUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelProviderTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformRemoteModelCatalogResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 模型供应商域（通道 CRUD＋远端清单拉取＋变更留痕）的 wire 契约测试
 * （对接 aiplatform #367/#368，spec #364 T3/T4，issue #110 B1——spec #101 B 批）。
 *
 * <p>契约事实（2026-11-03 对照 aiplatform 源码 {@code BackofficeModelProviderController} /
 * {@code BackofficeModelProviderResponse} / {@code BackofficeModelProviderTraceResponse} /
 * {@code BackofficeRemoteModelCatalogResponse} / {@code ModelProviderCreateCommand} /
 * {@code ModelProviderUpdateCommand} / {@code ModelProvider#maskApiKey} / V44 种子 /
 * {@code BackofficeModelCatalogSeamTest} 实值核实）：</p>
 * <ul>
 *   <li>七端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}：清单/
 *       留痕 data 为<strong>裸 List</strong>（通道行/留痕行有界，List 直出不造分页）；
 *       单行/创建/更新回执同形（单对象）；删除回执 {@code ApiResponse<Void>}（data null
 *       ——信封即回执，同生产承接置位口形制）；远端清单拉取回执为单对象（对照结构
 *       嵌套行）。</li>
 *   <li>读面行（7 件）：{@code providerKey}（键名＝协议路由前缀）＋{@code baseUrl}＋
 *       <strong>{@code apiKeyMasked} 掩码形（尾 4 位——provider {@code maskApiKey}
 *       单点：明文 &lt;8 位出 {@code ****}、≥8 位出 {@code ****}＋尾 4；null＝未配置）</strong>
 *       ——admin 忠实透传掩码值不二次处理不尝试解掩（spec #101）＋{@code enabled}
 *       （原生 boolean 恒在场）＋最近写者两肢（null＝种子/迁移写入）＋{@code updatedAt}。</li>
 *   <li>创建命令体（4 件）：{@code {providerKey, baseUrl, apiKey, enabled}}——逐字镜像
 *       {@code ModelProviderCreateCommand}。apiKey 收<strong>全量值</strong>（写面唯一明文
 *       入口；null/空白＝暂不配置）；enabled <strong>Boolean 装箱</strong>（provider 命令
 *       同型）——null 出站在场＝provider 缺省 true，若北向用原生 boolean 缺省会落成
 *       false（停用）语义反了。</li>
 *   <li>更新命令体（3 件）：{@code {baseUrl, apiKey, enabled}}——逐字镜像
 *       {@code ModelProviderUpdateCommand}。baseUrl 终态替换；apiKey 与 enabled
 *       <strong>缺省＝保持现值</strong>（两肢可空肢在 wire 请求体上保真——null 出站在场
 *       ＝保持现值，不臆造默认值；携现值掩码形同判保持＝round-trip 安全）。判定全在
 *       provider 侧，admin 零推导。</li>
 *   <li>留痕行（10 件）：{@code id}（TSID 十进制串）＋old&#42;/new&#42; 三件对（baseUrl/
 *       apiKeyMasked 掩码快照/enabled——oldEnabled/newEnabled 为 Boolean 装箱，创建痕
 *       old 全 null、删除痕 new 全 null）＋操作者两肢＋{@code operatedAt}。append-only
 *       跨行存活（行删除后留痕仍可读）。</li>
 *   <li>远端清单回执（4 件）：{@code providerKey}＋{@code pulledAt}＋
 *       {@code remoteModels[]}（modelId/name/status/domain/trackedTierKeys——空＝远端
 *       有库里无建议入库，非空＝已入库档位键清单；status 如实转述上游：无＝活跃、
 *       Shutdown/Retiring＝下架/退场中）＋{@code staleTiers[]}（tierKey/modelName/
 *       displayName/enabled——库内指向远端已无）。拉取无请求体（null body）。</li>
 *   <li>错误透传（PRJ 域码 4×1000＋序号）：PRJ_065→4065（404 通道不存在——单行/更新/
 *       删除/拉取/留痕五端点同语义）；PRJ_067→4067（409 键名已存在——创建）；PRJ_069→
 *       4069（400 baseUrl 必填——创建/更新）；PRJ_034→4034（400 操作者不能为空——
 *       创建/更新/删除写操作必留痕，admin 不拦）；PRJ_072→4072（409 仍被档位引用
 *       ——删除）；PRJ_076→4076（400 通道 key 空——拉取前置，无凭证不打上游）；
 *       PRJ_077→4077（502 上游不可用——拉取明确报错不静默）——fail-fast 原样透传，
 *       admin 不映射不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformModelProviderClientContractTest {

    /** 通道键（V44 种子两通道之一）：volcengine 主用（火山方舟）。 */
    private static final String PROVIDER_KEY = "volcengine";

    /**
     * aiplatform GET /api/backoffice/model-providers 的真实成功响应形状（V44 种子两通道
     * 代表面）：volcengine 主用（apiKey 已配置——掩码尾 4 位、最近写者 null＝种子写入）
     * ＋deepseek 官方备胎（apiKeyMasked null＝未配置）。data 为裸 List、按键序。
     */
    private static final String PROVIDERS_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "providerKey": "deepseek",
                  "baseUrl": "https://api.deepseek.com",
                  "apiKeyMasked": null,
                  "enabled": true,
                  "operatorId": null,
                  "operatorName": null,
                  "updatedAt": "2026-10-20T10:00:00"
                },
                {
                  "providerKey": "volcengine",
                  "baseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                  "apiKeyMasked": "****cd12",
                  "enabled": true,
                  "operatorId": null,
                  "operatorName": null,
                  "updatedAt": "2026-10-20T10:00:00"
                }
              ],
              "requestId": "req-mp1a2b",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/model-providers/{providerKey} 与 POST/PUT 回执的真实
     * 成功响应形状（运维中代表面）：apiKey 掩码尾 4 位＋最近写者两肢俱全（区别于种子行
     * null——掩码形照 provider 实值，admin 不二次处理）。
     */
    private static final String PROVIDER_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "providerKey": "volcengine",
                "baseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                "apiKeyMasked": "****cd12",
                "enabled": true,
                "operatorId": "700210",
                "operatorName": "运营·智能体管理员",
                "updatedAt": "2026-11-02T15:20:00"
              },
              "requestId": "req-mp3c4d",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/model-providers/{providerKey}/traces 的真实成功响应
     * 形状：裸 List、append-only 时间倒序（最近先）。三痕覆盖三形：首条＝换 key 更新痕
     * （old/new apiKey 掩码两侧各一面——尾 4 足以比对哪次换了哪把 key）；次条＝创建痕
     * （old 三件全 null——行此前不存在）；三条＝删除痕（new 三件全 null——行已不存在，
     * 留痕跨行存活）。oldEnabled/newEnabled 为 Boolean 装箱（创建痕 old 侧 null、删除痕
     * new 侧 null）。
     */
    private static final String TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3900000003333333",
                  "oldBaseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                  "oldApiKeyMasked": "****ab12",
                  "oldEnabled": true,
                  "newBaseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                  "newApiKeyMasked": "****cd12",
                  "newEnabled": true,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-02T15:20:00"
                },
                {
                  "id": "3900000002222222",
                  "oldBaseUrl": null,
                  "oldApiKeyMasked": null,
                  "oldEnabled": null,
                  "newBaseUrl": "https://ark.cn-beijing.volces.com/api/v3",
                  "newApiKeyMasked": "****ab12",
                  "newEnabled": true,
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-10-20T10:00:00"
                },
                {
                  "id": "3900000004444444",
                  "oldBaseUrl": "https://api.deepseek.com",
                  "oldApiKeyMasked": null,
                  "oldEnabled": true,
                  "newBaseUrl": null,
                  "newApiKeyMasked": null,
                  "newEnabled": null,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-01T09:30:00"
                }
              ],
              "requestId": "req-mp5e6f",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/model-providers/{providerKey}/remote-models 的真实
     * 成功响应形状（V44 种子实值＋BackofficeModelCatalogSeamTest 语义缩影）：远端五条
     * （modelId 序稳定）——已入库三条（trackedTierKeys 非空，V44 真实配对：flash→
     * deepseek-v4-flash-ga-260731／pro→deepseek-v4-pro-ga-260813／doubao-flagship→
     * doubao-seed-2-1-pro-260915）＋建议入库一条（trackedTierKeys 空——远端新档库里无）
     * ＋下架一条（status=Shutdown 如实转述，未入库）；库内过时一条（staleTiers——实际
     * 模型名 deepseek-v3-0324 不在远端清单〔对照语义的定义判据，provider 自家 seam 测试
     * 用不在清单的 m-gone-2512 同款〕）。
     */
    private static final String REMOTE_CATALOG_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "providerKey": "volcengine",
                "pulledAt": "2026-11-03T09:00:00",
                "remoteModels": [
                  {
                    "modelId": "deepseek-v4-flash-ga-260731",
                    "name": "DeepSeek V4 Flash",
                    "status": null,
                    "domain": "LLM",
                    "trackedTierKeys": ["flash"]
                  },
                  {
                    "modelId": "deepseek-v4-pro-ga-260813",
                    "name": "DeepSeek V4 Pro",
                    "status": null,
                    "domain": "LLM",
                    "trackedTierKeys": ["pro"]
                  },
                  {
                    "modelId": "doubao-seed-2-1-flash-250615",
                    "name": "Doubao Seed 2.1 Flash",
                    "status": null,
                    "domain": "LLM",
                    "trackedTierKeys": []
                  },
                  {
                    "modelId": "doubao-seed-2-1-pro-260915",
                    "name": "Doubao Seed 2.1 Pro",
                    "status": null,
                    "domain": "LLM",
                    "trackedTierKeys": ["doubao-flagship"]
                  },
                  {
                    "modelId": "doubao-seed-1-6-250615",
                    "name": "Doubao Seed 1.6",
                    "status": "Shutdown",
                    "domain": "LLM",
                    "trackedTierKeys": []
                  }
                ],
                "staleTiers": [
                  {
                    "tierKey": "deepseek-v3",
                    "modelName": "deepseek-v3-0324",
                    "displayName": "DeepSeek V3（退场版）",
                    "enabled": false
                  }
                ]
              },
              "requestId": "req-mp7g8h",
              "errors": null
            }
            """;

    /** 删除回执（ApiResponse<Void>——data null，信封即回执）：删成无面。 */
    private static final String VOID_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": null,
              "requestId": "req-mp9i0j",
              "errors": null
            }
            """;

    /** 通道不存在：HTTP 404 + 信封 code=4065（PRJ_065，域码 4——单行/更新/删除/拉取/留痕同语义）。 */
    private static final String PRJ_065_ERROR_ENVELOPE = """
            {
              "code": 4065,
              "message": "模型供应商通道不存在",
              "data": null,
              "requestId": "req-mpk1l2",
              "errors": null
            }
            """;

    /** 键名已存在：HTTP 409 + 信封 code=4067（PRJ_067——创建）。 */
    private static final String PRJ_067_ERROR_ENVELOPE = """
            {
              "code": 4067,
              "message": "模型供应商通道已存在（键名唯一——改既有行走 PUT）",
              "data": null,
              "requestId": "req-mpm3n4",
              "errors": null
            }
            """;

    /** baseUrl 必填：HTTP 400 + 信封 code=4069（PRJ_069——创建/更新）。 */
    private static final String PRJ_069_ERROR_ENVELOPE = """
            {
              "code": 4069,
              "message": "通道配置字段不完整（baseUrl 必填）",
              "data": null,
              "requestId": "req-mpo5p6",
              "errors": null
            }
            """;

    /** 操作者缺头（写操作必留痕——域面守卫拦）：HTTP 400 + 信封 code=4034（PRJ_034）。 */
    private static final String PRJ_034_ERROR_ENVELOPE = """
            {
              "code": 4034,
              "message": "操作者不能为空",
              "data": null,
              "requestId": "req-mpq7r8",
              "errors": null
            }
            """;

    /** 仍被档位引用（悬空引用拦截）：HTTP 409 + 信封 code=4072（PRJ_072——删除）。 */
    private static final String PRJ_072_ERROR_ENVELOPE = """
            {
              "code": 4072,
              "message": "该通道仍被模型档位引用，先删改这些档位行再删通道",
              "data": null,
              "requestId": "req-mps9t0",
              "errors": null
            }
            """;

    /** 通道 key 空（拉取前置——无凭证不打上游）：HTTP 400 + 信封 code=4076（PRJ_076）。 */
    private static final String PRJ_076_ERROR_ENVELOPE = """
            {
              "code": 4076,
              "message": "该通道未配置 apiKey，无法拉取模型清单：deepseek（先在通道配置填 key 后重试）",
              "data": null,
              "requestId": "req-mpu1v2",
              "errors": null
            }
            """;

    /** 上游不可用（明确报错不静默——带理由）：HTTP 502 + 信封 code=4077（PRJ_077）。 */
    private static final String PRJ_077_ERROR_ENVELOPE = """
            {
              "code": 4077,
              "message": "模型清单拉取失败：ConnectTimeout（上游不可用或响应异常——检查通道 baseUrl/key 与供应商可用性后重拉）",
              "data": null,
              "requestId": "req-mpw3x4",
              "errors": null
            }
            """;

    // ========== 清单读面（GET /model-providers：裸 List + apiKeyMasked 掩码形照实绑定）==========

    @Test
    void given_providersRead_when_listProviders_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDERS_ENVELOPE, wireUrl, new String[1]);

        appService.listProviders();

        // 出站 wire 形状逐字镜像 provider 契约：GET /api/backoffice/model-providers（裸顶层、
        // 无查询参数——通道行有界不造分页）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/model-providers");
    }

    @Test
    void given_providersEnvelope_when_listProviders_then_maskedKeyBoundAsIsOrNullForUnset() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDERS_ENVELOPE, new String[1], new String[1]);

        List<AiplatformModelProviderResponse> providers = appService.listProviders();

        // 信封正确拆开——证明 MODEL_PROVIDER_LIST_TYPEREF 按 ApiResponse<List<…>> 裸 List
        // 反序列化；两通道行（V44 种子：deepseek 备胎在前＝键序）
        assertThat(providers).hasSize(2);

        // 未配置通道：apiKeyMasked null 如实绑定（掩码单点 null＝未配置——admin 不二次处理）
        var deepseek = providers.get(0);
        assertThat(deepseek.providerKey()).isEqualTo("deepseek");
        assertThat(deepseek.baseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(deepseek.apiKeyMasked()).isNull();
        assertThat(deepseek.enabled()).isTrue();
        // 种子写入：最近写者两肢 null
        assertThat(deepseek.operatorId()).isNull();
        assertThat(deepseek.operatorName()).isNull();
        assertThat(deepseek.updatedAt()).isEqualTo("2026-10-20T10:00:00");

        // 已配置通道：apiKeyMasked 掩码形照 provider 实值绑定（尾 4 位——不二次处理不尝试解掩）
        var volcengine = providers.get(1);
        assertThat(volcengine.providerKey()).isEqualTo("volcengine");
        assertThat(volcengine.baseUrl()).isEqualTo("https://ark.cn-beijing.volces.com/api/v3");
        assertThat(volcengine.apiKeyMasked()).isEqualTo("****cd12");
        assertThat(volcengine.enabled()).isTrue();
    }

    // ========== 单行读面（GET /model-providers/{providerKey}：providerKey 路径参数透传）==========

    @Test
    void given_providerKey_when_getProvider_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, wireUrl, new String[1]);

        appService.getProvider(PROVIDER_KEY);

        // providerKey 为字符串路径参数透传（键名即协议路由前缀），admin 不解析不预校验——
        // 未知键由 provider 判 404 PRJ_065（spec #101）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-providers/" + PROVIDER_KEY);
    }

    @Test
    void given_providerEnvelope_when_getProvider_then_sevenFacetsBoundWithMaskedKey() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelProviderResponse provider = appService.getProvider(PROVIDER_KEY);

        // 信封正确拆开——证明 MODEL_PROVIDER_TYPEREF 按 ApiResponse<…> 单对象反序列化；
        // 七件全绑定（运维中代表面：掩码＋最近写者俱全）
        assertThat(provider.providerKey()).isEqualTo("volcengine");
        assertThat(provider.baseUrl()).isEqualTo("https://ark.cn-beijing.volces.com/api/v3");
        assertThat(provider.apiKeyMasked()).isEqualTo("****cd12");
        assertThat(provider.enabled()).isTrue();
        assertThat(provider.operatorId()).isEqualTo("700210");
        assertThat(provider.operatorName()).isEqualTo("运营·智能体管理员");
        assertThat(provider.updatedAt()).isEqualTo("2026-11-02T15:20:00");
    }

    // ========== 创建（POST /model-providers：四件命令体——apiKey 收全量、enabled 装箱）==========

    @Test
    void given_createCommand_when_createProvider_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, wireUrl, wireBody);

        appService.createProvider(new AiplatformModelProviderCreateCommand(
                "moonshot", "https://api.moonshot.cn/v1", "sk-real-secret-key-9f2c", true));

        // 出站 wire 形状逐字镜像 provider 契约：POST /api/backoffice/model-providers，
        // 命令体 {providerKey, baseUrl, apiKey, enabled} 四字段——逐字镜像
        // ModelProviderCreateCommand，无增删；apiKey 全量值原样出站（写面唯一明文入口）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/model-providers");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("providerKey", "baseUrl", "apiKey", "enabled");
        assertThat(body.path("providerKey").asText()).isEqualTo("moonshot");
        assertThat(body.path("baseUrl").asText()).isEqualTo("https://api.moonshot.cn/v1");
        assertThat(body.path("apiKey").asText()).isEqualTo("sk-real-secret-key-9f2c");
        assertThat(body.path("enabled").asBoolean()).isTrue();
    }

    @Test
    void given_createCommandWithDefaults_when_createProvider_then_nullsSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, new String[1], wireBody);

        // 暂不配置 key＋enabled 缺省（provider 缺省 true）：apiKey/enabled null 出站——
        // null 在场而非缺省（全局 Jackson 含 null，智能体配置开关先例）；enabled 装箱
        // Boolean 与 provider 命令同型——原生 boolean 会把缺省绑成 false 停用，语义反了
        appService.createProvider(new AiplatformModelProviderCreateCommand(
                "moonshot", "https://api.moonshot.cn/v1", null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("providerKey", "baseUrl", "apiKey", "enabled");
        assertThat(body.path("apiKey").isNull()).isTrue();
        assertThat(body.path("enabled").isNull()).isTrue();
    }

    @Test
    void given_createReceiptEnvelope_when_createProvider_then_receiptMasked() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelProviderResponse receipt = appService.createProvider(
                new AiplatformModelProviderCreateCommand(
                        "volcengine", "https://ark.cn-beijing.volces.com/api/v3",
                        "sk-real-secret-key-cd12", null));

        // 回执 apiKey 已掩码（尾 4 位忠实透传——provider 单点掩码，admin 不触碰明文不二次处理）
        assertThat(receipt.providerKey()).isEqualTo("volcengine");
        assertThat(receipt.apiKeyMasked()).isEqualTo("****cd12");
        assertThat(receipt.updatedAt()).isEqualTo("2026-11-02T15:20:00");
    }

    // ========== 更新（PUT /model-providers/{providerKey}：缺省肢＝保持现值，wire 保真）==========

    @Test
    void given_updateCommandWithExplicitLimbs_when_updateProvider_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, wireUrl, wireBody);

        appService.updateProvider(PROVIDER_KEY, new AiplatformModelProviderUpdateCommand(
                "https://ark.cn-beijing.volces.com/api/v3", "sk-new-secret-key-77e1", false));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/model-providers/{providerKey}，
        // 命令体 {baseUrl, apiKey, enabled} 三字段——逐字镜像 ModelProviderUpdateCommand；
        // 换 key 携全量新值（写面唯一明文入口）、显式 false＝有意停用
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-providers/" + PROVIDER_KEY);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("baseUrl", "apiKey", "enabled");
        assertThat(body.path("baseUrl").asText()).isEqualTo("https://ark.cn-beijing.volces.com/api/v3");
        assertThat(body.path("apiKey").asText()).isEqualTo("sk-new-secret-key-77e1");
        assertThat(body.path("enabled").asBoolean()).isFalse();
    }

    @Test
    void given_updateCommandWithDefaultedLimbs_when_updateProvider_then_nullsKeepCurrentOnWire() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, new String[1], wireBody);

        // 只换 baseUrl（换地址）：apiKey/enabled 缺省＝保持现值——两可空肢 null 出站在场
        // （全局 Jackson 含 null），不臆造默认值；provider 侧 null＝保持现值（apiKey 读面
        // 只出掩码、缺省清空＝读一次即丢 key；enabled 缺省翻转会把停用通道意外复启）
        appService.updateProvider(PROVIDER_KEY,
                new AiplatformModelProviderUpdateCommand("https://ark.cn-beijing.volces.com/api/v4", null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("baseUrl", "apiKey", "enabled");
        assertThat(body.path("baseUrl").asText()).isEqualTo("https://ark.cn-beijing.volces.com/api/v4");
        assertThat(body.path("apiKey").isNull()).isTrue();
        assertThat(body.path("enabled").isNull()).isTrue();
    }

    @Test
    void given_updateReceiptEnvelope_when_updateProvider_then_receiptSameShapeAsGet() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                PROVIDER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelProviderResponse receipt = appService.updateProvider(PROVIDER_KEY,
                new AiplatformModelProviderUpdateCommand("https://ark.cn-beijing.volces.com/api/v3",
                        null, null));

        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图）：七件全绑定
        assertThat(receipt.apiKeyMasked()).isEqualTo("****cd12");
        assertThat(receipt.operatorId()).isEqualTo("700210");
    }

    // ========== 删除（DELETE /model-providers/{providerKey}：DELETE 动词＋无请求体）==========

    @Test
    void given_providerKey_when_deleteProvider_then_wireUrlMirrorsProviderContractWithNoBody() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                VOID_ENVELOPE, wireUrl, wireBody);

        appService.deleteProvider(PROVIDER_KEY);

        // DELETE 动词唯一出口（openApiClient.delete 无 body 参数）、无请求体（wireBody 记 null
        // ——stub delete 分支钉死）；回执 ApiResponse<Void> data null 信封即回执
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-providers/" + PROVIDER_KEY);
        assertThat(wireBody[0]).isNull();
    }

    // ========== 远端清单拉取（POST /model-providers/{providerKey}/remote-models：无请求体＋对照结构）==========

    @Test
    void given_providerKey_when_pullRemoteModels_then_wireUrlMirrorsProviderContractWithNoBody() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                REMOTE_CATALOG_ENVELOPE, wireUrl, wireBody);

        appService.pullRemoteModels(PROVIDER_KEY);

        // 拉取动作非资源创建——provider 路由原形 POST，但无请求体（null body 发空 body，
        // 同重试归档先例）；只读探测不动库（纯读动作零留痕）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-providers/" + PROVIDER_KEY + "/remote-models");
        assertThat(wireBody[0]).isNull();
    }

    @Test
    void given_remoteCatalogEnvelope_when_pullRemoteModels_then_comparisonStructureBound() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                REMOTE_CATALOG_ENVELOPE, new String[1], new String[1]);

        AiplatformRemoteModelCatalogResponse catalog = appService.pullRemoteModels(PROVIDER_KEY);

        // 信封正确拆开——证明 REMOTE_MODEL_CATALOG_TYPEREF 按 ApiResponse<…> 单对象（含嵌套行）反序列化
        assertThat(catalog.providerKey()).isEqualTo("volcengine");
        assertThat(catalog.pulledAt()).isEqualTo("2026-11-03T09:00:00");

        // 远端五条（modelId 序稳定）：对照标记如实——已入库三条（trackedTierKeys 非空，V44
        // 真实配对 flash→v4-flash／pro→v4-pro／doubao-flagship→doubao-seed-2-1-pro）＋
        // 建议入库一条（空——远端新档库里无）＋下架一条（status=Shutdown 如实转述上游、未入库）
        assertThat(catalog.remoteModels()).hasSize(5);
        var tracked = catalog.remoteModels().get(0);
        assertThat(tracked.modelId()).isEqualTo("deepseek-v4-flash-ga-260731");
        assertThat(tracked.name()).isEqualTo("DeepSeek V4 Flash");
        assertThat(tracked.status()).isNull();
        assertThat(tracked.domain()).isEqualTo("LLM");
        assertThat(tracked.trackedTierKeys()).containsExactly("flash");
        // V44 真实配对第二条：pro → deepseek-v4-pro-ga-260813（档位键与实际模型名一一对应）
        assertThat(catalog.remoteModels().get(1).modelId()).isEqualTo("deepseek-v4-pro-ga-260813");
        assertThat(catalog.remoteModels().get(1).trackedTierKeys()).containsExactly("pro");

        var fresh = catalog.remoteModels().get(2);
        assertThat(fresh.modelId()).isEqualTo("doubao-seed-2-1-flash-250615");
        // trackedTierKeys 空＝远端有库里无（建议入库——勾选后走档位创建写口，modelName 填此 modelId）
        assertThat(fresh.trackedTierKeys()).isEmpty();

        assertThat(catalog.remoteModels().get(3).modelId()).isEqualTo("doubao-seed-2-1-pro-260915");
        assertThat(catalog.remoteModels().get(3).trackedTierKeys()).containsExactly("doubao-flagship");

        var shutdown = catalog.remoteModels().get(4);
        assertThat(shutdown.modelId()).isEqualTo("doubao-seed-1-6-250615");
        assertThat(shutdown.status()).isEqualTo("Shutdown");
        assertThat(shutdown.trackedTierKeys()).isEmpty();

        // 库内过时一条：实际模型名 deepseek-v3-0324 不在远端清单（对照语义的定义判据——
        // 提示过时，升版或改指向前处置；停用档位如实出 enabled=false）
        assertThat(catalog.staleTiers()).hasSize(1);
        var stale = catalog.staleTiers().get(0);
        assertThat(stale.tierKey()).isEqualTo("deepseek-v3");
        assertThat(stale.modelName()).isEqualTo("deepseek-v3-0324");
        assertThat(stale.displayName()).isEqualTo("DeepSeek V3（退场版）");
        assertThat(stale.enabled()).isFalse();
    }

    // ========== 留痕读面（GET /model-providers/{providerKey}/traces：掩码快照＋创建/删除痕 null 形）==========

    @Test
    void given_providerKey_when_listTraces_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.listTraces(PROVIDER_KEY);

        // 寻址同 GET 单行读面（providerKey 透传不预校验），留痕子路径 /traces 直挂
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-providers/" + PROVIDER_KEY + "/traces");
    }

    @Test
    void given_tracesEnvelope_when_listTraces_then_maskedSnapshotsBoundWithCreateDeleteNullShapes() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithStubTransport(
                TRACES_ENVELOPE, new String[1], new String[1]);

        List<AiplatformModelProviderTraceResponse> traces = appService.listTraces(PROVIDER_KEY);

        // 信封正确拆开——data 为裸 List（append-only 时间倒序，最近先）；三痕覆盖三形
        assertThat(traces).hasSize(3);

        // 首条最近（换 key 更新痕）：old/new apiKey 掩码两侧各一面——尾 4 足以比对哪次
        // 换了哪把 key（ab12→cd12），不作密钥找回面；baseUrl/enabled 同值在场
        var latest = traces.get(0);
        assertThat(latest.id()).isEqualTo("3900000003333333");
        assertThat(latest.oldApiKeyMasked()).isEqualTo("****ab12");
        assertThat(latest.oldEnabled()).isTrue();
        assertThat(latest.newApiKeyMasked()).isEqualTo("****cd12");
        assertThat(latest.newEnabled()).isTrue();
        assertThat(latest.oldBaseUrl()).isEqualTo(latest.newBaseUrl());
        assertThat(latest.operatorId()).isEqualTo("700210");
        assertThat(latest.operatorName()).isEqualTo("运营·智能体管理员");
        assertThat(latest.operatedAt()).isEqualTo("2026-11-02T15:20:00");

        // 次条（创建痕）：old 三件全 null——行此前不存在（创建即落痕 old 全 null）
        var created = traces.get(1);
        assertThat(created.id()).isEqualTo("3900000002222222");
        assertThat(created.oldBaseUrl()).isNull();
        assertThat(created.oldApiKeyMasked()).isNull();
        assertThat(created.oldEnabled()).isNull();
        assertThat(created.newBaseUrl()).isEqualTo("https://ark.cn-beijing.volces.com/api/v3");
        assertThat(created.newApiKeyMasked()).isEqualTo("****ab12");
        assertThat(created.newEnabled()).isTrue();
        // 留痕链衔接：创建痕 new key 掩码＝换 key 痕 old key 掩码（append-only 时间线诚实可查）
        assertThat(created.newApiKeyMasked()).isEqualTo(latest.oldApiKeyMasked());
        assertThat(created.operatorId()).isEqualTo("700100");
        assertThat(created.operatedAt()).isEqualTo("2026-10-20T10:00:00");

        // 三条（删除痕）：new 三件全 null——行已不存在；留痕跨行存活（append-only 历史事实
        // 不随行消失）；null 键侧掩码同 null（此前未配置）
        var deleted = traces.get(2);
        assertThat(deleted.id()).isEqualTo("3900000004444444");
        assertThat(deleted.oldBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(deleted.oldApiKeyMasked()).isNull();
        assertThat(deleted.oldEnabled()).isTrue();
        assertThat(deleted.newBaseUrl()).isNull();
        assertThat(deleted.newApiKeyMasked()).isNull();
        assertThat(deleted.newEnabled()).isNull();
        assertThat(deleted.operatedAt()).isEqualTo("2026-11-01T09:30:00");
    }

    // ========== 错误族透传（PRJ_065/067/069/034/072/076/077 数字业务码，不映射）==========

    @Test
    void given_prj072ErrorEnvelope_when_deleteProvider_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                409, PRJ_072_ERROR_ENVELOPE, new String[1]);

        // 悬空引用拦截（先删改挂其下的档位行）：HTTP 409 + 数字业务码 4072
        // （PRJ_072＝域码 4×1000＋72）+ provider message 原样——admin 不映射不翻译
        assertThatThrownBy(() -> appService.deleteProvider(PROVIDER_KEY))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4072);
                    assertThat(upstream.getMessage())
                            .isEqualTo("该通道仍被模型档位引用，先删改这些档位行再删通道");
                });
    }

    @Test
    void given_prj077ErrorEnvelope_when_pullRemoteModels_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                502, PRJ_077_ERROR_ENVELOPE, new String[1]);

        // 上游不可用明确报错不静默（带理由）：HTTP 502 + 数字业务码 4077
        // （PRJ_077——检查通道 baseUrl/key 与供应商可用性后重拉）
        assertThatThrownBy(() -> appService.pullRemoteModels(PROVIDER_KEY))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(502);
                    assertThat(upstream.envelopeCode()).isEqualTo(4077);
                    assertThat(upstream.getMessage())
                            .startsWith("模型清单拉取失败：ConnectTimeout");
                });
    }

    @Test
    void given_prj076ErrorEnvelope_when_pullRemoteModels_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                400, PRJ_076_ERROR_ENVELOPE, new String[1]);

        // 拉取前置（通道 key 空——无凭证不打上游）：HTTP 400 + 数字业务码 4076（PRJ_076）
        assertThatThrownBy(() -> appService.pullRemoteModels("deepseek"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4076);
                    assertThat(upstream.getMessage()).startsWith("该通道未配置 apiKey，无法拉取模型清单");
                });
    }

    @Test
    void given_prj065ErrorEnvelope_when_anyEndpoint_then_passThroughWithoutMapping() {
        // 未知键由 provider 裁决（admin 不预检 providerKey 字符串路径参数）：单行/更新/
        // 删除/拉取/留痕五端点同语义——HTTP 404 + 数字业务码 4065（PRJ_065）+ message 原样
        assertPrj065Passthrough("detail");
        assertPrj065Passthrough("update");
        assertPrj065Passthrough("delete");
        assertPrj065Passthrough("pullRemoteModels");
        assertPrj065Passthrough("traces");
    }

    /** 断言 PRJ_065 经指定端点族透传：HTTP 404＋数字业务码 4065＋message 原样。 */
    private static void assertPrj065Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                404, PRJ_065_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "detail" -> (Runnable) () -> appService.getProvider("moonshot");
            case "update" -> (Runnable) () -> appService.updateProvider("moonshot",
                    new AiplatformModelProviderUpdateCommand("https://api.moonshot.cn/v1", null, null));
            case "delete" -> (Runnable) () -> appService.deleteProvider("moonshot");
            case "pullRemoteModels" -> (Runnable) () -> appService.pullRemoteModels("moonshot");
            case "traces" -> (Runnable) () -> appService.listTraces("moonshot");
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_065（数字业务码 4065）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4065);
                    assertThat(upstream.getMessage()).isEqualTo("模型供应商通道不存在");
                });
    }

    @Test
    void given_prj067ErrorEnvelope_when_createProvider_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                409, PRJ_067_ERROR_ENVELOPE, new String[1]);

        // 键名已存在（键名唯一——改既有行走 PUT）：HTTP 409 + 数字业务码 4067（PRJ_067）
        assertThatThrownBy(() -> appService.createProvider(new AiplatformModelProviderCreateCommand(
                "volcengine", "https://ark.cn-beijing.volces.com/api/v3", null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4067);
                    assertThat(upstream.getMessage())
                            .isEqualTo("模型供应商通道已存在（键名唯一——改既有行走 PUT）");
                });
    }

    @Test
    void given_prj069ErrorEnvelope_when_createOrUpdate_then_passThroughWithoutMapping() {
        // baseUrl 必填（通道配置字段不完整）——创建/更新同语义：HTTP 400 + 数字业务码 4069
        assertPrj069Passthrough("create");
        assertPrj069Passthrough("update");
    }

    /** 断言 PRJ_069 经创建/更新透传：HTTP 400＋数字业务码 4069＋message 原样。 */
    private static void assertPrj069Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                400, PRJ_069_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "create" -> (Runnable) () -> appService.createProvider(
                    new AiplatformModelProviderCreateCommand("moonshot", null, null, null));
            case "update" -> (Runnable) () -> appService.updateProvider("volcengine",
                    new AiplatformModelProviderUpdateCommand(null, null, null));
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_069（数字业务码 4069）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4069);
                    assertThat(upstream.getMessage()).isEqualTo("通道配置字段不完整（baseUrl 必填）");
                });
    }

    @Test
    void given_prj034ErrorEnvelope_when_writeActions_then_passThroughWithoutMapping() {
        // 操作者缺头由 provider 域面守卫拦截（写操作必留痕，admin 不拦——透传断言即证据）：
        // 创建/更新/删除三写口同语义——HTTP 400 + 数字业务码 4034（PRJ_034）
        assertPrj034Passthrough("create");
        assertPrj034Passthrough("update");
        assertPrj034Passthrough("delete");
    }

    /** 断言 PRJ_034 经三写口透传：HTTP 400＋数字业务码 4034＋message 原样。 */
    private static void assertPrj034Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelProviderAppServiceWithErrorTransport(
                400, PRJ_034_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "create" -> (Runnable) () -> appService.createProvider(
                    new AiplatformModelProviderCreateCommand("moonshot", "https://api.moonshot.cn/v1", null, null));
            case "update" -> (Runnable) () -> appService.updateProvider("volcengine",
                    new AiplatformModelProviderUpdateCommand("https://api.moonshot.cn/v1", null, null));
            case "delete" -> (Runnable) () -> appService.deleteProvider("volcengine");
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_034（数字业务码 4034）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4034);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }
}
