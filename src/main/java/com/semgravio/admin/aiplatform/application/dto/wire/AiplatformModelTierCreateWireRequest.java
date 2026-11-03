package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 模型档位创建命令体的 wire 镜像——与 aiplatform
 * {@code ModelTierCreateCommand}（#367 档位落库）字段同构：语义键随体（已存在 409
 * PRJ_068）、所属通道必须已有行（PRJ_071——先建通道再挂档位）、实际模型名必填
 * （PRJ_070）、显示名可空（＝回落语义键）、enabled 缺省 true。
 *
 * <p>字段合法性（语义键唯一、通道存在性、modelName 必填、操作者缺头 400 PRJ_034）由
 * provider 聚合守卫裁决，BFF 不代判不代填——同
 * {@link AiplatformModelProviderCreateWireRequest} 形制。{@code enabled} 为 Boolean
 * 装箱（与 provider 命令同型）——null 出站在场＝provider 缺省 true。</p>
 *
 * @param tierKey     档位语义键（运营短名——引用腿）
 * @param providerKey 所属通道键（必须已存在）
 * @param modelName   实际模型名（必填）
 * @param displayName 运营显示名（null/空白＝回落语义键）
 * @param enabled     启用态（缺省 true）
 */
public record AiplatformModelTierCreateWireRequest(
        String tierKey,
        String providerKey,
        String modelName,
        String displayName,
        Boolean enabled
) {
}
