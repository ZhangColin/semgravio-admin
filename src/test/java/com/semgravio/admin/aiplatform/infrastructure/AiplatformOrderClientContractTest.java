package com.semgravio.admin.aiplatform.infrastructure;

import java.net.http.HttpHeaders;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformOrderCancelCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformOrderQuoteCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformOrderQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformOrderResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformOrderSummaryResponse;
import com.cartisan.openapi.client.BinaryResponse;
import com.cartisan.web.response.PageResponse;

/**
 * aiplatform 订单域读路径（清单/详情/源码包，issue #64）＋写路径三操作（报价/改价、运营取消、
 * 重试归档，issue #70）＋设计资产包取件（issue #96，#95 设计能力线 T1——交付物类型字段对
 * ＋design-package 二进制端点）的 wire 契约测试。
 *
 * <p>契约事实（对照 aiplatform 源码 {@code BackofficeOrderController} /
 * {@code BackofficeOrderSummaryResponse} / {@code BackofficeOrderDetailResponse} /
 * {@code OrderResponse} / {@code PriceEntryResponse} / {@code SubmitQuoteCommand} /
 * {@code CancelOrderCommand} / {@code OrderMessage}）：</p>
 * <ul>
 *   <li>清单/详情均包 {@code ApiResponse<T>} 信封，须按信封反序列化并取 {@code .data()}；
 *       清单 data 为 {@code PageResponse{items,total,page,size}}（page 1-based）。</li>
 *   <li>owner 摘要两肢（aiplatform #243 起，issue #76）：{@code ownerExternalId}（OIDC sub——
 *       账号档案读口的寻址键）+ {@code ownerDisplayName}；下单账号可空/跨 BC 软引用缺档为 null。</li>
 *   <li>交付物类型字段对（#96 镜像 provider #297/#298，issue #95 设计能力线 T1）：
 *       {@code deliverableType} Integer code（1=设计 2=系统 3=系统＋设计，下单冻结自项目终点
 *       类型）+ {@code deliverableTypeName} 中文名——provider 三个订单出口 DTO（清单行/详情/
 *       写路径用户面回执 OrderResponse）均已配对新增、位于 statusName 邻位（amount 前），
 *       admin 三镜像同步（Integer code 直通不建 typed enum，对齐 tokenKind 先例——provider
 *       枚举非本应用领域枚举）。</li>
 *   <li>清单 {@code status} 多选为<strong>逗号分隔单值</strong>（{@code status=1,5}）——aiplatform
 *       签名协议按 query 参数名去重，重复参数只有末值入签（区别于 payment 的重复参数展开）。</li>
 *   <li>金额 {@code Long}（分）、{@code status} Integer code + {@code statusName} 中文名、
 *       时间 ISO——类型绑定靠真实 Jackson 反序列化钉死。</li>
 *   <li>源码包<strong>无信封</strong>：{@code application/gzip} 二进制 + Content-Disposition
 *       文件名，经 cartisan-openapi {@code download}（cartisan-boot#30）保全原始字节与响应头。</li>
 *   <li>设计资产包同款二进制无信封（#96 镜像 provider #298）：直取下单冻结时物化的设计资产包
 *       （交付物按单定格，此后改稿不漂移）、<strong>不按支付态门控</strong>；错误族＝订单不存在
 *       ORD_001→5001（404）、系统单交付物不含设计资产包 ORD_016→5016（404）、冻结件缺失
 *       PRJ_051→4051（404，跨域码 4×1000＋51）、读取失败 WSP_002→1002（500）原样透传。</li>
 *   <li>分页（spec #62 平台分页统一决议目标态）：北向 1-based 直传、回显 provider 回报值，
 *       全链零 ±1 换算（区别于 payment 的 page-1）。</li>
 *   <li>写路径三操作共用回执 {@code ApiResponse<OrderResponse>}（provider <strong>用户面</strong>
 *       同构 DTO——区别于后台详情 21 字段）：内嵌价目行为<strong>五字段</strong>（无操作者留痕，
 *       操作者两肢只在后台详情价目行上）。「已报价态重复提交＝改价」语义由 provider 承担。</li>
 *   <li>报价命令体 {@code {amount, note}}：amount Long 分→JSON <strong>string</strong>
 *       （cartisan-web 全局 Long→ToStringSerializer 出口口径，provider 默认 string→Long 强转
 *       可回读）；note null 照常序列化在场——BFF 不代填。取消命令体 {@code {reason}} 必填。
 *       重试归档无请求体（null body）。</li>
 *   <li>错误透传（数字业务码＝域码 5×1000＋序号）：ORD_001→5001（404）、ORD_005→5005（409）、
 *       ORD_007→5007（409）、ORD_008→5008（400）、ORD_009→5009（400）、ORD_010→5010（400）、
 *       ORD_012→5012（409）、ORD_013→5013（400）、ORD_014→5014（400）；重试归档的跨域码
 *       <strong>PRJ_013→4013</strong>（409，域码 4×1000＋13——项目已归档守卫）原样透传。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留；写路径命令体在
 * stub 里按生产 mapper 口径（Long→ToStringSerializer）序列化断言出站 JSON 形状。</p>
 *
 * @since 0.1.0
 */
class AiplatformOrderClientContractTest {

    /**
     * aiplatform GET /api/backoffice/orders 的真实成功响应形状（信封 + PageResponse：两行，一已支付一待报价）。
     * Long 字段（amount/total）为 <strong>JSON string</strong>——cartisan-web 全局
     * {@code Long→ToStringSerializer} 的出口口径（spec #62「金额 Long（分、JSON string）」）。
     */
    private static final String ORDER_PAGE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "items": [
                  {
                    "id": "3829492001234567",
                    "projectId": "3829492007654321",
                    "projectName": "英语学习助手",
                    "ownerExternalId": "auth0|65f2c8a1",
                    "ownerDisplayName": "文野",
                    "status": 3,
                    "statusName": "已支付",
                    "deliverableType": 3,
                    "deliverableTypeName": "系统＋设计",
                    "amount": "1999000",
                    "currency": "CNY",
                    "createdAt": "2026-09-10T14:20:00",
                    "quotedAt": "2026-09-11T09:00:00"
                  },
                  {
                    "id": "3829492005555444",
                    "projectId": "3829492007654321",
                    "projectName": "英语学习助手",
                    "ownerExternalId": null,
                    "ownerDisplayName": null,
                    "status": 1,
                    "statusName": "待报价",
                    "deliverableType": 1,
                    "deliverableTypeName": "设计",
                    "amount": null,
                    "currency": null,
                    "createdAt": "2026-09-14T18:30:00",
                    "quotedAt": null
                  }
                ],
                "total": "42",
                "page": 3,
                "size": 20
              },
              "requestId": "req-1a2b3c",
              "errors": null
            }
            """;

    /** aiplatform GET /api/backoffice/orders/{id} 的真实成功响应形状（已报价单：两次报价留痕，新→旧）。 */
    private static final String ORDER_DETAIL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829492001234567",
                "projectId": "3829492007654321",
                "projectName": "英语学习助手",
                "ownerExternalId": "auth0|65f2c8a1",
                "ownerDisplayName": "文野",
                "status": 2,
                "statusName": "已报价",
                "deliverableType": 1,
                "deliverableTypeName": "设计",
                "amount": "2199000",
                "currency": "CNY",
                "note": "含加急费用",
                "priceEntries": [
                  {
                    "id": "3829492011111111",
                    "amount": "2199000",
                    "currency": "CNY",
                    "note": "含加急费用",
                    "operatorId": "1234567890123456",
                    "operatorName": "报价运营",
                    "createdAt": "2026-09-12T10:00:00"
                  },
                  {
                    "id": "3829492009999999",
                    "amount": "1999000",
                    "currency": "CNY",
                    "note": "首次报价",
                    "operatorId": null,
                    "operatorName": null,
                    "createdAt": "2026-09-11T09:00:00"
                  }
                ],
                "prdSnapshot": "# PRD\\n\\n做一个英语学习助手……",
                "createdAt": "2026-09-10T14:20:00",
                "quotedAt": "2026-09-11T09:00:00",
                "paidAt": null,
                "archivedAt": null,
                "archiveOperatorId": null,
                "archiveOperatorName": null,
                "cancelledAt": null,
                "cancelReason": null,
                "cancelOperatorId": null,
                "cancelOperatorName": null
              },
              "requestId": "req-2b3c4d",
              "errors": null
            }
            """;

    /** aiplatform 过滤参数绑定失败的真实错误形状：HTTP 400 + 信封 code=5010（ORD_010 数字业务码）。 */
    private static final String ORD_010_ERROR_ENVELOPE = """
            {
              "code": 5010,
              "message": "无效的订单过滤参数",
              "data": null,
              "requestId": "req-3c4d5e",
              "errors": null
            }
            """;

    /** aiplatform 订单不存在的真实错误形状：HTTP 404 + 信封 code=5001（ORD_001 数字业务码）。 */
    private static final String ORD_001_ERROR_ENVELOPE = """
            {
              "code": 5001,
              "message": "订单不存在",
              "data": null,
              "requestId": "req-4d5e6f",
              "errors": null
            }
            """;

    /** gzip 魔数起始、含非法 UTF-8 字节（0xff/0xfe）与 NUL 的字节流——ofString 解码会损坏的回归锚点。 */
    private static final byte[] TAR_GZ_BYTES = {
            (byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, (byte) 0xfe, 0x41, 0x00, 0x0d, 0x0a
    };

    private static final String SOURCE_DISPOSITION = "attachment; filename=\"3829492001234567-source.tar.gz\"";

    /** 设计资产包的 Content-Disposition 原值（provider {@code BinaryResponses.attachment}：{orderId}-design.tar.gz）。 */
    private static final String DESIGN_DISPOSITION = "attachment; filename=\"3829492001234567-design.tar.gz\"";

    /** 设计资产包守卫①：系统单交付物是源码包——HTTP 404 + 信封 code=5016（ORD_016 数字业务码）。 */
    private static final String ORD_016_ERROR_ENVELOPE = """
            {
              "code": 5016,
              "message": "该订单的交付物不含设计资产包（系统单交付物是源码包）",
              "data": null,
              "requestId": "req-a1b2c3",
              "errors": null
            }
            """;

    /** 设计资产包守卫②：冻结件缺失不以空产物顶替——HTTP 404 + 信封 code=4051（PRJ_051，域码 4×1000＋51）。 */
    private static final String PRJ_051_ERROR_ENVELOPE = """
            {
              "code": 4051,
              "message": "设计资产包不存在（下单冻结件缺失）",
              "data": null,
              "requestId": "req-b2c3d4",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/orders/{id}/quote 的真实成功响应形状（<strong>改价</strong>
     * 场景——已报价态重复提交）：用户面同构 OrderResponse，现值取最新价目行、价目历史新→旧
     * 两行（<strong>五字段</strong>，无操作者留痕——区别于后台详情价目行）、quotedAt 仍是首次
     * 报价时点（改价不刷新）。amount Long 为 JSON string。
     */
    private static final String QUOTE_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829492001234567",
                "projectId": "3829492007654321",
                "status": 2,
                "statusName": "已报价",
                "deliverableType": 3,
                "deliverableTypeName": "系统＋设计",
                "amount": "2199000",
                "currency": "CNY",
                "note": "含加急费用",
                "quotedAt": "2026-09-11T09:00:00",
                "priceEntries": [
                  {
                    "id": "3829492011111111",
                    "amount": "2199000",
                    "currency": "CNY",
                    "note": "含加急费用",
                    "createdAt": "2026-09-12T10:00:00"
                  },
                  {
                    "id": "3829492009999999",
                    "amount": "1999000",
                    "currency": "CNY",
                    "note": "首次报价",
                    "createdAt": "2026-09-11T09:00:00"
                  }
                ],
                "createdAt": "2026-09-10T14:20:00",
                "cancelledAt": null,
                "paidAt": null,
                "archivedAt": null
              },
              "requestId": "req-5e6f7a",
              "errors": null
            }
            """;

    /** aiplatform POST /api/backoffice/orders/{id}/cancel 的真实成功响应形状（已取消终态，cancelledAt 落定）。 */
    private static final String CANCEL_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829492001234567",
                "projectId": "3829492007654321",
                "status": 5,
                "statusName": "已取消",
                "deliverableType": 2,
                "deliverableTypeName": "系统",
                "amount": "2199000",
                "currency": "CNY",
                "note": "含加急费用",
                "quotedAt": "2026-09-11T09:00:00",
                "priceEntries": [
                  {
                    "id": "3829492011111111",
                    "amount": "2199000",
                    "currency": "CNY",
                    "note": "含加急费用",
                    "createdAt": "2026-09-12T10:00:00"
                  }
                ],
                "createdAt": "2026-09-10T14:20:00",
                "cancelledAt": "2026-09-16T15:00:00",
                "paidAt": null,
                "archivedAt": null
              },
              "requestId": "req-6f7a8b",
              "errors": null
            }
            """;

    /** aiplatform POST /api/backoffice/orders/{id}/retry-archive 的真实成功响应形状（已归档终态，paidAt/archivedAt 双时点）。 */
    private static final String ARCHIVE_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829492001234567",
                "projectId": "3829492007654321",
                "status": 4,
                "statusName": "已归档",
                "deliverableType": 3,
                "deliverableTypeName": "系统＋设计",
                "amount": "2199000",
                "currency": "CNY",
                "note": "含加急费用",
                "quotedAt": "2026-09-11T09:00:00",
                "priceEntries": [
                  {
                    "id": "3829492011111111",
                    "amount": "2199000",
                    "currency": "CNY",
                    "note": "含加急费用",
                    "createdAt": "2026-09-12T10:00:00"
                  }
                ],
                "createdAt": "2026-09-10T14:20:00",
                "cancelledAt": null,
                "paidAt": "2026-09-15T20:00:00",
                "archivedAt": "2026-09-16T09:30:00"
              },
              "requestId": "req-7a8b9c",
              "errors": null
            }
            """;

    /** 报价守卫：已支付/已终结拒改价——HTTP 409 + 信封 code=5007（ORD_007 数字业务码）。 */
    private static final String ORD_007_ERROR_ENVELOPE = """
            {
              "code": 5007,
              "message": "订单已支付或已终结，无法报价或改价",
              "data": null,
              "requestId": "req-8b9c0d",
              "errors": null
            }
            """;

    /** 取消守卫：原因缺失——HTTP 400 + 信封 code=5013（ORD_013 数字业务码）。 */
    private static final String ORD_013_ERROR_ENVELOPE = """
            {
              "code": 5013,
              "message": "取消原因必填（运营取消须填写原因）",
              "data": null,
              "requestId": "req-9c0d1e",
              "errors": null
            }
            """;

    /** 重试归档跨域码：项目已归档——HTTP 409 + 信封 code=4013（PRJ_013，域码 4×1000＋13）。 */
    private static final String PRJ_013_ERROR_ENVELOPE = """
            {
              "code": 4013,
              "message": "项目已归档（归档是单向终点）",
              "data": null,
              "requestId": "req-0d1e2f",
              "errors": null
            }
            """;

    // ========== 清单（四维检索 + 分页 1-based 透传）==========

    @Test
    void given_fullFourDimensionFilter_when_listOrders_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithStubTransport(
                ORDER_PAGE_ENVELOPE, null, wireUrl);

        appService.list(new AiplatformOrderQuery(
                List.of(1, 5),
                LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                LocalDateTime.of(2026, 9, 15, 23, 59, 59),
                "auth0|65f2c8a1", "3829492001234567"), 3, 20);

        // 出站 wire 形状逐字镜像 provider 契约：page 1-based 直传（无 ±1）；status 逗号分隔单值
        // （签名按参数名去重——禁用 payment 式重复参数）；时间为全秒 ISO；特殊字符 URL 编码
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/orders"
                + "?page=3&size=20"
                + "&status=1%2C5"
                + "&createdFrom=2026-09-01T00%3A00%3A00"
                + "&createdTo=2026-09-15T23%3A59%3A59"
                + "&externalId=auth0%7C65f2c8a1"
                + "&orderId=3829492001234567");
    }

    @Test
    void given_pageEnvelope_when_listOrders_then_envelopeUnwrappedAndTypesBound() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithStubTransport(
                ORDER_PAGE_ENVELOPE, null, wireUrl);

        PageResponse<AiplatformOrderSummaryResponse> page =
                appService.list(new AiplatformOrderQuery(null, null, null, null, null), 1, 20);

        // 信封正确拆开（items 非 null）——证明 ORDER_PAGE_TYPEREF 按 ApiResponse<PageResponse<…>> 反序列化并取 .data()
        assertThat(page.items()).hasSize(2);

        // 分页回显：provider 回报值原样透传（1-based，含 clamp 后值）——BFF 不夹取不换算
        assertThat(page.total()).isEqualTo(42L);
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);

        var paid = page.items().get(0);
        // 类型口径：id String（TSID 十进制串）、status Integer code + statusName 中文名、金额 Long（分）
        assertThat(paid.id()).isEqualTo("3829492001234567");
        assertThat(paid.projectId()).isEqualTo("3829492007654321");
        assertThat(paid.projectName()).isEqualTo("英语学习助手");
        // owner 摘要含 externalId（aiplatform #243 起，issue #76）——账号档案读口的寻址键
        assertThat(paid.ownerExternalId()).isEqualTo("auth0|65f2c8a1");
        assertThat(paid.ownerDisplayName()).isEqualTo("文野");
        assertThat(paid.status()).isEqualTo(3);
        assertThat(paid.statusName()).isEqualTo("已支付");
        // 交付物类型字段对（#96）：Integer code 直通（不建 typed enum）+ 中文名随行
        assertThat(paid.deliverableType()).isEqualTo(3);
        assertThat(paid.deliverableTypeName()).isEqualTo("系统＋设计");
        assertThat(paid.amount()).isEqualTo(1999000L);
        assertThat(paid.currency()).isEqualTo("CNY");
        assertThat(paid.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 20, 0));
        assertThat(paid.quotedAt()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0, 0));

        // 待报价行的 NULL 口径（全局 Jackson 含 null 序列化，反序列化回 null）
        var pending = page.items().get(1);
        assertThat(pending.status()).isEqualTo(1);
        assertThat(pending.statusName()).isEqualTo("待报价");
        // 类型对在待报价态已有值（下单冻结，早于报价——区别于 amount 的待报价 NULL）
        assertThat(pending.deliverableType()).isEqualTo(1);
        assertThat(pending.deliverableTypeName()).isEqualTo("设计");
        assertThat(pending.amount()).isNull();
        assertThat(pending.currency()).isNull();
        assertThat(pending.quotedAt()).isNull();
        assertThat(pending.ownerExternalId()).isNull();
        assertThat(pending.ownerDisplayName()).isNull();
    }

    @Test
    void given_ord010ErrorEnvelope_when_listOrders_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                400, ORD_010_ERROR_ENVELOPE, wireUrl);

        // 忠实透传（spec #62）：HTTP 400 + 数字业务码 5010（ORD_010）+ provider message 原样
        assertThatThrownBy(() -> appService.list(new AiplatformOrderQuery(null, null, null, null, null), 1, 20))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5010);
                    assertThat(upstream.getMessage()).isEqualTo("无效的订单过滤参数");
                });
    }

    // ========== 详情（价目历史 append-only 嵌套）==========

    @Test
    void given_detailEnvelope_when_getDetail_then_priceEntriesBoundWithOperatorLimbs() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithStubTransport(
                ORDER_DETAIL_ENVELOPE, null, wireUrl);

        var detail = appService.getDetail("3829492001234567");

        // 出站路径逐字镜像 provider backoffice 路由
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/orders/3829492001234567");

        // 顶层字段：金额 Long（分）、现值取最新价目行、PRD 快照正文
        assertThat(detail.id()).isEqualTo("3829492001234567");
        // owner 摘要含 externalId（aiplatform #243 起，issue #76）——账号档案读口的寻址键
        assertThat(detail.ownerExternalId()).isEqualTo("auth0|65f2c8a1");
        assertThat(detail.status()).isEqualTo(2);
        assertThat(detail.statusName()).isEqualTo("已报价");
        // 交付物类型字段对（#96）：设计单——PRD 快照即含设计物清单章、取件走 design-package
        assertThat(detail.deliverableType()).isEqualTo(1);
        assertThat(detail.deliverableTypeName()).isEqualTo("设计");
        assertThat(detail.amount()).isEqualTo(2199000L);
        assertThat(detail.note()).isEqualTo("含加急费用");
        assertThat(detail.prdSnapshot()).startsWith("# PRD");

        // 价目历史：append-only 全量，新 → 旧；新行带操作者两肢、存量行操作者为空
        assertThat(detail.priceEntries()).hasSize(2);
        var newest = detail.priceEntries().get(0);
        assertThat(newest.id()).isEqualTo("3829492011111111");
        assertThat(newest.amount()).isEqualTo(2199000L);
        assertThat(newest.currency()).isEqualTo("CNY");
        assertThat(newest.note()).isEqualTo("含加急费用");
        assertThat(newest.operatorId()).isEqualTo("1234567890123456");
        assertThat(newest.operatorName()).isEqualTo("报价运营");
        assertThat(newest.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 12, 10, 0, 0));
        var oldest = detail.priceEntries().get(1);
        assertThat(oldest.operatorId()).isNull();
        assertThat(oldest.operatorName()).isNull();

        // 未支付/未归档/未取消的 NULL 时点与留痕两肢
        assertThat(detail.quotedAt()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0, 0));
        assertThat(detail.paidAt()).isNull();
        assertThat(detail.archivedAt()).isNull();
        assertThat(detail.archiveOperatorId()).isNull();
        assertThat(detail.cancelledAt()).isNull();
        assertThat(detail.cancelReason()).isNull();
        assertThat(detail.cancelOperatorId()).isNull();
        assertThat(detail.cancelOperatorName()).isNull();
    }

    @Test
    void given_ord001ErrorEnvelope_when_getDetail_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                404, ORD_001_ERROR_ENVELOPE, wireUrl);

        assertThatThrownBy(() -> appService.getDetail("3829499999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5001);
                    assertThat(upstream.getMessage()).isEqualTo("订单不存在");
                });
    }

    // ========== 源码包（二进制无信封，字节与响应头保全）==========

    @Test
    void given_tarGzBinaryResponse_when_downloadSourcePackage_then_bytesAndHeadersPreserved() {
        String[] wireUrl = new String[1];
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/gzip"),
                "Content-Disposition", List.of(SOURCE_DISPOSITION)), (a, b) -> true);
        var appService = AiplatformWireTestSupport.orderAppServiceWithStubTransport(
                null, new BinaryResponse(200, providerHeaders, TAR_GZ_BYTES), wireUrl);

        var pkg = appService.getSourcePackage("3829492001234567");

        // 出站路径逐字镜像 provider backoffice 路由
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/3829492001234567/source-package");

        // gzip 字节逐位相等（含 0xff/0xfe/NUL——ofString 解码损坏的回归锚点），内存中转无信封
        assertThat(pkg.content()).containsExactly(TAR_GZ_BYTES);
        // provider 响应头 raw 值透传（不打解析、不重构文件名）
        assertThat(pkg.contentType()).isEqualTo("application/gzip");
        assertThat(pkg.contentDisposition()).isEqualTo(SOURCE_DISPOSITION);
    }

    @Test
    void given_ord001OnDownload_when_downloadSourcePackage_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                404, ORD_001_ERROR_ENVELOPE, wireUrl);

        // download 的 ≥400 由框架抛 OpenApiClientException（错误信封在 body 里）——AiplatformClient
        // 同款翻译透传，与 JSON 端点错误路径一致
        assertThatThrownBy(() -> appService.getSourcePackage("3829499999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5001);
                    assertThat(upstream.getMessage()).isEqualTo("订单不存在");
                });
    }

    // ========== 设计资产包（#96：二进制无信封，下单冻结件直取，字节与响应头保全）==========

    @Test
    void given_tarGzBinaryResponse_when_downloadDesignPackage_then_bytesAndHeadersPreserved() {
        String[] wireUrl = new String[1];
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/gzip"),
                "Content-Disposition", List.of(DESIGN_DISPOSITION)), (a, b) -> true);
        var appService = AiplatformWireTestSupport.orderAppServiceWithStubTransport(
                null, new BinaryResponse(200, providerHeaders, TAR_GZ_BYTES), wireUrl);

        var pkg = appService.getDesignPackage("3829492001234567");

        // 出站路径逐字镜像 provider backoffice 路由（对偶 source-package）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/3829492001234567/design-package");

        // gzip 字节逐位相等（含 0xff/0xfe/NUL——ofString 解码损坏的回归锚点），内存中转无信封
        assertThat(pkg.content()).containsExactly(TAR_GZ_BYTES);
        // provider 响应头 raw 值透传（不打解析、不重构文件名——design 后缀区别于 source）
        assertThat(pkg.contentType()).isEqualTo("application/gzip");
        assertThat(pkg.contentDisposition()).isEqualTo(DESIGN_DISPOSITION);
    }

    @Test
    void given_ord016ErrorEnvelope_when_downloadDesignPackage_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                404, ORD_016_ERROR_ENVELOPE, wireUrl);

        // 守卫序第二环（订单存在之后）：系统单交付物是源码包——ORD_016 如实（5016），
        // 不以空产物顶替不换码
        assertThatThrownBy(() -> appService.getDesignPackage("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5016);
                    assertThat(upstream.getMessage()).isEqualTo("该订单的交付物不含设计资产包（系统单交付物是源码包）");
                });
    }

    @Test
    void given_prj051ErrorEnvelope_when_downloadDesignPackage_then_crossDomainCodePassesThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                404, PRJ_051_ERROR_ENVELOPE, wireUrl);

        // 守卫序第三环：冻结件读取缺失——PRJ_051 来自项目域（域码 4×1000＋51），经订单域端点
        // 透传（与重试归档的 PRJ_013 同款跨域码先例），BFF 不重编码不翻译
        assertThatThrownBy(() -> appService.getDesignPackage("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4051);
                    assertThat(upstream.getMessage()).isEqualTo("设计资产包不存在（下单冻结件缺失）");
                });
    }

    // ========== 报价/改价（命令体序列化 + 用户面同构回执）==========

    @Test
    void given_quoteCommand_when_quote_then_wireUrlAndCommandBodyMirrorProvider() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.orderWriteAppServiceWithStubTransport(
                QUOTE_RECEIPT_ENVELOPE, wireUrl, wireBody);

        appService.quote("3829492001234567",
                new AiplatformOrderQuoteCommand(2199000L, "含加急费用"));

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/3829492001234567/quote");
        // 命令体出站形状（生产 mapper 口径）：amount Long 分 → JSON string（全局 Long→ToStringSerializer
        // 出口口径，provider 默认 string→Long 强转可回读）——逐字镜像 provider SubmitQuoteCommand
        assertThat(wireBody[0]).isEqualTo("{\"amount\":\"2199000\",\"note\":\"含加急费用\"}");
    }

    @Test
    void given_quoteCommandWithoutNote_when_quote_then_nullNoteSerializedNotFilled() {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.orderWriteAppServiceWithStubTransport(
                QUOTE_RECEIPT_ENVELOPE, new String[1], wireBody);

        appService.quote("3829492001234567", new AiplatformOrderQuoteCommand(1999000L, null));

        // note 可空：null 照常序列化在场（全局 Jackson 含 null）——BFF 不代填空串，provider 自行落 NULL
        assertThat(wireBody[0]).isEqualTo("{\"amount\":\"1999000\",\"note\":null}");
    }

    @Test
    void given_quoteReceiptEnvelope_when_quote_then_userFacingReceiptBound() {
        var appService = AiplatformWireTestSupport.orderWriteAppServiceWithStubTransport(
                QUOTE_RECEIPT_ENVELOPE, new String[1], new String[1]);

        AiplatformOrderResponse receipt = appService.quote("3829492001234567",
                new AiplatformOrderQuoteCommand(2199000L, "含加急费用"));

        // 信封正确拆开——ORDER_RECEIPT_TYPEREF 按 ApiResponse<OrderResponse> 反序列化取 .data()；
        // 顶层字段：金额 Long（分）、status Integer code + statusName、现值取最新价目行
        assertThat(receipt.id()).isEqualTo("3829492001234567");
        assertThat(receipt.projectId()).isEqualTo("3829492007654321");
        assertThat(receipt.status()).isEqualTo(2);
        assertThat(receipt.statusName()).isEqualTo("已报价");
        // 交付物类型字段对（#96）：写路径用户面回执 OrderResponse 同步携带（provider 出口已配对）
        assertThat(receipt.deliverableType()).isEqualTo(3);
        assertThat(receipt.deliverableTypeName()).isEqualTo("系统＋设计");
        assertThat(receipt.amount()).isEqualTo(2199000L);
        assertThat(receipt.currency()).isEqualTo("CNY");
        assertThat(receipt.note()).isEqualTo("含加急费用");
        // quotedAt 仍是首次报价时点（改价不刷新）
        assertThat(receipt.quotedAt()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0, 0));

        // 价目历史：append-only 新→旧两行，五字段（无操作者留痕——操作者两肢只在后台详情价目行上）
        assertThat(receipt.priceEntries()).hasSize(2);
        var newest = receipt.priceEntries().get(0);
        assertThat(newest.id()).isEqualTo("3829492011111111");
        assertThat(newest.amount()).isEqualTo(2199000L);
        assertThat(newest.currency()).isEqualTo("CNY");
        assertThat(newest.note()).isEqualTo("含加急费用");
        assertThat(newest.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 12, 10, 0, 0));
        var oldest = receipt.priceEntries().get(1);
        assertThat(oldest.amount()).isEqualTo(1999000L);
        assertThat(oldest.note()).isEqualTo("首次报价");

        // 未取消/未支付/未归档的 NULL 时点如实回 null（用户面回执无操作者留痕两肢——字段不存在）
        assertThat(receipt.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 20, 0));
        assertThat(receipt.cancelledAt()).isNull();
        assertThat(receipt.paidAt()).isNull();
        assertThat(receipt.archivedAt()).isNull();
    }

    // ========== 运营取消（命令体序列化 + 已取消终态回执）==========

    @Test
    void given_cancelCommand_when_cancel_then_wireMirrorsProviderAndCancelledReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.orderWriteAppServiceWithStubTransport(
                CANCEL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformOrderResponse receipt = appService.cancel("3829492001234567",
                new AiplatformOrderCancelCommand("重复下单，用户要求取消"));

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/3829492001234567/cancel");
        // 命令体逐字镜像 provider CancelOrderCommand：reason 必填（缺失/超长归 provider 聚合守卫）
        assertThat(wireBody[0]).isEqualTo("{\"reason\":\"重复下单，用户要求取消\"}");

        // 已取消终态回执：status 5 + cancelledAt 落定；报价事实（金额/价目行）保留在回执上
        assertThat(receipt.status()).isEqualTo(5);
        assertThat(receipt.statusName()).isEqualTo("已取消");
        // 类型三值全覆盖之「系统」单（取消回执 fixture）——Integer code 直通
        assertThat(receipt.deliverableType()).isEqualTo(2);
        assertThat(receipt.deliverableTypeName()).isEqualTo("系统");
        assertThat(receipt.amount()).isEqualTo(2199000L);
        assertThat(receipt.priceEntries()).hasSize(1);
        assertThat(receipt.cancelledAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 15, 0, 0));
        assertThat(receipt.paidAt()).isNull();
        assertThat(receipt.archivedAt()).isNull();
    }

    // ========== 重试归档（无请求体 + 已归档终态回执）==========

    @Test
    void given_paidStuckOrder_when_retryArchive_then_noBodyAndArchivedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.orderWriteAppServiceWithStubTransport(
                ARCHIVE_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformOrderResponse receipt = appService.retryArchive("3829492001234567");

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/3829492001234567/retry-archive");
        // 无请求体（provider 端只读路径参数——同沙箱四动作/单价表停用先例）
        assertThat(wireBody[0]).isNull();

        // 已归档终态回执：status 4 + paidAt/archivedAt 双时点（支付成功在前、补归档在后）
        assertThat(receipt.status()).isEqualTo(4);
        assertThat(receipt.statusName()).isEqualTo("已归档");
        assertThat(receipt.paidAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 20, 0, 0));
        assertThat(receipt.archivedAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 9, 30, 0));
        assertThat(receipt.cancelledAt()).isNull();
    }

    // ========== 写路径错误透传（ORD_/PRJ_ 数字业务码，不映射）==========

    @Test
    void given_ord007ErrorEnvelope_when_quote_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                409, ORD_007_ERROR_ENVELOPE, new String[1]);

        // 忠实透传（spec #62）：HTTP 409 + 数字业务码 5007（ORD_007＝域码 5×1000＋7）+ provider message 原样
        assertThatThrownBy(() -> appService.quote("3829492001234567",
                new AiplatformOrderQuoteCommand(2199000L, "含加急费用")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5007);
                    assertThat(upstream.getMessage()).isEqualTo("订单已支付或已终结，无法报价或改价");
                });
    }

    @Test
    void given_ord013ErrorEnvelope_when_cancel_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                400, ORD_013_ERROR_ENVELOPE, new String[1]);

        assertThatThrownBy(() -> appService.cancel("3829492001234567", new AiplatformOrderCancelCommand(null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5013);
                    assertThat(upstream.getMessage()).isEqualTo("取消原因必填（运营取消须填写原因）");
                });
    }

    @Test
    void given_prj013ErrorEnvelope_when_retryArchive_then_crossDomainCodePassesThrough() {
        var appService = AiplatformWireTestSupport.orderAppServiceWithErrorTransport(
                409, PRJ_013_ERROR_ENVELOPE, new String[1]);

        // 重试归档的跨域码（issue #70）：PRJ_013 来自项目域（域码 4×1000＋13），经订单域端点透传
        // ——BFF 不重编码不翻译，前端比对 aiplatform 业务码的分支活
        assertThatThrownBy(() -> appService.retryArchive("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4013);
                    assertThat(upstream.getMessage()).isEqualTo("项目已归档（归档是单向终点）");
                });
    }
}
