package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 智能体配置读面的 wire 镜像——与 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5；B3 issue #112 增路由强档三件；issue #122 增
 * #432 提速批次十件〔迁移 V53–V57〕——镜像 provider V53–V57 落地后的 B3 面）字段同构：
 * 生效值＋覆盖标记＋枚举默认预览＋增强工具开关存储态＋路由强档三件＋<strong>提速批次
 * 十五件</strong>（十开关/阈值/禁用集生效值＋决策注入三件的覆盖标记与默认预览）＋最近
 * 写者。GET 读面与 PUT 回执同形（provider 写入后重读同一视图）。
 *
 * <p>覆盖标记与开关 provider 恒在场（原生 boolean，wire 同型）；prompt/档位生效值与
 * 默认预览四串恒在场（生效值在无覆盖行语境＝枚举默认）；operator 两肢与 updatedAt 可空
 * （null＝从未配置——无覆盖行＝全回落）。modelId/routingStrongModelId 语义＝档位键
 * （ADR-0031 语义键，V44 存量已刷档——非裸模型名）。</p>
 *
 * <p>provider 读面另带路由开关/两阈值/升档标签映射/verify 面六件（#354/#424/#426/#427），
 * B3 票面不点名——不镜像（Jackson 容错忽略、读面不呈现；BFF 是唯一写面、对应写肢从无
 * 可设处——票面字面口径）。#122 提速批次十件（三闸/单会话/并行机器/决策注入三键＋
 * verify 自决）仅 run 执行体行消费——main/designer 页可隐藏或置灰。</p>
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
 * @param verificationPresenceGateEnabled 验证在场闸开关生效态（#433；缺省 false＝闸关。仅 run 执行体行消费）
 * @param proofOfOutputGateEnabled      空转产出证明闸开关生效态（#433；缺省 false＝闸关。仅 run 执行体行消费）
 * @param finalVerificationGateEnabled  整轨终验拦「生成完成」开关生效态（#433；缺省 false＝闸关。仅 run 执行体行消费）
 * @param singleSessionGenerationEnabled 单会话连续编码开关生效态（#436；缺省 true＝单会话开。仅 run 执行体行消费）
 * @param parallelSliceMachineEnabled   切片并行机器开关生效态（#436；缺省 false＝并行机器关。仅 run 执行体行消费）
 * @param decisionHintsEnabled          决策时注入总开关生效态（#437；缺省 true＝注入开。仅 run 执行体行消费）
 * @param decisionHintErrorStreak       失败止损阈值生效值（#437；库覆盖值或代码缺省 3）
 * @param decisionHintErrorStreakOverridden 失败止损阈值是否库覆盖值
 * @param defaultDecisionHintErrorStreak 代码缺省失败止损阈值（预览：清空覆盖即落此值）
 * @param decisionHintStackCap          微指令堆叠上限生效值（#437；库覆盖值或代码缺省 3）
 * @param decisionHintStackCapOverridden 堆叠上限是否库覆盖值
 * @param defaultDecisionHintStackCap   代码缺省堆叠上限（预览：清空覆盖即落此值）
 * @param decisionHintDisabled          禁用集生效值（#437；键数组——空数组＝全启用）
 * @param decisionHintDisabledOverridden 禁用集是否库覆盖值（false＝全启用）
 * @param verifySelfDecisionEnabled     verify 自决开关生效态（#442；缺省 true＝自决开。仅 run 执行体行消费）
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
        boolean verificationPresenceGateEnabled,
        boolean proofOfOutputGateEnabled,
        boolean finalVerificationGateEnabled,
        boolean singleSessionGenerationEnabled,
        boolean parallelSliceMachineEnabled,
        boolean decisionHintsEnabled,
        Integer decisionHintErrorStreak,
        boolean decisionHintErrorStreakOverridden,
        Integer defaultDecisionHintErrorStreak,
        Integer decisionHintStackCap,
        boolean decisionHintStackCapOverridden,
        Integer defaultDecisionHintStackCap,
        List<String> decisionHintDisabled,
        boolean decisionHintDisabledOverridden,
        boolean verifySelfDecisionEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
