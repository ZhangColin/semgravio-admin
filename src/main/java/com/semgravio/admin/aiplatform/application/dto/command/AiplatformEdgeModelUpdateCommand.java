package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台边缘调用点档位写北向命令——运营人员提交的「该边缘点覆盖态终态」意图，逐字镜像
 * aiplatform {@code EdgeModelUpdateCommand}（#366 边缘四点收编，issue #112 spec #101 B 批
 * B3）。PUT 全量语义、值面仅模型档位键一件（边缘点无 systemPrompt/工具开关/路由策略
 * 语义，窄形拒绝误带）：null/缺省/纯空白＝清空覆盖，装配回落 {@code AgentEdgePoint}
 * 代码缺省档位键 flash。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照 {@code AiplatformAgentConfigUpdateCommand}
 * 先例）：档位键即时校验与三座同口径（#367：非空须在模型档位面 {@code /model-tiers} 有行
 * 且启用——缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075，错值写不进去不再靠留痕回滚
 * 兜底；null＝清空回落缺省不校验），错误信封原样透传——BFF 不加戏不预校验。操作者身份
 * 不在命令体——经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕
 * （配置变更必留痕，缺头由 provider 域面守卫 PRJ_034＝4034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformEdgeModelUpdateCommand(

        /** 覆盖模型档位键（null/缺省/纯空白＝清空覆盖回落代码缺省 flash；即时校验须有行且启用） */
        @Schema(description = "覆盖模型档位键（ADR-0031 语义键，如 pro——档位表/model-tiers"
                + " 为值域正本；null/缺省/纯空白＝清空覆盖，装配回落代码缺省 flash。即时校验："
                + "非空须在模型档位面有行且启用——缺行 400 PRJ_074＝4074、停用 400 "
                + "PRJ_075＝4075）", example = "pro")
        String modelId
) {
}
