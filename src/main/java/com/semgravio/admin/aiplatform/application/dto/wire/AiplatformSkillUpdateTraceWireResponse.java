package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 更新留痕读面行的 wire 镜像——与 aiplatform
 * {@code BackofficeSkillUpdateTraceResponse}（#250 历史版本可查）字段同构：一次显式更新的
 * from→to 版本＋操作者＋时刻。append-only——卸载不删留痕（历史事实不随库行消失）；按时间
 * 倒序（最近先），装时版本经链条首个 from 可回溯。
 *
 * @param sourcePackage 来源包标识（规范化仓库地址）
 * @param fromVersion   更新前版本（装时/上次更新 commit）
 * @param toVersion     更新后版本（更新时刻远端 HEAD commit）
 * @param operatorId    更新操作者 id（显式动作必留痕）
 * @param operatorName  更新操作者名（直读展示）
 * @param operatedAt    更新动作时刻（留痕生成时刻）
 */
public record AiplatformSkillUpdateTraceWireResponse(
        String sourcePackage,
        String fromVersion,
        String toVersion,
        String operatorId,
        String operatorName,
        LocalDateTime operatedAt
) {
}
