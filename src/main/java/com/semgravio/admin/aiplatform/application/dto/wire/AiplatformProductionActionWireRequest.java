package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 生产承接动作命令体的 wire 镜像——与 aiplatform
 * {@code ProductionHandoffActionCommand}（#338，推进）字段同构：依据＝人工推进留痕的
 * 必填自由文本（操作人/时间由 provider 服务端随行落）。
 *
 * <p>字段合法性（缺失 PRJ_057、超长 PRJ_058、操作者必携 PRJ_059、状态守卫
 * PRJ_056）归 provider 聚合守卫，BFF 不代判不代填——同
 * {@link AiplatformContractActionWireRequest} 形制。</p>
 *
 * @param basis 依据（必填，至多 1000 字——append-only 留痕行的正身）
 */
public record AiplatformProductionActionWireRequest(
        String basis
) {
}
