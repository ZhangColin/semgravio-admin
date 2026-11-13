package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台沉淀草稿标注队列行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSedimentDraftSummaryResponse}（#464「数据标注」面，issue #124 镜像）：
 * 采集层自荐的沉淀草稿分诊面（人审队列），按自荐时间倒序（最近先，provider 定死）。
 *
 * <p>{@code id} 为 TSID 十进制串——BFF 字符串透传不解析不预校验（勿做数值假设）；血统
 * 三件（来源项目/run/槽位）为审核溯源锚；{@code scanVerdict} 字符串透传不映射枚举；
 * {@code inflightSlot} 在途序号 1-3——每项目每 kind 在途上限 3 的<strong>结构性承载</strong>
 * （队列拥挤度可视化锚；改标签跨类别占目标类别在途序号，终态翻行即释放名额）；
 * {@code status} Integer code＋{@code statusName} 中文名随行。队列<strong>分页 1-based
 * 含终态行</strong>（status 缺省＝全量——终态留档可查；待办视角须显式 status=1）。
 * 错误面：kind 非法 400 KNW_011（2011）、未知 status code 骑 provider 框架 400 带合法
 * 取值表、缺签名 401（机机签名由 BFF 出站承担）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSedimentDraftSummaryResponse(

        /** 草稿柄（详情/三键寻址，TSID 十进制串，勿做数值假设；畸形/未寻址由 provider 判 404 KNW_008） */
        @Schema(description = "草稿柄（详情/三键寻址，TSID 十进制串，勿做数值假设——畸形/未寻址"
                + "由 provider 判 404 KNW_008＝数字业务码 2008）",
                example = "3853000001111111")
        String id,

        /** 沉淀物类别（CASE=需求案例 MODULE=功能模块 RULE=业务规则） */
        @Schema(description = "沉淀物类别（CASE=需求案例 MODULE=功能模块 RULE=业务规则；"
                + "改标签跨类别占目标类别在途名额）", example = "CASE")
        String kind,

        /** 标题 */
        String title,

        /** 一句话摘要 */
        String summary,

        /** 来源槽位稳定键（main=主智能体 executor=run 执行体 subagent=子智能体） */
        @Schema(description = "来源槽位稳定键（main=主智能体 executor=run 执行体 "
                + "subagent=子智能体）", example = "executor")
        String slot,

        /** 来源项目 id（血统——审核溯源与项目删除清理入口） */
        @Schema(description = "来源项目 id（血统——审核溯源与项目删除清理入口）")
        String projectId,

        /** 来源 run 标识（血统——run 级溯源锚） */
        @Schema(description = "来源 run 标识（血统——run 级溯源锚）")
        String runId,

        /** 写入前静态扫描判定（SAFE/CAUTION；DANGEROUS 已在写入口拒收不落库） */
        @Schema(description = "写入前静态扫描判定（SAFE/CAUTION——DANGEROUS 已在写入口"
                + "拒收不落库）", example = "SAFE")
        String scanVerdict,

        /** 在途序号 1-3（每项目每 kind 在途上限 3 的结构性承载——队列拥挤度可视化锚） */
        @Schema(description = "在途序号 1-3（每项目每 kind 在途上限 3 的结构性承载——"
                + "队列拥挤度可视化锚；终态翻行即释放名额）", example = "1")
        Integer inflightSlot,

        /** 状态 code（1=在途 2=已激活 3=已拒绝；缺省队列含终态行——待办视角须显式 status=1） */
        @Schema(description = "状态 code（1=在途 2=已激活 3=已拒绝；队列缺省含终态行"
                + "——终态留档可查，待办视角须显式 status=1）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** 自荐时刻（队列按此刻倒序——最近先） */
        @Schema(description = "自荐时刻（队列按此刻倒序——最近先，provider 定死）")
        LocalDateTime createdAt
) {
}
