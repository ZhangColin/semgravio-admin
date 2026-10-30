-- ============================================================================
-- V19: Seed menus —— AI 平台技能线三叶之末：智能体配置（issue #84 /
--      aiplatform 技能线 T5 #251）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入智能体配置叶子菜单 (id=240, sort=9)，
--          承载技能线 T5 两座智能体（main=主智能体／executor=run 执行体）运营配置
--          （systemPrompt/模型档位全量写＋变更留痕时间线＋留痕写回回滚）的侧边栏
--          导航；工具面（槽位工具清单＋增强工具开关）并入本页区块、不单独种页
--          （spec #79 定稿）。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (aiplatform 技能线 spec #78/#79, CONTEXT.md「AI 平台菜单」):
--   aiplatform 技能线（ADR-0021/#246）三叶随票逐叶种——T1 已种 skill 叶 (220,
--   sort=7, V17)、T3 已种 assignment 叶 (230, sort=8, V18)、本迁移带 agent-config
--   叶 (240, sort=9)，技能线三叶齐。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17/V18 用
--   220/230）：id=240 智能体配置 (menu, sort=9)。
--
--   icon=carbon:prompt-session——已对照 admin-web 实际依赖 @iconify/json@2.2.472
--   的 carbon 集核实存在（V16 教训：勿以 iconify 线上 API 为准——线上集可领先于
--   前端锁定的离线集）。prompt 会话语义贴本页核心对象（systemPrompt 全文维护＋
--   模型档位），与 skill 叶 carbon:tools、assignment 叶 carbon:list-checked 区分
--   明显。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（九叶口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 智能体配置 menu（id=240）—— sort=9：两座智能体（main/executor）运营配置
--    读写＋变更留痕读面（append-only 时间线）＋留痕写回回滚；工具面并入本页区块
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    240,
    '智能体配置',
    'aiplatform_agent_config',
    '/aiplatform/agent-config',
    'view.aiplatform_agent_config',
    'carbon:prompt-session',
    1,  -- iconify
    'route.aiplatform_agent_config',
    82,
    9,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
