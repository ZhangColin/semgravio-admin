package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型供应商通道更新北向命令——运营人员提交的「通道行终态」意图，逐字镜像
 * aiplatform {@code ModelProviderUpdateCommand}（#367 通道更新，issue #110 B1，spec
 * #101 B 批）。PUT 全量语义下的<strong>缺省肢＝保持现值</strong>特例：baseUrl 终态
 * 全量替换（必填）；apiKey 与 enabled 缺省＝保持现值（两肢同是「不可/不宜因缺省翻转」
 * 的终态字段——apiKey 读面只出掩码、缺省清空＝读一次即丢 key；enabled 是有意状态、
 * 缺省归一 true 会把停用通道在换 key 时意外复启）。
 *
 * <p><strong>apiKey 特例（掩码回显的必然对偶）</strong>：null/空白＝保持现值
 * （round-trip 安全），携现值掩码形（如 {@code ****abcd}）同判保持；换 key 才写
 * 全量新值（写面唯一明文入口）。清除 key 无写面——下线通道走 {@code enabled=false}
 * （凭证随行留置不影响装配——停用即明确报错）。以上判定全在 provider 侧，BFF 零推导
 * 零校验（照 {@code AiplatformAgentConfigUpdateCommand} 先例不带 bean 校验注解）。
 * {@code enabled} 为 <strong>Boolean 装箱</strong>（与 provider 命令同型）：null 出站
 * 在场＝保持现值——若用原生 boolean，北向缺省会绑成 false（翻转停用），语义反了。
 * 操作者身份不在命令体——经框架自动透传 {@code X-User-Id/X-User-Name} 出站落痕
 * （缺头 PRJ_034＝4034 由 provider 拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelProviderUpdateCommand(

        /** 通道 baseUrl（终态全量替换，必填——缺省 400 PRJ_069） */
        @Schema(description = "通道 baseUrl（终态全量替换，必填——缺省 400 PRJ_069＝数字"
                + "业务码 4069）", example = "https://ark.cn-beijing.volces.com/api/v3")
        String baseUrl,

        /** apiKey：写全量新值＝换 key（写面唯一明文入口）；null/空白/等于现值掩码形＝保持现值（round-trip 安全） */
        @Schema(description = "apiKey：写全量新值＝换 key（写面唯一明文入口——读面一律掩码"
                + "尾 4 位）；null/空白/等于现值掩码形＝保持现值（读面掩码回显的 round-trip "
                + "安全语义）。清除 key 无写面——下线通道走 enabled=false")
        String apiKey,

        /** 启用态（装箱——缺省 null＝保持现值，显式 true/false 才翻转；停用通道换 key/换地址不被意外复启） */
        @Schema(description = "启用态（缺省＝保持现值，显式 true/false 才翻转——停用通道换 "
                + "key/换地址不被意外复启；停用＝引用该通道的装配明确报错，不静默换道）")
        Boolean enabled
) {
}
