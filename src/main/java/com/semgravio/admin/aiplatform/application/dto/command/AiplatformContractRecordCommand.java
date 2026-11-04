package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台合同草稿重录北向命令（issue #105，spec #102 A 批 A2）——逐字镜像 aiplatform
 * {@code RecordContractCommand}（#336「需更新」处置正路）：同号重录——预览字段集整组录入＋
 * 冻结集取订单现值重新定格＋合同版本 +1＋「需更新」清零。仅草稿态（准备中/待核对与签署）
 * 可达，签署进行中起变动走撤回/作废重建（ORD_031）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性（窗口
 * ORD_031、内容超长 ORD_032、依据 ORD_022/023、冻结集前置门 ORD_025/026）由 provider 聚合
 * 守卫裁决，错误信封原样透传——BFF 零推导零校验。四个预览字段可空（可空＝未录入，provider
 * 空白归一 null——BFF 不代填不归一）。操作者身份不在命令体——经框架透传头落痕。</p>
 *
 * @since 0.1.0
 */
public record AiplatformContractRecordCommand(

        /** 甲方主体（可空＝未录入；至多 200 字——超长归 provider ORD_032） */
        @Schema(description = "甲方主体（可空＝未录入；至多 200 字——超长由 provider 裁决 ORD_032；"
                + "整组录入＝预览字段集一次定格）", example = "张三")
        String partyA,

        /** 乙方主体（可空；至多 200 字——超长归 provider ORD_032） */
        @Schema(description = "乙方主体（可空＝未录入；至多 200 字——超长由 provider 裁决 ORD_032）",
                example = "语义引力科技有限公司")
        String partyB,

        /** 项目范围（可空；至多 2000 字——超长归 provider ORD_032） */
        @Schema(description = "项目范围（可空；至多 2000 字——超长由 provider 裁决 ORD_032）",
                example = "管理后台＋小程序")
        String projectScope,

        /** 付款计划文本面（可空；至多 2000 字——节点对象归付款计划域，此处文本面） */
        @Schema(description = "付款计划文本面（可空；至多 2000 字——节点对象归付款计划域，"
                + "此处文本面）", example = "开工款 50%，验收 50%")
        String paymentPlan,

        /** 依据（必填，至多 1000 字——重录留痕正身；缺失/超长归 provider ORD_022/023） */
        @Schema(description = "操作依据（必填，至多 1000 字——草稿重录留痕正身；重录后版本 +1、"
                + "「需更新」清零）", example = "改价后重录")
        String basis
) {
}
