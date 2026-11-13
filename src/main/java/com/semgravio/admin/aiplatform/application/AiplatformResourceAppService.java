package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceOverviewResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceSweepResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformResourceOverviewWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformResourceSweepWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 后台「资源面」BFF 应用服务——资源大盘＋手动清扫（issue #128，镜像
 * aiplatform #489：#473/ADR-0045 资源回收通道的观测与手动处置面，两枚全新端点）。
 * 资源事实与收编面归 aiplatform base.workspace 上下文——admin 不持业务逻辑、不持
 * 采样/留痕数据，纯透传零加戏。
 *
 * <p>大盘（{@link #overview()}）纯读库秒级返回（provider 侧零 docker 调用——数据全部
 * 来自扫描轮采样落库 {@code wsp_host_observation} 与失联标记/清扫留痕表）：
 * <strong>null＝缺测、0＝真零</strong>两态透传不打折——从未采样整面 null（呈现「采样中」
 * 而非零）、docker 不可达轮 docker 列 null。在册失联计数（{@code lostWorkspaceCount}）
 * 只标记待人工裁决（出口＝一键回收 reclaim——在册≠无主，不得当无主自动清）。</p>
 *
 * <p>清扫（{@link #sweep()}）复用扫描轮自动收编同一实现同一准绳（保留集＝注册表全量
 * ＋10 分钟宽限双闸、点名删非 prune），逐项处置报告原样透传；{@code registryReachable}
 * =false＝库不可达一刀未动如实呈报。操作者经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（AppService 不经手，缺头落空 NULL——照
 * 沙箱动作面口径，provider 侧不落强制守卫）；错误经 {@link AiplatformClient} 统一翻译
 * 为 {@link AiplatformUpstreamException} 原样透传（docker CLI 自身失败 500 WSP_002），
 * 本层不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformResourceAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformResourceAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 资源大盘（透传 aiplatform）——观测面：主机盘（平台进程所在文件系统，path＝df 对账锚）
     * ＋docker 四类用量（镜像/容器/本地卷/build cache 计数与体量——体量十进制单位与 docker
     * 显示同基）＋无主资源计数（ADR-0045 准绳）＋在册失联计数＋最近手动清扫。未采样各面
     * 如实 null（null≠零）、sampledAt 新鲜度标注——provider 裁决全量透传，admin 不复算。
     */
    public AiplatformResourceOverviewResponse overview() {
        return toOverview(aiplatformClient.getResourceOverview());
    }

    /**
     * 手动清扫（透传 aiplatform）——按需触发一次无主收编（不等扫描轮），回执＝逐项处置
     * 报告（SWEPT/SKIPPED〔REGISTERED/GRACE_WINDOW/TIMESTAMP_UNREADABLE〕/FAILED）＋
     * 计数面。库不可达（registryReachable=false）一刀未动如实呈报。<strong>无请求体</strong>；
     * 操作者透传头落痕（缺头落空 NULL）。docker CLI 自身失败 500 WSP_002 原样透传。
     */
    public AiplatformResourceSweepResponse sweep() {
        return toSweep(aiplatformClient.sweepResources());
    }

    private static AiplatformResourceOverviewResponse toOverview(
            AiplatformResourceOverviewWireResponse wire) {
        return new AiplatformResourceOverviewResponse(
                wire.host() == null ? null : new AiplatformResourceOverviewResponse.HostFace(
                        wire.host().path(), wire.host().totalBytes(),
                        wire.host().usableBytes(), wire.host().usedBytes()),
                wire.docker() == null ? null : new AiplatformResourceOverviewResponse.DockerFace(
                        wire.docker().imageCount(), wire.docker().imageSizeBytes(),
                        wire.docker().containerCount(), wire.docker().containerSizeBytes(),
                        wire.docker().volumeCount(), wire.docker().volumeSizeBytes(),
                        wire.docker().buildCacheCount(), wire.docker().buildCacheSizeBytes()),
                wire.orphans() == null ? null : new AiplatformResourceOverviewResponse.OrphanFace(
                        wire.orphans().containerCount(), wire.orphans().volumeCount()),
                wire.lostWorkspaceCount(),
                wire.sampledAt(),
                wire.lastSweep() == null ? null : new AiplatformResourceOverviewResponse.LastSweep(
                        wire.lastSweep().sweptAt(),
                        wire.lastSweep().operatorId(), wire.lastSweep().operatorName(),
                        wire.lastSweep().registryReachable(),
                        wire.lastSweep().sweptContainers(), wire.lastSweep().sweptVolumes(),
                        wire.lastSweep().skippedCount(), wire.lastSweep().failedCount()));
    }

    private static AiplatformResourceSweepResponse toSweep(
            AiplatformResourceSweepWireResponse wire) {
        // provider 构造器归一 items 恒非 null（域报告一源）——直接镜像不另设缺省
        List<AiplatformResourceSweepResponse.SweepItem> items = wire.items().stream()
                .map(item -> new AiplatformResourceSweepResponse.SweepItem(
                        item.kind(), item.identifier(), item.disposition(),
                        item.skipReason(), item.resourceCreatedAt()))
                .toList();
        return new AiplatformResourceSweepResponse(
                wire.sweptAt(), wire.operatorId(), wire.operatorName(),
                wire.registryReachable(), wire.graceLine(),
                wire.sweptContainers(), wire.sweptVolumes(),
                wire.skippedCount(), wire.failedCount(), items);
    }
}
