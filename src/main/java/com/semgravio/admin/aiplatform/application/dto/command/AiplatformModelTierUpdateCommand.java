package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型档位更新北向命令——运营人员提交的「档位行终态」意图，逐字镜像 aiplatform
 * {@code ModelTierUpdateCommand}（#367 档位更新，issue #111 B2，spec #101 B 批）。换通道/
 * 升模型版本/改显示名/停启用皆此一处改指向（全部引用自动跟上）。PUT 全量语义：
 * {@code providerKey}/{@code modelName}/{@code displayName} <strong>终态全量替换</strong>
 * （displayName 可空＝清显示名回落语义键——null 即清空，区别于通道 apiKey 的「null＝保持
 * 现值」）；{@code enabled} <strong>缺省＝保持现值</strong>（终态字段不因缺省翻转——停用档
 * 换模型名/换通道不被意外复启，显式 true/false 才翻转）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformModelProviderUpdateCommand}
 * 先例）：providerKey/modelName 必填（缺省 400 PRJ_070）、所属通道必须已有行（400
 * PRJ_071）、档位不存在（404 PRJ_066）全由 provider 聚合守卫裁决，BFF 零推导零校验。
 * {@code enabled} 为 <strong>Boolean 装箱</strong>（与 provider 命令同型）：null 出站在场
 * ＝保持现值——若用原生 boolean，北向缺省会绑成 false（翻转停用），语义反了。操作者
 * 身份不在命令体——经框架自动透传 {@code X-User-Id/X-User-Name} 出站落痕（缺头
 * PRJ_034＝4034 由 provider 拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelTierUpdateCommand(

        /** 所属通道键（终态全量替换，必填且须已存在——不存在 400 PRJ_071） */
        @Schema(description = "所属通道键（终态全量替换，必填且须已存在——不存在 400 "
                + "PRJ_071＝数字业务码 4071，先建通道行）", example = "volcengine")
        String providerKey,

        /** 实际模型名（终态全量替换，必填——版本升级改此处指向、全部引用自动跟上；缺省 400 PRJ_070） */
        @Schema(description = "实际模型名（终态全量替换，必填——版本升级改此处指向、全部引用"
                + "自动跟上；缺省 400 PRJ_070＝数字业务码 4070）",
                example = "deepseek-v4-flash-ga-261031")
        String modelName,

        /** 运营显示名（终态替换——null/空白＝清空回落语义键展示，区别于通道 apiKey 的「null＝保持现值」） */
        @Schema(description = "运营显示名（终态替换——null/空白＝清空，回落语义键展示；区别于"
                + "通道 apiKey 的「null＝保持现值」——displayName 清空无副作用，apiKey 清空＝"
                + "读一次即丢 key）", example = "快模型档")
        String displayName,

        /** 启用态（装箱——缺省 null＝保持现值，显式 true/false 才翻转；停用档升版/换通道不被意外复启） */
        @Schema(description = "启用态（缺省＝保持现值，显式 true/false 才翻转——停用档换模型名/"
                + "换通道不被意外复启；停用＝既有绑定装配报错 PRJ_061、新绑定被即时校验拒绝 "
                + "PRJ_075——临时下线用停用不删行）")
        Boolean enabled
) {
}
