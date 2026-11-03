package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 范围录入命令体的 wire 镜像（issue #108）——与 aiplatform {@code EnterScopeCommand}
 * （#336 后台机机面）字段同构：三段整组替换（不收半组，每次全量提交）。
 *
 * <p>字段合法性（三段必填、每段至多 2000 字）归 provider 聚合守卫（ORD_029/ORD_030），
 * BFF 不代判；null 字段照常序列化在场（全局 Jackson 含 null）。限未支付态守卫
 * ORD_033 同归 provider。</p>
 *
 * @param scopeDeliverables 交付清单（必填，至多 2000 字——缺失/超长归 provider ORD_029/030）
 * @param scopeAcceptance   验收方式（必填，至多 2000 字——校验同上）
 * @param scopeTimeline     工期口径（必填，至多 2000 字——校验同上）
 */
public record AiplatformOrderScopeWireRequest(
        String scopeDeliverables,
        String scopeAcceptance,
        String scopeTimeline
) {
}
