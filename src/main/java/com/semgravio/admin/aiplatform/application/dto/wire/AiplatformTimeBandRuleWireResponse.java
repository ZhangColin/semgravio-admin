package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.Instant;

/**
 * aiplatform 后台时段规则行的 wire 镜像——与 aiplatform {@code TimeBandRuleResponse}
 * （#497／ADR-0044 峰谷 1/4，清单行/开行回执/停用回执共形；改窗回执的 closed/opened
 * 行元素亦本形）字段同构：通道 × 时段带 × 星期口径 × UTC 窗口 × 生效区间＋操作者。
 *
 * <p>{@code band} 为 Integer code（1=峰 2=谷——provider 侧 BaseEnum 落库 code，BFF 直通
 * 零 typed enum）＋{@code bandName} 中文名随行；{@code dayMask} 为星期口径 bitmask
 * （ISO 周序 Mon=1 Tue=2 … Sun=64，合法域 1..127）＋{@code dayMaskName} 连续段压缩直读名
 * （31→「周一至周五」——provider 直译，BFF 不复译）；{@code startMinute}/{@code endMinute}
 * 为 UTC 分钟（end 不含，须 start&lt;end——跨午夜窗口 v1 不支持、拆两行）；{@code effectiveTo}
 * 为 null 即当前行，历史行含区间两端。行操作者两列＝该行最近管理动作（开行/改窗落新行、
 * 停用落被关行）的留痕；存量行/无头请求（含种子脚本种入行）为 null。</p>
 *
 * @param id            规则行标识（TSID 十进制字符串）
 * @param provider      模型提供方（匹配键）
 * @param band          时段带 code（1=峰 2=谷）
 * @param bandName      时段带名（直读展示）
 * @param dayMask       星期口径 bitmask（ISO 周序 Mon=1…Sun=64）
 * @param dayMaskName   星期口径直读名（连续段压缩：31→「周一至周五」）
 * @param startMinute   UTC 窗口起点（含）
 * @param endMinute     UTC 窗口终点（不含）
 * @param effectiveFrom 生效起点（含）
 * @param effectiveTo   生效终点（不含；null = 当前行）
 * @param operatorId    操作者标识（admin 侧管理员 id 字符串；存量行/无头为 null）
 * @param operatorName  操作者昵称（直读展示；同上落空口径）
 */
public record AiplatformTimeBandRuleWireResponse(
        String id,
        String provider,
        Integer band,
        String bandName,
        Integer dayMask,
        String dayMaskName,
        Integer startMinute,
        Integer endMinute,
        Instant effectiveFrom,
        Instant effectiveTo,
        String operatorId,
        String operatorName
) {
}
