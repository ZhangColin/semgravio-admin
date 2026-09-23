package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置变更留痕读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeAgentConfigTraceResponse}（#251 留痕回滚，#246 技能线 T5，spec #79 定稿、
 * issue #84）：一次实际变更的前后全量值快照（old&#42;/new&#42; 四件对）＋操作者＋时刻。
 *
 * <p>每痕＝一次实际变更（同值幂等重写不落痕——时间线里没有假变更）；按时间倒序（最近先，
 * provider 定死排序）。append-only——留痕不随任何动作删除（历史事实不随配置行消失）。
 * 回滚＝把某痕旧值快照经 PUT 写回（即一次新变更、留新痕，不做版本树）——old&#42; 四件即
 * 写回依据。old&#42;/new&#42; 的 prompt/档位两件可空（null＝该侧即缺省回落），开关四件 provider
 * 恒在场（原生 boolean，admin 同型镜像）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigTraceResponse(

        /** 留痕标识（TSID 十进制串） */
        @Schema(description = "留痕标识（TSID 十进制串）")
        String id,

        /** 变更前 systemPrompt（null＝此前即缺省回落——回滚即把此值写回） */
        @Schema(description = "变更前 systemPrompt（null＝此前即缺省回落——回滚即把此值写回）")
        String oldSystemPrompt,

        /** 变更前模型档位（null＝此前即缺省回落） */
        @Schema(description = "变更前模型档位（null＝此前即缺省回落）")
        String oldModelId,

        /** 变更前联网搜索开关 */
        @Schema(description = "变更前联网搜索开关")
        boolean oldWebSearchEnabled,

        /** 变更前网页抓取开关 */
        @Schema(description = "变更前网页抓取开关")
        boolean oldFetchUrlEnabled,

        /** 变更后 systemPrompt（null＝本次清空回落枚举默认） */
        @Schema(description = "变更后 systemPrompt（null＝本次清空回落枚举默认）")
        String newSystemPrompt,

        /** 变更后模型档位（null＝本次清空回落） */
        @Schema(description = "变更后模型档位（null＝本次清空回落）")
        String newModelId,

        /** 变更后联网搜索开关 */
        @Schema(description = "变更后联网搜索开关")
        boolean newWebSearchEnabled,

        /** 变更后网页抓取开关 */
        @Schema(description = "变更后网页抓取开关")
        boolean newFetchUrlEnabled,

        /** 变更操作者 id */
        @Schema(description = "变更操作者 id", example = "700200")
        String operatorId,

        /** 变更操作者名（直读展示） */
        @Schema(description = "变更操作者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 变更动作时刻（留痕生成时刻；读面按此时间倒序——最近先） */
        @Schema(description = "变更动作时刻（留痕生成时刻；读面按此时间倒序——最近先）")
        LocalDateTime operatedAt
) {
}
