package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同动作留痕行——北向出口，逐字镜像 aiplatform {@code ContractTransitionResponse}
 * （#336），无增删字段、无换型。后台详情时间线条目（新→旧，append-only 全量）。
 *
 * @since 0.1.0
 */
public record AiplatformContractTransitionResponse(

        /**
         * 动作 code：1=推进 2=撤回 3=作废 4=签署标记 5=草稿重录 6=失败或过期。
         * 迁移类（推进/撤回/作废/失败或过期）如实记录两侧状态；非迁移类
         * （签署标记/草稿重录）from = to（在场快照）。
         */
        @Schema(description = "动作 code（1=推进 2=撤回 3=作废 4=签署标记 5=草稿重录 6=失败或过期"
                + "——非迁移动作 fromStatus = toStatus）", example = "1")
        Integer action,

        /** 动作名（provider 出口提供，前端直读展示） */
        String actionName,

        /** 动作前状态（code，六态；非迁移动作 from = to） */
        Integer fromStatus,

        /** 动作前状态名 */
        String fromStatusName,

        /** 动作后状态（code，六态） */
        Integer toStatus,

        /** 动作后状态名 */
        String toStatusName,

        /** 依据（必填自由文本，人工动作留痕——推进/失败/撤回/作废/签署/重录均必填） */
        String basis,

        /** 操作者标识（admin 侧管理员 id，经 X-User-Id 透传头落痕——合同动作操作者必填） */
        String operatorId,

        /** 操作者名（直读展示） */
        String operatorName,

        /** 动作时间 */
        LocalDateTime createdAt
) {
}
