-- ============================================================================
-- V24: Seed menus —— AI 平台「数据标注」叶（issue #124 / aiplatform #464
--      数据飞轮采集层人审口）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入数据标注叶子菜单 (id=290, sort=14)，
--          承载沉淀草稿标注队列＋激活/拒绝/改标签三键（aiplatform #464 后台
--          「数据标注」面五端点 BFF 镜像）的侧边栏导航；详情判读与三键操作走
--          页内抽屉/弹窗（CONTEXT.md「详情页＝弹窗优先」），不种路由。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (issue #124, GLOSSARY「数据标注」词条):
--   aiplatform #464（数据飞轮一期第二层·审核侧）落地后台「数据标注」面 5 端点，
--   admin BFF 镜像后前端需要承载页。词表定名「数据标注」——勿用审核中心/标注
--   平台（#124 票面钉死）。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17/V18/V19 用
--   220/230/240；V20 用 250；V22 用 260；V23 用 270/280）：id=290 数据标注
--   (menu, sort=14)。
--
--   route_path 取 elegant-router 正典推导 '/' + routeName.replaceAll('_','/')
--   = /aiplatform/sediment/draft（V21 教训：手写扁平 kebab 会令深链 403，
--   MenuSeedRoutePathCanonIntegrationTest 全链推导校验）。
--
--   icon=carbon:tag——已对照 admin-web 实际依赖 @iconify/json 离线集（node
--   require 核实 2026-11-12）carbon 集存在（tag=true；候选 text-annotation/
--   labels 不存在勿用）。「标注＝打标签」语义直配；先例预警同遵：勿以
--   iconify 线上 API 为准（V16 教训）。
--
--   权限码四枚（58→62，照 skill-draft 三码先例——连字符子域、每写操作一码）：
--   admin:aiplatform:sediment-draft:read（队列/详情两读端点复用）/ :activate /
--   :reject / :relabel（三键各一码——激活权/拒绝权/分诊权独立授受，读写不串、
--   三写码互不串）。权限码挂 @RequirePermission（运行期 RBAC），无 role 种子行。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十四叶
--   口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 数据标注 menu（id=290）—— sort=14：沉淀草稿人审队列（采集层自荐草稿按
--    项目/类别/状态过滤＋自荐时间倒序＋分页；详情判读＝正文全文＋payload＋
--    scanFindings 逐条＋终态留痕；激活＝原样升格不改稿（已激活≠已入库——升格
--    归激活链）；拒绝＝理由必填终态留档；改标签＝在途分诊修正占目标类别名额）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    290,
    '数据标注',
    'aiplatform_sediment_draft',
    '/aiplatform/sediment/draft',
    'view.aiplatform_sediment_draft',
    'carbon:tag',
    1,  -- iconify
    'route.aiplatform_sediment_draft',
    82,
    14,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
