package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 模型供应商通道创建命令体的 wire 镜像——与 aiplatform
 * {@code ModelProviderCreateCommand}（#367 通道落库）字段同构：键名（＝协议路由前缀）
 * 随体、baseUrl 必填、apiKey 写收全量值（null/空白＝暂不配置）、enabled 缺省 true。
 *
 * <p>字段合法性（键名已存在 409 PRJ_067、baseUrl 必填 400 PRJ_069、操作者缺头 400
 * PRJ_034）由 provider 聚合守卫裁决，BFF 不代判不代填——同
 * {@link AiplatformAgentConfigUpdateWireRequest} 形制。{@code enabled} 为 Boolean
 * 装箱（与 provider 命令同型）——null 出站在场＝provider 缺省 true。</p>
 *
 * @param providerKey 键名＝协议路由前缀（volcengine/deepseek 等）
 * @param baseUrl     通道 baseUrl（必填）
 * @param apiKey      凭证全量值（null/空白＝暂不配置）
 * @param enabled     启用态（缺省 true）
 */
public record AiplatformModelProviderCreateWireRequest(
        String providerKey,
        String baseUrl,
        String apiKey,
        Boolean enabled
) {
}
