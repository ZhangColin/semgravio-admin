package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 生产承接 wire 响应——与 aiplatform
 * {@code BackofficeProductionHandoffResponse}（#338 交付承接全量面）字段同构：
 * 承接字段（交付联系人/下一步说明/进展更新/预计开始时间——可空段如实 null）＋
 * 两态＋动作留痕时间线（新 → 旧，append-only 全量——动作/两侧状态/依据/操作者/时间；
 * 创建行＝开工判定系统操作者）。
 *
 * @param id              承接标识（TSID 十进制字符串）
 * @param projectId       挂靠项目（承接挂项目唯一——全生命周期一行）
 * @param orderId         来源订单（TSID 十进制字符串）
 * @param status          承接状态（code）：1=待安排生产 2=生产中（两态单向推进，无终态概念）
 * @param statusName      状态名
 * @param contact         交付联系人（可空，至多 200 字）
 * @param nextStep        下一步说明（可空，至多 1000 字，人工措辞）
 * @param progressNote    进展更新（可空，至多 2000 字，人工措辞、不上工程术语）
 * @param estimatedStartAt 预计开始时间（可空＝待安排——仅录入真实值才显示）
 * @param createdAt       创建时间（＝开工判定触发建档时点）
 * @param transitions     动作留痕时间线（新 → 旧，append-only）
 */
public record AiplatformProductionHandoffWireResponse(
        String id,
        String projectId,
        String orderId,
        Integer status,
        String statusName,
        String contact,
        String nextStep,
        String progressNote,
        LocalDateTime estimatedStartAt,
        LocalDateTime createdAt,
        List<TransitionWireResponse> transitions
) {

    /**
     * 留痕行（新 → 旧单行）：动作/两侧状态/依据/操作者/时间。非迁移类（创建/信息更新）
     * fromStatus = toStatus（在场快照），迁移类（推进）如实记录两侧。
     *
     * @param action         动作（code）：1=创建 2=推进 3=信息更新
     * @param actionName     动作名
     * @param fromStatus     动作前状态（code）
     * @param fromStatusName 动作前状态名
     * @param toStatus       动作后状态（code）
     * @param toStatusName   动作后状态名
     * @param basis          依据（必填，至多 1000 字——append-only 留痕行的正身）
     * @param operatorId     操作者标识（创建行＝system；后台动作＝admin 管理员 id）
     * @param operatorName   操作者名（创建行＝开工判定；后台动作＝admin 管理员昵称）
     * @param createdAt      留痕时间
     */
    public record TransitionWireResponse(
            Integer action,
            String actionName,
            Integer fromStatus,
            String fromStatusName,
            Integer toStatus,
            String toStatusName,
            String basis,
            String operatorId,
            String operatorName,
            LocalDateTime createdAt
    ) {
    }
}
