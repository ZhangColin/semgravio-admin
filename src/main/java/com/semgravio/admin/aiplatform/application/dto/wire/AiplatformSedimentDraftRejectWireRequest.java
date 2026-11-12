package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 沉淀草稿拒绝命令体的 wire 镜像——与 aiplatform
 * {@code SedimentDraftRejectCommand}（#464「数据标注」面）字段同构：拒绝理由单字段
 * （终态留档，拒绝须有据）。
 *
 * <p>出站序列化口径：单 String 字段，无 Long/BigDecimal 特殊口径。字段合法性（空白
 * 400 KNW_010＝数字业务码 2010）由 provider 拒绝用例裁决，BFF 不代判不预校验
 * （spec 忠实透传零加戏）。</p>
 *
 * @param reason 拒绝理由（随终态草稿留档，标注面可查；空白由 provider 判 KNW_010）
 */
public record AiplatformSedimentDraftRejectWireRequest(
        String reason
) {
}
