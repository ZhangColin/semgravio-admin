package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型供应商通道变更留痕读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeModelProviderTraceResponse}（#367 通道留痕，issue #110 B1，spec
 * #101 B 批）：一次实际变更的前后全量值快照＋操作者＋时刻，按时间倒序（最近先）。
 *
 * <p>append-only——留痕不随任何动作删除（历史事实跨行存活：行删除后留痕仍可读，创建痕
 * old 全 null、删除痕 new 全 null）。回滚＝把旧值快照写回（即一次新变更、留新痕）；
 * 同值幂等重写不落痕。<strong>apiKey 快照读面一律掩码</strong>（尾 4 位——留痕不是密钥
 * 找回面，掩码尾 4 足以比对「哪次换的哪把 key」；回滚换 key＝操作者重输全量新值）——
 * 掩码由 provider 单点完成，admin 忠实透传掩码值不二次处理。{@code oldEnabled/newEnabled}
 * 为 Boolean 装箱（创建痕/删除痕对应侧 null），admin 同型镜像。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelProviderTraceResponse(

        /** 留痕标识（TSID 十进制串） */
        @Schema(description = "留痕标识（TSID 十进制串）")
        String id,

        /** 变更前 baseUrl（null＝创建痕——行此前不存在） */
        @Schema(description = "变更前 baseUrl（null＝创建痕——行此前不存在）")
        String oldBaseUrl,

        /** 变更前 apiKey 掩码（尾 4 位；null＝此前未配置或创建痕） */
        @Schema(description = "变更前 apiKey 掩码（尾 4 位；null＝此前未配置或创建痕）")
        String oldApiKeyMasked,

        /** 变更前启用态（null＝创建痕） */
        @Schema(description = "变更前启用态（null＝创建痕）")
        Boolean oldEnabled,

        /** 变更后 baseUrl（null＝删除痕——行已不存在） */
        @Schema(description = "变更后 baseUrl（null＝删除痕——行已不存在）")
        String newBaseUrl,

        /** 变更后 apiKey 掩码（尾 4 位；null＝删除痕或未配置） */
        @Schema(description = "变更后 apiKey 掩码（尾 4 位；null＝删除痕或未配置）")
        String newApiKeyMasked,

        /** 变更后启用态（null＝删除痕） */
        @Schema(description = "变更后启用态（null＝删除痕）")
        Boolean newEnabled,

        /** 变更操作者 id */
        @Schema(description = "变更操作者 id", example = "700210")
        String operatorId,

        /** 变更操作者名（直读展示） */
        @Schema(description = "变更操作者名（直读展示）", example = "运营·智能体管理员")
        String operatorName,

        /** 变更动作时刻（留痕生成时刻） */
        @Schema(description = "变更动作时刻（留痕生成时刻）")
        LocalDateTime operatedAt
) {
}
