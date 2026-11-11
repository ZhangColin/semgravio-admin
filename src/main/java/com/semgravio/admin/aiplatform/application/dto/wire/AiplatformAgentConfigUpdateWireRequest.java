package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 智能体配置全量写命令体的 wire 镜像——与 aiplatform {@code AgentConfigUpdateCommand}
 * （#251 智能体运营配置，#246 技能线 T5；B3 issue #112 增路由强档件；issue #122 增 #432 提速
 * 批次十件〔V53–V57〕）字段同构：覆盖三件（systemPrompt/modelId/routingStrongModelId）＋
 * 增强工具开关两件＋提速批次十件（三闸/单会话/并行机器/决策注入三键＋verify 自决）。
 *
 * <p>出站序列化口径：String/List/Integer 字段无 Long/BigDecimal 特殊口径；开关全部
 * <strong>Boolean 装箱</strong>（与 provider 命令同型）——北向缺省＝null 出站在场（全局
 * Jackson 含 null，同技能安装 excludeDirs 先例）。缺省方向<strong>不一致</strong>（票面
 * 语义陷阱②）：webSearch/fetchUrl/singleSession/decisionHints/verifySelfDecision 缺省
 * true（开）；三闸（verificationPresenceGate/proofOfOutputGate/finalVerificationGate）与
 * parallelSliceMachine 缺省 false（关）——null 对两类均＝回落缺省，原生 boolean 会把缺省
 * 绑成 false（关）语义反了。覆盖三件 null＝清空回落枚举/代码默认；决策注入两阈值 null＝
 * 清空回落代码缺省 3；禁用集 null/空数组＝全启用。越界值（errorStreak 2–10 / stackCap
 * 1–3 / 禁用集非法 token）由 provider 写用例裁决 PRJ_081/082/083，admin 不代判不预校验。</p>
 *
 * <p>provider 命令另带路由开关/两阈值/升档标签映射/verify 面九件（#354/#424/#426/#427）
 * ——B3 票面不点名，本票十件之外不镜像：admin 命令体不出站＝provider null＝缺省态；
 * BFF 是 provider 后台唯一写面、九件从无可设处，结构上恒缺省态（票面字面口径）。</p>
 *
 * @param systemPrompt       覆盖 systemPrompt（null＝清空回落枚举默认）
 * @param modelId            覆盖模型档位键（null＝清空回落枚举默认）
 * @param webSearchEnabled   联网搜索开关（null＝provider 缺省 true 开）
 * @param fetchUrlEnabled    网页抓取开关（null＝provider 缺省 true 开）
 * @param routingStrongModelId 覆盖路由强档键（null＝清空回落代码缺省 pro）
 * @param verificationPresenceGateEnabled 验证在场闸开关（#433；null＝缺省 false＝闸关。仅 run 执行体行消费）
 * @param proofOfOutputGateEnabled 空转产出证明闸开关（#433；null＝缺省 false＝闸关。仅 run 执行体行消费）
 * @param finalVerificationGateEnabled 整轨终验拦「生成完成」开关（#433；null＝缺省 false＝闸关。仅 run 执行体行消费）
 * @param singleSessionGenerationEnabled 单会话连续编码开关（#436；null＝缺省 true＝单会话开。仅 run 执行体行消费）
 * @param parallelSliceMachineEnabled 切片并行机器开关（#436；null＝缺省 false＝并行机器关。仅 run 执行体行消费）
 * @param decisionHintsEnabled 决策时注入总开关（#437；null＝缺省 true＝注入开。仅 run 执行体行消费）
 * @param decisionHintErrorStreak 失败止损阈值（#437；null＝清空回落代码缺省 3；2–10 整数，越界 PRJ_081）
 * @param decisionHintStackCap 微指令堆叠上限（#437；null＝清空回落代码缺省 3；1–3 整数，越界 PRJ_082）
 * @param decisionHintDisabled 禁用集（#437；键数组——封闭集 failure_stop_loss/anti_mock/context_economy/parallel_tools，非法 token PRJ_083；null/空数组＝全启用）
 * @param verifySelfDecisionEnabled verify 自决开关（#442；null＝缺省 true＝自决开。仅 run 执行体行消费）
 */
public record AiplatformAgentConfigUpdateWireRequest(
        String systemPrompt,
        String modelId,
        Boolean webSearchEnabled,
        Boolean fetchUrlEnabled,
        String routingStrongModelId,
        Boolean verificationPresenceGateEnabled,
        Boolean proofOfOutputGateEnabled,
        Boolean finalVerificationGateEnabled,
        Boolean singleSessionGenerationEnabled,
        Boolean parallelSliceMachineEnabled,
        Boolean decisionHintsEnabled,
        Integer decisionHintErrorStreak,
        Integer decisionHintStackCap,
        List<String> decisionHintDisabled,
        Boolean verifySelfDecisionEnabled
) {
}
