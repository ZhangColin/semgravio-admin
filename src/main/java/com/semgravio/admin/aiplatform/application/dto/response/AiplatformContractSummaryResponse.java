package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同清单条目——北向出口，逐字镜像 aiplatform
 * {@code BackofficeContractSummaryResponse}（#336 合同域六态承接），无增删字段、
 * 无换型（spec #102 忠实透传）。
 *
 * <p>类型口径：{@code id}/{@code orderId} String（TSID 十进制串）、{@code contractNo}
 * SG- 串；{@code status} Integer code + {@code statusName} 中文名（前端直读 *Name）；
 * {@code version} int；{@code amount} Long（分）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractSummaryResponse(

        /** 合同标识（TSID 十进制字符串） */
        String id,

        /** 合同编号（SG- 风格，全局唯一——SG-＋TSID base36，创建即定格） */
        String contractNo,

        /** 所属订单标识（TSID 十进制字符串） */
        String orderId,

        /**
         * 合同状态（六态，人工推进单向：1→2→3→4，5/6 旁落终态）：1=准备中
         * 2=待核对与签署 3=签署进行中 4=签署完成 5=签署失败或过期 6=已撤回或作废。
         * Integer code 直通不建 typed enum（provider 枚举非本应用领域枚举）。
         */
        @Schema(description = "合同状态 code（六态人工推进：1=准备中 2=待核对与签署 3=签署进行中 "
                + "4=签署完成 5=签署失败或过期 6=已撤回或作废——终态 4/5/6 永不物理删除、"
                + "撤回/作废后经创建重建口换新号新合同）", example = "3")
        Integer status,

        /** 状态中文名（provider 出口提供，前端直读展示） */
        String statusName,

        /** 合同版本（草稿同号重录 +1；重建从 1 起） */
        int version,

        /** 冻结金额（分，随冻结价目行定格；改价后待重录重新定格） */
        Long amount,

        /** 币种（v1 恒 CNY） */
        String currency,

        /** 创建时间（确认范围/重建时点） */
        LocalDateTime createdAt
) {
}
