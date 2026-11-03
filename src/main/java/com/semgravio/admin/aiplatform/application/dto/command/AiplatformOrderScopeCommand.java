package com.semgravio.admin.aiplatform.application.dto.command;

/**
 * AI 平台订单范围录入北向命令（issue #108，spec #102 A 批 A5）——逐字镜像 aiplatform
 * {@code EnterScopeCommand}（#336 后台机机面）：三段（交付清单/验收方式/工期口径）
 * <strong>整组替换</strong>——不收半组，每次全量提交。scope 是合同冻结集的<strong>工作副本</strong>
 * （用户「确认范围并准备合同」确认面的数据源；确认后本副本仍可改，变动触发草稿合同
 * 「需更新」派生标志、后台重录重新冻结）——工作副本现值语义，provider 侧无 append-only
 * 留痕，操作者不落痕（区别于报价/取消/归档三写操作）。
 *
 * <p><strong>刻意不带 bean 校验注解</strong>（与 provider 命令同款形制）：字段合法性
 * （三段缺失 ORD_029、每段至多 2000 字 ORD_030）由 provider 聚合守卫裁决，错误信封
 * 原样透传——BFF 不加戏不重复校验（忠实透传零加戏，spec #102）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformOrderScopeCommand(

        /** 交付清单（必填，至多 2000 字——缺失/超长归 provider 聚合守卫 ORD_029/030） */
        String scopeDeliverables,

        /** 验收方式（必填，至多 2000 字——校验同上） */
        String scopeAcceptance,

        /** 工期口径（必填，至多 2000 字——校验同上） */
        String scopeTimeline
) {
}
