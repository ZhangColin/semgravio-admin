package com.semgravio.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentToolToggleCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentToolResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentToolSlotResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 工具面域（槽位清单＋增强工具开关）的 wire 契约测试（对接 aiplatform
 * #252 技能线 T6，issue #85）。
 *
 * <p>契约事实（2026-09-24 对照 aiplatform 本地实例 swagger {@code /v3/api-docs} 实读＋
 * 源码 {@code BackofficeAgentToolController} / {@code BackofficeAgentToolAppService} /
 * {@code BackofficeAgentToolResponse} / {@code BackofficeAgentToolSlotResponse} /
 * {@code AgentToolToggleCommand} / {@code AgentTool} / {@code AgentToolKind} /
 * {@code HarnessBuiltinTools} / {@code ProfileSubagentSupplier} / {@code ProjectMessage} /
 * {@code ErrorCodePrefix} 核实）：</p>
 * <ul>
 *   <li>两端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}：GET
 *       {@code /agent-tools} data 为<strong>裸 List</strong>（三槽位 main/executor/subagent
 *       有界——List 直出同形，不造分页）；PUT {@code /agent-tools/{toolName}} 回执为单工具行
 *       （与清单嵌套行同形——provider 回执即该工具行）。</li>
 *   <li>槽位行（3 件）：{@code slot}（与技能槽位同键：main=主智能体／executor=run 执行体／
 *       subagent=子智能体）＋{@code slotName}＋{@code tools[]}。呈现序 provider 定死＝平台
 *       资产在前（骨架后增强，AgentTool 枚举声明序）、harness 内建在后——admin 原样保序。</li>
 *   <li>工具行（4 件）：{@code name}（注册名＝按名寻址键）＋{@code kind}（类别名<strong>字符串</strong>
 *       SKELETON／ENHANCEMENT／HARNESS_BUILTIN——开关判定正本，admin 不映射枚举）＋
 *       {@code enabled}（provider 原生 boolean <strong>恒在场</strong>：骨架/harness 内建恒
 *       true；增强按运营配置生效态——false＝在册但退出装配面）＋{@code description}。</li>
 *   <li>PUT 命令体（1 件）：{@code {enabled}}——逐字镜像 {@code AgentToolToggleCommand}，
 *       <strong>Boolean 装箱</strong>（provider 命令同型）＋provider 侧 {@code @RequestBody(required=false)}
 *       缺 body 容忍：null 出站在场（全局 Jackson 含 null）＝provider 判缺目标态 400
 *       PRJ_037（无缺省翻转语义），若北向用原生 boolean 缺省会落成 false（关）凭空制造一次写。</li>
 *   <li>错误透传（PRJ 域码 4×1000＋序号）：PRJ_035→4035（404 工具不存在——清单读面即全集）；
 *       PRJ_036→4036（403 骨架/harness 内建不开放开关——ADR-0021 编排权不下放配置）；
 *       PRJ_037→4037（400 开关目标态必填）；PRJ_034→4034（400 操作者不能为空——开关写必留痕，
 *       缺头域面守卫拦，admin 不拦）——fail-fast 原样透传，admin 不映射不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformAgentToolClientContractTest {

    /** 增强工具注册名之一（AgentTool 枚举实值）：web_search=联网搜索——开关代表面。 */
    private static final String TOOL_NAME = "web_search";

    /**
     * aiplatform GET /api/backoffice/agent-tools 的真实成功响应形状（三槽位代表面）：
     * main 槽平台资产全八件（六骨架＋两增强——fetch_url 关态代表：在册但退出装配面）、
     * executor 槽两骨架＋harness 内建呈现（main 只读面结构性无内建）、subagent 槽声明工具面
     * （HARNESS_BUILTIN 恒 true、同一句声明描述）。行值全取 provider 源码实值（AgentTool
     * 枚举名/描述、HarnessBuiltinTools 名单、SELF_TEST_TOOLS 哨兵 allowlist）。
     */
    private static final String INVENTORY_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "slot": "main",
                  "slotName": "主智能体",
                  "tools": [
                    {"name": "ask_user", "kind": "SKELETON", "enabled": true,
                     "description": "每轮一问的澄清工具（问答挂起的唯一起源——编排链路）"},
                    {"name": "savePrd", "kind": "SKELETON", "enabled": true,
                     "description": "PRD 产出/修订落盘＋业务登记（需求侧判定的观测面——编排链路）"},
                    {"name": "saveBuildPlan", "kind": "SKELETON", "enabled": true,
                     "description": "切片计划事实登记（生成编排的切片输入——编排链路）"},
                    {"name": "list_workspace_files", "kind": "SKELETON", "enabled": true,
                     "description": "工作区文件清单（答询查证只读件）"},
                    {"name": "read_workspace_file", "kind": "SKELETON", "enabled": true,
                     "description": "读工作区文件内容（答询查证只读件）"},
                    {"name": "query_project_facts", "kind": "SKELETON", "enabled": true,
                     "description": "查项目事实含系统访问地址（答询查证只读件）"},
                    {"name": "fetch_url", "kind": "ENHANCEMENT", "enabled": false,
                     "description": "读用户贴的 http/https 地址内容（外部资料，取数口安全底线兜底——增强可关）"},
                    {"name": "web_search", "kind": "ENHANCEMENT", "enabled": true,
                     "description": "自主搜索调研补缺口（增强可关——成本与滥用可控）"}
                  ]
                },
                {
                  "slot": "executor",
                  "slotName": "run 执行体",
                  "tools": [
                    {"name": "finish_edit", "kind": "SKELETON", "enabled": true,
                     "description": "更新收口结束工具（「要不要动系统」的判定面——编排链路）"},
                    {"name": "update_plan", "kind": "SKELETON", "enabled": true,
                     "description": "run 级步骤清单的全量快照（呈现面在部件映射表——编排链路）"},
                    {"name": "read_file", "kind": "HARNESS_BUILTIN", "enabled": true,
                     "description": "读取沙箱内文件内容"},
                    {"name": "write_file", "kind": "HARNESS_BUILTIN", "enabled": true,
                     "description": "向沙箱内写入文件"}
                  ]
                },
                {
                  "slot": "subagent",
                  "slotName": "子智能体",
                  "tools": [
                    {"name": "read_file", "kind": "HARNESS_BUILTIN", "enabled": true,
                     "description": "self-test 子智能体声明工具面（哨兵 allowlist 结构性收窄，只读交付代码＋跑测试＋报告写隔离根）"},
                    {"name": "execute", "kind": "HARNESS_BUILTIN", "enabled": true,
                     "description": "self-test 子智能体声明工具面（哨兵 allowlist 结构性收窄，只读交付代码＋跑测试＋报告写隔离根）"}
                  ]
                }
              ],
              "requestId": "req-at1i2o",
              "errors": null
            }
            """;

    /**
     * aiplatform PUT /api/backoffice/agent-tools/{toolName} 的真实成功回执形状：开关后工具行
     * （与清单嵌套行同形）——本例 web_search 关（enabled=false＝退出装配面）。
     */
    private static final String TOGGLE_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "name": "web_search",
                "kind": "ENHANCEMENT",
                "enabled": false,
                "description": "自主搜索调研补缺口（增强可关——成本与滥用可控）"
              },
              "requestId": "req-at3t4g",
              "errors": null
            }
            """;

    /** 工具不存在（清单读面即全集）：HTTP 404 + 信封 code=4035（PRJ_035，域码 4）。 */
    private static final String PRJ_035_ERROR_ENVELOPE = """
            {
              "code": 4035,
              "message": "工具不存在（工具面按注册名寻址，清单读面即全集）",
              "data": null,
              "requestId": "req-at5o5l",
              "errors": null
            }
            """;

    /** 骨架/harness 内建不开放开关（编排权不下放配置）：HTTP 403 + 信封 code=4036（PRJ_036，域码 4）。 */
    private static final String PRJ_036_ERROR_ENVELOPE = """
            {
              "code": 4036,
              "message": "该工具不开放开关（骨架/harness 内建结构性锁死——编排权不下放配置）",
              "data": null,
              "requestId": "req-at7s6e",
              "errors": null
            }
            """;

    /** 开关目标态必填（无缺省翻转语义）：HTTP 400 + 信封 code=4037（PRJ_037，域码 4）。 */
    private static final String PRJ_037_ERROR_ENVELOPE = """
            {
              "code": 4037,
              "message": "开关目标态必填（enabled=true/false）",
              "data": null,
              "requestId": "req-at9e7a",
              "errors": null
            }
            """;

    /** 操作者缺头（开关写必留痕——域面守卫拦）：HTTP 400 + 信封 code=4034（PRJ_034，域码 4）。 */
    private static final String PRJ_034_ERROR_ENVELOPE = """
            {
              "code": 4034,
              "message": "操作者不能为空",
              "data": null,
              "requestId": "req-at1r4c",
              "errors": null
            }
            """;

    // ========== 工具面清单（GET /agent-tools：三槽位裸 List + 嵌套工具行全绑定）==========

    @Test
    void given_noParams_when_listToolSlots_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithStubTransport(
                INVENTORY_ENVELOPE, wireUrl, new String[1]);

        appService.listToolSlots();

        // 清单无路径参数无查询参数——根路径直出（三槽位有界 List，admin 不造分页不过滤）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/agent-tools");
    }

    @Test
    void given_inventoryEnvelope_when_listToolSlots_then_threeSlotsWithKindsAndToggleStatesBound() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithStubTransport(
                INVENTORY_ENVELOPE, new String[1], new String[1]);

        List<AiplatformAgentToolSlotResponse> slots = appService.listToolSlots();

        // 信封正确拆开——data 为裸 List（三槽位有界，List 直出同形）；保序透传（呈现序
        // provider 定死＝平台资产在前、harness 内建在后）
        assertThat(slots).hasSize(3);
        assertThat(slots.stream().map(AiplatformAgentToolSlotResponse::slot))
                .containsExactly("main", "executor", "subagent");
        assertThat(slots.get(0).slotName()).isEqualTo("主智能体");
        assertThat(slots.get(1).slotName()).isEqualTo("run 执行体");
        assertThat(slots.get(2).slotName()).isEqualTo("子智能体");

        // main 槽平台资产全八件（呈现序＝骨架后增强——AgentTool 枚举声明序原样保序）：
        // 六骨架恒挂载＋两增强按运营配置生效态——fetch_url 关态代表（false＝在册但退出装配面）
        List<AiplatformAgentToolResponse> mainTools = slots.get(0).tools();
        assertThat(mainTools).hasSize(8);
        assertThat(mainTools.stream().map(AiplatformAgentToolResponse::name)).containsExactly(
                "ask_user", "savePrd", "saveBuildPlan", "list_workspace_files",
                "read_workspace_file", "query_project_facts", "fetch_url", "web_search");
        assertThat(mainTools.stream().map(AiplatformAgentToolResponse::kind))
                .containsOnly("SKELETON", "ENHANCEMENT");
        assertThat(mainTools.get(0).enabled()).isTrue();
        assertThat(mainTools.get(0).description()).contains("澄清工具");
        AiplatformAgentToolResponse fetchUrl = mainTools.get(6);
        assertThat(fetchUrl.kind()).isEqualTo("ENHANCEMENT");
        assertThat(fetchUrl.enabled()).isFalse();
        assertThat(fetchUrl.description()).contains("增强可关");
        AiplatformAgentToolResponse webSearch = mainTools.get(7);
        assertThat(webSearch.enabled()).isTrue();

        // executor 槽：两骨架＋harness 内建呈现口径（框架自带、不可开关——恒 true）；
        // kind 为类别名字符串原样透传（admin 不映射枚举）
        List<AiplatformAgentToolResponse> executorTools = slots.get(1).tools();
        assertThat(executorTools).hasSize(4);
        assertThat(executorTools.get(2).name()).isEqualTo("read_file");
        assertThat(executorTools.get(2).kind()).isEqualTo("HARNESS_BUILTIN");
        assertThat(executorTools.get(2).enabled()).isTrue();

        // subagent 槽：声明工具面（self-test 哨兵 allowlist 同源引用——同一句声明描述）
        List<AiplatformAgentToolResponse> subagentTools = slots.get(2).tools();
        assertThat(subagentTools).hasSize(2);
        assertThat(subagentTools.get(0).name()).isEqualTo("read_file");
        assertThat(subagentTools.get(0).kind()).isEqualTo("HARNESS_BUILTIN");
        assertThat(subagentTools.get(0).description()).contains("self-test 子智能体声明工具面");
        assertThat(subagentTools.get(1).name()).isEqualTo("execute");
    }

    // ========== 增强工具开关（PUT /agent-tools/{toolName}：单字段命令体 + 回执同形清单行）==========

    @Test
    void given_toggleCommand_when_toggleTool_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithStubTransport(
                TOGGLE_RECEIPT_ENVELOPE, wireUrl, wireBody);

        appService.toggleTool(TOOL_NAME, new AiplatformAgentToolToggleCommand(false));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/agent-tools/{toolName}
        // （toolName 注册名字符串路径参数透传，admin 不预校验），命令体 {enabled} 单字段——
        // 逐字镜像 AgentToolToggleCommand，无增删
        assertThat(wireUrl[0])
                .isEqualTo("http://stub-aiplatform/api/backoffice/agent-tools/" + TOOL_NAME);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("enabled");
        assertThat(body.path("enabled").asBoolean()).isFalse();
    }

    @Test
    void given_missingEnabled_when_toggleTool_then_nullSerializedPresentNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithStubTransport(
                TOGGLE_RECEIPT_ENVELOPE, new String[1], wireBody);

        // 缺 body（provider @RequestBody(required=false) 形态——可选绑定不前置拦截）与
        // 缺 enabled（装箱 Boolean null）两形都 null 透传——null 在场而非缺省（全局
        // Jackson 含 null），provider 侧判 400 PRJ_037（无缺省翻转语义）
        appService.toggleTool(TOOL_NAME, new AiplatformAgentToolToggleCommand(null));
        var explicitNull = new ObjectMapper().readTree(wireBody[0]);
        assertThat(explicitNull.fieldNames()).toIterable().containsExactly("enabled");
        assertThat(explicitNull.path("enabled").isNull()).isTrue();

        appService.toggleTool(TOOL_NAME, null);
        var missingBody = new ObjectMapper().readTree(wireBody[0]);
        assertThat(missingBody.fieldNames()).toIterable().containsExactly("enabled");
        assertThat(missingBody.path("enabled").isNull()).isTrue();
    }

    @Test
    void given_toggleReceiptEnvelope_when_toggleTool_then_receiptSameShapeAsInventoryRow() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithStubTransport(
                TOGGLE_RECEIPT_ENVELOPE, new String[1], new String[1]);

        AiplatformAgentToolResponse receipt = appService.toggleTool(
                TOOL_NAME, new AiplatformAgentToolToggleCommand(false));

        // 回执＝开关后工具行（与清单嵌套行同形）：四件全绑定——关态代表面
        // （enabled=false＝退出槽位装配面——模型不可见）
        assertThat(receipt.name()).isEqualTo(TOOL_NAME);
        assertThat(receipt.kind()).isEqualTo("ENHANCEMENT");
        assertThat(receipt.enabled()).isFalse();
        assertThat(receipt.description()).contains("增强可关");
    }

    // ========== 错误族透传（PRJ_035/036/037/034 数字业务码，不映射）==========

    @Test
    void given_prj035ErrorEnvelope_when_toggleUnknownTool_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithErrorTransport(
                404, PRJ_035_ERROR_ENVELOPE, new String[1]);

        // 未知名由 provider 裁决（admin 不预检 toolName 字符串路径参数——清单读面即全集）：
        // HTTP 404 + 数字业务码 4035（PRJ_035＝域码 4×1000＋35）+ provider message 原样
        assertThatThrownBy(() -> appService.toggleTool("no_such_tool",
                new AiplatformAgentToolToggleCommand(true)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4035);
                    assertThat(upstream.getMessage())
                            .isEqualTo("工具不存在（工具面按注册名寻址，清单读面即全集）");
                });
    }

    @Test
    void given_prj036ErrorEnvelope_when_toggleSkeletonTool_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithErrorTransport(
                403, PRJ_036_ERROR_ENVELOPE, new String[1]);

        // 骨架锁死判定归 provider（ADR-0021 编排权不下放配置——admin 不代判 kind）：
        // HTTP 403 + 数字业务码 4036（PRJ_036＝域码 4×1000＋36）+ provider message 原样
        assertThatThrownBy(() -> appService.toggleTool("ask_user",
                new AiplatformAgentToolToggleCommand(false)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(403);
                    assertThat(upstream.envelopeCode()).isEqualTo(4036);
                    assertThat(upstream.getMessage())
                            .isEqualTo("该工具不开放开关（骨架/harness 内建结构性锁死——编排权不下放配置）");
                });
    }

    @Test
    void given_prj037ErrorEnvelope_when_toggleWithoutTarget_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithErrorTransport(
                400, PRJ_037_ERROR_ENVELOPE, new String[1]);

        // 缺目标态由 provider 裁决（enabled 必填、无缺省翻转语义——admin 不前置拦截）：
        // HTTP 400 + 数字业务码 4037（PRJ_037＝域码 4×1000＋37）+ provider message 原样
        assertThatThrownBy(() -> appService.toggleTool(TOOL_NAME,
                new AiplatformAgentToolToggleCommand(null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4037);
                    assertThat(upstream.getMessage()).isEqualTo("开关目标态必填（enabled=true/false）");
                });
    }

    @Test
    void given_prj034ErrorEnvelope_when_toggle_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.agentToolAppServiceWithErrorTransport(
                400, PRJ_034_ERROR_ENVELOPE, new String[1]);

        // 操作者缺头由 provider 域面守卫拦截（开关写必留痕，admin 不拦——透传断言即证据）：
        // HTTP 400 + 数字业务码 4034（PRJ_034＝域码 4×1000＋34）+ provider message 原样
        assertThatThrownBy(() -> appService.toggleTool(TOOL_NAME,
                new AiplatformAgentToolToggleCommand(true)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4034);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }
}
