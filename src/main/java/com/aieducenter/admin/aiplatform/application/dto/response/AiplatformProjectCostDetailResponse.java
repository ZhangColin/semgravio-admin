package com.aieducenter.admin.aiplatform.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单项目成本下钻响应（BFF 北向出口）——逐字镜像 aiplatform {@code BackofficeProjectCostDetailResponse}
 * （issue #67）：subject 回显 + 窗口回显 + 五档总量 + 成本分桶 + 未配价档位 + 分模型/分智能体两分解。
 *
 * <p>复用 bySubject 聚合口径（与全局总览/项目清单同一换算规则）；无生效单价的分量不进 {@code cost}，
 * 其档位在 {@code unpriced} 如实呈现（互补不重叠）。无用量/查无此号返回全零 total 与空结构，
 * 不是错误（明确空态、不 404）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformProjectCostDetailResponse(

        /** 项目标识（计量 subject 原值回显） */
        String projectId,

        /** 窗口起点（含；原样回显） */
        Instant from,

        /** 窗口终点（不含；原样回显） */
        Instant to,

        /** 总量（五档分列） */
        AiplatformTokenUsageResponse total,

        /** 平台成本（币种分桶；含图片生成成本同桶混算；全未配价/无事件时空 Map） */
        @Schema(description = "平台成本（币种分桶直读不折算：键 = ISO 4217 币种码、值 = 金额；"
                + "token 与图片生成成本〔按张×时点生效单价〕同桶混算不单列；"
                + "无生效单价的分量不进本桶——其集合在 unpriced 呈现，互补不重叠）",
                example = "{\"USD\": 12.34}")
        Map<String, BigDecimal> cost,

        /** 未配价档位清单（provider/model/档位码序；六档含 6=按张；与 cost 互补不重叠、无 token 计数） */
        List<UnpricedTier> unpriced,

        /** 分模型聚合（provider + model 为单价表匹配键） */
        List<ModelUsage> byModel,

        /** 分智能体聚合（agentKind 裸维度串 + agentKindName 随行） */
        List<AgentKindUsage> byAgentKind
) {

    /** 未配价标注项（tokenKind Integer code + tokenKindName，1=输入 … 5=推理 6=按张——按张档
     * 未配价同公式进清单；档位用量汇总走全局 unpriced 端点，此处无计数）。 */
    public record UnpricedTier(
            String provider,
            String model,
            Integer tokenKind,
            String tokenKindName
    ) {
    }

    /** 分模型聚合项。 */
    public record ModelUsage(String provider, String model, AiplatformTokenUsageResponse tokens) {
    }

    /** 分智能体聚合项（agentKind 原值 + agentKindName 中文名，辅助标记为 null）。 */
    public record AgentKindUsage(String agentKind, String agentKindName, AiplatformTokenUsageResponse tokens) {
    }
}
