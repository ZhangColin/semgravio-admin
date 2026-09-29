package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能草稿详情——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSkillDraftDetailResponse}（#259 审核面）：清单行同形字段＋正文全文＋
 * 扫描 findings 逐条留档＋终态留痕四字段，人审判读承载面（审核口径见 provider 侧
 * docs/agents/skill-audit-guide.md——真伪/重叠/description 三要素/血统可溯：来源 run
 * 的外部资料接触史必查）。
 *
 * <p><strong>任意状态可查</strong>（终态留档——拒绝回执即本形状直读）；草稿不存在
 * （含未寻址/畸形 TSID）404 SKL_015＝数字业务码 7015（provider 裁决，admin 不预检）。
 * {@code scanFindings} 为 untyped map 清单逐字透传（provider 源即
 * {@code List<Map<String, Object>>}——字段名仅存在于描述层，BFF 不造 typed record）；
 * 空列表＝无发现。终态留痕四字段在途恒 null（全局 Jackson 出 JSON null 如实在场）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillDraftDetailResponse(

        /** 草稿柄（TSID 十进制串，同清单行 id） */
        @Schema(description = "草稿柄（TSID 十进制串，同清单行 id——详情/审核操作寻址）",
                example = "3852400001111111")
        String id,

        /** 技能名 */
        String name,

        /** 简介 */
        String description,

        /** 来源槽位稳定键（main/executor/subagent） */
        @Schema(description = "来源槽位稳定键（main=主智能体 executor=run 执行体 "
                + "subagent=子智能体）", example = "executor")
        String slot,

        /** 来源项目 id（血统三件之一） */
        @Schema(description = "来源项目 id（血统三件之一——审核溯源与项目删除清理入口）")
        String projectId,

        /** 来源 run 标识（血统三件之一） */
        @Schema(description = "来源 run 标识（血统三件之一——外部资料接触史等 run 级溯源锚）")
        String runId,

        /** 写入前静态扫描判定（SAFE/CAUTION） */
        @Schema(description = "写入前静态扫描判定（SAFE/CAUTION——DANGEROUS 已在写入口"
                + "拒收不落库）", example = "CAUTION")
        String scanVerdict,

        /** 状态 code（1=在途 2=已晋升 3=已拒绝；任意状态可查——终态留档） */
        @Schema(description = "状态 code（1=在途 2=已晋升 3=已拒绝；任意状态可查——"
                + "终态留档）", example = "1")
        Integer status,

        /** 状态名（直读展示） */
        String statusName,

        /** 自荐时刻 */
        @Schema(description = "自荐时刻")
        LocalDateTime createdAt,

        /** 技能正文全文（自荐只有正文——a-only 无 scripts；判读主体，不经截断/改写） */
        @Schema(description = "技能正文全文（自荐只有正文——a-only 无 scripts；判读主体，"
                + "不经截断/改写——审核者不改稿，采纳原样入池）")
        String content,

        /** 扫描 findings 留档（空列表＝无发现；untyped map 逐字透传） */
        @Schema(description = "扫描 findings 留档（空列表＝无发现）：每条 patternId/severity/"
                + "category/file/line/matchText/description（provider 源即 untyped map——"
                + "字段名仅存在于描述层，逐字透传不做形状断言）")
        List<Map<String, Object>> scanFindings,

        /** 终态审核操作者 id（在途为 null） */
        @Schema(description = "终态审核操作者 id（已晋升/已拒绝草稿回填；在途为 null）",
                example = "700200")
        String operatorId,

        /** 终态审核操作者名（在途为 null） */
        @Schema(description = "终态审核操作者名（直读展示；在途为 null）",
                example = "运营·技能审核员")
        String operatorName,

        /** 终态时刻（在途为 null） */
        @Schema(description = "终态时刻（晋升/拒绝落定时刻；在途为 null）")
        LocalDateTime reviewedAt,

        /** 拒绝理由（已拒绝态携带；其余 null） */
        @Schema(description = "拒绝理由（已拒绝态携带——终态留档拒绝须有据；其余 null）",
                example = "正文与现有技能 prd-writing 方法论重叠，解消方向不明")
        String rejectReason
) {
}
