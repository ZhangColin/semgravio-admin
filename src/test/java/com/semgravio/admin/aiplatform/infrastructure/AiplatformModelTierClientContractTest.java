package com.semgravio.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierTraceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 模型档位域（语义键 CRUD＋变更留痕）的 wire 契约测试（对接 aiplatform #367，
 * spec #364 T3，决策正本 ADR-0031，issue #111 B2——spec #101 B 批）。
 *
 * <p>契约事实（2026-11-03 对照 aiplatform 源码 {@code BackofficeModelTierController} /
 * {@code BackofficeModelTierResponse} / {@code BackofficeModelTierTraceResponse} /
 * {@code ModelTierCreateCommand} / {@code ModelTierUpdateCommand} /
 * {@code BackofficeModelChannelAppService} 档位方法 / V44/V45 种子核实）：</p>
 * <ul>
 *   <li>六端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}：清单/
 *       留痕 data 为<strong>裸 List</strong>（档位行/留痕行有界，List 直出不造分页）；
 *       单行/创建/更新回执同形（单对象）；删除回执 {@code ApiResponse<Void>}（data null
 *       ——信封即回执，同通道删除口形制）。</li>
 *   <li>读面行（8 件）：{@code tierKey}（语义键＝引用腿——绑定面〔三座/边缘点/路由强档〕
 *       绑定的就是它）＋{@code providerKey}（所属通道键＝模型串前缀腿）＋{@code modelName}
 *       （实际模型名——版本升级改此处指向、全部引用自动跟上）＋{@code displayName}
 *       （运营显示名，null＝回落语义键展示）＋{@code enabled}（原生 boolean 恒在场——
 *       停用＝既有绑定装配报错 PRJ_061、新绑定被即时校验拒绝 PRJ_075）＋最近写者两肢
 *       （null＝种子/迁移写入）＋{@code updatedAt}。</li>
 *   <li>创建命令体（5 件）：{@code {tierKey, providerKey, modelName, displayName, enabled}}
 *       ——逐字镜像 {@code ModelTierCreateCommand}。语义键随体（已存在 409 PRJ_068）；
 *       所属通道必须已有行（400 PRJ_071——先建通道再挂档位）；modelName 必填（400
 *       PRJ_070）；displayName 可空（＝回落语义键）；enabled <strong>Boolean 装箱</strong>
 *       （provider 命令同型）——null 出站在场＝provider 缺省 true，若北向用原生 boolean
 *       缺省会落成 false（停用）语义反了。创建即落痕（old 全 null）；建行即可被绑定。</li>
 *   <li>更新命令体（4 件）：{@code {providerKey, modelName, displayName, enabled}}——逐字
 *       镜像 {@code ModelTierUpdateCommand}。providerKey/modelName/displayName 终态全量替换
 *       （displayName null/空白＝清显示名回落语义键）；enabled <strong>缺省＝保持现值</strong>
 *       （可空肢在 wire 请求体上保真——null 出站在场＝保持现值，不臆造默认值；停用档换
 *       模型名/换通道不被意外复启，显式 true/false 才翻转）。判定全在 provider 侧，
 *       admin 零推导。</li>
 *   <li>留痕行（12 件）：{@code id}（TSID 十进制串）＋old&#42;/new&#42; 四件对（providerKey/
 *       modelName/displayName/enabled——oldEnabled/newEnabled 为 Boolean 装箱，创建痕 old
 *       全 null、删除痕 new 全 null；newDisplayName null＝本次清空回落语义键）＋操作者
 *       两肢＋{@code operatedAt}。append-only 跨行存活（行删除后留痕仍可读）；<strong>留痕
 *       读面不要求行存在</strong>——未知键空清单 200 非 404（空清单＝从未有行，行读面负责
 *       404 判别）。</li>
 *   <li>错误透传（PRJ 域码 4×1000＋序号）：PRJ_066→4066（404 档位不存在——单行/更新/删除
 *       三端点同语义；留痕读面不 404）；PRJ_068→4068（409 语义键已存在——创建）；PRJ_070→
 *       4070（400 字段不完整 providerKey/modelName 必填——创建/更新）；PRJ_071→4071
 *       （400 所属通道不存在——创建/更新，先建通道行）；PRJ_073→4073（409 仍被智能体
 *       运营配置绑定——删除，先改绑或清空绑定再删）；PRJ_034→4034（400 操作者不能为
 *       空——创建/更新/删除写操作必留痕，admin 不拦）——fail-fast 原样透传，admin 不映射
 *       不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformModelTierClientContractTest {

    /** 档位语义键（V44 种子三档之一）：flash 快模型档（三座 modelId 刷档后的现行引用腿）。 */
    private static final String TIER_KEY = "flash";

    /**
     * aiplatform GET /api/backoffice/model-tiers 的真实成功响应形状（V44/V45 种子三档
     * 代表面，按键序）：doubao-flagship（豆包旗舰）/flash（快模型档）/pro（强模型档）
     * ——均挂 volcengine 主用通道、enabled true、最近写者 null＝种子写入（V45 补
     * display_name）。data 为裸 List。
     */
    private static final String TIERS_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "tierKey": "doubao-flagship",
                  "providerKey": "volcengine",
                  "modelName": "doubao-seed-2-1-pro-260915",
                  "displayName": "豆包旗舰",
                  "enabled": true,
                  "operatorId": null,
                  "operatorName": null,
                  "updatedAt": "2026-10-20T10:00:00"
                },
                {
                  "tierKey": "flash",
                  "providerKey": "volcengine",
                  "modelName": "deepseek-v4-flash-ga-260731",
                  "displayName": "快模型档",
                  "enabled": true,
                  "operatorId": null,
                  "operatorName": null,
                  "updatedAt": "2026-10-20T10:00:00"
                },
                {
                  "tierKey": "pro",
                  "providerKey": "volcengine",
                  "modelName": "deepseek-v4-pro-ga-260813",
                  "displayName": "强模型档",
                  "enabled": true,
                  "operatorId": null,
                  "operatorName": null,
                  "updatedAt": "2026-10-20T10:00:00"
                }
              ],
              "requestId": "req-mt1a2b",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/model-tiers/{tierKey} 与 POST/PUT 回执的真实成功响应
     * 形状（运维中代表面）：flash 档升版后现值＋最近写者两肢俱全（区别于种子行 null）。
     */
    private static final String TIER_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "tierKey": "flash",
                "providerKey": "volcengine",
                "modelName": "deepseek-v4-flash-ga-261031",
                "displayName": "快模型档",
                "enabled": true,
                "operatorId": "700210",
                "operatorName": "运营·智能体管理员",
                "updatedAt": "2026-11-02T15:20:00"
              },
              "requestId": "req-mt3c4d",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/model-tiers/{tierKey}/traces 的真实成功响应形状：
     * 裸 List、append-only 时间倒序（最近先）。四痕覆盖四形且留痕链衔接：首条＝清显示名
     * 痕（newDisplayName null＝本次清空回落语义键——tier 特有 null 形）；次条＝升模型
     * 版本痕（modelName 260501→260731，换通道/显示名/启用态不动——语义键与模型版本解耦
     * 的核心语义）；三条＝创建痕（old 四件全 null——行此前不存在）；四条＝删除痕（new
     * 四件全 null——行已不存在，留痕跨行存活）。oldEnabled/newEnabled 为 Boolean 装箱。
     */
    private static final String TIER_TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3910000006666666",
                  "oldProviderKey": "volcengine",
                  "oldModelName": "deepseek-v4-flash-ga-260731",
                  "oldDisplayName": "快模型档",
                  "oldEnabled": true,
                  "newProviderKey": "volcengine",
                  "newModelName": "deepseek-v4-flash-ga-260731",
                  "newDisplayName": null,
                  "newEnabled": true,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-02T15:20:00"
                },
                {
                  "id": "3910000005555555",
                  "oldProviderKey": "volcengine",
                  "oldModelName": "deepseek-v4-flash-ga-260501",
                  "oldDisplayName": "快模型档",
                  "oldEnabled": true,
                  "newProviderKey": "volcengine",
                  "newModelName": "deepseek-v4-flash-ga-260731",
                  "newDisplayName": "快模型档",
                  "newEnabled": true,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-02T15:00:00"
                },
                {
                  "id": "3910000004444444",
                  "oldProviderKey": null,
                  "oldModelName": null,
                  "oldDisplayName": null,
                  "oldEnabled": null,
                  "newProviderKey": "volcengine",
                  "newModelName": "deepseek-v4-flash-ga-260501",
                  "newDisplayName": "快模型档",
                  "newEnabled": true,
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-10-20T10:00:00"
                },
                {
                  "id": "3910000007777777",
                  "oldProviderKey": "volcengine",
                  "oldModelName": "deepseek-v4-flash-ga-260731",
                  "oldDisplayName": null,
                  "oldEnabled": true,
                  "newProviderKey": null,
                  "newModelName": null,
                  "newDisplayName": null,
                  "newEnabled": null,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-01T09:30:00"
                }
              ],
              "requestId": "req-mt5e6f",
              "errors": null
            }
            """;

    /** 删除回执（ApiResponse<Void>——data null，信封即回执）：删成无面。 */
    private static final String VOID_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": null,
              "requestId": "req-mt7g8h",
              "errors": null
            }
            """;

    /** 档位不存在：HTTP 404 + 信封 code=4066（PRJ_066，域码 4——单行/更新/删除同语义；留痕读面不 404）。 */
    private static final String PRJ_066_ERROR_ENVELOPE = """
            {
              "code": 4066,
              "message": "模型档位不存在",
              "data": null,
              "requestId": "req-mti1j2",
              "errors": null
            }
            """;

    /** 语义键已存在：HTTP 409 + 信封 code=4068（PRJ_068——创建）。 */
    private static final String PRJ_068_ERROR_ENVELOPE = """
            {
              "code": 4068,
              "message": "模型档位已存在（语义键唯一——改既有档走 PUT）",
              "data": null,
              "requestId": "req-mtk3l4",
              "errors": null
            }
            """;

    /** 字段不完整：HTTP 400 + 信封 code=4070（PRJ_070——创建/更新，providerKey/modelName 必填）。 */
    private static final String PRJ_070_ERROR_ENVELOPE = """
            {
              "code": 4070,
              "message": "档位配置字段不完整（providerKey/modelName 必填）",
              "data": null,
              "requestId": "req-mtm5n6",
              "errors": null
            }
            """;

    /** 所属通道不存在：HTTP 400 + 信封 code=4071（PRJ_071——创建/更新，先建通道行）。 */
    private static final String PRJ_071_ERROR_ENVELOPE = """
            {
              "code": 4071,
              "message": "档位所属的模型供应商通道不存在：kimi（先建通道行）",
              "data": null,
              "requestId": "req-mto7p8",
              "errors": null
            }
            """;

    /** 仍被绑定引用（悬空绑定拦截）：HTTP 409 + 信封 code=4073（PRJ_073——删除）。 */
    private static final String PRJ_073_ERROR_ENVELOPE = """
            {
              "code": 4073,
              "message": "该档位仍被智能体运营配置绑定，先改绑或清空绑定再删（临时下线用停用不删行）",
              "data": null,
              "requestId": "req-mtq9r0",
              "errors": null
            }
            """;

    /** 操作者缺头（写操作必留痕——域面守卫拦）：HTTP 400 + 信封 code=4034（PRJ_034）。 */
    private static final String PRJ_034_ERROR_ENVELOPE = """
            {
              "code": 4034,
              "message": "操作者不能为空",
              "data": null,
              "requestId": "req-mts1t2",
              "errors": null
            }
            """;

    // ========== 清单读面（GET /model-tiers：裸 List + enabled=true 键集＝绑定下拉数据源）==========

    @Test
    void given_tiersRead_when_listTiers_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIERS_ENVELOPE, wireUrl, new String[1]);

        appService.listTiers();

        // 出站 wire 形状逐字镜像 provider 契约：GET /api/backoffice/model-tiers（裸顶层、
        // 无查询参数——档位行有界不造分页；键序归 provider 排序，admin 不重排）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/model-tiers");
    }

    @Test
    void given_tiersEnvelope_when_listTiers_then_tierRowShapeBoundWithSeedValues() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIERS_ENVELOPE, new String[1], new String[1]);

        List<AiplatformModelTierResponse> tiers = appService.listTiers();

        // 信封正确拆开——证明 MODEL_TIER_LIST_TYPEREF 按 ApiResponse<List<…>> 裸 List
        // 反序列化；三档行（V44/V45 种子：doubao-flagship/flash/pro 按键序）
        assertThat(tiers).hasSize(3);

        // 档位行形态五要素钉死：语义键/实际模型名/所属通道/显示名/启用态——
        // enabled=true 键集即绑定下拉数据源与改档写口即时校验放行值域
        var flagship = tiers.get(0);
        assertThat(flagship.tierKey()).isEqualTo("doubao-flagship");
        assertThat(flagship.providerKey()).isEqualTo("volcengine");
        assertThat(flagship.modelName()).isEqualTo("doubao-seed-2-1-pro-260915");
        assertThat(flagship.displayName()).isEqualTo("豆包旗舰");
        assertThat(flagship.enabled()).isTrue();

        // flash 快模型档：语义键与实际模型名解耦——版本升级改 modelName 指向、tierKey 不动
        var flash = tiers.get(1);
        assertThat(flash.tierKey()).isEqualTo("flash");
        assertThat(flash.modelName()).isEqualTo("deepseek-v4-flash-ga-260731");
        assertThat(flash.displayName()).isEqualTo("快模型档");
        assertThat(flash.enabled()).isTrue();
        // 种子写入：最近写者两肢 null
        assertThat(flash.operatorId()).isNull();
        assertThat(flash.operatorName()).isNull();
        assertThat(flash.updatedAt()).isEqualTo("2026-10-20T10:00:00");

        // pro 强模型档（路由强档的现行引用腿）
        assertThat(tiers.get(2).tierKey()).isEqualTo("pro");
        assertThat(tiers.get(2).modelName()).isEqualTo("deepseek-v4-pro-ga-260813");
        assertThat(tiers.get(2).displayName()).isEqualTo("强模型档");
    }

    // ========== 单行读面（GET /model-tiers/{tierKey}：tierKey 路径参数透传）==========

    @Test
    void given_tierKey_when_getTier_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, wireUrl, new String[1]);

        appService.getTier(TIER_KEY);

        // tierKey 为字符串路径参数透传（寻址键即绑定引用腿），admin 不解析不预校验——
        // 未知键由 provider 判 404 PRJ_066（spec #101）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-tiers/" + TIER_KEY);
    }

    @Test
    void given_tierEnvelope_when_getTier_then_eightFacetsBound() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelTierResponse tier = appService.getTier(TIER_KEY);

        // 信封正确拆开——证明 MODEL_TIER_TYPEREF 按 ApiResponse<…> 单对象反序列化；
        // 八件全绑定（运维中代表面：升版后现值＋最近写者俱全）
        assertThat(tier.tierKey()).isEqualTo("flash");
        assertThat(tier.providerKey()).isEqualTo("volcengine");
        assertThat(tier.modelName()).isEqualTo("deepseek-v4-flash-ga-261031");
        assertThat(tier.displayName()).isEqualTo("快模型档");
        assertThat(tier.enabled()).isTrue();
        assertThat(tier.operatorId()).isEqualTo("700210");
        assertThat(tier.operatorName()).isEqualTo("运营·智能体管理员");
        assertThat(tier.updatedAt()).isEqualTo("2026-11-02T15:20:00");
    }

    // ========== 创建（POST /model-tiers：五件命令体——语义键随体、displayName 可空、enabled 装箱）==========

    @Test
    void given_createCommand_when_createTier_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, wireUrl, wireBody);

        // 勾选入库复用档位创建写口（B1 remote-models 口径）：modelName 填所选条目 modelId、
        // 语义键人工命名——远端清单建议入库条的落库正路
        appService.createTier(new AiplatformModelTierCreateCommand(
                "doubao-lite", "volcengine", "doubao-seed-2-1-flash-250615", "豆包轻量", true));

        // 出站 wire 形状逐字镜像 provider 契约：POST /api/backoffice/model-tiers，
        // 命令体 {tierKey, providerKey, modelName, displayName, enabled} 五字段——逐字镜像
        // ModelTierCreateCommand，无增删
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/model-tiers");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("tierKey", "providerKey", "modelName", "displayName", "enabled");
        assertThat(body.path("tierKey").asText()).isEqualTo("doubao-lite");
        assertThat(body.path("providerKey").asText()).isEqualTo("volcengine");
        assertThat(body.path("modelName").asText()).isEqualTo("doubao-seed-2-1-flash-250615");
        assertThat(body.path("displayName").asText()).isEqualTo("豆包轻量");
        assertThat(body.path("enabled").asBoolean()).isTrue();
    }

    @Test
    void given_createCommandWithDefaults_when_createTier_then_nullsSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, new String[1], wireBody);

        // 暂不配显示名（＝回落语义键）＋enabled 缺省（provider 缺省 true）：displayName/
        // enabled null 出站——null 在场而非缺省（全局 Jackson 含 null，通道创建先例）；
        // enabled 装箱 Boolean 与 provider 命令同型——原生 boolean 会把缺省绑成 false 停用，
        // 语义反了
        appService.createTier(new AiplatformModelTierCreateCommand(
                "doubao-lite", "volcengine", "doubao-seed-2-1-flash-250615", null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("tierKey", "providerKey", "modelName", "displayName", "enabled");
        assertThat(body.path("displayName").isNull()).isTrue();
        assertThat(body.path("enabled").isNull()).isTrue();
    }

    @Test
    void given_createReceiptEnvelope_when_createTier_then_receiptBoundAsGet() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelTierResponse receipt = appService.createTier(new AiplatformModelTierCreateCommand(
                "flash", "volcengine", "deepseek-v4-flash-ga-261031", "快模型档", null));

        // 回执与 GET 同形（provider 落库后重读同一视图）：八件全绑定
        assertThat(receipt.tierKey()).isEqualTo("flash");
        assertThat(receipt.modelName()).isEqualTo("deepseek-v4-flash-ga-261031");
        assertThat(receipt.enabled()).isTrue();
        assertThat(receipt.operatorId()).isEqualTo("700210");
    }

    // ========== 更新（PUT /model-tiers/{tierKey}：终态替换＋enabled 缺省＝保持现值，wire 保真）==========

    @Test
    void given_updateCommandWithExplicitLimbs_when_updateTier_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, wireUrl, wireBody);

        // 升模型版本（方舟名带日期后缀、季度级更新）＝改档位行指向，全部引用自动跟上；
        // 显式 false＝有意停用
        appService.updateTier(TIER_KEY, new AiplatformModelTierUpdateCommand(
                "volcengine", "deepseek-v4-flash-ga-261031", "快模型档", false));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/model-tiers/{tierKey}，
        // 命令体 {providerKey, modelName, displayName, enabled} 四字段——逐字镜像
        // ModelTierUpdateCommand
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-tiers/" + TIER_KEY);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("providerKey", "modelName", "displayName", "enabled");
        assertThat(body.path("providerKey").asText()).isEqualTo("volcengine");
        assertThat(body.path("modelName").asText()).isEqualTo("deepseek-v4-flash-ga-261031");
        assertThat(body.path("displayName").asText()).isEqualTo("快模型档");
        assertThat(body.path("enabled").asBoolean()).isFalse();
    }

    @Test
    void given_updateCommandWithDefaultedLimbs_when_updateTier_then_displayNameClearsAndEnabledKeepsCurrentOnWire() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, new String[1], wireBody);

        // 只升模型版本：displayName null＝清显示名回落语义键（终态替换语义——null 即清空，
        // 区别于通道 apiKey 的「null＝保持现值」）；enabled 缺省＝保持现值——null 出站在场
        // （全局 Jackson 含 null），不臆造默认值；停用档升版不被意外复启
        appService.updateTier(TIER_KEY, new AiplatformModelTierUpdateCommand(
                "volcengine", "deepseek-v4-flash-ga-261031", null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("providerKey", "modelName", "displayName", "enabled");
        assertThat(body.path("providerKey").asText()).isEqualTo("volcengine");
        assertThat(body.path("modelName").asText()).isEqualTo("deepseek-v4-flash-ga-261031");
        assertThat(body.path("displayName").isNull()).isTrue();
        assertThat(body.path("enabled").isNull()).isTrue();
    }

    @Test
    void given_updateReceiptEnvelope_when_updateTier_then_receiptSameShapeAsGet() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_ENVELOPE, new String[1], new String[1]);

        AiplatformModelTierResponse receipt = appService.updateTier(TIER_KEY,
                new AiplatformModelTierUpdateCommand("volcengine",
                        "deepseek-v4-flash-ga-261031", "快模型档", null));

        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图）：八件全绑定
        assertThat(receipt.modelName()).isEqualTo("deepseek-v4-flash-ga-261031");
        assertThat(receipt.operatorName()).isEqualTo("运营·智能体管理员");
    }

    // ========== 删除（DELETE /model-tiers/{tierKey}：DELETE 动词＋无请求体）==========

    @Test
    void given_tierKey_when_deleteTier_then_wireUrlMirrorsProviderContractWithNoBody() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                VOID_ENVELOPE, wireUrl, wireBody);

        appService.deleteTier(TIER_KEY);

        // DELETE 动词唯一出口（openApiClient.delete 无 body 参数）、无请求体（wireBody 记 null
        // ——stub delete 分支钉死）；回执 ApiResponse<Void> data null 信封即回执
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-tiers/" + TIER_KEY);
        assertThat(wireBody[0]).isNull();
    }

    // ========== 留痕读面（GET /model-tiers/{tierKey}/traces：前后快照＋创建/删除痕 null 形）==========

    @Test
    void given_tierKey_when_listTierTraces_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.listTraces(TIER_KEY);

        // 寻址同 GET 单行读面（tierKey 透传不预校验），留痕子路径 /traces 直挂——
        // 留痕读面不要求行存在（未知键空清单 200 非 404，行读面负责 404 判别）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/model-tiers/" + TIER_KEY + "/traces");
    }

    @Test
    void given_tierTracesEnvelope_when_listTierTraces_then_fourFormsBoundWithCreateDeleteNullShapes() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithStubTransport(
                TIER_TRACES_ENVELOPE, new String[1], new String[1]);

        List<AiplatformModelTierTraceResponse> traces = appService.listTraces(TIER_KEY);

        // 信封正确拆开——data 为裸 List（append-only 时间倒序，最近先）；四痕覆盖四形
        assertThat(traces).hasSize(4);

        // 首条最近（清显示名痕）：newDisplayName null＝本次清空回落语义键——tier 特有 null 形
        //（终态替换语义，displayName null 即清空）；其余三肢两侧同值在场
        var cleared = traces.get(0);
        assertThat(cleared.id()).isEqualTo("3910000006666666");
        assertThat(cleared.oldDisplayName()).isEqualTo("快模型档");
        assertThat(cleared.newDisplayName()).isNull();
        assertThat(cleared.oldProviderKey()).isEqualTo(cleared.newProviderKey());
        assertThat(cleared.oldModelName()).isEqualTo(cleared.newModelName());
        assertThat(cleared.oldEnabled()).isTrue();
        assertThat(cleared.newEnabled()).isTrue();
        assertThat(cleared.operatorId()).isEqualTo("700210");
        assertThat(cleared.operatorName()).isEqualTo("运营·智能体管理员");
        assertThat(cleared.operatedAt()).isEqualTo("2026-11-02T15:20:00");

        // 次条（升模型版本痕）：modelName 260501→260731、providerKey/displayName/enabled
        // 两侧同值——语义键与模型版本解耦的核心证据（改一处指向、全部引用自动跟上）
        var upgraded = traces.get(1);
        assertThat(upgraded.id()).isEqualTo("3910000005555555");
        assertThat(upgraded.oldModelName()).isEqualTo("deepseek-v4-flash-ga-260501");
        assertThat(upgraded.newModelName()).isEqualTo("deepseek-v4-flash-ga-260731");
        assertThat(upgraded.oldProviderKey()).isEqualTo("volcengine");
        assertThat(upgraded.newProviderKey()).isEqualTo("volcengine");
        assertThat(upgraded.newDisplayName()).isEqualTo("快模型档");
        assertThat(upgraded.newEnabled()).isTrue();

        // 三条（创建痕）：old 四件全 null——行此前不存在（创建即落痕 old 全 null）；
        // 留痕链衔接：创建痕 new modelName＝升版痕 old modelName（append-only 时间线诚实可查）
        var created = traces.get(2);
        assertThat(created.id()).isEqualTo("3910000004444444");
        assertThat(created.oldProviderKey()).isNull();
        assertThat(created.oldModelName()).isNull();
        assertThat(created.oldDisplayName()).isNull();
        assertThat(created.oldEnabled()).isNull();
        assertThat(created.newProviderKey()).isEqualTo("volcengine");
        assertThat(created.newModelName()).isEqualTo("deepseek-v4-flash-ga-260501");
        assertThat(created.newDisplayName()).isEqualTo("快模型档");
        assertThat(created.newEnabled()).isTrue();
        assertThat(created.newModelName()).isEqualTo(upgraded.oldModelName());
        assertThat(created.operatorId()).isEqualTo("700100");
        assertThat(created.operatedAt()).isEqualTo("2026-10-20T10:00:00");

        // 四条（删除痕）：new 四件全 null——行已不存在；留痕跨行存活（append-only 历史事实
        // 不随行消失）；清显示名痕 new displayName null＝删除痕 old displayName null（链衔接）
        var deleted = traces.get(3);
        assertThat(deleted.id()).isEqualTo("3910000007777777");
        assertThat(deleted.oldProviderKey()).isEqualTo("volcengine");
        assertThat(deleted.oldModelName()).isEqualTo("deepseek-v4-flash-ga-260731");
        assertThat(deleted.oldDisplayName()).isNull();
        assertThat(deleted.oldEnabled()).isTrue();
        assertThat(deleted.newProviderKey()).isNull();
        assertThat(deleted.newModelName()).isNull();
        assertThat(deleted.newDisplayName()).isNull();
        assertThat(deleted.newEnabled()).isNull();
        assertThat(deleted.oldDisplayName()).isEqualTo(cleared.newDisplayName());
        assertThat(deleted.operatedAt()).isEqualTo("2026-11-01T09:30:00");
    }

    // ========== 错误族透传（PRJ_066/068/070/071/073/034 数字业务码，不映射）==========

    @Test
    void given_prj073ErrorEnvelope_when_deleteTier_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                409, PRJ_073_ERROR_ENVELOPE, new String[1]);

        // 悬空绑定拦截（先改绑或清空绑定再删——临时下线用停用不删行）：HTTP 409 +
        // 数字业务码 4073（PRJ_073＝域码 4×1000＋73）+ provider message 原样——admin
        // 不映射不翻译（绑定引用面在 provider 的 prj_agent_configs，BFF 不前置查询不代判）
        assertThatThrownBy(() -> appService.deleteTier(TIER_KEY))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4073);
                    assertThat(upstream.getMessage())
                            .isEqualTo("该档位仍被智能体运营配置绑定，先改绑或清空绑定再删（临时下线用停用不删行）");
                });
    }

    @Test
    void given_prj068ErrorEnvelope_when_createTier_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                409, PRJ_068_ERROR_ENVELOPE, new String[1]);

        // 语义键已存在（语义键唯一——改既有档走 PUT）：HTTP 409 + 数字业务码 4068（PRJ_068）
        assertThatThrownBy(() -> appService.createTier(new AiplatformModelTierCreateCommand(
                "flash", "volcengine", "deepseek-v4-flash-ga-261031", null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4068);
                    assertThat(upstream.getMessage())
                            .isEqualTo("模型档位已存在（语义键唯一——改既有档走 PUT）");
                });
    }

    @Test
    void given_prj071ErrorEnvelope_when_createOrUpdateTier_then_passThroughWithoutMapping() {
        // 所属通道不存在（FK 裸错的读面化——先建通道行再挂档位）：创建/更新同语义——
        // HTTP 400 + 数字业务码 4071（PRJ_071）
        assertPrj071Passthrough("create");
        assertPrj071Passthrough("update");
    }

    /** 断言 PRJ_071 经创建/更新透传：HTTP 400＋数字业务码 4071＋message 原样。 */
    private static void assertPrj071Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                400, PRJ_071_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "create" -> (Runnable) () -> appService.createTier(new AiplatformModelTierCreateCommand(
                    "kimi-lite", "kimi", "kimi-k2-261031", null, null));
            case "update" -> (Runnable) () -> appService.updateTier("flash",
                    new AiplatformModelTierUpdateCommand("kimi", "kimi-k2-261031", null, null));
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_071（数字业务码 4071）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4071);
                    assertThat(upstream.getMessage())
                            .isEqualTo("档位所属的模型供应商通道不存在：kimi（先建通道行）");
                });
    }

    @Test
    void given_prj070ErrorEnvelope_when_createOrUpdateTier_then_passThroughWithoutMapping() {
        // 字段不完整（providerKey/modelName 必填）——创建/更新同语义：HTTP 400 +
        // 数字业务码 4070（PRJ_070）
        assertPrj070Passthrough("create");
        assertPrj070Passthrough("update");
    }

    /** 断言 PRJ_070 经创建/更新透传：HTTP 400＋数字业务码 4070＋message 原样。 */
    private static void assertPrj070Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                400, PRJ_070_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "create" -> (Runnable) () -> appService.createTier(
                    new AiplatformModelTierCreateCommand("doubao-lite", "volcengine", null, null, null));
            case "update" -> (Runnable) () -> appService.updateTier("flash",
                    new AiplatformModelTierUpdateCommand(null, null, null, null));
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_070（数字业务码 4070）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4070);
                    assertThat(upstream.getMessage()).isEqualTo("档位配置字段不完整（providerKey/modelName 必填）");
                });
    }

    @Test
    void given_prj066ErrorEnvelope_when_tierRowEndpoints_then_passThroughWithoutMapping() {
        // 未知键由 provider 裁决（admin 不预检 tierKey 字符串路径参数）：单行/更新/删除
        // 三端点同语义——HTTP 404 + 数字业务码 4066（PRJ_066）+ message 原样。留痕读面
        // 不在族内（provider 行为：不要求行存在——未知键空清单 200 非 404，空清单＝
        // 从未有行，行读面负责 404 判别）
        assertPrj066Passthrough("detail");
        assertPrj066Passthrough("update");
        assertPrj066Passthrough("delete");
    }

    /** 断言 PRJ_066 经行族端点透传：HTTP 404＋数字业务码 4066＋message 原样。 */
    private static void assertPrj066Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                404, PRJ_066_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "detail" -> (Runnable) () -> appService.getTier("doubao-lite");
            case "update" -> (Runnable) () -> appService.updateTier("doubao-lite",
                    new AiplatformModelTierUpdateCommand("volcengine",
                            "doubao-seed-2-1-flash-250615", null, null));
            case "delete" -> (Runnable) () -> appService.deleteTier("doubao-lite");
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_066（数字业务码 4066）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4066);
                    assertThat(upstream.getMessage()).isEqualTo("模型档位不存在");
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
        var appService = AiplatformWireTestSupport.modelTierAppServiceWithErrorTransport(
                400, PRJ_034_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "create" -> (Runnable) () -> appService.createTier(new AiplatformModelTierCreateCommand(
                    "doubao-lite", "volcengine", "doubao-seed-2-1-flash-250615", null, null));
            case "update" -> (Runnable) () -> appService.updateTier("flash",
                    new AiplatformModelTierUpdateCommand("volcengine",
                            "deepseek-v4-flash-ga-261031", null, null));
            case "delete" -> (Runnable) () -> appService.deleteTier("flash");
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
