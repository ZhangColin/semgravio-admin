package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * aiplatform 开行命令体的 wire 镜像——与 aiplatform {@code OpenTimeBandRuleCommand}
 * （#497 时段规则写口）字段同构：空通道首开的初始插入通道（种子数据经幂等签名脚本走本
 * 端点种入——脚本侧幂等＝通道已有任意行即不再开行）。一次调用原子落 N 行窗口行＝一个规则
 * 版本。effectiveFrom 可回溯（种子敞口覆盖存量事件）、可指定未来时点（预发布），缺省即时
 * （provider 裁决 null→now()，BFF 不代填时点）。
 *
 * @param provider      模型提供方（与 UsageEvent.provider 同键＝匹配键全键）
 * @param windows       时段窗口清单（≥1 项；一项＝规则表一行，同单各窗口共享生效起点）
 * @param effectiveFrom 生效起点（可空＝缺省即时；可回溯；含未来时点＝预发布；同通道重叠
 *                      归 provider METER_008）
 */
public record AiplatformTimeBandRuleOpenWireRequest(
        String provider,
        @Schema(description = "时段窗口清单（≥1 项；每项＝规则表一行〔通道 × 生效区间 × "
                + "星期口径 × UTC 窗口〕，同单各窗口共享生效起点＝一个规则版本）")
        List<AiplatformTimeBandWindowWireRequest> windows,
        Instant effectiveFrom
) {
}
