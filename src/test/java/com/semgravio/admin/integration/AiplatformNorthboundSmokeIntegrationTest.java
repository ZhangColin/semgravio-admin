package com.semgravio.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;

import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformAccountController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformAgentConfigController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformAgentToolController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformCostController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformMaterialController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformOrderController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformPriceEntryController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformProjectController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformSkillController;
import com.semgravio.admin.aiplatform.endpoints.controller.AiplatformWorkspaceController;
import com.cartisan.security.annotation.RequirePermission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform BFF 北向全量冒烟复核（issue #72 T10 六域收口、#80–#85 技能线 T1–T6
 * 逐票扩入 15 端点、#86 七域收口、#90 指派双头预检——技能域第 16 端点、#92 草稿审核面
 * ——技能域第 17–20 端点、#93 使用计数观测扩展追平——清单/详情增 loadCount/lastLoadedAt、
 * 详情另增 resources scripts/ 资源面（#261 观测面＋#253 漂移补齐；纯字段无新端点无新
 * 权限码，端点计数/码清单不变）、#96 设计能力线 T1——订单域设计资产包取件第 55 端点
 * （镜像 provider #298，交付物类型字段对纯字段不加端点）、#98 设计能力线 T3——成本域
 * 按张档位（镜像 provider #288，tokenKind 第 6 档＋总览图片计量类描述；纯字段无新端点
 * 无新权限码，端点计数/码清单不变）、#61 冒烟四项之「域内全端点」就绪态）。
 *
 * <p>七域 55 端点合入后的收口审计，两条腿：</p>
 * <ul>
 *   <li><b>swagger 全量可见</b>：拉 {@code /v3/api-docs}（springdoc 真实输出），与反射扫描
 *       十个 controller 的映射注解派生的期望集<b>精确相等</b>（前缀 {@code /api/admin/aiplatform}
 *       下既不缺端点也不多端点，方法逐一路径一致）——admin-web 按 api-docs 对接（spec #62
 *       user story 35）的前提被钉死。期望集源自代码本身（单一事实源），端点计数 55 显式断言。</li>
 *   <li><b>权限注解与权限码清单一致</b>：每个北向映射方法恰一个 {@code @RequirePermission}；
 *       distinct 权限码集合 == 权限码清单（spec #62 十九码 + spec #79 技能线十一码
 *       read/install/disable/enable/uninstall/assign/update＋agent-config:read/update
 *       ＋agent-tool:read/toggle——留痕读面复用 read；#90 指派双头预检复用 skill:assign；
 *       #92 草稿审核面三码 skill-draft:read/promote/reject——两读端点复用 read、
 *       promote/reject 各一码，共三十三码）。注解计数 == 55（一端点一码，
 *       无裸奔端点；#96 design-package 复用 order:read 不新增码）。与
 *       {@code AiplatformRbacEnforcementIntegrationTest}（运行期三态行为）
 *       互补——此处钉静态清单完整性，防后续增删端点时清单漂移。</li>
 * </ul>
 *
 * <p>纯反射部分不依赖种子/登录；swagger 部分走 {@code RANDOM_PORT} 真实 MVC（api-docs 不在
 * 认证拦截范围）。#61 冒烟四项中的签名负例三连归联调环境执行，本票只保证端点与注解齐备
 * （本类 + 既有 RBAC/BFF/wire 契约测试共同构成就绪态证据）。</p>
 *
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiplatformNorthboundSmokeIntegrationTest {

    /** spec #62 + #79 技能线 T1–T6 + #90 预检 + #92 草稿审核面 + #93 观测扩展（纯字段不加端点）+ #96 设计能力线 T1（design-package 取件端点）：七域 55 端点 = 订单 7 + 项目 9 + 沙箱 6 + 成本 4 + 单价表 3 + 素材 5 + 技能 11 + 智能体配置 3 + 工具面 2 + 草稿 4 + 账号 1。 */
    private static final int EXPECTED_ENDPOINT_COUNT = 55;

    private static final String ROUTE_PREFIX = "/api/admin/aiplatform";

    private static final List<Class<?>> CONTROLLERS = List.of(
            AiplatformOrderController.class,
            AiplatformProjectController.class,
            AiplatformWorkspaceController.class,
            AiplatformCostController.class,
            AiplatformPriceEntryController.class,
            AiplatformMaterialController.class,
            AiplatformSkillController.class,
            AiplatformAgentConfigController.class,
            AiplatformAgentToolController.class,
            AiplatformAccountController.class);

    /** 权限码清单（33）：十读码（七域＋智能体配置＋工具面＋account＋草稿）＋二十三写码（spec #62＋#79 技能线 T1 read/T2 四写码/T3 assign/T4 update/T5 agent-config:update/T6 agent-tool:read+toggle——留痕读面复用 agent-config:read；#92 草稿面 read/promote/reject 三码——两读端点复用 read）。 */
    private static final Set<String> EXPECTED_PERMISSION_CODES = Set.of(
            "admin:aiplatform:order:read",
            "admin:aiplatform:project:read",
            "admin:aiplatform:workspace:read",
            "admin:aiplatform:cost:read",
            "admin:aiplatform:price-entry:read",
            "admin:aiplatform:material:read",
            "admin:aiplatform:account:read",
            "admin:aiplatform:skill:read",
            "admin:aiplatform:agent-config:read",
            "admin:aiplatform:skill-draft:read",
            "admin:aiplatform:order:quote",
            "admin:aiplatform:order:cancel",
            "admin:aiplatform:order:retry-archive",
            "admin:aiplatform:workspace:wake",
            "admin:aiplatform:workspace:hibernate",
            "admin:aiplatform:workspace:rebuild",
            "admin:aiplatform:workspace:seal",
            "admin:aiplatform:material:disable",
            "admin:aiplatform:material:enable",
            "admin:aiplatform:material:delete",
            "admin:aiplatform:price-entry:reprice",
            "admin:aiplatform:price-entry:deactivate",
            "admin:aiplatform:skill:install",
            "admin:aiplatform:skill:disable",
            "admin:aiplatform:skill:enable",
            "admin:aiplatform:skill:uninstall",
            "admin:aiplatform:skill:assign",
            "admin:aiplatform:skill:update",
            "admin:aiplatform:agent-config:update",
            "admin:aiplatform:agent-tool:read",
            "admin:aiplatform:agent-tool:toggle",
            "admin:aiplatform:skill-draft:promote",
            "admin:aiplatform:skill-draft:reject");

    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final int port;

    @Autowired
    AiplatformNorthboundSmokeIntegrationTest(
            TestRestTemplate restTemplate, ObjectMapper objectMapper, @Value("${local.server.port}") int port) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.port = port;
    }

    @Test
    @DisplayName("swagger 冒烟：/v3/api-docs 中 aiplatform 55 端点全量可见（与 controller 映射精确相等）")
    void given_openApiDocs_when_fetch_then_all55AiplatformEndpointsVisible() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).as("api-docs 应可匿名拉取：%s", response.getBody())
                .isEqualTo(HttpStatus.OK);

        // 期望集从 controller 映射注解派生（代码即单一事实源），计数钉死 54
        // （materials/{id} 同路径 get+delete 双方法、agent-configs/{agentKey} 同路径 get+put
        // 双方法、agent-tools/{toolName} 同路径 put——按「路径×方法」对计数，非路径数；
        // #90 预检为独立路径 assignments/{slot}/precheck 的 post、#92 草稿面四路径
        // drafts get＋drafts/{id} get＋promote post＋reject post，+4 对）
        Map<String, Set<String>> expected = deriveFromControllers();
        int endpointPairCount = expected.values().stream().mapToInt(Set::size).sum();
        assertThat(endpointPairCount).as("十 controller 映射（路径×方法）对应为 55（spec #62 + #79 技能线 T1–T6 + #90 预检 + #92 草稿面 + #96 设计资产包取件）")
                .isEqualTo(EXPECTED_ENDPOINT_COUNT);

        JsonNode paths = objectMapper.readTree(response.getBody()).path("paths");
        assertThat(paths.isObject()).as("api-docs 应含 paths 对象：%s", response.getBody()).isTrue();

        // 前缀下精确相等：既不缺端点（全量可见）、也不多端点（无意外暴露——如 provider「开行」）
        Set<String> actualAiplatformPaths = new TreeSet<>();
        paths.fieldNames().forEachRemaining(path -> {
            if (path.startsWith(ROUTE_PREFIX)) {
                actualAiplatformPaths.add(path);
            }
        });
        assertThat(actualAiplatformPaths)
                .as("api-docs 的 aiplatform 路径集应与 controller 映射精确相等")
                .isEqualTo(expected.keySet());

        // 逐路径断言 HTTP 方法一致（get/post/delete 逐一路径对齐）
        for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
            Set<String> actualMethods = new TreeSet<>();
            paths.path(entry.getKey()).fieldNames().forEachRemaining(actualMethods::add);
            assertThat(actualMethods)
                    .as("路径 %s 的 HTTP 方法应与 controller 映射一致", entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("权限注解复核：每映射方法恰一码、33 码清单精确一致（含 account:read/skill:read/agent-config:read/agent-tool:read/skill-draft:read——预检复用 skill:assign、草稿两读端点复用 skill-draft:read、design-package 复用 order:read）、注解计数=55")
    void given_aiplatformControllers_when_scanPermissionAnnotations_then_codesMatchSpecList() {
        Set<String> codes = new TreeSet<>();
        int annotatedEndpointCount = 0;

        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null) {
                    continue; // 非映射方法（辅助/私有方法不挂注解）
                }
                RequirePermission[] annotations = method.getAnnotationsByType(RequirePermission.class);
                assertThat(annotations)
                        .as("%s#%s 是北向映射方法，必须恰挂一个 @RequirePermission（无裸奔端点）",
                                controller.getSimpleName(), method.getName())
                        .hasSize(1);
                codes.add(annotations[0].value());
                annotatedEndpointCount++;
            }
        }

        assertThat(annotatedEndpointCount)
                .as("带权限注解的端点计数应为 55（与 swagger 冒烟的映射计数同一事实源）")
                .isEqualTo(EXPECTED_ENDPOINT_COUNT);
        assertThat(codes)
                .as("distinct 权限码集合应与权限码清单精确一致（十读 + 二十三写，不缺不多）")
                .isEqualTo(EXPECTED_PERMISSION_CODES);
        // 显式点名（spec #72 验收）：account:read 有码（嵌抽屉用、无页面）
        assertThat(codes).contains("admin:aiplatform:account:read");
        // 显式点名（spec #79 验收）：agent-tool:read 有码（并入智能体配置页区块、无独立页面）
        assertThat(codes).contains("admin:aiplatform:agent-tool:read");
        // 显式点名（spec #91 验收）：skill-draft:read 有码（草稿审核页承载、读写分权之首）
        assertThat(codes).contains("admin:aiplatform:skill-draft:read");
    }

    /**
     * 反射扫描十个 controller 的映射注解，派生「路径 → HTTP 方法集」期望集
     * （类级 + 方法级 {@code @RequestMapping} 合并；路径取首个值——本包约定每方法单路径）。
     */
    private static Map<String, Set<String>> deriveFromControllers() {
        Map<String, Set<String>> out = new TreeMap<>();
        for (Class<?> controller : CONTROLLERS) {
            RequestMapping classMapping =
                    AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            String prefix = classMapping == null || classMapping.path().length == 0
                    ? ""
                    : classMapping.path()[0];
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping =
                        AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String path = prefix + (mapping.path().length == 0 ? "" : mapping.path()[0]);
                Set<String> methods = out.computeIfAbsent(path, key -> new TreeSet<>());
                for (RequestMethod requestMethod : mapping.method()) {
                    methods.add(requestMethod.name().toLowerCase());
                }
            }
        }
        return out;
    }
}
