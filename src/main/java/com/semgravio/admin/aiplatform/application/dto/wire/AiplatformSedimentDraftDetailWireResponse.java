package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * aiplatform 后台沉淀草稿标注详情的 wire 镜像——与 aiplatform
 * {@code BackofficeSedimentDraftDetailResponse}（#464「数据标注」面）字段同构：
 * 队列行同形字段＋正文全文＋载荷＋扫描回执全文＋终态留痕字段（激活/拒绝后回填，
 * 在途恒 null）。审核口径：激活<strong>原样升格不改稿</strong>（审核面所见即入库
 * 内容）、拒绝理由必填、改标签只调类别。
 *
 * <p>{@code payload} 为 untyped {@code Map<String, Object>}（一期恒空面；kind 开放＋
 * 载荷 JSONB 生长面）——照 untyped 透传不造 typed record（对齐技能草稿 scanFindings/
 * frontmatter 先例）；{@code scanFindings} 同为 untyped {@code List<Map<String, Object>>}
 * （每条 patternId/severity/category/file/line/matchText/description，空列表＝无发现）。
 * <strong>任意状态可查</strong>（终态留档）；终态留痕四字段（操作者两肢/时刻/拒绝理由）
 * 在途恒 null、已激活/已拒绝回填。</p>
 *
 * @param id           草稿柄（TSID 十进制串）
 * @param kind         沉淀物类别（CASE/MODULE/RULE）
 * @param title        标题
 * @param summary      一句话摘要
 * @param slot         来源槽位稳定键（main/executor/subagent）
 * @param projectId    来源项目 id（软引用）
 * @param runId        来源 run 标识（血统溯源锚）
 * @param scanVerdict  扫描判定（SAFE/CAUTION）
 * @param inflightSlot 在途序号 1-3（终态翻行即释放名额）
 * @param status       状态 code（1=在途 2=已激活 3=已拒绝）
 * @param statusName   状态名（直读展示）
 * @param createdAt    自荐时刻
 * @param content      正文全文 markdown（激活升格时原样入库——审核者不改稿）
 * @param payload      扩展载荷（一期恒空面；kind 开放生长面，untyped 透传）
 * @param scanFindings 扫描 findings 留档（untyped 逐条；空列表＝无发现）
 * @param operatorId   终态审核操作者 id（在途为 null）
 * @param operatorName 终态审核操作者名（在途为 null）
 * @param reviewedAt   终态时刻（在途为 null）
 * @param rejectReason 拒绝理由（已拒绝态携带；其余 null）
 */
public record AiplatformSedimentDraftDetailWireResponse(
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
        LocalDateTime createdAt,
        String content,
        Map<String, Object> payload,
        List<Map<String, Object>> scanFindings,
        String operatorId,
        String operatorName,
        LocalDateTime reviewedAt,
        String rejectReason
) {
}
