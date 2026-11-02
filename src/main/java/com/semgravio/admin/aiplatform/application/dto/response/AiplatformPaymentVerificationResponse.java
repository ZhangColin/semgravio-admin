package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台付款核验留痕行——北向出口，逐字镜像 aiplatform
 * {@code PaymentVerificationResponse}（#337），无增删字段、无换型：每次到账核验/
 * 打回各一行（append-only）——实到金额/实到时间/关联凭证/依据、操作人随行落；
 * 「不足留痕」「打回依据留痕」的审计正身。新 → 旧由 provider 拼装方排序。
 *
 * @since 0.1.0
 */
public record AiplatformPaymentVerificationResponse(

        /** 留痕行标识（TSID 十进制字符串） */
        String id,

        /** 所属付款节点（TSID 十进制字符串——行上不重复映射归属列，拼装方自节点侧带入） */
        String nodeId,

        /** 动作（code）：1=到账核验 2=打回——Integer code 直通不建 typed enum */
        @Schema(description = "动作 code（1=到账核验 2=打回——留痕行的动作分类位，append-only）",
                example = "1")
        Integer action,

        /** 动作中文名（前端直读展示） */
        String actionName,

        /** 动作前节点状态（code，五态） */
        Integer fromStatus,

        /** 动作后节点状态（code，五态） */
        Integer toStatus,

        /** 实到金额（分；打回为 null——打回不动累计） */
        Long actualAmount,

        /** 实到时间（打回为 null） */
        LocalDateTime arrivedAt,

        /** 关联凭证（可空） */
        String voucherId,

        /** 依据/备注（打回必填；到账核验可空） */
        String basis,

        /** 操作者标识（admin 管理员 id 经透传头落痕——核验动作以操作者为审计正身） */
        String operatorId,

        /** 操作者名（直读展示） */
        String operatorName,

        /** 动作时间 */
        LocalDateTime createdAt
) {
}
