package com.semgravio.admin.aiplatform.application.dto.query;

/**
 * AI 平台沉淀草稿标注队列查询参数（BFF 透传 aiplatform）——北向三过滤维度，字段名与
 * aiplatform {@code GET /api/backoffice/sediment-drafts} 查询参数逐字镜像（issue #124，
 * provider aiplatform #464「数据标注」面）。
 *
 * <p>三维度可组合、均可缺省（缺省＝全量，含终态行——终态留档可查；待办视角须显式
 * {@code status=1}）：① {@code projectId} 来源项目 id 精确（字符串透传，<strong>宽松
 * 口径</strong>——畸形值由 provider {@code Tsid.parseOrNull} 归 null 即缺省不过滤，
 * 与 kind 非法即 400 <strong>有意不同</strong>）；② {@code kind} 类别单选
 * （CASE/MODULE/RULE，大小写不敏感——非法 400 KNW_011 带合法取值表，由 provider
 * 裁决、admin 不预校验）；③ {@code status} 状态单选 Integer code（1=在途 2=已激活
 * 3=已拒绝；非整数在 admin 绑定层即 404 框架类型不匹配口径，合法整数但未知 code
 * 由 provider 绑定层 400 带合法取值表——骑框架非 KNW 码）。排序服务端定死＝自荐
 * 时间倒序（id 倒序稳定），无 sort 参数。</p>
 *
 * <p>分页参数 {@code page}/{@code size} 不在此（controller 独立绑定，缺省 1/20 镜像
 * provider {@code Pagination}——clamp 归 provider，BFF 不重复夹取）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSedimentDraftQuery(

        /** 来源项目 id 精确（TSID 十进制串宽松透传——畸形值＝provider 缺省不过滤，非 400） */
        String projectId,

        /** 类别单选（CASE/MODULE/RULE，大小写不敏感）；null = 全部；非法由 provider 判 400 KNW_011 */
        String kind,

        /** 状态单选（1=在途 2=已激活 3=已拒绝）；null = 全部（含终态行——终态留档可查） */
        Integer status
) {
}
