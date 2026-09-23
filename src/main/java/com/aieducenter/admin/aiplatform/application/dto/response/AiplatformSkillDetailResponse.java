package com.aieducenter.admin.aiplatform.application.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能详情——北向出口，逐字镜像 aiplatform {@code BackofficeSkillDetailResponse}
 * （#247，审核面）：清单行同形元数据＋技能全文——frontmatter（解析态全量）与正文
 * （frontmatter 剥离后）分面可读，<strong>所见即运行时注入面</strong>（安装审核承载面，
 * 运营读全文判注入面与方法论重叠风险）。
 *
 * <p>{@code frontmatter} 为自由键值对象逐字透传（含 name/description，不做形状断言——
 * spec #79）；{@code content} 为 SKILL.md 正文全文（不经任何截断/改写）。{@code id}
 * 同清单行 opaque 串两形制；技能不存在（含未寻址内置名/TSID、畸形柄）404 SKL_001
 * ＝数字业务码 7001（provider 裁决，admin 不预检）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillDetailResponse(

        /** 技能柄（opaque 串两形制，同清单行 id：内置＝builtin:<技能名>、安装＝TSID 十进制串） */
        @Schema(description = "技能柄（opaque 串两形制，同清单行 id：内置＝builtin:<技能名>、"
                + "安装＝TSID 十进制串）", example = "builtin:prd-writing")
        String id,

        /** 技能名 */
        String name,

        /** 简介 */
        String description,

        /** 来源 code（1=内置 2=安装） */
        @Schema(description = "来源 code（1=内置 2=安装）", example = "1")
        Integer source,

        /** 来源名（直读展示） */
        String sourceName,

        /** 来源包标识（安装仓库；内置为 null） */
        String sourcePackage,

        /** 版本标识（装时 commit；内置为 null） */
        String version,

        /** 状态 code（1=启用 2=停用；内置恒 1） */
        @Schema(description = "状态 code（1=启用 2=停用；内置恒 1）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** SKILL.md frontmatter 全量（解析态键值——审核面所见即运行时注入面） */
        @Schema(description = "SKILL.md frontmatter 全量（解析态键值——审核面所见即运行时注入面）",
                example = "{\"name\": \"prd-writing\", \"description\": \"撰写或修订 PRD 时使用\"}")
        Map<String, Object> frontmatter,

        /** SKILL.md 正文（frontmatter 剥离后全文，审核承载面） */
        @Schema(description = "SKILL.md 正文（frontmatter 剥离后全文，审核承载面）")
        String content,

        /** 最近管理动作操作者 id（安装＝装者、启停＝最近动作者；内置为 null） */
        @Schema(description = "最近管理动作操作者 id（安装＝装者、启停＝最近动作者；"
                + "内置为 null）", example = "700200")
        String operatorId,

        /** 最近管理动作操作者名（直读展示；内置为 null） */
        @Schema(description = "最近管理动作操作者名（直读展示；内置为 null）",
                example = "运营·技能管理员")
        String operatorName,

        /** 远端有新版标记（来源包级，语义同清单行；null＝未检查过，内置恒 null） */
        @Schema(description = "远端有新版标记（来源包级，语义同清单行——true＝远端 HEAD ≠"
                + " 装时版本；null＝未检查过，内置技能恒 null 不适用）", example = "false")
        Boolean updateAvailable
) {
}
