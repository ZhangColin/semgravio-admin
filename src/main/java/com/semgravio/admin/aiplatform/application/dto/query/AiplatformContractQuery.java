package com.semgravio.admin.aiplatform.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 平台合同清单检索（北向查询，issue #104 镜像 provider aiplatform #336）——四维可组合、
 * 均可缺省（缺省＝全量）：状态多选/创建时间区间/订单号精确/合同号精确。
 *
 * <p>与订单检索（{@code AiplatformOrderQuery}）的差异：第四维是 {@code contractNo} 合同号
 * （SG- 串）而非 externalId 下单账号。BFF 零校验零推导——未知状态码/非数值订单号由
 * provider 裁决（绑定层 400 或空清单 200），照实透传。</p>
 *
 * @param status      合同状态多选（Integer code：1=准备中 2=待核对与签署 3=签署进行中
 *                    4=签署完成 5=签署失败或过期 6=已撤回或作废）
 * @param createdFrom 创建时间区间起（含，ISO-8601）
 * @param createdTo   创建时间区间止（含，ISO-8601）
 * @param orderId     所属订单标识精确（TSID 十进制；非数值→provider 判空清单 200）
 * @param contractNo  合同号精确（SG- 串）
 * @since 0.1.0
 */
public record AiplatformContractQuery(
        List<Integer> status,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        String orderId,
        String contractNo
) {
}
