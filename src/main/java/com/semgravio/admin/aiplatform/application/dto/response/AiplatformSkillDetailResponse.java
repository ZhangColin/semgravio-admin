package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
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
 * <p>scripts/ 资源面（#253 漂移补齐，T2 追平）：{@code resources} 为 {@code Map<String,String>}
 * （技能目录相对路径 → 文件内容全文，与运行时注入面同源——脚本也是注入面，审核须可读）；
 * 无 scripts／内置技能恒<strong>空对象</strong>（区别于 null）。使用计数观测面（#261）：
 * {@code loadCount}/{@code lastLoadedAt} 语义与北向形状同清单行（Long 北向 JSON string、
 * 0＝从未加载、内置恒 null 不适用）。</p>
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

        /** 来源 code（1=内置 2=安装 3=自产） */
        @Schema(description = "来源 code（1=内置 2=安装 3=自产）", example = "1")
        Integer source,

        /** 来源名（直读展示） */
        String sourceName,

        /** 来源包标识（安装仓库；内置为 null、自产＝固定虚拟值 self） */
        String sourcePackage,

        /** 版本标识（装时 commit；自产＝self；内置为 null） */
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

        /**
         * scripts/ 资源面：技能目录相对路径 → 文件内容全文（与运行时注入面同源——脚本也是
         * 注入面，审核须可读）；空对象＝无 scripts／内置技能。
         */
        @Schema(description = "scripts/ 资源面：技能目录相对路径 → 文件内容全文（与运行时注入面"
                + "同源，审核须可读）；空对象＝无 scripts／内置技能",
                example = "{\"scripts/run-tests.sh\": \"#!/bin/bash\\nset -e\\n\"}")
        Map<String, String> resources,

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
        Boolean updateAvailable,

        /**
         * 加载次数（使用计数观测面，语义同清单行）：load 实际发生口径——智能体 load 工具
         * 真实加载该技能一次即 +1（指派/启停/清单重建不计数）；0＝从未被加载过；内置技能
         * 恒 null 不适用。北向 JSON 呈 string 形态（同 id 口径）。
         */
        @Schema(description = "加载次数（使用计数观测面，语义同清单行——智能体 load 工具真实"
                + "加载该技能一次即 +1；0＝从未被加载过；内置技能恒 null 不适用；北向 JSON"
                + "呈 string 形态）", example = "3")
        Long loadCount,

        /** 最近加载时刻（使用计数观测面）：null＝从未被加载过（含新装行）；内置恒 null 不适用 */
        @Schema(description = "最近加载时刻（使用计数观测面）；null＝从未被加载过（含新装行）；"
                + "内置技能恒 null 不适用")
        LocalDateTime lastLoadedAt
) {
}
