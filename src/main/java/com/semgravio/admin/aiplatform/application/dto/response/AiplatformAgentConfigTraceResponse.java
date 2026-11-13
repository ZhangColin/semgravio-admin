package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台智能体配置变更留痕读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeAgentConfigTraceResponse}（#251 留痕回滚，#246 技能线 T5，spec #79 定稿、
 * issue #84；B3 issue #112 增路由强档两件；issue #122 增 #432 提速批次二十件〔V53–V57〕；
 * issue #123 增 #454 路由结构性信号开关 old/new 四枚〔V58〕）：一次实际变更的前后全量值快照
 * （old&#42;/new&#42; 对）＋操作者＋时刻。
 *
 * <p>每痕＝一次实际变更（同值幂等重写不落痕——时间线里没有假变更）；按时间倒序（最近先，
 * provider 定死排序）。append-only——留痕不随任何动作删除（历史事实不随配置行消失）。
 * 回滚＝把某痕旧值快照经 PUT 写回（即一次新变更、留新痕，不做版本树）——old&#42; 件即
 * 写回依据。old&#42;/new&#42; 的 prompt/档位/<strong>强档</strong>三件可空（null＝该侧即缺省
 * 回落；强档件是读面可改就必须可回滚的必然配套）；决策注入两阈值与禁用集可空（null＝该侧
 * 即代码缺省/全启用）；禁用集留痕为<strong>存储形逗号接键串</strong>（如
 * {@code "anti_mock,parallel_tools"}——provider 留痕快照直读存储列，与读面 List 形不同；
 * 空串＝全启用显式形）；开关件 provider 恒在场（原生 boolean，admin 同型镜像）。档位语义＝
 * 档位键（ADR-0031 语义键）。provider 留痕另带路由开关/两阈值/升档标签映射/verify 面旧件
 * （#354/#424/#426/#427），同读面口径不镜像。边缘调用点留痕行同形复用（/edge/{edgeKey}/
 * traces——边缘行档位列之外快照件恒缺省态：prompt null、开关缺省态〔webSearch/fetchUrl 恒
 * true〕、强档 null、提速批次恒代码缺省态）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformAgentConfigTraceResponse(

        /** 留痕标识（TSID 十进制串） */
        @Schema(description = "留痕标识（TSID 十进制串）")
        String id,

        /** 变更前 systemPrompt（null＝此前即缺省回落——回滚即把此值写回） */
        @Schema(description = "变更前 systemPrompt（null＝此前即缺省回落——回滚即把此值写回）")
        String oldSystemPrompt,

        /** 变更前模型档位键（null＝此前即缺省回落） */
        @Schema(description = "变更前模型档位键（null＝此前即缺省回落）")
        String oldModelId,

        /** 变更前联网搜索开关 */
        @Schema(description = "变更前联网搜索开关")
        boolean oldWebSearchEnabled,

        /** 变更前网页抓取开关 */
        @Schema(description = "变更前网页抓取开关")
        boolean oldFetchUrlEnabled,

        /** 变更前强模型档（null＝此前即回落代码缺省——回滚即把此值写回） */
        @Schema(description = "变更前强模型档（null＝此前即回落代码缺省——回滚即把此值写回）")
        String oldRoutingStrongModelId,

        /** 变更前文件面信号开关（#454；恒有值——缺省态恒 false＝不参与升档判定） */
        @Schema(description = "变更前文件面信号开关（#454；恒有值——缺省态恒 false＝不参与升档判定）")
        boolean oldRoutingFileCountSignalEnabled,

        /** 变更前规模标签信号开关（#454；恒有值——缺省态恒 false） */
        @Schema(description = "变更前规模标签信号开关（#454；恒有值——缺省态恒 false）")
        boolean oldRoutingSizeLabelSignalEnabled,

        /** 变更前验证在场闸开关（#433；恒有值——缺省态 false＝闸关） */
        @Schema(description = "变更前验证在场闸开关（#433；恒有值——缺省态 false＝闸关）")
        boolean oldVerificationPresenceGateEnabled,

        /** 变更前空转产出证明闸开关（#433；恒有值） */
        @Schema(description = "变更前空转产出证明闸开关（#433；恒有值）")
        boolean oldProofOfOutputGateEnabled,

        /** 变更前整轨终验拦「生成完成」开关（#433；恒有值） */
        @Schema(description = "变更前整轨终验拦「生成完成」开关（#433；恒有值）")
        boolean oldFinalVerificationGateEnabled,

        /** 变更前单会话连续编码开关（#436；恒有值——缺省态 true＝单会话开） */
        @Schema(description = "变更前单会话连续编码开关（#436；恒有值——缺省态 true＝单会话开）")
        boolean oldSingleSessionGenerationEnabled,

        /** 变更前切片并行机器开关（#436；恒有值——缺省态 false＝并行机器关） */
        @Schema(description = "变更前切片并行机器开关（#436；恒有值——缺省态 false＝并行机器关）")
        boolean oldParallelSliceMachineEnabled,

        /** 变更前决策时注入总开关（#437；恒有值——缺省态 true＝注入开） */
        @Schema(description = "变更前决策时注入总开关（#437；恒有值——缺省态 true＝注入开）")
        boolean oldDecisionHintsEnabled,

        /** 变更前失败止损阈值（#437；null＝此前即代码缺省 3） */
        @Schema(description = "变更前失败止损阈值（#437；null＝此前即代码缺省 3）")
        Integer oldDecisionHintErrorStreak,

        /** 变更前微指令堆叠上限（#437；null＝此前即代码缺省 3） */
        @Schema(description = "变更前微指令堆叠上限（#437；null＝此前即代码缺省 3）")
        Integer oldDecisionHintStackCap,

        /** 变更前禁用集（#437 存储形逗号接键串；null＝此前即全启用；空串＝全启用显式形） */
        @Schema(description = "变更前禁用集（#437 存储形逗号接键串，如 \"anti_mock,parallel_tools\"；"
                + "null＝此前即全启用；空串＝全启用显式形）")
        String oldDecisionHintDisabled,

        /** 变更前 verify 自决开关（#442；恒有值——缺省态 true＝自决开） */
        @Schema(description = "变更前 verify 自决开关（#442；恒有值——缺省态 true＝自决开）")
        boolean oldVerifySelfDecisionEnabled,

        /** 变更后 systemPrompt（null＝本次清空回落枚举默认） */
        @Schema(description = "变更后 systemPrompt（null＝本次清空回落枚举默认）")
        String newSystemPrompt,

        /** 变更后模型档位键（null＝本次清空回落） */
        @Schema(description = "变更后模型档位键（null＝本次清空回落）")
        String newModelId,

        /** 变更后联网搜索开关 */
        @Schema(description = "变更后联网搜索开关")
        boolean newWebSearchEnabled,

        /** 变更后网页抓取开关 */
        @Schema(description = "变更后网页抓取开关")
        boolean newFetchUrlEnabled,

        /** 变更后强模型档（null＝本次清空回落代码缺省） */
        @Schema(description = "变更后强模型档（null＝本次清空回落代码缺省）")
        String newRoutingStrongModelId,

        /** 变更后文件面信号开关（#454；恒有值） */
        @Schema(description = "变更后文件面信号开关（#454；恒有值）")
        boolean newRoutingFileCountSignalEnabled,

        /** 变更后规模标签信号开关（#454；恒有值） */
        @Schema(description = "变更后规模标签信号开关（#454；恒有值）")
        boolean newRoutingSizeLabelSignalEnabled,

        /** 变更后验证在场闸开关（#433；恒有值） */
        @Schema(description = "变更后验证在场闸开关（#433；恒有值）")
        boolean newVerificationPresenceGateEnabled,

        /** 变更后空转产出证明闸开关（#433；恒有值） */
        @Schema(description = "变更后空转产出证明闸开关（#433；恒有值）")
        boolean newProofOfOutputGateEnabled,

        /** 变更后整轨终验拦「生成完成」开关（#433；恒有值） */
        @Schema(description = "变更后整轨终验拦「生成完成」开关（#433；恒有值）")
        boolean newFinalVerificationGateEnabled,

        /** 变更后单会话连续编码开关（#436；恒有值） */
        @Schema(description = "变更后单会话连续编码开关（#436；恒有值）")
        boolean newSingleSessionGenerationEnabled,

        /** 变更后切片并行机器开关（#436；恒有值） */
        @Schema(description = "变更后切片并行机器开关（#436；恒有值）")
        boolean newParallelSliceMachineEnabled,

        /** 变更后决策时注入总开关（#437；恒有值） */
        @Schema(description = "变更后决策时注入总开关（#437；恒有值）")
        boolean newDecisionHintsEnabled,

        /** 变更后失败止损阈值（#437；null＝本次清空回落代码缺省 3） */
        @Schema(description = "变更后失败止损阈值（#437；null＝本次清空回落代码缺省 3）")
        Integer newDecisionHintErrorStreak,

        /** 变更后微指令堆叠上限（#437；null＝本次清空回落代码缺省 3） */
        @Schema(description = "变更后微指令堆叠上限（#437；null＝本次清空回落代码缺省 3）")
        Integer newDecisionHintStackCap,

        /** 变更后禁用集（#437 存储形逗号接键串；null＝本次清空全启用） */
        @Schema(description = "变更后禁用集（#437 存储形逗号接键串；null＝本次清空全启用）")
        String newDecisionHintDisabled,

        /** 变更后 verify 自决开关（#442；恒有值） */
        @Schema(description = "变更后 verify 自决开关（#442；恒有值）")
        boolean newVerifySelfDecisionEnabled,

        /** 变更操作者 id */
        @Schema(description = "变更操作者 id", example = "700200")
        String operatorId,

        /** 变更操作者名（直读展示） */
        @Schema(description = "变更操作者名（直读展示）", example = "运营·技能管理员")
        String operatorName,

        /** 变更动作时刻（留痕生成时刻；读面按此时间倒序——最近先） */
        @Schema(description = "变更动作时刻（留痕生成时刻；读面按此时间倒序——最近先）")
        LocalDateTime operatedAt
) {
}
