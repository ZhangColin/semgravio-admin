-- ============================================================================
-- V26: Seed menus —— AI 平台「时段规则」叶（issue #130 / aiplatform #497
--      时段规则表与机机写口，ADR-0044 峰谷 1/4）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入时段规则叶子菜单 (id=310, sort=16)，
--          承载时段规则行清单（含历史版本分页）＋开行（一单 N 窗口＝一个版本）＋
--          原子改窗（关当前版本全部行＋开新版本，回执 closed/opened）＋停用
--          （关版本不接新行）四端点 BFF 镜像（aiplatform #497 后台「时段规则」面，
--          峰谷运营位）的侧边栏导航；窗口录入与三键操作走页内弹窗（详情页＝弹窗优先），
--          不种路由。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (issue #130, GLOSSARY「时段规则」词条):
--   aiplatform #497（ADR-0044 峰谷 1/4）落地后台「时段规则」面 4 端点——时段规则＝
--   供应商峰谷定价的时段定义（通道 × 生效区间 × 星期口径 × UTC 窗口行，一行一窗口），
--   供应商侧可运营数据——运营者经签名通道种入并维护 DeepSeek 峰窗规则，无需改代码发版。
--   admin BFF 镜像后前端需要承载页（种子口径：admin 侧只需维护表单）。词表定名
--   「时段规则」——勿用峰谷配置/计价时段。
--
--   ID 分配沿用保留段（V15 已用：directory 82；leaves 160-210；V17/V18/V19 用
--   220/230/240；V20 用 250；V22 用 260；V23 用 270/280；V24 用 290；V25 用 300）：
--   id=310 时段规则 (menu, sort=16)。
--
--   route_path 取 elegant-router 正典推导 '/' + routeName.replaceAll('_','/')
--   = /aiplatform/time/band/rule（V21 教训：手写扁平 kebab 会令深链 403，
--   MenuSeedRoutePathCanonIntegrationTest 全链推导校验）。
--
--   icon=carbon:time——已对照 admin-web 实际依赖 @iconify/json 离线集（node
--   require 核实 2026-11-13）carbon 集存在（time=true；候选 hourglass/timer 存在
--   但 time 语义直配「时段」）。先例预警同遵：勿以 iconify 线上 API 为准（V16 教训）。
--
--   权限码四枚（66→70〔随 #128 口径〕——连字符子域、每写操作一码，照单价表域与
--   资源面先例）：admin:aiplatform:time-band-rule:read（清单读口）/ :open /
--   :rewindow / :deactivate（三写口各一码——开行权/调窗权/停用权独立授受，
--   读写分权）。权限码挂 @RequirePermission（运行期 RBAC），无 role 种子行。
--
--   SUPER_ADMIN 经框架 bypass 见全树，无需 role-menu 种子行。
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十六叶
--   口径随本票更新）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 时段规则 menu（id=310）—— sort=16：供应商峰谷时段定义工作面（规则史全貌
--    清单〔现行与历史行、生效起点倒序、effectiveTo null＝当前行〕＋开行〔一单 N
--    窗口＝一个版本，DeepSeek 峰窗两窗口一单种入语义〕＋原子改窗〔关当前版本全部行
--    ＋开新版本，回执 closed/opened 双段，被关行保留原操作者〕＋停用〔关版本不接新行，
--    预发布钳成空区间〕；窗不跨午夜 v1 不支持——表单侧提示后拆两行提交，服务端拦
--    start≥end 出 METER_013；band 1=峰 2=谷、dayMask Mon=1…Sun=64；操作者透传头
--    落痕缺头落空）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    310,
    '时段规则',
    'aiplatform_time_band_rule',
    '/aiplatform/time/band/rule',
    'view.aiplatform_time_band_rule',
    'carbon:time',
    1,  -- iconify
    'route.aiplatform_time_band_rule',
    82,
    16,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
