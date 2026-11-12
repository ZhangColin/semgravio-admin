package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 后台沉淀草稿标注队列行的 wire 镜像——与 aiplatform
 * {@code BackofficeSedimentDraftSummaryResponse}（#464「数据标注」面）字段同构：
 * 分诊面一行＝类别/标题/摘要＋血统三件（来源项目/run/槽位）＋扫描判定＋在途序号＋
 * 状态＋时间；正文与 findings 全文在详情。
 *
 * <p>{@code id} 为 TSID 十进制串（provider 顶层 Long→string 口径，消费方按 opaque 串
 * 回传——BFF 不解析不预校验）；{@code kind} 类别稳定键字符串透传（CASE/MODULE/RULE）
 * 不映射枚举；{@code scanVerdict} 字符串透传（SAFE/CAUTION——DANGEROUS 写入口已拒收
 * 不落库）；{@code inflightSlot} 在途序号 1-3（每项目每 kind 在途上限 3 的结构性承载
 * ——队列拥挤度可视化锚，终态翻行即释放名额）；{@code status} Integer code（1=在途
 * 2=已激活 3=已拒绝）＋{@code statusName} 中文名随行。队列<strong>含终态行</strong>
 * （status 缺省＝全量——终态留档可查；待办视角须显式 status=1）。</p>
 *
 * @param id           草稿柄（详情/三键寻址，TSID 十进制串）
 * @param kind         沉淀物类别（CASE/MODULE/RULE）
 * @param title        标题
 * @param summary      一句话摘要
 * @param slot         来源槽位稳定键（main/executor/subagent）
 * @param projectId    来源项目 id（血统——软引用，项目删除后未终结草稿随清）
 * @param runId        来源 run 标识（血统溯源锚）
 * @param scanVerdict  扫描判定（SAFE/CAUTION；DANGEROUS 拒写不落库）
 * @param inflightSlot 在途序号 1-3（每项目每 kind 上限 3 的结构性承载）
 * @param status       状态 code（1=在途 2=已激活 3=已拒绝）
 * @param statusName   状态名（直读展示）
 * @param createdAt    自荐时刻（队列按此刻倒序——最近先）
 */
public record AiplatformSedimentDraftSummaryWireResponse(
        String id,
        String kind,
        String title,
        String summary,
        String slot,
        String projectId,
        String runId,
        String scanVerdict,
        Integer inflightSlot,
        Integer status,
        String statusName,
        LocalDateTime createdAt
) {
}
