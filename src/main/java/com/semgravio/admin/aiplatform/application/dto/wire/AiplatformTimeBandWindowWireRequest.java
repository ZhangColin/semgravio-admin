package com.semgravio.admin.aiplatform.application.dto.wire;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * aiplatform 时段窗口命令项的 wire 镜像——与 aiplatform {@code TimeWindowCommand}
 * （#497 开行/改窗命令的清单元素）字段同构：一条 UTC 时段窗口＝时段规则表一行；一次命令
 * 带 N 个窗口项＝一个规则版本（同生效区间原子落 N 行）。数值字段用包装型：缺字段→null→
 * provider 守卫出干净 400 METER_013，而非拆箱 NPE 出 500。band 契约 Integer code（1=峰
 * 2=谷——provider 侧 BaseEnum 从 code 反序列化）。
 *
 * @param band        时段带 code（1=峰 2=谷）
 * @param dayMask     星期口径 bitmask（ISO 周序 Mon=1 Tue=2 … Sun=64，合法域 1..127 非零）
 * @param startMinute UTC 窗口起点（含），0..1439
 * @param endMinute   UTC 窗口终点（不含），1..1440；须大于起点——跨午夜窗口 v1 不支持、
 *                    拆两行（22:00–02:00＝22:00–24:00＋00:00–02:00），服务端拦 METER_013
 */
public record AiplatformTimeBandWindowWireRequest(
        @Schema(description = "时段带 code：1=峰 2=谷", example = "1")
        Integer band,
        @Schema(description = "星期口径 bitmask：ISO 周序 Mon=1 Tue=2 Wed=4 Thu=8 Fri=16 "
                + "Sat=32 Sun=64，合法域 1..127（如工作日＝31）", example = "31")
        Integer dayMask,
        @Schema(description = "UTC 窗口起点分钟（含），0..1439（如 01:00＝60）", example = "60")
        Integer startMinute,
        @Schema(description = "UTC 窗口终点分钟（不含），1..1440（如 04:00＝240）；须大于起点——"
                + "跨午夜窗口拆两行（22:00–02:00＝22:00–24:00＋00:00–02:00）", example = "240")
        Integer endMinute
) {
}
