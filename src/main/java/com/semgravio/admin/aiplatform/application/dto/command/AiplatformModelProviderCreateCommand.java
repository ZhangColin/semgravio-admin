package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型供应商通道创建北向命令——运营人员提交的「新建接入通道」意图，逐字镜像
 * aiplatform {@code ModelProviderCreateCommand}（#367 通道落库，spec #364-T3，ADR-0031，
 * issue #110 B1，spec #101 B 批）。键名（＝协议路由前缀）随体携带：已存在 409
 * PRJ_067＝数字业务码 4067，baseUrl 必填 400 PRJ_069＝4069——均由 provider 聚合守卫
 * 裁决，错误信封原样透传。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformSkillUpdateCommand} /
 * {@code AiplatformAgentConfigUpdateCommand} 先例）：字段合法性由 provider 写用例裁决，
 * BFF 不加戏不重复校验。{@code apiKey} 写收<strong>全量值</strong>（写面唯一明文入口——
 * null/空白＝暂不配置，通道行落空 key，装配按 PRJ_063 明确报错不静默兜底）；{@code enabled}
 * 为 <strong>Boolean 装箱</strong>（与 provider 命令同型）：缺省＝null 透传＝provider
 * 缺省 true（启用）——若用原生 boolean，北向缺省会被绑定层落成 false（停用），语义反了。
 * 操作者身份不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name}
 * 自动透传出站（创建即留痕，缺头由 provider 域面守卫 PRJ_034＝4034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelProviderCreateCommand(

        /** 通道键名＝协议路由前缀（模型串「前缀:实际模型名」的前缀腿；OpenAI 兼容通道统一接，SDK 内置前缀走原生 provider） */
        @Schema(description = "通道键名＝协议路由前缀（模型串「前缀:实际模型名」的前缀腿；"
                + "OpenAI 兼容通道统一接，SDK 内置前缀走原生 provider）", example = "volcengine")
        String providerKey,

        /** 通道 baseUrl（必填——创建上下文注入 SDK，覆盖依赖库默认地址；缺省 400 PRJ_069） */
        @Schema(description = "通道 baseUrl（创建上下文注入 SDK，覆盖依赖库默认地址；"
                + "必填——缺省 400 PRJ_069＝数字业务码 4069）",
                example = "https://ark.cn-beijing.volces.com/api/v3")
        String baseUrl,

        /** 凭证全量值（写面唯一明文入口；null/空白＝暂不配置——装配按 PRJ_063 明确报错不静默兜底） */
        @Schema(description = "凭证 apiKey 全量值（写面唯一明文入口——读面一律掩码尾 4 位；"
                + "null/空白＝暂不配置——装配按 PRJ_063〔HTTP 503，数字业务码 4063〕明确报错"
                + "不静默兜底）")
        String apiKey,

        /** 启用态（装箱——缺省 null＝provider 缺省 true 启用；停用＝引用该通道的装配明确报错） */
        @Schema(description = "启用态（缺省 true＝启用；停用＝引用该通道的装配明确报错，"
                + "不静默换道）")
        Boolean enabled
) {
}
