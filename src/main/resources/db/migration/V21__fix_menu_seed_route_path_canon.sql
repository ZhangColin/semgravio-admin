-- ============================================================================
-- V21: Fix menus —— routePath 对齐 elegant-router 正典推导（5 条扁平 kebab →
--      下划线分段）
--       (issue #94 / admin-web #70)
-- ============================================================================
-- Purpose: 将 5 条多段路由名种子的 route_path 从手写扁平 kebab 修正为
--          elegant-router 正典推导值（'/' + routeName.replaceAll('_', '/')）。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (issue #94 / admin-web #70 三层根因):
--   admin-web 动态菜单模式下路由按后端 /menus/my 下发的 routePath 注册
--   （transformBackendMenuToMenuRoute 直拷），而前端静态正典表
--   （src/router/elegant/transform.ts）按 @elegant-router/core 的
--   transformRouterNameToPath = '/' + name.replaceAll('_', '/') 从路由名推导
--   path（每个下划线一段）。种子手写扁平 kebab 与正典不一致 → 正典路径深链 /
--   F5 / 书签落 not-found catch-all → 守卫查路由名存在 → 导 403 而非 404
--   （侧边栏按路由 name 跳转照常渲染，日常无感）。
--
--   五条修正（issue #94 表列四条 + V20 草稿审核叶一条——票面审计 09-28 早于
--   V20 合入 09-29，同一推导规则下必漂移：前端按同级 snake_case 目录惯例建
--   views/aiplatform/skill_draft 页时正典即 /aiplatform/skill/draft）：
--     id=130 payment_channel_log         /payment/channel-log         → /payment/channel/log
--     id=200 aiplatform_price_entry      /aiplatform/price-entry      → /aiplatform/price/entry
--     id=230 aiplatform_skill_assignment /aiplatform/skill-assignment → /aiplatform/skill/assignment
--     id=240 aiplatform_agent_config     /aiplatform/agent-config     → /aiplatform/agent/config
--     id=250 aiplatform_skill_draft      /aiplatform/skill-draft      → /aiplatform/skill/draft
--
--   防复发（fail fast——admin-web #70 拍板，前端不加自愈）：
--   MenuSeedRoutePathCanonIntegrationTest 在独立 schema 全量跑 Flyway 链直查
--   种子终态，断言框架页 routePath == 推导值（href 外链行豁免——issue #94 明示
--   逃生门），未来种子漂移即红。
--
--   不修改 V13/V15/V18/V19/V20：现有环境已执行，Flyway 校验和锁定已应用版本，
--   改动触发 ValidationFailure（V12 先例）；以新增迁移保持历史线性。
--
--   幂等策略：UPDATE 重复执行值不变，天然幂等。
-- ============================================================================

-- id=130 通道交互日志（V13 种）
UPDATE sys_admin_menus
SET route_path = '/payment/channel/log', updated_at = CURRENT_TIMESTAMP
WHERE id = 130;

-- id=200 单价表（V15 种）
UPDATE sys_admin_menus
SET route_path = '/aiplatform/price/entry', updated_at = CURRENT_TIMESTAMP
WHERE id = 200;

-- id=230 指派配置（V18 种）
UPDATE sys_admin_menus
SET route_path = '/aiplatform/skill/assignment', updated_at = CURRENT_TIMESTAMP
WHERE id = 230;

-- id=240 智能体配置（V19 种）
UPDATE sys_admin_menus
SET route_path = '/aiplatform/agent/config', updated_at = CURRENT_TIMESTAMP
WHERE id = 240;

-- id=250 草稿审核（V20 种）
UPDATE sys_admin_menus
SET route_path = '/aiplatform/skill/draft', updated_at = CURRENT_TIMESTAMP
WHERE id = 250;
