package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同后台动作北向命令（issue #105，spec #102 A 批 A2——推进/失败或过期/撤回/作废
 * 四动作共用）——逐字镜像 aiplatform {@code ContractActionCommand}（#336 合同域六态承接）：
 * 依据＝人工推进留痕的必填自由文本（操作人/时间由 provider 服务端随行落）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，同
 * {@link AiplatformOrderCancelCommand}）：字段合法性（依据缺失 ORD_022、依据超长至多 1000 字
 * ORD_023、状态守卫 ORD_021）由 provider 聚合守卫裁决，错误信封原样透传——BFF 零状态机复算
 * 零加戏（守卫归 provider 裁决，spec #102）。操作者身份不在命令体——经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（provider 合同动作以
 * 操作者为审计正身、缺头 ORD_035 拒——前端无法伪造留痕）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractActionCommand(

        /** 依据（必填，至多 1000 字——append-only 留痕行的正身；缺失/超长归 provider ORD_022/023） */
        @Schema(description = "操作依据（必填，至多 1000 字——append-only 留痕行的正身。缺失/超长"
                + "由 provider 裁决：ORD_022/ORD_023）", example = "双方落签，合同完成")
        String basis
) {
}
