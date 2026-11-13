package com.semgravio.admin.aiplatform.application.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台沉淀草稿改标签北向命令——「数据标注」三键之一（issue #124，镜像 aiplatform
 * #464 {@code SedimentDraftRelabelCommand}）：在途草稿调整类别（纯队列分诊修正——
 * 不翻终态、不动正文）。改标签<strong>占目标类别在途名额</strong>：跨类别占目标类别
 * 在途序号最低空位（每项目每 kind 上限 3 的结构性约束跨类别生效），目标类别满
 * 409 KNW_012（数字业务码 2012）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（照后台命令 DTO 统一形制）：类别合法性
 * 由 provider 改标签用例裁决（非法取值 400 KNW_011＝数字业务码 2011 带合法取值表），
 * 错误信封原样透传——BFF 不加戏不重复校验（可选绑定：缺 body 不前置拦截，直出
 * provider 裁决）。操作者身份不在命令体——经框架透传头落痕（缺头 400 KNW_006＝2006）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformSedimentDraftRelabelCommand(

        /** 目标类别键（CASE/MODULE/RULE，大小写不敏感归一大写稳定键；同类别幂等 no-op；非法即 400 KNW_011） */
        @Schema(description = "目标沉淀物类别（CASE=需求案例 MODULE=功能模块 RULE=业务规则；"
                + "大小写不敏感；同类别幂等 no-op；非法取值 400 KNW_011＝数字业务码 2011）",
                example = "MODULE")
        String kind
) {
}
