package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台边缘调用点档位读面——北向出口，逐字镜像 aiplatform
 * {@code BackofficeEdgeModelConfigResponse}（#366 边缘四点收编，spec #364-T2／ADR-0031，
 * issue #112 spec #101 B 批 B3）：边缘调用点（一次性轻调用）的模型档位生效值＋覆盖标记＋
 * 缺省预览＋最近写者。GET 读面与 PUT 回执同形（provider 写入后重读同一视图）。
 *
 * <p>与三座读面（{@code AiplatformAgentConfigResponse}）同机制不同值面——边缘点仅模型
 * 档位键（无 systemPrompt/工具开关/路由件），字段名对齐三座面（admin 侧同一渲染件可复用）。
 * modelId 语义＝<strong>档位键</strong>（ADR-0031 语义键，V44 存量已刷档）；写口即时校验
 * 同三座口径（非空须在模型档位面有行且启用——缺行 400 PRJ_074＝4074、停用 400
 * PRJ_075＝4075）。寻址稳定键四把：naming=项目取名 / compaction=上下文压缩 /
 * classification=入口分类 / precheck=技能预检（缺省档位键全部 flash——轻调用不烧重档）；
 * 未知键 404 PRJ_064＝数字业务码 4064（admin 字符串透传不预校验）。无覆盖行＝全回落
 * （覆盖标记 false、写者两肢与 updatedAt null 如实出 JSON null）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformEdgeModelConfigResponse(

        /** 边缘点稳定键（naming=项目取名 / compaction=上下文压缩 / classification=入口分类 / precheck=技能预检） */
        @Schema(description = "边缘点稳定键（naming=项目取名 / compaction=上下文压缩 / "
                + "classification=入口分类 / precheck=技能预检；未知键 404 PRJ_064＝"
                + "数字业务码 4064）", example = "naming")
        String agentKey,

        /** 展示名 */
        @Schema(description = "展示名", example = "项目取名")
        String agentName,

        /** 生效模型档位键（库覆盖值或代码缺省——装配实取值） */
        @Schema(description = "生效模型档位键（ADR-0031 语义键，如 flash/pro——库覆盖值或"
                + "代码缺省，对照 modelIdOverridden 知来源）", example = "flash")
        String modelId,

        /** 模型档位是否库覆盖值（true＝覆盖；false＝代码缺省 flash） */
        @Schema(description = "模型档位是否库覆盖值（true＝覆盖；false＝代码缺省 flash）")
        boolean modelIdOverridden,

        /** 代码缺省模型档位键（清空覆盖即落此值） */
        @Schema(description = "代码缺省模型档位键（清空覆盖即落此值——轻调用不烧重档）",
                example = "flash")
        String defaultModelId,

        /** 最近写者 id（admin 侧 TSID；null＝从未配置） */
        @Schema(description = "最近写者 id（admin 侧 TSID；null＝从未配置）", example = "700210")
        String operatorId,

        /** 最近写者名（直读展示） */
        @Schema(description = "最近写者名（直读展示）", example = "运营·智能体管理员")
        String operatorName,

        /** 最近写入时刻（null＝从未配置） */
        @Schema(description = "最近写入时刻（null＝从未配置）")
        LocalDateTime updatedAt
) {
}
