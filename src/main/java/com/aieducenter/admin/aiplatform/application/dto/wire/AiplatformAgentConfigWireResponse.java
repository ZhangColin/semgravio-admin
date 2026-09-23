package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 智能体配置读面的 wire 镜像——与 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5）字段同构：生效值＋覆盖标记＋枚举默认预览＋增强
 * 工具开关存储态＋最近写者。GET 读面与 PUT 回执同形（provider 写入后重读同一视图）。
 *
 * <p>覆盖标记与开关四件 provider 恒在场（原生 boolean，wire 同型）；prompt/档位生效值与
 * 默认预览四串恒在场（生效值在无覆盖行语境＝枚举默认）；operator 两肢与 updatedAt 可空
 * （null＝从未配置——无覆盖行＝全回落）。</p>
 *
 * @param agentKey               智能体稳定键（main=主智能体 / executor=run 执行体）
 * @param agentName              展示名
 * @param systemPrompt           生效 systemPrompt（库覆盖值或枚举默认——装配实取值）
 * @param modelId                生效模型档位裸名
 * @param systemPromptOverridden systemPrompt 是否库覆盖值
 * @param modelIdOverridden      模型档位是否库覆盖值
 * @param defaultSystemPrompt    枚举默认 systemPrompt（清空覆盖即落此值）
 * @param defaultModelId         枚举默认模型档位（清空覆盖即落此值）
 * @param webSearchEnabled       联网搜索开关生效态
 * @param fetchUrlEnabled        网页抓取开关生效态
 * @param operatorId             最近写者 id（null＝从未配置）
 * @param operatorName           最近写者名
 * @param updatedAt              最近写入时刻（null＝从未配置）
 */
public record AiplatformAgentConfigWireResponse(
        String agentKey,
        String agentName,
        String systemPrompt,
        String modelId,
        boolean systemPromptOverridden,
        boolean modelIdOverridden,
        String defaultSystemPrompt,
        String defaultModelId,
        boolean webSearchEnabled,
        boolean fetchUrlEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
