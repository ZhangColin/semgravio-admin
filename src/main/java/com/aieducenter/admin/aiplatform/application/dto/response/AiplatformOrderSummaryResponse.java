package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台订单清单条目——北向出口，逐字镜像 aiplatform {@code BackofficeOrderSummaryResponse}
 * （#156 四维检索＋#297/#298 交付物类型字段对），无增删字段、无换型（spec #62 忠实透传）。
 *
 * <p>类型口径：{@code id}/{@code projectId} 为 String（TSID 十进制串）；{@code status} 为
 * Integer code + {@code statusName} 中文名（前端直读 *Name）；{@code deliverableType} 为
 * Integer code + {@code deliverableTypeName} 中文名（下单冻结自项目终点类型，取件端点按类型
 * 二选一——见 description）；{@code amount} 为 Long（分）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformOrderSummaryResponse(

        /** 订单标识（TSID 十进制字符串） */
        String id,

        /** 所属项目标识（TSID 十进制字符串） */
        String projectId,

        /** 项目名（软引用缺档为 null） */
        String projectName,

        /** 下单账号对外正身（OIDC sub——账号档案读口的寻址键；下单账号可空/缺档为 null） */
        String ownerExternalId,

        /** 下单用户显示名（下单账号可空或缺档为 null） */
        String ownerDisplayName,

        /** 订单状态（1=待报价 2=已报价 3=已支付 4=已归档 5=已取消） */
        Integer status,

        /** 状态中文名（provider 出口提供） */
        String statusName,

        /** 交付物类型（1=设计 2=系统 3=系统＋设计；下单冻结自项目终点类型，此后不随项目变更——取件二选一：设计面走 /{id}/design-package、系统单走 /{id}/source-package） */
        @Schema(description = "交付物类型 code（1=设计 2=系统 3=系统＋设计；下单冻结自项目终点类型，"
                + "此后不随项目变更——下载按钮按类型二选一：设计面走 /{id}/design-package、"
                + "系统单走 /{id}/source-package）", example = "3")
        Integer deliverableType,

        /** 交付物类型中文名（前端直读展示） */
        String deliverableTypeName,

        /** 当前总价（分；待报价 NULL） */
        Long amount,

        /** 币种（v1 恒 CNY；待报价 NULL） */
        String currency,

        /** 下单时间 */
        LocalDateTime createdAt,

        /** 首次报价时点（改价不刷新；待报价 NULL） */
        LocalDateTime quotedAt
) {
}
