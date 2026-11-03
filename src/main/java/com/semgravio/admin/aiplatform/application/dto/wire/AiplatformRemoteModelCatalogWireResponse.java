package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 远端模型清单拉取 wire 响应——与 aiplatform
 * {@code BackofficeRemoteModelCatalogResponse}（#368 手动同步，spec #364-T4）字段同构：
 * 远端全量条目（含库内档位对照）＋过时档位清单——「勾选入库」不经此响应回写，复用
 * 档位创建写口（POST /model-tiers）。
 *
 * <p>对照语义：{@code remoteModels[].trackedTierKeys} 空＝远端有库里无（建议入库）；
 * {@code staleTiers}＝库内指向远端已无（提示过时）。条目 status 如实转述上游（方舟：
 * 无 status＝活跃、Shutdown/Retiring＝下架/退场中）。每次全量真拉无缓存；纯读动作
 * 零留痕。</p>
 *
 * @param providerKey  通道键（拉取寻址）
 * @param pulledAt     拉取时刻（手动同步无缓存）
 * @param remoteModels 远端全量模型条目（modelId 序稳定返回）
 * @param staleTiers   库内指向远端已无的档位行（tierKey 序稳定返回）
 */
public record AiplatformRemoteModelCatalogWireResponse(
        String providerKey,
        LocalDateTime pulledAt,
        List<RemoteModelEntryWireResponse> remoteModels,
        List<StaleTierWireResponse> staleTiers
) {

    /** 单条远端模型：模型 ID（入库即此值）＋辨识字段＋已入库档位键（空＝建议入库）。 */
    public record RemoteModelEntryWireResponse(
            String modelId,
            String name,
            String status,
            String domain,
            List<String> trackedTierKeys
    ) {
    }

    /** 库内过时档位：实际模型名不在远端清单（提示过时——升版或改指向）。 */
    public record StaleTierWireResponse(
            String tierKey,
            String modelName,
            String displayName,
            boolean enabled
    ) {
    }
}
