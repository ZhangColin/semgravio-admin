package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能包更新留痕读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSkillUpdateTraceResponse}（#250 历史版本可查，spec #79 定稿、issue #83）：
 * 一次显式更新的 from→to 版本＋操作者＋时刻。
 *
 * <p>append-only——卸载不删留痕（历史事实不随库行消失）；按时间倒序（最近先，provider
 * 定死排序），装时版本经链条首个 from 可回溯。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillUpdateTraceResponse(

        /** 来源包标识（规范化仓库地址） */
        @Schema(description = "来源包标识（规范化仓库地址）")
        String sourcePackage,

        /** 更新前版本（装时/上次更新 commit） */
        @Schema(description = "更新前版本（装时/上次更新 commit）")
        String fromVersion,

        /** 更新后版本（更新时刻远端 HEAD commit） */
        @Schema(description = "更新后版本（更新时刻远端 HEAD commit）")
        String toVersion,

        /** 更新操作者 id（显式动作必留痕） */
        @Schema(description = "更新操作者 id（显式动作必留痕）", example = "700200")
        String operatorId,

        /** 更新操作者名（直读展示） */
        @Schema(description = "更新操作者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 更新动作时刻（留痕生成时刻；读面按此时间倒序——最近先） */
        @Schema(description = "更新动作时刻（留痕生成时刻；读面按此时间倒序——最近先）")
        LocalDateTime operatedAt
) {
}
