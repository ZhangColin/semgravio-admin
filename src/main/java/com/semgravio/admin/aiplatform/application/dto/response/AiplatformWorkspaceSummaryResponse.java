package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

/**
 * AI 平台沙箱清单行（BFF 透传 aiplatform）——北向 {@code GET /api/admin/aiplatform/workspaces}
 * 条目，与 aiplatform {@code BackofficeWorkspaceSummaryResponse} 字段逐字同构（issue #66）。
 *
 * <p>运营第一次能看见每台沙箱睡没睡、封没封、占多少存储——期望态与实态两列分示
 * （漂移一眼可见）。行内嵌项目引用（跳转项目详情的锚；无所属项目为 null）。实态列与卷大小
 * 为<strong>采样快照</strong>（aiplatform #487，issue #126）：响应秒级稳定（不再请求时现场
 * 探查），数值以 {@code sampledAt} 标注新鲜度；未采样（字段 null）≠ 探查失败（containerState=4
 * 未知）≠ 无容器（=3）——宜呈现「未采样」占位而非「未知」。另带在册失联标记 {@code lostAt}
 * （aiplatform #488，issue #127）：非 null＝注册行在而项目行已删、物理资源仍在——「失联多久」
 * 以此为锚，处置出口＝工作区一键回收（人工裁决后动作），机器不当无主资源自动清。</p>
 *
 * @since 0.1.0
 */
public record AiplatformWorkspaceSummaryResponse(

        /** 工作区标识（TSID 十进制字符串） */
        String workspaceId,

        /** 容器名（与 docker ps 对账锚点） */
        String containerName,

        /** 环境类型（1=开发 2=测试 3=生产） */
        Integer kind,

        /** 环境类型名 */
        String kindName,

        /** 置备状态（1=置备中 2=就绪 3=失败）：在途置备/唤醒（PROVISIONING）与漂移的区分锚 */
        Integer status,

        /** 置备状态名 */
        String statusName,

        /** 期望态（1=运行 2=休眠 3=封存，意图侧） */
        Integer desiredState,

        /** 期望态名 */
        String desiredStateName,

        /** 容器实态采样（1=运行中 2=已停止 3=无容器 4=未知——采样到探查失败）；未采样为 null
         * （新工作区首个采样窗内，aiplatform #487 采样快照口径） */
        Integer containerState,

        /** 容器实态名（未采样为 null） */
        String containerStateName,

        /** 最近触碰（闲置计时输入） */
        LocalDateTime lastTouchAt,

        /** 卷用量采样（字节；封存按意图容缺/未采样/采样失败为 null） */
        Long volumeSizeBytes,

        /** 实态列采样时刻（新鲜度标注，宜展示「采样于 X 分钟前」；未采样为 null） */
        LocalDateTime sampledAt,

        /** 在册失联判定时刻（aiplatform #488，issue #127——项目行已删而物理资源仍在的静止现场，
         * 「失联多久」以此为锚；处置出口＝一键回收人工裁决，机器不自动清）；未失联为 null */
        LocalDateTime lostAt,

        /** 封存时刻（未封存为 null） */
        LocalDateTime sealedAt,

        /** 封存包大小（字节；未封存为 null） */
        Long archiveSizeBytes,

        /** 所属项目引用（无所属项目为 null） */
        ProjectRef project
) {

    /**
     * 所属项目引用：认出项目即可（详情另取）——id＋名称＋归档位。
     */
    public record ProjectRef(

            /** 项目标识（TSID 十进制字符串） */
            String projectId,

            /** 项目名 */
            String name,

            /** 是否已归档 */
            Boolean archived
    ) {
    }
}
