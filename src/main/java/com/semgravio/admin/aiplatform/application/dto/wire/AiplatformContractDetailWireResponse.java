package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 后台合同详情的 wire 镜像——与 aiplatform {@code BackofficeContractDetailResponse}
 * （#336 已冻结）字段同构、字段序一致（三段）。用于 Jackson 反序列化。
 *
 * @since 0.1.0
 */
public record AiplatformContractDetailWireResponse(

        /** 合同全字段（含「需更新」派生标志与范围快照冻结面） */
        AiplatformContractWireResponse contract,

        /** PRD 确认版全文冗余拷贝（合同自含，不依赖版本表与订单存亡；后台依据面） */
        String prdSnapshot,

        /** 动作留痕（新 → 旧，append-only 全量——动作/两侧状态/依据/操作者两肢/时间） */
        List<AiplatformContractTransitionWireResponse> transitions
) {
}
