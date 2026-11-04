package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台远端模型清单拉取回执——北向出口，逐字镜像 aiplatform
 * {@code BackofficeRemoteModelCatalogResponse}（#368 手动同步，spec #364-T4，issue
 * #110 B1，spec #101 B 批）：远端全量条目（含库内档位对照）＋过时档位清单——
 * 「勾选入库」不经此响应回写，复用档位创建写口（POST /model-tiers，语义键人工命名）。
 *
 * <p><strong>对照语义</strong>：{@code remoteModels[].trackedTierKeys} 空＝远端有、库里无
 * （建议入库——勾选后走档位创建，modelName 填该条 modelId）；非空＝已入库档位键清单；
 * {@code staleTiers}＝库内指向远端已无（该通道档位行的实际模型名不在远端清单——提示
 * 过时，升版/改指向前处置）。条目 {@code status} 如实转述上游（方舟：无 status＝活跃、
 * Shutdown/Retiring＝下架/退场中——建议入库时结合判断，接口不代拍）。每次全量真拉
 * 无缓存（手动同步——自动定时同步明确不做，ADR-0031）。纯读动作零留痕。</p>
 *
 * @since 0.1.0
 */
public record AiplatformRemoteModelCatalogResponse(

        /** 通道键（拉取寻址） */
        @Schema(description = "通道键（拉取寻址）", example = "volcengine")
        String providerKey,

        /** 拉取时刻（手动同步无缓存——每次全量真拉） */
        @Schema(description = "拉取时刻（手动同步无缓存——每次全量真拉）")
        LocalDateTime pulledAt,

        /** 远端全量模型条目（modelId 序稳定返回） */
        @Schema(description = "远端全量模型条目（modelId 序稳定返回；条目对照结构见嵌套行）")
        List<RemoteModelEntry> remoteModels,

        /** 库内指向远端已无的档位行（tierKey 序稳定返回） */
        @Schema(description = "库内指向远端已无的档位行（tierKey 序稳定返回——提示过时，升版/改指向前处置）")
        List<StaleTier> staleTiers
) {

    /**
     * 单条远端模型：模型 ID（入库即此值）＋辨识字段＋已入库档位键（空＝建议入库）。
     *
     * @param modelId         模型 ID（入库即此值——档位创建的 modelName 填此）
     * @param name            展示名
     * @param status          上游状态如实转述（方舟：无 status＝活跃、Shutdown/Retiring＝下架/退场中）
     * @param domain          领域（辨识字段，上游有则出）
     * @param trackedTierKeys 已入库档位键清单（空＝远端有库里无——建议入库）
     */
    public record RemoteModelEntry(

            /** 模型 ID（入库即此值——档位创建的 modelName 填此） */
            @Schema(description = "模型 ID（入库即此值——档位创建的 modelName 填此）",
                    example = "doubao-seed-1-6-250615")
            String modelId,

            /** 展示名 */
            @Schema(description = "展示名", example = "Doubao-Seed-1.6")
            String name,

            /** 上游状态如实转述（方舟：无 status＝活跃、Shutdown/Retiring＝下架/退场中——接口不代拍） */
            @Schema(description = "上游状态如实转述（方舟：无 status＝活跃、Shutdown/Retiring＝"
                    + "下架/退场中——建议入库时结合判断，接口不代拍）", example = "Shutdown")
            String status,

            /** 领域（辨识字段，上游有则出） */
            @Schema(description = "领域（辨识字段，上游有则出）")
            String domain,

            /** 已入库档位键清单（空＝远端有库里无——建议入库） */
            @Schema(description = "已入库档位键清单（空＝远端有库里无——建议入库；勾选入库"
                    + "复用档位创建写口）")
            List<String> trackedTierKeys
    ) {
    }

    /**
     * 库内过时档位：实际模型名不在远端清单（提示过时——升版或改指向）。
     *
     * @param tierKey     档位语义键
     * @param modelName   实际模型名（不在远端清单——过时判据）
     * @param displayName 显示名
     * @param enabled     启用态
     */
    public record StaleTier(

            /** 档位语义键 */
            @Schema(description = "档位语义键", example = "volc-main-pro")
            String tierKey,

            /** 实际模型名（不在远端清单——过时判据） */
            @Schema(description = "实际模型名（不在远端清单——过时判据）")
            String modelName,

            /** 显示名 */
            @Schema(description = "显示名")
            String displayName,

            /** 启用态 */
            @Schema(description = "启用态")
            boolean enabled
    ) {
    }
}
