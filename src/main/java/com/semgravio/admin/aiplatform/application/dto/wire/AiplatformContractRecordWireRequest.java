package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 合同草稿重录命令体的 wire 镜像——与 aiplatform {@code RecordContractCommand}
 * （#336 后台机机面）字段同构：四个预览字段可空整组录入＋依据必填。
 *
 * <p>字段合法性（重录窗口 ORD_031、内容超长 ORD_032、依据 ORD_022/023、冻结集前置门
 * ORD_025/026）归 provider 聚合守卫，BFF 不代判不代填——同
 * {@link AiplatformContractActionWireRequest} 形制。</p>
 *
 * @param partyA       甲方主体（可空＝未录入；至多 200 字）
 * @param partyB       乙方主体（可空；至多 200 字）
 * @param projectScope 项目范围（可空；至多 2000 字）
 * @param paymentPlan  付款计划文本面（可空；至多 2000 字）
 * @param basis        依据（必填，至多 1000 字）
 */
public record AiplatformContractRecordWireRequest(
        String partyA,
        String partyB,
        String projectScope,
        String paymentPlan,
        String basis
) {
}
