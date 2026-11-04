package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同签署标记北向命令（issue #105，spec #102 A 批 A2）——逐字镜像 aiplatform
 * {@code SignContractCommand}（#336，电子签渠道真接前的后台承接口径）：落一方签署位＋时点，
 * <strong>不自动迁移状态</strong>（「签署完成」由 advance 显式推进，双方均签守卫 ORD_027
 * 归 provider）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性
 * （非签署进行中态 ORD_021、该方已签幂等拒 ORD_028、依据缺失/超长 ORD_022/023）由 provider
 * 聚合守卫裁决，错误信封原样透传。party 为 Integer code 直通（1=甲方〔客户方〕 2=乙方〔服务
 * 方〕——provider BaseEnum 按 code 反序列化，非法值 provider 侧 400 框架信封；admin 不建
 * typed enum 不预校验，对齐六态 status 先例）。操作者身份不在命令体——经框架透传头落痕。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractSignCommand(

        /** 签署位（1=甲方〔客户方〕 2=乙方〔服务方〕——Integer code 直通，非法值归 provider 400） */
        @Schema(description = "签署位：1=甲方（客户方） 2=乙方（服务方）——Integer code 直通，"
                + "非法值由 provider 裁决 400；仅签署进行中态可落签（ORD_021）", example = "2")
        Integer party,

        /** 依据（必填，至多 1000 字——签署留痕正身；缺失/超长归 provider ORD_022/023） */
        @Schema(description = "操作依据（必填，至多 1000 字——签署标记留痕正身；该方已签幂等拒"
                + " ORD_028）", example = "乙方盖章")
        String basis
) {
}
