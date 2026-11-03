package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 边缘调用点档位读面的 wire 镜像——与 aiplatform
 * {@code BackofficeEdgeModelConfigResponse}（#366 边缘四点收编，issue #112 B3）字段同构：
 * 生效值＋覆盖标记＋缺省预览＋最近写者（值面仅模型档位键——字段名对齐三座面）。GET 读面
 * 与 PUT 回执同形（provider 写入后重读同一视图）。
 *
 * <p>覆盖标记 provider 恒在场（原生 boolean，wire 同型）；operator 两肢与 updatedAt 可空
 * （null＝从未配置——无覆盖行＝全回落）。modelId 语义＝档位键（ADR-0031 语义键）。</p>
 *
 * @param agentKey          边缘点稳定键（naming/compaction/classification/precheck）
 * @param agentName         展示名
 * @param modelId           生效模型档位键（库覆盖值或代码缺省）
 * @param modelIdOverridden 模型档位是否库覆盖值
 * @param defaultModelId    代码缺省档位键（清空覆盖即落此值）
 * @param operatorId        最近写者 id（null＝从未配置）
 * @param operatorName      最近写者名
 * @param updatedAt         最近写入时刻（null＝从未配置）
 */
public record AiplatformEdgeModelConfigWireResponse(
        String agentKey,
        String agentName,
        String modelId,
        boolean modelIdOverridden,
        String defaultModelId,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
