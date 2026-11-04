package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 模型档位更新命令体的 wire 镜像——与 aiplatform
 * {@code ModelTierUpdateCommand}（#367 档位更新）字段同构：{@code providerKey}/
 * {@code modelName}/{@code displayName} 终态全量替换（displayName null/空白＝清显示名
 * 回落语义键）；{@code enabled} <strong>缺省＝保持现值</strong>（终态字段不因缺省翻转
 * ——停用档换模型名/换通道不被意外复启，显式 true/false 才翻转）。
 *
 * <p><strong>缺省肢语义在 wire 请求体上保真</strong>（spec #101）：{@code enabled} 为
 * 可空肢（Boolean 装箱）——null 出站在场＝保持现值，不臆造默认值（若绑成原生 boolean，
 * null 会被落成 false＝翻转停用，语义反了）；{@code displayName} null 出站＝终态清空
 * （与通道 apiKey 的「null＝保持现值」有意不同——displayName 清空无副作用）。判定全在
 * provider 侧，BFF 零推导。</p>
 *
 * @param providerKey 所属通道键（终态全量替换，必填且须已存在）
 * @param modelName   实际模型名（终态全量替换，必填）
 * @param displayName 运营显示名（null/空白＝清空，回落语义键展示）
 * @param enabled     缺省＝保持现值；显式 true/false 才翻转
 */
public record AiplatformModelTierUpdateWireRequest(
        String providerKey,
        String modelName,
        String displayName,
        Boolean enabled
) {
}
