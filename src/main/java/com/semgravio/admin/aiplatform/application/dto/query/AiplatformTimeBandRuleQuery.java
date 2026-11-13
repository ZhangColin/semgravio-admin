package com.semgravio.admin.aiplatform.application.dto.query;

/**
 * AI 平台时段规则清单北向查询——provider 精确等值过滤（可缺省＝全量行），分页 1-based
 * （北向原样直传 provider，spec #62 平台分页统一决议目标态）。
 *
 * @param provider 模型提供方（匹配键；null＝全量行）
 */
public record AiplatformTimeBandRuleQuery(
        String provider
) {
}
