-- ============================================================================
-- V22: Seed menus —— AI 平台合同管理叶（issue #104 / spec #102 A 批，
--      provider 源裁决 aiplatform #336 合同域六态承接）
-- ============================================================================
-- Purpose: 在「AI 平台」目录 (id=82) 下种入合同管理叶子菜单 (id=260, sort=11)，
--          承载合同六态人工承接工作台（清单四维检索/详情承接全量面/创建重建——
--          合同域第 1–3 端点 BFF 镜像；六态推进写口 advance/fail/withdraw/void/
--          sign/record 归 A2 issue #105 逐票落地）的侧边栏导航；详情与推进动作
--          走页内抽屉/弹窗（CONTEXT.md「详情页＝弹窗优先」）。
-- Context: Admin (sys_)
-- ============================================================================
-- Background (spec #102 / #104, CONTEXT.md「AI 平台菜单」):
--   aiplatform 上线改版把签署定为后台人工承接（合同六态人工推进），admin BFF
--   镜像 provider #336 BackofficeContractController；付款核验（嵌订单详情）与
--   生产承接（嵌项目详情）不种独立页面，合同管理是 A 批唯一新叶。
--
--   ID 分配沿用保留段（V15 已用 directory 82、leaves 160-210；V17/V18/V19 用
--   220/230/240；V20 用 250）：id=260 合同管理 (menu, sort=11)。
--
--   icon=carbon:document-signed——已对照 admin-web 锁定依赖 @iconify/json@2.2.472
--   的 carbon 离线集核实存在（2026-11-02 node require 核实；V16 教训：勿以 iconify
--   线上 API 为准）。签署盖章语义贴本页核心工作面（六态推进至签署完成）。
--
--   A 批权限码 +17 全量（spec #102 定稿，本迁移一次记账——后续 A 批票据不再有
--   迁移）：
--     合同 8 码   contract:read/create/advance/fail/withdraw/void/sign/record
--     付款 4 码   payment:read/update-plan/verify/reject（凭证直出复用 read）
--     承接 4 码   production:read/update/advance/supplement
--     订单 1 码   order:scope
--   权限码挂 @RequirePermission（运行期 RBAC，PermissionScanner 注解扫描注册），
--   无 role 种子行（V20 先例：SUPER_ADMIN 经框架 bypass 见全树/全放行，非超管经
--   角色分配逐码授权）；码随各票端点落地逐枚生效——本票 #104 落 contract:read
--   （清单/详情复用）/contract:create（创建重建），33→35；A2 #105 落推进六写口
--   六码，35→41；A3/A4/A5 落余码至 50。全部 17 码的端点归属见 spec #102
--   Implementation Decisions。
--
--   幂等策略：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，#79 定稿）。
--   不写种子快照测试（V12 教训）；种子可见性（消费面行为）由
--   AiplatformMenuSeedVisibilityIntegrationTest 以 HTTP 外部行为钉住（十一叶口径
--   随本票更新）；routePath 推导正典由 MenuSeedRoutePathCanonIntegrationTest 钉住。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) 合同管理 menu（id=260）—— sort=11：合同六态人工承接工作台（清单四维检索
--    /详情三段全量面/创建重建；「需更新」合同的重录处置与六态推进走详情抽屉）
-- ----------------------------------------------------------------------------
INSERT INTO sys_admin_menus (
    id, menu_name, route_name, route_path, component,
    icon, icon_type, i18n_key,
    parent_id, sort_order, menu_type, status, keep_alive,
    created_at, updated_at
) VALUES (
    260,
    '合同管理',
    'aiplatform_contract',
    '/aiplatform/contract',
    'view.aiplatform_contract',
    'carbon:document-signed',
    1,  -- iconify
    'route.aiplatform_contract',
    82,
    11,
    2,  -- menu
    1,  -- active
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
