package com.semgravio.admin.aiplatform.application.dto.wire;

/**
 * aiplatform 合同签署标记命令体的 wire 镜像——与 aiplatform {@code SignContractCommand}
 * （#336 后台机机面）字段同构：party Integer code（provider BaseEnum 按 code 反序列化）
 * ＋依据必填。
 *
 * <p>字段合法性（非签署进行中 ORD_021、已落签幂等拒 ORD_028、依据 ORD_022/023）归 provider
 * 聚合守卫，BFF 不代判不代填——同 {@link AiplatformContractActionWireRequest} 形制。</p>
 *
 * @param party 签署位（1=甲方〔客户方〕 2=乙方〔服务方〕——Integer code 直通）
 * @param basis 依据（必填，至多 1000 字）
 */
public record AiplatformContractSignWireRequest(
        Integer party,
        String basis
) {
}
