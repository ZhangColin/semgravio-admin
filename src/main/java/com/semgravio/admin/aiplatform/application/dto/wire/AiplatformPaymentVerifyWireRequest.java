package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 到账核验录入命令体的 wire 镜像——与 aiplatform
 * {@code VerifyPaymentCommand}（#337 后台机机面）字段同构：实到金额/实到时间/
 * 关联凭证随行留痕，渠道流水号可空落值（渠道接口位）。
 *
 * <p>字段合法性（录入无效 ORD_041、状态守卫 ORD_038、凭证归属 ORD_045、操作者
 * 必携 ORD_043）归 provider 聚合守卫，BFF 不代判不代填。actualAmount 为 Long
 * （分）——生产 mapper 出 JSON string，provider 默认可回读。</p>
 *
 * @param actualAmount 实到金额（分，正数，必填）
 * @param arrivedAt    实到时间（业务时间，必填，ISO-8601）
 * @param voucherId    关联凭证（可空——凭证行身份软引用）
 * @param channelRefNo 渠道流水号（可空；核验录入时落值）
 * @param basis        备注（可空，至多 1000 字）
 */
public record AiplatformPaymentVerifyWireRequest(
        Long actualAmount,
        LocalDateTime arrivedAt,
        String voucherId,
        String channelRefNo,
        String basis
) {
}
