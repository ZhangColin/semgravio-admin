package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 后台手动清扫回执的 wire 镜像——与 aiplatform
 * {@code BackofficeResourceSweepResponse}（#489 资源面，issue #128 镜像）字段同构：
 * 逐项处置报告——收编了什么（SWEPT）、跳过什么（SKIPPED：REGISTERED＝在册/
 * GRACE_WINDOW＝宽限窗内/TIMESTAMP_UNREADABLE＝时刻解析不出）、删除未成什么（FAILED——
 * 下轮扫描再收）。与平台扫描轮自动收编同一实现同一准绳（保留集＝注册表全量＋10 分钟
 * 宽限双闸、点名删非 prune）。
 *
 * <p>{@code registryReachable}=false＝库不可达一刀未动（准绳无从构成时绝不按「全部无主」
 * 动手，如实呈报不重试）；操作者两列＝透传头落痕值（缺头落空 NULL——照沙箱动作面口径）。
 * 手动清扫不受休眠总开关静默（管理员显式动作，ADR-0045 动刀类走 admin 手动＋留痕）。
 * docker CLI 自身失败 500 WSP_002（未执行完的清扫不留痕不定格）。</p>
 *
 * @param sweptAt           清扫时刻
 * @param operatorId        操作者标识（admin 侧；缺透传头为 null——落空口径）
 * @param operatorName      操作者昵称（同上）
 * @param registryReachable 注册表可达否（false＝库不可达整体跳过轮）
 * @param graceLine         宽限线（早于本线创建的无主资源才收——新建窗口保护 10 分钟）
 * @param sweptContainers   已收编容器数
 * @param sweptVolumes      已收编卷数
 * @param skippedCount      跳过数
 * @param failedCount       删除未成数
 * @param items             逐项处置（主命名资源全量：容器＋卷）
 */
public record AiplatformResourceSweepWireResponse(
        LocalDateTime sweptAt,
        String operatorId,
        String operatorName,
        boolean registryReachable,
        LocalDateTime graceLine,
        int sweptContainers,
        int sweptVolumes,
        int skippedCount,
        int failedCount,
        List<SweepItem> items) {

    /**
     * 逐项处置行：kind＝CONTAINER/VOLUME；disposition＝SWEPT/SKIPPED/FAILED；
     * skipReason＝REGISTERED/GRACE_WINDOW/TIMESTAMP_UNREADABLE（SWEPT/FAILED 为
     * null）；resourceCreatedAt＝docker 回执的启动/创建时刻（在册项未起 inspect 恒 null）。
     * 命名 {@code SweepItem}（非 {@code Item}）——swagger 组件名以内嵌 record 简名落全局
     * 命名空间，泛型名会被后来者撞号（provider 侧同款命名）。
     */
    public record SweepItem(String kind, String identifier, String disposition,
            String skipReason, LocalDateTime resourceCreatedAt) {
    }
}
