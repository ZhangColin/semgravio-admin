package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台生产承接动作北向命令（issue #107，spec #102 A 批 A4——推进）——逐字镜像
 * aiplatform {@code ProductionHandoffActionCommand}（#338）：依据＝人工推进留痕的
 * 必填自由文本（操作人/时间由 provider 服务端随行落）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，同
 * {@link AiplatformContractActionCommand}）：字段合法性（依据缺失 PRJ_057、超长
 * PRJ_058、操作者必携 PRJ_059、状态守卫 PRJ_056）由 provider 聚合守卫裁决，错误信封
 * 原样透传——BFF 零状态机复算零加戏（守卫归 provider 裁决，spec #102）。操作者身份
 * 不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传
 * 落痕（provider 承接动作以操作者为审计正身、缺头 PRJ_059 拒——前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProductionActionCommand(

        /** 依据（必填，至多 1000 字——append-only 留痕行的正身；缺失/超长归 provider PRJ_057/058） */
        @Schema(description = "操作依据（必填，至多 1000 字——append-only 留痕行的正身。缺失/超长"
                + "由 provider 裁决：PRJ_057/PRJ_058）", example = "排期落定，今日开工")
        String basis
) {
}
