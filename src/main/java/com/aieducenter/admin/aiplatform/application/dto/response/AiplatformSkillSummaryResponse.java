package com.aieducenter.admin.aiplatform.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能清单行——北向出口，逐字镜像 aiplatform {@code BackofficeSkillSummaryResponse}
 * （#247，清单行/安装回执共形）：内置（classpath 合成）与库中技能（安装＋自产）同权共形，
 * 来源包/版本标识/操作者仅库行有值（内置随平台发版；自产行 sourcePackage/version＝固定
 * 虚拟值 self）。技能库是有界目录（provider 定死：内置在前名称序、
 * 安装在后按来源包＋名称序），不分页不过滤（spec #79 同形不造分页）。
 *
 * <p>{@code id} 为 <strong>opaque 串两形制</strong>（内置 {@code builtin:<技能名>}／安装 TSID 十进制串）
 * ——前端按原值回传作详情/写口寻址柄，勿做数值假设（spec #79：admin 字符串透传不解析不预校验，
 * 畸形/未寻址柄由 provider 判 404 SKL_001＝数字业务码 7001）。{@code source}/{@code status}
 * Integer code + {@code sourceName}/{@code statusName} 中文名随行（provider 出口提供）；
 * {@code updateAvailable}＝来源包级远端有新版标记（null＝未检查过，内置恒 null——如实出
 * JSON null）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillSummaryResponse(

        /** 技能柄（详情寻址，opaque 串两形制，勿做数值假设）：内置＝builtin:<技能名>，安装＝TSID 十进制串 */
        @Schema(description = "技能柄（详情寻址，opaque 串两形制，勿做数值假设）：内置技能＝"
                + "builtin:<技能名>，安装技能＝TSID 十进制串",
                example = "builtin:prd-writing")
        String id,

        /** 技能名（frontmatter name；安装行与来源包合成唯一键） */
        String name,

        /** 简介（frontmatter description） */
        String description,

        /** 来源 code（1=内置 2=安装 3=自产） */
        @Schema(description = "来源 code（1=内置 2=安装 3=自产）", example = "1")
        Integer source,

        /** 来源名（直读展示） */
        String sourceName,

        /** 来源包标识（安装仓库；内置为 null、自产＝固定虚拟值 self） */
        @Schema(description = "来源包标识（安装仓库；内置为 null、自产＝固定虚拟值 self）")
        String sourcePackage,

        /** 版本标识（装时 commit；自产＝self；内置为 null） */
        @Schema(description = "版本标识（装时 commit；自产＝self；内置为 null）")
        String version,

        /** 状态 code（1=启用 2=停用；内置恒 1） */
        @Schema(description = "状态 code（1=启用 2=停用；内置恒 1）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** 最近管理动作操作者 id（安装＝装者、启停＝最近动作者；内置为 null） */
        @Schema(description = "最近管理动作操作者 id（安装＝装者、启停＝最近动作者；"
                + "内置为 null）", example = "700200")
        String operatorId,

        /** 最近管理动作操作者名（直读展示；内置为 null） */
        @Schema(description = "最近管理动作操作者名（直读展示；内置为 null）",
                example = "运营·技能管理员")
        String operatorName,

        /** 远端有新版标记（来源包级）：null＝未检查过（内置技能恒 null） */
        @Schema(description = "远端有新版标记（来源包级）：定期只读检查远端 HEAD 与装时版本"
                + "不同即 true——更新永远显式点（POST /update），平台不自动跟新远端；"
                + "null＝未检查过（内置技能恒 null 不适用）", example = "false")
        Boolean updateAvailable
) {
}
