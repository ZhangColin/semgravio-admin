package com.aieducenter.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.aieducenter.admin.aiplatform.application.AiplatformUpstreamException;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAccountProfileWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigTraceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigUpdateWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolSlotWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolToggleWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformCostOverviewWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformCostWindowWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformConversationEntryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformMaterialDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformMaterialSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderBriefWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderCancelWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderQuoteWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPriceEntryRepriceWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPriceEntryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPrdWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillAssignWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDraftDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDraftRejectWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDraftSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillInstallWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillPrecheckWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateTraceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSlotAssignmentWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectCostDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectCostWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectFileContentWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectFilesWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformTokenUsageWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryRepriceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnpricedUsageWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformVersionDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformVersionWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformWorkspaceDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformWorkspaceSummaryWireResponse;
import com.aieducenter.admin.aiplatform.infrastructure.AiplatformClient;
import com.aieducenter.admin.application.AdminUserManagementAppService;
import com.aieducenter.admin.application.RoleManagementAppService;
import com.aieducenter.admin.application.dto.command.AssignPermissionsCommand;
import com.aieducenter.admin.application.dto.command.AssignRolesCommand;
import com.aieducenter.admin.application.dto.command.CreateAdminUserCommand;
import com.aieducenter.admin.application.dto.command.CreateRoleCommand;
import com.cartisan.core.context.RequestContext;
import com.cartisan.openapi.client.BinaryResponse;
import com.cartisan.openapi.client.OpenApiClientException;
import com.cartisan.web.response.PageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;

/**
 * aiplatform BFF 端点 RBAC 强制执行集成测试（issue #63 T1 账号 + #64 订单读路径 + #65 项目核心读路径 +
 * #66 沙箱观测与四干预动作 + #67 成本四读口 + #68 单价表清单/原子改价/停用 + #69 知识素材
 * 清单/详情/停用⇄启用/删除 + #70 订单写路径报价/改价/取消/重试归档 + #71 项目文件区
 * 文件树/文件内容/文件包 + #80 技能线 T1 技能清单/详情 + #81 技能线 T2
 * 安装/停用⇄启用/卸载写口 + #82 技能线 T3 槽位指派读写 + #83 技能线 T4 显式更新＋更新留痕读面 +
 * #84 技能线 T5 智能体配置读写＋变更留痕读面 + #85 技能线 T6 工具面清单＋增强工具开关 +
 * #90 指派双头预检——技能域第 16 端点 + #92 草稿审核面——技能域第 17–20 端点 +
 * #96 设计能力线 T1——订单设计资产包取件（复用 order:read，交付物类型字段对随清单/详情/
 * 写回执北向断言））
 * ——真实 Sa-Token 过滤链，断言 {@code admin:aiplatform:account:read} / {@code admin:aiplatform:order:read} /
 * {@code admin:aiplatform:project:read} / {@code admin:aiplatform:workspace:read} /
 * {@code admin:aiplatform:cost:read} / {@code admin:aiplatform:price-entry:read} /
 * {@code admin:aiplatform:material:read} / {@code admin:aiplatform:skill:read} /
 * {@code admin:aiplatform:agent-config:read} + 订单三独立写码
 * （{@code order:quote|cancel|retry-archive}）+ 沙箱四独立写码
 * （{@code workspace:wake|hibernate|rebuild|seal}）+ 单价表两独立写码
 * （{@code price-entry:reprice|deactivate}）+ 知识素材三独立写码
 * （{@code material:disable|enable|delete}）+ 技能六独立写码
 * （{@code skill:install|disable|enable|uninstall|assign|update}——指派读面/更新留痕读面复用 read、
 * 写面独立 assign/update；指派双头预检<strong>复用 assign</strong>（#90：预检触发模型调用有成本，
 * 只读角色不得调用——read≠assign 两码不串；超管 bypass 与其他技能域端点一致，框架级测试钉住））
 * + 智能体配置独立写码
 * （{@code agent-config:update}——读面/留痕读面复用 read，高敏感独立授权面）+ 工具面两码
 * （{@code agent-tool:read|toggle}——清单读面与开关写面分权）+ 草稿审核面三码
 * （{@code skill-draft:read|promote|reject}——issue #92：两读端点复用 read、promote/reject
 * 各一码，读写不串、两写码互不串——晋升权与拒绝权独立授受）对未登录（401）/
 * 无权者（403）/ 有权者（200）的行为，
 * 并钉死北向出口形状、二进制透传与 provider 错误信封透传。成本域另钉死时间窗 from/to 北向必填
 * （缺参 400 / 非 Instant 404 均在本服务绑定层裁决、不到 provider，issue #67：不设默认窗口）。
 *
 * <p>镜像 {@code AccountRbacEnforcementIntegrationTest}（登录/鉴权辅助沿用
 * {@code RbacEnforcementIntegrationTest}）。200 用例 mock {@link AiplatformClient}，证明权限放行后
 * 整条 controller→appservice→client 通路接通。超管 bypass 由框架级测试钉住，此处不重复。</p>
 *
 * <p><strong>操作者身份透传契约</strong>（issue #66 断言必带；#69 素材域、#70 订单域、#81 技能域同款必带）：订单三写
 * 操作（报价落痕价目行、取消/归档落痕订单行）、沙箱四干预动作、
 * 单价表改价/停用、素材停用/启用、技能四写口（#82 增指派整包替换，同款必带）从 {@code RequestContext} 读 operator 审计，admin 不在 body 塞
 * 身份——框架 cartisan-openapi 自动从 {@code RequestContext} 带 {@code X-User-Id/X-User-Name}
 * 出站 header。本测试在 mocked {@link AiplatformClient} 边界用 {@code doAnswer} 在调用瞬间抓取
 * {@link RequestContext#getUserId()} / {@link RequestContext#getUserName()}，断言其 == 登录治理
 * 运营的 id / 昵称（{@code AccountRbacEnforcementIntegrationTest} 同款手法）。素材域该头是
 * provider 必拦项（缺头 400 KNW_006——治理动作必留痕），透传断言即「北向永缺不了头」的证据。</p>
 *
 * <p><strong>错误信封透传（spec #62 定稿）</strong>：下游 IDN_004（HTTP 404 + 数字业务码 6004）经
 * {@link AiplatformUpstreamException} 抵达 {@code AiplatformUpstreamErrorAdvice}，北向还原 provider
 * 形状——HTTP 404 + 信封 {@code code}=6004 + {@code message} 原文，<strong>不</strong>翻译成
 * {@code BaseCodeMessage.NOT_FOUND}（404/404）。这是该 advice 在真实 MVC 层的端到端证据
 * （{@code AiplatformBffIntegrationTest} 为 MOCK 环境，不经 HTTP）。</p>
 *
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiplatformRbacEnforcementIntegrationTest {

    private static final String PASSWORD = "Test1234";
    private static final String READ_PERMISSION = "admin:aiplatform:account:read";
    private static final String ORDER_READ_PERMISSION = "admin:aiplatform:order:read";
    private static final String PROJECT_READ_PERMISSION = "admin:aiplatform:project:read";
    private static final String EXTERNAL_ID = "auth0|65f2c8a1b3d4e5f6a7b8c9d0";
    private static final String PROFILE_ENDPOINT = "/api/admin/aiplatform/accounts/" + EXTERNAL_ID;
    private static final String ORDER_ID = "3829492001234567";
    private static final String ORDERS_ENDPOINT = "/api/admin/aiplatform/orders";
    private static final String ORDER_DETAIL_ENDPOINT = ORDERS_ENDPOINT + "/" + ORDER_ID;
    private static final String SOURCE_PACKAGE_ENDPOINT = ORDER_DETAIL_ENDPOINT + "/source-package";
    private static final String DESIGN_PACKAGE_ENDPOINT = ORDER_DETAIL_ENDPOINT + "/design-package";
    private static final String ORDER_QUOTE_PERMISSION = "admin:aiplatform:order:quote";
    private static final String ORDER_CANCEL_PERMISSION = "admin:aiplatform:order:cancel";
    private static final String ORDER_RETRY_ARCHIVE_PERMISSION = "admin:aiplatform:order:retry-archive";
    private static final String QUOTE_ENDPOINT = ORDER_DETAIL_ENDPOINT + "/quote";
    private static final String CANCEL_ENDPOINT = ORDER_DETAIL_ENDPOINT + "/cancel";
    private static final String RETRY_ARCHIVE_ENDPOINT = ORDER_DETAIL_ENDPOINT + "/retry-archive";
    private static final String QUOTE_BODY = "{\"amount\":2199000,\"note\":\"含加急费用\"}";
    private static final String CANCEL_BODY = "{\"reason\":\"重复下单，用户要求取消\"}";
    private static final String PROJECT_ID = "3829492007654321";
    private static final String PROJECTS_ENDPOINT = "/api/admin/aiplatform/projects";
    private static final String PROJECT_DETAIL_ENDPOINT = PROJECTS_ENDPOINT + "/" + PROJECT_ID;
    private static final String CONVERSATION_ENDPOINT = PROJECT_DETAIL_ENDPOINT + "/conversation";
    private static final String PRD_ENDPOINT = PROJECT_DETAIL_ENDPOINT + "/prd";
    private static final String VERSIONS_ENDPOINT = PROJECT_DETAIL_ENDPOINT + "/versions";
    private static final String VERSION_HASH = "3f9c1a2b7d84e5f6a0b1c2d3e4f5a6b7c8d9e0f1";
    private static final String VERSION_DETAIL_ENDPOINT = VERSIONS_ENDPOINT + "/" + VERSION_HASH;
    private static final String FILES_ENDPOINT = PROJECT_DETAIL_ENDPOINT + "/files";
    private static final String FILE_CONTENT_ENDPOINT = FILES_ENDPOINT + "/content";
    private static final String FILES_PACKAGE_ENDPOINT = FILES_ENDPOINT + "/package";
    private static final String FILE_TREE_PATH = "docs/PRD.md";
    private static final String WORKSPACE_READ_PERMISSION = "admin:aiplatform:workspace:read";
    private static final String WORKSPACE_WAKE_PERMISSION = "admin:aiplatform:workspace:wake";
    private static final String WORKSPACE_HIBERNATE_PERMISSION = "admin:aiplatform:workspace:hibernate";
    private static final String WORKSPACE_REBUILD_PERMISSION = "admin:aiplatform:workspace:rebuild";
    private static final String WORKSPACE_SEAL_PERMISSION = "admin:aiplatform:workspace:seal";
    private static final String WORKSPACE_ID = "3829492007777777";
    private static final String WORKSPACES_ENDPOINT = "/api/admin/aiplatform/workspaces";
    private static final String WORKSPACE_DETAIL_ENDPOINT = WORKSPACES_ENDPOINT + "/" + WORKSPACE_ID;
    private static final String WAKE_ENDPOINT = WORKSPACE_DETAIL_ENDPOINT + "/wake";
    private static final String HIBERNATE_ENDPOINT = WORKSPACE_DETAIL_ENDPOINT + "/hibernate";
    private static final String REBUILD_ENDPOINT = WORKSPACE_DETAIL_ENDPOINT + "/rebuild";
    private static final String SEAL_ENDPOINT = WORKSPACE_DETAIL_ENDPOINT + "/seal";
    private static final String COST_READ_PERMISSION = "admin:aiplatform:cost:read";
    private static final String COSTS_ENDPOINT = "/api/admin/aiplatform/costs";
    private static final String COST_FROM = "2026-09-01T00:00:00Z";
    private static final String COST_TO = "2026-09-16T00:00:00Z";
    private static final String COST_WINDOW_QUERY = "?from=" + COST_FROM + "&to=" + COST_TO;
    private static final String PRICE_ENTRY_READ_PERMISSION = "admin:aiplatform:price-entry:read";
    private static final String PRICE_ENTRY_REPRICE_PERMISSION = "admin:aiplatform:price-entry:reprice";
    private static final String PRICE_ENTRY_DEACTIVATE_PERMISSION = "admin:aiplatform:price-entry:deactivate";
    private static final String PRICE_ENTRY_ID = "3830100002222222";
    private static final String PRICE_ENTRIES_ENDPOINT = "/api/admin/aiplatform/price-entries";
    private static final String REPRICE_ENDPOINT = PRICE_ENTRIES_ENDPOINT + "/" + PRICE_ENTRY_ID + "/reprice";
    private static final String DEACTIVATE_ENDPOINT = PRICE_ENTRIES_ENDPOINT + "/" + PRICE_ENTRY_ID + "/deactivate";
    private static final String REPRICE_BODY =
            "{\"unitPrice\":0.0000018,\"currency\":\"USD\",\"effectiveFrom\":\"2026-09-20T00:00:00Z\"}";
    private static final String MATERIAL_READ_PERMISSION = "admin:aiplatform:material:read";
    private static final String MATERIAL_DISABLE_PERMISSION = "admin:aiplatform:material:disable";
    private static final String MATERIAL_ENABLE_PERMISSION = "admin:aiplatform:material:enable";
    private static final String MATERIAL_DELETE_PERMISSION = "admin:aiplatform:material:delete";
    private static final String MATERIAL_ID = "3840600001111111";
    private static final String MATERIALS_ENDPOINT = "/api/admin/aiplatform/materials";
    private static final String MATERIAL_DETAIL_ENDPOINT = MATERIALS_ENDPOINT + "/" + MATERIAL_ID;
    private static final String MATERIAL_DISABLE_ENDPOINT = MATERIAL_DETAIL_ENDPOINT + "/disable";
    private static final String MATERIAL_ENABLE_ENDPOINT = MATERIAL_DETAIL_ENDPOINT + "/enable";
    private static final String MATERIAL_DELETE_ENDPOINT = MATERIAL_DETAIL_ENDPOINT;
    private static final String SKILL_READ_PERMISSION = "admin:aiplatform:skill:read";
    private static final String SKILL_INSTALL_PERMISSION = "admin:aiplatform:skill:install";
    private static final String SKILL_DISABLE_PERMISSION = "admin:aiplatform:skill:disable";
    private static final String SKILL_ENABLE_PERMISSION = "admin:aiplatform:skill:enable";
    private static final String SKILL_UNINSTALL_PERMISSION = "admin:aiplatform:skill:uninstall";
    private static final String SKILLS_ENDPOINT = "/api/admin/aiplatform/skills";
    /** 安装技能 TSID 十进制串柄（opaque 两形制之一）。 */
    private static final String SKILL_ID = "3852300001111111";
    /** 内置技能柄（opaque 两形制之二）——清单行原值原样回传作详情寻址。 */
    private static final String BUILTIN_SKILL_HANDLE = "builtin:prd-writing";
    private static final String SKILL_DETAIL_ENDPOINT = SKILLS_ENDPOINT + "/" + SKILL_ID;
    private static final String SKILL_INSTALL_ENDPOINT = SKILLS_ENDPOINT + "/install";
    private static final String SKILL_DISABLE_ENDPOINT = SKILL_DETAIL_ENDPOINT + "/disable";
    private static final String SKILL_ENABLE_ENDPOINT = SKILL_DETAIL_ENDPOINT + "/enable";
    private static final String SKILL_UNINSTALL_ENDPOINT = SKILL_DETAIL_ENDPOINT;
    private static final String SKILL_INSTALL_BODY =
            "{\"repoUrl\":\"https://github.com/acme/agent-skills\",\"excludeDirs\":[\"deprecated\"]}";
    private static final String SKILL_ASSIGN_PERMISSION = "admin:aiplatform:skill:assign";
    /** 职能槽位键（SkillSlot 三把稳定键之一）：字符串路径参数透传，admin 不预校验。 */
    private static final String SLOT_KEY = "executor";
    private static final String SLOT_ASSIGNMENTS_ENDPOINT = SKILLS_ENDPOINT + "/assignments/" + SLOT_KEY;
    private static final String SLOT_ASSIGN_BODY =
            "{\"skillIds\":[\"3852300001111111\",\"3852300003333333\"]}";
    private static final String SLOT_PRECHECK_ENDPOINT = SLOT_ASSIGNMENTS_ENDPOINT + "/precheck";
    private static final String SKILL_UPDATE_PERMISSION = "admin:aiplatform:skill:update";
    private static final String SKILL_UPDATE_ENDPOINT = SKILLS_ENDPOINT + "/update";
    private static final String SKILL_UPDATE_TRACES_ENDPOINT = SKILLS_ENDPOINT + "/update-traces";
    private static final String SKILL_UPDATE_BODY =
            "{\"sourcePackage\":\"https://github.com/acme/agent-skills\"}";
    private static final String AGENT_CONFIG_READ_PERMISSION = "admin:aiplatform:agent-config:read";
    private static final String AGENT_CONFIG_UPDATE_PERMISSION = "admin:aiplatform:agent-config:update";
    /** 智能体稳定键（AgentProfile 两把之一：main=主智能体）：字符串路径参数透传，admin 不预校验。 */
    private static final String AGENT_KEY = "main";
    private static final String AGENT_CONFIGS_ENDPOINT = "/api/admin/aiplatform/agent-configs";
    private static final String AGENT_CONFIG_ENDPOINT = AGENT_CONFIGS_ENDPOINT + "/" + AGENT_KEY;
    private static final String AGENT_CONFIG_TRACES_ENDPOINT = AGENT_CONFIG_ENDPOINT + "/traces";
    private static final String AGENT_CONFIG_UPDATE_BODY =
            "{\"systemPrompt\":\"你是英语学习助手的主智能体：先判意图，再编排执行体。\","
                    + "\"modelId\":\"deepseek-v4-pro\",\"webSearchEnabled\":true,\"fetchUrlEnabled\":false}";
    private static final String AGENT_TOOL_READ_PERMISSION = "admin:aiplatform:agent-tool:read";
    private static final String AGENT_TOOL_TOGGLE_PERMISSION = "admin:aiplatform:agent-tool:toggle";
    /** 增强工具注册名之一（AgentTool 枚举实值）：web_search=联网搜索——开关代表面。 */
    private static final String TOOL_NAME = "web_search";
    private static final String AGENT_TOOLS_ENDPOINT = "/api/admin/aiplatform/agent-tools";
    private static final String AGENT_TOOL_TOGGLE_ENDPOINT = AGENT_TOOLS_ENDPOINT + "/" + TOOL_NAME;
    private static final String AGENT_TOOL_TOGGLE_BODY = "{\"enabled\":false}";
    private static final String SKILL_DRAFT_READ_PERMISSION = "admin:aiplatform:skill-draft:read";
    private static final String SKILL_DRAFT_PROMOTE_PERMISSION = "admin:aiplatform:skill-draft:promote";
    private static final String SKILL_DRAFT_REJECT_PERMISSION = "admin:aiplatform:skill-draft:reject";
    /** 草稿 TSID 十进制串柄（透传不解析不预校验——畸形/未寻址由 provider 判 404 SKL_015）。 */
    private static final String DRAFT_ID = "3852400001111111";
    private static final String SKILL_DRAFTS_ENDPOINT = SKILLS_ENDPOINT + "/drafts";
    private static final String SKILL_DRAFT_DETAIL_ENDPOINT = SKILL_DRAFTS_ENDPOINT + "/" + DRAFT_ID;
    private static final String SKILL_DRAFT_PROMOTE_ENDPOINT = SKILL_DRAFT_DETAIL_ENDPOINT + "/promote";
    private static final String SKILL_DRAFT_REJECT_ENDPOINT = SKILL_DRAFT_DETAIL_ENDPOINT + "/reject";
    private static final String SKILL_DRAFT_REJECT_BODY =
            "{\"reason\":\"正文与现有技能 prd-writing 方法论重叠，解消方向不明\"}";

    private final AdminUserManagementAppService userAppService;
    private final RoleManagementAppService roleAppService;
    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final int port;

    @MockBean
    private AiplatformClient aiplatformClient;

    private String usernameWithReadPermission;
    private String usernameWithoutPermission;
    private String usernameWithWorkspaceWrite;
    private String usernameWithHibernateOnly;
    private String usernameWithPriceWrite;
    private String usernameWithRepriceOnly;
    private String usernameWithMaterialWrite;
    private String usernameWithDisableOnly;
    private String usernameWithOrderWrite;
    private String usernameWithQuoteOnly;
    private String usernameWithSkillWrite;
    private String usernameWithSkillDisableOnly;
    private String usernameWithSkillAssign;
    private String usernameWithSkillUpdate;
    private String usernameWithAgentConfigUpdate;
    private String usernameWithAgentToolToggle;
    private String usernameWithSkillDraftWrite;
    private String usernameWithDraftPromoteOnly;

    // 在 mocked client 调用瞬间抓取 RequestContext——证明 operator 身份抵达出站调用点
    // （供 OpenApiClient 带 X-User-Id/X-User-Name 出站头，issue #66/#69/#70 断言必带）
    private final AtomicReference<Long> capturedOperatorId = new AtomicReference<>();
    private final AtomicReference<String> capturedOperatorName = new AtomicReference<>();
    private Long workspaceWriteUserId;
    private String workspaceWriteUserNickname;
    private Long priceWriteUserId;
    private String priceWriteUserNickname;
    private Long materialWriteUserId;
    private String materialWriteUserNickname;
    private Long orderWriteUserId;
    private String orderWriteUserNickname;
    private Long skillWriteUserId;
    private String skillWriteUserNickname;
    private Long skillAssignUserId;
    private String skillAssignUserNickname;
    private Long skillUpdateUserId;
    private String skillUpdateUserNickname;
    private Long agentConfigUpdateUserId;
    private String agentConfigUpdateUserNickname;
    private Long agentToolToggleUserId;
    private String agentToolToggleUserNickname;
    private Long skillDraftWriteUserId;
    private String skillDraftWriteUserNickname;

    @Autowired
    AiplatformRbacEnforcementIntegrationTest(
            AdminUserManagementAppService userAppService,
            RoleManagementAppService roleAppService,
            TestRestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${local.server.port}") int port) {
        this.userAppService = userAppService;
        this.roleAppService = roleAppService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.port = port;
    }

    @BeforeEach
    void setUp() {
        capturedOperatorId.set(null);
        capturedOperatorName.set(null);

        String suffix = uuidSuffix();
        usernameWithReadPermission = "aiplaread" + suffix;
        usernameWithoutPermission = "aiplanone" + suffix;
        usernameWithWorkspaceWrite = "aiplawspr" + suffix;
        usernameWithHibernateOnly = "aiplahibr" + suffix;
        usernameWithPriceWrite = "aiplapwpr" + suffix;
        usernameWithRepriceOnly = "aiplarpon" + suffix;
        usernameWithMaterialWrite = "aiplamwpr" + suffix;
        usernameWithDisableOnly = "aipladson" + suffix;
        usernameWithOrderWrite = "aiplaowpr" + suffix;
        usernameWithQuoteOnly = "aiplaqpon" + suffix;
        usernameWithSkillWrite = "aiplaswpr" + suffix;
        usernameWithSkillDisableOnly = "aiplasdon" + suffix;
        usernameWithSkillAssign = "aiplasaan" + suffix;
        usernameWithSkillUpdate = "aiplasupn" + suffix;
        usernameWithAgentConfigUpdate = "aiplaacup" + suffix;
        usernameWithAgentToolToggle = "aiplaatog" + suffix;
        usernameWithSkillDraftWrite = "aiplasdwpr" + suffix;
        usernameWithDraftPromoteOnly = "aiplasdpo" + suffix;

        Long readRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台读权限_" + suffix, "AIPLAREAD_" + suffix,
                        "AI 平台账号档案 + 订单 + 项目 + 沙箱 + 成本 + 单价表 + 知识素材 + 智能体配置 + 工具面读权限", 80, null));
        roleAppService.assignPermissions(readRoleId,
                new AssignPermissionsCommand(List.of(READ_PERMISSION, ORDER_READ_PERMISSION,
                        PROJECT_READ_PERMISSION, WORKSPACE_READ_PERMISSION, COST_READ_PERMISSION,
                        PRICE_ENTRY_READ_PERMISSION, MATERIAL_READ_PERMISSION, SKILL_READ_PERMISSION,
                        AGENT_CONFIG_READ_PERMISSION, AGENT_TOOL_READ_PERMISSION,
                        SKILL_DRAFT_READ_PERMISSION)));

        Long userWithReadId = userAppService.create(
                new CreateAdminUserCommand(usernameWithReadPermission, PASSWORD, "只读运营_" + suffix, null, null, null));
        userAppService.assignRoles(userWithReadId, new AssignRolesCommand(List.of(readRoleId)));

        userAppService.create(
                new CreateAdminUserCommand(usernameWithoutPermission, PASSWORD, "无权限运营_" + suffix, null, null, null));

        // 沙箱四写码专用运营（全四写码，无读码）——操作者透传断言用其 id/昵称
        Long workspaceWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台沙箱写操作_" + suffix, "AIPLAWSWR_" + suffix,
                        "AI 平台沙箱四干预动作权限", 90, null));
        roleAppService.assignPermissions(workspaceWriteRoleId, new AssignPermissionsCommand(List.of(
                WORKSPACE_WAKE_PERMISSION, WORKSPACE_HIBERNATE_PERMISSION,
                WORKSPACE_REBUILD_PERMISSION, WORKSPACE_SEAL_PERMISSION)));
        workspaceWriteUserNickname = "沙箱治理运营_" + suffix;
        workspaceWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithWorkspaceWrite, PASSWORD,
                        workspaceWriteUserNickname, null, null, null));
        userAppService.assignRoles(workspaceWriteUserId, new AssignRolesCommand(List.of(workspaceWriteRoleId)));

        // 仅 hibernate 单写码——钉死四写码彼此独立（spec #62 最小授权）
        Long hibernateOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台沙箱休眠_" + suffix, "AIPLAHIBR_" + suffix,
                        "AI 平台沙箱仅强制休眠权限", 95, null));
        roleAppService.assignPermissions(hibernateOnlyRoleId,
                new AssignPermissionsCommand(List.of(WORKSPACE_HIBERNATE_PERMISSION)));
        Long hibernateOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithHibernateOnly, PASSWORD,
                        "休眠专员_" + suffix, null, null, null));
        userAppService.assignRoles(hibernateOnlyUserId, new AssignRolesCommand(List.of(hibernateOnlyRoleId)));

        // 单价表双写码专用运营（reprice+deactivate，无读码）——操作者透传断言用其 id/昵称
        Long priceWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台单价表写操作_" + suffix, "AIPLAPWPR_" + suffix,
                        "AI 平台单价表改价 + 停用权限", 96, null));
        roleAppService.assignPermissions(priceWriteRoleId, new AssignPermissionsCommand(List.of(
                PRICE_ENTRY_REPRICE_PERMISSION, PRICE_ENTRY_DEACTIVATE_PERMISSION)));
        priceWriteUserNickname = "调价运营_" + suffix;
        priceWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithPriceWrite, PASSWORD,
                        priceWriteUserNickname, null, null, null));
        userAppService.assignRoles(priceWriteUserId, new AssignRolesCommand(List.of(priceWriteRoleId)));

        // 仅 reprice 单写码——钉死改价/停用两写码彼此独立（spec #62 最小授权）
        Long repriceOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台单价表改价_" + suffix, "AIPLARPON_" + suffix,
                        "AI 平台单价表仅改价权限", 97, null));
        roleAppService.assignPermissions(repriceOnlyRoleId,
                new AssignPermissionsCommand(List.of(PRICE_ENTRY_REPRICE_PERMISSION)));
        Long repriceOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithRepriceOnly, PASSWORD,
                        "调价专员_" + suffix, null, null, null));
        userAppService.assignRoles(repriceOnlyUserId, new AssignRolesCommand(List.of(repriceOnlyRoleId)));

        // 知识素材三写码专用运营（disable+enable+delete，无读码）——操作者透传断言用其 id/昵称
        Long materialWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台知识素材写操作_" + suffix, "AIPLAMWPR_" + suffix,
                        "AI 平台知识素材停用 + 启用 + 删除权限", 98, null));
        roleAppService.assignPermissions(materialWriteRoleId, new AssignPermissionsCommand(List.of(
                MATERIAL_DISABLE_PERMISSION, MATERIAL_ENABLE_PERMISSION, MATERIAL_DELETE_PERMISSION)));
        materialWriteUserNickname = "内容治理运营_" + suffix;
        materialWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithMaterialWrite, PASSWORD,
                        materialWriteUserNickname, null, null, null));
        userAppService.assignRoles(materialWriteUserId, new AssignRolesCommand(List.of(materialWriteRoleId)));

        // 仅 disable 单写码——钉死三写码彼此独立（spec #62 最小授权：可逆治理与不可逆删除分权）
        Long disableOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台知识素材停用_" + suffix, "AIPLADSON_" + suffix,
                        "AI 平台知识素材仅停用权限", 99, null));
        roleAppService.assignPermissions(disableOnlyRoleId,
                new AssignPermissionsCommand(List.of(MATERIAL_DISABLE_PERMISSION)));
        Long disableOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithDisableOnly, PASSWORD,
                        "素材停用专员_" + suffix, null, null, null));
        userAppService.assignRoles(disableOnlyUserId, new AssignRolesCommand(List.of(disableOnlyRoleId)));

        // 订单三写码专用运营（quote+cancel+retry-archive，无读码）——操作者透传断言用其 id/昵称
        Long orderWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台订单写操作_" + suffix, "AIPLAOWPR_" + suffix,
                        "AI 平台订单报价改价 + 取消 + 重试归档权限", 93, null));
        roleAppService.assignPermissions(orderWriteRoleId, new AssignPermissionsCommand(List.of(
                ORDER_QUOTE_PERMISSION, ORDER_CANCEL_PERMISSION, ORDER_RETRY_ARCHIVE_PERMISSION)));
        orderWriteUserNickname = "交易监管运营_" + suffix;
        orderWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithOrderWrite, PASSWORD,
                        orderWriteUserNickname, null, null, null));
        userAppService.assignRoles(orderWriteUserId, new AssignRolesCommand(List.of(orderWriteRoleId)));

        // 仅 quote 单写码——钉死三写码彼此独立（spec #62 最小授权：定价与处置/归档分权）
        Long quoteOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台订单报价_" + suffix, "AIPLAQPON_" + suffix,
                        "AI 平台订单仅报价改价权限", 94, null));
        roleAppService.assignPermissions(quoteOnlyRoleId,
                new AssignPermissionsCommand(List.of(ORDER_QUOTE_PERMISSION)));
        Long quoteOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithQuoteOnly, PASSWORD,
                        "定价专员_" + suffix, null, null, null));
        userAppService.assignRoles(quoteOnlyUserId, new AssignRolesCommand(List.of(quoteOnlyRoleId)));

        // 技能四写码专用运营（install+disable+enable+uninstall，无读码）——操作者透传断言用其 id/昵称
        Long skillWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台技能写操作_" + suffix, "AIPLASWPR_" + suffix,
                        "AI 平台技能安装 + 停用 + 启用 + 卸载权限", 100, null));
        roleAppService.assignPermissions(skillWriteRoleId, new AssignPermissionsCommand(List.of(
                SKILL_INSTALL_PERMISSION, SKILL_DISABLE_PERMISSION,
                SKILL_ENABLE_PERMISSION, SKILL_UNINSTALL_PERMISSION)));
        skillWriteUserNickname = "技能治理运营_" + suffix;
        skillWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithSkillWrite, PASSWORD,
                        skillWriteUserNickname, null, null, null));
        userAppService.assignRoles(skillWriteUserId, new AssignRolesCommand(List.of(skillWriteRoleId)));

        // 仅 disable 单写码——钉死四写码彼此独立（spec #79 最小授权：可逆治理与装/卸分权）
        Long skillDisableOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台技能停用_" + suffix, "AIPLASDON_" + suffix,
                        "AI 平台技能仅停用权限", 101, null));
        roleAppService.assignPermissions(skillDisableOnlyRoleId,
                new AssignPermissionsCommand(List.of(SKILL_DISABLE_PERMISSION)));
        Long skillDisableOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithSkillDisableOnly, PASSWORD,
                        "技能停用专员_" + suffix, null, null, null));
        userAppService.assignRoles(skillDisableOnlyUserId, new AssignRolesCommand(List.of(skillDisableOnlyRoleId)));

        // 仅 assign 单写码（无读码）——钉死指派读面复用 read、写面独立 assign（issue #82）；
        // 操作者透传断言用其 id/昵称（provider 整包替换留最近动作者）
        Long skillAssignRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台槽位指派_" + suffix, "AIPLASAAN_" + suffix,
                        "AI 平台技能槽位指派权限", 102, null));
        roleAppService.assignPermissions(skillAssignRoleId,
                new AssignPermissionsCommand(List.of(SKILL_ASSIGN_PERMISSION)));
        skillAssignUserNickname = "装配运营_" + suffix;
        skillAssignUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithSkillAssign, PASSWORD,
                        skillAssignUserNickname, null, null, null));
        userAppService.assignRoles(skillAssignUserId, new AssignRolesCommand(List.of(skillAssignRoleId)));

        // 仅 update 单写码（无读码）——钉死更新写面独立 update、留痕读面复用 read（issue #83）；
        // 操作者透传断言用其 id/昵称（provider 显式动作必留痕）
        Long skillUpdateRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台技能更新_" + suffix, "AIPLASUDN_" + suffix,
                        "AI 平台技能包显式更新权限", 103, null));
        roleAppService.assignPermissions(skillUpdateRoleId,
                new AssignPermissionsCommand(List.of(SKILL_UPDATE_PERMISSION)));
        skillUpdateUserNickname = "版本治理运营_" + suffix;
        skillUpdateUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithSkillUpdate, PASSWORD,
                        skillUpdateUserNickname, null, null, null));
        userAppService.assignRoles(skillUpdateUserId, new AssignRolesCommand(List.of(skillUpdateRoleId)));

        // 仅 agent-config:update 单写码（无读码）——钉死配置写面独立 update、读面/留痕读面复用
        // read（issue #84 高敏感独立授权面）；操作者透传断言用其 id/昵称（provider 配置变更必留痕）
        Long agentConfigUpdateRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台智能体配置_" + suffix, "AIPLAACUP_" + suffix,
                        "AI 平台智能体配置维护权限", 104, null));
        roleAppService.assignPermissions(agentConfigUpdateRoleId,
                new AssignPermissionsCommand(List.of(AGENT_CONFIG_UPDATE_PERMISSION)));
        agentConfigUpdateUserNickname = "智能体配置运营_" + suffix;
        agentConfigUpdateUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithAgentConfigUpdate, PASSWORD,
                        agentConfigUpdateUserNickname, null, null, null));
        userAppService.assignRoles(agentConfigUpdateUserId, new AssignRolesCommand(List.of(agentConfigUpdateRoleId)));

        // 仅 agent-tool:toggle 单写码（无读码）——钉死工具面清单读面与开关写面两码分权（issue #85）；
        // 操作者透传断言用其 id/昵称（provider 开关写必留痕、缺头必拦 PRJ_034）
        Long agentToolToggleRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台工具面开关_" + suffix, "AIPLAATOG_" + suffix,
                        "AI 平台增强工具开关权限", 105, null));
        roleAppService.assignPermissions(agentToolToggleRoleId,
                new AssignPermissionsCommand(List.of(AGENT_TOOL_TOGGLE_PERMISSION)));
        agentToolToggleUserNickname = "工具面运营_" + suffix;
        agentToolToggleUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithAgentToolToggle, PASSWORD,
                        agentToolToggleUserNickname, null, null, null));
        userAppService.assignRoles(agentToolToggleUserId, new AssignRolesCommand(List.of(agentToolToggleRoleId)));

        // 草稿两写码专用运营（promote+reject，无读码）——钉死 skill-draft 读写不串（issue #92）；
        // 操作者透传断言用其 id/昵称（provider 晋升/拒绝必留痕、缺头必拦 SKL_009）
        Long skillDraftWriteRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台草稿审核写操作_" + suffix, "AIPLASDWR_" + suffix,
                        "AI 平台技能草稿晋升 + 拒绝权限", 106, null));
        roleAppService.assignPermissions(skillDraftWriteRoleId, new AssignPermissionsCommand(List.of(
                SKILL_DRAFT_PROMOTE_PERMISSION, SKILL_DRAFT_REJECT_PERMISSION)));
        skillDraftWriteUserNickname = "技能审核员_" + suffix;
        skillDraftWriteUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithSkillDraftWrite, PASSWORD,
                        skillDraftWriteUserNickname, null, null, null));
        userAppService.assignRoles(skillDraftWriteUserId, new AssignRolesCommand(List.of(skillDraftWriteRoleId)));

        // 仅 promote 单写码——钉死两写码彼此独立（spec #91：晋升权与拒绝权独立授受——
        // 有采纳权而无否决权，反之亦然）
        Long draftPromoteOnlyRoleId = roleAppService.create(
                new CreateRoleCommand("AI平台草稿晋升_" + suffix, "AIPLASDPO_" + suffix,
                        "AI 平台技能草稿仅晋升权限", 107, null));
        roleAppService.assignPermissions(draftPromoteOnlyRoleId,
                new AssignPermissionsCommand(List.of(SKILL_DRAFT_PROMOTE_PERMISSION)));
        Long draftPromoteOnlyUserId = userAppService.create(
                new CreateAdminUserCommand(usernameWithDraftPromoteOnly, PASSWORD,
                        "草稿晋升专员_" + suffix, null, null, null));
        userAppService.assignRoles(draftPromoteOnlyUserId, new AssignRolesCommand(List.of(draftPromoteOnlyRoleId)));

        // 200 用例：aiplatform 下游 mock，证明通路接通（不依赖真实 aiplatform 服务）
        when(aiplatformClient.getAccountProfile(eq(EXTERNAL_ID))).thenReturn(
                new AiplatformAccountProfileWireResponse(
                        "3829492001234567", EXTERNAL_ID, "文野",
                        LocalDateTime.of(2026, 1, 15, 10, 30, 0)));
        when(aiplatformClient.listOrders(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
                List.of(new AiplatformOrderSummaryWireResponse(
                        ORDER_ID, "3829492007654321", "英语学习助手", EXTERNAL_ID, "文野",
                        3, "已支付", 3, "系统＋设计", 1999000L, "CNY",
                        LocalDateTime.of(2026, 9, 10, 14, 20, 0),
                        LocalDateTime.of(2026, 9, 11, 9, 0, 0))), 42, 3, 20));
        when(aiplatformClient.getOrder(eq(ORDER_ID))).thenReturn(new AiplatformOrderDetailWireResponse(
                ORDER_ID, "3829492007654321", "英语学习助手", EXTERNAL_ID, "文野",
                2, "已报价", 1, "设计", 2199000L, "CNY", "含加急费用",
                List.of(new AiplatformPriceEntryWireResponse(
                        "3829492011111111", 2199000L, "CNY", "含加急费用",
                        "1234567890123456", "报价运营", LocalDateTime.of(2026, 9, 12, 10, 0, 0))),
                "# PRD\n\n做一个英语学习助手……",
                LocalDateTime.of(2026, 9, 10, 14, 20, 0), LocalDateTime.of(2026, 9, 11, 9, 0, 0),
                null, null, null, null, null, null, null, null));
        when(aiplatformClient.downloadSourcePackage(eq(ORDER_ID))).thenReturn(new BinaryResponse(200,
                java.net.http.HttpHeaders.of(Map.of(
                        "Content-Type", List.of("application/gzip"),
                        "Content-Disposition", List.of("attachment; filename=\"" + ORDER_ID + "-source.tar.gz\"")),
                        (a, b) -> true),
                new byte[] {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00}));
        // 设计资产包 mock（#96 T1）：对偶源码包——design 后缀冻结件（不按支付态门控，交付裁决归 provider）
        when(aiplatformClient.downloadDesignPackage(eq(ORDER_ID))).thenReturn(new BinaryResponse(200,
                java.net.http.HttpHeaders.of(Map.of(
                        "Content-Type", List.of("application/gzip"),
                        "Content-Disposition", List.of("attachment; filename=\"" + ORDER_ID + "-design.tar.gz\"")),
                        (a, b) -> true),
                new byte[] {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00}));
        when(aiplatformClient.listProjects(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(
                new AiplatformProjectSummaryWireResponse(
                        PROJECT_ID, "英语学习助手", EXTERNAL_ID, "文野",
                        1, "官网", 3, "已归档", true,
                        LocalDateTime.of(2026, 8, 1, 9, 0, 0),
                        LocalDateTime.of(2026, 9, 1, 12, 0, 0))), 7, 2, 50));
        when(aiplatformClient.getProject(eq(PROJECT_ID))).thenReturn(new AiplatformProjectDetailWireResponse(
                PROJECT_ID, "英语学习助手", EXTERNAL_ID, "文野", "3829492009999999",
                1, "官网", 1, "进行中", false,
                LocalDateTime.of(2026, 9, 10, 14, 20, 0), LocalDateTime.of(2026, 9, 14, 18, 30, 0),
                LocalDateTime.of(2026, 9, 10, 20, 0, 0), LocalDateTime.of(2026, 9, 11, 8, 0, 0),
                new AiplatformOrderBriefWireResponse(ORDER_ID, 2, "已报价"),
                new AiplatformOrderBriefWireResponse("3829492005555444", 4, "已归档"),
                new AiplatformProjectDetailWireResponse.CostSummary(Map.of("CNY", new BigDecimal("12.3456")), true)));
        when(aiplatformClient.getConversation(eq(PROJECT_ID))).thenReturn(List.of(
                new AiplatformConversationEntryWireResponse(
                        90001L, 1, "用户发言", null, "首页加一个轮播图",
                        null, null, null, null, false, LocalDateTime.of(2026, 9, 12, 10, 0, 0)),
                new AiplatformConversationEntryWireResponse(
                        90003L, 5, "收尾卡", "run-abc123", null,
                        null, Map.of("runId", "run-abc123", "commitHash", VERSION_HASH), null, null, false,
                        LocalDateTime.of(2026, 9, 12, 11, 30, 0))));
        when(aiplatformClient.getPrd(eq(PROJECT_ID))).thenReturn(new AiplatformPrdWireResponse(
                PROJECT_ID, "# PRD\n\n做一个英语学习助手……", Instant.parse("2026-09-12T08:30:00Z")));
        when(aiplatformClient.listVersions(eq(PROJECT_ID))).thenReturn(List.of(new AiplatformVersionWireResponse(
                VERSION_HASH, "轮播图上线", "run-abc123", null, LocalDateTime.of(2026, 9, 12, 11, 30, 0))));
        when(aiplatformClient.getVersion(eq(PROJECT_ID), eq(VERSION_HASH))).thenReturn(
                new AiplatformVersionDetailWireResponse(
                        VERSION_HASH, "轮播图上线", "run-abc123", null,
                        LocalDateTime.of(2026, 9, 12, 11, 30, 0),
                        Map.of("runId", "run-abc123", "commitHash", VERSION_HASH)));
        // 文件区 mock（issue #71）：文件树 + 文件内容 + 文件包（封存包态——两态文件名由 provider 决定）
        when(aiplatformClient.getProjectFiles(eq(PROJECT_ID))).thenReturn(
                new AiplatformProjectFilesWireResponse(PROJECT_ID, List.of(
                        new AiplatformProjectFilesWireResponse.FileEntry("README.md", 512L),
                        new AiplatformProjectFilesWireResponse.FileEntry(FILE_TREE_PATH, 4096L))));
        when(aiplatformClient.getProjectFileContent(eq(PROJECT_ID), eq(FILE_TREE_PATH)))
                .thenReturn(new AiplatformProjectFileContentWireResponse(
                        FILE_TREE_PATH, "# PRD\n\n做一个英语学习助手……"));
        when(aiplatformClient.downloadProjectFilesPackage(eq(PROJECT_ID))).thenReturn(new BinaryResponse(200,
                java.net.http.HttpHeaders.of(Map.of(
                        "Content-Type", List.of("application/gzip"),
                        "Content-Disposition", List.of("attachment; filename=\"" + PROJECT_ID + "-archive.tar.gz\"")),
                        (a, b) -> true),
                new byte[] {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00}));
        // 沙箱观测面 mock：漂移行（期望运行而实态无容器）+ 封存态详情（资源观测 + 项目引用）
        when(aiplatformClient.listWorkspaces(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(
                new AiplatformWorkspaceSummaryWireResponse(
                        WORKSPACE_ID, "ws-" + WORKSPACE_ID,
                        1, "开发", 2, "就绪", 1, "运行", 3, "无容器",
                        LocalDateTime.of(2026, 9, 14, 22, 10, 0),
                        2147483648L, null, null,
                        new AiplatformWorkspaceSummaryWireResponse.ProjectRef(
                                PROJECT_ID, "英语学习助手", false))), 17, 2, 20));
        when(aiplatformClient.getWorkspace(eq(WORKSPACE_ID))).thenReturn(
                workspaceActionReceiptWire(3, "封存", 3, "无容器",
                        "workspace-archives/" + WORKSPACE_ID + ".tar.gz", 89128960L));
        // 四干预动作 mock：回执详情 + 抓取 RequestContext（操作者透传契约证据——issue #66 断言必带）
        Runnable capture = () -> {
            capturedOperatorId.set(RequestContext.getUserId());
            capturedOperatorName.set(RequestContext.getUserName());
        };
        // 订单写路径 mock：三操作回执（用户面同构 OrderResponse）+ 抓取 RequestContext（操作者
        // 透传契约证据——issue #70 断言必带；provider 落痕价目行/订单行，缺头落空）
        doAnswer(inv -> {
            capture.run();
            return orderReceiptWire(2, "已报价", null, null, null);
        }).when(aiplatformClient).quoteOrder(eq(ORDER_ID), any(AiplatformOrderQuoteWireRequest.class));
        doAnswer(inv -> {
            capture.run();
            return orderReceiptWire(5, "已取消", LocalDateTime.of(2026, 9, 16, 15, 0, 0), null, null);
        }).when(aiplatformClient).cancelOrder(eq(ORDER_ID), any(AiplatformOrderCancelWireRequest.class));
        doAnswer(inv -> {
            capture.run();
            return orderReceiptWire(4, "已归档", null,
                    LocalDateTime.of(2026, 9, 15, 20, 0, 0), LocalDateTime.of(2026, 9, 16, 9, 30, 0));
        }).when(aiplatformClient).retryArchiveOrder(eq(ORDER_ID));
        doAnswer(inv -> {
            capture.run();
            return workspaceActionReceiptWire(1, "运行", 1, "运行中", null, null);
        }).when(aiplatformClient).wakeWorkspace(eq(WORKSPACE_ID));
        doAnswer(inv -> {
            capture.run();
            return workspaceActionReceiptWire(2, "休眠", 3, "无容器", null, null);
        }).when(aiplatformClient).hibernateWorkspace(eq(WORKSPACE_ID));
        doAnswer(inv -> {
            capture.run();
            return workspaceActionReceiptWire(1, "运行", 1, "运行中", null, null);
        }).when(aiplatformClient).rebuildWorkspace(eq(WORKSPACE_ID));
        doAnswer(inv -> {
            capture.run();
            return workspaceActionReceiptWire(3, "封存", 3, "无容器",
                    "workspace-archives/" + WORKSPACE_ID + ".tar.gz", 89128960L);
        }).when(aiplatformClient).sealWorkspace(eq(WORKSPACE_ID));
        // 成本四读口 mock：总览（五档总量 + 币种分桶 + 双分解）/ unpriced 警示 / 项目成本清单 / 单项目下钻
        Instant costFrom = Instant.parse(COST_FROM);
        Instant costTo = Instant.parse(COST_TO);
        when(aiplatformClient.getCostOverview(eq(new AiplatformCostWindowWireRequest(costFrom, costTo))))
                .thenReturn(new AiplatformCostOverviewWireResponse(
                        costFrom, costTo,
                        new AiplatformTokenUsageWireResponse(5000, 1200, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")),
                        List.of(new AiplatformCostOverviewWireResponse.ModelUsage(
                                "anthropic", "claude-fable-5",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800))),
                        List.of(new AiplatformCostOverviewWireResponse.AgentKindUsage(
                                "naming", null,
                                new AiplatformTokenUsageWireResponse(1000, 200, 0, 0, 0)))));
        when(aiplatformClient.getUnpricedUsage(eq(new AiplatformCostWindowWireRequest(costFrom, costTo))))
                .thenReturn(new AiplatformUnpricedUsageWireResponse(
                        costFrom, costTo,
                        List.of(new AiplatformUnpricedUsageWireResponse.UnpricedTier(
                                "openai", "gpt-5.2", 1, "输入", 700))));
        when(aiplatformClient.listProjectCosts(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(
                new AiplatformProjectCostWireResponse(
                        PROJECT_ID,
                        new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")), false),
                new AiplatformProjectCostWireResponse(
                        "3829492005555444",
                        new AiplatformTokenUsageWireResponse(1000, 200, 0, 0, 0),
                        Map.of(), true)), 2, 1, 20));
        when(aiplatformClient.getProjectCostDetail(eq(PROJECT_ID),
                eq(new AiplatformCostWindowWireRequest(costFrom, costTo))))
                .thenReturn(new AiplatformProjectCostDetailWireResponse(
                        PROJECT_ID, costFrom, costTo,
                        new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")),
                        List.of(new AiplatformProjectCostDetailWireResponse.UnpricedTier(
                                "openai", "gpt-5.2", 1, "输入")),
                        List.of(new AiplatformProjectCostDetailWireResponse.ModelUsage(
                                "anthropic", "claude-fable-5",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800))),
                        List.of(new AiplatformProjectCostDetailWireResponse.AgentKindUsage(
                                "executor", "执行智能体",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800)))));
        // 单价表域 mock：清单（当前行 + 历史行）/ 改价双行回执（抓取 RequestContext——操作者透传
        // 契约证据，同沙箱四动作）/ 停用单行回执
        when(aiplatformClient.listPriceEntries(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(
                new AiplatformUnitPriceEntryWireResponse(
                        PRICE_ENTRY_ID, "anthropic", "claude-fable-5", 1, "输入",
                        "0.000002", "USD",
                        Instant.parse("2026-08-01T00:00:00Z"), null, "700160", "运营·单价管理员"),
                new AiplatformUnitPriceEntryWireResponse(
                        "3830100001111111", "anthropic", "claude-fable-5", 1, "输入",
                        "0.00000132", "USD",
                        Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
                        null, null)), 12, 2, 20));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformUnitPriceEntryRepriceWireResponse(
                    new AiplatformUnitPriceEntryWireResponse(
                            PRICE_ENTRY_ID, "anthropic", "claude-fable-5", 1, "输入",
                            "0.000002", "USD",
                            Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-20T00:00:00Z"),
                            "700160", "运营·单价管理员"),
                    new AiplatformUnitPriceEntryWireResponse(
                            "3830100003333333", "anthropic", "claude-fable-5", 1, "输入",
                            "0.0000018", "USD",
                            Instant.parse("2026-09-20T00:00:00Z"), null,
                            String.valueOf(priceWriteUserId), priceWriteUserNickname));
        }).when(aiplatformClient).repricePriceEntry(eq(PRICE_ENTRY_ID), any(AiplatformPriceEntryRepriceWireRequest.class));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformUnitPriceEntryWireResponse(
                    PRICE_ENTRY_ID, "anthropic", "claude-fable-5", 1, "输入",
                    "0.000002", "USD",
                    Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-16T10:30:00Z"),
                    String.valueOf(priceWriteUserId), priceWriteUserNickname);
        }).when(aiplatformClient).deactivatePriceEntry(eq(PRICE_ENTRY_ID));
        // 知识素材域 mock：清单（已治理行 + 未治理行）/ 详情（PRD 全文）/ 三治理动作回执（抓取
        // RequestContext——操作者透传契约证据，provider 缺头必拦 KNW_006，issue #69 断言必带）
        when(aiplatformClient.listMaterials(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(
                new AiplatformMaterialSummaryWireResponse(
                        MATERIAL_ID, "PRD", PROJECT_ID, "英语学习助手", "英语学习助手 · PRD",
                        2, "停用", Instant.parse("2026-09-01T08:30:00Z"),
                        String.valueOf(materialWriteUserId), materialWriteUserNickname),
                new AiplatformMaterialSummaryWireResponse(
                        "3840600002222222", "PRD", "3829492005555444", "待办清单应用",
                        "待办清单应用 · PRD", 1, "启用", Instant.parse("2026-08-20T12:00:00Z"),
                        null, null)), 9, 2, 20));
        when(aiplatformClient.getMaterial(eq(MATERIAL_ID))).thenReturn(new AiplatformMaterialDetailWireResponse(
                MATERIAL_ID, "PRD", PROJECT_ID, "英语学习助手", "英语学习助手 · PRD",
                1, "启用", Instant.parse("2026-09-01T08:30:00Z"), null, null,
                "# PRD\n\n做一个英语学习助手……\n\n## 功能范围\n\n1. 单词卡片\n2. 复习计划"));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformMaterialSummaryWireResponse(
                    MATERIAL_ID, "PRD", PROJECT_ID, "英语学习助手", "英语学习助手 · PRD",
                    2, "停用", Instant.parse("2026-09-01T08:30:00Z"),
                    String.valueOf(materialWriteUserId), materialWriteUserNickname);
        }).when(aiplatformClient).disableMaterial(eq(MATERIAL_ID));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformMaterialSummaryWireResponse(
                    MATERIAL_ID, "PRD", PROJECT_ID, "英语学习助手", "英语学习助手 · PRD",
                    1, "启用", Instant.parse("2026-09-01T08:30:00Z"),
                    String.valueOf(materialWriteUserId), materialWriteUserNickname);
        }).when(aiplatformClient).enableMaterial(eq(MATERIAL_ID));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformMaterialSummaryWireResponse(
                    MATERIAL_ID, "PRD", PROJECT_ID, "英语学习助手", "英语学习助手 · PRD",
                    2, "停用", Instant.parse("2026-09-01T08:30:00Z"),
                    String.valueOf(materialWriteUserId), materialWriteUserNickname);
        }).when(aiplatformClient).deleteMaterial(eq(MATERIAL_ID));
        // 技能管理域 mock（issue #80，aiplatform #247 技能线 T1）：清单两行（内置 builtin: 柄 +
        // 安装 TSID 柄——opaque 两形制代表面）/ 详情（frontmatter 自由键值 + 正文全文，审核面）；
        // 使用计数观测面（#93 T2 追平）：内置行恒 null 不适用、安装行实值（已加载过代表面）
        when(aiplatformClient.listSkills()).thenReturn(List.of(
                new AiplatformSkillSummaryWireResponse(
                        BUILTIN_SKILL_HANDLE, "prd-writing", "撰写或修订 PRD 时使用",
                        1, "内置", null, null,
                        1, "启用", null, null, null,
                        null, null),
                new AiplatformSkillSummaryWireResponse(
                        SKILL_ID, "web-research", "联网检索与网页抓取整理",
                        2, "安装", "https://github.com/acme/agent-skills",
                        "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                        2, "停用", "700200", "运营·技能管理员", true,
                        5L, LocalDateTime.of(2026, 9, 20, 10, 0, 0))));
        when(aiplatformClient.getSkill(eq(SKILL_ID))).thenReturn(new AiplatformSkillDetailWireResponse(
                SKILL_ID, "web-research", "联网检索与网页抓取整理",
                2, "安装", "https://github.com/acme/agent-skills",
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                1, "启用",
                Map.of("name", "web-research", "description", "联网检索与网页抓取整理", "license", "MIT"),
                "# Web Research\n\n先检索后作答……\n\n## 步骤\n\n1. 拆解问题\n2. 检索来源",
                Map.of("scripts/run-tests.sh", "#!/bin/bash\nset -e\n"),
                "700200", "运营·技能管理员", false,
                5L, LocalDateTime.of(2026, 9, 20, 10, 0, 0)));
        // 技能四写口 mock（issue #81，aiplatform #248 技能线 T2）：安装回执＝本次装入条目清单
        // （裸 List，装者留痕）/ 启停回执＝翻转后清单行 / 卸载回执＝删除前终态——均抓取
        // RequestContext（操作者透传契约证据，provider 缺头必拦 SKL_009，断言必带）
        doAnswer(inv -> {
            capture.run();
            return List.of(
                    skillActionReceiptWire("3852300002222222", 1, "启用"),
                    skillActionReceiptWire("3852300003333333", 1, "启用"));
        }).when(aiplatformClient).installSkills(any(AiplatformSkillInstallWireRequest.class));
        doAnswer(inv -> {
            capture.run();
            return skillActionReceiptWire(SKILL_ID, 2, "停用");
        }).when(aiplatformClient).disableSkill(eq(SKILL_ID));
        doAnswer(inv -> {
            capture.run();
            return skillActionReceiptWire(SKILL_ID, 1, "启用");
        }).when(aiplatformClient).enableSkill(eq(SKILL_ID));
        doAnswer(inv -> {
            capture.run();
            // 卸载回执＝删除前终态（启用态行——确认移除了什么）；无行可留不留痕
            return skillActionReceiptWire(SKILL_ID, 1, "启用");
        }).when(aiplatformClient).uninstallSkill(eq(SKILL_ID));
        // 槽位指派两端点 mock（issue #82，aiplatform #249 技能线 T3）：读面/回执同形——
        // {slot, skills[]}（含停用行实态）；PUT 抓取 RequestContext（操作者透传契约证据，
        // provider 整包替换留最近动作者、缺头必拦 SKL_009，断言必带）
        when(aiplatformClient.getSlotAssignments(eq(SLOT_KEY))).thenReturn(slotAssignmentWire());
        doAnswer(inv -> {
            capture.run();
            return slotAssignmentWire();
        }).when(aiplatformClient).assignSlot(eq(SLOT_KEY), any(AiplatformSkillAssignWireRequest.class));
        // 指派双头预检 mock（issue #90，aiplatform #255 技能域第 16 端点）：回执＝两态纪律载体
        // （executed=true＋hints 非空＝发现重叠代表面——首条四字段俱全、次条 null 容差）；预检
        // 只读不留痕（无操作者头要求——不抓 RequestContext，区别于 PUT 指派）
        when(aiplatformClient.precheckSlot(eq(SLOT_KEY), any(AiplatformSkillAssignWireRequest.class)))
                .thenReturn(new AiplatformSkillPrecheckWireResponse(true, List.of(
                        new AiplatformSkillPrecheckWireResponse.PrecheckHint(
                                List.of("报告生成"), "工作协议",
                                "都在 PRD 产出环节做结构化写作，方法论高度重叠",
                                "二选一或收窄 description"),
                        new AiplatformSkillPrecheckWireResponse.PrecheckHint(
                                List.of("web-research", "site-fetch"), null, null, "可不指派其一"))));
        // 显式更新＋留痕读面 mock（issue #83，aiplatform #250 技能线 T4）：更新回执＝
        // from/to 版本＋被移除技能名＋终态行集（远端已前进代表面）；POST 抓取 RequestContext
        // （操作者透传契约证据，provider 显式动作必留痕、缺头必拦 SKL_009，断言必带）
        doAnswer(inv -> {
            capture.run();
            return skillUpdateReceiptWire();
        }).when(aiplatformClient).updateSkills(any(AiplatformSkillUpdateWireRequest.class));
        // 留痕读面：裸 List、append-only 时间倒序（最近先）——版本链衔接（次条 toVersion＝
        // 首条 fromVersion）
        when(aiplatformClient.listSkillUpdateTraces(eq("https://github.com/acme/agent-skills")))
                .thenReturn(List.of(
                        new AiplatformSkillUpdateTraceWireResponse(
                                "https://github.com/acme/agent-skills",
                                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                                "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                                String.valueOf(skillUpdateUserId), skillUpdateUserNickname,
                                LocalDateTime.of(2026, 9, 22, 14, 30, 0)),
                        new AiplatformSkillUpdateTraceWireResponse(
                                "https://github.com/acme/agent-skills",
                                "0f1e2d3c4b5a6978f1e2d3c4b5a6978f1e2d3c4b",
                                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                                "700100", "运营·平台管理员",
                                LocalDateTime.of(2026, 9, 10, 9, 5, 0))));
        // 智能体配置域 mock（issue #84，aiplatform #251 技能线 T5）：读面＝有覆盖行代表面
        // （生效值≠默认预览、覆盖标记 true×2、开关一开一关、最近写者两肢俱全）；PUT 抓取
        // RequestContext（操作者透传契约证据，provider 配置变更必留痕、缺头必拦 PRJ_034，
        // 断言必带）；留痕读面＝裸 List 时间倒序（首条清空变更 new null、次条首配 old null——
        // 四件对 null 两侧各一面）
        when(aiplatformClient.getAgentConfig(eq(AGENT_KEY))).thenReturn(agentConfigWire(
                "700200", "运营·技能管理员", LocalDateTime.of(2026, 9, 22, 14, 30, 0)));
        doAnswer(inv -> {
            capture.run();
            return agentConfigWire(
                    String.valueOf(agentConfigUpdateUserId), agentConfigUpdateUserNickname,
                    LocalDateTime.of(2026, 9, 23, 10, 0, 0));
        }).when(aiplatformClient).updateAgentConfig(eq(AGENT_KEY), any(AiplatformAgentConfigUpdateWireRequest.class));
        when(aiplatformClient.listAgentConfigTraces(eq(AGENT_KEY))).thenReturn(List.of(
                new AiplatformAgentConfigTraceWireResponse(
                        "3900000002222222",
                        "首次覆盖 prompt 全文", "deepseek-v4-pro", true, false,
                        null, null, true, true,
                        String.valueOf(agentConfigUpdateUserId), agentConfigUpdateUserNickname,
                        LocalDateTime.of(2026, 9, 22, 14, 30, 0)),
                new AiplatformAgentConfigTraceWireResponse(
                        "3900000001111111",
                        null, null, true, true,
                        "首次覆盖 prompt 全文", "deepseek-v4-pro", true, false,
                        "700100", "运营·平台管理员",
                        LocalDateTime.of(2026, 9, 10, 9, 5, 0))));
        // 工具面域 mock（issue #85，aiplatform #252 技能线 T6）：清单＝三槽位代表面（main 槽
        // 骨架两件＋增强两件一开一关、executor 槽骨架＋harness 内建、subagent 槽声明面）；
        // PUT 开关抓取 RequestContext（操作者透传契约证据，provider 开关写必留痕、缺头必拦
        // PRJ_034，断言必带），回执＝开关后工具行（web_search 关——与清单嵌套行同形）
        when(aiplatformClient.listAgentTools()).thenReturn(List.of(
                new AiplatformAgentToolSlotWireResponse("main", "主智能体", List.of(
                        new AiplatformAgentToolWireResponse("ask_user", "SKELETON", true,
                                "每轮一问的澄清工具（问答挂起的唯一起源——编排链路）"),
                        new AiplatformAgentToolWireResponse("query_project_facts", "SKELETON", true,
                                "查项目事实含系统访问地址（答询查证只读件）"),
                        new AiplatformAgentToolWireResponse("fetch_url", "ENHANCEMENT", false,
                                "读用户贴的 http/https 地址内容（外部资料，取数口安全底线兜底——增强可关）"),
                        new AiplatformAgentToolWireResponse("web_search", "ENHANCEMENT", true,
                                "自主搜索调研补缺口（增强可关——成本与滥用可控）"))),
                new AiplatformAgentToolSlotWireResponse("executor", "run 执行体", List.of(
                        new AiplatformAgentToolWireResponse("finish_edit", "SKELETON", true,
                                "更新收口结束工具（「要不要动系统」的判定面——编排链路）"),
                        new AiplatformAgentToolWireResponse("read_file", "HARNESS_BUILTIN", true,
                                "读取沙箱内文件内容"))),
                new AiplatformAgentToolSlotWireResponse("subagent", "子智能体", List.of(
                        new AiplatformAgentToolWireResponse("execute", "HARNESS_BUILTIN", true,
                                "self-test 子智能体声明工具面（哨兵 allowlist 结构性收窄，只读交付代码＋跑测试＋报告写隔离根）")))));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformAgentToolWireResponse(TOOL_NAME, "ENHANCEMENT", false,
                    "自主搜索调研补缺口（增强可关——成本与滥用可控）");
        }).when(aiplatformClient).toggleAgentTool(eq(TOOL_NAME), any(AiplatformAgentToolToggleWireRequest.class));
        // 草稿审核面 mock（issue #92，aiplatform #258 自产线）：在途清单两行（跨项目血统三件
        // ＋扫描判定，时间倒序）/ 详情＝在途态（findings untyped map 代表面＋终态留痕四 null）；
        // promote/reject 抓取 RequestContext（操作者透传契约证据，provider 两写口必留痕、缺头
        // 必拦 SKL_009，断言必带）——promote 回执＝技能库清单行（自产 self/self）、reject 回执＝
        // 终态草稿详情（留痕直读）
        when(aiplatformClient.listSkillDrafts()).thenReturn(List.of(
                new AiplatformSkillDraftSummaryWireResponse(
                        "3852400002222222", "seo-checklist", "发布前 SEO 检查时使用",
                        "executor", PROJECT_ID, "run-9f22e1", "CAUTION",
                        1, "在途", LocalDateTime.of(2026, 9, 28, 16, 40, 0)),
                new AiplatformSkillDraftSummaryWireResponse(
                        DRAFT_ID, "report-outline", "生成结构化报告大纲时使用",
                        "main", "3829492001234567", "run-3a77bc", "SAFE",
                        1, "在途", LocalDateTime.of(2026, 9, 27, 9, 15, 0))));
        when(aiplatformClient.getSkillDraft(eq(DRAFT_ID))).thenReturn(new AiplatformSkillDraftDetailWireResponse(
                DRAFT_ID, "report-outline", "生成结构化报告大纲时使用",
                "main", "3829492001234567", "run-3a77bc", "SAFE",
                1, "在途", LocalDateTime.of(2026, 9, 27, 9, 15, 0),
                "# Report Outline\n\n先定读者与决策点，再列章节……",
                List.of(Map.of(
                        "patternId", "secret-like-token", "severity", "CAUTION",
                        "category", "security", "file", "SKILL.md",
                        "line", 42, "matchText", "sk-abc123",
                        "description", "疑似密钥字面量，人审确认是否脱敏")),
                null, null, null, null));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformSkillSummaryWireResponse(
                    "3852300005555555", "report-outline", "生成结构化报告大纲时使用",
                    3, "自产", "self", "self",
                    1, "启用",
                    String.valueOf(skillDraftWriteUserId), skillDraftWriteUserNickname, null,
                    0L, null);
        }).when(aiplatformClient).promoteSkillDraft(eq(DRAFT_ID));
        doAnswer(inv -> {
            capture.run();
            return new AiplatformSkillDraftDetailWireResponse(
                    DRAFT_ID, "report-outline", "生成结构化报告大纲时使用",
                    "main", "3829492001234567", "run-3a77bc", "SAFE",
                    3, "已拒绝", LocalDateTime.of(2026, 9, 27, 9, 15, 0),
                    "# Report Outline\n\n先定读者与决策点，再列章节……",
                    List.of(),
                    String.valueOf(skillDraftWriteUserId), skillDraftWriteUserNickname,
                    LocalDateTime.of(2026, 9, 29, 11, 20, 0),
                    "正文与现有技能 prd-writing 方法论重叠，解消方向不明");
        }).when(aiplatformClient).rejectSkillDraft(eq(DRAFT_ID), any(AiplatformSkillDraftRejectWireRequest.class));
    }

    /**
     * 智能体配置读面/写回执共用的 wire 载荷工厂（issue #84，有覆盖行代表面）：生效值＝库覆盖
     * （覆盖标记 true×2、生效值≠默认预览——「现在跑的是覆盖」）、开关一开一关；操作者两肢＋
     * 写入时刻参数化（读面＝既有写者、写回执＝本次写者——provider 落痕口径：值面有动必落痕）。
     */
    private static AiplatformAgentConfigWireResponse agentConfigWire(
            String operatorId, String operatorName, LocalDateTime updatedAt) {
        return new AiplatformAgentConfigWireResponse(
                AGENT_KEY, "主智能体",
                "你是英语学习助手的主智能体：先判意图，再编排执行体。", "deepseek-v4-pro",
                true, true,
                "你是主智能体。（枚举默认全文）", "deepseek-v4-flash",
                true, false,
                operatorId, operatorName, updatedAt);
    }

    /**
     * 显式更新回执 wire 载荷工厂（issue #83，远端已前进代表面）：from→to 版本不同＋
     * removedSkillNames 含被移除技能名＋更新后终态行集（两行——同名行原地翻新 id 稳定、
     * 新技能插入启用）。操作者两肢取登录版本治理运营（provider 落痕口径：显式动作必留痕）。
     */
    private AiplatformSkillUpdateWireResponse skillUpdateReceiptWire() {
        return new AiplatformSkillUpdateWireResponse(
                "https://github.com/acme/agent-skills",
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                List.of("legacy-summary"),
                String.valueOf(skillUpdateUserId), skillUpdateUserNickname,
                List.of(
                        new AiplatformSkillSummaryWireResponse(
                                SKILL_ID, "web-research", "联网检索与网页抓取整理",
                                2, "安装", "https://github.com/acme/agent-skills",
                                "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                                1, "启用",
                                String.valueOf(skillUpdateUserId), skillUpdateUserNickname, false,
                                5L, LocalDateTime.of(2026, 9, 20, 10, 0, 0)),
                        new AiplatformSkillSummaryWireResponse(
                                "3852300004444444", "code-review", "代码评审时使用",
                                2, "安装", "https://github.com/acme/agent-skills",
                                "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                                1, "启用",
                                String.valueOf(skillUpdateUserId), skillUpdateUserNickname, false,
                                0L, null)));
    }

    /**
     * 三写操作回执共用的 wire 载荷工厂（用户面同构 OrderResponse）：按动作后的终态
     * （status/时点组）参数化，其余字段固定（报价事实 + 单条五字段价目行——回执形状的
     * 代表面）。paidAt/cancelledAt/archivedAt 全参数化——各终态只落自己的时点（改价/取消单
     * 未支付 paidAt=null，归档单双时点俱全）。
     */
    private static AiplatformOrderWireResponse orderReceiptWire(
            Integer status, String statusName,
            LocalDateTime cancelledAt, LocalDateTime paidAt, LocalDateTime archivedAt) {
        return new AiplatformOrderWireResponse(
                ORDER_ID, "3829492007654321", status, statusName,
                2, "系统",
                2199000L, "CNY", "含加急费用", LocalDateTime.of(2026, 9, 11, 9, 0, 0),
                List.of(new AiplatformOrderWireResponse.PriceEntry(
                        "3829492011111111", 2199000L, "CNY", "含加急费用",
                        LocalDateTime.of(2026, 9, 12, 10, 0, 0))),
                LocalDateTime.of(2026, 9, 10, 14, 20, 0),
                cancelledAt, paidAt, archivedAt);
    }

    /**
     * 技能写口回执共用的 wire 载荷工厂（清单行单行）：按动作后的状态（启停＝翻转后、卸载＝删除
     * 前终态）参数化，操作者两肢＝登录治理运营（provider 落痕口径——安装＝装者、启停＝最近动作者）；
     * 新版标记 null＝未检查过（装时快照刚落，provider 恒 null 如实出 JSON null）。
     */
    private AiplatformSkillSummaryWireResponse skillActionReceiptWire(String id, Integer status, String statusName) {
        return new AiplatformSkillSummaryWireResponse(
                id, "web-research", "联网检索与网页抓取整理",
                2, "安装", "https://github.com/acme/agent-skills",
                "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                status, statusName,
                String.valueOf(skillWriteUserId), skillWriteUserNickname, null,
                5L, LocalDateTime.of(2026, 9, 20, 10, 0, 0));
    }

    /**
     * 槽位指派读面/回执共用的 wire 载荷工厂（issue #82）：{@code {slot, skills[]}}——指派行集
     * 与清单行共形且含停用行（「指派了但已停用」实态的代表面：次行停用态）。操作者两肢取
     * 装配运营（provider 落痕口径：整包替换留最近动作者）。
     */
    private AiplatformSlotAssignmentWireResponse slotAssignmentWire() {
        return new AiplatformSlotAssignmentWireResponse(SLOT_KEY, List.of(
                new AiplatformSkillSummaryWireResponse(
                        SKILL_ID, "web-research", "联网检索与网页抓取整理",
                        2, "安装", "https://github.com/acme/agent-skills",
                        "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                        1, "启用",
                        String.valueOf(skillAssignUserId), skillAssignUserNickname, false,
                        5L, LocalDateTime.of(2026, 9, 20, 10, 0, 0)),
                new AiplatformSkillSummaryWireResponse(
                        "3852300003333333", "prd-writing", "撰写或修订 PRD 时使用",
                        2, "安装", "https://github.com/acme/agent-skills",
                        "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                        2, "停用",
                        String.valueOf(skillAssignUserId), skillAssignUserNickname, null,
                        0L, null)));
    }

    /**
     * 四动作回执/详情共用的 wire 载荷工厂：按动作后的新事实（期望态/实态/封存包元数据）参数化，
     * 其余字段固定（资源观测一行 + 项目引用——详情全量形状的代表面）。
     */
    private static AiplatformWorkspaceDetailWireResponse workspaceActionReceiptWire(
            Integer desiredState, String desiredStateName,
            Integer containerState, String containerStateName,
            String archivePath, Long archiveSizeBytes) {
        return new AiplatformWorkspaceDetailWireResponse(
                WORKSPACE_ID, "ws-" + WORKSPACE_ID, "net-" + WORKSPACE_ID,
                1, "开发", 2, "就绪", null,
                desiredState, desiredStateName, containerState, containerStateName,
                LocalDateTime.of(2026, 9, 16, 9, 0, 0), 1073741824L,
                archivePath == null ? null : LocalDateTime.of(2026, 9, 16, 9, 0, 0),
                archivePath, archiveSizeBytes,
                LocalDateTime.of(2026, 9, 8, 10, 0, 0), LocalDateTime.of(2026, 9, 16, 9, 0, 0),
                List.of(new AiplatformWorkspaceDetailWireResponse.MiddlewareResourceObservation(
                        1, "mw-" + WORKSPACE_ID + "-pg",
                        "postgresql://aiedu:secret@mw-" + WORKSPACE_ID + "-pg:5432/aiedu")),
                new AiplatformWorkspaceSummaryWireResponse.ProjectRef(PROJECT_ID, "英语学习助手", false));
    }

    // ========== 账号档案（admin:aiplatform:account:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:account:read → 档案 200，北向 data 逐字镜像 provider")
    void given_nonSuperAdminWithRead_when_getProfile_then_200AndDataMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(PROFILE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 北向出口形状：admin ApiResponse 信封 + data 四字段逐字镜像（id 为 String、createdAt ISO）
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo("3829492001234567");
        assertThat(data.path("externalId").asText()).isEqualTo(EXTERNAL_ID);
        assertThat(data.path("displayName").asText()).isEqualTo("文野");
        assertThat(data.path("createdAt").asText()).isEqualTo("2026-01-15T10:30:00");
    }

    @Test
    @DisplayName("非超管且缺少权限 → 档案 403（独立权限码 account:read，2026-09-15 拍板）")
    void given_nonSuperAdminWithoutPermission_when_getProfile_then_403() {
        String token = login(usernameWithoutPermission);
        ResponseEntity<String> response = getWithToken(PROFILE_ENDPOINT, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问账号档案返回 401")
    void given_unauthenticated_when_getProfile_then_401() {
        ResponseEntity<String> response = getWithToken(PROFILE_ENDPOINT, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== 订单（admin:aiplatform:order:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:order:read → 清单 200，分页回显 provider 值、金额 Long（分）")
    void given_nonSuperAdminWithOrderRead_when_listOrders_then_200AndPageShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        // 四维过滤 + 分页 1-based 直传（page=3 无 ±1）
        ResponseEntity<String> response = getWithToken(
                ORDERS_ENDPOINT + "?status=1,5&externalId=auth0%7C65f2c8a1&page=3&size=20", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // PageResponse 形状：items/total/page/size，回显 provider 回报值（page=3——1-based 原值）
        assertThat(data.path("total").asLong()).isEqualTo(42L);
        assertThat(data.path("page").asInt()).isEqualTo(3);
        assertThat(data.path("size").asInt()).isEqualTo(20);
        JsonNode row = data.path("items").get(0);
        assertThat(row.path("id").asText()).isEqualTo(ORDER_ID);
        assertThat(row.path("status").asInt()).isEqualTo(3);
        assertThat(row.path("statusName").asText()).isEqualTo("已支付");
        // 交付物类型字段对（#96）：北向出 JSON number（Integer code 非 Long——ToStringSerializer
        // 只覆盖包装 Long）+ 中文名随行（前端直读 *Name，ADR-0009）
        assertThat(row.path("deliverableType").asInt()).isEqualTo(3);
        assertThat(row.path("deliverableTypeName").asText()).isEqualTo("系统＋设计");
        assertThat(row.path("amount").asLong()).isEqualTo(1999000L);
        assertThat(row.path("currency").asText()).isEqualTo("CNY");
        assertThat(row.path("createdAt").asText()).isEqualTo("2026-09-10T14:20:00");
    }

    @Test
    @DisplayName("非超管且拥有 order:read → 详情 200，价目历史嵌套行带操作者留痕")
    void given_nonSuperAdminWithOrderRead_when_getDetail_then_200AndPriceEntriesNested() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(ORDER_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(ORDER_ID);
        assertThat(data.path("status").asInt()).isEqualTo(2);
        assertThat(data.path("statusName").asText()).isEqualTo("已报价");
        // 交付物类型字段对（#96）：设计单——详情基本信息 tag 数据面
        assertThat(data.path("deliverableType").asInt()).isEqualTo(1);
        assertThat(data.path("deliverableTypeName").asText()).isEqualTo("设计");
        assertThat(data.path("amount").asLong()).isEqualTo(2199000L);
        assertThat(data.path("note").asText()).isEqualTo("含加急费用");
        assertThat(data.path("prdSnapshot").asText()).startsWith("# PRD");
        JsonNode entry = data.path("priceEntries").get(0);
        assertThat(entry.path("amount").asLong()).isEqualTo(2199000L);
        assertThat(entry.path("operatorId").asText()).isEqualTo("1234567890123456");
        assertThat(entry.path("operatorName").asText()).isEqualTo("报价运营");
        // 未支付/未取消的 NULL 字段如实出 JSON null（全局 Jackson 含 null）
        assertThat(data.path("paidAt").isNull()).isTrue();
        assertThat(data.path("cancelReason").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 order:read → 源码包 200，gzip 字节 + provider 响应头透传（无信封）")
    void given_nonSuperAdminWithOrderRead_when_downloadSourcePackage_then_200BinaryWithProviderHeaders() {
        String token = login(usernameWithReadPermission);
        ResponseEntity<byte[]> response = getBinaryWithToken(SOURCE_PACKAGE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 无 ApiResponse 信封：body 即 tar.gz 字节流（含非法 UTF-8 字节，逐位透传）
        assertThat(response.getBody())
                .containsExactly((byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00);
        // Content-Type / Content-Disposition 取 provider 原值（attachment 文件名透传）
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/gzip");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"" + ORDER_ID + "-source.tar.gz\"");
    }

    @Test
    @DisplayName("非超管且拥有 order:read → 设计资产包 200，gzip 字节 + provider 响应头透传（无信封，#96）")
    void given_nonSuperAdminWithOrderRead_when_downloadDesignPackage_then_200BinaryWithProviderHeaders() {
        String token = login(usernameWithReadPermission);
        ResponseEntity<byte[]> response = getBinaryWithToken(DESIGN_PACKAGE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 无 ApiResponse 信封：body 即 tar.gz 字节流（含非法 UTF-8 字节，逐位透传——下单冻结件直取）
        assertThat(response.getBody())
                .containsExactly((byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00);
        // Content-Type / Content-Disposition 取 provider 原值（design 后缀区别于 source）
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/gzip");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"" + ORDER_ID + "-design.tar.gz\"");
    }

    @Test
    @DisplayName("非超管且缺少权限 → 订单四端点 403（独立权限码 order:read——design-package 复用同码）")
    void given_nonSuperAdminWithoutPermission_when_orderEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(ORDERS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(ORDER_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(SOURCE_PACKAGE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(DESIGN_PACKAGE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问订单清单/源码包/设计资产包返回 401")
    void given_unauthenticated_when_orderEndpoints_then_401() {
        assertThat(getWithToken(ORDERS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(SOURCE_PACKAGE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(DESIGN_PACKAGE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("订单详情下游 ORD_001 → 北向 HTTP 404 + 信封 code=5001 + message 原文（不映射）")
    void given_ord001FromDownstream_when_getDetail_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.getOrder(eq("3829499999999999"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":5001,\"message\":\"订单不存在\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(
                "/api/admin/aiplatform/orders/3829499999999999", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(5001);
        assertThat(root.path("message").asText()).isEqualTo("订单不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 订单写操作（quote/cancel/retry-archive 三独立写码）· 权限三态 + 操作者身份透传 ==========

    @Test
    @DisplayName("非超管且拥有三写码 → 报价/取消/重试归档 200，回执=用户面 OrderResponse 且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithThreeWriteCodes_when_orderActions_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithOrderWrite);

        // 报价（本例＝改价：已报价态重复提交）：回执现值取最新价目行 + append-only 价目历史
        // （五字段，无操作者留痕——查全量留痕转后台详情读口）
        ResponseEntity<String> quote = postWithToken(QUOTE_ENDPOINT, QUOTE_BODY, token);
        assertThat(quote.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode quoted = objectMapper.readTree(quote.getBody()).path("data");
        assertThat(quoted.path("id").asText()).isEqualTo(ORDER_ID);
        assertThat(quoted.path("status").asInt()).isEqualTo(2);
        assertThat(quoted.path("statusName").asText()).isEqualTo("已报价");
        // 交付物类型字段对（#96）：写回执同步镜像（用户面 OrderResponse 已配对）
        assertThat(quoted.path("deliverableType").asInt()).isEqualTo(2);
        assertThat(quoted.path("deliverableTypeName").asText()).isEqualTo("系统");
        assertThat(quoted.path("amount").asLong()).isEqualTo(2199000L);
        assertThat(quoted.path("note").asText()).isEqualTo("含加急费用");
        assertThat(quoted.path("quotedAt").asText()).isEqualTo("2026-09-11T09:00:00");
        JsonNode entry = quoted.path("priceEntries").get(0);
        assertThat(entry.path("amount").asLong()).isEqualTo(2199000L);
        assertThat(entry.path("createdAt").asText()).isEqualTo("2026-09-12T10:00:00");
        // 用户面回执无操作者留痕两肢——字段不存在（isMissingNode），区别于后台详情价目行
        assertThat(entry.path("operatorId").isMissingNode()).isTrue();

        // 取消：已取消终态（cancelledAt 落定，报价事实保留）
        ResponseEntity<String> cancel = postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, token);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cancelled = objectMapper.readTree(cancel.getBody()).path("data");
        assertThat(cancelled.path("status").asInt()).isEqualTo(5);
        assertThat(cancelled.path("statusName").asText()).isEqualTo("已取消");
        assertThat(cancelled.path("cancelledAt").asText()).isEqualTo("2026-09-16T15:00:00");

        // 重试归档：已归档终态（paidAt/archivedAt 双时点）
        ResponseEntity<String> retryArchive = postWithToken(RETRY_ARCHIVE_ENDPOINT, token);
        assertThat(retryArchive.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode archived = objectMapper.readTree(retryArchive.getBody()).path("data");
        assertThat(archived.path("status").asInt()).isEqualTo(4);
        assertThat(archived.path("paidAt").asText()).isEqualTo("2026-09-15T20:00:00");
        assertThat(archived.path("archivedAt").asText()).isEqualTo("2026-09-16T09:30:00");

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，issue #70 断言必带；provider 落痕价目行/订单行）：
        // == 登录交易监管运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(orderWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(orderWriteUserNickname);
    }

    @Test
    @DisplayName("仅有 order:read（无写码）→ 三写操作 403（read ≠ 三写码）")
    void given_nonSuperAdminWithReadOnly_when_orderWriteActions_then_403() {
        String token = login(usernameWithReadPermission);
        assertThat(postWithToken(QUOTE_ENDPOINT, QUOTE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(RETRY_ARCHIVE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("仅有 order:quote 单写码 → 报价 200、取消/重试归档 403（三写码彼此独立，最小授权）")
    void given_nonSuperAdminWithQuoteOnly_when_actions_then_quote200Others403() {
        String token = login(usernameWithQuoteOnly);
        assertThat(postWithToken(QUOTE_ENDPOINT, QUOTE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(RETRY_ARCHIVE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 订单三写端点 403")
    void given_nonSuperAdminWithoutPermission_when_orderWriteEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(postWithToken(QUOTE_ENDPOINT, QUOTE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(RETRY_ARCHIVE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问订单三写端点返回 401")
    void given_unauthenticated_when_orderWriteEndpoints_then_401() {
        assertThat(postWithToken(QUOTE_ENDPOINT, QUOTE_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(RETRY_ARCHIVE_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("取消下游 ORD_005（已支付拒取消）→ 北向 HTTP 409 + 信封 code=5005 + message 原文（不映射）")
    void given_ord005FromDownstream_when_cancel_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.cancelOrder(eq(ORDER_ID), any(AiplatformOrderCancelWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":5005,\"message\":\"订单已支付或已终结，无法取消\",\"data\":null}")));

        String token = login(usernameWithOrderWrite);
        ResponseEntity<String> response = postWithToken(CANCEL_ENDPOINT, CANCEL_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 5005（ORD_005＝域码 5×1000＋5），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(5005);
        assertThat(root.path("message").asText()).isEqualTo("订单已支付或已终结，无法取消");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("重试归档下游 PRJ_013（项目已归档，跨域码）→ 北向 HTTP 409 + 信封 code=4013 + message 原文（不映射）")
    void given_prj013FromDownstream_when_retryArchive_then_crossDomainCodePassedThrough() throws Exception {
        when(aiplatformClient.retryArchiveOrder(eq(ORDER_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":4013,\"message\":\"项目已归档（归档是单向终点）\",\"data\":null}")));

        String token = login(usernameWithOrderWrite);
        ResponseEntity<String> response = postWithToken(RETRY_ARCHIVE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 跨域码透传（issue #70）：PRJ_013 来自项目域（域码 4×1000＋13），经订单域端点原样抵达北向
        assertThat(root.path("code").asInt()).isEqualTo(4013);
        assertThat(root.path("message").asText()).isEqualTo("项目已归档（归档是单向终点）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 项目（admin:aiplatform:project:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:project:read → 清单 200，分页回显 provider 值、status 单选")
    void given_nonSuperAdminWithProjectRead_when_listProjects_then_200AndPageShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        // 四维过滤（status 三档单选）+ 分页 1-based 直传
        ResponseEntity<String> response = getWithToken(
                PROJECTS_ENDPOINT + "?status=3&externalId=auth0%7C65f2c8a1&page=2&size=50", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // PageResponse 形状：items/total/page/size，回显 provider 回报值（page=2——1-based 原值）
        assertThat(data.path("total").asLong()).isEqualTo(7L);
        assertThat(data.path("page").asInt()).isEqualTo(2);
        assertThat(data.path("size").asInt()).isEqualTo(50);
        JsonNode row = data.path("items").get(0);
        // 归档行照读：type/status Integer + *Name、archived 原始事实位
        assertThat(row.path("id").asText()).isEqualTo(PROJECT_ID);
        assertThat(row.path("type").asInt()).isEqualTo(1);
        assertThat(row.path("typeName").asText()).isEqualTo("官网");
        assertThat(row.path("status").asInt()).isEqualTo(3);
        assertThat(row.path("statusName").asText()).isEqualTo("已归档");
        assertThat(row.path("archived").asBoolean()).isTrue();
        // owner 摘要两肢（issue #76）：externalId＝账号档案读口的寻址键 + 显示名
        assertThat(row.path("ownerExternalId").asText()).isEqualTo(EXTERNAL_ID);
        assertThat(row.path("ownerDisplayName").asText()).isEqualTo("文野");
    }

    @Test
    @DisplayName("非超管且拥有 project:read → 详情 200，订单引用 + 成本指针嵌套镜像 provider")
    void given_nonSuperAdminWithProjectRead_when_getDetail_then_200WithOrderRefsAndCostSummary() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(PROJECT_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(PROJECT_ID);
        assertThat(data.path("workspaceId").asText()).isEqualTo("3829492009999999");
        assertThat(data.path("status").asInt()).isEqualTo(1);
        assertThat(data.path("statusName").asText()).isEqualTo("进行中");
        // 订单引用：activeOrder（未终结已报价——有值即冻结迭代）+ latestOrder（最近一张已归档）
        assertThat(data.path("activeOrder").path("id").asText()).isEqualTo(ORDER_ID);
        assertThat(data.path("activeOrder").path("status").asInt()).isEqualTo(2);
        assertThat(data.path("activeOrder").path("statusName").asText()).isEqualTo("已报价");
        assertThat(data.path("latestOrder").path("statusName").asText()).isEqualTo("已归档");
        // 成本指针：cost 按币种（JSON 数字）+ unpriced 标记
        assertThat(data.path("costSummary").path("cost").path("CNY").asDouble()).isEqualTo(12.3456);
        assertThat(data.path("costSummary").path("unpriced").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 project:read → 对话史/PRD/版本 200，kind+kindName 随行、载荷原样")
    void given_nonSuperAdminWithProjectRead_when_deepRead_then_200AndPayloadsMirrored() throws Exception {
        String token = login(usernameWithReadPermission);

        // 对话史：kind Integer code + kindName 中文名随行（aiplatform#186 已落——provider 出口提供）
        ResponseEntity<String> conversation = getWithToken(CONVERSATION_ENDPOINT, token);
        assertThat(conversation.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode entries = objectMapper.readTree(conversation.getBody()).path("data");
        assertThat(entries.size()).isEqualTo(2);
        assertThat(entries.get(0).path("kind").asInt()).isEqualTo(1);
        assertThat(entries.get(0).path("kindName").asText()).isEqualTo("用户发言");
        assertThat(entries.get(1).path("closing").path("commitHash").asText()).isEqualTo(VERSION_HASH);

        // PRD：markdown 正文 + updatedAt（Instant，ISO-8601 UTC 带 Z）
        ResponseEntity<String> prd = getWithToken(PRD_ENDPOINT, token);
        assertThat(prd.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode prdData = objectMapper.readTree(prd.getBody()).path("data");
        assertThat(prdData.path("content").asText()).startsWith("# PRD");
        assertThat(prdData.path("updatedAt").asText()).isEqualTo("2026-09-12T08:30:00Z");

        // 版本列表（新→旧）+ 版本详情（ref＝hex 40 位，closing 载荷原样）
        ResponseEntity<String> versions = getWithToken(VERSIONS_ENDPOINT, token);
        assertThat(versions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(versions.getBody()).path("data").size()).isEqualTo(1);
        ResponseEntity<String> versionDetail = getWithToken(VERSION_DETAIL_ENDPOINT, token);
        assertThat(versionDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode versionData = objectMapper.readTree(versionDetail.getBody()).path("data");
        assertThat(versionData.path("commitHash").asText()).isEqualTo(VERSION_HASH);
        assertThat(versionData.path("closing").path("runId").asText()).isEqualTo("run-abc123");
    }

    @Test
    @DisplayName("非超管且缺少权限 → 项目九端点 403（独立权限码 project:read，深读三组与文件区同码）")
    void given_nonSuperAdminWithoutPermission_when_projectEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(PROJECTS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(PROJECT_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(CONVERSATION_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(PRD_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(VERSIONS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(VERSION_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(FILES_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(FILE_CONTENT_ENDPOINT + "?path=" + FILE_TREE_PATH, token)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(FILES_PACKAGE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问项目九端点返回 401")
    void given_unauthenticated_when_projectEndpoints_then_401() {
        assertThat(getWithToken(PROJECTS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(PROJECT_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(CONVERSATION_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(PRD_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(VERSIONS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(VERSION_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(FILES_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(FILE_CONTENT_ENDPOINT + "?path=" + FILE_TREE_PATH, null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(FILES_PACKAGE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("项目详情下游 PRJ_001 → 北向 HTTP 404 + 信封 code=4001 + message 原文（不映射）")
    void given_prj001FromDownstream_when_getProjectDetail_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.getProject(eq("3829499999999999"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":4001,\"message\":\"项目不存在\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(PROJECTS_ENDPOINT + "/3829499999999999", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4001（PRJ_001），而非映射后的 404——前端比对 aiplatform 业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4001);
        assertThat(root.path("message").asText()).isEqualTo("项目不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 project:read → 文件树/文件内容 200，[{path,size}] 与 {path,content} 镜像 provider")
    void given_nonSuperAdminWithProjectRead_when_fileArea_then_200AndTreeAndContentMirrored() throws Exception {
        String token = login(usernameWithReadPermission);

        // 文件树：交付文件视图 [{path, size}]（size 出 JSON string——全局 Long→ToStringSerializer）
        ResponseEntity<String> files = getWithToken(FILES_ENDPOINT, token);
        assertThat(files.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode filesData = objectMapper.readTree(files.getBody()).path("data");
        assertThat(filesData.path("projectId").asText()).isEqualTo(PROJECT_ID);
        assertThat(filesData.path("files").size()).isEqualTo(2);
        assertThat(filesData.path("files").get(0).path("path").asText()).isEqualTo("README.md");
        assertThat(filesData.path("files").get(0).path("size").asLong()).isEqualTo(512L);
        assertThat(filesData.path("files").get(1).path("path").asText()).isEqualTo(FILE_TREE_PATH);
        assertThat(filesData.path("files").get(1).path("size").asLong()).isEqualTo(4096L);

        // 文件内容（点看）：path 原样回显 + content 原样
        ResponseEntity<String> content = getWithToken(
                FILE_CONTENT_ENDPOINT + "?path=" + FILE_TREE_PATH, token);
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode contentData = objectMapper.readTree(content.getBody()).path("data");
        assertThat(contentData.path("path").asText()).isEqualTo(FILE_TREE_PATH);
        assertThat(contentData.path("content").asText()).startsWith("# PRD");
    }

    @Test
    @DisplayName("非超管且拥有 project:read → 文件包 200，gzip 字节 + provider 响应头透传（无信封）")
    void given_nonSuperAdminWithProjectRead_when_downloadFilesPackage_then_200BinaryWithProviderHeaders() {
        String token = login(usernameWithReadPermission);
        ResponseEntity<byte[]> response = getBinaryWithToken(FILES_PACKAGE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 无 ApiResponse 信封：body 即 tar.gz 字节流（含非法 UTF-8 字节，逐位透传）
        assertThat(response.getBody())
                .containsExactly((byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00);
        // Content-Type / Content-Disposition 取 provider 原值（sealed/source 两态文件名由 provider 决定）
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/gzip");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"" + PROJECT_ID + "-archive.tar.gz\"");
    }

    @Test
    @DisplayName("文件内容下游 PRJ_020 → 北向 HTTP 400 + 信封 code=4020 + message 原文（不映射）")
    void given_prj020FromDownstream_when_getFileContent_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.getProjectFileContent(eq(PROJECT_ID), eq(".env"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(400,
                                "{\"code\":4020,\"message\":\"该文件不在可浏览范围\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(FILE_CONTENT_ENDPOINT + "?path=.env", token);

        // 只读策略（机密/逃逸拒）归 provider 裁决——BFF 不预检不解释，信封原样还原
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(4020);
        assertThat(root.path("message").asText()).isEqualTo("该文件不在可浏览范围");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 沙箱观测（admin:aiplatform:workspace:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:workspace:read → 清单 200，期望态/实态漂移两列镜像 provider")
    void given_nonSuperAdminWithWorkspaceRead_when_listWorkspaces_then_200AndDriftColumnsMirrorProvider()
            throws Exception {
        String token = login(usernameWithReadPermission);
        // 漂移清单口径：desired=1（期望运行）+ actual=3（实态无容器）+ 分页 1-based 直传
        ResponseEntity<String> response = getWithToken(
                WORKSPACES_ENDPOINT + "?desired=1&actual=3&page=2&size=20", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // PageResponse 形状：items/total/page/size，回显 provider 回报值（page=2——1-based 原值）
        assertThat(data.path("total").asLong()).isEqualTo(17L);
        assertThat(data.path("page").asInt()).isEqualTo(2);
        assertThat(data.path("size").asInt()).isEqualTo(20);
        JsonNode row = data.path("items").get(0);
        // 漂移行两列分示：期望运行（1/运行）而实态无容器（3/无容器）+ 四枚举 code+*Name 成对
        assertThat(row.path("workspaceId").asText()).isEqualTo(WORKSPACE_ID);
        assertThat(row.path("kind").asInt()).isEqualTo(1);
        assertThat(row.path("kindName").asText()).isEqualTo("开发");
        assertThat(row.path("statusName").asText()).isEqualTo("就绪");
        assertThat(row.path("desiredState").asInt()).isEqualTo(1);
        assertThat(row.path("desiredStateName").asText()).isEqualTo("运行");
        assertThat(row.path("containerState").asInt()).isEqualTo(3);
        assertThat(row.path("containerStateName").asText()).isEqualTo("无容器");
        // 卷用量 Long（JSON string）+ 项目引用三字段（跳转项目详情的锚）
        assertThat(row.path("volumeSizeBytes").asLong()).isEqualTo(2147483648L);
        assertThat(row.path("project").path("projectId").asText()).isEqualTo(PROJECT_ID);
        assertThat(row.path("project").path("name").asText()).isEqualTo("英语学习助手");
        assertThat(row.path("project").path("archived").asBoolean()).isFalse();
        // 未封存字段如实出 JSON null（全局 Jackson 含 null）
        assertThat(row.path("sealedAt").isNull()).isTrue();
        assertThat(row.path("archiveSizeBytes").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 workspace:read → 详情 200，资源观测清单 + 封存包元数据镜像 provider")
    void given_nonSuperAdminWithWorkspaceRead_when_getWorkspaceDetail_then_200AndResourcesMirrored()
            throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(WORKSPACE_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("workspaceId").asText()).isEqualTo(WORKSPACE_ID);
        assertThat(data.path("networkName").asText()).isEqualTo("net-" + WORKSPACE_ID);
        assertThat(data.path("desiredState").asInt()).isEqualTo(3);
        assertThat(data.path("desiredStateName").asText()).isEqualTo("封存");
        // 封存包寻址键 + 包大小（Long JSON string）
        assertThat(data.path("archivePath").asText()).isEqualTo("workspace-archives/" + WORKSPACE_ID + ".tar.gz");
        assertThat(data.path("archiveSizeBytes").asLong()).isEqualTo(89128960L);
        // 中间件资源清单：kind Integer code（provider 出口无 *Name——忠实镜像）+ 连接串原文
        JsonNode pg = data.path("resources").get(0);
        assertThat(pg.path("kind").asInt()).isEqualTo(1);
        assertThat(pg.path("containerName").asText()).isEqualTo("mw-" + WORKSPACE_ID + "-pg");
        assertThat(pg.path("internalUrl").asText()).startsWith("postgresql://");
        assertThat(data.path("project").path("projectId").asText()).isEqualTo(PROJECT_ID);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 沙箱读两端点 403（独立权限码 workspace:read）")
    void given_nonSuperAdminWithoutPermission_when_workspaceReadEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(WORKSPACES_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(WORKSPACE_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问沙箱观测/动作端点返回 401")
    void given_unauthenticated_when_workspaceEndpoints_then_401() {
        assertThat(getWithToken(WORKSPACES_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(WORKSPACE_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(WAKE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== 沙箱四干预动作（四个独立写码）· 权限三态 + 操作者身份透传 ==========

    @Test
    @DisplayName("非超管且拥有四写码 → 唤醒/休眠/重建/封存 200，回执=详情 DTO 且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithFourWriteCodes_when_fourActions_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithWorkspaceWrite);

        // 唤醒：回执＝动作后的观测详情（期望运行 + 实态运行中的新事实）
        ResponseEntity<String> wake = postWithToken(WAKE_ENDPOINT, token);
        assertThat(wake.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode wakeData = objectMapper.readTree(wake.getBody()).path("data");
        assertThat(wakeData.path("desiredState").asInt()).isEqualTo(1);
        assertThat(wakeData.path("containerState").asInt()).isEqualTo(1);
        assertThat(wakeData.path("containerStateName").asText()).isEqualTo("运行中");
        assertThat(wakeData.path("resources").size()).isEqualTo(1);

        // 休眠：期望休眠 + 实态无容器（删容器保卷）
        ResponseEntity<String> hibernate = postWithToken(HIBERNATE_ENDPOINT, token);
        assertThat(hibernate.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode hibernateData = objectMapper.readTree(hibernate.getBody()).path("data");
        assertThat(hibernateData.path("desiredStateName").asText()).isEqualTo("休眠");
        assertThat(hibernateData.path("containerStateName").asText()).isEqualTo("无容器");

        // 重建：同唤醒收敛（期望运行 + 实态运行中）
        ResponseEntity<String> rebuild = postWithToken(REBUILD_ENDPOINT, token);
        assertThat(rebuild.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(rebuild.getBody()).path("data")
                .path("containerStateName").asText()).isEqualTo("运行中");

        // 封存：期望封存 + 封存包元数据（寻址键/大小）
        ResponseEntity<String> seal = postWithToken(SEAL_ENDPOINT, token);
        assertThat(seal.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode sealData = objectMapper.readTree(seal.getBody()).path("data");
        assertThat(sealData.path("desiredStateName").asText()).isEqualTo("封存");
        assertThat(sealData.path("archivePath").asText()).isEqualTo("workspace-archives/" + WORKSPACE_ID + ".tar.gz");

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，issue #66 断言必带）：== 登录沙箱治理运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(workspaceWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(workspaceWriteUserNickname);
    }

    @Test
    @DisplayName("仅有 workspace:read（无写码）→ 四动作 403（read ≠ 四写码）")
    void given_nonSuperAdminWithReadOnly_when_workspaceActions_then_403() {
        String token = login(usernameWithReadPermission);
        assertThat(postWithToken(WAKE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(HIBERNATE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(REBUILD_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SEAL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("仅有 workspace:hibernate 单写码 → 休眠 200、唤醒/重建/封存 403（四写码彼此独立，最小授权）")
    void given_nonSuperAdminWithHibernateOnly_when_actions_then_hibernate200Others403() {
        String token = login(usernameWithHibernateOnly);
        assertThat(postWithToken(HIBERNATE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(WAKE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(REBUILD_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SEAL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("沙箱动作下游 WSP_015 → 北向 HTTP 409 + 信封 code=1015 + message 原文（不映射）")
    void given_wsp015FromDownstream_when_hibernate_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.hibernateWorkspace(eq(WORKSPACE_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":1015,\"message\":\"编码 run 进行中，沙箱动作被拒（先取消 run 或等收口）\",\"data\":null}")));

        String token = login(usernameWithWorkspaceWrite);
        ResponseEntity<String> response = postWithToken(HIBERNATE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 1015（WSP_015），而非映射后的 409——前端比对 aiplatform 业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(1015);
        assertThat(root.path("message").asText()).isEqualTo("编码 run 进行中，沙箱动作被拒（先取消 run 或等收口）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 成本观测（admin:aiplatform:cost:read）· 权限三态 + 北向出口形状 + 时间窗必填 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:cost:read → 总览/unpriced 200，五档+币种分桶+分解镜像 provider")
    void given_nonSuperAdminWithCostRead_when_overviewAndUnpriced_then_200AndShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);

        // 全局总览：窗口回显 + 五档总量（JSON 数字）+ 币种分桶 + 分智能体（agentKind 裸维度串 +
        // agentKindName 随行——aiplatform#186 已落，辅助标记为 null 如实出 JSON null）
        ResponseEntity<String> overview = getWithToken(COSTS_ENDPOINT + "/overview" + COST_WINDOW_QUERY, token);
        assertThat(overview.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode overviewData = objectMapper.readTree(overview.getBody()).path("data");
        assertThat(overviewData.path("from").asText()).isEqualTo(COST_FROM);
        assertThat(overviewData.path("to").asText()).isEqualTo(COST_TO);
        JsonNode total = overviewData.path("total");
        assertThat(total.path("input").asLong()).isEqualTo(5000L);
        assertThat(total.path("output").asLong()).isEqualTo(1200L);
        assertThat(total.path("cacheRead").asLong()).isEqualTo(300L);
        assertThat(total.path("cacheWrite").asLong()).isEqualTo(0L);
        assertThat(total.path("reasoning").asLong()).isEqualTo(800L);
        assertThat(overviewData.path("cost").path("CNY").asDouble()).isEqualTo(12.3456);
        JsonNode agentKind = overviewData.path("byAgentKind").get(0);
        assertThat(agentKind.path("agentKind").asText()).isEqualTo("naming");
        assertThat(agentKind.path("agentKindName").isNull()).isTrue();

        // unpriced 全局警示：tokenKind Integer code + tokenKindName + tokens 只计无价分量
        ResponseEntity<String> unpriced = getWithToken(COSTS_ENDPOINT + "/unpriced" + COST_WINDOW_QUERY, token);
        assertThat(unpriced.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode tier = objectMapper.readTree(unpriced.getBody()).path("data").path("items").get(0);
        assertThat(tier.path("provider").asText()).isEqualTo("openai");
        assertThat(tier.path("tokenKind").asInt()).isEqualTo(1);
        assertThat(tier.path("tokenKindName").asText()).isEqualTo("输入");
        assertThat(tier.path("tokens").asLong()).isEqualTo(700L);
    }

    @Test
    @DisplayName("非超管且拥有 cost:read → 项目成本清单/单项目下钻 200，分页 1-based 回显 + allUnpriced 标记")
    void given_nonSuperAdminWithCostRead_when_projectCosts_then_200AndPageEchoed() throws Exception {
        String token = login(usernameWithReadPermission);

        // 项目成本清单：PageResponse 形状（total JSON string 口径在 Long 字段），回显 provider 回报值
        ResponseEntity<String> projects = getWithToken(
                COSTS_ENDPOINT + "/projects" + COST_WINDOW_QUERY + "&page=1&size=20", token);
        assertThat(projects.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode page = objectMapper.readTree(projects.getBody()).path("data");
        assertThat(page.path("total").asLong()).isEqualTo(2L);
        assertThat(page.path("page").asInt()).isEqualTo(1);
        JsonNode row = page.path("items").get(0);
        assertThat(row.path("projectId").asText()).isEqualTo(PROJECT_ID);
        assertThat(row.path("cost").path("CNY").asDouble()).isEqualTo(12.3456);
        assertThat(row.path("allUnpriced").asBoolean()).isFalse();

        // 单项目下钻：subject 原值回显 + unpriced 档位（bySubject 口径无计数）+ 双分解
        ResponseEntity<String> detail = getWithToken(
                COSTS_ENDPOINT + "/projects/" + PROJECT_ID + COST_WINDOW_QUERY, token);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode detailData = objectMapper.readTree(detail.getBody()).path("data");
        assertThat(detailData.path("projectId").asText()).isEqualTo(PROJECT_ID);
        JsonNode tier = detailData.path("unpriced").get(0);
        assertThat(tier.path("tokenKind").asInt()).isEqualTo(1);
        assertThat(tier.path("tokenKindName").asText()).isEqualTo("输入");
        assertThat(detailData.path("byAgentKind").get(0).path("agentKindName").asText()).isEqualTo("执行智能体");
    }

    @Test
    @DisplayName("成本端点时间窗 from/to 必填 → 缺参 400 / 非 Instant 404（框架绑定层裁决，不到 provider）")
    void given_missingOrInvalidWindow_when_costEndpoints_then_bindingLayerRejects() {
        String token = login(usernameWithReadPermission);
        // 缺 to（必填）→ 400（MissingServletRequestParameterException）；非 Instant（无时区态）→ 404
        // （框架 handleTypeMismatch → NOT_FOUND 既定口径）——均在本服务绑定层，不到 provider
        assertThat(getWithToken(COSTS_ENDPOINT + "/overview?from=" + COST_FROM, token)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getWithToken(COSTS_ENDPOINT + "/overview?from=2026-09-01&to=" + COST_TO, token)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 成本四端点 403（独立权限码 cost:read）")
    void given_nonSuperAdminWithoutPermission_when_costEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(COSTS_ENDPOINT + "/overview" + COST_WINDOW_QUERY, token)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(COSTS_ENDPOINT + "/unpriced" + COST_WINDOW_QUERY, token)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(COSTS_ENDPOINT + "/projects" + COST_WINDOW_QUERY, token)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(COSTS_ENDPOINT + "/projects/" + PROJECT_ID + COST_WINDOW_QUERY, token)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问成本四端点返回 401")
    void given_unauthenticated_when_costEndpoints_then_401() {
        assertThat(getWithToken(COSTS_ENDPOINT + "/overview" + COST_WINDOW_QUERY, null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(COSTS_ENDPOINT + "/unpriced" + COST_WINDOW_QUERY, null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(COSTS_ENDPOINT + "/projects" + COST_WINDOW_QUERY, null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(COSTS_ENDPOINT + "/projects/" + PROJECT_ID + COST_WINDOW_QUERY, null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("成本查询下游 METER_011 → 北向 HTTP 400 + 信封 code=3011 + message 原文（不映射）")
    void given_meter011FromDownstream_when_getOverview_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.getCostOverview(any())).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":3011,\"message\":\"无效的成本查询参数\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(COSTS_ENDPOINT + "/overview" + COST_WINDOW_QUERY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 3011（METER_011＝域码 3×1000＋11），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(3011);
        assertThat(root.path("message").asText()).isEqualTo("无效的成本查询参数");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 单价表（admin:aiplatform:price-entry:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:price-entry:read → 清单 200，含历史行 + tokenKind/*Name + unitPrice String + 留痕镜像 provider")
    void given_nonSuperAdminWithPriceEntryRead_when_list_then_200AndShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        // provider/model 精确过滤 + 分页 1-based 直传（page=2 无 ±1）
        ResponseEntity<String> response = getWithToken(
                PRICE_ENTRIES_ENDPOINT + "?provider=anthropic&model=claude-fable-5&page=2&size=20", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // PageResponse 形状：items/total/page/size，回显 provider 回报值（page=2——1-based 原值）
        assertThat(data.path("total").asLong()).isEqualTo(12L);
        assertThat(data.path("page").asInt()).isEqualTo(2);
        assertThat(data.path("size").asInt()).isEqualTo(20);
        // 首行：当前行（effectiveTo 出 JSON null）——tokenKind Integer code + tokenKindName、
        // unitPrice String 明文小数、操作者留痕两肢
        JsonNode current = data.path("items").get(0);
        assertThat(current.path("id").asText()).isEqualTo(PRICE_ENTRY_ID);
        assertThat(current.path("tokenKind").asInt()).isEqualTo(1);
        assertThat(current.path("tokenKindName").asText()).isEqualTo("输入");
        assertThat(current.path("unitPrice").asText()).isEqualTo("0.000002");
        assertThat(current.path("effectiveFrom").asText()).isEqualTo("2026-08-01T00:00:00Z");
        assertThat(current.path("effectiveTo").isNull()).isTrue();
        assertThat(current.path("operatorId").asText()).isEqualTo("700160");
        // 次行：历史行（区间两端俱全）——存量形制操作者两列如实出 JSON null（全局 Jackson 含 null）
        JsonNode historical = data.path("items").get(1);
        assertThat(historical.path("effectiveTo").asText()).isEqualTo("2026-08-01T00:00:00Z");
        assertThat(historical.path("operatorId").isNull()).isTrue();
        assertThat(historical.path("operatorName").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且缺少权限 → 单价表三端点 403（read 与两写码皆无）")
    void given_nonSuperAdminWithoutPermission_when_priceEntryEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(PRICE_ENTRIES_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(DEACTIVATE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("单价表清单非法分页值 → 404（本服务绑定层类型不匹配裁决，不到 provider——METER_009 不经北向暴露）")
    void given_illegalPageParam_when_listPriceEntries_then_bindingLayerRejects() {
        String token = login(usernameWithReadPermission);
        // 非整数 page 在北向 int 绑定即类型不匹配，按框架 handleTypeMismatch 既定口径 404
        // （同成本域非 Instant 404 先例）——provider 的 METER_009 口径不经北向可达
        assertThat(getWithToken(PRICE_ENTRIES_ENDPOINT + "?page=abc", token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("仅有 price-entry:read（无写码）→ 改价/停用 403（read ≠ 两写码）")
    void given_nonSuperAdminWithReadOnly_when_priceEntryActions_then_403() {
        String token = login(usernameWithReadPermission);
        assertThat(postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(DEACTIVATE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问单价表三端点返回 401")
    void given_unauthenticated_when_priceEntryEndpoints_then_401() {
        assertThat(getWithToken(PRICE_ENTRIES_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(DEACTIVATE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== 单价表写操作（reprice/deactivate 两独立写码）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有双写码 → 改价/停用 200，closed/opened 双行回执且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithBothWriteCodes_when_repriceAndDeactivate_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithPriceWrite);

        // 改价：回执＝{closed, opened} 双行——closed 落 effectiveTo＝新起点（保留原开行留痕 700160），
        // opened 沿用匹配键、新单价 String、敞口（effectiveTo null）、带改价操作者
        ResponseEntity<String> reprice = postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, token);
        assertThat(reprice.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(reprice.getBody()).path("data");
        JsonNode closed = data.path("closed");
        assertThat(closed.path("id").asText()).isEqualTo(PRICE_ENTRY_ID);
        assertThat(closed.path("effectiveTo").asText()).isEqualTo("2026-09-20T00:00:00Z");
        assertThat(closed.path("operatorId").asText()).isEqualTo("700160");
        JsonNode opened = data.path("opened");
        assertThat(opened.path("id").asText()).isEqualTo("3830100003333333");
        assertThat(opened.path("unitPrice").asText()).isEqualTo("0.0000018");
        assertThat(opened.path("effectiveFrom").asText()).isEqualTo("2026-09-20T00:00:00Z");
        assertThat(opened.path("effectiveTo").isNull()).isTrue();
        // 改价操作者＝登录运营（RequestContext 透传落痕，provider 口径）
        assertThat(opened.path("operatorId").asText()).isEqualTo(String.valueOf(priceWriteUserId));
        assertThat(opened.path("operatorName").asText()).isEqualTo(priceWriteUserNickname);

        // 停用：被关行单行回执（effectiveTo 已落、停用操作者落被关行）
        ResponseEntity<String> deactivate = postWithToken(DEACTIVATE_ENDPOINT, token);
        assertThat(deactivate.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode closedRow = objectMapper.readTree(deactivate.getBody()).path("data");
        assertThat(closedRow.path("id").asText()).isEqualTo(PRICE_ENTRY_ID);
        assertThat(closedRow.path("effectiveTo").asText()).isEqualTo("2026-09-16T10:30:00Z");

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头）：== 登录调价运营的 id/昵称（沙箱四动作同款手法）
        assertThat(capturedOperatorId.get()).isEqualTo(priceWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(priceWriteUserNickname);
    }

    @Test
    @DisplayName("仅有 price-entry:reprice 单写码 → 改价 200、停用 403（两写码彼此独立，最小授权）")
    void given_nonSuperAdminWithRepriceOnly_when_actions_then_reprice200Deactivate403() {
        String token = login(usernameWithRepriceOnly);
        assertThat(postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(DEACTIVATE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("单价表改价下游 METER_008 → 北向 HTTP 409 + 信封 code=3008 + message 原文（不映射）")
    void given_meter008FromDownstream_when_reprice_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.repricePriceEntry(eq(PRICE_ENTRY_ID), any(AiplatformPriceEntryRepriceWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":3008,\"message\":\"同匹配键生效区间重叠（跨区间或同起点）\",\"data\":null}")));

        String token = login(usernameWithPriceWrite);
        ResponseEntity<String> response = postWithToken(REPRICE_ENDPOINT, REPRICE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 3008（METER_008＝域码 3×1000＋8），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(3008);
        assertThat(root.path("message").asText()).isEqualTo("同匹配键生效区间重叠（跨区间或同起点）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 知识素材（admin:aiplatform:material:read）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:material:read → 清单 200，三维过滤 + status/*Name + 留痕镜像 provider")
    void given_nonSuperAdminWithMaterialRead_when_listMaterials_then_200AndShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        // 三维过滤（status 单选 + 沉淀时间闭区间 Instant + projectId 精确）+ 分页 1-based 直传
        ResponseEntity<String> response = getWithToken(
                MATERIALS_ENDPOINT + "?status=2&sunkFrom=2026-09-01T00:00:00Z&sunkTo=2026-09-15T23:59:59Z"
                        + "&projectId=" + PROJECT_ID + "&page=2&size=20", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // PageResponse 形状：items/total/page/size，回显 provider 回报值（page=2——1-based 原值）
        assertThat(data.path("total").asLong()).isEqualTo(9L);
        assertThat(data.path("page").asInt()).isEqualTo(2);
        assertThat(data.path("size").asInt()).isEqualTo(20);
        // 首行：已治理行——status Integer code + statusName、来源项目引用、治理留痕两肢
        JsonNode governed = data.path("items").get(0);
        assertThat(governed.path("id").asText()).isEqualTo(MATERIAL_ID);
        assertThat(governed.path("kind").asText()).isEqualTo("PRD");
        assertThat(governed.path("projectId").asText()).isEqualTo(PROJECT_ID);
        assertThat(governed.path("projectName").asText()).isEqualTo("英语学习助手");
        assertThat(governed.path("status").asInt()).isEqualTo(2);
        assertThat(governed.path("statusName").asText()).isEqualTo("停用");
        assertThat(governed.path("sunkAt").asText()).isEqualTo("2026-09-01T08:30:00Z");
        assertThat(governed.path("operatorId").asText()).isEqualTo(String.valueOf(materialWriteUserId));
        assertThat(governed.path("operatorName").asText()).isEqualTo(materialWriteUserNickname);
        // 次行：未治理行——操作者两列如实出 JSON null（全局 Jackson 含 null）
        JsonNode untreated = data.path("items").get(1);
        assertThat(untreated.path("status").asInt()).isEqualTo(1);
        assertThat(untreated.path("statusName").asText()).isEqualTo("启用");
        assertThat(untreated.path("operatorId").isNull()).isTrue();
        assertThat(untreated.path("operatorName").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 material:read → 详情 200，元数据 + PRD 全文 content 镜像 provider")
    void given_nonSuperAdminWithMaterialRead_when_getMaterialDetail_then_200WithFullContent() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(MATERIAL_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(MATERIAL_ID);
        assertThat(data.path("status").asInt()).isEqualTo(1);
        assertThat(data.path("statusName").asText()).isEqualTo("启用");
        // 全文＝块按 seq 以空行拼接（多段形态原样到达）；未治理过的操作者两列 JSON null
        assertThat(data.path("content").asText()).startsWith("# PRD");
        assertThat(data.path("content").asText()).contains("\n\n## 功能范围\n\n1. 单词卡片\n2. 复习计划");
        assertThat(data.path("operatorId").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且缺少权限 → 素材五端点 403（read 与三写码皆无）")
    void given_nonSuperAdminWithoutPermission_when_materialEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(MATERIALS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(MATERIAL_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(MATERIAL_DISABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(MATERIAL_ENABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(MATERIAL_DELETE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("素材清单非法参数 → 404（本服务绑定层类型不匹配裁决，不到 provider——KNW_007 不经北向暴露）")
    void given_illegalFilterParam_when_listMaterials_then_bindingLayerRejects() {
        String token = login(usernameWithReadPermission);
        // 非整数 status / 非 Instant 沉淀时间（无时区态）在北向绑定即类型不匹配，按框架
        // handleTypeMismatch 既定口径 404——provider 的 KNW_007 口径仅剩「数值但未知 code」一径可达
        assertThat(getWithToken(MATERIALS_ENDPOINT + "?status=abc", token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getWithToken(MATERIALS_ENDPOINT + "?sunkFrom=2026-09-01", token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("未登录访问素材五端点返回 401（含 DELETE）")
    void given_unauthenticated_when_materialEndpoints_then_401() {
        assertThat(getWithToken(MATERIALS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(MATERIAL_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(MATERIAL_DISABLE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(MATERIAL_ENABLE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(deleteWithToken(MATERIAL_DELETE_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== 素材三治理动作（disable/enable/delete 三独立写码）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有三写码 → 停用/启用/删除 200，回执=summary 且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithThreeWriteCodes_when_governanceActions_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithMaterialWrite);

        // 停用：回执＝provider 重读登记行——已停用 + 治理操作者落素材级
        ResponseEntity<String> disable = postWithToken(MATERIAL_DISABLE_ENDPOINT, token);
        assertThat(disable.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode disabled = objectMapper.readTree(disable.getBody()).path("data");
        assertThat(disabled.path("id").asText()).isEqualTo(MATERIAL_ID);
        assertThat(disabled.path("status").asInt()).isEqualTo(2);
        assertThat(disabled.path("statusName").asText()).isEqualTo("停用");
        assertThat(disabled.path("operatorId").asText()).isEqualTo(String.valueOf(materialWriteUserId));
        assertThat(disabled.path("operatorName").asText()).isEqualTo(materialWriteUserNickname);

        // 启用：恢复启用（可逆开关的另一侧）
        ResponseEntity<String> enable = postWithToken(MATERIAL_ENABLE_ENDPOINT, token);
        assertThat(enable.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode enabled = objectMapper.readTree(enable.getBody()).path("data");
        assertThat(enabled.path("status").asInt()).isEqualTo(1);
        assertThat(enabled.path("statusName").asText()).isEqualTo("启用");

        // 删除：回执＝删除前终态（provider 契约如此——确认移除了什么；本例是一行已停用素材）
        ResponseEntity<String> delete = deleteWithToken(MATERIAL_DELETE_ENDPOINT, token);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deleted = objectMapper.readTree(delete.getBody()).path("data");
        assertThat(deleted.path("id").asText()).isEqualTo(MATERIAL_ID);
        assertThat(deleted.path("status").asInt()).isEqualTo(2);

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 缺头必拦 KNW_006——issue #69 断言必带）：
        // == 登录内容治理运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(materialWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(materialWriteUserNickname);
    }

    @Test
    @DisplayName("仅有 material:read（无写码）→ 三治理动作 403（read ≠ 三写码）")
    void given_nonSuperAdminWithReadOnly_when_materialActions_then_403() {
        String token = login(usernameWithReadPermission);
        assertThat(postWithToken(MATERIAL_DISABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(MATERIAL_ENABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(MATERIAL_DELETE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("仅有 material:disable 单写码 → 停用 200、启用/删除 403（三写码彼此独立，可逆与不可逆分权）")
    void given_nonSuperAdminWithDisableOnly_when_actions_then_disable200Others403() {
        String token = login(usernameWithDisableOnly);
        assertThat(postWithToken(MATERIAL_DISABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(MATERIAL_ENABLE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(MATERIAL_DELETE_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("素材停用下游 KNW_006（缺操作者头）→ 北向 HTTP 400 + 信封 code=2006 + message 原文（不映射）")
    void given_knw006FromDownstream_when_disable_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.disableMaterial(eq(MATERIAL_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":2006,\"message\":\"操作者不能为空\",\"data\":null}")));

        String token = login(usernameWithMaterialWrite);
        ResponseEntity<String> response = postWithToken(MATERIAL_DISABLE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 2006（KNW_006＝域码 2×1000＋6），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(2006);
        assertThat(root.path("message").asText()).isEqualTo("操作者不能为空");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("素材删除下游 KNW_005（重复删除）→ 北向 HTTP 404 + 信封 code=2005 + message 原文（不映射）")
    void given_knw005FromDownstream_when_delete_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.deleteMaterial(eq(MATERIAL_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(404,
                        "{\"code\":2005,\"message\":\"知识素材不存在\",\"data\":null}")));

        String token = login(usernameWithMaterialWrite);
        ResponseEntity<String> response = deleteWithToken(MATERIAL_DELETE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 2005（KNW_005＝域码 2×1000＋5），而非映射后的 404——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(2005);
        assertThat(root.path("message").asText()).isEqualTo("知识素材不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 技能管理（admin:aiplatform:skill:read，技能线 T1 读面）· 权限三态 + 北向出口形状 ==========

    @Test
    @DisplayName("非超管且拥有 admin:aiplatform:skill:read → 清单 200，裸 List（不分页）+ opaque 两形制柄 + 内置/安装行镜像 provider")
    void given_nonSuperAdminWithSkillRead_when_listSkills_then_200AndBareListMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILLS_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // data 为裸 List（有界目录不分页——无 PageResponse 信封，排序服务端定死）
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);

        // 首行：内置行——builtin: 柄原样（opaque 串两形制之一）、source/sourceName 1/内置、
        // 来源包/版本/操作者/新版标记/使用计数两肢如实出 JSON null（全局 Jackson 含 null；
        // 观测面内置恒 null 不适用）
        JsonNode builtin = data.get(0);
        assertThat(builtin.path("id").asText()).isEqualTo(BUILTIN_SKILL_HANDLE);
        assertThat(builtin.path("name").asText()).isEqualTo("prd-writing");
        assertThat(builtin.path("source").asInt()).isEqualTo(1);
        assertThat(builtin.path("sourceName").asText()).isEqualTo("内置");
        assertThat(builtin.path("status").asInt()).isEqualTo(1);
        assertThat(builtin.path("statusName").asText()).isEqualTo("启用");
        assertThat(builtin.path("sourcePackage").isNull()).isTrue();
        assertThat(builtin.path("version").isNull()).isTrue();
        assertThat(builtin.path("operatorId").isNull()).isTrue();
        assertThat(builtin.path("updateAvailable").isNull()).isTrue();
        assertThat(builtin.path("loadCount").isNull()).isTrue();
        assertThat(builtin.path("lastLoadedAt").isNull()).isTrue();

        // 次行：安装行——TSID 十进制串柄（两形制之二）、source/status Integer + *Name 随行、
        // 来源包＋装时 commit＋治理留痕两肢＋新版标记 true＋使用计数实值——loadCount 北向出
        // JSON string（cartisan-web 全局 Long→ToStringSerializer，同 id 口径）
        JsonNode installed = data.get(1);
        assertThat(installed.path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(installed.path("source").asInt()).isEqualTo(2);
        assertThat(installed.path("sourceName").asText()).isEqualTo("安装");
        assertThat(installed.path("sourcePackage").asText()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(installed.path("version").asText()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(installed.path("status").asInt()).isEqualTo(2);
        assertThat(installed.path("statusName").asText()).isEqualTo("停用");
        assertThat(installed.path("operatorId").asText()).isEqualTo("700200");
        assertThat(installed.path("operatorName").asText()).isEqualTo("运营·技能管理员");
        assertThat(installed.path("updateAvailable").asBoolean()).isTrue();
        assertThat(installed.path("loadCount").isTextual()).isTrue();
        assertThat(installed.path("loadCount").asText()).isEqualTo("5");
        assertThat(installed.path("lastLoadedAt").asText()).isEqualTo("2026-09-20T10:00:00");
    }

    @Test
    @DisplayName("非超管且拥有 skill:read → 详情 200，元数据 + frontmatter 自由键值 + 正文全文（审核面）镜像 provider")
    void given_nonSuperAdminWithSkillRead_when_getSkillDetail_then_200WithFrontmatterAndContent() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILL_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(data.path("source").asInt()).isEqualTo(2);
        assertThat(data.path("sourceName").asText()).isEqualTo("安装");
        // frontmatter 自由键值对象逐字透传：name/description 之外的键（license）原样在场
        assertThat(data.path("frontmatter").path("name").asText()).isEqualTo("web-research");
        assertThat(data.path("frontmatter").path("license").asText()).isEqualTo("MIT");
        // 正文全文（frontmatter 剥离后）原样到达——安装审核承载面
        assertThat(data.path("content").asText()).startsWith("# Web Research");
        assertThat(data.path("content").asText()).contains("\n\n## 步骤\n\n1. 拆解问题\n2. 检索来源");
        assertThat(data.path("updateAvailable").asBoolean()).isFalse();
        // scripts/ 资源面（#253 漂移补齐）：相对路径→内容全文（审核须可读）＋使用计数实值
        // （loadCount 北向 JSON string——同 id 口径）
        assertThat(data.path("resources").path("scripts/run-tests.sh").asText())
                .isEqualTo("#!/bin/bash\nset -e\n");
        assertThat(data.path("resources").size()).isEqualTo(1);
        assertThat(data.path("loadCount").isTextual()).isTrue();
        assertThat(data.path("loadCount").asText()).isEqualTo("5");
        assertThat(data.path("lastLoadedAt").asText()).isEqualTo("2026-09-20T10:00:00");
    }

    @Test
    @DisplayName("非超管且缺少权限 → 技能两端点 403（独立权限码 skill:read，技能线 per-verb 11 码之首）")
    void given_nonSuperAdminWithoutPermission_when_skillEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(SKILLS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(SKILL_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问技能两端点返回 401")
    void given_unauthenticated_when_skillEndpoints_then_401() {
        assertThat(getWithToken(SKILLS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(SKILL_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("技能详情下游 SKL_001（未寻址/畸形柄）→ 北向 HTTP 404 + 信封 code=7001 + message 原文（不映射）")
    void given_skl001FromDownstream_when_getSkillDetail_then_errorEnvelopePassedThrough() throws Exception {
        // 畸形/未寻址柄由 provider 裁决（admin 不预检）——mock 任一未寻址柄触发透传路径
        when(aiplatformClient.getSkill(eq("builtin:no-such-skill"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":7001,\"message\":\"技能不存在\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILLS_ENDPOINT + "/builtin:no-such-skill", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7001（SKL_001＝域码 7×1000＋1），而非映射后的 404——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7001);
        assertThat(root.path("message").asText()).isEqualTo("技能不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 技能四写操作（install/disable/enable/uninstall 四独立写码）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有四写码 → 安装/停用/启用/卸载 200，回执共形清单行且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithFourWriteCodes_when_skillActions_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithSkillWrite);

        // 安装（带命令体 repoUrl＋excludeDirs）：回执＝本次装入的条目清单（裸 List，与清单行共形），
        // 装者留痕两肢＝登录技能治理运营
        ResponseEntity<String> install = postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, token);
        assertThat(install.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode installed = objectMapper.readTree(install.getBody()).path("data");
        assertThat(installed.isArray()).isTrue();
        assertThat(installed.size()).isEqualTo(2);
        JsonNode firstInstalled = installed.get(0);
        assertThat(firstInstalled.path("id").asText()).isEqualTo("3852300002222222");
        assertThat(firstInstalled.path("source").asInt()).isEqualTo(2);
        assertThat(firstInstalled.path("sourceName").asText()).isEqualTo("安装");
        assertThat(firstInstalled.path("sourcePackage").asText()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(firstInstalled.path("status").asInt()).isEqualTo(1);
        assertThat(firstInstalled.path("statusName").asText()).isEqualTo("启用");
        assertThat(firstInstalled.path("operatorId").asText()).isEqualTo(String.valueOf(skillWriteUserId));
        assertThat(firstInstalled.path("operatorName").asText()).isEqualTo(skillWriteUserNickname);
        // 装时快照刚落，新版标记未检查过＝JSON null 如实在场（全局 Jackson 含 null）
        assertThat(firstInstalled.path("updateAvailable").isNull()).isTrue();

        // 停用（无请求体）：回执＝翻转后清单行——停用态＋最近动作者
        ResponseEntity<String> disable = postWithToken(SKILL_DISABLE_ENDPOINT, token);
        assertThat(disable.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode disabled = objectMapper.readTree(disable.getBody()).path("data");
        assertThat(disabled.path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(disabled.path("status").asInt()).isEqualTo(2);
        assertThat(disabled.path("statusName").asText()).isEqualTo("停用");
        assertThat(disabled.path("operatorId").asText()).isEqualTo(String.valueOf(skillWriteUserId));

        // 启用：可逆开关另一侧——恢复启用
        ResponseEntity<String> enable = postWithToken(SKILL_ENABLE_ENDPOINT, token);
        assertThat(enable.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(enable.getBody()).path("data").path("statusName").asText())
                .isEqualTo("启用");

        // 卸载（DELETE 动词）：回执＝删除前终态（确认移除了什么）
        ResponseEntity<String> uninstall = deleteWithToken(SKILL_UNINSTALL_ENDPOINT, token);
        assertThat(uninstall.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode removed = objectMapper.readTree(uninstall.getBody()).path("data");
        assertThat(removed.path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(removed.path("status").asInt()).isEqualTo(1);

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 缺头必拦 SKL_009——issue #81 断言必带）：
        // == 登录技能治理运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(skillWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(skillWriteUserNickname);
    }

    @Test
    @DisplayName("仅有 skill:read（无写码）→ 四写操作 403（read ≠ 四写码）")
    void given_nonSuperAdminWithReadOnly_when_skillWriteActions_then_403() {
        String token = login(usernameWithReadPermission);
        assertThat(postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_DISABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_ENABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(SKILL_UNINSTALL_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("仅有 skill:disable 单写码 → 停用 200、安装/启用/卸载 403（四写码彼此独立，可逆与装/卸分权）")
    void given_nonSuperAdminWithDisableOnly_when_skillActions_then_disable200Others403() {
        String token = login(usernameWithSkillDisableOnly);
        assertThat(postWithToken(SKILL_DISABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_ENABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(SKILL_UNINSTALL_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 技能四写端点 403")
    void given_nonSuperAdminWithoutPermission_when_skillWriteEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_DISABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_ENABLE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteWithToken(SKILL_UNINSTALL_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问技能四写端点返回 401（含 DELETE 卸载）")
    void given_unauthenticated_when_skillWriteEndpoints_then_401() {
        assertThat(postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(SKILL_DISABLE_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(SKILL_ENABLE_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(deleteWithToken(SKILL_UNINSTALL_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("安装下游 SKL_003（同源重复安装）→ 北向 HTTP 409 + 信封 code=7003 + message 原文（不映射）")
    void given_skl003FromDownstream_when_install_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.installSkills(any(AiplatformSkillInstallWireRequest.class))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":7003,\"message\":\"该技能仓库已安装过，不能重复安装（同源去重，更新走显式更新）\","
                                + "\"data\":null}")));

        String token = login(usernameWithSkillWrite);
        ResponseEntity<String> response = postWithToken(SKILL_INSTALL_ENDPOINT, SKILL_INSTALL_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7003（SKL_003＝域码 7×1000＋3），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7003);
        assertThat(root.path("message").asText())
                .isEqualTo("该技能仓库已安装过，不能重复安装（同源去重，更新走显式更新）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("卸载下游 SKL_008（有指派在身拒卸）→ 北向 HTTP 409 + 信封 code=7008 + message 原文（不映射）")
    void given_skl008FromDownstream_when_uninstall_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.uninstallSkill(eq(SKILL_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":7008,\"message\":\"技能有指派在身，先解绑再卸载\",\"data\":null}")));

        String token = login(usernameWithSkillWrite);
        ResponseEntity<String> response = deleteWithToken(SKILL_UNINSTALL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7008（SKL_008＝域码 7×1000＋8），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7008);
        assertThat(root.path("message").asText()).isEqualTo("技能有指派在身，先解绑再卸载");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 槽位指派（skill:read 读面 / skill:assign 写面，技能线 T3）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有 skill:read → 指派读面 200，{slot, skills[]} 含停用行实态（指派关系随行保留）")
    void given_nonSuperAdminWithSkillRead_when_getSlotAssignments_then_200AssignmentShapeMirrorsProvider()
            throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SLOT_ASSIGNMENTS_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // data 为 {slot, skills[]} 单对象：slot 键原样回显（字符串路径参数透传）
        assertThat(data.path("slot").asText()).isEqualTo(SLOT_KEY);
        assertThat(data.path("skills").isArray()).isTrue();
        assertThat(data.path("skills").size()).isEqualTo(2);

        // 首行启用行——指派行集正常面（与清单行共形）
        JsonNode enabled = data.path("skills").get(0);
        assertThat(enabled.path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(enabled.path("source").asInt()).isEqualTo(2);
        assertThat(enabled.path("status").asInt()).isEqualTo(1);
        assertThat(enabled.path("statusName").asText()).isEqualTo("启用");
        assertThat(enabled.path("operatorId").asText()).isEqualTo(String.valueOf(skillAssignUserId));

        // 次行停用行——「指派了但已停用」实态如实出 JSON（启停是可逆开关、指派关系随行保留；
        // 停用行不参与装配合成的裁决在 provider，admin 不过滤）；新版标记 null 如实在场
        JsonNode disabled = data.path("skills").get(1);
        assertThat(disabled.path("status").asInt()).isEqualTo(2);
        assertThat(disabled.path("statusName").asText()).isEqualTo("停用");
        assertThat(disabled.path("updateAvailable").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有 skill:assign → 指派整包替换 200，回执与 GET 同形且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithSkillAssign_when_assignSlot_then_200ReceiptSameShapeAsGet_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithSkillAssign);

        ResponseEntity<String> response = putWithToken(SLOT_ASSIGNMENTS_ENDPOINT, SLOT_ASSIGN_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 回执＝替换后读面（与 GET 同形）：slot 键＋指派行集（PUT 全量语义——skillIds 即终态）
        assertThat(data.path("slot").asText()).isEqualTo(SLOT_KEY);
        assertThat(data.path("skills").size()).isEqualTo(2);
        assertThat(data.path("skills").get(1).path("statusName").asText()).isEqualTo("停用");

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 整包替换留最近动作者、缺头必拦 SKL_009
        // ——issue #82 断言必带）：== 登录装配运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(skillAssignUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(skillAssignUserNickname);
    }

    @Test
    @DisplayName("仅有 skill:read（无 assign）→ 指派写 403；仅有 skill:assign（无 read）→ 指派读面 403（读写两码不串）")
    void given_readOrAssignOnly_when_slotAssignmentEndpoints_then_403OnTheOtherSide() {
        // 读面复用 skill:read（issue #82 验收）：read ≠ assign，写面拒
        String reader = login(usernameWithReadPermission);
        assertThat(putWithToken(SLOT_ASSIGNMENTS_ENDPOINT, SLOT_ASSIGN_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // assign 不带读面：GET 走 skill:read 拒
        String assigner = login(usernameWithSkillAssign);
        assertThat(getWithToken(SLOT_ASSIGNMENTS_ENDPOINT, assigner).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 指派两端点 403")
    void given_nonSuperAdminWithoutPermission_when_slotAssignmentEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(SLOT_ASSIGNMENTS_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(putWithToken(SLOT_ASSIGNMENTS_ENDPOINT, SLOT_ASSIGN_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问指派两端点返回 401（含 PUT 整包替换）")
    void given_unauthenticated_when_slotAssignmentEndpoints_then_401() {
        assertThat(getWithToken(SLOT_ASSIGNMENTS_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(putWithToken(SLOT_ASSIGNMENTS_ENDPOINT, SLOT_ASSIGN_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("指派读面下游 SKL_010（未知槽位）→ 北向 HTTP 404 + 信封 code=7010 + message 原文（不映射）")
    void given_skl010FromDownstream_when_getSlotAssignments_then_errorEnvelopePassedThrough() throws Exception {
        // 未知槽位由 provider 裁决（admin 不预检 slot 字符串路径参数）
        when(aiplatformClient.getSlotAssignments(eq("unknown-slot"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":7010,\"message\":\"职能槽位不存在\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(
                SKILLS_ENDPOINT + "/assignments/unknown-slot", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7010（SKL_010＝域码 7×1000＋10），而非映射后的 404——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7010);
        assertThat(root.path("message").asText()).isEqualTo("职能槽位不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("指派写下游 SKL_011（内置柄不可指派）→ 北向 HTTP 400 + 信封 code=7011 + message 原文（不映射）")
    void given_skl011FromDownstream_when_assignSlot_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.assignSlot(eq(SLOT_KEY), any(AiplatformSkillAssignWireRequest.class))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":7011,\"message\":\"内置技能不可指派（内置随平台发版，装配合成按配置挂载，无需指派）\","
                                + "\"data\":null}")));

        String token = login(usernameWithSkillAssign);
        ResponseEntity<String> response = putWithToken(SLOT_ASSIGNMENTS_ENDPOINT, SLOT_ASSIGN_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7011（SKL_011＝域码 7×1000＋11），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7011);
        assertThat(root.path("message").asText())
                .isEqualTo("内置技能不可指派（内置随平台发版，装配合成按配置挂载，无需指派）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 指派双头预检（复用 skill:assign，技能域第 16 端点，issue #90）· 权限三态 + 两态纪律回执形状 ==========

    @Test
    @DisplayName("非超管且拥有 skill:assign → 指派双头预检 200，回执 executed=true＋hints 逐条四字段（两态纪律载体，null 如实在场）")
    void given_nonSuperAdminWithSkillAssign_when_precheckSlot_then_200ThreeStateReceiptMirrorsProvider()
            throws Exception {
        String token = login(usernameWithSkillAssign);

        ResponseEntity<String> response = postWithToken(SLOT_PRECHECK_ENDPOINT, SLOT_ASSIGN_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 两态纪律回执：executed=true＋hints 非空＝发现方法论重叠（提示是输入不是门——判定权
        // 留管理员，非阻断不拦 PUT 指派）
        assertThat(data.path("executed").asBoolean()).isTrue();
        assertThat(data.path("hints").size()).isEqualTo(2);

        // 首条四字段俱全：skills（候选侧技能名）/counterpart（「工作协议」——双头对照侧）/
        // overlap（重叠内容）/resolution（消解方向）
        JsonNode full = data.path("hints").get(0);
        assertThat(full.path("skills").get(0).asText()).isEqualTo("报告生成");
        assertThat(full.path("counterpart").asText()).isEqualTo("工作协议");
        assertThat(full.path("overlap").asText()).isEqualTo("都在 PRD 产出环节做结构化写作，方法论高度重叠");
        assertThat(full.path("resolution").asText()).isEqualTo("二选一或收窄 description");

        // 次条 null 容差：hint 三 String 字段可 null（模型输出解析态）——出 JSON null 如实在场
        // （全局 Jackson 含 null），前端按 null 分支呈现、BFF 不臆造兜底文案
        JsonNode partial = data.path("hints").get(1);
        assertThat(partial.path("skills").size()).isEqualTo(2);
        assertThat(partial.path("counterpart").isNull()).isTrue();
        assertThat(partial.path("overlap").isNull()).isTrue();
        assertThat(partial.path("resolution").asText()).isEqualTo("可不指派其一");
    }

    @Test
    @DisplayName("仅有 skill:read（无 assign）→ 预检 403；无任何技能权限 → 预检 403（复用 assign，只读角色不得烧模型 token）")
    void given_readOnlyOrNoSkillPermission_when_precheckSlot_then_403() {
        // 预检复用 skill:assign（issue #90）：预检触发模型调用有真实成本（flash 档 skillcheck
        // 计量），read≠assign——读写两码不串，只读角色被拒
        String reader = login(usernameWithReadPermission);
        assertThat(postWithToken(SLOT_PRECHECK_ENDPOINT, SLOT_ASSIGN_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        String noPermission = login(usernameWithoutPermission);
        assertThat(postWithToken(SLOT_PRECHECK_ENDPOINT, SLOT_ASSIGN_BODY, noPermission).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问指派双头预检返回 401")
    void given_unauthenticated_when_precheckSlot_then_401() {
        assertThat(postWithToken(SLOT_PRECHECK_ENDPOINT, SLOT_ASSIGN_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== 显式更新＋留痕读面（skill:update 写面 / skill:read 读面，技能线 T4）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有 skill:update → 显式更新 200，回执含 from/to＋removedSkillNames＋终态行集，且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithSkillUpdate_when_update_then_200ReceiptShapeMirrorsProvider_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithSkillUpdate);

        ResponseEntity<String> response = postWithToken(SKILL_UPDATE_ENDPOINT, SKILL_UPDATE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 回执＝from/to 版本＋被移除技能名＋更新后终态行集（远端已前进代表面——issue #83 验收）
        assertThat(data.path("sourcePackage").asText()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(data.path("fromVersion").asText())
                .isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(data.path("toVersion").asText())
                .isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        assertThat(data.path("removedSkillNames").get(0).asText()).isEqualTo("legacy-summary");
        // 终态行集与清单行共形：首行同名行原地翻新（TSID 柄稳定＝SKILL_ID）、版本换新
        assertThat(data.path("skills").size()).isEqualTo(2);
        assertThat(data.path("skills").get(0).path("id").asText()).isEqualTo(SKILL_ID);
        assertThat(data.path("skills").get(0).path("version").asText())
                .isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        assertThat(data.path("operatorId").asText()).isEqualTo(String.valueOf(skillUpdateUserId));
        assertThat(data.path("operatorName").asText()).isEqualTo(skillUpdateUserNickname);

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 显式动作必留痕、缺头必拦 SKL_009
        // ——issue #83 断言必带）：== 登录版本治理运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(skillUpdateUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(skillUpdateUserNickname);
    }

    @Test
    @DisplayName("非超管且拥有 skill:read → 留痕读面 200，裸 List 时间倒序（最近先）＋版本链衔接")
    void given_nonSuperAdminWithSkillRead_when_updateTraces_then_200TraceListMirrorsProvider()
            throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(
                SKILL_UPDATE_TRACES_ENDPOINT + "?sourcePackage=https://github.com/acme/agent-skills", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // data 为裸 List（append-only 时间倒序——最近先；装时版本经链条首个 from 可回溯）
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);
        JsonNode latest = data.get(0);
        assertThat(latest.path("sourcePackage").asText()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(latest.path("fromVersion").asText())
                .isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(latest.path("toVersion").asText())
                .isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        assertThat(latest.path("operatorId").asText()).isEqualTo(String.valueOf(skillUpdateUserId));
        assertThat(latest.path("operatedAt").asText()).isEqualTo("2026-09-22T14:30:00");
        // 版本链衔接：次条 toVersion＝首条 fromVersion（append-only 链可回溯装时版本）
        assertThat(data.get(1).path("toVersion").asText()).isEqualTo(latest.path("fromVersion").asText());
    }

    @Test
    @DisplayName("仅有 skill:read（无 update）→ 更新写 403；仅有 skill:update（无 read）→ 留痕读面 403（读写两码不串）")
    void given_readOrUpdateOnly_when_updateEndpoints_then_403OnTheOtherSide() {
        // 更新写面独立 skill:update（issue #83 验收）：read ≠ update，写面拒
        String reader = login(usernameWithReadPermission);
        assertThat(postWithToken(SKILL_UPDATE_ENDPOINT, SKILL_UPDATE_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // update 不带读面：GET 留痕读面走 skill:read 拒
        String updater = login(usernameWithSkillUpdate);
        assertThat(getWithToken(
                SKILL_UPDATE_TRACES_ENDPOINT + "?sourcePackage=https://github.com/acme/agent-skills",
                updater).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 更新写面/留痕读面 403")
    void given_nonSuperAdminWithoutPermission_when_updateEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(postWithToken(SKILL_UPDATE_ENDPOINT, SKILL_UPDATE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(
                SKILL_UPDATE_TRACES_ENDPOINT + "?sourcePackage=https://github.com/acme/agent-skills",
                token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问更新写面/留痕读面返回 401（含 POST 显式更新）")
    void given_unauthenticated_when_updateEndpoints_then_401() {
        assertThat(postWithToken(SKILL_UPDATE_ENDPOINT, SKILL_UPDATE_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(
                SKILL_UPDATE_TRACES_ENDPOINT + "?sourcePackage=https://github.com/acme/agent-skills",
                null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("更新写下游 SKL_013（移除项有指派在身整体拒）→ 北向 HTTP 409 + 信封 code=7013 + message 原文（不映射）")
    void given_skl013FromDownstream_when_update_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.updateSkills(any(AiplatformSkillUpdateWireRequest.class))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":7013,\"message\":\"更新将移除有指派在身的技能，先解绑再更新\",\"data\":null}")));

        String token = login(usernameWithSkillUpdate);
        ResponseEntity<String> response = postWithToken(SKILL_UPDATE_ENDPOINT, SKILL_UPDATE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7013（SKL_013＝域码 7×1000＋13），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7013);
        assertThat(root.path("message").asText()).isEqualTo("更新将移除有指派在身的技能，先解绑再更新");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("留痕读面下游 SKL_014（来源包空/缺）→ 北向 HTTP 400 + 信封 code=7014 + message 原文（可选绑定不前置拦截）")
    void given_skl014FromDownstream_when_updateTracesWithoutParam_then_errorEnvelopePassedThrough() throws Exception {
        // sourcePackage 可选绑定（spec #79：admin 不在绑定层前置拦截）——缺参直出 provider，
        // provider 手工校验出 400 SKL_014 原样透传
        when(aiplatformClient.listSkillUpdateTraces(isNull())).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":7014,\"message\":\"来源包标识不能为空（取清单行 sourcePackage 值）\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILL_UPDATE_TRACES_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7014（SKL_014＝域码 7×1000＋14），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7014);
        assertThat(root.path("message").asText()).isEqualTo("来源包标识不能为空（取清单行 sourcePackage 值）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 智能体配置（agent-config:read 读面+留痕读面 / agent-config:update 写面，技能线 T5）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有 agent-config:read → 配置读面 200，生效值/覆盖标记/默认预览/工具开关/最近写者镜像 provider")
    void given_nonSuperAdminWithAgentConfigRead_when_getConfig_then_200ConfigShapeMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(AGENT_CONFIG_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 键两肢（稳定键 + 展示名 provider 自带 *Name）
        assertThat(data.path("agentKey").asText()).isEqualTo(AGENT_KEY);
        assertThat(data.path("agentName").asText()).isEqualTo("主智能体");
        // 生效值＝库覆盖（装配实取值）＋覆盖标记 true×2——「现在跑的是覆盖」
        assertThat(data.path("systemPrompt").asText()).startsWith("你是英语学习助手的主智能体");
        assertThat(data.path("modelId").asText()).isEqualTo("deepseek-v4-pro");
        assertThat(data.path("systemPromptOverridden").asBoolean()).isTrue();
        assertThat(data.path("modelIdOverridden").asBoolean()).isTrue();
        // 枚举默认预览（清空覆盖即落此值——恢复默认前可先看落点）
        assertThat(data.path("defaultSystemPrompt").asText()).contains("枚举默认全文");
        assertThat(data.path("defaultModelId").asText()).isEqualTo("deepseek-v4-flash");
        // 增强工具开关存储态（一开一关代表面）
        assertThat(data.path("webSearchEnabled").asBoolean()).isTrue();
        assertThat(data.path("fetchUrlEnabled").asBoolean()).isFalse();
        // 最近写者两肢＋写入时刻
        assertThat(data.path("operatorId").asText()).isEqualTo("700200");
        assertThat(data.path("operatorName").asText()).isEqualTo("运营·技能管理员");
        assertThat(data.path("updatedAt").asText()).isEqualTo("2026-09-22T14:30:00");
    }

    @Test
    @DisplayName("非超管且拥有 agent-config:read → 留痕读面 200，裸 List 时间倒序 + old*/new* 四件对（null 两侧）")
    void given_nonSuperAdminWithAgentConfigRead_when_traces_then_200TraceListMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(AGENT_CONFIG_TRACES_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // data 为裸 List（append-only 时间倒序——最近先；留痕不随任何动作删除）
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);

        // 首条最近（清空覆盖变更）：new prompt/档位 null＝本次清空回落枚举默认（如实出 JSON
        // null——全局 Jackson 含 null）；old 快照＝回滚写回依据
        JsonNode latest = data.get(0);
        assertThat(latest.path("id").asText()).isEqualTo("3900000002222222");
        assertThat(latest.path("oldSystemPrompt").asText()).isEqualTo("首次覆盖 prompt 全文");
        assertThat(latest.path("oldModelId").asText()).isEqualTo("deepseek-v4-pro");
        assertThat(latest.path("oldWebSearchEnabled").asBoolean()).isTrue();
        assertThat(latest.path("oldFetchUrlEnabled").asBoolean()).isFalse();
        assertThat(latest.path("newSystemPrompt").isNull()).isTrue();
        assertThat(latest.path("newModelId").isNull()).isTrue();
        assertThat(latest.path("newWebSearchEnabled").asBoolean()).isTrue();
        assertThat(latest.path("newFetchUrlEnabled").asBoolean()).isTrue();
        assertThat(latest.path("operatorId").asText()).isEqualTo(String.valueOf(agentConfigUpdateUserId));
        assertThat(latest.path("operatedAt").asText()).isEqualTo("2026-09-22T14:30:00");

        // 次条更早（首次配置）：old prompt/档位 null＝此前即缺省回落（回滚即把 null 写回）——
        // 留痕链衔接：次条 new＝首条 old（append-only 时间线诚实可查）
        JsonNode earlier = data.get(1);
        assertThat(earlier.path("oldSystemPrompt").isNull()).isTrue();
        assertThat(earlier.path("oldModelId").isNull()).isTrue();
        assertThat(earlier.path("newSystemPrompt").asText()).isEqualTo(latest.path("oldSystemPrompt").asText());
        assertThat(earlier.path("newModelId").asText()).isEqualTo(latest.path("oldModelId").asText());
    }

    @Test
    @DisplayName("非超管且拥有 agent-config:update → 全量写 200，回执与 GET 同形且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithAgentConfigUpdate_when_update_then_200ReceiptSameShapeAsGet_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithAgentConfigUpdate);

        ResponseEntity<String> response = putWithToken(AGENT_CONFIG_ENDPOINT, AGENT_CONFIG_UPDATE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 回执＝写入后读面（与 GET 同形——provider 落库后重读同一视图）：覆盖行代表面，
        // 最近写者＝登录智能体配置运营（provider 落痕口径：值面有动必落痕）
        assertThat(data.path("agentKey").asText()).isEqualTo(AGENT_KEY);
        assertThat(data.path("systemPromptOverridden").asBoolean()).isTrue();
        assertThat(data.path("modelId").asText()).isEqualTo("deepseek-v4-pro");
        assertThat(data.path("defaultModelId").asText()).isEqualTo("deepseek-v4-flash");
        assertThat(data.path("fetchUrlEnabled").asBoolean()).isFalse();
        assertThat(data.path("operatorId").asText()).isEqualTo(String.valueOf(agentConfigUpdateUserId));
        assertThat(data.path("operatorName").asText()).isEqualTo(agentConfigUpdateUserNickname);

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 配置变更必留痕、缺头必拦 PRJ_034
        // ——issue #84 断言必带）：== 登录智能体配置运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(agentConfigUpdateUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(agentConfigUpdateUserNickname);
    }

    @Test
    @DisplayName("仅有 agent-config:read（无 update）→ 全量写 403；仅有 agent-config:update（无 read）→ 读面/留痕读面 403（读写两码不串）")
    void given_readOrUpdateOnly_when_agentConfigEndpoints_then_403OnTheOtherSide() {
        // 写面独立 agent-config:update（issue #84 高敏感独立授权面）：read ≠ update，写面拒
        String reader = login(usernameWithReadPermission);
        assertThat(putWithToken(AGENT_CONFIG_ENDPOINT, AGENT_CONFIG_UPDATE_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // update 不带读面：GET 配置读面与留痕读面走 agent-config:read 拒
        String updater = login(usernameWithAgentConfigUpdate);
        assertThat(getWithToken(AGENT_CONFIG_ENDPOINT, updater).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(AGENT_CONFIG_TRACES_ENDPOINT, updater).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 智能体配置三端点 403")
    void given_nonSuperAdminWithoutPermission_when_agentConfigEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(AGENT_CONFIG_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(putWithToken(AGENT_CONFIG_ENDPOINT, AGENT_CONFIG_UPDATE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(AGENT_CONFIG_TRACES_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问智能体配置三端点返回 401（含 PUT 全量写）")
    void given_unauthenticated_when_agentConfigEndpoints_then_401() {
        assertThat(getWithToken(AGENT_CONFIG_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(putWithToken(AGENT_CONFIG_ENDPOINT, AGENT_CONFIG_UPDATE_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(AGENT_CONFIG_TRACES_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("读面下游 PRJ_033（未知智能体键——classify/naming 等一次性判定不属本面）→ 北向 HTTP 404 + 信封 code=4033 + message 原文（不映射）")
    void given_prj033FromDownstream_when_getConfig_then_errorEnvelopePassedThrough() throws Exception {
        // 未知键由 provider 裁决（admin 不预检 agentKey 字符串路径参数）
        when(aiplatformClient.getAgentConfig(eq("classify"))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(404,
                        "{\"code\":4033,\"message\":\"智能体不存在（运营配置面只有 main/executor 两座智能体）\","
                                + "\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(AGENT_CONFIGS_ENDPOINT + "/classify", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4033（PRJ_033＝域码 4×1000＋33），而非映射后的 404——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4033);
        assertThat(root.path("message").asText())
                .isEqualTo("智能体不存在（运营配置面只有 main/executor 两座智能体）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("全量写下游 PRJ_034（操作者缺头——配置变更必留痕）→ 北向 HTTP 400 + 信封 code=4034 + message 原文（不映射）")
    void given_prj034FromDownstream_when_update_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.updateAgentConfig(eq(AGENT_KEY), any(AiplatformAgentConfigUpdateWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":4034,\"message\":\"操作者不能为空\",\"data\":null}")));

        String token = login(usernameWithAgentConfigUpdate);
        ResponseEntity<String> response = putWithToken(AGENT_CONFIG_ENDPOINT, AGENT_CONFIG_UPDATE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4034（PRJ_034＝域码 4×1000＋34），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4034);
        assertThat(root.path("message").asText()).isEqualTo("操作者不能为空");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 工具面（agent-tool:read 清单读面 / agent-tool:toggle 开关写面，技能线 T6）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有 agent-tool:read → 工具面清单 200，三槽位×kind×挂载态镜像 provider（裸 List 不分页）")
    void given_nonSuperAdminWithAgentToolRead_when_listToolSlots_then_200InventoryMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(AGENT_TOOLS_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // data 为裸 List（三槽位有界——无 PageResponse 信封，不造分页）
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(3);

        // 三槽位键序 provider 定死（与技能槽位同键）＋展示名
        assertThat(data.get(0).path("slot").asText()).isEqualTo("main");
        assertThat(data.get(0).path("slotName").asText()).isEqualTo("主智能体");
        assertThat(data.get(1).path("slot").asText()).isEqualTo("executor");
        assertThat(data.get(2).path("slot").asText()).isEqualTo("subagent");

        // main 槽：骨架恒挂载（enabled true）＋增强两件一开一关（关＝在册但退出装配面——
        // fetch_url false 代表面；kind 类别名原样透传不映射）
        JsonNode mainTools = data.get(0).path("tools");
        assertThat(mainTools.size()).isEqualTo(4);
        assertThat(mainTools.get(0).path("name").asText()).isEqualTo("ask_user");
        assertThat(mainTools.get(0).path("kind").asText()).isEqualTo("SKELETON");
        assertThat(mainTools.get(0).path("enabled").asBoolean()).isTrue();
        JsonNode fetchUrl = mainTools.get(2);
        assertThat(fetchUrl.path("kind").asText()).isEqualTo("ENHANCEMENT");
        assertThat(fetchUrl.path("enabled").asBoolean()).isFalse();
        assertThat(mainTools.get(3).path("name").asText()).isEqualTo(TOOL_NAME);
        assertThat(mainTools.get(3).path("enabled").asBoolean()).isTrue();

        // executor 槽：骨架＋harness 内建呈现口径（不可开关——恒 true）
        JsonNode executorTools = data.get(1).path("tools");
        assertThat(executorTools.get(1).path("name").asText()).isEqualTo("read_file");
        assertThat(executorTools.get(1).path("kind").asText()).isEqualTo("HARNESS_BUILTIN");
        assertThat(executorTools.get(1).path("enabled").asBoolean()).isTrue();

        // subagent 槽：声明工具面（同一句声明描述）
        JsonNode subagentTools = data.get(2).path("tools");
        assertThat(subagentTools.get(0).path("description").asText()).contains("self-test 子智能体声明工具面");
    }

    @Test
    @DisplayName("非超管且拥有 agent-tool:toggle → 增强工具开关 200，回执=开关后工具行且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithAgentToolToggle_when_toggle_then_200ReceiptMirrorsProvider_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithAgentToolToggle);

        ResponseEntity<String> response = putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, AGENT_TOOL_TOGGLE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        // 回执＝开关后工具行（与清单嵌套行同形）：四件全绑定——关态代表面
        // （enabled=false＝退出槽位装配面——模型不可见）
        assertThat(data.path("name").asText()).isEqualTo(TOOL_NAME);
        assertThat(data.path("kind").asText()).isEqualTo("ENHANCEMENT");
        assertThat(data.path("enabled").asBoolean()).isFalse();
        assertThat(data.path("description").asText()).contains("增强可关");

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 开关写必留痕、缺头必拦 PRJ_034
        // ——issue #85 断言必带）：== 登录工具面运营的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(agentToolToggleUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(agentToolToggleUserNickname);
    }

    @Test
    @DisplayName("仅有 agent-tool:read（无 toggle）→ 开关写 403；仅有 agent-tool:toggle（无 read）→ 清单读面 403（读写两码不串）")
    void given_readOrToggleOnly_when_agentToolEndpoints_then_403OnTheOtherSide() {
        // 开关写面独立 agent-tool:toggle（issue #85 验收）：read ≠ toggle，写面拒
        String reader = login(usernameWithReadPermission);
        assertThat(putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, AGENT_TOOL_TOGGLE_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // toggle 不带读面：GET 清单走 agent-tool:read 拒
        String toggler = login(usernameWithAgentToolToggle);
        assertThat(getWithToken(AGENT_TOOLS_ENDPOINT, toggler).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 工具面两端点 403")
    void given_nonSuperAdminWithoutPermission_when_agentToolEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(AGENT_TOOLS_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, AGENT_TOOL_TOGGLE_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问工具面两端点返回 401（含 PUT 开关）")
    void given_unauthenticated_when_agentToolEndpoints_then_401() {
        assertThat(getWithToken(AGENT_TOOLS_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, AGENT_TOOL_TOGGLE_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("开关下游 PRJ_035（工具不存在——清单读面即全集）→ 北向 HTTP 404 + 信封 code=4035 + message 原文（不映射）")
    void given_prj035FromDownstream_when_toggle_then_errorEnvelopePassedThrough() throws Exception {
        // 未知名由 provider 裁决（admin 不预检 toolName 字符串路径参数）
        when(aiplatformClient.toggleAgentTool(eq("no_such_tool"), any(AiplatformAgentToolToggleWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(404,
                        "{\"code\":4035,\"message\":\"工具不存在（工具面按注册名寻址，清单读面即全集）\","
                                + "\"data\":null}")));

        String token = login(usernameWithAgentToolToggle);
        ResponseEntity<String> response = putWithToken(
                AGENT_TOOLS_ENDPOINT + "/no_such_tool", AGENT_TOOL_TOGGLE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4035（PRJ_035＝域码 4×1000＋35），而非映射后的 404——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4035);
        assertThat(root.path("message").asText()).isEqualTo("工具不存在（工具面按注册名寻址，清单读面即全集）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("开关下游 PRJ_036（骨架/harness 内建锁死）→ 北向 HTTP 403 + 信封 code=4036 + message 原文（不映射）")
    void given_prj036FromDownstream_when_toggle_then_errorEnvelopePassedThrough() throws Exception {
        // 骨架锁死判定归 provider（ADR-0021 编排权不下放配置——admin 不代判 kind）
        when(aiplatformClient.toggleAgentTool(eq("ask_user"), any(AiplatformAgentToolToggleWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(403,
                        "{\"code\":4036,\"message\":\"该工具不开放开关（骨架/harness 内建结构性锁死——编排权不下放配置）\","
                                + "\"data\":null}")));

        String token = login(usernameWithAgentToolToggle);
        ResponseEntity<String> response = putWithToken(
                AGENT_TOOLS_ENDPOINT + "/ask_user", AGENT_TOOL_TOGGLE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4036（PRJ_036＝域码 4×1000＋36），而非映射后的 403——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4036);
        assertThat(root.path("message").asText())
                .isEqualTo("该工具不开放开关（骨架/harness 内建结构性锁死——编排权不下放配置）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("开关下游 PRJ_037（缺目标态——enabled 必填无缺省）→ 北向 HTTP 400 + 信封 code=4037 + message 原文（可选绑定不前置拦截）")
    void given_prj037FromDownstream_when_toggleWithoutTarget_then_errorEnvelopePassedThrough() throws Exception {
        // enabled 缺省＝null 透传（Boolean 装箱与 provider 命令同型——provider @RequestBody(required=false)
        // 缺 body 容忍同语义），provider 手工校验出 400 PRJ_037 原样透传
        when(aiplatformClient.toggleAgentTool(eq(TOOL_NAME), any(AiplatformAgentToolToggleWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":4037,\"message\":\"开关目标态必填（enabled=true/false）\",\"data\":null}")));

        String token = login(usernameWithAgentToolToggle);
        ResponseEntity<String> response = putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, "{}", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4037（PRJ_037＝域码 4×1000＋37），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4037);
        assertThat(root.path("message").asText()).isEqualTo("开关目标态必填（enabled=true/false）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("开关下游 PRJ_034（操作者缺头——开关写必留痕）→ 北向 HTTP 400 + 信封 code=4034 + message 原文（不映射）")
    void given_prj034FromDownstream_when_toggle_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.toggleAgentTool(eq(TOOL_NAME), any(AiplatformAgentToolToggleWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":4034,\"message\":\"操作者不能为空\",\"data\":null}")));

        String token = login(usernameWithAgentToolToggle);
        ResponseEntity<String> response = putWithToken(AGENT_TOOL_TOGGLE_ENDPOINT, AGENT_TOOL_TOGGLE_BODY, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 4034（PRJ_034＝域码 4×1000＋34），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(4034);
        assertThat(root.path("message").asText()).isEqualTo("操作者不能为空");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 草稿审核面（skill-draft:read 读面 / skill-draft:promote、skill-draft:reject 写面，issue #92）· 权限三态 + 回执形状 + 操作者透传 ==========

    @Test
    @DisplayName("非超管且拥有 skill-draft:read → 在途清单 200，裸 List（不分页）＋血统三件＋扫描判定镜像 provider")
    void given_nonSuperAdminWithSkillDraftRead_when_listDrafts_then_200BareListMirrorsProvider() throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILL_DRAFTS_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // data 为裸 List（在途活跃面不分页——人审天然瓶颈；终态不列、恒 status=1）
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);

        // 首行（最近自荐）：跨项目血统三件＋CAUTION 判定（字符串透传不映射枚举）＋status Integer
        // code＋statusName 中文名随行
        JsonNode latest = data.get(0);
        assertThat(latest.path("id").asText()).isEqualTo("3852400002222222");
        assertThat(latest.path("name").asText()).isEqualTo("seo-checklist");
        assertThat(latest.path("slot").asText()).isEqualTo("executor");
        assertThat(latest.path("projectId").asText()).isEqualTo(PROJECT_ID);
        assertThat(latest.path("runId").asText()).isEqualTo("run-9f22e1");
        assertThat(latest.path("scanVerdict").asText()).isEqualTo("CAUTION");
        assertThat(latest.path("status").asInt()).isEqualTo(1);
        assertThat(latest.path("statusName").asText()).isEqualTo("在途");
        assertThat(latest.path("createdAt").asText()).isEqualTo("2026-09-28T16:40:00");

        // 次行（更早自荐）：SAFE 判定代表面
        JsonNode earlier = data.get(1);
        assertThat(earlier.path("id").asText()).isEqualTo(DRAFT_ID);
        assertThat(earlier.path("scanVerdict").asText()).isEqualTo("SAFE");
    }

    @Test
    @DisplayName("非超管且拥有 skill-draft:read → 草稿详情 200，正文全文＋findings untyped＋终态留痕四 null")
    void given_nonSuperAdminWithSkillDraftRead_when_getDraftDetail_then_200FindingsAndNullTraceMirrored()
            throws Exception {
        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(SKILL_DRAFT_DETAIL_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(DRAFT_ID);
        assertThat(data.path("status").asInt()).isEqualTo(1);
        assertThat(data.path("statusName").asText()).isEqualTo("在途");
        // 正文全文（a-only 无 scripts）原样到达——判读主体
        assertThat(data.path("content").asText()).startsWith("# Report Outline");
        // findings untyped map 逐条留档：line 为数字（Object 绑定混合类型在场）
        JsonNode finding = data.path("scanFindings").get(0);
        assertThat(finding.path("patternId").asText()).isEqualTo("secret-like-token");
        assertThat(finding.path("line").asInt()).isEqualTo(42);
        // 终态留痕四字段在途恒 null（如实出 JSON null——全局 Jackson 含 null）
        assertThat(data.path("operatorId").isNull()).isTrue();
        assertThat(data.path("operatorName").isNull()).isTrue();
        assertThat(data.path("reviewedAt").isNull()).isTrue();
        assertThat(data.path("rejectReason").isNull()).isTrue();
    }

    @Test
    @DisplayName("非超管且拥有两写码 → 晋升/拒绝 200，回执＝库行/终态详情且 RequestContext 透传登录 operator")
    void given_nonSuperAdminWithDraftWriteCodes_when_promoteAndReject_then_200_andRequestContextCarriesOperator()
            throws Exception {
        String token = login(usernameWithSkillDraftWrite);

        // 晋升（无请求体）：回执＝技能库清单行——来源 3 自产、来源包/版本＝固定虚拟值 self、
        // 启用态（入池即同权）、操作者＝晋升者；updateAvailable null＝自产无远端不适用
        ResponseEntity<String> promote = postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, token);
        assertThat(promote.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode promoted = objectMapper.readTree(promote.getBody()).path("data");
        assertThat(promoted.path("id").asText()).isEqualTo("3852300005555555");
        assertThat(promoted.path("source").asInt()).isEqualTo(3);
        assertThat(promoted.path("sourceName").asText()).isEqualTo("自产");
        assertThat(promoted.path("sourcePackage").asText()).isEqualTo("self");
        assertThat(promoted.path("version").asText()).isEqualTo("self");
        assertThat(promoted.path("status").asInt()).isEqualTo(1);
        assertThat(promoted.path("statusName").asText()).isEqualTo("启用");
        assertThat(promoted.path("operatorId").asText()).isEqualTo(String.valueOf(skillDraftWriteUserId));
        assertThat(promoted.path("operatorName").asText()).isEqualTo(skillDraftWriteUserNickname);
        assertThat(promoted.path("updateAvailable").isNull()).isTrue();

        // 拒绝（带 reason 命令体）：回执＝终态草稿详情——status=3 已拒绝、留痕四字段回填直读、
        // findings 空列表＝无发现
        ResponseEntity<String> reject = postWithToken(SKILL_DRAFT_REJECT_ENDPOINT, SKILL_DRAFT_REJECT_BODY, token);
        assertThat(reject.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rejected = objectMapper.readTree(reject.getBody()).path("data");
        assertThat(rejected.path("id").asText()).isEqualTo(DRAFT_ID);
        assertThat(rejected.path("status").asInt()).isEqualTo(3);
        assertThat(rejected.path("statusName").asText()).isEqualTo("已拒绝");
        assertThat(rejected.path("operatorId").asText()).isEqualTo(String.valueOf(skillDraftWriteUserId));
        assertThat(rejected.path("reviewedAt").asText()).isEqualTo("2026-09-29T11:20:00");
        assertThat(rejected.path("rejectReason").asText())
                .isEqualTo("正文与现有技能 prd-writing 方法论重叠，解消方向不明");
        assertThat(rejected.path("scanFindings").isArray()).isTrue();
        assertThat(rejected.path("scanFindings").size()).isZero();

        // 操作者身份不经 body——经 RequestContext 抵达出站调用点（OpenApiClient 据此带
        // X-User-Id/X-User-Name 出站头，provider 晋升/拒绝必留痕、缺头必拦 SKL_009
        // ——issue #92 断言必带）：== 登录技能审核员的 id/昵称
        assertThat(capturedOperatorId.get()).isEqualTo(skillDraftWriteUserId);
        assertThat(capturedOperatorName.get()).isEqualTo(skillDraftWriteUserNickname);
    }

    @Test
    @DisplayName("读写两码不串：无 skill-draft:read → 两读口 403；仅有 skill-draft:read（无写码）→ 两写口 403")
    void given_readCodesOnly_when_draftEndpoints_then_403OnTheOtherSide() {
        // 写码专用运营（promote+reject，无 skill-draft:read）→ 两读口 403：skill-draft 独立
        // 成码（连技能域既有读码也不放行——spec #91 三码分权）
        String draftWriter = login(usernameWithSkillDraftWrite);
        assertThat(getWithToken(SKILL_DRAFTS_ENDPOINT, draftWriter).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(SKILL_DRAFT_DETAIL_ENDPOINT, draftWriter).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // skill-draft:read（无写码）→ 两写口 403：审阅只读角色不得晋升/拒绝
        String reader = login(usernameWithReadPermission);
        assertThat(postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_DRAFT_REJECT_ENDPOINT, SKILL_DRAFT_REJECT_BODY, reader).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("仅有 skill-draft:promote 单写码 → 晋升 200、拒绝 403（两写码彼此独立，晋升权与拒绝权分权）")
    void given_nonSuperAdminWithPromoteOnly_when_draftActions_then_promote200Reject403() {
        String token = login(usernameWithDraftPromoteOnly);
        assertThat(postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWithToken(SKILL_DRAFT_REJECT_ENDPOINT, SKILL_DRAFT_REJECT_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("非超管且缺少权限 → 草稿四端点 403")
    void given_nonSuperAdminWithoutPermission_when_draftEndpoints_then_403() {
        String token = login(usernameWithoutPermission);
        assertThat(getWithToken(SKILL_DRAFTS_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getWithToken(SKILL_DRAFT_DETAIL_ENDPOINT, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(postWithToken(SKILL_DRAFT_REJECT_ENDPOINT, SKILL_DRAFT_REJECT_BODY, token).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("未登录访问草稿四端点返回 401（含 POST 晋升/拒绝）")
    void given_unauthenticated_when_draftEndpoints_then_401() {
        assertThat(getWithToken(SKILL_DRAFTS_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken(SKILL_DRAFT_DETAIL_ENDPOINT, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(postWithToken(SKILL_DRAFT_REJECT_ENDPOINT, SKILL_DRAFT_REJECT_BODY, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("晋升下游 SKL_016（撞名拒晋升）→ 北向 HTTP 409 + 信封 code=7016 + message 原文（不映射）")
    void given_skl016FromDownstream_when_promoteDraft_then_errorEnvelopePassedThrough() throws Exception {
        when(aiplatformClient.promoteSkillDraft(eq(DRAFT_ID))).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":7016,\"message\":\"技能库已有同名技能，晋升被拒（审核期间同名入库——先处置同名再晋升）\","
                                + "\"data\":null}")));

        String token = login(usernameWithSkillDraftWrite);
        ResponseEntity<String> response = postWithToken(SKILL_DRAFT_PROMOTE_ENDPOINT, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7016（SKL_016＝域码 7×1000＋16），而非映射后的 409——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7016);
        assertThat(root.path("message").asText())
                .isEqualTo("技能库已有同名技能，晋升被拒（审核期间同名入库——先处置同名再晋升）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("拒绝下游 SKL_018（理由空白，可选绑定不前置拦截）→ 北向 HTTP 400 + 信封 code=7018 + message 原文（不映射）")
    void given_skl018FromDownstream_when_rejectDraftWithoutReason_then_errorEnvelopePassedThrough() throws Exception {
        // reason 空白/缺失由 provider 手工校验出 400 SKL_018 原样透传（admin 不在绑定层前置拦截）
        when(aiplatformClient.rejectSkillDraft(eq(DRAFT_ID), any(AiplatformSkillDraftRejectWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":7018,\"message\":\"拒绝理由不能为空（终态留档，拒绝须有据）\",\"data\":null}")));

        String token = login(usernameWithSkillDraftWrite);
        ResponseEntity<String> response = postWithToken(SKILL_DRAFT_REJECT_ENDPOINT,
                "{\"reason\":\"  \"}", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 7018（SKL_018＝域码 7×1000＋18），而非映射后的 400——前端比对业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(7018);
        assertThat(root.path("message").asText()).isEqualTo("拒绝理由不能为空（终态留档，拒绝须有据）");
        assertThat(root.path("data").isNull()).isTrue();
    }

    // ========== 错误信封透传（忠实透传，不做映射） ==========

    @Test
    @DisplayName("下游 IDN_004 → 北向 HTTP 404 + 信封 code=6004 + message 原文（不映射为 BaseCodeMessage）")
    void given_idn004FromDownstream_when_getProfile_then_errorEnvelopePassedThrough() throws Exception {
        // 错误信封翻译已归 AiplatformClient（wire 契约测试钉死）；此处经真实 from() 翻译路径构造
        // 透传异常，验证北向 advice 在真实 MVC 层还原 provider 形状
        when(aiplatformClient.getAccountProfile(eq("auth0|unknown"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":6004,\"message\":\"账号不存在\",\"data\":null}")));

        String token = login(usernameWithReadPermission);
        ResponseEntity<String> response = getWithToken(
                "/api/admin/aiplatform/accounts/auth0|unknown", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = objectMapper.readTree(response.getBody());
        // 信封 code＝数字业务码 6004（IDN_004），而非映射后的 404——前端比对 aiplatform 业务码的分支活
        assertThat(root.path("code").asInt()).isEqualTo(6004);
        assertThat(root.path("message").asText()).isEqualTo("账号不存在");
        assertThat(root.path("data").isNull()).isTrue();
    }

    private String login(String username) {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"rememberMe\":false}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String token = root.path("data").path("token").asText();
            assertThat(token).as("登录应返回 token：%s", response.getBody()).isNotBlank();
            return token;
        } catch (Exception e) {
            throw new AssertionError("解析登录响应失败：" + response.getBody(), e);
        }
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(jsonHeadersWithToken(token)), String.class);
    }

    /** 无请求体 POST（沙箱四干预动作、单价表停用同形——provider 端只读路径参数，body 为空）。 */
    private ResponseEntity<String> postWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(null, jsonHeadersWithToken(token)), String.class);
    }

    /** JSON 请求体 POST（单价表改价命令体同形——逐字镜像 provider 契约 {unitPrice, currency, effectiveFrom}）。 */
    private ResponseEntity<String> postWithToken(String path, String body, String token) {
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeadersWithToken(token)), String.class);
    }

    /** 无请求体 DELETE（素材删除同形——provider 端只读路径参数，body 为空）。 */
    private ResponseEntity<String> deleteWithToken(String path, String token) {
        return restTemplate.exchange(url(path), HttpMethod.DELETE,
                new HttpEntity<>(null, jsonHeadersWithToken(token)), String.class);
    }

    /** JSON 请求体 PUT（槽位指派整包替换同形——PUT 全量语义，请求体 {skillIds} 即终态）。 */
    private ResponseEntity<String> putWithToken(String path, String body, String token) {
        return restTemplate.exchange(url(path), HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeadersWithToken(token)), String.class);
    }

    /** JSON 请求头 + Sa-Token 头（前缀按框架配置拼装；token null 则只带内容类型）。 */
    private HttpHeaders jsonHeadersWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            SaTokenConfig cfg = SaManager.getConfig();
            String value = (cfg.getTokenPrefix() == null || cfg.getTokenPrefix().isEmpty())
                    ? token
                    : cfg.getTokenPrefix() + " " + token;
            headers.set(cfg.getTokenName(), value);
        }
        return headers;
    }

    /** 二进制端点取回（byte[] body——不把 gzip 字节流经 String 解码，透传断言的前提）。 */
    private ResponseEntity<byte[]> getBinaryWithToken(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            SaTokenConfig cfg = SaManager.getConfig();
            String value = (cfg.getTokenPrefix() == null || cfg.getTokenPrefix().isEmpty())
                    ? token
                    : cfg.getTokenPrefix() + " " + token;
            headers.set(cfg.getTokenName(), value);
        }
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String uuidSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
