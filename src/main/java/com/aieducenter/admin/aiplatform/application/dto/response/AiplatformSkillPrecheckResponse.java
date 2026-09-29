package com.aieducenter.admin.aiplatform.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台指派双头预检回执——北向出口，逐字镜像 aiplatform
 * {@code BackofficeSkillPrecheckResponse}（#255 指派双头预检）：{@code executed}＋{@code hints}
 * 两件同读。PUT 指派提交前的非阻断方法论重叠检查回执（提示是输入不是门，判定权留管理员）。
 *
 * <p><strong>两态纪律（admin-web 消费契约）</strong>：executed=true 且 hints 空＝确认无重叠；
 * executed=true 且 hints 非空＝发现方法论重叠（逐条四字段，管理员判断后仍可提交）；
 * executed=false＝<strong>未执行</strong>（判定失败/超时/输出不可解析，hints 恒空）——不得当
 * 无重叠呈现。非阻断：预检结果不拦 PUT 指派、预检组件故障不阻塞指派照常提交。</p>
 *
 * <p>{@code executed} 装箱 Boolean 透传（provider 侧 primitive boolean 恒非 null）；
 * {@code hints} 恒至少空表（provider 构造器归一）；hint 三 String 字段可 null（模型输出
 * 解析态）——全局 Jackson 出 JSON null 如实在场，前端按 null 分支呈现、不臆造兜底文案。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillPrecheckResponse(

        /** 判定是否已执行（true＝判定已执行；false＝未执行——判定失败/超时/输出不可解析，不得当无重叠呈现） */
        @Schema(description = "判定是否已执行（true＝判定已执行——hints 空即确认无重叠；"
                + "false＝未执行：判定失败/超时/输出不可解析，hints 恒空——<strong>不得当"
                + "「确认无重叠」呈现</strong>，如实标注「预检未执行」；非阻断：预检结果"
                + "不拦 PUT 指派，预检组件故障不阻塞指派照常提交）",
                example = "true")
        Boolean executed,

        /** 方法论重叠提示列表（恒至少空表；每条四字段——skills/counterpart/overlap/resolution） */
        @Schema(description = "方法论重叠提示列表（恒至少空表；空＋executed=true＝确认无重叠）。"
                + "每条：skills 涉及的候选技能名、counterpart 对照侧（「工作协议」或对侧技能名）、"
                + "overlap 重叠内容、resolution 消解方向；三 String 字段可 null（模型输出解析态，"
                + "出 JSON null 如实在场）")
        List<PrecheckHint> hints
) {

    /**
     * 单条方法论重叠提示——逐字镜像 aiplatform
     * {@code BackofficeSkillPrecheckResponse.PrecheckHint}：涉及哪些技能（skills——候选侧
     * 技能名）、与什么重叠（counterpart——「工作协议」或对侧技能名）、重叠内容（overlap）、
     * 消解方向（resolution）。三 String 字段可 null（模型输出解析态），照透传不兜底。
     */
    public record PrecheckHint(

        /** 涉及的候选技能名清单 */
        @Schema(description = "涉及的候选技能名清单", example = "[\"报告生成\"]")
        List<String> skills,

        /** 对照侧（「工作协议」或对侧技能名；可 null——模型输出解析态） */
        @Schema(description = "对照侧（「工作协议」或对侧技能名；可 null——模型输出解析态）",
                example = "工作协议")
        String counterpart,

        /** 重叠内容一句话（可 null——模型输出解析态） */
        @Schema(description = "重叠内容一句话（可 null——模型输出解析态）",
                example = "都在 PRD 产出环节做结构化写作，方法论高度重叠")
        String overlap,

        /** 消解方向（如二选一、收窄 description、可不指派；可 null——模型输出解析态） */
        @Schema(description = "消解方向（如二选一、收窄 description、可不指派；可 null——"
                + "模型输出解析态）", example = "二选一或收窄 description")
        String resolution
    ) {
    }
}
