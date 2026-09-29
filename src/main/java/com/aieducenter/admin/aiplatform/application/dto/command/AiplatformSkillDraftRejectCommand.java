package com.aieducenter.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台草稿拒绝北向命令——审核员提交的「不采纳理由」，逐字镜像 aiplatform
 * {@code SkillDraftRejectCommand}（#262 自产线 T2，issue #92 镜像 spec #91）。
 * 拒绝即终态：草稿标已拒绝＋操作者/时刻/理由随行留档，不改稿不复活——重提只能靠未来
 * 会话产生新草稿。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillInstallCommand}
 * 等技能线先例）：理由合法性由 provider 拒绝用例裁决（空白/缺 body 400 SKL_018＝数字
 * 业务码 7018），错误信封原样透传——BFF 不加戏不重复校验（可选绑定：缺 body 不前置
 * 拦截，直出 provider 裁决）。操作者身份不在命令体——经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（缺头由 provider 域面守卫 SKL_009＝数字
 * 业务码 7009 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillDraftRejectCommand(

        /** 拒绝理由（终态留档可查——拒绝须有据；空白即 400 SKL_018，由 provider 裁决不前置拦截） */
        @Schema(description = "拒绝理由（终态留档可查——拒绝须有据；空白即 400 SKL_018＝"
                + "数字业务码 7018，由 provider 裁决、BFF 不前置拦截）",
                example = "正文与现有技能 prd-writing 方法论重叠，解消方向不明")
        String reason
) {
}
