package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型档位创建北向命令——运营人员提交的「新建档位（语义键）」意图，逐字镜像
 * aiplatform {@code ModelTierCreateCommand}（#367 档位落库，spec #364-T3，ADR-0031，
 * issue #111 B2，spec #101 B 批）。语义键随体：已存在 409 PRJ_068＝数字业务码 4068
 * （语义键唯一——改既有档走 PUT）；所属通道必须已有行（400 PRJ_071＝4071——先建通道
 * 行再挂档位）；实际模型名必填（400 PRJ_070＝4070）——均由 provider 聚合守卫裁决，
 * 错误信封原样透传。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformModelProviderCreateCommand}
 * 先例）：字段合法性由 provider 写用例裁决，BFF 不加戏不重复校验。语义键即引用腿
 * ——建行后即可被绑定（绑定改档写口的即时校验放行值＝清单 enabled=true 键集）；
 * 「勾选入库」（远端清单建议入库条）复用本写口：modelName 填所选条目 modelId、语义键
 * 人工命名。{@code displayName} 可空（null/空白＝回落语义键展示）；{@code enabled} 为
 * <strong>Boolean 装箱</strong>（与 provider 命令同型）：缺省＝null 透传＝provider 缺省
 * true（启用）——若用原生 boolean，北向缺省会被绑定层落成 false（停用），语义反了。
 * 操作者身份不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name}
 * 自动透传出站（创建即留痕，缺头由 provider 域面守卫 PRJ_034＝4034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelTierCreateCommand(

        /** 档位语义键（运营短名——引用腿：三座/边缘点/路由强档绑定的值；已存在 409 PRJ_068） */
        @Schema(description = "档位语义键（运营短名，如 flash/pro/doubao-flagship——引用腿："
                + "智能体运营配置与路由强档绑定的就是它；已存在 409 PRJ_068＝数字业务码 4068，"
                + "语义键唯一——改既有档走 PUT）", example = "doubao-lite")
        String tierKey,

        /** 所属通道键（必须已存在——先建通道行再挂档位；不存在 400 PRJ_071） */
        @Schema(description = "所属通道键（必须已存在——先建通道行再挂档位；不存在 400 "
                + "PRJ_071＝数字业务码 4071）", example = "volcengine")
        String providerKey,

        /** 实际模型名（通道侧 Model ID——方舟名带日期后缀，版本升级改档位行指向；必填 400 PRJ_070） */
        @Schema(description = "实际模型名（通道侧 Model ID，方舟名带日期后缀——季度级版本升级"
                + "改档位行指向、全部引用自动跟上；必填——缺省 400 PRJ_070＝数字业务码 4070；"
                + "「勾选入库」时填远端清单所选条目 modelId）", example = "doubao-seed-2-1-flash-250615")
        String modelName,

        /** 运营显示名（后台绑定下拉的人话标签；可空＝回落语义键展示） */
        @Schema(description = "运营显示名（后台绑定下拉的人话标签；可空＝回落语义键展示）",
                example = "豆包轻量")
        String displayName,

        /** 启用态（装箱——缺省 null＝provider 缺省 true 启用；停用＝既有绑定装配报错、新绑定被即时校验拒绝） */
        @Schema(description = "启用态（缺省 true＝启用；停用＝既有绑定装配报错 PRJ_061、"
                + "新绑定被即时校验拒绝 PRJ_075——临时下线用停用不删行）")
        Boolean enabled
) {
}
