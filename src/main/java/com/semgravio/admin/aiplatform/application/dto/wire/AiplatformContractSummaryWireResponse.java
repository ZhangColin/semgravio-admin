package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 合同清单条目的 wire 镜像——与 aiplatform
 * {@code BackofficeContractSummaryResponse}（#336 已冻结）字段同构、字段序一致。
 * 用于 Jackson 反序列化 {@link com.cartisan.openapi.client.OpenApiClient} 响应，
 * 不与 aiplatform 内部 DTO 耦合。
 *
 * <p>类型口径（同订单域先例）：{@code id}/{@code orderId} 为 <strong>String</strong>
 * （TSID 十进制串）、{@code contractNo} 为 SG- 串；{@code status} 为
 * <strong>Integer code</strong> + {@code statusName} 中文名（六态，provider 已配对，
 * BFF 透传不做 code→中文映射）；{@code version} primitive int（JSON number）；
 * {@code amount} 为 <strong>Long（分）</strong>（JSON string——全局 Long→ToStringSerializer
 * 出口口径），同型零换算。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractSummaryWireResponse(

        /** 合同标识（TSID 十进制字符串） */
        String id,

        /** 合同编号（SG- 风格，全局唯一——SG-＋TSID base36，创建即定格） */
        String contractNo,

        /** 所属订单标识（TSID 十进制字符串） */
        String orderId,

        /** 合同状态（1=准备中 2=待核对与签署 3=签署进行中 4=签署完成 5=签署失败或过期 6=已撤回或作废） */
        Integer status,

        /** 状态中文名（provider 出口提供） */
        String statusName,

        /** 合同版本（草稿同号重录 +1；重建从 1 起） */
        int version,

        /** 冻结金额（分，随冻结价目行定格） */
        Long amount,

        /** 币种（v1 恒 CNY） */
        String currency,

        /** 创建时间（确认范围/重建时点） */
        LocalDateTime createdAt
) {
}
