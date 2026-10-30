package com.semgravio.admin.aiplatform.application.dto.command;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台技能包安装北向命令——运营人员提交的安装意图，逐字镜像 aiplatform
 * {@code SkillInstallCommand}（#248 快照安装，spec #79 定稿、issue #81）。装时 clone 解析
 * 全部 SKILL.md 固化入库（ADR-0021 快照安装、不订阅远端），版本＝装时 HEAD commit。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，照
 * {@code AiplatformOrderQuoteCommand} 先例）：字段合法性由 provider 安装用例裁决（地址空
 * SKL_002、零技能 SKL_005、SKILL.md 不合格 SKL_006 等），错误信封原样透传——BFF 不加戏
 * 不重复校验。excludeDirs v1 为手输目录段（provider 无仓库结构预览端点，spec #79）。
 * 操作者身份不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name}
 * 自动透传落痕（缺头由 provider 域面守卫 SKL_009 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillInstallCommand(

        /** 技能仓库 git 地址（https/ssh/本地路径；clone 走宿主 git 凭据，装时固化快照、版本＝装时 HEAD commit） */
        @Schema(description = "技能仓库 git 地址（https/ssh/本地路径；clone 走宿主 git 凭据，"
                + "装时固化快照、版本＝装时 HEAD commit）",
                example = "https://github.com/mattpocock/skills.git")
        String repoUrl,

        /** 排除目录段名单（可缺省）：技能的仓库相对路径任一段命中即不入库（如 deprecated 类目目录整支排除；段名精确匹配） */
        @Schema(description = "排除目录段名单（可缺省）：技能的仓库相对路径任一段命中即"
                + "不入库（如 deprecated 类目目录整支排除；段名精确匹配）",
                example = "[\"deprecated\", \"in-progress\"]")
        List<String> excludeDirs
) {
}
