package com.semgravio.admin.aiplatform.application.dto.response;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 项目成本清单行响应（BFF 北向出口）——逐字镜像 aiplatform {@code BackofficeProjectCostResponse}
 * （issue #67）：窗口内该项目的总量 + 平台成本（币种分桶）+ 全未配价标注。
 *
 * <p>排序服务端定死成本降序（全未配价项目排后且 allUnpriced=true、同序按 projectId 升序稳定）；
 * 用量驱动：无用量项目不出现在清单（空窗＝空清单）。{@code projectId} 为计量 subject 原值
 * （已删项目的历史花费照列——成本观测不抹历史；项目名归 admin 侧按 id 互查）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProjectCostResponse(

        /** 项目标识（计量 subject 原值回显，TSID 十进制串） */
        String projectId,

        /** 总量（五档分列） */
        AiplatformTokenUsageResponse total,

        /** 平台成本（币种分桶；含图片生成成本同桶混算；全未配价时空 Map——不伪装 0） */
        @Schema(description = "平台成本（币种分桶直读不折算：键 = ISO 4217 币种码、值 = 金额；"
                + "token 与图片生成成本〔按张×时点生效单价〕同桶混算不单列；"
                + "全未配价时为空对象，成本标量缺失不伪装 0——allUnpriced 同行为 true）",
                example = "{\"USD\": 12.34}")
        Map<String, BigDecimal> cost,

        /** 全未配价标注（有用量但无任何已配价分量——true 时成本不完整） */
        boolean allUnpriced
) {
}
