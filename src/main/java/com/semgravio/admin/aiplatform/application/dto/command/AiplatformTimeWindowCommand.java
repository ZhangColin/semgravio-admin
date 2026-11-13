package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台时段窗口北向命令项——运营人员录入的一条 UTC 时段窗口（＝规则表一行），逐字镜像
 * aiplatform {@code TimeWindowCommand}（#497 开行/改窗命令的清单元素）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性
 * （字段不完整/星期口径越界/跨午夜窗口 start≥end METER_013、同通道区间重叠 METER_008）
 * 由 provider 聚合守卫裁决，错误信封原样透传——BFF 不加戏不重复校验（忠实透传零加戏，
 * spec #62）。数值字段用包装型：缺字段→null→provider 守卫出干净 400，而非拆箱 NPE 出 500。
 * 跨午夜窗口 v1 不支持：表单侧建议提示后拆两行提交（22:00–02:00＝22:00–24:00＋
 * 00:00–02:00——星期口径×跨午夜语义有歧义，服务端拦 start≥end 出 METER_013）。</p>
 *
 * @param band        时段带 code（1=峰 2=谷）
 * @param dayMask     星期口径 bitmask（ISO 周序 Mon=1 Tue=2 Wed=4 Thu=8 Fri=16 Sat=32
 *                    Sun=64，合法域 1..127 非零——复选周一～周日编码）
 * @param startMinute UTC 窗口起点分钟（含），0..1439（时分控件录 UTC）
 * @param endMinute   UTC 窗口终点分钟（不含），1..1440；须大于起点
 */
public record AiplatformTimeWindowCommand(

        /** 时段带 code（1=峰 2=谷） */
        @Schema(description = "时段带 code：1=峰 2=谷", example = "1")
        Integer band,

        /** 星期口径 bitmask（ISO 周序 Mon=1 Tue=2 … Sun=64，合法域 1..127；如工作日＝31） */
        @Schema(description = "星期口径 bitmask：ISO 周序 Mon=1 Tue=2 Wed=4 Thu=8 Fri=16 "
                + "Sat=32 Sun=64，合法域 1..127（如工作日＝31）", example = "31")
        Integer dayMask,

        /** UTC 窗口起点分钟（含），0..1439（如 01:00＝60） */
        @Schema(description = "UTC 窗口起点分钟（含），0..1439（如 01:00＝60）", example = "60")
        Integer startMinute,

        /** UTC 窗口终点分钟（不含），1..1440（如 04:00＝240）；须大于起点——跨午夜窗口拆两行 */
        @Schema(description = "UTC 窗口终点分钟（不含），1..1440（如 04:00＝240）；须大于起点——"
                + "跨午夜窗口拆两行（22:00–02:00＝22:00–24:00＋00:00–02:00）", example = "240")
        Integer endMinute
) {
}
