package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台增强工具开关北向命令——运营人员提交的「开关目标态」意图，逐字镜像 aiplatform
 * {@code AgentToolToggleCommand}（#252 工具面，#246 技能线 T6，spec #79 定稿、issue #85）。
 * 目标态必须显式（无缺省翻转语义——幂等写面，同值重写零写入不落痕）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformAgentConfigUpdateCommand}
 * 先例）：字段合法性由 provider 写用例裁决（仅增强工具 web_search/fetch_url 接受——骨架与
 * harness 内建 403 PRJ_036；未知工具名 404 PRJ_035；缺 enabled 400 PRJ_037），错误信封
 * 原样透传——BFF 不加戏不重复校验。{@code enabled} 为 <strong>Boolean 装箱</strong>（与
 * provider 命令同型）：缺省＝null 透传＝provider 判目标态必填违例（400 PRJ_037＝数字业务码
 * 4037）——若用原生 boolean，北向缺省会被绑定层落成 false（关），凭空制造一次写操作。
 * 操作者身份不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name}
 * 自动透传落痕（开关写必留痕，缺头由 provider 域面守卫 PRJ_034＝数字业务码 4034 拦，
 * admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentToolToggleCommand(

        /** 开关目标态（必填、无缺省：true=挂载回归装配面 / false=退出槽位装配面——变更走配置留痕，与智能体配置同机制） */
        @Schema(description = "开关目标态（必填、无缺省：true=挂载回归装配面 / "
                + "false=退出槽位装配面——变更走配置留痕，与智能体配置同机制；缺省 null 透传＝"
                + "provider 判 400 PRJ_037）", example = "false")
        Boolean enabled
) {
}
