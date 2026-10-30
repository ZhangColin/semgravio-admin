package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 智能体配置变更留痕读面行的 wire 镜像——与 aiplatform
 * {@code BackofficeAgentConfigTraceResponse}（#251 留痕回滚，#246 技能线 T5）字段同构：
 * 一次实际变更的前后全量值快照（old&#42;/new&#42; 四件对）＋操作者＋时刻。append-only——留痕不随
 * 任何动作删除；按时间倒序（最近先）；回滚＝把某痕旧值快照经 PUT 写回（一次新变更、留新痕，
 * 不做版本树）。
 *
 * <p>old&#42;/new&#42; 的 prompt/档位两件可空（null＝该侧即缺省回落——回滚写回 null 即清空覆盖），
 * 开关四件 provider 恒在场（原生 boolean，wire 同型）。</p>
 *
 * @param id                  留痕标识（TSID 十进制串）
 * @param oldSystemPrompt     变更前 systemPrompt（null＝此前即缺省回落）
 * @param oldModelId          变更前模型档位（null＝此前即缺省回落）
 * @param oldWebSearchEnabled 变更前联网搜索开关
 * @param oldFetchUrlEnabled  变更前网页抓取开关
 * @param newSystemPrompt     变更后 systemPrompt（null＝本次清空回落枚举默认）
 * @param newModelId          变更后模型档位（null＝本次清空回落）
 * @param newWebSearchEnabled 变更后联网搜索开关
 * @param newFetchUrlEnabled  变更后网页抓取开关
 * @param operatorId          变更操作者 id
 * @param operatorName        变更操作者名
 * @param operatedAt          变更动作时刻（留痕生成时刻）
 */
public record AiplatformAgentConfigTraceWireResponse(
        String id,
        String oldSystemPrompt,
        String oldModelId,
        boolean oldWebSearchEnabled,
        boolean oldFetchUrlEnabled,
        String newSystemPrompt,
        String newModelId,
        boolean newWebSearchEnabled,
        boolean newFetchUrlEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime operatedAt
) {
}
