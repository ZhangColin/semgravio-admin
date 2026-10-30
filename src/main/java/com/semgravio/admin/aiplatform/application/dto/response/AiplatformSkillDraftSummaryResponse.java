package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能草稿清单行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSkillDraftSummaryResponse}（#259 自产线，ADR-0022 库制草稿）：
 * 智能体自荐技能草稿的<strong>在途活跃面</strong>（人审队列）——跨项目全部在途草稿按
 * 自荐时间倒序（最近先），终态（已晋升/已拒绝）不列活跃面、详情仍可按 id 查。
 * 草稿不参与任何装配（未审内容不影响任何 run）。
 *
 * <p>{@code id} 为 TSID 十进制串——BFF 字符串透传不解析不预校验（勿做数值假设）；
 * 血统三件（来源项目/run/槽位）为审核溯源锚；{@code scanVerdict} 字符串透传不映射枚举；
 * {@code status} Integer code＋{@code statusName} 中文名随行。清单<strong>不分页</strong>
 * （provider 有界人审队列天然瓶颈——对齐技能清单「有界目录不造分页」先例，ADR-0012
 * 不适用于无分页端点）。错误面仅 401 签名（无业务码）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillDraftSummaryResponse(

        /** 草稿柄（详情寻址，TSID 十进制串，勿做数值假设；畸形/未寻址由 provider 判 404 SKL_015） */
        @Schema(description = "草稿柄（详情/审核操作寻址，TSID 十进制串，勿做数值假设——"
                + "畸形/未寻址由 provider 判 404 SKL_015＝数字业务码 7015）",
                example = "3852400001111111")
        String id,

        /** 技能名（撞名判断键——晋升时撞名复查） */
        String name,

        /** 简介 */
        String description,

        /** 来源槽位稳定键（main=主智能体 executor=run 执行体 subagent=子智能体） */
        @Schema(description = "来源槽位稳定键（main=主智能体 executor=run 执行体 "
                + "subagent=子智能体）", example = "executor")
        String slot,

        /** 来源项目 id（血统——审核溯源与项目删除清理入口） */
        @Schema(description = "来源项目 id（血统——审核溯源与项目删除清理入口）")
        String projectId,

        /** 来源 run 标识（血统——外部资料接触史等 run 级溯源锚） */
        @Schema(description = "来源 run 标识（血统——外部资料接触史等 run 级溯源锚，"
                + "审核必查项见 provider 侧 skill-audit-guide）")
        String runId,

        /** 写入前静态扫描判定（SAFE/CAUTION；DANGEROUS 已在写入口拒收不落库） */
        @Schema(description = "写入前静态扫描判定（SAFE/CAUTION——DANGEROUS 已在写入口"
                + "拒收不落库）", example = "SAFE")
        String scanVerdict,

        /** 状态 code（1=在途 2=已晋升 3=已拒绝；列表＝活跃面恒 1） */
        @Schema(description = "状态 code（1=在途 2=已晋升 3=已拒绝；列表＝活跃面恒 1——"
                + "终态不列活跃面、详情仍可查）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** 自荐时刻（清单按此刻倒序——最近先） */
        @Schema(description = "自荐时刻（清单按此刻倒序——最近先）")
        LocalDateTime createdAt
) {
}
