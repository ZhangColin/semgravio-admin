package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 模型档位 wire 响应——与 aiplatform
 * {@code BackofficeModelTierResponse}（#367 档位维护）字段同构：GET 清单/单行、POST
 * 创建、PUT 更新回执同形。
 *
 * <p>档位＝语义键（{@code tierKey} 引用腿——绑定面绑定的值）→实际模型名＋所属通道＋
 * 显示名＋启用态（清单 {@code enabled=true} 键集＝绑定下拉数据源与改档写口即时校验
 * 放行值域）。{@code displayName} null＝回落语义键展示；{@code enabled} 为原生
 * boolean（provider 恒在场）。</p>
 *
 * @param tierKey      档位语义键（引用腿）
 * @param providerKey  所属通道键（模型串前缀腿）
 * @param modelName    实际模型名（版本升级改此处、全部引用自动跟上）
 * @param displayName  运营显示名（null＝回落语义键）
 * @param enabled      启用态
 * @param operatorId   最近写者 id（null＝种子/迁移写入）
 * @param operatorName 最近写者名（直读展示）
 * @param updatedAt    最近写入时刻
 */
public record AiplatformModelTierWireResponse(
        String tierKey,
        String providerKey,
        String modelName,
        String displayName,
        boolean enabled,
        String operatorId,
        String operatorName,
        LocalDateTime updatedAt
) {
}
