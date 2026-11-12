package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台沉淀草稿标注详情——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSedimentDraftDetailResponse}（#464「数据标注」面，issue #124 镜像）：
 * 队列行同形字段＋正文全文＋载荷＋扫描回执全文＋终态留痕字段。「数据标注」人审判读
 * 承载面（真伪/重叠/血统可溯）；<strong>任意状态可查</strong>（终态留档）。
 *
 * <p>{@code content} 正文全文 markdown——激活<strong>原样升格不改稿</strong>（审核面
 * 所见即入库内容；升格 knw_materials 随激活链）——语义陷阱：素材检索在升格端点落地前
 * 不命中已激活沉淀，标注面文案不得暗示「激活即可被检索」。{@code payload} untyped
 * {@code Map}（一期恒空面，kind 开放生长面）；{@code scanFindings} untyped
 * {@code List<Map>} 逐条留档（patternId/severity/category/file/line/matchText/
 * description，空列表＝无发现——provider 源即 untyped，照 untyped 透传不造 typed
 * record）。终态留痕四字段在途恒 null、三键审结后回填（operatorId/operatorName＝
 * 标注操作者，经 X-User-* 透传头落痕）。草稿不存在（含畸形/未寻址 TSID）404 KNW_008
 * （数字业务码 2008）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSedimentDraftDetailResponse(

        /** 草稿柄（TSID 十进制串，勿做数值假设） */
        @Schema(description = "草稿柄（TSID 十进制串，勿做数值假设——畸形/未寻址由 provider"
                + "判 404 KNW_008＝数字业务码 2008）", example = "3853000001111111")
        String id,

        /** 沉淀物类别（CASE=需求案例 MODULE=功能模块 RULE=业务规则） */
        @Schema(description = "沉淀物类别（CASE=需求案例 MODULE=功能模块 RULE=业务规则）",
                example = "CASE")
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

        /** 在途序号 1-3（终态翻行即释放名额；改标签跨类别占目标类别在途序号最低空位） */
        @Schema(description = "在途序号 1-3（每项目每 kind 在途上限 3 的结构性承载；"
                + "终态翻行即释放名额）", example = "1")
        Integer inflightSlot,

        /** 状态 code（1=在途 2=已激活 3=已拒绝） */
        @Schema(description = "状态 code（1=在途 2=已激活 3=已拒绝）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** 自荐时刻 */
        @Schema(description = "自荐时刻")
        LocalDateTime createdAt,

        /** 正文全文 markdown（激活升格时原样入库——审核者不改稿，所见即入库内容） */
        @Schema(description = "正文全文 markdown（激活升格时原样入库——审核者不改稿，"
                + "所见即入库内容）")
        String content,

        /** 扩展载荷（一期恒空面；kind 开放＋载荷 JSONB 生长面，untyped 透传） */
        @Schema(description = "扩展载荷（一期恒空面；kind 开放＋载荷 JSONB 生长面——"
                + "untyped 逐字透传不做形状断言）")
        Map<String, Object> payload,

        /** 扫描 findings 留档（untyped 逐条；空列表＝无发现） */
        @Schema(description = "扫描 findings 留档：每条 patternId/severity/category/file/"
                + "line/matchText/description（空列表＝无发现——untyped 逐字透传）")
        List<Map<String, Object>> scanFindings,

        /** 终态审核操作者 id（在途为 null） */
        @Schema(description = "终态审核操作者 id（在途为 null——三键审结后回填）")
        String operatorId,

        /** 终态审核操作者名（在途为 null） */
        @Schema(description = "终态审核操作者名（在途为 null——直读展示）")
        String operatorName,

        /** 终态时刻（在途为 null） */
        @Schema(description = "终态时刻（在途为 null）")
        LocalDateTime reviewedAt,

        /** 拒绝理由（已拒绝态携带；其余 null） */
        @Schema(description = "拒绝理由（已拒绝态携带；其余 null——终态留档拒绝须有据）")
        String rejectReason
) {
}
