-- ============================================================================
-- V20: Seed menus —— AI 平台草稿审核叶（issue #92 / aiplatform 自产线 #258，
--      spec #91 草稿审核面）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入草稿审核叶子菜单 (id=250, sort=10)，
--          承载自产技能草稿人审队列（在途清单/详情判读/晋升/拒绝——技能域第
--          17–20 端点 BFF 镜像）的侧边栏导航；详情判读与审核操作走页内抽屉/
--          弹窗（CONTEXT.md「详情页＝弹窗优先」），不种路由。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (spec #91 / #92, CONTEXT.md「AI 平台菜单」):
--   aiplatform 自产线（#258，ADR-0022 库制草稿）落地草稿审核面 4 端点，admin
--   BFF 镜像后前端需要承载页——技能线三叶（220/230/240，V17/V18/V19）之后
--   第四叶：草稿审核 (250, sort=10)。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17/V18/V19 用
--   220/230/240）：id=250 草稿审核 (menu, sort=10)。
--
--   icon=carbon:task-approved——已对照 admin-web 实际依赖 @iconify/json@2.2.472
--   的 carbon 离线集核实存在（2026-09-29 node require 核实；task-approved=true、
--   先例预警的 task-assignments 确不存在勿用；V16 教训：勿以 iconify 线上 API
--   为准）。任务已核准语义贴本页核心动作（人审晋升/拒绝）。
--
--   权限码三枚（30→33，spec #91 grill 定稿 2026-09-29）：admin:aiplatform:
--   skill-draft:read / :promote / :reject——连字符子域对齐 agent-tool 先例、
--   每写操作一码对齐技能线先例；两读端点复用 read、promote/reject 各一码
--   （读写不串、两写码互不串）。权限码挂 @RequirePermission（运行期 RBAC），
--   无 role 种子行。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十叶口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 草稿审核 menu（id=250）—— sort=10：自产技能草稿人审队列（在途清单跨项目
--    血统三件＋扫描判定＋自荐时间倒序；详情判读＝正文全文＋scanFindings 逐条
--    ＋终态留痕；晋升＝一键入池不自动指派；拒绝＝理由必填终态留档）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    250,
    '草稿审核',
    'aiplatform_skill_draft',
    '/aiplatform/skill-draft',
    'view.aiplatform_skill_draft',
    'carbon:task-approved',
    1,  -- iconify
    'route.aiplatform_skill_draft',
    82,
    10,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
