package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 打回命令体的 wire 镜像——与 aiplatform {@code RejectPaymentCommand}
 * （#337 后台机机面，核验族动作——凭证主张被否）字段同构：依据必填留痕
 * （ORD_042）、回待支付。
 *
 * <p>字段合法性（依据必填/至多 1000 字 ORD_042、状态守卫 ORD_038、凭证归属
 * ORD_045、操作者必携 ORD_043）归 provider 聚合守卫，BFF 不代判不代填。</p>
 *
 * @param voucherId 关联凭证（可空）
 * @param basis     打回依据（必填自由文本，至多 1000 字）
 */
public record AiplatformPaymentRejectWireRequest(
        String voucherId,
        String basis
) {
}
