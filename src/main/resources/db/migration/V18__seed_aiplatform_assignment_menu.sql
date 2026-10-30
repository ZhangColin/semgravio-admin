-- ============================================================================
-- V18: Seed menus —— AI 平台技能线三叶之二：指派配置（issue #82 /
--      aiplatform 技能线 T3 #249）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入指派配置叶子菜单 (id=230, sort=8)，
--          承载技能线 T3 槽位指派读写（三职能槽位各自勾选技能、整包替换保存）的
--          侧边栏导航。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (aiplatform 技能线 spec #78/#79, CONTEXT.md「AI 平台菜单」):
--   aiplatform 技能线（ADR-0021/#246）三叶随票逐叶种——T1 已种 skill 叶 (220,
--   sort=7, V17)、本迁移带 assignment 叶 (230, sort=8)、T5 票带 agent-config 叶
--   (240, sort=9)；工具面并入智能体配置页区块不单独种页。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17 用 220）：
--   id=230 指派配置 (menu, sort=8)。
--
--   icon=carbon:list-checked——已对照 admin-web 实际依赖 @iconify/json@2.2.472 的
--   carbon 集核实存在（V16 教训：勿以 iconify 线上 API 为准——线上集可领先于前端
--   锁定的离线集；carbon:task-assignments 即不存在于锁定集）。勾选清单语义贴指派
--   交互（三槽位各自勾选技能、清单即终态），与 skill 叶 carbon:tools 区分明显。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（八叶口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 指派配置 menu（id=230）—— sort=8：三职能槽位（main/executor/subagent）指派
--    读写——读面含停用行实态、PUT 整包替换（清单即终态、空清单＝清空）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    230,
    '指派配置',
    'aiplatform_skill_assignment',
    '/aiplatform/skill-assignment',
    'view.aiplatform_skill_assignment',
    'carbon:list-checked',
    1,  -- iconify
    'route.aiplatform_skill_assignment',
    82,
    8,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
