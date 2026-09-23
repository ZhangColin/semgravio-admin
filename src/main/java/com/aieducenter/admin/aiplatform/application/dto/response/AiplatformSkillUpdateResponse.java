package com.aieducenter.admin.aiplatform.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能包显式更新回执——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSkillUpdateResponse}（#250 显式更新，spec #79 定稿、issue #83）：一次
 * 「按来源包整体重拉快照入库」的结果确认面——from→to 版本＋操作者＋更新后该包终态行集＋
 * 被移除技能名（确认更新动了什么，对齐卸载回执「确认移除了什么」的口径）。
 *
 * <p>远端未前进时 from==to、removedSkillNames 为空、行集即原样（不落痕不写行——幂等回执，
 * 历史里没有噪音）。{@code skills} 与技能清单行共形（同名行原地翻新保留 id/状态/指派——
 * TSID 柄稳定；新技能插入启用；远端已删行移除——移除有指派在身的技能整体拒绝
 * 409 SKL_013＝数字业务码 7013，先解绑再更新）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillUpdateResponse(

        /** 来源包标识（规范化仓库地址） */
        @Schema(description = "来源包标识（规范化仓库地址）")
        String sourcePackage,

        /** 更新前版本（装时/上次更新 commit；远端未前进时与 toVersion 相等——幂等回执） */
        @Schema(description = "更新前版本（装时/上次更新 commit；远端未前进时与 toVersion 相等——幂等回执）")
        String fromVersion,

        /** 更新后版本（更新时刻远端 HEAD commit——快照锚翻新） */
        @Schema(description = "更新后版本（更新时刻远端 HEAD commit——快照锚翻新）")
        String toVersion,

        /** 本次更新移除的技能名（远端已删且无指派在身的行；有指派在身的移除整体拒绝 SKL_013，不至此；无即空） */
        @Schema(description = "本次更新移除的技能名（远端已删且无指派在身的行；有指派在身的"
                + "移除会被整体拒绝 SKL_013，不至此）")
        List<String> removedSkillNames,

        /** 更新操作者 id（显式动作必留痕） */
        @Schema(description = "更新操作者 id（显式动作必留痕）", example = "700200")
        String operatorId,

        /** 更新操作者名（直读展示） */
        @Schema(description = "更新操作者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 更新后该来源包全部条目（清单行同形；同名行原地翻新保留 id/状态/指派，新增行插入、消失行移除） */
        @Schema(description = "更新后该来源包全部条目（清单行同形；同名行原地翻新保留 id/状态/指派，"
                + "新增行插入、消失行移除）")
        List<AiplatformSkillSummaryResponse> skills
) {
}
