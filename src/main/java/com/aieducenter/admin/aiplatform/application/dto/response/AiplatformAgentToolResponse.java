package com.aieducenter.admin.aiplatform.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台工具面清单行——北向出口，逐字镜像 aiplatform {@code BackofficeAgentToolResponse}
 * （#252 工具面，#246 技能线 T6，spec #79 定稿、issue #85）：注册名＋类别＋挂载态＋一句
 * 描述。清单嵌套行与 PUT 开关回执同形（provider 回执即该工具行）。
 *
 * <p>{@code kind} 为类别名字符串（SKELETON=骨架——编排链路＋项目事实只读件，结构性锁死
 * 不开放关停；ENHANCEMENT=增强——联网搜索/网页抓取，窄幅可开关；HARNESS_BUILTIN=harness
 * 内建编码工具——框架自带呈现口径，不可开关）——开关判定正本，前端按 kind 决定开关是否
 * 可交互。{@code enabled} 为 provider 原生 boolean 恒在场（admin 同型镜像）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentToolResponse(

        /** 工具注册名（模型可见名＝按名寻址键；清单读面即全集——PUT 开关与骨架锁死判定都按本名） */
        @Schema(description = "工具注册名（模型可见名＝工具面按名寻址键；清单读面即全集——"
                + "PUT 开关与骨架锁死判定都按本名）", example = "web_search")
        String name,

        /** 工具面类别（开关判定正本）：SKELETON=骨架（编排链路＋项目事实只读件，结构性锁死不开放关停——ADR-0021 编排权不下放配置）；ENHANCEMENT=增强（联网搜索/网页抓取，窄幅可开关：关即退出槽位装配面、开即回归）；HARNESS_BUILTIN=harness 内建编码工具（框架自带，呈现口径，不可开关） */
        @Schema(description = "工具面类别（开关判定正本）：SKELETON=骨架（编排链路＋项目事实"
                + "只读件，结构性锁死不开放关停——ADR-0021 编排权不下放配置）；ENHANCEMENT="
                + "增强（联网搜索/网页抓取，窄幅可开关：关即退出槽位装配面、开即回归）；"
                + "HARNESS_BUILTIN=harness 内建编码工具（框架自带，呈现口径，不可开关）",
                example = "ENHANCEMENT")
        String kind,

        /** 挂载态：骨架/harness 内建恒 true；增强按运营配置生效态（false＝在册但退出装配面——模型不可见） */
        @Schema(description = "挂载态：骨架/harness 内建恒 true；增强按运营配置生效态"
                + "（false＝在册但退出装配面——模型不可见）")
        boolean enabled,

        /** 一句职能描述（清单即审核/排障面） */
        @Schema(description = "一句职能描述（清单即审核/排障面）",
                example = "自主搜索调研补缺口（增强可关——成本与滥用可控）")
        String description
) {
}
