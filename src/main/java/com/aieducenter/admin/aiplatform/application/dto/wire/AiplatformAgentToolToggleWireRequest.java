package com.aieducenter.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 增强工具开关命令体的 wire 镜像——与 aiplatform {@code AgentToolToggleCommand}
 * （#252 工具面，#246 技能线 T6）字段同构：开关目标态单字段。
 *
 * <p>出站序列化口径：{@code enabled} 为 <strong>Boolean 装箱</strong>（与 provider 命令
 * 同型）——北向缺省＝null 出站在场（全局 Jackson 含 null，同智能体配置开关先例），provider
 * 侧 null＝目标态必填违例（400 PRJ_037＝数字业务码 4037，无缺省翻转语义）。目标态合法性
 * （仅增强工具接受、骨架/harness 内建 403 PRJ_036）由 provider 写用例裁决，BFF 不代判
 * 不预校验（spec #79 忠实透传零加戏）。</p>
 *
 * @param enabled 开关目标态（必填、无缺省：true=挂载回归装配面 / false=退出槽位装配面）
 */
public record AiplatformAgentToolToggleWireRequest(
        Boolean enabled
) {
}
