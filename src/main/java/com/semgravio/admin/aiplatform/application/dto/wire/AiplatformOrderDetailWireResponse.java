package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;

/**
 * aiplatform 订单详情的 wire 镜像——与 aiplatform {@code BackofficeOrderDetailResponse}
 * （#155 价目留痕已冻结＋#298 交付物类型字段对＋#336 范围文本三段）字段同构
 * （29 字段 + 内嵌价目历史——#120 双版本两肢按 aiplatform #391 契约先行镜像），逐字镜像。
 *
 * <p>报价依据全量事实：PRD 快照正文（下单冻结）、双版本引用（#335 随单冻结）、范围文本
 * 三段（scope 工作副本现值——scope 录入端点的读面）、价目历史（append-only 全量，
 * 新 → 旧，带操作者）、全部状态时点。取消/归档操作者两肢为 admin 侧管理员标识
 * （透传头落痕），存量行/无头 NULL。</p>
 *
 * @since 0.1.0
 */
public record AiplatformOrderDetailWireResponse(

        /** 订单标识（TSID 十进制字符串） */
        String id,

        /** 所属项目标识（TSID 十进制字符串） */
        String projectId,

        /** 项目名（软引用缺档为 null） */
        String projectName,

        /** 下单账号对外正身（OIDC sub——账号档案读口的寻址键；下单账号可空/跨 BC 软引用缺档为 null） */
        String ownerExternalId,

        /** 下单用户昵称（下单账号可空/缺档为 null） */
        String ownerDisplayName,

        /** 订单状态（1=待报价 2=已报价 3=已支付 4=已归档 5=已取消） */
        Integer status,

        /** 状态中文名（provider 出口提供） */
        String statusName,

        /**
         * 交付物类型（1=设计 2=系统 3=系统＋设计；#297 下单冻结自项目终点类型，此后不随项目
         * 变更——设计单快照即含设计物清单章、取件走 /{id}/design-package）。Integer code 直通
         * 不建 typed enum（provider 枚举非本应用领域枚举，对齐 tokenKind 先例）。
         */
        Integer deliverableType,

        /** 交付物类型中文名（provider 出口提供） */
        String deliverableTypeName,

        /**
         * 下单冻结的已确认 PRD 版本号（#335 V37 双版本引用——域列已有，#120 按契约先行镜像
         * 对接 aiplatform #391 详情读面增补；遗留单 NULL、provider 发版前窗口缺席亦 NULL——
         * 两口径同形）。Integer 直通不建 typed enum（版本号是计数非枚举）。
         */
        Integer prdVersionNo,

        /** 下单冻结的已确认 Demo 版本号（#335；遗留单 NULL，口径同 prdVersionNo） */
        Integer demoVersionNo,

        /** 当前总价（分；待报价 NULL） */
        Long amount,

        /** 币种（v1 恒 CNY；待报价 NULL） */
        String currency,

        /** 当前后台备注（最新价目行；待报价 NULL） */
        String note,

        /** 范围文本·交付清单（provider #336 后台录入工作副本现值；未录入 null——scope 录入端点的现值回显） */
        String scopeDeliverables,

        /** 范围文本·验收方式（provider #336；未录入 null） */
        String scopeAcceptance,

        /** 范围文本·工期口径（provider #336；未录入 null） */
        String scopeTimeline,

        /** 价目历史（新 → 旧，append-only 全量；带操作者，存量行操作者为空；待报价空表） */
        List<AiplatformPriceEntryWireResponse> priceEntries,

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

        /** 重试归档操作者 id（#158；支付链自动归档/缺透传头为 null） */
        String archiveOperatorId,

        /** 重试归档操作者名（直读；口径同取消留痕两列） */
        String archiveOperatorName,

        /** 取消时点（未取消 NULL） */
        LocalDateTime cancelledAt,

        /** 取消原因（#157 运营取消必填留痕，运营内部口径；用户取消/未取消为 null） */
        String cancelReason,

        /** 取消操作者 id（admin 侧管理员 TSID；用户取消/缺透传头为 null） */
        String cancelOperatorId,

        /** 取消操作者名（直读；口径同价目行 operator） */
        String cancelOperatorName
) {
}
