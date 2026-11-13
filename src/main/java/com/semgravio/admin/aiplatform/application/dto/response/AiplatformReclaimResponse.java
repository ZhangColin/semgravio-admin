package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台一键回收回执——北向出口，逐字镜像 aiplatform
 * {@code BackofficeReclaimResponse}（#490，issue #129 镜像）：项目级
 * {@code POST projects/{id}/reclaim}（在飞 run 先级联停止→工作区销毁→prj_* 真删级联，
 * 受理即删——与用户面 DELETE 同内核；账户属主无会话凭证的项目不再裸操改挂属主、生成中项目
 * 回收不留僵尸任务）与工作区级 {@code POST workspaces/{id}/reclaim}（单台三清：容器/卷/
 * 注册表行，项目行与其余资产不动——#488 在册失联行〔lostAt〕的人工裁决出口，标记随注册行
 * 级联消亡）<strong>同形回执</strong>。
 *
 * <p>逐面 {@code CLEAN}/{@code RESIDUE}/{@code UNKNOWN}（工作区级项目面 {@code UNTOUCHED}
 * 如实分示）＋{@code residueFree} 零残留判据。<strong>UNKNOWN 不算清</strong>：销毁未全成/
 * 探查未成时各面 UNKNOWN、residueFree＝false——呈现「回收未全成、残留归收编通道」，不得当
 * 成功静默；{@code RESIDUE} 如实红不报错重试（物理销毁尽力而为不阻断记录删除〔受理即删〕，
 * 残留归扫描轮/手动清扫收编——建议前端展示「残留待收编」提示）。动刀类走 admin 手动＋留痕
 * （ADR-0045）：每次回收 append-only 落 {@code wsp_reclaim_actions}——操作者经
 * {@code X-User-Id/X-User-Name} 透传头落痕（缺头落空 NULL）；确认弹窗建议展示回收范围
 * （项目级＝项目连同工作区全删；工作区级＝只删单台沙箱，项目仍在）。错误：项目不存在 404
 * PRJ_001；工作区不存在（含畸形 id）404 WSP_001；销毁自身失败 500（未成不留痕，可重试）
 * ——原样透传。</p>
 *
 * @since 0.1.0
 */
public record AiplatformReclaimResponse(

        /** 回收范围（PROJECT=项目级 WORKSPACE=工作区级） */
        @Schema(description = "回收范围（PROJECT=项目级——项目连同工作区全删；WORKSPACE=工作区级"
                + "——只删单台沙箱，项目仍在）", example = "PROJECT")
        String scope,

        /** 项目级＝回收的项目；工作区级＝所属项目引用（无所属 null——在册失联裁决与 fork） */
        @Schema(description = "项目级＝回收的项目；工作区级＝所属项目引用（无所属为 null——在册失联"
                + "裁决面与 fork）", example = "3829492007654001")
        String projectId,

        /** 回收的工作区（三清对象） */
        @Schema(description = "回收的工作区（三清对象）", example = "3829492007654321")
        String workspaceId,

        /** 在飞 run 级联停止与否（回收时在飞 run 被级联停止为 true；无在飞 false） */
        @Schema(description = "在飞 run 级联停止与否（无在飞 false）", example = "true")
        boolean runsStopped,

        /** 容器面（CLEAN=已清 RESIDUE=残留〔归收编通道，如实红〕 UNKNOWN=未知〔不算清〕） */
        @Schema(description = "容器面（CLEAN=已清 RESIDUE=残留〔归扫描轮/手动清扫收编，如实红不报错"
                + "重试〕 UNKNOWN=销毁未全成/探查未成〔不算清——residueFree=false〕）",
                example = "CLEAN")
        String container,

        /** 卷面：同上 */
        @Schema(description = "卷面：同 container 三态", example = "CLEAN")
        String volume,

        /** 注册表行面：同上 */
        @Schema(description = "注册表行面：同 container 三态", example = "CLEAN")
        String registryRow,

        /** 项目行面（项目级 CLEAN/RESIDUE/UNKNOWN；工作区级 UNTOUCHED——不动项目面如实分示） */
        @Schema(description = "项目行面（项目级 CLEAN/RESIDUE/UNKNOWN；工作区级恒 UNTOUCHED——单台"
                + "三清不动项目行，非清非残留）", example = "CLEAN")
        String projectRow,

        /** 零残留判据（scope 内受检面全 CLEAN；任一面 RESIDUE/UNKNOWN 即 false——UNKNOWN 不算清） */
        @Schema(description = "零残留判据（scope 内受检面全 CLEAN 为 true；任一面 RESIDUE/UNKNOWN"
                + " 即 false——UNKNOWN 不算清，回执如实红）", example = "true")
        boolean residueFree,

        /** 操作者标识（admin 侧；缺透传头落空 NULL——append-only 落 wsp_reclaim_actions） */
        @Schema(description = "操作者标识（admin 侧；缺 X-User-Id/X-User-Name 透传头落空 NULL——"
                + "append-only 落 wsp_reclaim_actions 留痕行）", example = "700101")
        String operatorId,

        /** 操作者昵称（同上） */
        @Schema(description = "操作者昵称（同上）", example = "运维·老张")
        String operatorName,

        /** 回收时刻 */
        @Schema(description = "回收时刻", example = "2026-11-13T09:30:00")
        LocalDateTime reclaimedAt) {
}
