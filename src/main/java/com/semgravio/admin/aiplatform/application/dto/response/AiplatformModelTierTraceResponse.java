package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型档位变更留痕读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeModelTierTraceResponse}（#367 档位留痕，issue #111 B2，spec #101 B 批）：
 * 一次实际变更的前后全量值快照＋操作者＋时刻，按时间倒序（最近先）。
 *
 * <p>append-only——留痕不随任何动作删除（历史事实跨行存活：行删除后留痕仍可读，创建痕
 * old 四件全 null、删除痕 new 四件全 null；留痕读面不要求行存在——未知键空清单 200 非
 * 404，空清单＝从未有行，行读面负责 404 判别）。回滚＝把某痕旧值快照写回（行在＝PUT、
 * 行已删＝按旧值重建行，皆一次新变更留新痕，不做版本树）；同值幂等重写不落痕。
 * {@code newDisplayName} null＝本次清空回落语义键（终态替换语义）。{@code oldEnabled/
 * newEnabled} 为 Boolean 装箱（创建痕/删除痕对应侧 null），admin 同型镜像。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelTierTraceResponse(

        /** 留痕标识（TSID 十进制串） */
        @Schema(description = "留痕标识（TSID 十进制串）")
        String id,

        /** 变更前所属通道键（null＝创建痕——行此前不存在） */
        @Schema(description = "变更前所属通道键（null＝创建痕——行此前不存在）", example = "volcengine")
        String oldProviderKey,

        /** 变更前实际模型名（null＝创建痕） */
        @Schema(description = "变更前实际模型名（null＝创建痕）")
        String oldModelName,

        /** 变更前显示名（null＝此前无显示名或创建痕） */
        @Schema(description = "变更前显示名（null＝此前无显示名或创建痕）", example = "快模型档")
        String oldDisplayName,

        /** 变更前启用态（null＝创建痕） */
        @Schema(description = "变更前启用态（null＝创建痕）")
        Boolean oldEnabled,

        /** 变更后所属通道键（null＝删除痕——行已不存在） */
        @Schema(description = "变更后所属通道键（null＝删除痕——行已不存在）", example = "volcengine")
        String newProviderKey,

        /** 变更后实际模型名（null＝删除痕） */
        @Schema(description = "变更后实际模型名（null＝删除痕）")
        String newModelName,

        /** 变更后显示名（null＝本次清空回落语义键或删除痕） */
        @Schema(description = "变更后显示名（null＝本次清空回落语义键或删除痕）")
        String newDisplayName,

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
