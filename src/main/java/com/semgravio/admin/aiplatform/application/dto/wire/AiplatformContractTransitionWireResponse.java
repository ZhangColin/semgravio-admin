package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 合同动作留痕行的 wire 镜像——与 aiplatform {@code ContractTransitionResponse}
 * （#336 已冻结）字段同构、字段序一致。后台详情时间线条目（新→旧，append-only 全量）。
 *
 * <p>动作 {@code action} Integer code：1=推进 2=撤回 3=作废 4=签署标记 5=草稿重录
 * 6=失败或过期——非迁移动作（签署标记/草稿重录）from = to（在场快照）。合同动作操作者
 * 必填（provider ORD_035 拒无名留痕），操作者两肢恒有值。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractTransitionWireResponse(

        /** 动作（1=推进 2=撤回 3=作废 4=签署标记 5=草稿重录 6=失败或过期） */
        Integer action,

        /** 动作名（provider 出口提供） */
        String actionName,

        /** 动作前状态（code，六态；非迁移动作 from = to） */
        Integer fromStatus,

        /** 动作前状态名 */
        String fromStatusName,

        /** 动作后状态（code，六态） */
        Integer toStatus,

        /** 动作后状态名 */
        String toStatusName,

        /** 依据（必填自由文本，人工动作留痕） */
        String basis,

        /** 操作者标识（admin 侧管理员 id，X-User-Id 透传头落痕） */
        String operatorId,

        /** 操作者名（直读展示） */
        String operatorName,

        /** 动作时间 */
        LocalDateTime createdAt
) {
}
