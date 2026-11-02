package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台付款打回北向命令（issue #106，spec #102 A 批 A3）——逐字镜像 aiplatform
 * {@code RejectPaymentCommand}（#337 凭证核验族动作——凭证主张被否）：回「待支付」＋
 * <strong>依据必填留痕</strong>（ORD_042——打回依据是审计正身）。打回不动累计已核到账
 * （核到账只经核验录入累积）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性
 * （依据必填/至多 1000 字 ORD_042、仅线下转账待核验态可达 ORD_038、关联凭证须属该节点
 * ORD_045、操作者必携 ORD_043）由 provider 聚合守卫裁决，错误信封原样透传——BFF 不
 * 前置拦截依据必填。</p>
 *
 * @since 0.1.0
 */
public record AiplatformPaymentRejectCommand(

        /** 关联凭证（可空——同核验校验 ORD_045） */
        @Schema(description = "关联凭证（可空——非空须存在且属该节点，否则 provider 裁决 ORD_045）",
                example = "3829497000000001")
        String voucherId,

        /** 打回依据（必填自由文本，至多 1000 字——打回留痕行的审计正身） */
        @Schema(description = "打回依据（必填自由文本，至多 1000 字——打回是审计正身动作，缺失/超长由"
                + " provider 裁决 ORD_042）", example = "回单模糊无法对账，请重新上传")
        String basis
) {
}
