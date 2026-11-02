package com.semgravio.admin.aiplatform.application.dto.command;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台到账核验录入北向命令（issue #106，spec #102 A 批 A3）——逐字镜像 aiplatform
 * {@code VerifyPaymentCommand}（#337 凭证核验路径第二跳·后台人工录入）：实到金额/
 * 实到时间/关联凭证/渠道流水号随行 append-only 留痕。<strong>全额判定归 provider</strong>
 * （累计核到账 ≥ 节点金额：足额转「已到账」receivedAt＝实到时间；不足留痕＋回「待支付」
 * 累计不清零）——BFF 不复算不预判。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性
 * （实到金额须为正/实到时间必填 ORD_041、仅线下转账待核验态可达 ORD_038、关联凭证须属
 * 该节点 ORD_045、操作者必携 ORD_043）由 provider 聚合守卫裁决，错误信封原样透传。</p>
 *
 * @since 0.1.0
 */
public record AiplatformPaymentVerifyCommand(

        /** 实到金额（分，正数，必填——非正/缺失归 provider ORD_041） */
        @Schema(description = "实到金额（分，正数，必填——非正由 provider 裁决 ORD_041）。全额判定"
                + "＝累计核到账 ≥ 节点金额：足额转「已到账」、不足留痕回「待支付」（累计不清零）",
                example = "1950000")
        Long actualAmount,

        /** 实到时间（业务时间，必填，ISO-8601；足额转已到账时 receivedAt 落本值） */
        @Schema(description = "实到时间（业务时间，必填，ISO-8601；足额转「已到账」时点的正身）",
                example = "2026-10-17T10:00:00")
        LocalDateTime arrivedAt,

        /** 关联凭证（可空——凭证行身份软引用；非空须存在且属该节点 ORD_045） */
        @Schema(description = "关联凭证（可空——软引用凭证行身份；非空须存在且属该节点，否则 provider"
                + " 裁决 ORD_045）", example = "3829497000000001")
        String voucherId,

        /** 渠道流水号（可空；核验录入时落值——渠道接口位） */
        @Schema(description = "渠道流水号（可空；核验录入时落值——渠道接口位）",
                example = "BANK-20261017-0001")
        String channelRefNo,

        /** 备注（可空，至多 1000 字——随行留痕） */
        @Schema(description = "备注（可空，至多 1000 字——随核验留痕行落）", example = "银企对账一致")
        String basis
) {
}
