package com.semgravio.admin.aiplatform.application.dto.command;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台生产承接信息更新北向命令（issue #107，spec #102 A 批 A4）——逐字镜像 aiplatform
 * {@code UpdateProductionHandoffCommand}（#338 交付承接，「生产承接更新」之字段面）：
 * <strong>整组替换</strong>语义——未录入段传 null 即清空（预计开始时间清空＝回待安排
 * 呈现，「仅真实值显示」）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，同
 * {@link AiplatformContractActionCommand}）：字段合法性（依据必填 PRJ_057、超长
 * PRJ_058、操作者必携 PRJ_059）由 provider 聚合守卫裁决，错误信封原样透传——BFF 零
 * 状态机复算零加戏（守卫归 provider 裁决，spec #102）。操作者身份不在命令体——经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（provider 承接
 * 动作以操作者为审计正身、缺头 PRJ_059 拒——前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProductionUpdateCommand(

        /** 交付联系人（可空，至多 200 字——超长归 provider PRJ_058） */
        @Schema(description = "交付联系人（可空，至多 200 字——超长由 provider 裁决 PRJ_058）",
                example = "李经理（微信 ligoods）")
        String contact,

        /** 下一步说明（可空，至多 1000 字，人工措辞——不上工程术语） */
        @Schema(description = "下一步说明（可空，至多 1000 字，人工措辞）", example = "准备服务器环境与域名备案")
        String nextStep,

        /** 进展更新（可空，至多 2000 字，人工措辞） */
        @Schema(description = "进展更新（可空，至多 2000 字，人工措辞、不上工程术语）",
                example = "已完成技术方案拆解")
        String progressNote,

        /** 预计开始时间（可空＝待安排——仅录入真实值才显示；清空即回待安排呈现） */
        @Schema(description = "预计开始时间（可空＝待安排——仅录入真实值才显示，传 null 即清空）",
                example = "2026-10-20T09:00:00")
        LocalDateTime estimatedStartAt,

        /** 依据（必填，至多 1000 字——append-only 留痕行的正身；缺失/超长归 provider PRJ_057/058） */
        @Schema(description = "操作依据（必填，至多 1000 字——append-only 留痕行的正身。缺失/超长"
                + "由 provider 裁决：PRJ_057/PRJ_058）", example = "首次排期录入")
        String basis
) {
}
