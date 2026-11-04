package com.semgravio.admin.aiplatform.application.dto.command;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台付款节点录入北向命令（issue #106，spec #102 A 批 A3）——逐字镜像 aiplatform
 * {@code EnterPaymentNodesCommand}（#337，「付款节点录入/开工门设定」后台承接口）：
 * <strong>整组替换</strong>——不收半组，每次全量提交；序号按列表序服务端派生（录入序
 * 即展示序），附言码随序自合同号定格。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制，同
 * {@link AiplatformContractActionCommand}）：字段合法性（至少一条/名称必填/金额为正
 * ORD_039、支付已开始重录窗口 ORD_044）由 provider 聚合守卫裁决，错误信封原样透传
 * ——BFF 零复算零加戏。开工门标志录入时定（开工款≠默认全款——#338 开工判定按本
 * 标志取节点集合）。操作者身份不在命令体——经框架透传头落痕。</p>
 *
 * @since 0.1.0
 */
public record AiplatformPaymentNodesCommand(

        /** 节点规格整组（≥1 条——名称必填/金额正数归 provider ORD_039 裁决；空组同拒） */
        @Schema(description = "节点规格整组（整组替换——不收半组；序号按列表序服务端派生、"
                + "附言码自合同号定格。单条合法性（名称必填/金额为正）与至少一条由 provider 裁决"
                + "ORD_039；支付活动已开始（有凭证/有留痕/非全待支付）拒重录 ORD_044")
                List<NodeSpec> nodes
) {

    /** 节点规格单条（逐字镜像 provider {@code PaymentNodeSpec}——名称＋绝对额＋开工门标志）。 */
    public record NodeSpec(

            /** 节点名称（必填，如「首期开工款」——缺失归 provider ORD_039） */
            @Schema(description = "节点名称（必填，如「首期开工款」——缺失由 provider 裁决 ORD_039）",
                    example = "首期开工款")
            String name,

            /** 金额（分，正数——绝对额录入；占比是展示换算不落库） */
            @Schema(description = "金额（分，正数——绝对额录入；占比由服务端现算不落库），"
                    + "非正由 provider 裁决 ORD_039", example = "1950000")
            Long amount,

            /** 开工门标志（录入时定；开工款≠默认全款——开工判定按本标志取节点集合） */
            @Schema(description = "开工门标志（录入时定；开工款≠默认全款——#338 开工判定按本标志"
                    + "取节点集合）", example = "true")
            boolean kickoffGate
    ) {
    }
}
