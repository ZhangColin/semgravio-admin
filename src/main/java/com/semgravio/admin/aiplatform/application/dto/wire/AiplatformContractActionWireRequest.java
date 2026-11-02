package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 合同后台动作命令体的 wire 镜像——与 aiplatform {@code ContractActionCommand}
 * （#336 后台机机面，advance/fail/withdraw/void 四动作共用）字段同构：依据必填自由文本。
 *
 * <p>字段合法性（缺失 ORD_022、超长至多 1000 字 ORD_023、状态守卫 ORD_021）归 provider
 * 聚合守卫，BFF 不代判不代填——同 {@link AiplatformOrderCancelWireRequest} 形制。</p>
 *
 * @param basis 依据（必填，至多 1000 字——缺失/超长归 provider ORD_022/023）
 */
public record AiplatformContractActionWireRequest(
        String basis
) {
}
