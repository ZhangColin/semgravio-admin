package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置读面——北向出口，逐字镜像 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5，spec #79 定稿、issue #84；B3 issue #112 增路由
 * 强档三件——镜像 provider #367 落地后的 B3 面）：生效值＋覆盖标记＋枚举默认预览＋增强
 * 工具开关存储态＋<strong>路由强档三件</strong>＋最近写者。GET 读面与 PUT 回执同形
 * （provider 写入后重读同一视图）。
 *
 * <p>生效值＝库覆盖值或枚举默认（装配实取值——对照覆盖标记知来源）；默认预览即清空覆盖的
 * 落点（恢复默认前可先看）。无覆盖行＝全回落：覆盖标记 false、写者两肢与 updatedAt null
 * 如实出 JSON null（全局 Jackson 含 null）。覆盖标记与开关四件 provider 恒在场（原生
 * boolean，admin 同型镜像）。modelId/routingStrongModelId 语义＝<strong>档位键</strong>
 * （ADR-0031 语义键如 flash/pro——V44 存量已刷档，非裸模型名；写口即时校验须在模型档位面
 * 有行且启用，缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075）。寻址稳定键三把：
 * main=主智能体 / executor=run 执行体 / designer=设计执行体——边缘调用点（naming 等）走
 * /edge/{edgeKey} 档位端点，三座端点对边缘键 404 PRJ_033＝数字业务码 4033（admin 字符串
 * 透传不预校验）。provider 读面另带路由开关/两阈值六件（#354），B3 票面只点名
 * routingStrongModelId 一族——六件不镜像（读面不呈现）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigResponse(

        /** 智能体稳定键（main=主智能体 / executor=run 执行体 / designer=设计执行体） */
        @Schema(description = "智能体稳定键（main=主智能体 / executor=run 执行体 / "
                + "designer=设计执行体；边缘调用点走 /edge/{edgeKey}；未知键 404 "
                + "PRJ_033＝数字业务码 4033）", example = "main")
        String agentKey,

        /** 展示名 */
        @Schema(description = "展示名", example = "主智能体")
        String agentName,

        /** 生效 systemPrompt（库覆盖值或枚举默认——装配实取值，对照 systemPromptOverridden 知来源） */
        @Schema(description = "生效 systemPrompt（库覆盖值或枚举默认——装配实取值，"
                + "对照 systemPromptOverridden 知来源）")
        String systemPrompt,

        /** 生效模型档位键（ADR-0031 语义键，如 flash/pro） */
        @Schema(description = "生效模型档位键（ADR-0031 语义键，如 flash/pro——档位表"
                + "/model-tiers 为值域正本；对照 modelIdOverridden 知来源）", example = "pro")
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

        /** 枚举默认模型档位键（清空覆盖即落此值） */
        @Schema(description = "枚举默认模型档位键（清空覆盖即落此值）", example = "flash")
        String defaultModelId,

        /** 联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开） */
        @Schema(description = "联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开）")
        boolean webSearchEnabled,

        /** 网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开） */
        @Schema(description = "网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开）")
        boolean fetchUrlEnabled,

        /** 强模型档生效值（路由升档落点；库覆盖值或代码缺省——对照 routingStrongModelIdOverridden 知来源） */
        @Schema(description = "强模型档生效值（#354 路由升档落点；库覆盖值或代码缺省——"
                + "对照 routingStrongModelIdOverridden 知来源；主档/强档同范式管理）",
                example = "pro")
        String routingStrongModelId,

        /** 强模型档是否库覆盖值（true＝覆盖；false＝代码缺省 pro） */
        @Schema(description = "强模型档是否库覆盖值（true＝覆盖；false＝代码缺省 pro）")
        boolean routingStrongModelIdOverridden,

        /** 代码缺省强模型档（预览：清空覆盖即落此值） */
        @Schema(description = "代码缺省强模型档（预览：清空覆盖即落此值）", example = "pro")
        String defaultRoutingStrongModelId,

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
