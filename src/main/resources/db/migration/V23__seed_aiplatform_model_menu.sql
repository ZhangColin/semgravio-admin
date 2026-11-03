-- ============================================================================
-- V23: Seed menus —— AI 平台模型供应商/模型档位两叶（issue #110 / spec #101 B 批，
--      provider 源裁决 aiplatform #367/#368 通道化，决策正本 ADR-0031）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入模型供应商 (id=270, sort=12) 与模型档位
--          (id=280, sort=13) 两叶子菜单——B 批唯一菜单迁移：模型供应商页承载通道
--          CRUD/远端模型清单手动拉取（对照库内档位）/变更留痕（B1 issue #110 七端点，
--          含 apiKey 掩码尾 4 位读面）；模型档位页承载语义键档位 CRUD/留痕（B2
--          issue #111 六端点，随票落码）。两新叶与智能体配置（id=240）同目录并排
--          （agent-configs 的 modelId 即档位键引用——配置面与档位表口径一致）。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (spec #101 / #103, CONTEXT.md「AI 平台菜单」):
--   aiplatform 模型供应商接入通道化（spec #364 收口，ADR-0031：切火山方舟）已全部
--   实现（V44/V45）：模型供应商（通道）/模型档位（语义键）两层维护面、每供应商
--   远端模型清单手动拉取。admin BFF 忠实镜像 provider 新后台端点——通道/档位分权，
--   两新叶独立种页（区别于付款/承接嵌详情无页面）。
--
--   ID 分配沿用保留段（V15 已用 directory 82、leaves 160-210；V17/V18/V19 用
--   220/230/240；V20 用 250；V22 用 260）：id=270 模型供应商 (menu, sort=12)、
--   id=280 模型档位 (menu, sort=13)。
--
--   routePath 按 elegant-router 正典推导（V21 教训：'/' + routeName.replaceAll('_','/')）
--   ——aiplatform_model_provider → /aiplatform/model/provider、aiplatform_model_tier
--   → /aiplatform/model/tier；MenuSeedRoutePathCanonIntegrationTest 钉死。
--
--   icon=carbon:gateway（通道语义）/carbon:layers（档位分层语义）——已对照 admin-web
--   锁定依赖 @iconify/json@2.2.472 的 carbon 离线集核实存在（2026-11-03 node require
--   核实；V16 教训：勿以 iconify 线上 API 为准）。
--
--   B 批权限码 +8 全量（spec #101 定稿，本迁移一次记账——B 批后续票据不再有迁移）：
--     通道 4 码   model-provider:read/create/update/delete（remote-models/traces
--                复用 read——只读探测不动库/留痕读面；勾选入库复用档位创建写口，
--                权限在彼处自然拦截）
--     档位 4 码   model-tier:read/create/update/delete（traces 复用 read）
--   权限码挂 @RequirePermission（运行期 RBAC，PermissionScanner 注解扫描注册），
--   无 role 种子行（V20/V22 先例：SUPER_ADMIN 经框架 bypass 见全树/全放行，非超管经
--   角色分配逐码授权）；码随各票端点落地逐枚生效——本票 #110 落通道四码，50→54；
--   B2 #111 落档位四码，54→58。全部 8 码的端点归属见 spec #101 Implementation
--   Decisions。
--
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十三叶口径
--   随本票更新）；routePath 推导正典由 MenuSeedRoutePathCanonIntegrationTest 钉住。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 模型供应商 menu（id=270）—— sort=12：通道 CRUD（apiKey 掩码读面）＋远端模型
--    清单手动拉取（对照库内档位）＋变更留痕时间线；「勾选入库」复用档位创建写口
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    270,
    '模型供应商',
    'aiplatform_model_provider',
    '/aiplatform/model/provider',
    'view.aiplatform_model_provider',
    'carbon:gateway',
    1,  -- iconify
    'route.aiplatform_model_provider',
    82,
    12,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2) 模型档位 menu（id=280）—— sort=13：语义键档位 CRUD（运营命名与模型版本解耦）
--    ＋变更留痕；agent-configs 的 modelId 即档位键引用（enabled=true 键集＝绑定
--    下拉数据源与校验放行值域——B2 issue #111 落端点）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    280,
    '模型档位',
    'aiplatform_model_tier',
    '/aiplatform/model/tier',
    'view.aiplatform_model_tier',
    'carbon:layers',
    1,  -- iconify
    'route.aiplatform_model_tier',
    82,
    13,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
