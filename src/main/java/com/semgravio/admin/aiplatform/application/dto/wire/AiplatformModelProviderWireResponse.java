package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 模型供应商通道 wire 响应——与 aiplatform
 * {@code BackofficeModelProviderResponse}（#367 通道维护）字段同构：GET 清单/单行、
 * POST 创建、PUT 更新回执同形。
 *
 * <p><strong>apiKey 一律掩码回显</strong>（{@code apiKeyMasked} 尾 4 位——provider
 * {@code ModelProvider#maskApiKey} 单点；null＝未配置）：接口回显面零明文 key。admin
 * 忠实透传掩码值——不二次处理、不尝试解掩（spec #101）。{@code enabled} 为原生
 * boolean（provider 恒在场）。</p>
 *
 * @param providerKey  键名＝协议路由前缀
 * @param baseUrl      通道 baseUrl
 * @param apiKeyMasked apiKey 掩码形（尾 4 位；null＝未配置）
 * @param enabled      启用态
 * @param operatorId   最近写者 id（null＝种子/迁移写入）
 * @param operatorName 最近写者名（直读展示）
 * @param updatedAt    最近写入时刻
 */
public record AiplatformModelProviderWireResponse(
        String providerKey,
        String baseUrl,
        String apiKeyMasked,
        boolean enabled,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
