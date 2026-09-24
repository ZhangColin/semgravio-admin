package com.aieducenter.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 工具面清单行（清单嵌套行与开关回执共形）的 wire 镜像——与 aiplatform
 * {@code BackofficeAgentToolResponse}（#252 工具面，#246 技能线 T6）字段同构：注册名＋
 * 类别＋挂载态＋一句描述。GET 清单嵌套行与 PUT 开关回执同形（provider 回执即该工具行）。
 *
 * <p>{@code kind} 为类别名字符串（provider 出口 {@code AgentToolKind.name()} 字符串化：
 * SKELETON=骨架／ENHANCEMENT=增强／HARNESS_BUILTIN=harness 内建）——admin 原样透传，
 * 不映射枚举（开关判定正本在 provider，spec #79 忠实透传零加戏）。{@code enabled} 为
 * provider 原生 boolean 恒在场（骨架/harness 内建恒 true；增强按运营配置生效态）。</p>
 *
 * @param name        工具注册名（模型可见名＝按名寻址键——PUT 开关与骨架锁死判定都按本名）
 * @param kind        工具面类别（SKELETON／ENHANCEMENT／HARNESS_BUILTIN——开关判定正本）
 * @param enabled     挂载态（骨架/harness 内建恒 true；增强按运营配置生效态——false＝在册但退出装配面）
 * @param description 一句职能描述
 */
public record AiplatformAgentToolWireResponse(
        String name,
        String kind,
        boolean enabled,
        String description
) {
}
