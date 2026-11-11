package com.semgravio.admin.aiplatform.application.dto.command;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置全量写北向命令——运营人员提交的「该智能体覆盖态终态」意图，逐字镜像
 * aiplatform {@code AgentConfigUpdateCommand}（#251 智能体运营配置，#246 技能线 T5，spec #79
 * 定稿、issue #84；B3 issue #112 增路由强档件；issue #122 增 #432 提速批次十件〔V53–V57〕）。
 * PUT 全量语义：请求体即覆盖态终态——值面全量替换，与槽位指派「清单即终态」同款形制。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillUpdateCommand} 先例）：
 * 字段合法性由 provider 写用例裁决（覆盖三件 null/缺省/纯空白＝清空覆盖回落枚举/代码默认；
 * 档位键即时校验——modelId 与 routingStrongModelId 非空时必须在模型档位面
 * {@code /model-tiers} 有行且启用，缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075；决策注入
 * 两阈值越界 400 PRJ_081/082＝4081/4082、禁用集非法 token 400 PRJ_083＝4083——错值当场拦截
 * 归 provider，错误信封原样透传，BFF 不加戏不重复校验）。</p>
 *
 * <p><strong>缺省方向不一致（#122 语义陷阱②——占位/重置须按字段区分）</strong>：
 * webSearch/fetchUrl/singleSession/decisionHints/verifySelfDecision 缺省 true（开）；三闸
 * （verificationPresenceGate/proofOfOutputGate/finalVerificationGate）与 parallelSliceMachine
 * 缺省 false（闸关）。<strong>null 方向相反（语义陷阱③）</strong>：三闸 null＝闸关（与工具/
 * 路由开关「null＝开」相反——#435 归一口径翻转）；其余开关 null＝回落缺省。开关全部
 * <strong>Boolean 装箱</strong>（与 provider 命令同型）：缺省＝null 出站在场——若用原生
 * boolean，北向缺省会被绑定层落成 false（关），对缺省开的字段语义反了。决策注入两阈值
 * Integer 可空（null＝清空回落代码缺省 3）；禁用集 List&lt;String&gt;（null/空数组＝全启用）。
 * 旧页面不带新字段保存会把三闸静默刷回关（语义陷阱①——表单须带全字段，前端票跟进）。</p>
 *
 * <p>provider 命令另带路由开关/两阈值/升档标签映射/verify 面九件（#354/#424/#426/#427）
 * ——B3 票面不点名，本票十件之外不镜像：admin 命令体九件不出站＝provider null＝缺省态；
 * BFF 是 provider 后台唯一写面，九件从无可设处，结构上恒缺省态（票面字面口径）。提速批次
 * 十件仅 run 执行体行消费（main/designer 行同列无消费——前端可隐藏或置灰）。操作者身份
 * 不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕
 * （配置变更必留痕，缺头由 provider 域面守卫 PRJ_034＝数字业务码 4034 拦，admin 不拦）。</p>
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
        String routingStrongModelId,

        /** 验证在场闸开关（#433；装箱——缺省 null＝闸关〔false，#435 归一口径〕；true＝闸开。仅 run 执行体行消费） */
        @Schema(description = "验证在场闸开关（#433；缺省 false＝闸关——本 run 无 "
                + "report_verification 登记不收口不生效；true＝闸开。仅 run 执行体行消费）")
        Boolean verificationPresenceGateEnabled,

        /** 空转产出证明闸开关（#433；装箱——缺省 null＝闸关；true＝闸开。仅 run 执行体行消费） */
        @Schema(description = "空转产出证明闸开关（#433；缺省 false＝闸关——段起点以来零 diff "
                + "且无「能力已存在」确认不收口不生效；true＝闸开。仅 run 执行体行消费）")
        Boolean proofOfOutputGateEnabled,

        /** 整轨终验拦「生成完成」开关（#433；装箱——缺省 null＝闸关；true＝闸开。仅 run 执行体行消费） */
        @Schema(description = "整轨终验拦「生成完成」开关（#433；缺省 false＝闸关——终验不起 "
                + "run；true＝闸开——轨道完成前终验 fail 拦完成叙事。仅 run 执行体行消费）")
        Boolean finalVerificationGateEnabled,

        /** 单会话连续编码开关（#436；装箱——缺省 null＝单会话开〔true〕；false＝旧「每片新会话」形态。仅 run 执行体行消费） */
        @Schema(description = "单会话连续编码开关（#436；缺省 true＝单会话开——阶段 0＋全部"
                + "切片同会话槽位连续追加；false＝旧「每片新会话」形态。仅 run 执行体行消费）")
        Boolean singleSessionGenerationEnabled,

        /** 切片并行机器开关（#436；装箱——缺省 null＝并行机器关〔false〕；true＝宽波 fork 并行旧行为。仅 run 执行体行消费） */
        @Schema(description = "切片并行机器开关（#436；缺省 false＝并行机器关——fork 隔离工作区/"
                + "波调度/并行执行/合并全关；true＝宽波 fork 并行旧行为。仅 run 执行体行消费）")
        Boolean parallelSliceMachineEnabled,

        /** 决策时注入总开关（#437；装箱——缺省 null＝注入开〔true〕；false＝全停原样透传。仅 run 执行体行消费） */
        @Schema(description = "决策时注入总开关（#437；缺省 true＝注入开——执行体 ReAct 循环每"
                + "迭代判定注入一条微指令；false＝全停原样透传。仅 run 执行体行消费）")
        Boolean decisionHintsEnabled,

        /** 失败止损阈值（#437；可空——null＝清空回落代码缺省 3；2–10 整数，越界 400 PRJ_081） */
        @Schema(description = "失败止损阈值（#437；同类错误连续失败 ≥ 此值注入「换策略」微指令；"
                + "null＝清空回落代码缺省 3；2–10 整数，越界 400 PRJ_081＝4081）",
                example = "3")
        Integer decisionHintErrorStreak,

        /** 微指令堆叠上限（#437；可空——null＝清空回落代码缺省 3；1–3 整数，越界 400 PRJ_082） */
        @Schema(description = "微指令堆叠上限（#437；单轮最多注入几条不同微指令；null＝清空"
                + "回落代码缺省 3；1–3 整数，越界 400 PRJ_082＝4082）",
                example = "3")
        Integer decisionHintStackCap,

        /** 禁用集（#437；键数组——封闭集 failure_stop_loss/anti_mock/context_economy/parallel_tools，非法 token 400 PRJ_083；null/空数组＝全启用） */
        @Schema(description = "禁用集（#437；库条目键数组——封闭集 failure_stop_loss/anti_mock/"
                + "context_economy/parallel_tools，非法 token 400 PRJ_083＝4083；空数组/null＝全启用）",
                example = "[\"parallel_tools\"]")
        List<String> decisionHintDisabled,

        /** verify 自决开关（#442；装箱——缺省 null＝自决开〔true〕；false＝自决关闭。仅 run 执行体行消费） */
        @Schema(description = "verify 自决开关（#442；缺省 true＝自决开——执行体生成过程中自决"
                + "调用 verify 自测；false＝自决关闭——生成过程不自发委派 verify〔提示层收窄〕，"
                + "平台驱动验证腿〔三闸回执／整轨终验编排〕不受影响。仅 run 执行体行消费）")
        Boolean verifySelfDecisionEnabled
) {
}
