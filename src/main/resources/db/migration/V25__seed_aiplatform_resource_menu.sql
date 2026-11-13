-- ============================================================================
-- V25: Seed menus —— AI 平台「资源面」叶（issue #128 / aiplatform #489
--      资源回收通道的观测与手动处置面——resources/overview 大盘＋sweep 手动清扫）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入资源面叶子菜单 (id=300, sort=15)，
--          承载资源大盘（主机盘/docker 四类用量/无主计数/在册失联计数/最近清扫）
--          与手动清扫触发＋逐项处置报告的侧边栏导航；大盘判读与清扫触发走页内
--          区块/弹窗（CONTEXT.md「详情页＝弹窗优先」），不种二级路由。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (issue #128, GLOSSARY「资源面」词条):
--   aiplatform #489（#473/ADR-0045）落地后台资源面两端点（V64 迁移
--   wsp_host_observation＋wsp_sweep_actions），admin BFF 镜像后前端需要承载页。
--   词表定名「资源面」——勿用资源回收站/资源中心（#128 票面钉死）。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17/V18/V19 用
--   220/230/240；V20 用 250；V22 用 260；V23 用 270/280；V24 用 290）：id=300
--   资源面 (menu, sort=15)。
--
--   route_path 取 elegant-router 正典推导 '/' + routeName.replaceAll('_','/')
--   = /aiplatform/resource（V21 教训：手写扁平 kebab 会令深链 403，
--   MenuSeedRoutePathCanonIntegrationTest 全链推导校验）。
--
--   icon=carbon:data-center——已对照 admin-web 实际依赖 @iconify/json 离线集（node
--   require 核实 2026-11-13）carbon 集存在（data-center=true；候选 resource/
--   wastebasket/infrastructure 不存在勿用）。「主机盘＋docker 用量」语义直配；
--   先例预警同遵：勿以 iconify 线上 API 为准（V16 教训）。
--
--   权限码两枚（62→64，照沙箱先例——观测读码/动刀写码分权，ADR-0045 动刀类走
--   admin 手动＋留痕）：admin:aiplatform:resource:read（大盘读口）/
--   resource:sweep（手动清扫写口）——观测权与动刀权独立授受，如只给值班员配大盘
--   查看而不给清扫。权限码挂 @RequirePermission（运行期 RBAC），无 role 种子行。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十五叶
--   口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 资源面 menu（id=300）—— sort=15：资源大盘（主机盘面 host.path/total/usable/
--    used＋docker 四类用量 image/container/volume/buildCache 计数与体量＋无主计数
--    orphans＋在册失联计数 lostWorkspaceCount〔只标记待裁决，出口＝一键回收〕＋
--    sampledAt 新鲜度〔null＝缺测≠0＝真零，未采样呈现「采样中」〕＋最近手动清扫
--    lastSweep 收编结果面）＋手动清扫（sweep 逐项处置报告：SWEPT/SKIPPED〔REGISTERED
--    在册/GRACE_WINDOW 宽限窗/TIMESTAMP_UNREADABLE〕/FAILED；registryReachable=false
--    一刀未动如实呈报；操作者透传头落痕缺头落空）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    300,
    '资源面',
    'aiplatform_resource',
    '/aiplatform/resource',
    'view.aiplatform_resource',
    'carbon:data-center',
    1,  -- iconify
    'route.aiplatform_resource',
    82,
    15,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
