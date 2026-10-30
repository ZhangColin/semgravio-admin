package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * aiplatform 后台技能草稿详情的 wire 镜像——与 aiplatform
 * {@code BackofficeSkillDraftDetailResponse}（#259 审核面）字段同构：清单行同形字段＋
 * 正文全文＋扫描 findings 逐条留档＋终态留痕四字段。人审判读承载面（审核口径见 provider
 * 侧 docs/agents/skill-audit-guide.md——真伪/重叠/description 三要素/血统可溯）。
 *
 * <p><strong>任意状态可查</strong>（终态留档）；终态留痕四字段（{@code operatorId}/
 * {@code operatorName}/{@code reviewedAt}/{@code rejectReason}）已晋升/已拒绝草稿回填、
 * <strong>在途恒 null</strong>。{@code scanFindings} 为
 * <strong>{@code List<Map<String, Object>>}</strong>——provider 源即 untyped
 * （字段名 patternId/severity/category/file/line/matchText/description 仅存在于描述层），
 * admin 照 untyped 镜像<strong>不造 typed record</strong>（对齐 frontmatter 自由键值先例；
 * 区别于 precheck hint 的 typed record——那边 provider 源本就 typed）；空列表＝无发现。</p>
 *
 * @param id           草稿柄（TSID 十进制串，同清单行）
 * @param name         技能名
 * @param description  简介
 * @param slot         来源槽位稳定键（main/executor/subagent）
 * @param projectId    来源项目 id（血统三件之一）
 * @param runId        来源 run 标识（血统三件之一）
 * @param scanVerdict  写入前静态扫描判定（SAFE/CAUTION）
 * @param status       状态 code（1=在途 2=已晋升 3=已拒绝）
 * @param statusName   状态名（直读展示）
 * @param createdAt    自荐时刻
 * @param content      技能正文全文（自荐只有正文——a-only 无 scripts）
 * @param scanFindings 扫描 findings 留档（空列表＝无发现；untyped map 逐字透传）
 * @param operatorId   终态审核操作者 id（在途为 null）
 * @param operatorName 终态审核操作者名（在途为 null）
 * @param reviewedAt   终态时刻（在途为 null）
 * @param rejectReason 拒绝理由（已拒绝态携带；其余 null）
 */
public record AiplatformSkillDraftDetailWireResponse(
        String id,
        String name,
        String description,
        String slot,
        String projectId,
        String runId,
        String scanVerdict,
        Integer status,
        String statusName,
        LocalDateTime createdAt,
        String content,
        List<Map<String, Object>> scanFindings,
        String operatorId,
        String operatorName,
        LocalDateTime reviewedAt,
        String rejectReason
) {
}
