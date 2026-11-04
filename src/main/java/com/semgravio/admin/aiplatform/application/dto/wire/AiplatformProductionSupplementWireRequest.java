package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform「需用户补充」置位命令体的 wire 镜像——与 aiplatform
 * {@code SetNeedsUserSupplementCommand}（#338 后台机机面）字段同构：置位语义
 * （何时需要用户补充、补充什么——对话区沟通承载）归后台人工判定，命令只携布尔。
 *
 * @param required true＝置位（前台六态盖帽「需用户补充」）、false＝清零
 */
public record AiplatformProductionSupplementWireRequest(
        boolean required
) {
}
