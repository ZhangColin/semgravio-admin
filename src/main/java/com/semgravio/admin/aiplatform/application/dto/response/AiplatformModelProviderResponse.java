package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型供应商通道读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeModelProviderResponse}（#367 通道维护，issue #110 B1，spec #101
 * B 批）：GET 清单/单行、POST 创建、PUT 更新回执同形（单对象行）。
 *
 * <p><strong>apiKey 一律掩码回显</strong>（{@code apiKeyMasked} 尾 4 位；null＝未配置
 * ——接口回显面零明文 key）。掩码由 provider 单点完成（{@code ModelProvider#maskApiKey}），
 * admin <strong>忠实透传掩码值</strong>——不二次处理、不尝试解掩（spec #101：写口收
 * 全量原样出站，读与留痕一律掩码形照抄）。换通道、换 key、换 baseUrl 皆不发版不重启
 * ——下一轮命令构建即生效（模型实例缓存键含通道指纹）。{@code enabled} 为原生
 * boolean（provider 恒在场），admin 同型镜像。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelProviderResponse(

        /** 通道键名＝协议路由前缀 */
        @Schema(description = "通道键名＝协议路由前缀", example = "volcengine")
        String providerKey,

        /** 通道 baseUrl（创建上下文注入 SDK） */
        @Schema(description = "通道 baseUrl（创建上下文注入 SDK）",
                example = "https://ark.cn-beijing.volces.com/api/v3")
        String baseUrl,

        /** apiKey 掩码形（尾 4 位；null＝未配置——接口回显零明文 key，admin 忠实透传不二次处理） */
        @Schema(description = "apiKey 掩码形（尾 4 位；null＝未配置——provider 已掩码、admin "
                + "忠实透传不二次处理不尝试解掩）", example = "****ab12")
        String apiKeyMasked,

        /** 启用态（停用＝引用该通道的装配明确报错，不静默换道） */
        @Schema(description = "启用态（停用＝引用该通道的装配明确报错，不静默换道）")
        boolean enabled,

        /** 最近写者 id（null＝种子/迁移写入） */
        @Schema(description = "最近写者 id（admin 侧 TSID 十进制串；null＝种子/迁移写入）",
                example = "700210")
        String operatorId,

        /** 最近写者名（直读展示） */
        @Schema(description = "最近写者名（直读展示）", example = "运营·智能体管理员")
        String operatorName,

        /** 最近写入时刻 */
        @Schema(description = "最近写入时刻")
        LocalDateTime updatedAt
) {
}
