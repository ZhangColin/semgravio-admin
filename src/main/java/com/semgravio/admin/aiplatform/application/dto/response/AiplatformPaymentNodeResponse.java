package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台付款节点——北向出口，逐字镜像 aiplatform {@code PaymentNodeResponse}
 * （#337），无增删字段、无换型：序号/名称/金额＋占比/五态/附言码/开工门标志＋渠道
 * 接口位＋累计已核到账与「还需支付」推导＋本人凭证清单。
 *
 * @since 0.1.0
 */
public record AiplatformPaymentNodeResponse(

        /** 节点标识（TSID 十进制字符串） */
        String id,

        /** 序号（1 起——录入序即展示序） */
        int seq,

        /** 名称（如「首期开工款」） */
        String name,

        /** 金额（分，绝对额录入——Long 北向出 JSON string） */
        Long amount,

        /** 占比（整数百分比，展示换算现算不落库——计划未录入节点时无此面） */
        Integer percentage,

        /**
         * 节点状态（五态）：1=待支付 2=支付处理中 3=支付失败或关闭 4=线下转账待核验
         * 5=已到账。Integer code 直通不建 typed enum；v1 只走凭证核验路径
         * （1→4→5 或打回回 1），2/3 渠道真接后启用。
         */
        @Schema(description = "节点状态 code（五态：1=待支付 2=支付处理中 3=支付失败或关闭 "
                + "4=线下转账待核验 5=已到账——v1 只走凭证核验路径：待支付 →（用户上传凭证）"
                + "线下转账待核验 →（后台核验）已到账或打回回待支付；2/3 渠道真接后启用）",
                example = "4")
        Integer status,

        /** 状态中文名（provider 出口提供，前端直读展示） */
        String statusName,

        /** 附言码（{@code <合同号>-K<序>}，转账回执关联位） */
        @Schema(description = "附言码（<合同号>-K<序>——线下转账回执对账的关联位，录入时自合同号"
                + "定格）", example = "SG-l5eq8m2k7c-K4")
        String remarkCode,

        /** 开工门标志（#338 开工判定按本标志取节点集合——开工款≠默认全款） */
        @Schema(description = "开工门标志（后台录入时定；开工判定取本标志为 true 的节点集合——"
                + "全部到账且合同已签即开工）", example = "true")
        boolean kickoffGate,

        /** 渠道类型（code）：1=线下转账 2=扫码（占位）——Integer code 直通 */
        @Schema(description = "渠道类型 code（1=线下转账 2=扫码〔占位，渠道真接后启用〕）",
                example = "1")
        Integer channelType,

        /** 渠道类型中文名（前端直读展示） */
        String channelTypeName,

        /** 渠道流水号（可空；核验录入时落值——渠道接口位） */
        @Schema(description = "渠道流水号（可空；到账核验录入时落值——渠道接口位，后续接渠道不大改"
                + "的承接字段）", example = "BANK-20261017-0001")
        String channelRefNo,

        /** 累计已核到账（分；不足留痕后仍累积，打回不动） */
        Long settledAmount,

        /** 还需支付（分＝未到账节点的 amount − settledAmount——前台推导面） */
        Long remainingAmount,

        /** 转已到账时点（实到时间；未到账 null） */
        LocalDateTime receivedAt,

        /** 凭证清单（上传序——多次上传追加保留不覆盖；上传仅代表已提交） */
        List<AiplatformPaymentVoucherResponse> vouchers
) {
}
