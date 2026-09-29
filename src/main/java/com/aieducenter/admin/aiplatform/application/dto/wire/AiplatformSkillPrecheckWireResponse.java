package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 指派双头预检回执的 wire 镜像——与 aiplatform
 * {@code BackofficeSkillPrecheckResponse}（#255 指派双头预检）字段同构：
 * {@code executed}＋{@code hints} 两件同读。
 *
 * <p>两态纪律（BFF 照透传不解释）：{@code executed}=true 且 hints 空＝确认无重叠；
 * executed=true 且 hints 非空＝发现方法论重叠（逐条四字段）；executed=false＝<strong>未执行</strong>
 * （判定失败/超时/输出不可解析）——不得当无重叠呈现。非阻断：提示是输入不是门，预检结果
 * 不拦 PUT 指派、组件故障不阻塞指派照常提交（裁决全在 provider，admin 不加戏）。</p>
 *
 * <p>出站/入站口径：{@code executed} 装箱 Boolean 透传（provider 侧 primitive boolean 恒非
 * null）；{@code hints} 恒至少空表（provider 构造器归一——null 进 List.of()）；hint 三 String
 * 字段（counterpart/overlap/resolution）经模型输出解析<strong>可 null</strong>——照透传
 * （全局 Jackson 出 JSON null），不臆造兜底文案。</p>
 *
 * @param executed 判定是否已执行（false＝未执行——降级态，与「无重叠的成功态」靠此区分）
 * @param hints    方法论重叠提示列表（恒至少空表；空＋executed=true＝确认无重叠）
 */
public record AiplatformSkillPrecheckWireResponse(
        Boolean executed,
        List<PrecheckHint> hints
) {

    /**
     * 单条方法论重叠提示——与 aiplatform {@code BackofficeSkillPrecheckResponse.PrecheckHint}
     * 字段同构：涉及哪些技能（skills——候选侧技能名）、与什么重叠（counterpart——「工作协议」
     * 或对侧技能名）、重叠内容（overlap）、消解方向（resolution）。三 String 字段可 null
     * （模型输出解析态），照透传不兜底。
     *
     * @param skills      涉及的候选技能名清单
     * @param counterpart 对照侧（「工作协议」或对侧技能名；可 null）
     * @param overlap     重叠内容一句话（可 null）
     * @param resolution  消解方向（如二选一、收窄 description、可不指派；可 null）
     */
    public record PrecheckHint(
            List<String> skills,
            String counterpart,
            String overlap,
            String resolution
    ) {
    }
}
