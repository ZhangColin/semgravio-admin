package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台生产承接——北向出口，逐字镜像 aiplatform
 * {@code BackofficeProductionHandoffResponse}（#338 承接全量面），无增删字段、无换型：
 * 承接字段（交付联系人/下一步说明/进展更新/预计开始时间——可空段如实 null，呈现层
 * 「仅真实值显示」）＋两态＋动作留痕时间线（新 → 旧，append-only 全量——动作/两侧
 * 状态/依据/操作者/时间；创建行＝开工判定系统操作者）。
 *
 * <p>类型口径：两态/动作 Integer code 直通不建 typed enum（provider 枚举非本应用
 * 领域枚举，对齐合同六态/付款五态先例）＋{@code *Name} 中文名随行。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProductionHandoffResponse(

        /** 承接标识（TSID 十进制字符串） */
        String id,

        /** 挂靠项目（承接挂项目唯一——全生命周期一行） */
        String projectId,

        /** 来源订单（TSID 十进制字符串） */
        String orderId,

        /** 承接状态（code）：1=待安排生产 2=生产中 */
        @Schema(description = "承接状态（code）：1=待安排生产 2=生产中（两态单向推进，"
                + "无终态概念——承接是交付过程的状态承接页）", example = "1")
        Integer status,

        /** 状态名 */
        String statusName,

        /** 交付联系人（可空，至多 200 字） */
        String contact,

        /** 下一步说明（可空，至多 1000 字，人工措辞） */
        String nextStep,

        /** 进展更新（可空，至多 2000 字，人工措辞、不上工程术语） */
        String progressNote,

        /** 预计开始时间（可空＝待安排——仅录入真实值才显示） */
        LocalDateTime estimatedStartAt,

        /** 创建时间（＝开工判定触发建档时点） */
        LocalDateTime createdAt,

        /** 动作留痕时间线（新 → 旧，append-only 全量） */
        List<TransitionResponse> transitions
) {

    /**
     * 留痕行（新 → 旧单行）：动作/两侧状态/依据/操作者/时间——非迁移类（创建/信息
     * 更新）fromStatus = toStatus（在场快照），迁移类（推进）如实记录两侧。
     */
    public record TransitionResponse(

            /** 动作（code）：1=创建 2=推进 3=信息更新 */
            @Schema(description = "动作（code）：1=创建 2=推进 3=信息更新——创建行由开工"
                    + "判定系统触发落（operatorId=system）", example = "1")
            Integer action,

            /** 动作名 */
            String actionName,

            /** 动作前状态（code） */
            Integer fromStatus,

            /** 动作前状态名 */
            String fromStatusName,

            /** 动作后状态（code） */
            Integer toStatus,

            /** 动作后状态名 */
            String toStatusName,

            /** 依据（必填，至多 1000 字——append-only 留痕行的正身） */
            String basis,

            /** 操作者标识（创建行＝system；后台动作＝admin 管理员 id） */
            String operatorId,

            /** 操作者名（创建行＝开工判定；后台动作＝admin 管理员昵称） */
            String operatorName,

            /** 留痕时间 */
            LocalDateTime createdAt
    ) {
    }
}
