package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台单价行——北向出口，逐字镜像 aiplatform {@code UnitPriceEntryResponse}
 * （#160，清单行/改价回执/停用回执共形）：匹配键＋单价＋生效区间＋操作者。与订单价目行
 * （{@link AiplatformPriceEntryResponse}，订单报价史）是两个域概念——本行为平台成本换算用的
 * 单价数据（met_price_entries）。
 *
 * <p>{@code unitPrice} 为<strong>String 明文小数</strong>（BigDecimal 语义，provider 按
 * {@code stripTrailingZeros().toPlainString()} 精确十进制串交接——直出 JSON 数值会落科学计数，
 * 单价契约按十进制原串交接）；{@code tokenKind} 为 Integer code + {@code tokenKindName} 中文名
 * 随行（1=输入 2=输出 3=缓存读 4=缓存写 5=推理 6=按张——#288 图片生成按张计价档，档位单价
 * 语义：token 档每 token、按张档每张，同一字段承载）；{@code effectiveTo} 为 null 即当前行。
 * 操作者两肢为 admin 侧管理员标识（X-User-Id/X-User-Name 透传落痕）；存量行/种子脚本种入行
 * 如实出 JSON null。</p>
 *
 * @since 0.1.0
 */
public record AiplatformUnitPriceEntryResponse(

        /** 单价行标识（TSID 十进制字符串） */
        String id,

        /** 模型提供方 */
        String provider,

        /** 模型 */
        String model,

        /** 档位 code（1=输入 2=输出 3=缓存读 4=缓存写 5=推理 6=按张） */
        @Schema(description = "档位 code（1=输入 2=输出 3=缓存读 4=缓存写 5=推理 6=按张——6 为图片"
                + "生成按张计价档〔aiplatform #288〕，开行/改价/停用对六档同一套语义；"
                + "tokenKindName 中文名随行直读）", example = "6")
        Integer tokenKind,

        /** 档位名（直读展示；按张档＝「按张」） */
        String tokenKindName,

        /** 档位单价（token 档每 token、按张档每张；精确十进制串） */
        @Schema(description = "档位单价（token 档每 token、按张档每张；响应侧为 string——精确十进制串"
                + "交接，规避 BigDecimal 直出 JSON 落科学计数；改价入参侧为 number，类型照实"
                + "各自呈现、有意不做统一）",
                example = "\"0.18\"")
        String unitPrice,

        /** 币种（ISO 4217） */
        String currency,

        /** 生效起点（含；ISO-8601 Instant，UTC 带 Z） */
        Instant effectiveFrom,

        /** 生效终点（不含；null = 当前行） */
        Instant effectiveTo,

        /** 操作者标识（admin 侧管理员 TSID 十进制串；存量行/无头为 null） */
        String operatorId,

        /** 操作者昵称（直读展示；同上落空口径） */
        String operatorName
) {
}
