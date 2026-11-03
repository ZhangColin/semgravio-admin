package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 边缘调用点档位写命令体的 wire 镜像——与 aiplatform {@code EdgeModelUpdateCommand}
 * （#366，issue #112 B3）字段同构：值面仅模型档位键一件（窄形拒绝误带——边缘点无 prompt/
 * 工具开关/路由策略语义）。
 *
 * <p>出站序列化口径：单字段 String 无 Long/BigDecimal 特殊口径，boot 默认 mapper 即生产
 * 口径；null 出站在场（全局 Jackson 含 null）＝provider 侧清空覆盖回落代码缺省 flash。
 * 档位键即时校验（#367：非空须在模型档位面有行且启用——PRJ_074/075）由 provider 写用例
 * 裁决，BFF 不代判不预校验（spec #79 忠实透传零加戏）。</p>
 *
 * @param modelId 覆盖模型档位键（null＝清空回落代码缺省 flash）
 */
public record AiplatformEdgeModelUpdateWireRequest(
        String modelId
) {
}
