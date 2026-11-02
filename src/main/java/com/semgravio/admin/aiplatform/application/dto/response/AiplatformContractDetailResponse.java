package com.semgravio.admin.aiplatform.application.dto.response;

import java.util.List;

/**
 * AI 平台后台合同详情——北向出口，逐字镜像 aiplatform
 * {@code BackofficeContractDetailResponse}（#336 承接全量面），三段结构：
 * {@code contract} 全字段（含「需更新」派生标志与范围快照冻结面）＋{@code prdSnapshot}
 * PRD 确认版全文冗余拷贝（后台依据面）＋{@code transitions} 动作留痕时间线
 * （新→旧，append-only 全量）。无增删字段、无换型（spec #102 忠实透传）。
 *
 * @since 0.1.0
 */
public record AiplatformContractDetailResponse(

        /** 合同全字段（含「需更新」派生标志与范围快照冻结面） */
        AiplatformContractResponse contract,

        /** PRD 确认版全文冗余拷贝（合同自含，不依赖版本表与订单存亡；后台推进决定前的依据面） */
        String prdSnapshot,

        /** 动作留痕（新 → 旧，append-only 全量——动作/两侧状态/依据/操作者两肢/时间） */
        List<AiplatformContractTransitionResponse> transitions
) {
}
