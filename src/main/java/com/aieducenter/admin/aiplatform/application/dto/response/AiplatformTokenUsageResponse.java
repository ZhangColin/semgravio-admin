package com.aieducenter.admin.aiplatform.application.dto.response;

/**
 * token 用量五档（BFF 北向出口）——镜像 aiplatform {@code TokenUsage}（成本域四端点共用载荷，
 * issue #67）：input/output/cacheRead/cacheWrite/reasoning。
 *
 * <p>五档为 <strong>primitive long</strong>——北向出 JSON 数字（与 provider 出口同形；
 * 非包装 Long 的字符串口径）。空聚合＝五档全零，非 null。<strong>按张用量不在五档</strong>
 * （aiplatform #288 张与 token 并列不混算——按张事件五档为零，张数仅经 cost 桶与 unpriced
 * 档位呈现，见 {@link AiplatformCostOverviewResponse}）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformTokenUsageResponse(

        /** 输入 token */
        long input,

        /** 输出 token */
        long output,

        /** 缓存读 token */
        long cacheRead,

        /** 缓存写 token */
        long cacheWrite,

        /** 推理 token */
        long reasoning
) {
}
