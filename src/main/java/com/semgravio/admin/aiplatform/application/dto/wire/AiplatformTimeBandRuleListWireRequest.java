package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 时段规则清单过滤的 wire 镜像——与 aiplatform {@code BackofficeTimeBandRuleQuery}
 * （#497）字段同构：provider 为匹配键＝精确等值过滤、可缺省（缺省＝全量行）。
 *
 * @param provider 模型提供方（匹配键；null＝全量行）
 */
public record AiplatformTimeBandRuleListWireRequest(
        String provider
) {
}
