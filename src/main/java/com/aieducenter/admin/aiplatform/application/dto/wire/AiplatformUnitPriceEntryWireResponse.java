package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.time.Instant;

/**
 * aiplatform 后台单价行的 wire 镜像——与 aiplatform {@code UnitPriceEntryResponse}
 * （#160 成本运营，清单行/改价回执/停用回执共形）字段同构：匹配键＋单价＋生效区间＋操作者。
 * 与订单价目行（{@code AiplatformPriceEntryWireResponse}，订单报价史）是两个域概念——本行为
 * 平台成本换算用的单价数据（met_price_entries）。
 *
 * <p>{@code unitPrice} 为<strong>精确十进制串</strong>（provider 侧 {@code stripTrailingZeros().toPlainString()}
 * ——BigDecimal 直出 JSON 会落科学计数，单价契约按十进制原串交接；改价入参侧为 number，两侧类型
 * 有意不对称照实各自呈现）；{@code effectiveTo} 为 null
 * 即当前行，历史行含区间两端。行操作者两列＝该行最近管理动作（开行或停用）的留痕；存量行/
 * 无头请求（含种子脚本种入行）为 null。</p>
 *
 * @param id             单价行标识（TSID 十进制字符串）
 * @param provider       模型提供方
 * @param model          模型
 * @param tokenKind      档位 code（1=输入 2=输出 3=缓存读 4=缓存写 5=推理 6=按张——#288 图片生成
 *                       按张计价档，开行/改价/关行对六档同一套语义）
 * @param tokenKindName  档位名（直读展示；按张档＝「按张」）
 * @param unitPrice      档位单价（token 档每 token、按张档每张；精确十进制串）
 * @param currency       币种（ISO 4217）
 * @param effectiveFrom  生效起点（含）
 * @param effectiveTo    生效终点（不含；null = 当前行）
 * @param operatorId     操作者标识（admin 侧管理员 TSID 十进制串；存量行/无头为 null）
 * @param operatorName   操作者昵称（直读展示；同上落空口径）
 */
public record AiplatformUnitPriceEntryWireResponse(
        String id,
        String provider,
        String model,
        Integer tokenKind,
        String tokenKindName,
        String unitPrice,
        String currency,
        Instant effectiveFrom,
        Instant effectiveTo,
        String operatorId,
        String operatorName
) {
}
