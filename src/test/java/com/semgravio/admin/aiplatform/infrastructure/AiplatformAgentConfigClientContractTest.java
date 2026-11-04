package com.semgravio.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformEdgeModelUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformEdgeModelConfigResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 智能体配置域（读面＋全量写＋变更留痕读面＋边缘调用点三端点）的 wire 契约测试
 * （对接 aiplatform #251 技能线 T5，issue #84；#366 边缘四点收编＋#367 档位键即时校验，
 * issue #112 spec #101 B 批 B3）。
 *
 * <p>契约事实（2026-09-23 对照 aiplatform 本地实例 swagger {@code /v3/api-docs} 实读＋
 * 源码 {@code BackofficeAgentConfigController} / {@code BackofficeAgentConfigResponse} /
 * {@code BackofficeAgentConfigTraceResponse} / {@code AgentConfigUpdateCommand} /
 * {@code AgentProfile} / {@code ProjectMessage} / {@code ErrorCodePrefix} 核实；
 * B3 增量 2026-11-03 对照 provider develop（V44/V45 落地后）复核）：</p>
 * <ul>
 *   <li>三端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}：GET
 *       {@code /agent-configs/{agentKey}} 与 PUT 回执同形（单对象——provider 写入后重读同一
 *       视图）；GET {@code /agent-configs/{agentKey}/traces} data 为<strong>裸 List</strong>
 *       （append-only 时间倒序、有界——与技能更新留痕 List 直出同形，不造分页）。</li>
 *   <li>读面字段（16 件镜像）：{@code agentKey}（main=主智能体／executor=run 执行体／
 *       designer=设计执行体——展示名分别为「主智能体」/「run 执行体」/「设计执行体」，默认
 *       档位键 flash/flash/pro，{@code AgentProfile} 枚举定死；边缘调用点走 /edge/{edgeKey}
 *       三座端点对边缘键 404 PRJ_033）＋生效值两件＋覆盖标记两件＋枚举默认预览两件＋增强
 *       工具开关两件＋<strong>路由强档三件</strong>（routingStrongModelId 生效值／
 *       routingStrongModelIdOverridden／defaultRoutingStrongModelId——主档/强档同范式，B3
 *       忠实跟随）＋最近写者两肢＋{@code updatedAt}。modelId 语义＝<strong>档位键</strong>
 *       （ADR-0031 语义键如 flash/pro/doubao-flagship，V44 存量已刷档——非裸模型名）。覆盖
 *       标记与开关四件 provider 恒在场（原生 boolean）；写者两肢与 updatedAt 可空（null＝
 *       从未配置——无覆盖行＝全回落，覆盖标记 false）。provider 读面另带路由开关/两阈值六件
 *       （#354，routingEnabled±/routingFileCountThreshold±/routingSliceFeatureThreshold±），
 *       B3 票面只点名 routingStrongModelId——六件不镜像（Jackson 容错忽略，读面不呈现），
 *       fixtures 如实携带以钉住容错事实。</li>
 *   <li>PUT 命令体（5 件）：{@code {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled,
 *       routingStrongModelId}}——逐字镜像 {@code AgentConfigUpdateCommand}（B3 面）。覆盖三件
 *       null/缺省/纯空白＝清空覆盖回落枚举/代码默认；开关两件 <strong>Boolean 装箱</strong>
 *       （provider 命令同型）——null 出站在场（全局 Jackson 含 null）＝provider 缺省 true
 *       （开），若北向用原生 boolean 缺省会落成 false（关）语义反了。<strong>档位键即时
 *       校验</strong>（#367）：modelId 与 routingStrongModelId 非空时必须在模型档位面
 *       （/model-tiers）有行且启用——缺行 400 PRJ_074、停用 400 PRJ_075（错值写不进去，
 *       不再靠留痕回滚兜底；null＝清空回落缺省不校验）。provider 命令另带路由开关/两阈值
 *       三件（routingEnabled/routingFileCountThreshold/routingSliceFeatureThreshold——
 *       #354），B3 票面不镜像：admin 命令体三肢不出站＝provider null＝缺省态（开/60/4）
 *       ——BFF 是唯一写面、三件从无可设处，结构上恒缺省态（票面字面口径，2026-11-03 拍板）。</li>
 *   <li>留痕行（14 件镜像）：{@code id}（TSID 十进制串）＋old&#42;/new&#42; 对（prompt/档位/
 *       <strong>路由强档</strong>三件可空——null＝该侧即缺省回落，旧值快照即回滚写回依据
 *       〔含强档——读面可改就必须可回滚〕；开关四件恒在场）＋操作者两肢＋{@code operatedAt}
 *       （LocalDateTime ISO）。每痕＝一次实际变更（同值幂等重写不落痕）。provider 留痕另带
 *       路由开关/两阈值六件（#354），同读面口径不镜像（fixtures 如实携带钉容错）。</li>
 *   <li>边缘调用点三端点（#366 同表收编——值面仅模型档位键，无 prompt/工具开关/路由件）：
 *       GET/PUT {@code /agent-configs/edge/{edgeKey}} 与 GET {@code /edge/{edgeKey}/traces}。
 *       寻址稳定键四把（{@code AgentEdgePoint} 实值）：naming=项目取名（code 11）／
 *       compaction=上下文压缩（code 12）／classification=入口分类（code 13）／precheck=技能
 *       预检（code 14），缺省档位键全部 flash（轻调用不烧重档）。读面 8 件（agentKey/
 *       agentName/modelId/modelIdOverridden/defaultModelId/operatorId/operatorName/updatedAt
 *       ——字段名对齐三座面，admin 侧同一渲染件可复用）；PUT 命令体 1 件 {@code {modelId}}
 *       （窄形拒绝误带——null/缺省/纯空白＝清空覆盖回落代码缺省 flash；即时校验同三座口径
 *       PRJ_074/075）；留痕行与三座同形（边缘行档位列之外快照件恒缺省态：prompt null、开关
 *       true、路由件缺省态）。</li>
 *   <li>错误透传（PRJ 域码 4×1000＋序号）：PRJ_033→4033（404 智能体不存在——运营配置面
 *       只有 main/executor/designer 三座，边缘键走 /edge/{edgeKey}；三座三端点同语义）；
 *       PRJ_064→4064（404 边缘调用点不存在——档位配置面只有 naming/compaction/
 *       classification/precheck 四点；边缘三端点同语义）；PRJ_034→4034（400 操作者不能为
 *       空——配置变更必留痕，缺头域面守卫拦，admin 不拦）；PRJ_074→4074（400 绑定的模型
 *       档位不存在——先在模型档位面建行）；PRJ_075→4075（400 绑定的模型档位已停用——启用
 *       该档位或改绑其他档位）——fail-fast 原样透传，admin 不映射不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformAgentConfigClientContractTest {

    /** 智能体稳定键（AgentProfile 三把之一）：main=主智能体（枚举默认档位键 flash）。 */
    private static final String AGENT_KEY = "main";

    /**
     * aiplatform GET /api/backoffice/agent-configs/{agentKey} 的真实成功响应形状（有覆盖行
     * 代表面）：生效值＝库覆盖（覆盖标记 true×2＋路由强档覆盖标记 true）、生效值≠默认预览、
     * 开关一开一关、最近写者两肢＋写入时刻俱全。PUT 回执与本信封同形。modelId/强档取 V44
     * 档位键实值（flash/pro/doubao-flagship——存量已刷档，非裸模型名）。路由开关/两阈值六件
     * （#354）provider 在场、B3 票面不镜像——如实携带钉住「Jackson 容错忽略不炸读面」事实。
     */
    private static final String CONFIG_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "main",
                "agentName": "主智能体",
                "systemPrompt": "你是英语学习助手的主智能体：先判意图，再编排执行体。（库覆盖全文）",
                "modelId": "pro",
                "systemPromptOverridden": true,
                "modelIdOverridden": true,
                "defaultSystemPrompt": "你是主智能体。（枚举默认全文——清空覆盖即落此值）",
                "defaultModelId": "flash",
                "webSearchEnabled": true,
                "fetchUrlEnabled": false,
                "routingEnabled": true,
                "routingStrongModelId": "doubao-flagship",
                "routingStrongModelIdOverridden": true,
                "defaultRoutingStrongModelId": "pro",
                "routingFileCountThreshold": 60,
                "routingFileCountThresholdOverridden": false,
                "defaultRoutingFileCountThreshold": 60,
                "routingSliceFeatureThreshold": 4,
                "routingSliceFeatureThresholdOverridden": false,
                "defaultRoutingSliceFeatureThreshold": 4,
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "updatedAt": "2026-09-22T14:30:00"
              },
              "requestId": "req-a1c2g",
              "errors": null
            }
            """;

    /**
     * 无覆盖行代表面（executor=run 执行体，从未配置）：全回落——覆盖标记 false×2＋路由强档
     * 覆盖标记 false、生效值＝枚举默认（flash 为 executor 枚举默认档位键、pro 为代码缺省
     * 强档——#341 起 executor 缺省档 pro→flash）、开关缺省 true×2、写者两肢与 updatedAt
     * 全 null（如实出 JSON null，全局 Jackson 含 null）。路由开关/两阈值六件同恒缺省态。
     */
    private static final String CONFIG_UNTOUCHED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "executor",
                "agentName": "run 执行体",
                "systemPrompt": "你是 run 执行体：按派单执行编码任务。（枚举默认全文）",
                "modelId": "flash",
                "systemPromptOverridden": false,
                "modelIdOverridden": false,
                "defaultSystemPrompt": "你是 run 执行体：按派单执行编码任务。（枚举默认全文）",
                "defaultModelId": "flash",
                "webSearchEnabled": true,
                "fetchUrlEnabled": true,
                "routingEnabled": true,
                "routingStrongModelId": "pro",
                "routingStrongModelIdOverridden": false,
                "defaultRoutingStrongModelId": "pro",
                "routingFileCountThreshold": 60,
                "routingFileCountThresholdOverridden": false,
                "defaultRoutingFileCountThreshold": 60,
                "routingSliceFeatureThreshold": 4,
                "routingSliceFeatureThresholdOverridden": false,
                "defaultRoutingSliceFeatureThreshold": 4,
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
     * 裸 List、append-only 时间倒序（最近先）。首条＝清空覆盖变更（new prompt/档位/强档
     * null——本次清空回落枚举/代码默认）；次条＝首次配置（old 三件 null——此前即缺省回落，
     * 回滚即把 null 写回）。可空三件对 null 两侧各一面；留痕链衔接（次条 new＝首条 old）。
     * 路由开关/两阈值六件（#354）边缘三座同口径不镜像——如实携带钉容错（阈值 null＝该侧
     * 即代码缺省 60/4）。
     */
    private static final String TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3900000002222222",
                  "oldSystemPrompt": "首次覆盖 prompt 全文",
                  "oldModelId": "pro",
                  "oldWebSearchEnabled": true,
                  "oldFetchUrlEnabled": false,
                  "oldRoutingEnabled": true,
                  "oldRoutingStrongModelId": "doubao-flagship",
                  "oldRoutingFileCountThreshold": null,
                  "oldRoutingSliceFeatureThreshold": null,
                  "newSystemPrompt": null,
                  "newModelId": null,
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": true,
                  "newRoutingEnabled": true,
                  "newRoutingStrongModelId": null,
                  "newRoutingFileCountThreshold": null,
                  "newRoutingSliceFeatureThreshold": null,
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
                  "oldRoutingEnabled": true,
                  "oldRoutingStrongModelId": null,
                  "oldRoutingFileCountThreshold": null,
                  "oldRoutingSliceFeatureThreshold": null,
                  "newSystemPrompt": "首次覆盖 prompt 全文",
                  "newModelId": "pro",
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": false,
                  "newRoutingEnabled": true,
                  "newRoutingStrongModelId": "doubao-flagship",
                  "newRoutingFileCountThreshold": null,
                  "newRoutingSliceFeatureThreshold": null,
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-09-10T09:05:00"
                }
              ],
              "requestId": "req-a5t6r",
              "errors": null
            }
            """;

    /** 智能体不存在（未知键——三座端点对边缘键同此；边缘键走 /edge/{edgeKey}）：HTTP 404 + 信封 code=4033（PRJ_033，域码 4）。 */
    private static final String PRJ_033_ERROR_ENVELOPE = """
            {
              "code": 4033,
              "message": "智能体不存在（运营配置面只有 main/executor/designer 三座智能体）",
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

    /**
     * aiplatform GET /api/backoffice/agent-configs/edge/{edgeKey} 的真实成功响应形状（有覆盖行
     * 代表面）：naming=项目取名（{@code AgentEdgePoint} 实值 code 11）、生效档位键＝库覆盖
     * pro（覆盖标记 true、生效值≠代码缺省）、代码缺省预览 flash（#366 各点缺省档位键——轻
     * 调用不烧重档）、最近写者两肢＋写入时刻俱全。值面仅档位键一件——无 prompt/工具开关/
     * 路由件（字段名对齐三座面，admin 侧同一渲染件可复用）。PUT 回执与本信封同形。
     */
    private static final String EDGE_CONFIG_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "naming",
                "agentName": "项目取名",
                "modelId": "pro",
                "modelIdOverridden": true,
                "defaultModelId": "flash",
                "operatorId": "700210",
                "operatorName": "运营·智能体管理员",
                "updatedAt": "2026-11-01T10:15:00"
              },
              "requestId": "req-e1b2c",
              "errors": null
            }
            """;

    /**
     * 无覆盖行代表面（classification=入口分类，从未配置）：全回落——覆盖标记 false、生效档位
     * 键＝代码缺省 flash（各点缺省同档）、写者两肢与 updatedAt 全 null（如实出 JSON null）。
     */
    private static final String EDGE_CONFIG_UNTOUCHED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "agentKey": "classification",
                "agentName": "入口分类",
                "modelId": "flash",
                "modelIdOverridden": false,
                "defaultModelId": "flash",
                "operatorId": null,
                "operatorName": null,
                "updatedAt": null
              },
              "requestId": "req-e3d4e",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/agent-configs/edge/{edgeKey}/traces 的真实成功响应形状：
     * 裸 List、append-only 时间倒序（最近先）——快照列与三座同形（同 DTO 复用），边缘行
     * 档位列之外的快照件恒缺省态：prompt null、开关 true、路由件缺省态（强档 null——
     * provider 边缘写把非档位覆盖列结构性归缺省）。首条＝升档变更（flash→pro）；次条＝首次
     * 配置（old null）。
     */
    private static final String EDGE_TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3900000003333333",
                  "oldSystemPrompt": null,
                  "oldModelId": "flash",
                  "oldWebSearchEnabled": true,
                  "oldFetchUrlEnabled": true,
                  "oldRoutingEnabled": true,
                  "oldRoutingStrongModelId": null,
                  "oldRoutingFileCountThreshold": null,
                  "oldRoutingSliceFeatureThreshold": null,
                  "newSystemPrompt": null,
                  "newModelId": "pro",
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": true,
                  "newRoutingEnabled": true,
                  "newRoutingStrongModelId": null,
                  "newRoutingFileCountThreshold": null,
                  "newRoutingSliceFeatureThreshold": null,
                  "operatorId": "700210",
                  "operatorName": "运营·智能体管理员",
                  "operatedAt": "2026-11-01T10:15:00"
                },
                {
                  "id": "3900000003222222",
                  "oldSystemPrompt": null,
                  "oldModelId": null,
                  "oldWebSearchEnabled": true,
                  "oldFetchUrlEnabled": true,
                  "oldRoutingEnabled": true,
                  "oldRoutingStrongModelId": null,
                  "oldRoutingFileCountThreshold": null,
                  "oldRoutingSliceFeatureThreshold": null,
                  "newSystemPrompt": null,
                  "newModelId": "flash",
                  "newWebSearchEnabled": true,
                  "newFetchUrlEnabled": true,
                  "newRoutingEnabled": true,
                  "newRoutingStrongModelId": null,
                  "newRoutingFileCountThreshold": null,
                  "newRoutingSliceFeatureThreshold": null,
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-10-22T16:40:00"
                }
              ],
              "requestId": "req-e5f6g",
              "errors": null
            }
            """;

    /** 边缘调用点不存在（未知键——档位配置面只有四点）：HTTP 404 + 信封 code=4064（PRJ_064，域码 4）。 */
    private static final String PRJ_064_ERROR_ENVELOPE = """
            {
              "code": 4064,
              "message": "边缘调用点不存在（档位配置面只有 naming/compaction/classification/precheck 四点）",
              "data": null,
              "requestId": "req-e7h8i",
              "errors": null
            }
            """;

    /** 绑定的模型档位不存在（缺行——先在模型档位面建行）：HTTP 400 + 信封 code=4074（PRJ_074，域码 4）。 */
    private static final String PRJ_074_ERROR_ENVELOPE = """
            {
              "code": 4074,
              "message": "绑定的模型档位不存在：doubao-ultra（先在模型档位面建行）",
              "data": null,
              "requestId": "req-e9j0k",
              "errors": null
            }
            """;

    /** 绑定的模型档位已停用（启用该档位或改绑其他档位）：HTTP 400 + 信封 code=4075（PRJ_075，域码 4）。 */
    private static final String PRJ_075_ERROR_ENVELOPE = """
            {
              "code": 4075,
              "message": "绑定的模型档位已停用：doubao-flagship（启用该档位或改绑其他档位）",
              "data": null,
              "requestId": "req-e1k2l",
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

        // 生效值＝库覆盖（装配实取值）＋覆盖标记 true×2——「现在跑的是覆盖」；modelId 语义＝
        // 档位键（ADR-0031 语义键，V44 存量已刷档——pro 为 flash 之上的运营覆盖）
        assertThat(config.systemPrompt()).startsWith("你是英语学习助手的主智能体");
        assertThat(config.modelId()).isEqualTo("pro");
        assertThat(config.systemPromptOverridden()).isTrue();
        assertThat(config.modelIdOverridden()).isTrue();

        // 枚举默认预览——清空覆盖即落此值（恢复默认前可先看落点）：与生效值不同源
        assertThat(config.defaultSystemPrompt()).contains("枚举默认全文");
        assertThat(config.defaultModelId()).isEqualTo("flash");

        // 路由强档三件（B3 忠实跟随——主档/强档同范式）：生效值＝库覆盖 doubao-flagship、
        // 覆盖标记 true、代码缺省预览 pro（对照知来源；清空覆盖即落 pro）；provider 另带的
        // 路由开关/两阈值六件不镜像——fixture 在场而读面不呈现（Jackson 容错忽略不炸）
        assertThat(config.routingStrongModelId()).isEqualTo("doubao-flagship");
        assertThat(config.routingStrongModelIdOverridden()).isTrue();
        assertThat(config.defaultRoutingStrongModelId()).isEqualTo("pro");

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

        // 无覆盖行＝全回落（「现在跑的是默认」）：覆盖标记 false×2＋路由强档 false、生效值
        // ＝枚举默认（executor 枚举默认档位键 flash——#341 起由 pro 改 flash；强档代码缺省
        // pro）、开关缺省 true×2
        assertThat(config.agentKey()).isEqualTo("executor");
        assertThat(config.agentName()).isEqualTo("run 执行体");
        assertThat(config.systemPromptOverridden()).isFalse();
        assertThat(config.modelIdOverridden()).isFalse();
        assertThat(config.systemPrompt()).isEqualTo(config.defaultSystemPrompt());
        assertThat(config.modelId()).isEqualTo(config.defaultModelId());
        assertThat(config.routingStrongModelIdOverridden()).isFalse();
        assertThat(config.routingStrongModelId())
                .isEqualTo(config.defaultRoutingStrongModelId()).isEqualTo("pro");
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
                "pro", true, false, "doubao-flagship"));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/agent-configs/{agentKey}，
        // 命令体 {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled, routingStrongModelId}
        // 五字段——逐字镜像 AgentConfigUpdateCommand（B3 面），无增删（PUT 全量语义：请求体
        // 即覆盖态终态）；modelId/routingStrongModelId 均档位键——非空须在模型档位面有行且
        // 启用（缺行 400 PRJ_074/停用 400 PRJ_075，provider 即时校验裁决，admin 不预判）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-configs/" + AGENT_KEY);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("systemPrompt", "modelId", "webSearchEnabled",
                        "fetchUrlEnabled", "routingStrongModelId");
        assertThat(body.path("systemPrompt").asText()).contains("先判意图");
        assertThat(body.path("modelId").asText()).isEqualTo("pro");
        assertThat(body.path("webSearchEnabled").asBoolean()).isTrue();
        assertThat(body.path("fetchUrlEnabled").asBoolean()).isFalse();
        assertThat(body.path("routingStrongModelId").asText()).isEqualTo("doubao-flagship");
    }

    @Test
    void given_clearingCommand_when_updateConfig_then_nullsSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_UNTOUCHED_ENVELOPE, new String[1], wireBody);

        // 清空覆盖＋开关缺省＋强档清空：五件全 null 出站——null 在场而非缺省（全局 Jackson
        // 含 null，技能安装 excludeDirs 先例）；provider 侧 null＝覆盖清空回落枚举/代码默认 /
        // 开关缺省 true（装箱 Boolean 与 provider 命令同型——原生 boolean 会把缺省绑成
        // false 关，语义反了；强档 null＝清空回落代码缺省 pro，即时校验对 null 放行）
        appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand(null, null, null, null, null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable()
                .containsExactly("systemPrompt", "modelId", "webSearchEnabled",
                        "fetchUrlEnabled", "routingStrongModelId");
        assertThat(body.path("systemPrompt").isNull()).isTrue();
        assertThat(body.path("modelId").isNull()).isTrue();
        assertThat(body.path("webSearchEnabled").isNull()).isTrue();
        assertThat(body.path("fetchUrlEnabled").isNull()).isTrue();
        assertThat(body.path("routingStrongModelId").isNull()).isTrue();
    }

    @Test
    void given_updateReceiptEnvelope_when_updateConfig_then_receiptSameShapeAsGet() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                CONFIG_ENVELOPE, new String[1], new String[1]);

        AiplatformAgentConfigResponse receipt = appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand("新 prompt", "pro", true, false, "doubao-flagship"));

        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图，admin 同形镜像）：
        // 16 件全绑定（覆盖行代表面，含路由强档三件）
        assertThat(receipt.agentKey()).isEqualTo(AGENT_KEY);
        assertThat(receipt.systemPromptOverridden()).isTrue();
        assertThat(receipt.defaultModelId()).isEqualTo("flash");
        assertThat(receipt.fetchUrlEnabled()).isFalse();
        assertThat(receipt.routingStrongModelId()).isEqualTo("doubao-flagship");
        assertThat(receipt.routingStrongModelIdOverridden()).isTrue();
        assertThat(receipt.defaultRoutingStrongModelId()).isEqualTo("pro");
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

        // 首条最近（清空覆盖变更）：new prompt/档位/强档 null＝本次清空回落枚举/代码默认
        // （如实在场），old 快照＝此前覆盖值（回滚写回依据——含强档：读面可改就必须可回滚）
        // ＋开关四件＋操作者两肢＋时刻
        var latest = traces.get(0);
        assertThat(latest.id()).isEqualTo("3900000002222222");
        assertThat(latest.oldSystemPrompt()).isEqualTo("首次覆盖 prompt 全文");
        assertThat(latest.oldModelId()).isEqualTo("pro");
        assertThat(latest.oldWebSearchEnabled()).isTrue();
        assertThat(latest.oldFetchUrlEnabled()).isFalse();
        assertThat(latest.oldRoutingStrongModelId()).isEqualTo("doubao-flagship");
        assertThat(latest.newSystemPrompt()).isNull();
        assertThat(latest.newModelId()).isNull();
        assertThat(latest.newWebSearchEnabled()).isTrue();
        assertThat(latest.newFetchUrlEnabled()).isTrue();
        assertThat(latest.newRoutingStrongModelId()).isNull();
        assertThat(latest.operatorId()).isEqualTo("700200");
        assertThat(latest.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(latest.operatedAt()).isEqualTo("2026-09-22T14:30:00");

        // 次条更早（首次配置）：old prompt/档位/强档 null＝此前即缺省回落（回滚即把 null
        // 写回）——留痕链衔接：次条 new＝首条 old（append-only 时间线诚实可查，强档同链）
        var earlier = traces.get(1);
        assertThat(earlier.id()).isEqualTo("3900000001111111");
        assertThat(earlier.oldSystemPrompt()).isNull();
        assertThat(earlier.oldModelId()).isNull();
        assertThat(earlier.oldRoutingStrongModelId()).isNull();
        assertThat(earlier.newSystemPrompt()).isEqualTo(latest.oldSystemPrompt());
        assertThat(earlier.newModelId()).isEqualTo(latest.oldModelId());
        assertThat(earlier.newRoutingStrongModelId()).isEqualTo(latest.oldRoutingStrongModelId());
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
                    new AiplatformAgentConfigUpdateCommand(null, null, null, null, null));
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
                            .isEqualTo("智能体不存在（运营配置面只有 main/executor/designer 三座智能体）");
                });
    }

    @Test
    void given_prj034ErrorEnvelope_when_updateConfig_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                400, PRJ_034_ERROR_ENVELOPE, new String[1]);

        // 操作者缺头由 provider 域面守卫拦截（配置变更必留痕，admin 不拦——透传断言即证据）：
        // HTTP 400 + 数字业务码 4034（PRJ_034＝域码 4×1000＋34）+ provider message 原样
        assertThatThrownBy(() -> appService.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand("新 prompt", null, null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4034);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }

    // ========== 边缘调用点档位读面（GET /agent-configs/edge/{edgeKey}：值面仅档位键，8 件全绑定）==========

    @Test
    void given_edgeKey_when_getEdgeModelConfig_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_ENVELOPE, wireUrl, new String[1]);

        appService.getEdgeModelConfig("naming");

        // edgeKey 为字符串路径参数透传（四把稳定键 naming/compaction/classification/precheck），
        // 子路径 /edge/ 直挂三座端点同前缀——admin 不解析不预校验，未知键由 provider 判
        // 404 PRJ_064（#366）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-configs/edge/naming");
    }

    @Test
    void given_edgeConfigEnvelope_when_getEdgeModelConfig_then_eightFacetsBound() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_ENVELOPE, new String[1], new String[1]);

        var config = appService.getEdgeModelConfig("naming");

        // 值面仅档位键一件（#366 同表收编——无 prompt/工具开关/路由件）：键两肢＋生效值＝
        // 库覆盖 pro＋覆盖标记 true＋代码缺省预览 flash（AgentEdgePoint 定死——轻调用不烧
        // 重档）＋最近写者两肢＋写入时刻（LocalDateTime ISO 反序列化）
        assertThat(config.agentKey()).isEqualTo("naming");
        assertThat(config.agentName()).isEqualTo("项目取名");
        assertThat(config.modelId()).isEqualTo("pro");
        assertThat(config.modelIdOverridden()).isTrue();
        assertThat(config.defaultModelId()).isEqualTo("flash");
        assertThat(config.operatorId()).isEqualTo("700210");
        assertThat(config.operatorName()).isEqualTo("运营·智能体管理员");
        assertThat(config.updatedAt()).isEqualTo("2026-11-01T10:15:00");
    }

    @Test
    void given_untouchedEdgeEnvelope_when_getEdgeModelConfig_then_allFallbackFacetsBound() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_UNTOUCHED_ENVELOPE, new String[1], new String[1]);

        var config = appService.getEdgeModelConfig("classification");

        // 无覆盖行＝全回落：覆盖标记 false、生效档位键＝代码缺省 flash、写者两肢与 updatedAt
        // 全 null 如实绑定（全局 Jackson 含 null——契约断言 isNull 而非缺省）
        assertThat(config.agentKey()).isEqualTo("classification");
        assertThat(config.agentName()).isEqualTo("入口分类");
        assertThat(config.modelIdOverridden()).isFalse();
        assertThat(config.modelId()).isEqualTo(config.defaultModelId()).isEqualTo("flash");
        assertThat(config.operatorId()).isNull();
        assertThat(config.operatorName()).isNull();
        assertThat(config.updatedAt()).isNull();
    }

    // ========== 边缘调用点档位写（PUT /agent-configs/edge/{edgeKey}：单字段窄命令体 + 回执与 GET 同形）==========

    @Test
    void given_edgeUpdateCommand_when_updateEdgeModel_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_ENVELOPE, wireUrl, wireBody);

        appService.updateEdgeModel("naming", new AiplatformEdgeModelUpdateCommand("pro"));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/agent-configs/edge/{edgeKey}，
        // 命令体 {modelId} 单字段——窄形拒绝误带（边缘点无 prompt/开关/路由语义，provider
        // EdgeModelUpdateCommand 同形）；档位键即时校验同三座口径（缺行 400 PRJ_074、停用
        // 400 PRJ_075——provider 裁决，admin 不预判）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-configs/edge/naming");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("modelId");
        assertThat(body.path("modelId").asText()).isEqualTo("pro");
    }

    @Test
    void given_clearingEdgeCommand_when_updateEdgeModel_then_nullSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_UNTOUCHED_ENVELOPE, new String[1], wireBody);

        // 清空覆盖：modelId null 出站——null 在场而非缺省（全局 Jackson 含 null）；provider
        // 侧 null/缺省/纯空白＝清空覆盖回落代码缺省 flash（即时校验对 null 放行）
        appService.updateEdgeModel("naming", new AiplatformEdgeModelUpdateCommand(null));

        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("modelId");
        assertThat(body.path("modelId").isNull()).isTrue();
    }

    @Test
    void given_edgeReceiptEnvelope_when_updateEdgeModel_then_receiptSameShapeAsGet() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_CONFIG_ENVELOPE, new String[1], new String[1]);

        var receipt = appService.updateEdgeModel("naming", new AiplatformEdgeModelUpdateCommand("pro"));

        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图，admin 同形镜像）
        assertThat(receipt.agentKey()).isEqualTo("naming");
        assertThat(receipt.modelId()).isEqualTo("pro");
        assertThat(receipt.modelIdOverridden()).isTrue();
        assertThat(receipt.defaultModelId()).isEqualTo("flash");
        assertThat(receipt.operatorId()).isEqualTo("700210");
    }

    // ========== 边缘调用点留痕读面（GET /agent-configs/edge/{edgeKey}/traces：快照列与三座同形）==========

    @Test
    void given_edgeKey_when_listEdgeModelTraces_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.listEdgeModelTraces("naming");

        // 寻址同 edge 读面（edgeKey 透传不预校验），留痕子路径 /traces 直挂——快照列与三座
        // 同形（provider 同 DTO 复用，边缘行非档位快照件恒缺省态）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/agent-configs/edge/naming/traces");
    }

    @Test
    void given_edgeTracesEnvelope_when_listEdgeModelTraces_then_tierColumnOnlyChangesMapped() {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithStubTransport(
                EDGE_TRACES_ENVELOPE, new String[1], new String[1]);

        List<AiplatformAgentConfigTraceResponse> traces = appService.listEdgeModelTraces("naming");

        // data 为裸 List（append-only 时间倒序，最近先）；快照列与三座同形——只有档位列在动
        // （首条升档 flash→pro），prompt/强档 null＝边缘行结构性缺省态、开关恒 true
        assertThat(traces).hasSize(2);

        var latest = traces.get(0);
        assertThat(latest.id()).isEqualTo("3900000003333333");
        assertThat(latest.oldModelId()).isEqualTo("flash");
        assertThat(latest.newModelId()).isEqualTo("pro");
        assertThat(latest.oldSystemPrompt()).isNull();
        assertThat(latest.newSystemPrompt()).isNull();
        assertThat(latest.oldRoutingStrongModelId()).isNull();
        assertThat(latest.newRoutingStrongModelId()).isNull();
        assertThat(latest.oldWebSearchEnabled()).isTrue();
        assertThat(latest.newFetchUrlEnabled()).isTrue();
        assertThat(latest.operatorId()).isEqualTo("700210");
        assertThat(latest.operatedAt()).isEqualTo("2026-11-01T10:15:00");

        // 次条更早（首次配置）：old 档位 null＝此前即代码缺省 flash（回滚即把 null 写回）——
        // 留痕链衔接：次条 new flash＝首条 old（升档前值）
        var earlier = traces.get(1);
        assertThat(earlier.oldModelId()).isNull();
        assertThat(earlier.newModelId()).isEqualTo(latest.oldModelId());
        assertThat(earlier.operatorId()).isEqualTo("700100");
        assertThat(earlier.operatedAt()).isEqualTo("2026-10-22T16:40:00");
    }

    // ========== 错误族透传（PRJ_064 边缘点不存在 / PRJ_074 缺行 / PRJ_075 停用——数字业务码，不映射）==========

    @Test
    void given_prj064ErrorEnvelope_when_anyEdgeEndpoint_then_passThroughWithoutMapping() {
        // 未知键由 provider 裁决（admin 不预检 edgeKey 字符串路径参数）：边缘三端点同语义
        // ——HTTP 404 + 数字业务码 4064（PRJ_064＝域码 4×1000＋64）+ provider message 原样
        assertPrj064Passthrough("config");
        assertPrj064Passthrough("update");
        assertPrj064Passthrough("traces");
    }

    /** 断言 PRJ_064 经指定边缘端点族透传：HTTP 404＋数字业务码 4064＋message 原样。 */
    private static void assertPrj064Passthrough(String endpoint) {
        var appService = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                404, PRJ_064_ERROR_ENVELOPE, new String[1]);

        var invocation = switch (endpoint) {
            case "config" -> (Runnable) () -> appService.getEdgeModelConfig("summarize");
            case "update" -> (Runnable) () -> appService.updateEdgeModel("summarize",
                    new AiplatformEdgeModelUpdateCommand(null));
            case "traces" -> (Runnable) () -> appService.listEdgeModelTraces("summarize");
            default -> throw new IllegalArgumentException("未知端点族: " + endpoint);
        };

        assertThatThrownBy(invocation::run)
                .as("PRJ_064（数字业务码 4064）应经边缘 %s 透传", endpoint)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4064);
                    assertThat(upstream.getMessage())
                            .isEqualTo("边缘调用点不存在（档位配置面只有 naming/compaction/classification/precheck 四点）");
                });
    }

    @Test
    void given_prj074Or075ErrorEnvelope_when_tierBindingRejected_then_passThroughWithoutMapping() {
        // 档位键即时校验（#367）的负例契约：错值档位键写不进去（不再靠留痕回滚兜底）——
        // 三座全量写（modelId/routingStrongModelId 两腿同校验）与边缘写同口径。
        // 缺行（PRJ_074＝4074）：HTTP 400 + provider message 原样
        var missingTier = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                400, PRJ_074_ERROR_ENVELOPE, new String[1]);
        assertThatThrownBy(() -> missingTier.updateConfig(AGENT_KEY,
                new AiplatformAgentConfigUpdateCommand("新 prompt", "doubao-ultra", null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4074);
                    assertThat(upstream.getMessage())
                            .isEqualTo("绑定的模型档位不存在：doubao-ultra（先在模型档位面建行）");
                });

        // 停用（PRJ_075＝4075）：边缘写腿同口径——HTTP 400 + provider message 原样
        var disabledTier = AiplatformWireTestSupport.agentConfigAppServiceWithErrorTransport(
                400, PRJ_075_ERROR_ENVELOPE, new String[1]);
        assertThatThrownBy(() -> disabledTier.updateEdgeModel("naming",
                new AiplatformEdgeModelUpdateCommand("doubao-flagship")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4075);
                    assertThat(upstream.getMessage())
                            .isEqualTo("绑定的模型档位已停用：doubao-flagship（启用该档位或改绑其他档位）");
                });
    }
}
