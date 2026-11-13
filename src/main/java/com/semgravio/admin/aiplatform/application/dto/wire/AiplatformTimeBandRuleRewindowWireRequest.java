package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * aiplatform 原子改窗命令体的 wire 镜像——与 aiplatform {@code RewindowTimeBandRuleCommand}
 * （#497 原子改窗——对称单价表原子改价）字段同构：单调用「关当前版本全部行＋开新版本」
 * 同事务；新窗口/星期口径取本命令，通道沿用被关版本。整版本替换语义——清单即终态。
 * effectiveFrom 可空＝缺省即时（provider 裁决），须不早于被关版本起点（否则 provider
 * METER_014）。
 *
 * @param windows       新时段窗口清单（≥1 项；整版本替换语义——清单即终态）
 * @param effectiveFrom 新版本生效起点（可空＝缺省即时；含未来时点＝预发布，对齐供应商调
 *                      窗口公告期；须不早于被关版本起点，否则 provider METER_014）
 */
public record AiplatformTimeBandRuleRewindowWireRequest(
        @Schema(description = "新时段窗口清单（≥1 项；整版本替换语义——清单即终态，被关版本"
                + "的全部窗口不再延续）")
        List<AiplatformTimeBandWindowWireRequest> windows,
        Instant effectiveFrom
) {
}
