package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 工具面清单槽位分组的 wire 镜像——与 aiplatform {@code BackofficeAgentToolSlotResponse}
 * （#252 工具面，#246 技能线 T6）字段同构：职能槽位稳定键＋展示名＋该槽位工具行集。
 *
 * <p>三槽位与技能槽位同键（main=主智能体／executor=run 执行体／subagent=子智能体）——
 * admin 不解析槽位键（清单由 provider 组装直出，无路径参数）。呈现序＝平台资产在前、
 * harness 内建在后（provider 定死，admin 原样保序透传）。</p>
 *
 * @param slot     职能槽位稳定键（与技能槽位同键）：main／executor／subagent
 * @param slotName 槽位展示名
 * @param tools    该槽位工具面（呈现序＝平台资产在前、harness 内建在后）
 */
public record AiplatformAgentToolSlotWireResponse(
        String slot,
        String slotName,
        List<AiplatformAgentToolWireResponse> tools
) {
}
