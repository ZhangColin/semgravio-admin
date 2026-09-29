package com.aieducenter.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 后台技能清单行的 wire 镜像——与 aiplatform {@code BackofficeSkillSummaryResponse}
 * （#247 技能库，清单行/安装回执共形）字段同构：内置（classpath 合成）与库中技能（安装＋
 * 自产）同权共形，来源包/版本标识/状态仅库行有值（内置随平台发版：来源包与版本标识 null、
 * 状态恒启用；自产行 sourcePackage/version＝固定虚拟值 self）。
 *
 * <p>{@code id} 为 <strong>opaque 串两形制</strong>（内置 {@code builtin:<技能名>}／安装 TSID 十进制串）
 * ——admin 字符串透传不解析不预校验（spec #79），畸形/未寻址柄由 provider 判 404 SKL_001。
 * {@code source} Integer code（1=内置 2=安装 3=自产）+ {@code sourceName} 中文名、{@code status} Integer
 * code（1=启用 2=停用）+ {@code statusName} 中文名随行（provider 出口提供，BFF 透传不臆造映射）。
 * {@code updateAvailable}＝来源包级远端有新版标记（null＝未检查过，内置恒 null）。</p>
 *
 * <p>使用计数观测面（#261，T2 追平）：{@code loadCount} 装箱 Long——provider 存储态 primitive
 * long（库行恒有值：0＝从未被加载过，含新装/新晋行；skill load 实际发生 +1），wire 上经
 * cartisan-web 全局 Long→ToStringSerializer 呈 <strong>JSON string</strong> 形态（同 TSID id 口径，
 * Jackson 反序列化 string→Long coercion）；仅内置行 null 不适用。{@code lastLoadedAt}
 * LocalDateTime（null＝从未被加载过）。</p>
 *
 * @param id              技能柄（详情/写口寻址；opaque 串两形制）
 * @param name            技能名（frontmatter name；安装行与来源包合成唯一键）
 * @param description     简介（frontmatter description）
 * @param source          来源 code（1=内置 2=安装 3=自产）
 * @param sourceName      来源名（直读展示）
 * @param sourcePackage   来源包标识（内置为 null、自产＝固定虚拟值 self）
 * @param version         版本标识（装时 commit；自产＝self；内置为 null）
 * @param status          状态 code（1=启用 2=停用；内置恒 1）
 * @param statusName      状态名（直读展示）
 * @param operatorId      最近管理动作操作者 id（安装＝装者、启停＝最近动作者；内置为 null）
 * @param operatorName    最近管理动作操作者名（直读展示；内置为 null）
 * @param updateAvailable 远端有新版标记（来源包级；null＝未检查过）
 * @param loadCount       加载次数（load 实际发生口径，指派/启停/清单重建不计数；0＝从未
 *                        加载；内置恒 null 不适用）
 * @param lastLoadedAt    最近加载时刻（null＝从未被加载过；内置恒 null 不适用）
 */
public record AiplatformSkillSummaryWireResponse(
        String id,
        String name,
        String description,
        Integer source,
        String sourceName,
        String sourcePackage,
        String version,
        Integer status,
        String statusName,
        String operatorId,
        String operatorName,
        Boolean updateAvailable,
        Long loadCount,
        LocalDateTime lastLoadedAt
) {
}
