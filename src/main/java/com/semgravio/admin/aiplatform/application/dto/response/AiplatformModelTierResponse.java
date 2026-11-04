package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台模型档位读面行——北向出口，逐字镜像 aiplatform
 * {@code BackofficeModelTierResponse}（#367 档位维护，issue #111 B2，spec #101 B 批）：
 * GET 清单/单行、POST 创建、PUT 更新回执同形（单对象行）。
 *
 * <p>档位行形态五要素：语义键（{@code tierKey} 引用腿——绑定面〔三座/边缘点/路由强档〕
 * 绑定的值，清单 {@code enabled=true} 键集即绑定下拉数据源与改档写口即时校验放行值域）/
 * 实际模型名（{@code modelName} 通道侧 Model ID——语义键与模型版本解耦：方舟名带日期
 * 后缀、季度级更新，版本升级改档位行指向、全部引用自动跟上）/所属通道（{@code providerKey}
 * 模型串「前缀:实际模型名」的前缀腿）/显示名（{@code displayName} 可空＝回落语义键
 * 展示）/启用态。{@code enabled} 为原生 boolean（provider 恒在场），admin 同型镜像。</p>
 *
 * @since 0.1.0
 */
public record AiplatformModelTierResponse(

        /** 档位语义键（引用腿——绑定面绑定的值） */
        @Schema(description = "档位语义键（引用腿——三座/边缘点/路由强档绑定的值；清单 "
                + "enabled=true 键集＝绑定下拉数据源与改档写口即时校验放行值域）",
                example = "flash")
        String tierKey,

        /** 所属通道键（模型串「前缀:实际模型名」的前缀腿） */
        @Schema(description = "所属通道键（模型串「前缀:实际模型名」的前缀腿——换供应商只动"
                + "档位行指向、绑定不动）", example = "volcengine")
        String providerKey,

        /** 实际模型名（通道侧 Model ID；版本升级改此处、全部引用自动跟上） */
        @Schema(description = "实际模型名（通道侧 Model ID；方舟名带日期后缀——季度级版本升级"
                + "改此处指向、全部引用自动跟上）", example = "deepseek-v4-flash-ga-260731")
        String modelName,

        /** 运营显示名（后台绑定下拉的人话标签；null＝回落语义键展示） */
        @Schema(description = "运营显示名（后台绑定下拉的人话标签；null＝回落语义键展示）",
                example = "快模型档")
        String displayName,

        /** 启用态（停用＝既有绑定装配报错、新绑定被即时校验拒绝） */
        @Schema(description = "启用态（停用＝既有绑定装配报错 PRJ_061、新绑定被即时校验拒绝 "
                + "PRJ_075——临时下线用停用不删行）")
        boolean enabled,

        /** 最近写者 id（null＝种子/迁移写入） */
        @Schema(description = "最近写者 id（admin 侧 TSID 十进制串；null＝种子/迁移写入）",
                example = "700210")
        String operatorId,

        /** 最近写者名（直读展示） */
        @Schema(description = "最近写者名（直读展示）", example = "运营·智能体管理员")
        String operatorName,

        /** 最近写入时刻 */
        @Schema(description = "最近写入时刻")
        LocalDateTime updatedAt
) {
}
