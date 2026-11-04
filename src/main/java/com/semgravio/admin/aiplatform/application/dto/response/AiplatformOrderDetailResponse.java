package com.semgravio.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台订单详情——北向出口，逐字镜像 aiplatform {@code BackofficeOrderDetailResponse}
 * （29 字段 + 内嵌价目历史，含 #297/#298 交付物类型字段对＋#335 双版本引用两肢〔#120 按
 * aiplatform #391 契约先行镜像〕＋#336 范围文本三段），无换型（spec #62 忠实透传）。
 *
 * <p>报价依据全量事实：PRD 快照正文（设计单快照即含设计物清单章）、交付物类型、双版本
 * 引用（随单冻结）、范围文本三段（scope 工作副本现值——scope 录入端点的读面）、价目
 * 历史（append-only，新 → 旧，带操作者留痕）、全部状态时点。取消/归档操作者两肢为
 * admin 侧管理员标识，存量行/无头 NULL。</p>
 *
 * @since 0.1.0
 */
public record AiplatformOrderDetailResponse(

        /** 订单标识（TSID 十进制字符串） */
        String id,

        /** 所属项目标识（TSID 十进制字符串） */
        String projectId,

        /** 项目名（软引用缺档为 null） */
        String projectName,

        /** 下单账号对外正身（OIDC sub——账号档案读口的寻址键；下单账号可空/缺档为 null） */
        String ownerExternalId,

        /** 下单用户昵称（下单账号可空/缺档为 null） */
        String ownerDisplayName,

        /** 订单状态（1=待报价 2=已报价 3=已支付 4=已归档 5=已取消） */
        Integer status,

        /** 状态中文名（provider 出口提供） */
        String statusName,

        /** 交付物类型（1=设计 2=系统 3=系统＋设计；下单冻结自项目终点类型，此后不随项目变更——设计单 PRD 快照即含设计物清单章、取件走 /{id}/design-package） */
        @Schema(description = "交付物类型 code（1=设计 2=系统 3=系统＋设计；下单冻结自项目终点类型，"
                + "此后不随项目变更——设计单 PRD 快照即含设计物清单章、设计资产包取件走 "
                + "/{id}/design-package、系统单走 /{id}/source-package）", example = "1")
        Integer deliverableType,

        /** 交付物类型中文名（前端直读展示，基本信息 tag） */
        String deliverableTypeName,

        /**
         * 下单冻结的已确认 PRD 版本号（#335 V37 双版本引用——#120 详情读面增补，对接
         * aiplatform #391 契约；遗留单 null）。
         */
        @Schema(description = "下单冻结的已确认 PRD 版本号（V37 双版本引用随单冻结；遗留单 null）",
                example = "3")
        Integer prdVersionNo,

        /** 下单冻结的已确认 Demo 版本号（#335；遗留单 null） */
        @Schema(description = "下单冻结的已确认 Demo 版本号（V37 双版本引用随单冻结；遗留单 null）",
                example = "2")
        Integer demoVersionNo,

        /** 当前总价（分；待报价 NULL） */
        Long amount,

        /** 币种（v1 恒 CNY；待报价 NULL） */
        String currency,

        /** 当前后台备注（最新价目行；待报价 NULL） */
        String note,

        /**
         * 范围文本·交付清单（#336 后台录入工作副本现值；未录入 null——scope 录入端点的
         * 现值回显，合同冻结集的底稿）。
         */
        @Schema(description = "范围文本·交付清单（scope 工作副本现值；未录入 null——范围录入端点"
                + "的现值回显，合同冻结集的底稿）",
                example = "交付清单：管理后台＋小程序（含 5 个核心页面）")
        String scopeDeliverables,

        /** 范围文本·验收方式（#336 工作副本；未录入 null） */
        @Schema(description = "范围文本·验收方式（scope 工作副本现值；未录入 null）",
                example = "验收方式：按 PRD v3 逐项验收")
        String scopeAcceptance,

        /** 范围文本·工期口径（#336 工作副本；未录入 null） */
        @Schema(description = "范围文本·工期口径（scope 工作副本现值；未录入 null）",
                example = "工期口径：4 周（含 1 周联调）")
        String scopeTimeline,

        /** 价目历史（新 → 旧，append-only 全量；带操作者，存量行操作者为空；待报价空表） */
        List<AiplatformPriceEntryResponse> priceEntries,

        /** 下单时 PRD 全文快照（交易标的，只插不改） */
        String prdSnapshot,

        /** 下单时间 */
        LocalDateTime createdAt,

        /** 首次报价时点（改价不刷新；待报价 NULL） */
        LocalDateTime quotedAt,

        /** 支付成功时点（未支付 NULL） */
        LocalDateTime paidAt,

        /** 归档时点（未归档 NULL） */
        LocalDateTime archivedAt,

        /** 重试归档操作者 id（支付链自动归档/缺透传头为 null） */
        String archiveOperatorId,

        /** 重试归档操作者名（直读；口径同取消留痕两列） */
        String archiveOperatorName,

        /** 取消时点（未取消 NULL） */
        LocalDateTime cancelledAt,

        /** 取消原因（运营取消必填留痕，运营内部口径；用户取消/未取消为 null） */
        String cancelReason,

        /** 取消操作者 id（admin 侧管理员 TSID；用户取消/缺透传头为 null） */
        String cancelOperatorId,

        /** 取消操作者名（直读；口径同价目行 operator） */
        String cancelOperatorName
) {
}
