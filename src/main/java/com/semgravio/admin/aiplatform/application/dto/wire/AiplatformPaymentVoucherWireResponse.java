package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 转账凭证元数据行 wire 响应——与 aiplatform {@code PaymentVoucherResponse}
 * （#337）字段同构：上传仅代表已提交（核验结论在留痕行），字节不在读面（后台
 * 点看走内容端点）。
 *
 * @param id          凭证标识（TSID 十进制字符串）
 * @param nodeId      所属付款节点
 * @param fileName    净化后原始文件名（回执单/水单人读）
 * @param contentType MIME（application/pdf | image/png | image/jpeg）
 * @param byteSize    字节数
 * @param createdAt   上传时间
 */
public record AiplatformPaymentVoucherWireResponse(
        String id,
        String nodeId,
        String fileName,
        String contentType,
        Long byteSize,
        LocalDateTime createdAt
) {
}
