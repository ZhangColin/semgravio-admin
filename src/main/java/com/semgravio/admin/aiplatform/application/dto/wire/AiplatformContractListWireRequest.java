package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 合同清单 wire 层过滤参数（issue #104）——由应用层从
 * {@code AiplatformContractQuery} 映射而来，与 provider {@code GET /api/backoffice/contracts}
 * 查询参数同形。status 多选在客户端拼为逗号分隔单值（签名协议按参数名去重）。
 *
 * @since 0.1.0
 */
public record AiplatformContractListWireRequest(
        List<Integer> status,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        String orderId,
        String contractNo
) {
}
