package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

/**
 * AI 平台转账凭证元数据行——北向出口，逐字镜像 aiplatform {@code PaymentVoucherResponse}
 * （#337），无增删字段、无换型：上传仅代表已提交（核验结论在留痕行），字节不在读面
 * （后台点看走凭证内容二进制端点）。
 *
 * @since 0.1.0
 */
public record AiplatformPaymentVoucherResponse(

        /** 凭证标识（TSID 十进制字符串） */
        String id,

        /** 所属付款节点（TSID 十进制字符串） */
        String nodeId,

        /** 净化后原始文件名（回执单/水单人读） */
        String fileName,

        /** MIME（application/pdf | image/png | image/jpeg） */
        String contentType,

        /** 字节数（Long 北向出 JSON string） */
        Long byteSize,

        /** 上传时间 */
        LocalDateTime createdAt
) {
}
