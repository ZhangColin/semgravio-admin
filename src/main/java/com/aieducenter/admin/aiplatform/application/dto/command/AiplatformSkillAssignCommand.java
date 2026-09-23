package com.aieducenter.admin.aiplatform.application.dto.command;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台槽位指派北向命令——运营人员提交的整包替换意图，逐字镜像 aiplatform
 * {@code SkillSlotAssignCommand}（#249 槽位指派，spec #79 定稿、issue #82）。PUT 全量语义：
 * {@code skillIds} 即该槽位终态（未列入即解绑、空清单＝清空），支持按来源包勾满后送全量。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，照
 * {@code AiplatformSkillInstallCommand} 先例）：字段合法性由 provider 指派用例裁决（未寻址/
 * 畸形 TSID 404 SKL_001、内置柄 400 SKL_011、空/未知槽位 404 SKL_010），错误信封原样透传
 * ——BFF 不加戏不重复校验。指派目标只收安装库行 TSID 柄（柄取技能清单行原值回传，opaque 串
 * 透传不解析）。操作者身份不在命令体——经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（整包替换留最近动作者，缺头由 provider 域面
 * 守卫 SKL_009 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSkillAssignCommand(

        /** 指派技能柄全量清单（安装行 TSID 十进制串，整包替换——清单即终态；空清单＝清空该槽位） */
        @Schema(description = "指派技能柄全量清单（安装行 TSID 十进制串，整包替换语义——"
                + "清单即终态，未列入即解绑；空清单＝清空该槽位；内置 builtin: 柄不可"
                + "指派 400 SKL_011）",
                example = "[\"7600000000001\", \"7600000000003\"]")
        List<String> skillIds
) {
}
