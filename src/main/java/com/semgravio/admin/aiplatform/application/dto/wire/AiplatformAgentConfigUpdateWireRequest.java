package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 智能体配置全量写命令体的 wire 镜像——与 aiplatform {@code AgentConfigUpdateCommand}
 * （#251 智能体运营配置，#246 技能线 T5）字段同构：覆盖两件（systemPrompt/modelId）＋增强
 * 工具开关两件（webSearchEnabled/fetchUrlEnabled）。
 *
 * <p>出站序列化口径：四字段无 Long/BigDecimal 特殊口径；开关两件为 <strong>Boolean 装箱</strong>
 * （与 provider 命令同型）——北向缺省＝null 出站在场（全局 Jackson 含 null，同技能安装
 * excludeDirs 先例），provider 侧 null＝缺省 true（开）；覆盖两件 null＝清空回落枚举默认。
 * 字段合法性（档位名不做白名单校验等）由 provider 写用例裁决，BFF 不代判不预校验
 * （spec #79 忠实透传零加戏）。</p>
 *
 * @param systemPrompt      覆盖 systemPrompt（null＝清空回落枚举默认）
 * @param modelId           覆盖模型档位裸名（null＝清空回落枚举默认）
 * @param webSearchEnabled  联网搜索开关（null＝provider 缺省 true 开）
 * @param fetchUrlEnabled   网页抓取开关（null＝provider 缺省 true 开）
 */
public record AiplatformAgentConfigUpdateWireRequest(
        String systemPrompt,
        String modelId,
        Boolean webSearchEnabled,
        Boolean fetchUrlEnabled
) {
}
