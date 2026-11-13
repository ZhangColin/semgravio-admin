package com.semgravio.admin.aiplatform.application.dto.response;

import java.util.List;

/**
 * AI 平台原子改窗回执——北向出口，逐字镜像 aiplatform {@code TimeBandRuleRewindowResponse}
 * （#497 原子改窗——对称单价表 {@code UnitPriceEntryRepriceResponse}）：单调用「关当前版本
 * 全部行＋开新版本」的两段结果。被关行保留其原开行操作者（不被改写）；新行操作者取本次
 * 请求（X-User-Id/X-User-Name 透传头落痕，缺头落空）。中途任一守卫失败两段都不落
 * （事务回滚），回执即库内事实。
 *
 * @param closed 被关版本全部行（关行时点＝新版本生效起点；含其原操作者留痕与区间两端）
 * @param opened 新版本全部行（窗口清单随行落库，清单即终态）
 */
public record AiplatformTimeBandRuleRewindowResponse(
        List<AiplatformTimeBandRuleResponse> closed,
        List<AiplatformTimeBandRuleResponse> opened
) {
}
