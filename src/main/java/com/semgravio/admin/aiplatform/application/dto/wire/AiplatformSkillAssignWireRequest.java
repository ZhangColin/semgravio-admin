package com.semgravio.admin.aiplatform.application.dto.wire;

import java.util.List;

/**
 * aiplatform 槽位指派命令体的 wire 镜像——与 aiplatform {@code SkillSlotAssignCommand}
 * （#249 槽位指派）字段同构：指派技能柄全量清单（PUT 整包替换——清单即终态）。
 *
 * <p>出站序列化口径：{@code skillIds} 为 List&lt;String&gt;（opaque 串透传），无 Long/BigDecimal
 * 特殊口径；空清单照常出 {@code []}（清空该槽位是合法终态，非缺省）。字段合法性（未寻址/
 * 畸形 TSID SKL_001、内置柄 SKL_011）由 provider 指派用例裁决，BFF 不代判不预校验
 * （spec #79 忠实透传零加戏）。</p>
 *
 * @param skillIds 指派技能柄全量清单（安装行 TSID 十进制串；空清单＝清空该槽位）
 */
public record AiplatformSkillAssignWireRequest(
        List<String> skillIds
) {
}
