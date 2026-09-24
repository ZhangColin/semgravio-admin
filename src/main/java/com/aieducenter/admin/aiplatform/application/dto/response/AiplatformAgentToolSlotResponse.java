package com.aieducenter.admin.aiplatform.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台工具面清单槽位分组——北向出口，逐字镜像 aiplatform
 * {@code BackofficeAgentToolSlotResponse}（#252 工具面，#246 技能线 T6，spec #79 定稿、
 * issue #85）：职能槽位稳定键＋展示名＋该槽位工具行集。
 *
 * <p>三槽位与技能槽位同键（main=主智能体／executor=run 执行体／subagent=子智能体）——
 * 三呈现源（平台资产骨架/增强、harness 内建编码工具 executor 槽、子智能体声明工具面）
 * 由 provider 组装，admin 无路径参数无过滤原样保序透传。呈现序＝平台资产在前、harness
 * 内建在后（provider 定死）。清单 List 直出（三槽位有界，不造分页）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentToolSlotResponse(

        /** 职能槽位稳定键（与技能槽位同键）：main=主智能体 / executor=run 执行体 / subagent=子智能体 */
        @Schema(description = "职能槽位稳定键（与技能槽位同键）：main=主智能体 / "
                + "executor=run 执行体 / subagent=子智能体", example = "main")
        String slot,

        /** 槽位展示名 */
        @Schema(description = "槽位展示名", example = "主智能体")
        String slotName,

        /** 该槽位工具面（呈现序＝平台资产在前、harness 内建在后） */
        @Schema(description = "该槽位工具面（呈现序＝平台资产在前、harness 内建在后）")
        List<AiplatformAgentToolResponse> tools
) {
}
