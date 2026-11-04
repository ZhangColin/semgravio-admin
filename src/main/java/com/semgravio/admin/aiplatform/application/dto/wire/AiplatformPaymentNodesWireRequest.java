package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 付款节点录入命令体的 wire 镜像——与 aiplatform
 * {@code EnterPaymentNodesCommand}（#337 后台机机面）字段同构：整组替换——不收
 * 半组，每次全量提交；序号按列表序服务端派生（录入序即展示序），附言码随序自
 * 合同号定格。
 *
 * <p>字段合法性（至少一条/名称必填/金额为正 ORD_039、重录窗口 ORD_044）归
 * provider 聚合守卫，BFF 不代判不代填。amount 为 Long（分）——生产 mapper 出
 * JSON string，provider 默认可回读（订单 quote 的 amount 同款口径）。</p>
 *
 * @param nodes 节点规格（≥1 条：名称/金额〔分〕/开工门标志）
 */
public record AiplatformPaymentNodesWireRequest(
        List<NodeSpecWire> nodes
) {

    /** 节点规格单条（镜像 provider {@code PaymentNodeSpec}：名称＋绝对额＋开工门标志）。 */
    public record NodeSpecWire(
            String name,
            Long amount,
            boolean kickoffGate
    ) {
    }
}
