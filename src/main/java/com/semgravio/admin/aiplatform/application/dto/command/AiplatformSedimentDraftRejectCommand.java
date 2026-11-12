package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台沉淀草稿拒绝北向命令——「数据标注」三键之一（issue #124，镜像 aiplatform #464
 * {@code SedimentDraftRejectCommand}）：理由必填（终态留档拒绝须有据，库级 CHECK 双锁）。
 * 拒绝即终态：不改稿不复活——重提只能靠未来会话产生新草稿。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillDraftRejectCommand}
 * 技能草稿先例）：理由合法性由 provider 拒绝用例裁决（空白 400 KNW_010＝数字业务码
 * 2010），错误信封原样透传——BFF 不加戏不重复校验（可选绑定：缺 body 不前置拦截，
 * 直出 provider 裁决）。操作者身份不在命令体——经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（缺头由 provider 域面守卫 KNW_006＝数字
 * 业务码 2006 拦，admin 不拦——标注动作必留痕，无落空通道）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSedimentDraftRejectCommand(

        /** 拒绝理由（终态留档可查——拒绝须有据；空白即 400 KNW_010，由 provider 裁决不前置拦截） */
        @Schema(description = "拒绝理由（终态留档可查——拒绝须有据；空白即 400 KNW_010＝"
                + "数字业务码 2010，由 provider 裁决、BFF 不前置拦截）",
                example = "与已激活知识「排课冲突案例」内容重叠，无增量价值")
        String reason
) {
}
