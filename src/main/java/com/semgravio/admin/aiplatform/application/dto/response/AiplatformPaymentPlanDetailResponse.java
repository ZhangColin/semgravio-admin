package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台后台付款计划详情——北向出口，逐字镜像 aiplatform
 * {@code BackofficePaymentPlanDetailResponse}（#337 核验承接全量面），无增删字段、
 * 无换型：活跃计划全量（节点五态/凭证清单/核验留痕时间线）＋历史计划行摘要（旧计划
 * 随旧合同留痕——撤回重建/范围重冻后旧行定格可见）。
 *
 * @since 0.1.0
 */
public record AiplatformPaymentPlanDetailResponse(

        /** 活跃计划（订单名下最新行——用户面同构响应） */
        AiplatformPaymentPlanResponse plan,

        /** 核验留痕时间线（新 → 旧，跨节点全量——到账核验/打回各一行 append-only） */
        List<AiplatformPaymentVerificationResponse> trails,

        /** 历史计划行摘要（新 → 旧，不含活跃行——节点细节不在本面） */
        List<HistoryPlanResponse> history
) {

    /** 历史计划行摘要（留痕面——行身份与冻结集指向足矣，节点细节归活跃计划）。 */
    public record HistoryPlanResponse(

            /** 计划标识（TSID 十进制字符串） */
            String id,

            /** 创建时点的合同（TSID 十进制字符串） */
            String contractId,

            /** 合同号定格 */
            String contractNo,

            /** 节点数（旧计划节点定格保留） */
            int nodeCount,

            /** 计划总额（分） */
            Long totalAmount,

            /** 累计已核到账（分） */
            Long totalSettled,

            /** 创建时间（＝合同冻结同刻） */
            LocalDateTime createdAt
    ) {
    }
}
