package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 模型供应商通道更新命令体的 wire 镜像——与 aiplatform
 * {@code ModelProviderUpdateCommand}（#367 通道更新）字段同构：baseUrl 终态全量
 * 替换；apiKey 与 enabled <strong>缺省＝保持现值</strong>（两肢皆「不可/不宜因缺省
 * 翻转」的终态字段——apiKey 读面只出掩码、缺省清空＝读一次即丢 key；enabled 是有意
 * 状态、缺省归一 true 会把停用通道在换 key 时意外复启）。
 *
 * <p><strong>缺省肢语义在 wire 请求体上保真</strong>（spec #101）：{@code apiKey} 与
 * {@code enabled} 为可空肢（String/Boolean 装箱）——null 出站在场＝保持现值，不臆造
 * 默认值（若绑成原生 boolean，null 会被落成 false＝翻转停用，语义反了）。apiKey
 * 特例：携现值掩码形（如 {@code ****abcd}）同判保持＝掩码回显的 round-trip 安全；
 * 换 key 才写全量新值。判定全在 provider 侧，BFF 零推导。</p>
 *
 * @param baseUrl 通道 baseUrl（终态全量替换，必填）
 * @param apiKey  全量新值＝换 key；null/空白/现值掩码形＝保持现值
 * @param enabled 缺省＝保持现值；显式 true/false 才翻转
 */
public record AiplatformModelProviderUpdateWireRequest(
        String baseUrl,
        String apiKey,
        Boolean enabled
) {
}
