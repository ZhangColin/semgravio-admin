package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同全字段——北向出口，逐字镜像 aiplatform {@code ContractResponse}
 * （#336：用户面合同预览卡＋后台承接口共用同构 DTO），无增删字段、无换型
 * （spec #102 忠实透传）。详情 {@code contract} 段与创建重建回执共用。
 *
 * <p>类型口径：{@code status} Integer code（六态）+ {@code statusName} 中文名（前端直读
 * *Name）；{@code needsUpdate} boolean；{@code version} int；{@code amount} Long（分）；
 * 双版本引用 Integer；双方签署位 boolean＋落签时点（未签 null）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractResponse(

        /** 合同标识（TSID 十进制字符串） */
        String id,

        /** 合同编号（SG- 风格，全局唯一——SG-＋TSID base36，创建即定格） */
        String contractNo,

        /** 所属订单标识（TSID 十进制字符串） */
        String orderId,

        /**
         * 合同状态（六态人工推进）：1=准备中 2=待核对与签署 3=签署进行中 4=签署完成
         * 5=签署失败或过期 6=已撤回或作废。Integer code 直通不建 typed enum。
         */
        @Schema(description = "合同状态 code（六态人工推进：1=准备中 2=待核对与签署 3=签署进行中 "
                + "4=签署完成 5=签署失败或过期 6=已撤回或作废——线性单向 1→2→3→4，末边须双方均签；"
                + "5/6 旁落终态）", example = "3")
        Integer status,

        /** 状态中文名（provider 出口提供，前端直读展示） */
        String statusName,

        /** 合同版本（草稿同号重录 +1；重建从 1 起） */
        int version,

        /**
         * 「需更新」派生标志：草稿态（1/2）且冻结集与订单现值不一致——改价/改范围后
         * 待后台重录（record）重新定格；非草稿态恒 false。
         */
        @Schema(description = "「需更新」派生标志（草稿态且冻结集与订单现值不一致——改价/改范围后"
                + "待后台重录重新冻结并清零；非草稿态恒 false）", example = "false")
        boolean needsUpdate,

        /** 甲方主体（客户方；后台录入，未录入 null） */
        String partyA,

        /** 乙方主体（服务方；后台录入，未录入 null） */
        String partyB,

        /** 项目范围（后台录入，未录入 null） */
        String projectScope,

        /** 付款计划文本面（后台录入，未录入 null；节点对象归付款计划域） */
        String paymentPlan,

        /** 范围快照·交付清单（冻结副本——创建/重录时取订单范围文本工作副本定格，此后不漂移） */
        String scopeDeliverables,

        /** 范围快照·验收方式（冻结副本，口径同上） */
        String scopeAcceptance,

        /** 范围快照·工期口径（冻结副本，口径同上） */
        String scopeTimeline,

        /** 冻结金额（分，随冻结价目行定格；改价后待重录重新定格） */
        Long amount,

        /** 币种（v1 恒 CNY） */
        String currency,

        /** PRD 确认版本号（下单冻结集冗余） */
        Integer prdVersionNo,

        /** Demo 确认版本号引用（下单冻结集） */
        Integer demoVersionNo,

        /** 甲方签署位（未签/已签——签署标记落位不自动迁移状态，「签署完成」由推进显式达成） */
        boolean partyASigned,

        /** 甲方落签时点（未签 null） */
        LocalDateTime partyASignedAt,

        /** 乙方签署位 */
        boolean partyBSigned,

        /** 乙方落签时点（未签 null） */
        LocalDateTime partyBSignedAt,

        /** 创建时间（确认范围/后台重建时点） */
        LocalDateTime createdAt
) {
}
