package com.aieducenter.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aieducenter.admin.aiplatform.application.AiplatformUpstreamException;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 智能体配置域（读面＋全量写＋变更留痕读面）的 wire 契约测试（对接 aiplatform
 * #251 技能线 T5，issue #84）。
 *
 * <p>契约事实（2026-09-23 对照 aiplatform 本地实例 swagger {@code /v3/api-docs} 实读＋
 * 源码 {@code BackofficeAgentConfigController} / {@code BackofficeAgentConfigResponse} /
 * {@code BackofficeAgentConfigTraceResponse} / {@code AgentConfigUpdateCommand} /
 * {@code AgentProfile} / {@code ProjectMessage} / {@code ErrorCodePrefix} 核实）：</p>
 * <ul>
 *   <li>三端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}：GET
 *       {@code /agent-configs/{agentKey}} 与 PUT 回执同形（单对象——provider 写入后重读同一
 *       视图）；GET {@code /agent-configs/{agentKey}/traces} data 为<strong>裸 List</strong>
 *       （append-only 时间倒序、有界——与技能更新留痕 List 直出同形，不造分页）。</li>
 *   <li>读面字段（13 件）：{@code agentKey}（main=主智能体／executor=run 执行体——展示名
 *       分别为「主智能体」/「run 执行体」，默认档位 deepseek-v4-flash/deepseek-v4-pro，
 *       {@code AgentProfile} 枚举定死）＋生效值两件＋覆盖标记两件＋枚举默认预览两件＋
 *       增强工具开关两件＋最近写者两肢＋{@code updatedAt}。覆盖标记与开关四件 provider
 *       <strong>恒在场</strong>（原生 boolean）；写者两肢与 updatedAt 可空（null＝从未配置
 *       ——无覆盖行＝全回落，覆盖标记 false）。</li>
 *   <li>PUT 命令体（4 件）：{@code {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled}}
 *       ——逐字镜像 {@code AgentConfigUpdateCommand}。覆盖两件 null/缺省/纯空白＝清空覆盖
 *       回落枚举默认；开关两件 <strong>Boolean 装箱</strong>（provider 命令同型）——null
 *       出站在场（全局 Jackson 含 null）＝provider 缺省 true（开），若北向用原生 boolean
 *       缺省会落成 false（关）语义反了。档位名不做白名单校验。</li>
 *   <li>留痕行（12 件）：{@code id}（TSID 十进制串）＋old&#42;/new&#42; 四件对（prompt/档位两件
 *       可空——null＝该侧即缺省回落，旧值快照即回滚写回依据；开关四件恒在场）＋操作者两肢
 *       ＋{@code operatedAt}（LocalDateTime ISO）。每痕＝一次实际变更（同值幂等重写不落痕）。
 *       </li>
 *   <li>错误透传（PRJ 域码 4×1000＋序号）：PRJ_033→4033（404 智能体不存在——运营配置面
 *       只有 main/executor 两座，classify/naming 等一次性判定不属本面；三端点同语义）；
 *       PRJ_034→4034（400 操作者不能为空——配置变更必留痕，缺头域面守卫拦，admin 不拦）
 *       ——fail-fast 原样透传，admin 不映射不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformAgentConfigClientContractTest {

    /** 智能体稳定键（AgentProfile 两把之一）：main=主智能体（默认档位 deepseek-v4-flash）。 */
    private static final String AGENT_KEY = "main";

    /**
     * aiplatform GET /api/backoffice/agent-configs/{agentKey} 的真实成功响应形状（有覆盖行
     * 代表面）：生效值＝库覆盖（覆盖标记 true×2）、生效值≠默认预览、开关一开一关、最近写者
     * 两肢＋写入时刻俱全。PUT 回执与本信封同形。
     */
    private static final String CONFIG_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "main",
                "agentName": "主智能体",
                "systemPrompt": "你是英语学习助手的主智能体：先判意图，再编排执行体。（库覆盖全文）",
                "modelId": "deepseek-v4-pro",
                "systemPromptOverridden": true,
                "modelIdOverridden": true,
                "defaultSystemPrompt": "你是主智能体。（枚举默认全文——清空覆盖即落此值）",
                "defaultModelId": "deepseek-v4-flash",
                "webSearchEnabled": true,
                "fetchUrlEnabled": false,
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "updatedAt": "2026-09-22T14:30:00"
              },
              "requestId": "req-a1c2g",
              "errors": null
            }
            """;

    /**
     * 无覆盖行代表面（executor=run 执行体，从未配置）：全回落——覆盖标记 false×2、生效值
     * ＝枚举默认（deepseek-v4-pro 为 executor 枚举默认档）、开关缺省 true×2、写者两肢与
     * updatedAt 全 null（如实出 JSON null，全局 Jackson 含 null）。
     */
    private static final String CONFIG_UNTOUCHED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "executor",
                "agentName": "run 执行体",
                "systemPrompt": "你是 run 执行体：按派单执行编码任务。（枚举默认全文）",
                "modelId": "deepseek-v4-pro",
                "systemPromptOverridden": false,
                "modelIdOverridden": false,
                "defaultSystemPrompt": "你是 run 执行体：按派单执行编码任务。（枚举默认全文）",
                "defaultModelId": "deepseek-v4-pro",
                "webSearchEnabled": true,
                "fetchUrlEnabled": true,
                "operatorId": null,
                "operatorName": null,
                "updatedAt": null
              },
              "requestId": "req-a3u4e",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/agent-configs/{agentKey}/traces 的真实成功响应形状：
     * 裸 List、append-only 时间倒序（最近先）。首条＝清空覆盖变更（new prompt/档位 null——
     * 本次清空回落枚举默认）；次条＝首次配置（old prompt/档位 null——此前即缺省回落，
     * 回滚即把 null 写回）。四件对 null 两侧各一面。
     */
    private static final String TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3900000002222222",
                  "oldSystemPrompt": "首次覆盖 prompt 全文",
                  "oldModelId": "deepseek-v4-pro",
                  "oldWebSearchEnabled": true,
                  "oldFetchUrlEnabled": false,
                  "newSystemPrompt": null,
                  "newModelId": null,
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": true,
                  "operatorId": "700200",
                  "operatorName": "运营·技能管理员",
                  "operatedAt": "2026-09-22T14:30:00"
                },
                {
                  "id": "3900000001111111",
                  "oldSystemPrompt": null,
                  "oldModelId": null,
                  "oldWebSearchEnabled": true,
                  "oldFetchUrlEnabled": true,
                  "newSystemPrompt": "首次覆盖 prompt 全文",
                  "newModelId": "deepseek-v4-pro",
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": false,
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-09-10T09:05:00"
                }
              ],
              "requestId": "req-a5t6r",
              "errors": null
            }
            """;

    /** 智能体不存在（未知键——含 classify/naming 等一次性判定）：HTTP 404 + 信封 code=4033（PRJ_033，域码 4）。 */
    private static final String PRJ_033_ERROR_ENVELOPE = """
            {
              "code": 4033,
              "message": "智能体不存在（运营配置面只有 main/executor 两座智能体）",
              "data": null,
              "requestId": "req-a7n3f",
              "errors": null
            }
            """;

    /** 操作者缺头（配置变更必留痕——域面守卫拦）：HTTP 400 + 信封 code=4034（PRJ_034，域码 4）。 */
    private static final String PRJ_034_ERROR_ENVELOPE = """
            {
              "code": 4034,
              "message": "操作者不能为空",
              "data": null,
              "requestId": "req-a9o4r",
              "errors": null
            }
            """;

    // ========== 配置读面（GET /agent-configs/{agentKey}：agentKey 字符串路径参数透传 + 13 件全绑定）==========

    @Test
    void given_agentKey_when_getConfig_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_ENVELOPE, wireUrl, new String[1]);

        appService.getConfig(AGENT_KEY);

        // agentKey 为字符串路径参数透传（main/executor 两把稳定键），admin 不解析不预校验——
        // 未知键由 provider 判 404 PRJ_033（spec #79）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-configs/" + AGENT_KEY);
    }

    @Test
    void given_configEnvelope_when_getConfig_then_effectiveFlagsDefaultsTogglesAndWriterBound() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_ENVELOPE, new String[1], new String[1]);

        AiplatformAgentConfigResponse config = appService.getConfig(AGENT_KEY);

        // 信封正确拆开——证明 AGENT_CONFIG_TYPEREF 按 ApiResponse<…> 单对象反序列化；
        // 键两肢（稳定键 + 展示名 provider 自带 *Name）
        assertThat(config.agentKey()).isEqualTo(AGENT_KEY);
        assertThat(config.agentName()).isEqualTo("主智能体");

        // 生效值＝库覆盖（装配实取值）＋覆盖标记 true×2——「现在跑的是覆盖」
        assertThat(config.systemPrompt()).startsWith("你是英语学习助手的主智能体");
        assertThat(config.modelId()).isEqualTo("deepseek-v4-pro");
        assertThat(config.systemPromptOverridden()).isTrue();
        assertThat(config.modelIdOverridden()).isTrue();

        // 枚举默认预览——清空覆盖即落此值（恢复默认前可先看落点）：与生效值不同源
        assertThat(config.defaultSystemPrompt()).contains("枚举默认全文");
        assertThat(config.defaultModelId()).isEqualTo("deepseek-v4-flash");

        // 增强工具开关存储态（一开一关代表面）
        assertThat(config.webSearchEnabled()).isTrue();
        assertThat(config.fetchUrlEnabled()).isFalse();

        // 最近写者两肢＋写入时刻（LocalDateTime ISO 反序列化）
        assertThat(config.operatorId()).isEqualTo("700200");
        assertThat(config.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(config.updatedAt()).isEqualTo("2026-09-22T14:30:00");
    }

    @Test
    void given_untouchedEnvelope_when_getConfig_then_allFallbackFacetsBound() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_UNTOUCHED_ENVELOPE, new String[1], new String[1]);

        AiplatformAgentConfigResponse config = appService.getConfig("executor");

        // 无覆盖行＝全回落（「现在跑的是默认」）：覆盖标记 false×2、生效值＝枚举默认
        // （executor 枚举默认档 deepseek-v4-pro——AgentProfile 定死）、开关缺省 true×2
        assertThat(config.agentKey()).isEqualTo("executor");
        assertThat(config.agentName()).isEqualTo("run 执行体");
        assertThat(config.systemPromptOverridden()).isFalse();
        assertThat(config.modelIdOverridden()).isFalse();
        assertThat(config.systemPrompt()).isEqualTo(config.defaultSystemPrompt());
        assertThat(config.modelId()).isEqualTo(config.defaultModelId());
        assertThat(config.webSearchEnabled()).isTrue();
        assertThat(config.fetchUrlEnabled()).isTrue();

        // 从未配置：写者两肢与 updatedAt 全 null 如实绑定（全局 Jackson 含 null——契约断言
        // isNull 而非缺省）
        assertThat(config.operatorId()).isNull();
        assertThat(config.operatorName()).isNull();
        assertThat(config.updatedAt()).isNull();
    }

    // ========== 全量写（PUT /agent-configs/{agentKey}：四件命令体 + 回执与 GET 同形）==========

    @Test
    void given_updateCommand_when_updateConfig_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_ENVELOPE, wireUrl, wireBody);

        appService.updateConfig(AGENT_KEY, new AiplatformAgentConfigUpdateCommand(
                "你是英语学习助手的主智能体：先判意图，再编排执行体。（库覆盖全文）",
                "deepseek-v4-pro", true, false));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/agent-configs/{agentKey}，
        // 命令体 {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled} 四字段——逐字镜像
        // AgentConfigUpdateCommand，无增删（PUT 全量语义：请求体即覆盖态终态）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-configs/" + AGENT_KEY);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("systemPrompt", "modelId", "webSearchEnabled", "fetchUrlEnabled");
        assertThat(body.path("systemPrompt").asText()).contains("先判意图");
        assertThat(body.path("modelId").asText()).isEqualTo("deepseek-v4-pro");
        assertThat(body.path("webSearchEnabled").asBoolean()).isTrue();
        assertThat(body.path("fetchUrlEnabled").asBoolean()).isFalse();
    }

    @Test
    void given_clearingCommand_when_updateConfig_then_nullsSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_UNTOUCHED_ENVELOPE, new String[1], wireBody);

        // 清空覆盖＋开关缺省：四件全 null 出站——null 在场而非缺省（全局 Jackson 含 null，
        // 技能安装 excludeDirs 先例）；provider 侧 null＝覆盖清空回落枚举默认 / 开关缺省 true
        // （装箱 Boolean 与 provider 命令同型——原生 boolean 会把缺省绑成 false 关，语义反了）
        appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand(null, null, null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("systemPrompt", "modelId", "webSearchEnabled", "fetchUrlEnabled");
        assertThat(body.path("systemPrompt").isNull()).isTrue();
        assertThat(body.path("modelId").isNull()).isTrue();
        assertThat(body.path("webSearchEnabled").isNull()).isTrue();
        assertThat(body.path("fetchUrlEnabled").isNull()).isTrue();
    }

    @Test
    void given_updateReceiptEnvelope_when_updateConfig_then_receiptSameShapeAsGet() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_ENVELOPE, new String[1], new String[1]);

        AiplatformAgentConfigResponse receipt = appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand("新 prompt", "deepseek-v4-pro", true, false));

        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图，admin 同形镜像）：
        // 13 件全绑定（覆盖行代表面）
        assertThat(receipt.agentKey()).isEqualTo(AGENT_KEY);
        assertThat(receipt.systemPromptOverridden()).isTrue();
        assertThat(receipt.defaultModelId()).isEqualTo("deepseek-v4-flash");
        assertThat(receipt.fetchUrlEnabled()).isFalse();
        assertThat(receipt.operatorId()).isEqualTo("700200");
        assertThat(receipt.updatedAt()).isEqualTo("2026-09-22T14:30:00");
    }

    // ========== 变更留痕读面（GET /agent-configs/{agentKey}/traces：old*/new* 四件对 + 时间倒序裸 List）==========

    @Test
    void given_agentKey_when_listTraces_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.listTraces(AGENT_KEY);

        // 寻址同 GET 配置读面（agentKey 透传不预校验），留痕子路径 /traces 直挂
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/agent-configs/" + AGENT_KEY + "/traces");
    }

    @Test
    void given_tracesEnvelope_when_listTraces_then_oldNewPairsBoundInDescOrder() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                TRACES_ENVELOPE, new String[1], new String[1]);

        List<AiplatformAgentConfigTraceResponse> traces = appService.listTraces(AGENT_KEY);

        // 信封正确拆开——data 为裸 List（append-only 时间倒序，最近先；同技能更新留痕
        // List 直出同形）
        assertThat(traces).hasSize(2);

        // 首条最近（清空覆盖变更）：new prompt/档位 null＝本次清空回落枚举默认（如实在场），
        // old 快照＝此前覆盖值（回滚写回依据）＋开关四件＋操作者两肢＋时刻
        var latest = traces.get(0);
        assertThat(latest.id()).isEqualTo("3900000002222222");
        assertThat(latest.oldSystemPrompt()).isEqualTo("首次覆盖 prompt 全文");
        assertThat(latest.oldModelId()).isEqualTo("deepseek-v4-pro");
        assertThat(latest.oldWebSearchEnabled()).isTrue();
        assertThat(latest.oldFetchUrlEnabled()).isFalse();
        assertThat(latest.newSystemPrompt()).isNull();
        assertThat(latest.newModelId()).isNull();
        assertThat(latest.newWebSearchEnabled()).isTrue();
        assertThat(latest.newFetchUrlEnabled()).isTrue();
        assertThat(latest.operatorId()).isEqualTo("700200");
        assertThat(latest.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(latest.operatedAt()).isEqualTo("2026-09-22T14:30:00");

        // 次条更早（首次配置）：old prompt/档位 null＝此前即缺省回落（回滚即把 null 写回）——
        // 留痕链衔接：次条 new＝首条 old（append-only 时间线诚实可查）
        var earlier = traces.get(1);
        assertThat(earlier.id()).isEqualTo("3900000001111111");
        assertThat(earlier.oldSystemPrompt()).isNull();
        assertThat(earlier.oldModelId()).isNull();
        assertThat(earlier.newSystemPrompt()).isEqualTo(latest.oldSystemPrompt());
        assertThat(earlier.newModelId()).isEqualTo(latest.oldModelId());
        assertThat(earlier.newFetchUrlEnabled()).isFalse();
        assertThat(earlier.operatorId()).isEqualTo("700100");
        assertThat(earlier.operatedAt()).isEqualTo("2026-09-10T09:05:00");
    }

    // ========== 错误族透传（PRJ_033/034 数字业务码，不映射）==========

    @Test
    void given_prj033ErrorEnvelope_when_anyEndpoint_then_passThroughWithoutMapping() {
        // 未知键由 provider 裁决（admin 不预检 agentKey 字符串路径参数）：读面/全量写/留痕读面
        // 三端点同语义——HTTP 404 + 数字业务码 4033（PRJ_033＝域码 4×1000＋33）+ provider
        // message 原样
        assertPrj033Passthrough("config");
        assertPrj033Passthrough("update");
        assertPrj033Passthrough("traces");
    }

    /** 断言 PRJ_033 经指定端点族透传：HTTP 404＋数字业务码 4033＋message 原样。 */
    private static void assertPrj033Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                404, PRJ_033_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "config" -> (Runnable) () -> appService.getConfig("classify");
            case "update" -> (Runnable) () -> appService.updateConfig("classify",
                    new AiplatformAgentConfigUpdateCommand(null, null, null, null));
            case "traces" -> (Runnable) () -> appService.listTraces("classify");
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_033（数字业务码 4033）应经 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4033);
                    assertThat(upstream.getMessage())
                            .isEqualTo("智能体不存在（运营配置面只有 main/executor 两座智能体）");
                });
    }

    @Test
    void given_prj034ErrorEnvelope_when_updateConfig_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                400, PRJ_034_ERROR_ENVELOPE, new String[1]);

        // 操作者缺头由 provider 域面守卫拦截（配置变更必留痕，admin 不拦——透传断言即证据）：
        // HTTP 400 + 数字业务码 4034（PRJ_034＝域码 4×1000＋34）+ provider message 原样
        assertThatThrownBy(() -> appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand("新 prompt", null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4034);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }
}
