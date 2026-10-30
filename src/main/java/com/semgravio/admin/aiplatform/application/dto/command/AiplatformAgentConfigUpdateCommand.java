package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置全量写北向命令——运营人员提交的「该智能体覆盖态终态」意图，逐字镜像
 * aiplatform {@code AgentConfigUpdateCommand}（#251 智能体运营配置，#246 技能线 T5，spec #79
 * 定稿、issue #84）。PUT 全量语义：请求体即覆盖态终态——值面四件全量替换，与槽位指派
 * 「清单即终态」同款形制。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillUpdateCommand} 先例）：
 * 字段合法性由 provider 写用例裁决（覆盖两件 null/缺省/纯空白＝清空覆盖回落枚举默认；档位名
 * 不做白名单校验——错值致新会话失败经留痕写回可回滚），错误信封原样透传——BFF 不加戏不重复
 * 校验。开关两件为 <strong>Boolean 装箱</strong>（与 provider 命令同型）：缺省＝null 透传＝
 * provider 缺省 true（开）——若用原生 boolean，北向缺省会被绑定层落成 false（关），语义反了。
 * 操作者身份不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动
 * 透传落痕（配置变更必留痕，缺头由 provider 域面守卫 PRJ_034＝数字业务码 4034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigUpdateCommand(

        /** 覆盖 systemPrompt（null/缺省/纯空白＝清空覆盖，装配回落枚举默认——GET 回执的 defaultSystemPrompt 即回落落点） */
        @Schema(description = "覆盖 systemPrompt 全文（PUT 全量语义：null/缺省/纯空白＝"
                + "清空覆盖，装配回落枚举默认——GET 回执的 defaultSystemPrompt 即回落落点）")
        String systemPrompt,

        /** 覆盖模型档位裸名（null/缺省/纯空白＝清空覆盖回落枚举默认；不做白名单校验） */
        @Schema(description = "覆盖模型档位裸名（如 deepseek-v4-pro；null/缺省/纯空白＝"
                + "清空覆盖回落枚举默认。不做白名单校验——错值致新会话失败经留痕写回可回滚）",
                example = "deepseek-v4-pro")
        String modelId,

        /** 增强工具开关：联网搜索（装箱——缺省 null＝provider 缺省 true 开；关即退出槽位装配面、开即回归） */
        @Schema(description = "增强工具开关：联网搜索（缺省 true＝开；关即退出槽位装配面、"
                + "开即回归——工具面窄幅写口见 PUT /agent-tools/{toolName}）")
        Boolean webSearchEnabled,

        /** 增强工具开关：网页抓取（装箱——缺省 null＝provider 缺省 true 开；生效语义同 webSearchEnabled） */
        @Schema(description = "增强工具开关：网页抓取（缺省 true＝开；生效语义同 webSearchEnabled）")
        Boolean fetchUrlEnabled
) {
}
