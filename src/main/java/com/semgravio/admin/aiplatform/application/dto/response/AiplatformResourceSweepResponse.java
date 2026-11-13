package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台手动清扫回执——北向出口，逐字镜像 aiplatform
 * {@code BackofficeResourceSweepResponse}（#489 资源面，issue #128 镜像）：
 * 逐项处置报告——收编了什么（SWEPT）、跳过什么为什么（SKIPPED）、删除未成什么（FAILED）。
 * 与平台扫描轮自动收编同一实现同一准绳（保留集＝注册表全量＋10 分钟宽限双闸、点名删非
 * prune——docker system df「可回收」口径永不作判定、volume prune 永不进任何通道）。
 *
 * <p>{@code registryReachable}=false＝库不可达一刀未动（准绳无从构成时绝不按「全部无主」
 * 动手，如实呈报不重试）；{@code graceLine}＝宽限线（早于本线创建的无主资源才收——新建
 * 「资源已建、记录未落」窗口保护 10 分钟）；删除未成（FAILED）下轮扫描自愈。操作者两列
 * ＝透传头落痕回显值（X-User-Id/X-User-Name，缺头落空 NULL）。手动清扫不受休眠总开关静默
 * （管理员显式动作，ADR-0045 动刀类走 admin 手动＋留痕）。docker CLI 自身失败 500
 * WSP_002（未执行完的清扫不留痕不定格）原样透传。</p>
 *
 * @since 0.1.0
 */
public record AiplatformResourceSweepResponse(

        /** 清扫时刻 */
        @Schema(description = "清扫时刻", example = "2026-11-13T09:05:00")
        LocalDateTime sweptAt,

        /** 操作者标识（admin 侧；缺透传头落空 NULL——照沙箱动作面口径） */
        @Schema(description = "操作者标识（admin 侧；缺 X-User-Id/X-User-Name 透传头落空 NULL）",
                example = "700101")
        String operatorId,

        /** 操作者昵称（同上） */
        @Schema(description = "操作者昵称（同上）", example = "运维·老张")
        String operatorName,

        /** 注册表可达否（false＝库不可达——一刀未动如实呈报，不重试） */
        @Schema(description = "注册表可达否（false＝库不可达整体跳过轮——一刀未动如实呈报，"
                + "不重试）", example = "true")
        boolean registryReachable,

        /** 宽限线（早于本线创建的无主资源才收——新建窗口保护 10 分钟） */
        @Schema(description = "宽限线（早于本线创建的无主资源才收——新建「资源已建、记录未落」"
                + "窗口保护 10 分钟）", example = "2026-11-13T08:55:00")
        LocalDateTime graceLine,

        /** 已收编容器数 */
        @Schema(description = "已收编容器数", example = "1")
        int sweptContainers,

        /** 已收编卷数 */
        @Schema(description = "已收编卷数", example = "1")
        int sweptVolumes,

        /** 跳过数（在册/宽限窗内/时刻解析不出三类原因之和） */
        @Schema(description = "跳过数（在册〔含休眠保卷——用户数据 ADR-0016〕/宽限窗内/时刻"
                + "解析不出三类原因之和）", example = "3")
        int skippedCount,

        /** 删除未成数（点名删失败——下轮扫描自愈，不虚报） */
        @Schema(description = "删除未成数（点名删失败——下轮扫描自愈，不虚报全清）", example = "0")
        int failedCount,

        /** 逐项处置（主命名资源全量：容器＋卷） */
        @Schema(description = "逐项处置（主命名资源全量：容器＋卷——动了没动都可审计）")
        List<SweepItem> items) {

    /**
     * 逐项处置行：kind＝CONTAINER/VOLUME；disposition＝SWEPT/SKIPPED/FAILED；
     * skipReason＝REGISTERED/GRACE_WINDOW/TIMESTAMP_UNREADABLE（SWEPT/FAILED 为
     * null——只有跳过才有原因）；resourceCreatedAt＝docker 回执的启动/创建时刻（在册项
     * 未起 inspect 恒 null）。命名 {@code SweepItem}（非 {@code Item}）——swagger 组件名
     * 以内嵌 record 简名落全局命名空间，泛型名会被后来者撞号（provider 侧同款命名）。
     */
    public record SweepItem(
            /** 资源类（CONTAINER=容器 VOLUME=本地卷） */
            @Schema(description = "资源类（CONTAINER=容器 VOLUME=本地卷）", example = "CONTAINER")
            String kind,
            /** docker 资源名（ws-<id>/vol-ws-<id> 主命名资源） */
            @Schema(description = "docker 资源名（ws-<id>/vol-ws-<id> 主命名资源）",
                    example = "ws-3829492007654321")
            String identifier,
            /** 处置（SWEPT=已收编 SKIPPED=跳过 FAILED=删除未成） */
            @Schema(description = "处置（SWEPT=已收编 SKIPPED=跳过〔原因见 skipReason〕"
                    + " FAILED=删除未成〔下轮扫描自愈〕）", example = "SWEPT")
            String disposition,
            /** 跳过原因（REGISTERED=在册〔含休眠保卷〕 GRACE_WINDOW=宽限窗内 TIMESTAMP_UNREADABLE=时刻解析不出；SWEPT/FAILED 为 null） */
            @Schema(description = "跳过原因（REGISTERED=在册〔含休眠保卷——用户数据不动〕"
                    + " GRACE_WINDOW=宽限窗内 TIMESTAMP_UNREADABLE=时刻解析不出无从判宽限；"
                    + "SWEPT/FAILED 为 null——只有跳过才有原因）", example = "GRACE_WINDOW")
            String skipReason,
            /** 资源创建时刻（docker 回执；在册项未起 inspect 恒 null） */
            @Schema(description = "资源创建时刻（docker 回执的启动/创建时刻；在册项未起 inspect"
                    + " 恒 null）", example = "2026-11-13T08:40:00")
            LocalDateTime resourceCreatedAt) {
    }
}
