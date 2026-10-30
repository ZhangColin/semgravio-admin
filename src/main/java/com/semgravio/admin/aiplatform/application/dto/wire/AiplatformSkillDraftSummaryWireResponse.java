package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 后台技能草稿清单行的 wire 镜像——与 aiplatform
 * {@code BackofficeSkillDraftSummaryResponse}（#259 自产线，ADR-0022 库制草稿）字段同构：
 * 智能体自荐技能草稿的<strong>在途活跃面</strong>（人审队列——终态已晋升/已拒绝不列、
 * 详情仍可按 id 查），跨项目按自荐时间倒序（最近先）。
 *
 * <p>{@code id} 为 TSID 十进制串——admin 按字符串透传不解析不预校验（spec #79 纪律，
 * 同技能库柄口径），畸形/未寻址由 provider 判 404 SKL_015。血统三件（{@code projectId}/
 * {@code runId}/{@code slot}）为审核溯源锚；{@code scanVerdict} 字符串透传不映射枚举
 * （SAFE/CAUTION——DANGEROUS 已在 provider 写入口拒收不落库）；{@code status} Integer code
 * （1=在途 2=已晋升 3=已拒绝；列表＝活跃面恒 1）＋{@code statusName} 中文名随行（provider
 * 出口提供，BFF 透传不臆造映射）。</p>
 *
 * @param id          草稿柄（详情寻址，TSID 十进制串，勿做数值假设）
 * @param name        技能名（撞名判断键——晋升时撞名复查）
 * @param description 简介
 * @param slot        来源槽位稳定键（main=主智能体 executor=run 执行体 subagent=子智能体）
 * @param projectId   来源项目 id（血统——审核溯源与项目删除清理入口）
 * @param runId       来源 run 标识（血统——外部资料接触史等 run 级溯源锚）
 * @param scanVerdict 写入前静态扫描判定（SAFE/CAUTION；DANGEROUS 已在写入口拒收不落库）
 * @param status      状态 code（1=在途 2=已晋升 3=已拒绝；列表＝活跃面恒 1）
 * @param statusName  状态名（直读展示）
 * @param createdAt   自荐时刻
 */
public record AiplatformSkillDraftSummaryWireResponse(
        String id,
        String name,
        String description,
        String slot,
        String projectId,
        String runId,
        String scanVerdict,
        Integer status,
        String statusName,
        LocalDateTime createdAt
) {
}
