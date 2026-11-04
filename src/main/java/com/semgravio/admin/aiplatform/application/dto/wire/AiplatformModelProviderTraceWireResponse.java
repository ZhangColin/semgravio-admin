package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 模型供应商通道变更留痕 wire 响应行——与 aiplatform
 * {@code BackofficeModelProviderTraceResponse}（#367 通道留痕）字段同构：一次实际
 * 变更的前后全量值快照＋操作者＋时刻，按时间倒序（最近先）。append-only——留痕不随
 * 任何动作删除（创建痕 old 全 null、删除痕 new 全 null，跨行存活）。
 *
 * <p><strong>apiKey 快照读面一律掩码</strong>（{@code oldApiKeyMasked/newApiKeyMasked}
 * 尾 4 位——留痕不是密钥找回面；null＝该侧未配置或创建/删除痕）：provider 单点掩码，
 * admin 忠实透传不二次处理。{@code oldEnabled/newEnabled} 为 Boolean 装箱（创建痕/
 * 删除痕对应侧 null）。</p>
 *
 * @param id              留痕标识（TSID 十进制串）
 * @param oldBaseUrl      变更前 baseUrl（创建痕＝null）
 * @param oldApiKeyMasked 变更前 apiKey 掩码（创建痕/null 键＝null）
 * @param oldEnabled      变更前启用态（创建痕＝null）
 * @param newBaseUrl      变更后 baseUrl（删除痕＝null）
 * @param newApiKeyMasked 变更后 apiKey 掩码（删除痕/null 键＝null）
 * @param newEnabled      变更后启用态（删除痕＝null）
 * @param operatorId      变更操作者 id
 * @param operatorName    变更操作者名（直读展示）
 * @param operatedAt      变更动作时刻
 */
public record AiplatformModelProviderTraceWireResponse(
        String id,
        String oldBaseUrl,
        String oldApiKeyMasked,
        Boolean oldEnabled,
        String newBaseUrl,
        String newApiKeyMasked,
        Boolean newEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime operatedAt
) {
}
