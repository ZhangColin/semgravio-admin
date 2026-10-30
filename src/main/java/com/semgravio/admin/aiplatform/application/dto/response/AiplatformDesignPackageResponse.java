package com.semgravio.admin.aiplatform.application.dto.response;

/**
 * AI 平台订单设计资产包——北向出口载体（tar.gz 二进制 + provider 响应头原值），issue #96
 * 镜像 aiplatform #298（对偶 {@link AiplatformSourcePackageResponse}）。
 *
 * <p>二进制端点无 {@code ApiResponse} 信封（spec #62）：provider 以 {@code application/gzip} +
 * {@code Content-Disposition: attachment; filename="{orderId}-design.tar.gz"} 出流，
 * {@code contentType}/{@code contentDisposition} 为 provider 响应头 <strong>raw 值</strong>
 * 原样透传（不打解析、不重构文件名）；{@code content} 为全量缓冲的原始字节（内存中转、
 * 不落盘——provider 侧本身即 byte[] 缓冲出口，经 cartisan-openapi {@code download} 保全）。
 * 语义区别于源码包：直取<strong>下单冻结时物化</strong>的设计资产包（交付物按单定格，此后
 * 改稿/再定稿不漂移），非实时打包。</p>
 *
 * @since 0.1.0
 */
public record AiplatformDesignPackageResponse(

        /** tar.gz 原始字节（gzip 字节流，不做任何字符解码） */
        byte[] content,

        /** provider 的 Content-Type raw 值（application/gzip） */
        String contentType,

        /** provider 的 Content-Disposition raw 值（attachment; filename="...-design.tar.gz"） */
        String contentDisposition
) {
}
