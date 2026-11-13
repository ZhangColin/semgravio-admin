package com.semgravio.admin.aiplatform.application.dto.command;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台时段规则原子改窗北向命令——单调用「关当前版本全部行＋开新版本」同事务，逐字镜像
 * aiplatform {@code RewindowTimeBandRuleCommand}（#497 原子改窗——对称单价表原子改价）：
 * 新窗口/星期口径取本命令，通道沿用被关版本（寻址行须当前版本成员——版本任一行皆可达，
 * 关行在版本级非单行）。整版本替换语义——清单即终态。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性由
 * provider 聚合守卫裁决，错误信封原样透传。操作者身份不在命令体——经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕新行（被关行保留其原
 * 开行操作者，不被改写）。{@code effectiveFrom} 可空＝缺省即时，含未来时点＝预发布（对齐
 * 供应商调窗口公告期），须不早于被关版本起点（否则 provider METER_014）。</p>
 *
 * @param windows       新时段窗口清单（≥1 项；整版本替换语义——清单即终态）
 * @param effectiveFrom 新版本生效起点（可空＝缺省即时；含未来时点＝预发布；须不早于被关
 *                      版本起点，否则 provider METER_014）
 */
public record AiplatformTimeBandRuleRewindowCommand(

        /** 新时段窗口清单（≥1 项；整版本替换语义——清单即终态，被关版本的全部窗口不再延续） */
        @Schema(description = "新时段窗口清单（≥1 项；整版本替换语义——清单即终态，被关版本"
                + "的全部窗口不再延续）")
        List<AiplatformTimeWindowCommand> windows,

        /** 新版本生效起点（可空＝缺省即时；含未来时点＝预发布；ISO-8601 Instant UTC 带 Z） */
        Instant effectiveFrom
) {
}
