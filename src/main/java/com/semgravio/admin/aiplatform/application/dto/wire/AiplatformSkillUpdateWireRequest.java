package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 技能包显式更新命令体的 wire 镜像——与 aiplatform {@code SkillUpdateCommand}
 * （#250 显式更新）字段同构：来源包标识单字段（取清单行 sourcePackage 原值回传，服务端
 * 再规范化，同源身份单源）。
 *
 * <p>出站序列化口径：单 String 字段，无 Long/BigDecimal 特殊口径。字段合法性（来源包空
 * SKL_014、未安装 SKL_012）由 provider 更新用例裁决，BFF 不代判不预校验（spec #79 忠实
 * 透传零加戏）。</p>
 *
 * @param sourcePackage 来源包标识（清单行 sourcePackage 原值；规范化仓库地址身份）
 */
public record AiplatformSkillUpdateWireRequest(
        String sourcePackage
) {
}
