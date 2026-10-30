package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 技能包安装命令体的 wire 镜像——与 aiplatform {@code SkillInstallCommand}
 * （#248 快照安装）字段同构：git 仓库地址＋可选排除目录段名单。
 *
 * <p>出站序列化口径：两字段均为 String/List&lt;String&gt;，无 Long/BigDecimal 特殊口径；
 * {@code excludeDirs} 可缺省（null 照常在场——全局 Jackson 含 null，provider 侧 null＝
 * 不排除）。字段合法性（地址空 SKL_002、零技能 SKL_005 等）由 provider 安装用例裁决，
 * BFF 不代判不预校验（spec #79 忠实透传零加戏）。</p>
 *
 * @param repoUrl     技能仓库 git 地址（https/ssh/本地路径，宿主 git 凭据适用）
 * @param excludeDirs 排除目录段名单（可缺省）：技能的仓库相对路径任一段命中即整支不入库（段名精确匹配）
 */
public record AiplatformSkillInstallWireRequest(
        String repoUrl,
        List<String> excludeDirs
) {
}
