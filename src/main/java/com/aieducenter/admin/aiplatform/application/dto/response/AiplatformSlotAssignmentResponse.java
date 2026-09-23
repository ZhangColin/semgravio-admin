package com.aieducenter.admin.aiplatform.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台槽位指派读面——北向出口，逐字镜像 aiplatform {@code BackofficeSlotAssignmentResponse}
 * （#249 槽位指派）：槽位键＋该槽位当前指派的技能清单行（与技能清单行共形——含停用行，运营
 * 可见「指派了但已停用」的实态；停用行不参与装配合成）。GET 与 PUT 回执同形（provider PUT
 * 换行集后重读同一视图）。
 *
 * <p>{@code slot} 为职能槽位稳定键（main/executor/subagent 三把）——admin 字符串路径参数
 * 透传不预校验（spec #79），未知槽位由 provider 判 404 SKL_010＝数字业务码 7010。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSlotAssignmentResponse(

        /** 职能槽位键（main=主智能体 / executor=run 执行体 / subagent=子智能体） */
        @Schema(description = "职能槽位键（main=主智能体 / executor=run 执行体 / "
                + "subagent=子智能体；未知槽位 404 SKL_010＝数字业务码 7010）",
                example = "executor")
        String slot,

        /** 已指派技能清单行（与技能清单行共形，含停用行——「指派了但已停用」实态；停用行不参与装配合成） */
        @Schema(description = "已指派技能清单行（与技能清单行共形，含停用行——停用行"
                + "不参与装配合成；跨包同名列靠来源包区分）")
        List<AiplatformSkillSummaryResponse> skills
) {
}
