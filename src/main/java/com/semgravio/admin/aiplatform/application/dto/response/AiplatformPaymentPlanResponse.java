package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 平台付款计划——北向出口，逐字镜像 aiplatform {@code PaymentPlanResponse}
 * （#337：用户面付款面板＋后台核验承接共用同构），无增删字段、无换型（spec #102
 * 忠实透传）。三写口（节点录入/到账核验/打回）回执与详情 {@code plan} 段共用本形。
 *
 * <p>类型口径：五态/渠道位 Integer code 直通不建 typed enum（provider 枚举非本应用
 * 领域枚举，对齐合同六态先例）＋{@code *Name} 中文名随行；金额类 Long（分）北向出
 * JSON string（全局出口口径）；{@code seq} primitive int。</p>
 *
 * @since 0.1.0
 */
public record AiplatformPaymentPlanResponse(

        /** 计划标识（TSID 十进制字符串） */
        String id,

        /** 所属订单标识（TSID 十进制字符串） */
        String orderId,

        /** 创建时点的合同（TSID 十进制字符串——随合同冻结同刻创建） */
        String contractId,

        /** 合同号定格（SG- 串，附言码正身源） */
        String contractNo,

        /** 计划总额（分＝节点金额合计；未录入节点为 0） */
        Long totalAmount,

        /** 累计已核到账（分——只经核验录入累积，打回不动） */
        Long totalSettled,

        /** 创建时间（＝合同冻结同刻） */
        LocalDateTime createdAt,

        /** 付款节点（录入序；空表＝待后台录入） */
        List<AiplatformPaymentNodeResponse> nodes
) {
}
