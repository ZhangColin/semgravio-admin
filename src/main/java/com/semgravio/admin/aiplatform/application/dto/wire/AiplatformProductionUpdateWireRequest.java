package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 生产承接信息更新命令体的 wire 镜像——与 aiplatform
 * {@code UpdateProductionHandoffCommand}（#338 后台机机面，「生产承接更新」之字段面）
 * 字段同构：<strong>整组替换</strong>语义——未录入段传 null 即清空（预计开始时间清空
 * ＝回待安排呈现，「仅真实值显示」）。
 *
 * <p>字段合法性（依据必填 PRJ_057、超长 PRJ_058、操作者必携 PRJ_059）由 provider
 * 聚合守卫裁决，BFF 不代判不代填——同 {@link AiplatformContractActionWireRequest} 形制。</p>
 *
 * @param contact          交付联系人（可空，至多 200 字）
 * @param nextStep         下一步说明（可空，至多 1000 字）
 * @param progressNote     进展更新（可空，至多 2000 字）
 * @param estimatedStartAt 预计开始时间（可空＝待安排）
 * @param basis            依据（必填，至多 1000 字——append-only 留痕行的正身）
 */
public record AiplatformProductionUpdateWireRequest(
        String contact,
        String nextStep,
        String progressNote,
        LocalDateTime estimatedStartAt,
        String basis
) {
}
