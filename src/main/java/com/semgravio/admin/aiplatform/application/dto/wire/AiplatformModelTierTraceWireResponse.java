package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 模型档位变更留痕 wire 响应行——与 aiplatform
 * {@code BackofficeModelTierTraceResponse}（#367 档位留痕）字段同构：一次实际变更的
 * 前后全量值快照＋操作者＋时刻，按时间倒序（最近先）。append-only——留痕不随任何动作
 * 删除（创建痕 old 四件全 null、删除痕 new 四件全 null，跨行存活；留痕读面不要求行
 * 存在——未知键空清单 200 非 404）。
 *
 * <p>{@code newDisplayName} null＝本次清空回落语义键（终态替换语义）；
 * {@code oldEnabled/newEnabled} 为 Boolean 装箱（创建痕/删除痕对应侧 null）。</p>
 *
 * @param id              留痕标识（TSID 十进制串）
 * @param oldProviderKey  变更前所属通道键（创建痕＝null）
 * @param oldModelName    变更前实际模型名（创建痕＝null）
 * @param oldDisplayName  变更前显示名（创建痕/此前无显示名＝null）
 * @param oldEnabled      变更前启用态（创建痕＝null）
 * @param newProviderKey  变更后所属通道键（删除痕＝null）
 * @param newModelName    变更后实际模型名（删除痕＝null）
 * @param newDisplayName  变更后显示名（本次清空回落语义键/删除痕＝null）
 * @param newEnabled      变更后启用态（删除痕＝null）
 * @param operatorId      变更操作者 id
 * @param operatorName    变更操作者名（直读展示）
 * @param operatedAt      变更动作时刻
 */
public record AiplatformModelTierTraceWireResponse(
        String id,
        String oldProviderKey,
        String oldModelName,
        String oldDisplayName,
        Boolean oldEnabled,
        String newProviderKey,
        String newModelName,
        String newDisplayName,
        Boolean newEnabled,
        String operatorId,
        String operatorName,
        LocalDateTime operatedAt
) {
}
