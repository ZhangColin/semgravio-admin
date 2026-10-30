package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 槽位指派读面的 wire 镜像——与 aiplatform {@code BackofficeSlotAssignmentResponse}
 * （#249 槽位指派）字段同构：槽位键＋已指派技能清单行（与清单行共形、含停用行——
 * 「指派了但已停用」实态）。GET 与 PUT 回执同形。
 *
 * <p>{@code slot} 为职能槽位稳定键（main/executor/subagent），admin 字符串路径参数透传
 * 不预校验（spec #79）——未知槽位由 provider 判 404 SKL_010（数字业务码 7010）。</p>
 *
 * @param slot   职能槽位键（main=主智能体 / executor=run 执行体 / subagent=子智能体）
 * @param skills 已指派条目（含停用行；排序同技能清单：来源包、名称）
 */
public record AiplatformSlotAssignmentWireResponse(
        String slot,
        List<AiplatformSkillSummaryWireResponse> skills
) {
}
