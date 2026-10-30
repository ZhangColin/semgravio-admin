package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 显式更新回执的 wire 镜像——与 aiplatform {@code BackofficeSkillUpdateResponse}
 * （#250 显式更新）字段同构：一次「重拉快照入库」的结果确认面——from→to 版本＋操作者＋
 * 更新后该包终态行集＋被移除技能名（确认更新动了什么，对齐卸载回执「确认移除了什么」
 * 的口径）。
 *
 * <p>远端未前进时 from==to、removedSkillNames 为空、行集即原样（无留痕追加——幂等回执）。
 * {@code skills} 与清单行共形（同名行原地翻新保留 id/状态/指派——TSID 柄稳定；新增行插入
 * 启用；消失行移除）。</p>
 *
 * @param sourcePackage     来源包标识（规范化仓库地址）
 * @param fromVersion       更新前版本（装时/上次更新 commit；远端未前进时与 to 相等）
 * @param toVersion         更新后版本（更新时刻远端 HEAD commit）
 * @param removedSkillNames 本次更新移除的技能名（远端已删、无指派在身的行；无即空）
 * @param operatorId        更新操作者 id（显式动作必留痕）
 * @param operatorName      更新操作者名（直读展示）
 * @param skills            更新后该来源包全部条目（清单行同形，名称序）
 */
public record AiplatformSkillUpdateWireResponse(
        String sourcePackage,
        String fromVersion,
        String toVersion,
        List<String> removedSkillNames,
        String operatorId,
        String operatorName,
        List<AiplatformSkillSummaryWireResponse> skills
) {
}
