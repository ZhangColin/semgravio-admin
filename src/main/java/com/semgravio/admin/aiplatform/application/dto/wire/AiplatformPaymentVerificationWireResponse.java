package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 付款核验留痕行 wire 响应——与 aiplatform {@code PaymentVerificationResponse}
 * （#337）字段同构：每次到账核验/打回各一行（append-only）——实到金额/实到时间/
 * 关联凭证/依据、操作人随行落；「不足留痕」「打回依据留痕」的审计正身。新 → 旧
 * 由 provider 拼装方排序。
 *
 * @param id           留痕行标识（TSID 十进制字符串）
 * @param nodeId       所属付款节点（行上不重复映射归属列——拼装方自节点侧带入）
 * @param action       动作（code）：1=到账核验 2=打回
 * @param actionName   动作名
 * @param fromStatus   动作前节点状态（code）
 * @param toStatus     动作后节点状态（code）
 * @param actualAmount 实到金额（分；打回为 null）
 * @param arrivedAt    实到时间（打回为 null）
 * @param voucherId    关联凭证（可空）
 * @param basis        依据/备注（打回必填；到账核验可空）
 * @param operatorId   操作者标识
 * @param operatorName 操作者名（直读展示）
 * @param createdAt    动作时间
 */
public record AiplatformPaymentVerificationWireResponse(
        String id,
        String nodeId,
        Integer action,
        String actionName,
        Integer fromStatus,
        Integer toStatus,
        Long actualAmount,
        LocalDateTime arrivedAt,
        String voucherId,
        String basis,
        String operatorId,
        String operatorName,
        LocalDateTime createdAt
) {
}
