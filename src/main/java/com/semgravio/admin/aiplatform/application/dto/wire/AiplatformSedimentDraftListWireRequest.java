package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 沉淀草稿标注队列的 wire 过滤参数——出站查询串成分（issue #124）。
 *
 * <p>与北向 {@code AiplatformSedimentDraftQuery} 字段一一对应（宽松 projectId 串＋
 * kind 串＋status Integer code），独立成 wire 记录以守 {@code AiplatformClient} 不
 * import query DTO 的分层口径（同订单/项目/素材清单先例）。三维度均可缺省（null
 * 出站省略＝缺省全量）。</p>
 *
 * @param projectId 来源项目 id 精确（宽松透传——畸形值 provider 归 null 不过滤）；null = 不限
 * @param kind      类别单选（CASE/MODULE/RULE）；null = 不限
 * @param status    状态单选 code（1=在途 2=已激活 3=已拒绝）；null = 不限（含终态行）
 */
public record AiplatformSedimentDraftListWireRequest(
        String projectId,
        String kind,
        Integer status
) {
}
