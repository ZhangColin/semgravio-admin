-- ============================================================================
-- V17: Seed menus —— AI 平台技能线三叶之首：技能管理（issue #80 /
--      aiplatform 技能线 T1 #247）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入技能管理叶子菜单 (id=220, sort=7)，
--          承载技能线 T1 读面（技能清单/详情审核）的侧边栏导航。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (aiplatform 技能线 spec #78/#79, CONTEXT.md「AI 平台菜单」):
--   aiplatform 技能线（ADR-0021/#246）新增后台第七域：技能管理。三叶随票逐叶种——
--   T1 票带 skill 叶（本迁移）、T3 票带 assignment 叶 (230, sort=8)、T5 票带
--   agent-config 叶 (240, sort=9)；工具面并入智能体配置页区块不单独种页；技能
--   详情审核面走抽屉不种菜单（CONTEXT.md「详情页 = 弹窗/抽屉优先」）。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210）：id=220 技能管理
--   (menu, sort=7)。
--
--   icon=carbon:tools——已对照 admin-web 实际依赖 @iconify/json@2.2.472 的 carbon
--   集核实存在（V16 教训：carbon:knowledge-base 不存在致菜单图标裂图；勿以
--   iconify 线上 API 为准——线上集可领先于前端锁定的离线集）。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（七叶口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 技能管理 menu（id=220）—— sort=7：技能清单（内置＋安装同权）/详情审核 +
--    安装向导/启停/卸载/更新（随 T2/T4 票补写口）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    220,
    '技能管理',
    'aiplatform_skill',
    '/aiplatform/skill',
    'view.aiplatform_skill',
    'carbon:tools',
    1,  -- iconify
    'route.aiplatform_skill',
    82,
    7,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
