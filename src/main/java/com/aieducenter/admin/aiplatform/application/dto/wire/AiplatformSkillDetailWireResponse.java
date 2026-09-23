package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.util.Map;

/**
 * aiplatform 后台技能详情的 wire 镜像——与 aiplatform {@code BackofficeSkillDetailResponse}
 * （#247 技能库，审核面）字段同构：清单行同形元数据＋技能全文——frontmatter（解析态全量）
 * 与正文（frontmatter 剥离后）分面可读，所见即运行时注入面（运营读全文判安装风险）。
 *
 * <p>{@code frontmatter} 为自由键值对象逐字透传（不做形状断言，spec #79）；{@code content}
 * 为 SKILL.md 正文全文（不经任何截断/改写）。{@code id} 同清单行 opaque 串两形制
 * （内置 {@code builtin:<技能名>}／安装 TSID 十进制串）。</p>
 *
 * @param id              技能柄（opaque 串两形制，同清单行）
 * @param name            技能名
 * @param description     简介
 * @param source          来源 code（1=内置 2=安装）
 * @param sourceName      来源名（直读展示）
 * @param sourcePackage   来源包标识（内置为 null）
 * @param version         版本标识（装时 commit；内置为 null）
 * @param status          状态 code（1=启用 2=停用；内置恒 1）
 * @param statusName      状态名（直读展示）
 * @param frontmatter     SKILL.md frontmatter 全量（解析态键值，含 name/description）
 * @param content         SKILL.md 正文（frontmatter 剥离后全文，审核承载面）
 * @param operatorId      最近管理动作操作者 id（安装＝装者、启停＝最近动作者；内置为 null）
 * @param operatorName    最近管理动作操作者名（直读展示；内置为 null）
 * @param updateAvailable 远端有新版标记（来源包级，语义同清单行；内置恒 null）
 */
public record AiplatformSkillDetailWireResponse(
        String id,
        String name,
        String description,
        Integer source,
        String sourceName,
        String sourcePackage,
        String version,
        Integer status,
        String statusName,
        Map<String, Object> frontmatter,
        String content,
        String operatorId,
        String operatorName,
        Boolean updateAvailable
) {
}
