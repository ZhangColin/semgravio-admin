package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 智能体配置读面的 wire 镜像——与 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5；B3 issue #112 增路由强档三件——镜像 provider
 * #367 落地后的 B3 面）字段同构：生效值＋覆盖标记＋枚举默认预览＋增强工具开关存储态＋
 * <strong>路由强档三件</strong>＋最近写者。GET 读面与 PUT 回执同形（provider 写入后重读
 * 同一视图）。
 *
 * <p>覆盖标记与开关四件 provider 恒在场（原生 boolean，wire 同型）；prompt/档位生效值与
 * 默认预览四串恒在场（生效值在无覆盖行语境＝枚举默认）；operator 两肢与 updatedAt 可空
 * （null＝从未配置——无覆盖行＝全回落）。modelId/routingStrongModelId 语义＝档位键
 * （ADR-0031 语义键，V44 存量已刷档——非裸模型名）。</p>
 *
 * <p>provider 读面另带路由开关/两阈值六件（{@code routingEnabled}±/
 * {@code routingFileCountThreshold}±/{@code routingSliceFeatureThreshold}±，#354）——B3
 * 票面只点名 routingStrongModelId 一族，六件不镜像（Jackson 容错忽略、读面不呈现；BFF
 * 是唯一写面、对应写肢从无可设处——票面字面口径）。</p>
 *
 * @param agentKey                      智能体稳定键（main=主智能体 / executor=run 执行体 / designer=设计执行体）
 * @param agentName                     展示名
 * @param systemPrompt                  生效 systemPrompt（库覆盖值或枚举默认——装配实取值）
 * @param modelId                       生效模型档位键（ADR-0031 语义键，如 flash/pro）
 * @param systemPromptOverridden        systemPrompt 是否库覆盖值
 * @param modelIdOverridden             模型档位是否库覆盖值
 * @param defaultSystemPrompt           枚举默认 systemPrompt（清空覆盖即落此值）
 * @param defaultModelId                枚举默认模型档位键（清空覆盖即落此值）
 * @param webSearchEnabled              联网搜索开关生效态
 * @param fetchUrlEnabled               网页抓取开关生效态
 * @param routingStrongModelId          强模型档生效值（库覆盖值或代码缺省 pro——路由升档落点）
 * @param routingStrongModelIdOverridden 强模型档是否库覆盖值
 * @param defaultRoutingStrongModelId   代码缺省强模型档（清空覆盖即落此值）
 * @param operatorId                    最近写者 id（null＝从未配置）
 * @param operatorName                  最近写者名
 * @param updatedAt                     最近写入时刻（null＝从未配置）
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
        String routingStrongModelId,
        boolean routingStrongModelIdOverridden,
        String defaultRoutingStrongModelId,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
