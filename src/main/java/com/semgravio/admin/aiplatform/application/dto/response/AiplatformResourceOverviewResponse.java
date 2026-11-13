package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台资源大盘——北向出口，逐字镜像 aiplatform
 * {@code BackofficeResourceOverviewResponse}（#489 资源面，issue #128 镜像）：
 * 主机盘面＋docker 四类用量＋无主资源计数＋在册失联计数＋采样新鲜度＋最近一次手动清扫。
 * 纯读库秒级返回（provider 侧零 docker 调用——观测面数据全部来自扫描轮采样落库）。
 *
 * <p><strong>null＝缺测、0＝真零</strong>两态必须分开展示（前端渲染红线）：从未采样（首个
 * 扫描窗内）{@code host}/{@code docker}/{@code orphans} 整面 null——呈现「采样中」而非
 * 零；docker 不可达轮 docker 面各列 null——「缺测」≠「零」。体量为 docker 显示同基的
 * 近似值（十进制单位 3 位有效数字）——呈现建议保留一位小数＋单位，不当精确字节求和。
 * 主机盘 usedBytes＝total−usable 派生（与 df 的 Used 列含保留块口径略有差异）。
 * {@code lostWorkspaceCount} 在册失联计数（#488——项目行已删而物理资源仍在，只标记待
 * 人工裁决，出口＝一键回收 reclaim，<strong>不得当无主自动清</strong>：在册≠无主）。
 * 无主计数口径（ADR-0045 准绳）：docker 侧工作区命名资源对注册表全量求差——docker
 * system df「可回收」口径不进本面（该口径把休眠卷一并计入）。{@code lastSweep} 从未
 * 手动清扫为 null。</p>
 *
 * @since 0.1.0
 */
public record AiplatformResourceOverviewResponse(

        /** 主机盘面（null＝从未采样——首个扫描窗内呈现「采样中」） */
        @Schema(description = "主机盘面（null＝从未采样——首个扫描窗内呈现「采样中」而非零）")
        HostFace host,

        /** docker 四类用量面（null＝从未采样；列内 null＝该轮 docker 不可达缺测） */
        @Schema(description = "docker 四类用量面（null＝从未采样；列内 null＝该轮 docker 不可达"
                + "缺测——「缺测」≠「零」，两态必须分开展示）")
        DockerFace docker,

        /** 无主资源计数面（null＝从未采样；ADR-0045 准绳——docker 侧 ws-<id> 容器/vol-ws-<id> 卷对注册表求差，docker system df「可回收」口径不进本面） */
        @Schema(description = "无主资源计数面（null＝从未采样；ADR-0045 准绳——docker 侧"
                + " ws-<id> 容器/vol-ws-<id> 卷对注册表全量求差；docker system df「可回收」"
                + "口径把休眠卷一并计入，永不进本面）")
        OrphanFace orphans,

        /** 在册失联工作区计数（项目行已删而物理资源仍在——只标记待人工裁决，出口＝一键回收） */
        @Schema(description = "在册失联工作区计数（项目行已删而物理资源仍在——只标记待人工裁决，"
                + "出口＝一键回收 reclaim；在册≠无主，不得当无主自动清）", example = "2")
        long lostWorkspaceCount,

        /** 采样时刻（新鲜度标注；未采样 null） */
        @Schema(description = "采样时刻（新鲜度标注；未采样 null——扫描轮采样落库约 5 分钟新鲜度）",
                example = "2026-11-13T10:30:00")
        LocalDateTime sampledAt,

        /** 最近一次手动清扫（收编结果面直读；从未手动清扫为 null） */
        @Schema(description = "最近一次手动清扫（收编结果面直读——操作者/时刻/收编计数；"
                + "从未手动清扫为 null）")
        LastSweep lastSweep) {

    /**
     * 主机盘面：平台进程所在文件系统（path＝与 df 对账的路径锚）。
     *
     * @param path        测量锚路径
     * @param totalBytes  总字节（statfs）
     * @param usableBytes 可用字节（statfs）
     * @param usedBytes   已用字节（total−usable 派生，与 df 的 Used 列含保留块口径略有差异；任一缺测为 null）
     */
    public record HostFace(
            @Schema(description = "测量锚路径（平台进程所在文件系统，与 df 对账的路径锚）",
                    example = "/var/lib/docker")
            String path,
            @Schema(description = "总字节（statfs）", example = "494384795648")
            Long totalBytes,
            @Schema(description = "可用字节（statfs）", example = "301989888000")
            Long usableBytes,
            @Schema(description = "已用字节（total−usable 派生，与 df 的 Used 列含保留块口径"
                    + "略有差异；任一缺测为 null）", example = "192394907648")
            Long usedBytes) {
    }

    /**
     * docker 四类用量面（计数/体量字节——体量按十进制单位换算与 docker 显示同基，近似值
     * 不当精确字节求和；列内 null＝该轮 docker 不可达缺测）。
     */
    public record DockerFace(
            @Schema(description = "镜像计数", example = "12") Long imageCount,
            @Schema(description = "镜像体量字节（docker 显示同基近似值）", example = "4442639553")
            Long imageSizeBytes,
            @Schema(description = "容器计数", example = "8") Long containerCount,
            @Schema(description = "容器体量字节（docker 显示同基近似值）", example = "2147483648")
            Long containerSizeBytes,
            @Schema(description = "本地卷计数", example = "9") Long volumeCount,
            @Schema(description = "本地卷体量字节（docker 显示同基近似值）", example = "12884901888")
            Long volumeSizeBytes,
            @Schema(description = "build cache 计数", example = "20") Long buildCacheCount,
            @Schema(description = "build cache 体量字节（docker 显示同基近似值）", example = "3221225472")
            Long buildCacheSizeBytes) {
    }

    /** 无主资源计数面（在册≠无主——在册失联另有 lostWorkspaceCount 承载，出口＝一键回收）。 */
    public record OrphanFace(
            @Schema(description = "无主容器计数（docker 侧 ws-<id> 容器对注册表求差）", example = "1")
            Long containerCount,
            @Schema(description = "无主卷计数（docker 侧 vol-ws-<id> 卷对注册表求差）", example = "1")
            Long volumeCount) {
    }

    /** 最近一次手动清扫（留痕行直读——大盘的「收编结果」面；从未手动清扫整面 null）。 */
    public record LastSweep(
            @Schema(description = "清扫时刻", example = "2026-11-13T09:00:00")
            LocalDateTime sweptAt,
            @Schema(description = "操作者标识（admin 侧；缺透传头落空 NULL）", example = "700101")
            String operatorId,
            @Schema(description = "操作者昵称（同上）", example = "运维·老张")
            String operatorName,
            @Schema(description = "注册表可达否（false＝库不可达轮——一刀未动如实呈报）",
                    example = "true")
            Boolean registryReachable,
            @Schema(description = "已收编容器数", example = "1") Integer sweptContainers,
            @Schema(description = "已收编卷数", example = "1") Integer sweptVolumes,
            @Schema(description = "跳过数（在册/宽限窗/时刻解析不出三类原因）", example = "3")
            Integer skippedCount,
            @Schema(description = "删除未成数（下轮扫描自愈）", example = "0")
            Integer failedCount) {
    }
}
