package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 付款计划 wire 响应——与 aiplatform {@code PaymentPlanResponse}
 * （#337 用户面付款面板＋后台核验承接共用同构）字段同构：计划身份/合同冻结
 * 同刻信息/总额与累计已核到账/节点清单（录入序）。三写口（节点录入/到账核验/
 * 打回）回执与详情 {@code plan} 段共用本形。
 *
 * <p>金额类（totalAmount/totalSettled）provider 出 JSON string（cartisan-web
 * 全局 Long→ToStringSerializer，admin 同口径回读）；五态/渠道位 Integer code
 * 直通（对齐合同六态先例，不建 typed enum）。</p>
 *
 * @param id           计划标识（TSID 十进制字符串）
 * @param orderId      所属订单
 * @param contractId   创建时点的合同
 * @param contractNo   合同号定格（附言码正身源）
 * @param totalAmount  计划总额（分＝节点金额合计；未录入节点为 0）
 * @param totalSettled 累计已核到账（分）
 * @param createdAt    创建时间（＝合同冻结同刻）
 * @param nodes        付款节点（录入序；空表＝待后台录入）
 */
public record AiplatformPaymentPlanWireResponse(
        String id,
        String orderId,
        String contractId,
        String contractNo,
        Long totalAmount,
        Long totalSettled,
        LocalDateTime createdAt,
        List<AiplatformPaymentNodeWireResponse> nodes
) {
}
