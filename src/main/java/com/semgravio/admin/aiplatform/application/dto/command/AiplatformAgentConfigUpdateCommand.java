package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置全量写北向命令——运营人员提交的「该智能体覆盖态终态」意图，逐字镜像
 * aiplatform {@code AgentConfigUpdateCommand}（#251 智能体运营配置，#246 技能线 T5，spec #79
 * 定稿、issue #84；B3 issue #112 增路由强档件——镜像 provider #367 落地后的 B3 面）。PUT
 * 全量语义：请求体即覆盖态终态——值面五件全量替换，与槽位指派「清单即终态」同款形制。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillUpdateCommand} 先例）：
 * 字段合法性由 provider 写用例裁决（覆盖三件 null/缺省/纯空白＝清空覆盖回落枚举/代码默认；
 * 档位键即时校验——modelId 与 routingStrongModelId 非空时必须在模型档位面
 * {@code /model-tiers} 有行且启用，缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075，错值
 * 当场拦截不再靠留痕回滚兜底；null＝清空回落缺省不校验），错误信封原样透传——BFF 不加戏
 * 不重复校验。开关两件为 <strong>Boolean 装箱</strong>（与 provider 命令同型）：缺省＝null
 * 透传＝provider 缺省 true（开）——若用原生 boolean，北向缺省会被绑定层落成 false（关），
 * 语义反了。</p>
 *
 * <p>provider 命令另带路由开关/两阈值三件（{@code routingEnabled}/
 * {@code routingFileCountThreshold}/{@code routingSliceFeatureThreshold}，#354）——B3 票面
 * 只点名 routingStrongModelId 一族，三件不镜像：admin 命令体三肢不出站＝provider null＝
 * 缺省态（开/60/4）；BFF 是 provider 后台唯一写面，三件从无可设处，结构上恒缺省态
 * （2026-11-03 票面字面口径拍板）。操作者身份不在命令体——经框架 {@code RequestContext}
 * →{@code X-User-Id/X-User-Name} 自动透传落痕（配置变更必留痕，缺头由 provider 域面守卫
 * PRJ_034＝数字业务码 4034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigUpdateCommand(

        /** 覆盖 systemPrompt（null/缺省/纯空白＝清空覆盖，装配回落枚举默认——GET 回执的 defaultSystemPrompt 即回落落点） */
        @Schema(description = "覆盖 systemPrompt 全文（PUT 全量语义：null/缺省/纯空白＝"
                + "清空覆盖，装配回落枚举默认——GET 回执的 defaultSystemPrompt 即回落落点）")
        String systemPrompt,

        /** 覆盖模型档位键（ADR-0031 语义键如 flash/pro；null/缺省/纯空白＝清空覆盖回落枚举默认；即时校验须在档位面有行且启用） */
        @Schema(description = "覆盖模型档位键（ADR-0031 语义键，如 flash/pro——档位表"
                + "/model-tiers 为值域正本，enabled=true 键集即改档下拉数据源；null/缺省/"
                + "纯空白＝清空覆盖回落枚举默认。即时校验：非空须在模型档位面有行且启用"
                + "——缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075，错值当场拦截）",
                example = "pro")
        String modelId,

        /** 增强工具开关：联网搜索（装箱——缺省 null＝provider 缺省 true 开；关即退出槽位装配面、开即回归） */
        @Schema(description = "增强工具开关：联网搜索（缺省 true＝开；关即退出槽位装配面、"
                + "开即回归——工具面窄幅写口见 PUT /agent-tools/{toolName}）")
        Boolean webSearchEnabled,

        /** 增强工具开关：网页抓取（装箱——缺省 null＝provider 缺省 true 开；生效语义同 webSearchEnabled） */
        @Schema(description = "增强工具开关：网页抓取（缺省 true＝开；生效语义同 webSearchEnabled）")
        Boolean fetchUrlEnabled,

        /** 覆盖路由强档键（路由升档落点；null/缺省/纯空白＝清空回落代码缺省 pro；即时校验同 modelId） */
        @Schema(description = "覆盖路由强档键（#354 路由升档落点——主档/强档同范式管理，"
                + "语义键同 modelId；null/缺省/纯空白＝清空回落代码缺省 pro。即时校验同"
                + " modelId：非空须在模型档位面有行且启用（PRJ_074/075））",
                example = "pro")
        String routingStrongModelId
) {
}
