package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 后台付款计划详情 wire 响应——与 aiplatform
 * {@code BackofficePaymentPlanDetailResponse}（#337 核验承接全量面）字段同构：
 * 活跃计划全量（节点＋凭证＋留痕时间线）＋历史计划行摘要（旧计划随旧合同留痕
 * ——撤回重建/范围重冻后旧行定格可见）。
 *
 * @param plan    活跃计划（用户面同构响应）
 * @param trails  核验留痕时间线（新 → 旧，跨节点全量）
 * @param history 历史计划行摘要（新 → 旧，不含活跃行）
 */
public record AiplatformPaymentPlanDetailWireResponse(
        AiplatformPaymentPlanWireResponse plan,
        List<AiplatformPaymentVerificationWireResponse> trails,
        List<HistoryPlanWireResponse> history
) {

    /** 历史计划行摘要（留痕面——节点细节不在本面，行身份与冻结集指向足矣）。 */
    public record HistoryPlanWireResponse(
            String id,
            String contractId,
            String contractNo,
            int nodeCount,
            Long totalAmount,
            Long totalSettled,
            LocalDateTime createdAt
    ) {
    }
}
