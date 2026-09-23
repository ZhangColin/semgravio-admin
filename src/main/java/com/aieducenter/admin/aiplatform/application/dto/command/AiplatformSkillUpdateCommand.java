package com.aieducenter.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能包显式更新北向命令——运营人员触发的「按来源包整体重拉快照」意图，逐字镜像
 * aiplatform {@code SkillUpdateCommand}（#250 显式更新，spec #79 定稿、issue #83）。
 * {@code sourcePackage} 取技能清单行原值回传（服务端再规范化，同源身份单源）——admin 不
 * 自拼原始 URL。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，照
 * {@code AiplatformSkillInstallCommand} 先例）：字段合法性由 provider 更新用例裁决（来源包
 * 空 400 SKL_014＝数字业务码 7014、未安装 404 SKL_012＝7012），错误信封原样透传——BFF 不加戏
 * 不重复校验。更新永远显式点（平台永不自动跟新远端，清单「有新版」标记只提示、不动作，
 * ADR-0021）。操作者身份不在命令体——经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（显式动作必留痕，缺头由 provider 域面守卫
 * SKL_009 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillUpdateCommand(

        /** 来源包标识（取技能清单行 sourcePackage 原值回传；显式更新＝重拉快照入库＋版本留痕，永不自动跟新） */
        @Schema(description = "来源包标识（取技能清单行 sourcePackage 原值回传；显式更新"
                + "＝重拉快照入库＋版本留痕，永不自动跟新）",
                example = "https://github.com/mattpocock/skills")
        String sourcePackage
) {
}
