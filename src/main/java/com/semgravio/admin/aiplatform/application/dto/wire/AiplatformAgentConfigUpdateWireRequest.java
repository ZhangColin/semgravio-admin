package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 智能体配置全量写命令体的 wire 镜像——与 aiplatform {@code AgentConfigUpdateCommand}
 * （#251 智能体运营配置，#246 技能线 T5；B3 issue #112 增路由强档件）字段同构：覆盖三件
 * （systemPrompt/modelId/routingStrongModelId）＋增强工具开关两件（webSearchEnabled/
 * fetchUrlEnabled）。
 *
 * <p>出站序列化口径：五字段无 Long/BigDecimal 特殊口径；开关两件为 <strong>Boolean 装箱</strong>
 * （与 provider 命令同型）——北向缺省＝null 出站在场（全局 Jackson 含 null，同技能安装
 * excludeDirs 先例），provider 侧 null＝缺省 true（开）；覆盖三件 null＝清空回落枚举/代码
 * 默认。modelId/routingStrongModelId 语义＝档位键（ADR-0031 语义键），非空即时校验须在
 * 模型档位面有行且启用（缺行 400 PRJ_074、停用 400 PRJ_075——provider 写用例裁决，BFF
 * 不代判不预校验，spec #79 忠实透传零加戏）。</p>
 *
 * <p>provider 命令另带路由开关/两阈值三件（{@code routingEnabled}/
 * {@code routingFileCountThreshold}/{@code routingSliceFeatureThreshold}，#354）——B3 票面
 * 只点名 routingStrongModelId 一族，三肢不出站＝provider null＝缺省态（开/60/4）；BFF 是
 * provider 后台唯一写面、三件从无可设处，结构上恒缺省态（票面字面口径）。</p>
 *
 * @param systemPrompt       覆盖 systemPrompt（null＝清空回落枚举默认）
 * @param modelId            覆盖模型档位键（null＝清空回落枚举默认）
 * @param webSearchEnabled   联网搜索开关（null＝provider 缺省 true 开）
 * @param fetchUrlEnabled    网页抓取开关（null＝provider 缺省 true 开）
 * @param routingStrongModelId 覆盖路由强档键（null＝清空回落代码缺省 pro）
 */
public record AiplatformAgentConfigUpdateWireRequest(
        String systemPrompt,
        String modelId,
        Boolean webSearchEnabled,
        Boolean fetchUrlEnabled,
        String routingStrongModelId
) {
}
