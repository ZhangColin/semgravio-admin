package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置读面——北向出口，逐字镜像 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5，spec #79 定稿、issue #84）：生效值＋覆盖标记＋
 * 枚举默认预览＋增强工具开关存储态＋最近写者。GET 读面与 PUT 回执同形（provider 写入后
 * 重读同一视图）。
 *
 * <p>生效值＝库覆盖值或枚举默认（装配实取值——对照覆盖标记知来源）；默认预览即清空覆盖的
 * 落点（恢复默认前可先看）。无覆盖行＝全回落：覆盖标记 false、写者两肢与 updatedAt null
 * 如实出 JSON null（全局 Jackson 含 null）。覆盖标记与开关四件 provider 恒在场（原生
 * boolean，admin 同型镜像）。寻址稳定键两把：main=主智能体 / executor=run 执行体——
 * classify/naming 等一次性判定提示词不属本面；未知键由 provider 判 404 PRJ_033＝数字业务码
 * 4033（admin 字符串透传不预校验）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigResponse(

        /** 智能体稳定键（main=主智能体 / executor=run 执行体） */
        @Schema(description = "智能体稳定键（main=主智能体 / executor=run 执行体；未知键 404 "
                + "PRJ_033＝数字业务码 4033）", example = "main")
        String agentKey,

        /** 展示名 */
        @Schema(description = "展示名", example = "主智能体")
        String agentName,

        /** 生效 systemPrompt（库覆盖值或枚举默认——装配实取值，对照 systemPromptOverridden 知来源） */
        @Schema(description = "生效 systemPrompt（库覆盖值或枚举默认——装配实取值，"
                + "对照 systemPromptOverridden 知来源）")
        String systemPrompt,

        /** 生效模型档位裸名（如 deepseek-v4-flash） */
        @Schema(description = "生效模型档位裸名（如 deepseek-v4-flash）", example = "deepseek-v4-flash")
        String modelId,

        /** systemPrompt 是否库覆盖值（true＝覆盖；false＝枚举默认） */
        @Schema(description = "systemPrompt 是否库覆盖值（true＝覆盖；false＝枚举默认）")
        boolean systemPromptOverridden,

        /** 模型档位是否库覆盖值（true＝覆盖；false＝枚举默认） */
        @Schema(description = "模型档位是否库覆盖值（true＝覆盖；false＝枚举默认）")
        boolean modelIdOverridden,

        /** 枚举默认 systemPrompt（预览：清空覆盖即落此值——恢复默认前可先看落点） */
        @Schema(description = "枚举默认 systemPrompt（预览：清空覆盖即落此值——身份与配置分治的缺省正本）")
        String defaultSystemPrompt,

        /** 枚举默认模型档位（清空覆盖即落此值） */
        @Schema(description = "枚举默认模型档位（清空覆盖即落此值）", example = "deepseek-v4-flash")
        String defaultModelId,

        /** 联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开） */
        @Schema(description = "联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开）")
        boolean webSearchEnabled,

        /** 网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开） */
        @Schema(description = "网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开）")
        boolean fetchUrlEnabled,

        /** 最近写者 id（admin 侧 TSID；null＝从未配置） */
        @Schema(description = "最近写者 id（admin 侧 TSID；null＝从未配置）", example = "700200")
        String operatorId,

        /** 最近写者名（直读展示） */
        @Schema(description = "最近写者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 最近写入时刻（null＝从未配置） */
        @Schema(description = "最近写入时刻（null＝从未配置）")
        LocalDateTime updatedAt
) {
}
