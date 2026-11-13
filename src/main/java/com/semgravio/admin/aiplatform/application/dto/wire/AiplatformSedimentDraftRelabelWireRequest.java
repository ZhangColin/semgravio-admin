package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 沉淀草稿改标签命令体的 wire 镜像——与 aiplatform
 * {@code SedimentDraftRelabelCommand}（#464「数据标注」面）字段同构：目标类别键单字段。
 *
 * <p>出站序列化口径：单 String 字段，无 Long/BigDecimal 特殊口径。字段合法性（非法
 * 取值 400 KNW_011＝数字业务码 2011 带合法取值表）由 provider 改标签用例裁决，BFF
 * 不代判不预校验（spec 忠实透传零加戏）。</p>
 *
 * @param kind 目标类别键（CASE/MODULE/RULE，大小写不敏感归一大写稳定键；同类别幂等 no-op）
 */
public record AiplatformSedimentDraftRelabelWireRequest(
        String kind
) {
}
