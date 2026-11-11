package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 智能体配置变更留痕读面行的 wire 镜像——与 aiplatform
 * {@code BackofficeAgentConfigTraceResponse}（#251 留痕回滚，#246 技能线 T5；B3 issue #112
 * 增路由强档两件；issue #122 增 #432 提速批次二十件〔V53–V57〕）字段同构：一次实际变更的
 * 前后全量值快照（old&#42;/new&#42; 对＋操作者＋时刻）。append-only——留痕不随任何动作删除；
 * 按时间倒序（最近先）；回滚＝把某痕旧值快照经 PUT 写回（一次新变更、留新痕，不做版本树）。
 *
 * <p>old&#42;/new&#42; 的 prompt/档位/<strong>强档</strong>三件可空（null＝该侧即缺省回落——
 * 回滚写回 null 即清空覆盖），决策注入两阈值与禁用集同样可空（null＝该侧即代码缺省/全启用；
 * 禁用集留痕为<strong>存储形逗号接键串</strong>，与读面 List 形不同——provider 留痕快照直读
 * 存储列）；开关件 provider 恒在场（原生 boolean，wire 同型）。provider 留痕另带路由开关/
 * 两阈值/升档标签映射/verify 面旧件（#354/#424/#426/#427），同读面口径不镜像（Jackson 容错
 * 忽略）。边缘调用点留痕行同形复用（边缘行档位列之外快照件恒缺省态：prompt null、开关
 * 缺省态〔webSearch/fetchUrl 恒 true〕、强档 null、提速批次恒代码缺省态）。</p>
 *
 * @param id                       留痕标识（TSID 十进制串）
 * @param oldSystemPrompt          变更前 systemPrompt（null＝此前即缺省回落）
 * @param oldModelId               变更前模型档位键（null＝此前即缺省回落）
 * @param oldWebSearchEnabled      变更前联网搜索开关
 * @param oldFetchUrlEnabled       变更前网页抓取开关
 * @param oldRoutingStrongModelId  变更前强模型档（null＝此前即回落代码缺省）
 * @param oldVerificationPresenceGateEnabled 变更前验证在场闸开关（#433；恒有值——缺省态 false＝闸关）
 * @param oldProofOfOutputGateEnabled 变更前空转产出证明闸开关（#433；恒有值）
 * @param oldFinalVerificationGateEnabled 变更前整轨终验拦「生成完成」开关（#433；恒有值）
 * @param oldSingleSessionGenerationEnabled 变更前单会话连续编码开关（#436；恒有值——缺省态 true）
 * @param oldParallelSliceMachineEnabled 变更前切片并行机器开关（#436；恒有值——缺省态 false）
 * @param oldDecisionHintsEnabled  变更前决策时注入总开关（#437；恒有值——缺省态 true）
 * @param oldDecisionHintErrorStreak 变更前失败止损阈值（#437；null＝此前即代码缺省 3）
 * @param oldDecisionHintStackCap  变更前微指令堆叠上限（#437；null＝此前即代码缺省 3）
 * @param oldDecisionHintDisabled  变更前禁用集（#437 存储形逗号接键串；null＝此前即全启用；空串＝全启用显式形）
 * @param oldVerifySelfDecisionEnabled 变更前 verify 自决开关（#442；恒有值——缺省态 true）
 * @param newSystemPrompt          变更后 systemPrompt（null＝本次清空回落枚举默认）
 * @param newModelId               变更后模型档位键（null＝本次清空回落）
 * @param newWebSearchEnabled      变更后联网搜索开关
 * @param newFetchUrlEnabled       变更后网页抓取开关
 * @param newRoutingStrongModelId  变更后强模型档（null＝本次清空回落代码缺省）
 * @param newVerificationPresenceGateEnabled 变更后验证在场闸开关（#433；恒有值）
 * @param newProofOfOutputGateEnabled 变更后空转产出证明闸开关（#433；恒有值）
 * @param newFinalVerificationGateEnabled 变更后整轨终验拦「生成完成」开关（#433；恒有值）
 * @param newSingleSessionGenerationEnabled 变更后单会话连续编码开关（#436；恒有值）
 * @param newParallelSliceMachineEnabled 变更后切片并行机器开关（#436；恒有值）
 * @param newDecisionHintsEnabled  变更后决策时注入总开关（#437；恒有值）
 * @param newDecisionHintErrorStreak 变更后失败止损阈值（#437；null＝本次清空回落代码缺省 3）
 * @param newDecisionHintStackCap  变更后微指令堆叠上限（#437；null＝本次清空回落代码缺省 3）
 * @param newDecisionHintDisabled  变更后禁用集（#437 存储形逗号接键串；null＝本次清空全启用）
 * @param newVerifySelfDecisionEnabled 变更后 verify 自决开关（#442；恒有值）
 * @param operatorId               变更操作者 id
 * @param operatorName             变更操作者名
 * @param operatedAt               变更动作时刻（留痕生成时刻）
 */
public record AiplatformAgentConfigTraceWireResponse(
        String id,
        String oldSystemPrompt,
        String oldModelId,
        boolean oldWebSearchEnabled,
        boolean oldFetchUrlEnabled,
        String oldRoutingStrongModelId,
        boolean oldVerificationPresenceGateEnabled,
        boolean oldProofOfOutputGateEnabled,
        boolean oldFinalVerificationGateEnabled,
        boolean oldSingleSessionGenerationEnabled,
        boolean oldParallelSliceMachineEnabled,
        boolean oldDecisionHintsEnabled,
        Integer oldDecisionHintErrorStreak,
        Integer oldDecisionHintStackCap,
        String oldDecisionHintDisabled,
        boolean oldVerifySelfDecisionEnabled,
        String newSystemPrompt,
        String newModelId,
        boolean newWebSearchEnabled,
        boolean newFetchUrlEnabled,
        String newRoutingStrongModelId,
        boolean newVerificationPresenceGateEnabled,
        boolean newProofOfOutputGateEnabled,
        boolean newFinalVerificationGateEnabled,
        boolean newSingleSessionGenerationEnabled,
        boolean newParallelSliceMachineEnabled,
        boolean newDecisionHintsEnabled,
        Integer newDecisionHintErrorStreak,
        Integer newDecisionHintStackCap,
        String newDecisionHintDisabled,
        boolean newVerifySelfDecisionEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime operatedAt
) {
}
