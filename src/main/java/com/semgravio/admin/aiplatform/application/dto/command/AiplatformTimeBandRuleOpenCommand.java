package com.semgravio.admin.aiplatform.application.dto.command;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台时段规则开行北向命令——运营人员对指定通道新开一个规则版本，逐字镜像 aiplatform
 * {@code OpenTimeBandRuleCommand}（#497 时段规则写口）：一单 N 时段窗口行原子落（每项＝
 * 规则表一行〔通道 × 生效区间 × 星期口径 × UTC 窗口〕，同单各窗口共享生效起点）——DeepSeek
 * 峰窗 01:00–04:00＋06:00–10:00 工作日（dayMask 31）两窗口一单种入。写口唯一化到管理 API
 * 后本端点是唯一初始插入通道（provider 侧种子数据经幂等签名脚本走 provider 端点种入）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性由
 * provider 聚合守卫裁决（METER_013/METER_008），错误信封原样透传。操作者身份不在命令体——
 * 经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕新行，前端无法
 * 伪造留痕。{@code effectiveFrom} 可回溯（种子敞口覆盖存量事件）、可指定未来时点（预发布），
 * 可空＝缺省即时（provider 裁决 now()，BFF 不代填时点）。</p>
 *
 * @param provider      模型提供方（与 UsageEvent.provider 同键＝匹配键全键）
 * @param windows       时段窗口清单（≥1 项；一项＝规则表一行，同单各窗口共享生效起点）
 * @param effectiveFrom 生效起点（可空＝缺省即时；可回溯；含未来时点＝预发布）
 */
public record AiplatformTimeBandRuleOpenCommand(

        /** 模型提供方（匹配键全键，如 deepseek） */
        String provider,

        /** 时段窗口清单（≥1 项；每项＝规则表一行，同单各窗口共享生效起点＝一个规则版本） */
        @Schema(description = "时段窗口清单（≥1 项；每项＝规则表一行〔通道 × 生效区间 × "
                + "星期口径 × UTC 窗口〕，同单各窗口共享生效起点＝一个规则版本）")
        List<AiplatformTimeWindowCommand> windows,

        /** 生效起点（可空＝缺省即时；可回溯；含未来时点＝预发布，ISO-8601 Instant UTC 带 Z） */
        Instant effectiveFrom
) {
}
