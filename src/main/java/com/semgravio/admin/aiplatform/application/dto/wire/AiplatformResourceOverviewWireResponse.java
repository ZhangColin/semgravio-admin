package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 后台资源大盘的 wire 镜像——与 aiplatform
 * {@code BackofficeResourceOverviewResponse}（#489 资源面，issue #128 镜像）字段同构：
 * 主机盘面＋docker 四类用量＋无主资源计数＋在册失联计数＋采样新鲜度＋最近一次手动清扫。
 *
 * <p>纯读库零 docker 调用（provider 侧数据来自扫描轮采样落库 {@code wsp_host_observation}
 * 与失联标记表）——<strong>null＝缺测、0＝真零</strong>两态必须分开展示：从未采样（首个扫描
 * 窗内）{@code host}/{@code docker}/{@code orphans} 整面 null（呈现「采样中」而非零）；
 * docker 不可达轮 docker 面各列 null。{@code lostWorkspaceCount} 在册失联计数（项目行已删
 * 而物理资源仍在——只标记待人工裁决，出口＝一键回收 reclaim，<strong>不得当无主自动清</strong>：
 * 在册≠无主）。{@code lastSweep} 从未手动清扫为 null。</p>
 *
 * @param host               主机盘面（null＝从未采样）
 * @param docker             docker 四类用量面（null＝从未采样；列内 null＝缺测轮）
 * @param orphans            无主资源计数面（null＝从未采样）
 * @param lostWorkspaceCount 在册失联工作区计数
 * @param sampledAt          采样时刻（新鲜度标注；未采样 null）
 * @param lastSweep          最近一次手动清扫（从未手动清扫为 null）
 */
public record AiplatformResourceOverviewWireResponse(
        HostFace host,
        DockerFace docker,
        OrphanFace orphans,
        long lostWorkspaceCount,
        LocalDateTime sampledAt,
        LastSweep lastSweep) {

    /**
     * 主机盘面：平台进程所在文件系统（path＝与 df 对账的路径锚）。
     *
     * @param path        测量锚路径
     * @param totalBytes  总字节（statfs）
     * @param usableBytes 可用字节（statfs）
     * @param usedBytes   已用字节（total−usable 派生；任一缺测为 null）
     */
    public record HostFace(String path, Long totalBytes, Long usableBytes, Long usedBytes) {
    }

    /**
     * docker 四类用量面（计数/体量字节——体量按十进制单位换算，与 docker 自身显示同基；
     * 呈现建议保留一位小数＋单位，不当精确字节求和）。
     */
    public record DockerFace(
            Long imageCount, Long imageSizeBytes,
            Long containerCount, Long containerSizeBytes,
            Long volumeCount, Long volumeSizeBytes,
            Long buildCacheCount, Long buildCacheSizeBytes) {
    }

    /** 无主资源计数面（docker 侧 ws-&lt;id&gt; 容器/vol-ws-&lt;id&gt; 卷对注册表求差——ADR-0045 准绳，docker system df「可回收」口径不进本面）。 */
    public record OrphanFace(Long containerCount, Long volumeCount) {
    }

    /** 最近一次手动清扫（留痕行直读——大盘的「收编结果」面）。 */
    public record LastSweep(
            LocalDateTime sweptAt,
            String operatorId, String operatorName,
            Boolean registryReachable,
            Integer sweptContainers, Integer sweptVolumes,
            Integer skippedCount, Integer failedCount) {
    }
}
