package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置读面——北向出口，逐字镜像 aiplatform {@code BackofficeAgentConfigResponse}
 * （#251 智能体运营配置，#246 技能线 T5，spec #79 定稿、issue #84；B3 issue #112 增路由
 * 强档三件；issue #122 增 #432 提速批次十五件〔V53–V57〕；issue #123 增 #454 路由结构性
 * 信号开关两枚〔V58〕）：生效值＋覆盖标记＋枚举默认预览＋增强工具开关存储态＋路由强档三件
 * ＋路由结构性信号开关两枚＋提速批次十五件＋最近写者。GET 读面与 PUT 回执同形
 * （provider 写入后重读同一视图）。
 *
 * <p>生效值＝库覆盖值或枚举默认（装配实取值——对照覆盖标记知来源）；默认预览即清空覆盖的
 * 落点（恢复默认前可先看）。无覆盖行＝全回落：覆盖标记 false、写者两肢与 updatedAt null
 * 如实出 JSON null（全局 Jackson 含 null）。覆盖标记与开关 provider 恒在场（原生
 * boolean，admin 同型镜像）。modelId/routingStrongModelId 语义＝<strong>档位键</strong>
 * （ADR-0031 语义键如 flash/pro——V44 存量已刷档，非裸模型名；写口即时校验须在模型档位面
 * {@code /model-tiers} 有行且启用，缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075）。寻址
 * 稳定键三把：main=主智能体 / executor=run 执行体 / designer=设计执行体——边缘调用点
 * （naming 等）走 /edge/{edgeKey} 档位端点，三座端点对边缘键 404 PRJ_033＝数字业务码 4033
 * （admin 字符串透传不预校验）。提速批次十件仅 run 执行体行消费（#122 语义陷阱④：
 * main/designer 页可隐藏或置灰）。provider 读面另带路由开关/两阈值/升档标签映射/verify 面
 * 六件（#354/#424/#426/#427），B3 票面不点名——不镜像（读面不呈现）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigResponse(

        /** 智能体稳定键（main=主智能体 / executor=run 执行体 / designer=设计执行体） */
        @Schema(description = "智能体稳定键（main=主智能体 / executor=run 执行体 / "
                + "designer=设计执行体；边缘调用点走 /edge/{edgeKey}；未知键 404 "
                + "PRJ_033＝数字业务码 4033）", example = "main")
        String agentKey,

        /** 展示名 */
        @Schema(description = "展示名", example = "主智能体")
        String agentName,

        /** 生效 systemPrompt（库覆盖值或枚举默认——装配实取值，对照 systemPromptOverridden 知来源） */
        @Schema(description = "生效 systemPrompt（库覆盖值或枚举默认——装配实取值，"
                + "对照 systemPromptOverridden 知来源）")
        String systemPrompt,

        /** 生效模型档位键（ADR-0031 语义键，如 flash/pro） */
        @Schema(description = "生效模型档位键（ADR-0031 语义键，如 flash/pro——档位表"
                + "/model-tiers 为值域正本；对照 modelIdOverridden 知来源）", example = "pro")
        String modelId,

        /** systemPrompt 是否库覆盖值（true＝覆盖；false＝枚举默认） */
        @Schema(description = "systemPrompt 是否库覆盖值（true＝覆盖；false＝枚举默认）")
        boolean systemPromptOverridden,

        /** 模型档位是否库覆盖值（true＝覆盖；false＝枚举默认） */
        @Schema(description = "模型档位是否库覆盖值（true＝覆盖；false＝枚举默认）")
        boolean modelIdOverridden,

        /** 枚举默认 systemPrompt（预览：清空覆盖即落此值——恢复默认前可先看落点） */
        @Schema(description = "枚举默认 systemPrompt（预览：清空覆盖即落此值——身份与配置分治的缺省正本）")
        String defaultSystemPrompt,

        /** 枚举默认模型档位键（清空覆盖即落此值） */
        @Schema(description = "枚举默认模型档位键（清空覆盖即落此值）", example = "flash")
        String defaultModelId,

        /** 联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开） */
        @Schema(description = "联网搜索开关生效态（关即退出槽位装配面；无覆盖行语境＝true 开）")
        boolean webSearchEnabled,

        /** 网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开） */
        @Schema(description = "网页抓取开关生效态（生效语义同 webSearchEnabled；无覆盖行语境＝true 开）")
        boolean fetchUrlEnabled,

        /** 强模型档生效值（路由升档落点；库覆盖值或代码缺省——对照 routingStrongModelIdOverridden 知来源） */
        @Schema(description = "强模型档生效值（#354 路由升档落点；库覆盖值或代码缺省——"
                + "对照 routingStrongModelIdOverridden 知来源；主档/强档同范式管理）",
                example = "pro")
        String routingStrongModelId,

        /** 强模型档是否库覆盖值（true＝覆盖；false＝代码缺省 pro） */
        @Schema(description = "强模型档是否库覆盖值（true＝覆盖；false＝代码缺省 pro）")
        boolean routingStrongModelIdOverridden,

        /** 代码缺省强模型档（预览：清空覆盖即落此值） */
        @Schema(description = "代码缺省强模型档（预览：清空覆盖即落此值）", example = "pro")
        String defaultRoutingStrongModelId,

        /** 文件面信号开关生效态（#454，ADR-0037；缺省 false＝关——文件面不参与升档判定〔观测值照留进判定快照〕；true＝恢复阈值判据，阈值旋钮照旧生效。失败重试升档信号不受本开关影响、常开。仅 run 执行体行消费） */
        @Schema(description = "文件面信号开关生效态（#454，ADR-0037；缺省 false＝关——文件面"
                + "不参与升档判定〔观测值照留进判定快照〕；true＝恢复阈值判据——阈值旋钮照旧"
                + "生效。失败重试升档信号不受本开关影响、常开。仅 run 执行体行消费）")
        boolean routingFileCountSignalEnabled,

        /** 规模标签信号开关生效态（#454，ADR-0037；缺省 false＝关——规模标签不参与升档判定；true＝恢复标签映射判据，映射旋钮照旧生效。失败重试升档信号不受两开关影响、常开。仅 run 执行体行消费） */
        @Schema(description = "规模标签信号开关生效态（#454，ADR-0037；缺省 false＝关——规模"
                + "标签不参与升档判定；true＝恢复标签映射判据——映射旋钮照旧生效。失败重试"
                + "升档信号不受两开关影响、常开。仅 run 执行体行消费）")
        boolean routingSizeLabelSignalEnabled,

        /** 验证在场闸开关生效态（#433；缺省 false＝闸关——本 run 无 report_verification 登记不收口。仅 run 执行体行消费） */
        @Schema(description = "验证在场闸开关生效态（#433；缺省 false＝闸关——本 run 无 "
                + "report_verification 登记不收口仅在该闸被运营打开时生效。仅 run 执行体行消费）")
        boolean verificationPresenceGateEnabled,

        /** 空转产出证明闸开关生效态（#433；缺省 false＝闸关——段起点零 diff 且无确认不收口。仅 run 执行体行消费） */
        @Schema(description = "空转产出证明闸开关生效态（#433；缺省 false＝闸关——段起点以来"
                + "零 diff 且无「能力已存在」确认不收口仅在该闸被运营打开时生效。仅 run 执行体行消费）")
        boolean proofOfOutputGateEnabled,

        /** 整轨终验拦「生成完成」开关生效态（#433；缺省 false＝闸关——终验不起 run、不拦完成叙事。仅 run 执行体行消费） */
        @Schema(description = "整轨终验拦「生成完成」开关生效态（#433；缺省 false＝闸关——"
                + "终验不起 run；true＝闸开——轨道完成前终验 fail 拦完成叙事。仅 run 执行体行消费）")
        boolean finalVerificationGateEnabled,

        /** 单会话连续编码开关生效态（#436；缺省 true＝单会话开。仅 run 执行体行消费） */
        @Schema(description = "单会话连续编码开关生效态（#436；缺省 true＝单会话开——阶段 0＋"
                + "全部切片同会话槽位连续追加；false＝旧「每片新会话」形态。仅 run 执行体行消费）")
        boolean singleSessionGenerationEnabled,

        /** 切片并行机器开关生效态（#436；缺省 false＝并行机器关。仅 run 执行体行消费） */
        @Schema(description = "切片并行机器开关生效态（#436；缺省 false＝并行机器关——fork 隔离"
                + "工作区/波调度/并行/合并全关；true＝宽波 fork 并行旧行为。仅 run 执行体行消费）")
        boolean parallelSliceMachineEnabled,

        /** 决策时注入总开关生效态（#437；缺省 true＝注入开。仅 run 执行体行消费） */
        @Schema(description = "决策时注入总开关生效态（#437；缺省 true＝注入开——执行体 ReAct "
                + "循环每迭代判定注入一条微指令；false＝全停原样透传。仅 run 执行体行消费）")
        boolean decisionHintsEnabled,

        /** 失败止损阈值生效值（#437；库覆盖值或代码缺省 3——null 库值即回落 3 呈现） */
        @Schema(description = "失败止损阈值生效值（#437；库覆盖值或代码缺省 3〔null 回落〕；"
                + "写口越界 2–10 回 400 PRJ_081）", example = "3")
        Integer decisionHintErrorStreak,

        /** 失败止损阈值是否库覆盖值（true＝覆盖；false＝代码缺省） */
        @Schema(description = "失败止损阈值是否库覆盖值（true＝覆盖；false＝代码缺省）")
        boolean decisionHintErrorStreakOverridden,

        /** 代码缺省失败止损阈值（预览：清空覆盖即落此值） */
        @Schema(description = "代码缺省失败止损阈值（预览：清空覆盖即落此值）", example = "3")
        Integer defaultDecisionHintErrorStreak,

        /** 微指令堆叠上限生效值（#437；库覆盖值或代码缺省 3——null 库值即回落 3 呈现） */
        @Schema(description = "微指令堆叠上限生效值（#437；库覆盖值或代码缺省 3〔null 回落〕；"
                + "写口越界 1–3 回 400 PRJ_082）", example = "3")
        Integer decisionHintStackCap,

        /** 堆叠上限是否库覆盖值（true＝覆盖；false＝代码缺省） */
        @Schema(description = "堆叠上限是否库覆盖值（true＝覆盖；false＝代码缺省）")
        boolean decisionHintStackCapOverridden,

        /** 代码缺省堆叠上限（预览：清空覆盖即落此值） */
        @Schema(description = "代码缺省堆叠上限（预览：清空覆盖即落此值）", example = "3")
        Integer defaultDecisionHintStackCap,

        /** 禁用集生效值（#437；键数组——空数组＝全启用；对照 decisionHintDisabledOverridden 知来源） */
        @Schema(description = "禁用集生效值（#437；库覆盖键数组——封闭集 failure_stop_loss/"
                + "anti_mock/context_economy/parallel_tools，非法 token 400 PRJ_083；"
                + "空数组＝全启用）", example = "[\"parallel_tools\"]")
        List<String> decisionHintDisabled,

        /** 禁用集是否库覆盖值（true＝覆盖；false＝全启用） */
        @Schema(description = "禁用集是否库覆盖值（true＝覆盖；false＝全启用）")
        boolean decisionHintDisabledOverridden,

        /** verify 自决开关生效态（#442；缺省 true＝自决开。仅 run 执行体行消费） */
        @Schema(description = "verify 自决开关生效态（#442；缺省 true＝自决开——执行体生成过程"
                + "中自决调用 verify 自测；false＝自决关闭——生成过程不自发委派 verify，平台驱动"
                + "验证腿〔三闸回执／整轨终验编排〕不受影响。仅 run 执行体行消费）")
        boolean verifySelfDecisionEnabled,

        /** 最近写者 id（admin 侧 TSID；null＝从未配置） */
        @Schema(description = "最近写者 id（admin 侧 TSID；null＝从未配置）", example = "700200")
        String operatorId,

        /** 最近写者名（直读展示） */
        @Schema(description = "最近写者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 最近写入时刻（null＝从未配置） */
        @Schema(description = "最近写入时刻（null＝从未配置）")
        LocalDateTime updatedAt
) {
}
