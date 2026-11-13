package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 原子改窗回执的 wire 镜像——与 aiplatform {@code TimeBandRuleRewindowResponse}
 * （#497 原子改窗——对称单价表 {@code UnitPriceEntryRepriceResponse}）字段同构：单调用
 * 「关当前版本全部行＋开新版本」的两段结果。被关行保留其原开行操作者（不被改写）；新行
 * 操作者取本次请求（X-User-Id/X-User-Name 透传头落痕，缺头落空）。中途任一守卫失败两段
 * 都不落（事务回滚）。
 *
 * @param closed 被关版本全部行（关行时点＝新版本生效起点；含其原操作者留痕与区间两端）
 * @param opened 新版本全部行（窗口清单随行落库，清单即终态）
 */
public record AiplatformTimeBandRuleRewindowWireResponse(
        List<AiplatformTimeBandRuleWireResponse> closed,
        List<AiplatformTimeBandRuleWireResponse> opened
) {
}
