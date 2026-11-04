package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台「需用户补充」置位北向命令（issue #107，spec #102 A 批 A4）——逐字镜像
 * aiplatform {@code SetNeedsUserSupplementCommand}（#338）：置位语义（何时需要用户
 * 补充、补充什么——对话区沟通承载）归后台人工判定，命令只携布尔。
 *
 * <p>项目寻址守卫（不存在/畸形 id 404 PRJ_001）由 provider 裁决，错误信封原样透传。
 * 本动作不落操作者留痕行（provider 命令面无操作者位——区别于承接推进/信息更新两
 * 写口），BFF 照原形零加戏。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProductionSupplementCommand(

        /** true＝置位（前台六态盖帽「需用户补充」）、false＝清零 */
        @Schema(description = "true＝置位（前台六态盖帽「需用户补充」，处理入口在对话区沟通）、"
                + "false＝清零", example = "true")
        boolean required
) {
}
