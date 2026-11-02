package com.semgravio.admin.aiplatform.infrastructure;

import java.net.http.HttpHeaders;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentNodesCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentVerifyCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentVoucherContentResponse;
import com.cartisan.openapi.client.BinaryResponse;

/**
 * aiplatform 付款核验域（issue #106，spec #102 A 批 A3——provider aiplatform #337 凭证
 * 核验承接；<strong>与既有支付服务 BFF 上下文无关，勿混读</strong>）五端点的 wire 契约测试：
 * 计划详情（核验承接全量面：节点五态/凭证清单/核验留痕时间线/历史计划行）/ 节点录入
 * （整组替换含开工门）/ 到账核验（全额判定归 provider）/ 打回（依据必填留痕）/ 凭证内容
 * （二进制直出无信封）。
 *
 * <p>契约事实（对照 aiplatform 源码 {@code BackofficePaymentController} /
 * {@code BackofficePaymentPlanDetailResponse} / {@code PaymentPlanResponse} /
 * {@code PaymentNodeResponse} / {@code PaymentVoucherResponse} /
 * {@code PaymentVerificationResponse} / {@code PaymentNodeSpec} /
 * {@code EnterPaymentNodesCommand} / {@code VerifyPaymentCommand} /
 * {@code RejectPaymentCommand} / {@code PaymentNodeStatus} /
 * {@code PaymentVerificationAction} / {@code OrderMessage}；fixture 实值对照 provider
 * {@code PaymentPlanAppServiceTest} 与 {@code BackofficePaymentControllerTest} 兜底）：</p>
 * <ul>
 *   <li>JSON 四端点均包 {@code ApiResponse<T>} 信封，须按信封反序列化并取 {@code .data()}；
 *       详情 data 三段 {@code plan/trails/history}；三写口回执 data＝PaymentPlanResponse
 *       （用户面付款面板同构，与详情 plan 段共形）。</li>
 *   <li>节点五态 {@code status} Integer code + {@code statusName} 中文名（BaseEnum 约定，
 *       provider 出 JSON number）：<strong>1=待支付 2=支付处理中 3=支付失败或关闭
 *       4=线下转账待核验 5=已到账</strong>——v1 只走凭证核验路径（1→4→5 或打回回 1），
 *       2/3 枚举立着渠道真接后启用；admin Integer code 直通不建 typed enum（合同六态
 *       先例）。渠道类型 code：1=线下转账 2=扫码（占位）。留痕动作 code：1=到账核验
 *       2=打回。</li>
 *   <li>金额类（amount/totalAmount/totalSettled/settledAmount/remainingAmount/
 *       actualAmount/byteSize）Long（分）→ JSON <strong>string</strong>（cartisan-web
 *       全局 Long→ToStringSerializer 出口口径，同订单/合同域）；{@code seq}/
 *       {@code percentage} int、五态/渠道/动作 Integer → JSON number；时间 ISO。</li>
 *   <li>三写口命令体逐字镜像：节点录入 {@code {nodes:[{name, amount, kickoffGate}]}}、
 *       核验 {@code {actualAmount, arrivedAt, voucherId, channelRefNo, basis}}（provider
 *       VerifyPaymentCommand 同序）、打回 {@code {voucherId, basis}}——Long 金额出站
 *       JSON string（生产 mapper 口径，provider 默认可回读，同订单 quote 的 amount）。</li>
 *   <li>凭证内容 {@code GET payment-vouchers/{voucherId}/content}：<strong>无信封</strong>
 *       二进制直出（真实媒体类型＋Content-Disposition attachment 文件名），经
 *       {@code download} 保全字节与响应头（先例＝源码包/设计资产包）。</li>
 *   <li>错误透传（数字业务码＝域码 5×1000＋序号）：计划不存在 ORD_036→5036（404）、
 *       节点不存在 ORD_037→5037（404）、状态守卫 ORD_038→5038（409）、录入无效
 *       ORD_039→5039（400）、核验录入无效 ORD_041→5041（400）、打回依据必填
 *       ORD_042→5042（400）、操作者必携 ORD_043→5043（400）、重录窗口 ORD_044→5044
 *       （409）、凭证不存在或不属该节点 ORD_045→5045（404）；订单不存在 ORD_001→5001。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformPaymentClientContractTest {

    /** 订单标识（TSID 十进制串——透传不解析不预校验，畸形/未寻址由 provider 判 404 ORD_001）。 */
    private static final String ORDER_ID = "3829492001234567";

    /** 凭证标识（TSID 十进制串——凭证内容端点寻址位）。 */
    private static final String VOUCHER_ID = "3829497000000001";

    /**
     * aiplatform GET /api/backoffice/orders/{orderId}/payment-plan 的真实成功响应形状
     * （详情三段 plan/trails/history）：<strong>五节点恰钉五态</strong>——1 待支付/2 支付
     * 处理中/3 支付失败或关闭/4 线下转账待核验/5 已到账（录入序即展示序，占比现算
     * 50/20/10/15/5）。节点 4 带两行凭证（上传追加保留不覆盖）；节点 5 已到账带渠道流水号
     * ＋receivedAt；留痕两行＝到账核验（action=1，from 4 to 5）与打回（action=2，from 4
     * to 1，实到两肢 null）各一行 append-only；历史一行＝撤回重建后旧计划留痕（provider
     * {@code PaymentPlanAppServiceTest}／{@code BackofficePaymentControllerTest} 实值口径）。
     * 金额类 Long 为 <strong>JSON string</strong>；五态/渠道/动作 Integer 为 JSON number。
     */
    private static final String PLAN_DETAIL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "plan": {
                  "id": "3829495000000001",
                  "orderId": "3829492001234567",
                  "contractId": "3829493005555555",
                  "contractNo": "SG-l5eq8m2k7c",
                  "totalAmount": "3900000",
                  "totalSettled": "195000",
                  "createdAt": "2026-10-20T09:00:00",
                  "nodes": [
                    {
                      "id": "3829496001000001",
                      "seq": 1,
                      "name": "首期开工款",
                      "amount": "1950000",
                      "percentage": 50,
                      "status": 1,
                      "statusName": "待支付",
                      "remarkCode": "SG-l5eq8m2k7c-K1",
                      "kickoffGate": true,
                      "channelType": 1,
                      "channelTypeName": "线下转账",
                      "channelRefNo": null,
                      "settledAmount": "0",
                      "remainingAmount": "1950000",
                      "receivedAt": null,
                      "vouchers": []
                    },
                    {
                      "id": "3829496001000002",
                      "seq": 2,
                      "name": "中期验收款",
                      "amount": "780000",
                      "percentage": 20,
                      "status": 2,
                      "statusName": "支付处理中",
                      "remarkCode": "SG-l5eq8m2k7c-K2",
                      "kickoffGate": false,
                      "channelType": 1,
                      "channelTypeName": "线下转账",
                      "channelRefNo": null,
                      "settledAmount": "0",
                      "remainingAmount": "780000",
                      "receivedAt": null,
                      "vouchers": []
                    },
                    {
                      "id": "3829496001000003",
                      "seq": 3,
                      "name": "增项补差款",
                      "amount": "390000",
                      "percentage": 10,
                      "status": 3,
                      "statusName": "支付失败或关闭",
                      "remarkCode": "SG-l5eq8m2k7c-K3",
                      "kickoffGate": false,
                      "channelType": 1,
                      "channelTypeName": "线下转账",
                      "channelRefNo": null,
                      "settledAmount": "0",
                      "remainingAmount": "390000",
                      "receivedAt": null,
                      "vouchers": []
                    },
                    {
                      "id": "3829496001000004",
                      "seq": 4,
                      "name": "尾款结算一",
                      "amount": "585000",
                      "percentage": 15,
                      "status": 4,
                      "statusName": "线下转账待核验",
                      "remarkCode": "SG-l5eq8m2k7c-K4",
                      "kickoffGate": false,
                      "channelType": 1,
                      "channelTypeName": "线下转账",
                      "channelRefNo": null,
                      "settledAmount": "0",
                      "remainingAmount": "585000",
                      "receivedAt": null,
                      "vouchers": [
                        {
                          "id": "3829497000000001",
                          "nodeId": "3829496001000004",
                          "fileName": "receipt-1.pdf",
                          "contentType": "application/pdf",
                          "byteSize": "102400",
                          "createdAt": "2026-10-16T15:00:00"
                        },
                        {
                          "id": "3829497000000002",
                          "nodeId": "3829496001000004",
                          "fileName": "receipt-2.png",
                          "contentType": "image/png",
                          "byteSize": "204800",
                          "createdAt": "2026-10-16T15:10:00"
                        }
                      ]
                    },
                    {
                      "id": "3829496001000005",
                      "seq": 5,
                      "name": "尾款结算二",
                      "amount": "195000",
                      "percentage": 5,
                      "status": 5,
                      "statusName": "已到账",
                      "remarkCode": "SG-l5eq8m2k7c-K5",
                      "kickoffGate": false,
                      "channelType": 1,
                      "channelTypeName": "线下转账",
                      "channelRefNo": "BANK-20261017-0001",
                      "settledAmount": "195000",
                      "remainingAmount": "0",
                      "receivedAt": "2026-10-17T10:00:00",
                      "vouchers": [
                        {
                          "id": "3829497000000003",
                          "nodeId": "3829496001000005",
                          "fileName": "settlement.pdf",
                          "contentType": "application/pdf",
                          "byteSize": "51200",
                          "createdAt": "2026-10-17T09:50:00"
                        }
                      ]
                    }
                  ]
                },
                "trails": [
                  {
                    "id": "3829498000000002",
                    "nodeId": "3829496001000005",
                    "action": 1,
                    "actionName": "到账核验",
                    "fromStatus": 4,
                    "toStatus": 5,
                    "actualAmount": "195000",
                    "arrivedAt": "2026-10-17T10:00:00",
                    "voucherId": "3829497000000003",
                    "basis": "银企对账一致",
                    "operatorId": "700100",
                    "operatorName": "运营·小刘",
                    "createdAt": "2026-10-17T10:05:00"
                  },
                  {
                    "id": "3829498000000001",
                    "nodeId": "3829496001000004",
                    "action": 2,
                    "actionName": "打回",
                    "fromStatus": 4,
                    "toStatus": 1,
                    "actualAmount": null,
                    "arrivedAt": null,
                    "voucherId": "3829497000000001",
                    "basis": "回单模糊无法对账，请重新上传",
                    "operatorId": "700100",
                    "operatorName": "运营·小刘",
                    "createdAt": "2026-10-16T15:30:00"
                  }
                ],
                "history": [
                  {
                    "id": "3829495000000000",
                    "contractId": "3829493001111111",
                    "contractNo": "SG-l590hd3t5y",
                    "nodeCount": 3,
                    "totalAmount": "4800000",
                    "totalSettled": "0",
                    "createdAt": "2026-10-12T11:00:00"
                  }
                ]
              },
              "requestId": "req-p1a2b3",
              "errors": null
            }
            """;

    /** aiplatform 付款计划不存在的真实错误形状：HTTP 404 + 信封 code=5036（ORD_036 数字业务码）。 */
    private static final String ORD_036_ERROR_ENVELOPE = """
            {
              "code": 5036,
              "message": "付款计划不存在",
              "data": null,
              "requestId": "req-p2b3c4",
              "errors": null
            }
            """;

    /** 录入无效①：HTTP 400 + 信封 code=5039（ORD_039——至少一条/名称必填/金额为正）。 */
    private static final String ORD_039_ERROR_ENVELOPE = """
            {
              "code": 5039,
              "message": "付款节点录入无效（至少一条、名称必填、金额为正）",
              "data": null,
              "requestId": "req-p3c4d5",
              "errors": null
            }
            """;

    /** 重录窗口已关：HTTP 409 + 信封 code=5044（ORD_044——支付活动已开始，聚合腿与凭证腿同译）。 */
    private static final String ORD_044_ERROR_ENVELOPE = """
            {
              "code": 5044,
              "message": "付款节点已开始支付，不可重新录入",
              "data": null,
              "requestId": "req-p4d5e6",
              "errors": null
            }
            """;

    /** 核验录入无效：HTTP 400 + 信封 code=5041（ORD_041——实到金额须为正、实到时间必填）。 */
    private static final String ORD_041_ERROR_ENVELOPE = """
            {
              "code": 5041,
              "message": "核验录入无效（实到金额须为正、实到时间必填）",
              "data": null,
              "requestId": "req-p5e6f7",
              "errors": null
            }
            """;

    /** 状态守卫：HTTP 409 + 信封 code=5038（ORD_038——仅线下转账待核验态可达）。 */
    private static final String ORD_038_ERROR_ENVELOPE = """
            {
              "code": 5038,
              "message": "付款节点当前状态不允许该操作",
              "data": null,
              "requestId": "req-p6f7a8",
              "errors": null
            }
            """;

    /** 打回依据必填：HTTP 400 + 信封 code=5042（ORD_042——打回依据是审计正身）。 */
    private static final String ORD_042_ERROR_ENVELOPE = """
            {
              "code": 5042,
              "message": "打回依据必填（人工核验打回须填写依据留痕）",
              "data": null,
              "requestId": "req-p7a8b9",
              "errors": null
            }
            """;

    /** 凭证不存在（或不属该节点）：HTTP 404 + 信封 code=5045（ORD_045——download 错误信封在 body 里）。 */
    private static final String ORD_045_ERROR_ENVELOPE = """
            {
              "code": 5045,
              "message": "凭证不存在或不属于该节点",
              "data": null,
              "requestId": "req-p8b9c0",
              "errors": null
            }
            """;

    // ========== 计划详情（三段：plan 节点五态/凭证清单 + trails 留痕时间线 + history 历史行）==========

    @Test
    void given_planDetailEnvelope_when_getPlanDetail_then_wireUrlMirrorsAndFiveStatesBoundVerbatim() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithStubTransport(
                PLAN_DETAIL_ENVELOPE, null, wireUrl, wireBody);

        AiplatformPaymentPlanDetailResponse detail = appService.getPlanDetail(ORDER_ID);

        // 出站路径逐字镜像 provider backoffice 路由（付款域嵌 orders 下，无统一顶层——照 provider 原形）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/" + ORDER_ID + "/payment-plan");

        // 第一段 plan：计划身份/合同冻结同刻信息/总额与累计已核到账
        var plan = detail.plan();
        assertThat(plan.id()).isEqualTo("3829495000000001");
        assertThat(plan.orderId()).isEqualTo(ORDER_ID);
        assertThat(plan.contractId()).isEqualTo("3829493005555555");
        assertThat(plan.contractNo()).isEqualTo("SG-l5eq8m2k7c");
        assertThat(plan.totalAmount()).isEqualTo(3900000L);
        assertThat(plan.totalSettled()).isEqualTo(195000L);
        assertThat(plan.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 9, 0, 0));

        // 五节点恰钉五态（code + 中文名随行，Integer code 直通不建 typed enum；录入序即展示序）
        assertThat(plan.nodes()).hasSize(5);
        assertThat(plan.nodes())
                .extracting(node -> node.status(), node -> node.statusName())
                .containsExactly(
                        tuple(1, "待支付"),
                        tuple(2, "支付处理中"),
                        tuple(3, "支付失败或关闭"),
                        tuple(4, "线下转账待核验"),
                        tuple(5, "已到账"));

        // 首节点全字段：附言码自合同号定格（<合同号>-K<序>）、开工门随行定、渠道起步线下转账
        var first = plan.nodes().get(0);
        assertThat(first.id()).isEqualTo("3829496001000001");
        assertThat(first.seq()).isEqualTo(1);
        assertThat(first.name()).isEqualTo("首期开工款");
        assertThat(first.amount()).isEqualTo(1950000L);
        assertThat(first.percentage()).isEqualTo(50);
        assertThat(first.remarkCode()).isEqualTo("SG-l5eq8m2k7c-K1");
        assertThat(first.kickoffGate()).isTrue();
        assertThat(first.channelType()).isEqualTo(1);
        assertThat(first.channelTypeName()).isEqualTo("线下转账");
        assertThat(first.channelRefNo()).isNull();
        assertThat(first.settledAmount()).isEqualTo(0L);
        assertThat(first.remainingAmount()).isEqualTo(1950000L);
        assertThat(first.receivedAt()).isNull();
        assertThat(first.vouchers()).isEmpty();

        // 占比展示换算（绝对额录入、服务端现算 50/20/10/15/5——总额 3,900,000 分）
        assertThat(plan.nodes())
                .extracting(node -> node.percentage())
                .containsExactly(50, 20, 10, 15, 5);

        // 待核验节点（4）：凭证两行上传追加保留不覆盖（上传仅代表已提交）
        var awaiting = plan.nodes().get(3);
        assertThat(awaiting.vouchers()).hasSize(2);
        assertThat(awaiting.vouchers())
                .extracting(voucher -> voucher.fileName(), voucher -> voucher.contentType())
                .containsExactly(
                        tuple("receipt-1.pdf", "application/pdf"),
                        tuple("receipt-2.png", "image/png"));
        assertThat(awaiting.vouchers().get(0).id()).isEqualTo(VOUCHER_ID);
        assertThat(awaiting.vouchers().get(0).nodeId()).isEqualTo("3829496001000004");
        assertThat(awaiting.vouchers().get(0).byteSize()).isEqualTo(102400L);
        assertThat(awaiting.vouchers().get(1).createdAt())
                .isEqualTo(LocalDateTime.of(2026, 10, 16, 15, 10, 0));

        // 已到账节点（5）：渠道流水号落值＋receivedAt＝实到时间＋累计/推导（195,000 全额→remaining 0）
        var received = plan.nodes().get(4);
        assertThat(received.channelRefNo()).isEqualTo("BANK-20261017-0001");
        assertThat(received.receivedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0, 0));
        assertThat(received.settledAmount()).isEqualTo(195000L);
        assertThat(received.remainingAmount()).isEqualTo(0L);

        // 第二段 trails：留痕两行新 → 旧——到账核验（action=1，实到金额/时间/关联凭证/依据）与
        // 打回（action=2，实到两肢 null——打回不动累计）各一行 append-only，操作者两肢随行落
        assertThat(detail.trails()).hasSize(2);
        var verifyTrail = detail.trails().get(0);
        assertThat(verifyTrail.id()).isEqualTo("3829498000000002");
        assertThat(verifyTrail.nodeId()).isEqualTo("3829496001000005");
        assertThat(verifyTrail.action()).isEqualTo(1);
        assertThat(verifyTrail.actionName()).isEqualTo("到账核验");
        assertThat(verifyTrail.fromStatus()).isEqualTo(4);
        assertThat(verifyTrail.toStatus()).isEqualTo(5);
        assertThat(verifyTrail.actualAmount()).isEqualTo(195000L);
        assertThat(verifyTrail.arrivedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0, 0));
        assertThat(verifyTrail.voucherId()).isEqualTo("3829497000000003");
        assertThat(verifyTrail.basis()).isEqualTo("银企对账一致");
        assertThat(verifyTrail.operatorId()).isEqualTo("700100");
        assertThat(verifyTrail.operatorName()).isEqualTo("运营·小刘");
        assertThat(verifyTrail.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 5, 0));
        var rejectTrail = detail.trails().get(1);
        assertThat(rejectTrail.action()).isEqualTo(2);
        assertThat(rejectTrail.actionName()).isEqualTo("打回");
        assertThat(rejectTrail.fromStatus()).isEqualTo(4);
        assertThat(rejectTrail.toStatus()).isEqualTo(1);
        assertThat(rejectTrail.actualAmount()).isNull();
        assertThat(rejectTrail.arrivedAt()).isNull();
        assertThat(rejectTrail.basis()).isEqualTo("回单模糊无法对账，请重新上传");

        // 第三段 history：历史计划行摘要（旧计划随旧合同留痕——撤回重建后旧行定格可见）
        assertThat(detail.history()).hasSize(1);
        var historyRow = detail.history().get(0);
        assertThat(historyRow.id()).isEqualTo("3829495000000000");
        assertThat(historyRow.contractId()).isEqualTo("3829493001111111");
        assertThat(historyRow.contractNo()).isEqualTo("SG-l590hd3t5y");
        assertThat(historyRow.nodeCount()).isEqualTo(3);
        assertThat(historyRow.totalAmount()).isEqualTo(4800000L);
        assertThat(historyRow.totalSettled()).isEqualTo(0L);
        assertThat(historyRow.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 12, 11, 0, 0));
    }

    @Test
    void given_ord036ErrorEnvelope_when_getPlanDetail_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                404, ORD_036_ERROR_ENVELOPE, wireUrl);

        // 忠实透传（spec #102）：HTTP 404 + 数字业务码 5036（ORD_036＝域码 5×1000＋36）
        // + provider message 原样——该订单名下无付款计划（尚无合同即无计划），非映射不顶替
        assertThatThrownBy(() -> appService.getPlanDetail(ORDER_ID))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5036);
                    assertThat(upstream.getMessage()).isEqualTo("付款计划不存在");
                });
    }

    /**
     * aiplatform POST /api/backoffice/orders/{orderId}/payment-plan/nodes 的真实成功响应
     * 形状（录入回执＝PaymentPlanResponse，三节点 40/40/20——provider
     * {@code PaymentPlanAppServiceTest#given_empty_plan_when_enter_nodes…} 实值口径）：
     * 序号按录入序派生、附言码自合同号定格（K1/K2/K3）、开工门随行定、渠道起步线下转账
     * （v1 唯一走通路径）＋流水号空、占比现算（40/40/20——绝对额录入）、待支付起步零核到账。
     */
    private static final String ENTER_NODES_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829495000000001",
                "orderId": "3829492001234567",
                "contractId": "3829493005555555",
                "contractNo": "SG-l5eq8m2k7c",
                "totalAmount": "4800000",
                "totalSettled": "0",
                "createdAt": "2026-10-20T09:00:00",
                "nodes": [
                  {
                    "id": "3829496002000001",
                    "seq": 1,
                    "name": "首期开工款",
                    "amount": "1920000",
                    "percentage": 40,
                    "status": 1,
                    "statusName": "待支付",
                    "remarkCode": "SG-l5eq8m2k7c-K1",
                    "kickoffGate": true,
                    "channelType": 1,
                    "channelTypeName": "线下转账",
                    "channelRefNo": null,
                    "settledAmount": "0",
                    "remainingAmount": "1920000",
                    "receivedAt": null,
                    "vouchers": []
                  },
                  {
                    "id": "3829496002000002",
                    "seq": 2,
                    "name": "中期验收款",
                    "amount": "1920000",
                    "percentage": 40,
                    "status": 1,
                    "statusName": "待支付",
                    "remarkCode": "SG-l5eq8m2k7c-K2",
                    "kickoffGate": false,
                    "channelType": 1,
                    "channelTypeName": "线下转账",
                    "channelRefNo": null,
                    "settledAmount": "0",
                    "remainingAmount": "1920000",
                    "receivedAt": null,
                    "vouchers": []
                  },
                  {
                    "id": "3829496002000003",
                    "seq": 3,
                    "name": "尾款结算",
                    "amount": "960000",
                    "percentage": 20,
                    "status": 1,
                    "statusName": "待支付",
                    "remarkCode": "SG-l5eq8m2k7c-K3",
                    "kickoffGate": false,
                    "channelType": 1,
                    "channelTypeName": "线下转账",
                    "channelRefNo": null,
                    "settledAmount": "0",
                    "remainingAmount": "960000",
                    "receivedAt": null,
                    "vouchers": []
                  }
                ]
              },
              "requestId": "req-p9c0d1",
              "errors": null
            }
            """;

    // ========== 节点录入（整组替换＋开工门设定；支付未开始为唯一重录窗口）==========

    @Test
    void given_nodesCommand_when_enterNodes_then_wireMirrorsProviderAndReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithStubTransport(
                ENTER_NODES_RECEIPT_ENVELOPE, null, wireUrl, wireBody);

        AiplatformPaymentPlanResponse receipt = appService.enterNodes(ORDER_ID,
                new AiplatformPaymentNodesCommand(List.of(
                        new AiplatformPaymentNodesCommand.NodeSpec("首期开工款", 1920000L, true),
                        new AiplatformPaymentNodesCommand.NodeSpec("中期验收款", 1920000L, false),
                        new AiplatformPaymentNodesCommand.NodeSpec("尾款结算", 960000L, false))));

        // 出站路径逐字镜像 provider backoffice 路由（录入口挂 payment-plan/nodes）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/orders/" + ORDER_ID + "/payment-plan/nodes");
        // 命令体逐字镜像 {nodes:[{name, amount, kickoffGate}]}——整组替换；amount Long 分出
        // JSON string（生产 mapper 口径，provider 默认可回读——同订单 quote 的 amount）；
        // 序号不在录入面（服务端按列表序派生）
        assertThat(wireBody[0]).isEqualTo(
                "{\"nodes\":[{\"name\":\"首期开工款\",\"amount\":\"1920000\",\"kickoffGate\":true},"
                        + "{\"name\":\"中期验收款\",\"amount\":\"1920000\",\"kickoffGate\":false},"
                        + "{\"name\":\"尾款结算\",\"amount\":\"960000\",\"kickoffGate\":false}]}");

        // 录入回执：三节点待支付起步（provider 测试实值 40/40/20）——附言码自合同号定格
        // （K1/K2/K3）、开工门随行定（首节点 true、非默认全款）、渠道起步线下转账流水号空
        assertThat(receipt.totalAmount()).isEqualTo(4800000L);
        assertThat(receipt.totalSettled()).isEqualTo(0L);
        assertThat(receipt.nodes()).hasSize(3);
        assertThat(receipt.nodes())
                .extracting(node -> node.remarkCode(), node -> node.kickoffGate())
                .containsExactly(
                        tuple("SG-l5eq8m2k7c-K1", true),
                        tuple("SG-l5eq8m2k7c-K2", false),
                        tuple("SG-l5eq8m2k7c-K3", false));
        assertThat(receipt.nodes())
                .extracting(node -> node.percentage())
                .containsExactly(40, 40, 20);
        assertThat(receipt.nodes()).allSatisfy(node -> {
            assertThat(node.status()).isEqualTo(1);
            assertThat(node.statusName()).isEqualTo("待支付");
            assertThat(node.channelType()).isEqualTo(1);
            assertThat(node.channelTypeName()).isEqualTo("线下转账");
            assertThat(node.channelRefNo()).isNull();
            assertThat(node.receivedAt()).isNull();
        });
    }

    @Test
    void given_ord039ErrorEnvelope_when_enterNodes_then_invalidSpecsGuardPassedThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                400, ORD_039_ERROR_ENVELOPE, wireUrl);

        // 录入无效（至少一条、名称必填、金额为正）归 provider 聚合守卫——BFF 不前置拦截空组/非正值
        assertThatThrownBy(() -> appService.enterNodes(ORDER_ID, new AiplatformPaymentNodesCommand(List.of())))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5039);
                    assertThat(upstream.getMessage())
                            .isEqualTo("付款节点录入无效（至少一条、名称必填、金额为正）");
                });
    }

    @Test
    void given_ord044ErrorEnvelope_when_enterNodes_then_reentryWindowGuardPassedThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                409, ORD_044_ERROR_ENVELOPE, wireUrl);

        // 重录窗口（支付活动未开始——全部待支付且无凭证无留痕）守卫归 provider（聚合腿＋凭证腿
        // 两腿同译）；已开始变动走合同侧撤回重建——BFF 不复算窗口
        assertThatThrownBy(() -> appService.enterNodes(ORDER_ID, new AiplatformPaymentNodesCommand(List.of(
                new AiplatformPaymentNodesCommand.NodeSpec("尾款", 960000L, false)))))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5044);
                    assertThat(upstream.getMessage()).isEqualTo("付款节点已开始支付，不可重新录入");
                });
    }

    /**
     * aiplatform POST /api/backoffice/orders/{orderId}/payment-nodes/{nodeId}/verify 的真实
     * 成功响应形状（核验回执＝PaymentPlanResponse，<strong>全额转已到账</strong>——provider
     * {@code PaymentPlanAppServiceTest#given_awaiting_node_when_verify_full_amount…} 实值
     * 口径）：节点 1 转 5 已到账（receivedAt＝实到时间）、渠道流水号落值、累计核到账
     * 1,920,000 全额、remaining 0、totalSettled 同步累计。
     */
    private static final String VERIFY_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829495000000001",
                "orderId": "3829492001234567",
                "contractId": "3829493005555555",
                "contractNo": "SG-l5eq8m2k7c",
                "totalAmount": "4800000",
                "totalSettled": "1920000",
                "createdAt": "2026-10-20T09:00:00",
                "nodes": [
                  {
                    "id": "3829496002000001",
                    "seq": 1,
                    "name": "首期开工款",
                    "amount": "1920000",
                    "percentage": 40,
                    "status": 5,
                    "statusName": "已到账",
                    "remarkCode": "SG-l5eq8m2k7c-K1",
                    "kickoffGate": true,
                    "channelType": 1,
                    "channelTypeName": "线下转账",
                    "channelRefNo": "BANK-20261017-0001",
                    "settledAmount": "1920000",
                    "remainingAmount": "0",
                    "receivedAt": "2026-10-17T10:00:00",
                    "vouchers": [
                      {
                        "id": "3829497000000001",
                        "nodeId": "3829496002000001",
                        "fileName": "receipt.pdf",
                        "contentType": "application/pdf",
                        "byteSize": "1024",
                        "createdAt": "2026-10-17T09:30:00"
                      }
                    ]
                  }
                ]
              },
              "requestId": "req-pa1b2c",
              "errors": null
            }
            """;

    // ========== 到账核验（全额判定归 provider；不足留痕回待支付）==========

    @Test
    void given_verifyCommand_when_verify_then_wireMirrorsProviderAndReceivedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithStubTransport(
                VERIFY_RECEIPT_ENVELOPE, null, wireUrl, wireBody);

        AiplatformPaymentPlanResponse receipt = appService.verify(ORDER_ID, "3829496002000001",
                new AiplatformPaymentVerifyCommand(1920000L, LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                        VOUCHER_ID, "BANK-20261017-0001", "银企对账一致"));

        // 出站路径逐字镜像 provider backoffice 路由（核验口挂 payment-nodes/{nodeId}/verify）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/orders/" + ORDER_ID
                + "/payment-nodes/3829496002000001/verify");
        // 命令体逐字镜像 {actualAmount, arrivedAt, voucherId, channelRefNo, basis}（provider
        // VerifyPaymentCommand 同序）——actualAmount Long 分出 JSON string（生产 mapper 口径）、
        // arrivedAt 全秒 ISO；操作者不在 body（经透传头落痕，缺头 provider ORD_043 拒）
        assertThat(wireBody[0]).isEqualTo("{\"actualAmount\":\"1920000\","
                + "\"arrivedAt\":\"2026-10-17T10:00:00\","
                + "\"voucherId\":\"3829497000000001\","
                + "\"channelRefNo\":\"BANK-20261017-0001\","
                + "\"basis\":\"银企对账一致\"}");

        // 全额判定（累计核到账 ≥ 节点金额）在 provider 过——足额转「已到账」：status 5、
        // receivedAt＝实到时间、渠道流水号落值、settled 全额、remaining 0、totalSettled 同步
        var node = receipt.nodes().get(0);
        assertThat(node.status()).isEqualTo(5);
        assertThat(node.statusName()).isEqualTo("已到账");
        assertThat(node.receivedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0, 0));
        assertThat(node.channelRefNo()).isEqualTo("BANK-20261017-0001");
        assertThat(node.settledAmount()).isEqualTo(1920000L);
        assertThat(node.remainingAmount()).isEqualTo(0L);
        assertThat(receipt.totalSettled()).isEqualTo(1920000L);
        // 凭证清单随行（上传序——核验不改凭证面）
        assertThat(node.vouchers()).hasSize(1);
        assertThat(node.vouchers().get(0).fileName()).isEqualTo("receipt.pdf");
    }

    @Test
    void given_ord041ErrorEnvelope_when_verify_then_invalidEntryGuardPassedThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                400, ORD_041_ERROR_ENVELOPE, wireUrl);

        // 核验录入无效（实到金额须为正、实到时间必填）归 provider——BFF 不前置拦截 0/null
        assertThatThrownBy(() -> appService.verify(ORDER_ID, "3829496002000001",
                new AiplatformPaymentVerifyCommand(0L, LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                        null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5041);
                    assertThat(upstream.getMessage()).isEqualTo("核验录入无效（实到金额须为正、实到时间必填）");
                });
    }

    @Test
    void given_ord038ErrorEnvelope_when_verify_then_stateGuardPassedThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                409, ORD_038_ERROR_ENVELOPE, wireUrl);

        // 状态守卫（仅线下转账待核验态可达——先有凭证主张才有核验对象）归 provider——
        // 待支付直接核验拒、被拒动作零留痕行
        assertThatThrownBy(() -> appService.verify(ORDER_ID, "3829496002000001",
                new AiplatformPaymentVerifyCommand(100L, LocalDateTime.now(), null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5038);
                    assertThat(upstream.getMessage()).isEqualTo("付款节点当前状态不允许该操作");
                });
    }

    /**
     * aiplatform POST /api/backoffice/orders/{orderId}/payment-nodes/{nodeId}/reject 的真实
     * 成功响应形状（打回回执＝PaymentPlanResponse，<strong>回待支付</strong>——provider
     * {@code PaymentPlanAppServiceTest#given_awaiting_node_when_reject…} 实值口径）：
     * 节点回 1 待支付、settledAmount 0（打回不动累计已核到账——核到账只经核验录入累积）。
     */
    private static final String REJECT_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829495000000001",
                "orderId": "3829492001234567",
                "contractId": "3829493005555555",
                "contractNo": "SG-l5eq8m2k7c",
                "totalAmount": "4800000",
                "totalSettled": "0",
                "createdAt": "2026-10-20T09:00:00",
                "nodes": [
                  {
                    "id": "3829496002000001",
                    "seq": 1,
                    "name": "首期开工款",
                    "amount": "1920000",
                    "percentage": 40,
                    "status": 1,
                    "statusName": "待支付",
                    "remarkCode": "SG-l5eq8m2k7c-K1",
                    "kickoffGate": true,
                    "channelType": 1,
                    "channelTypeName": "线下转账",
                    "channelRefNo": null,
                    "settledAmount": "0",
                    "remainingAmount": "1920000",
                    "receivedAt": null,
                    "vouchers": [
                      {
                        "id": "3829497000000001",
                        "nodeId": "3829496002000001",
                        "fileName": "blurry.pdf",
                        "contentType": "application/pdf",
                        "byteSize": "2048",
                        "createdAt": "2026-10-16T15:00:00"
                      }
                    ]
                  }
                ]
              },
              "requestId": "req-pb3c4d",
              "errors": null
            }
            """;

    // ========== 打回（依据必填留痕，回待支付不动累计）==========

    @Test
    void given_rejectCommand_when_reject_then_wireMirrorsProviderAndPendingReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithStubTransport(
                REJECT_RECEIPT_ENVELOPE, null, wireUrl, wireBody);

        AiplatformPaymentPlanResponse receipt = appService.reject(ORDER_ID, "3829496002000001",
                new AiplatformPaymentRejectCommand(null, "回单模糊无法对账，请重新上传"));

        // 出站路径逐字镜像 provider backoffice 路由（打回口挂 payment-nodes/{nodeId}/reject）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/orders/" + ORDER_ID
                + "/payment-nodes/3829496002000001/reject");
        // 命令体逐字镜像 {voucherId, basis}——依据必填是 provider 守卫（ORD_042），
        // voucherId 可空在场（null 序列化为 JSON null）
        assertThat(wireBody[0]).isEqualTo(
                "{\"voucherId\":null,\"basis\":\"回单模糊无法对账，请重新上传\"}");

        // 打回回执：回「待支付」（用户可再动作态——补传凭证）、settledAmount 0 不动累计、
        // totalSettled 同步为 0（核到账只经核验录入累积）
        var node = receipt.nodes().get(0);
        assertThat(node.status()).isEqualTo(1);
        assertThat(node.statusName()).isEqualTo("待支付");
        assertThat(node.settledAmount()).isEqualTo(0L);
        assertThat(node.remainingAmount()).isEqualTo(1920000L);
        assertThat(node.receivedAt()).isNull();
        assertThat(receipt.totalSettled()).isEqualTo(0L);
    }

    @Test
    void given_ord042ErrorEnvelope_when_reject_then_basisRequiredGuardPassedThrough() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                400, ORD_042_ERROR_ENVELOPE, wireUrl);

        // 打回依据必填归 provider（打回依据是审计正身）——BFF 不前置拦截空白依据
        assertThatThrownBy(() -> appService.reject(ORDER_ID, "3829496002000001",
                new AiplatformPaymentRejectCommand(null, " ")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5042);
                    assertThat(upstream.getMessage()).isEqualTo("打回依据必填（人工核验打回须填写依据留痕）");
                });
    }

    // ========== 凭证内容（二进制无信封，字节与响应头保全）==========

    /** PDF 魔数起始、含非法 UTF-8 字节（0xff/0xfe）与 NUL 的字节流——ofString 解码会损坏的回归锚点。 */
    private static final byte[] VOUCHER_PDF_BYTES = {
            (byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2d, (byte) 0x31,
            (byte) 0x2e, (byte) 0x34, 0x20, (byte) 0xff, (byte) 0xfe, 0x41, 0x00, 0x0d, 0x0a
    };

    private static final String VOUCHER_DISPOSITION = "attachment; filename=\"receipt-1.pdf\"";

    @Test
    void given_voucherBinaryResponse_when_downloadVoucherContent_then_bytesAndHeadersPreserved() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/pdf"),
                "Content-Disposition", List.of(VOUCHER_DISPOSITION)), (a, b) -> true);
        var appService = AiplatformWireTestSupport.paymentAppServiceWithStubTransport(
                null, new BinaryResponse(200, providerHeaders, VOUCHER_PDF_BYTES), wireUrl, wireBody);

        AiplatformPaymentVoucherContentResponse content = appService.getVoucherContent(VOUCHER_ID);

        // 出站路径逐字镜像 provider backoffice 路由（凭证内容口挂 payment-vouchers 顶层——
        // 付款域无统一顶层路径，照 provider 原形）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/payment-vouchers/" + VOUCHER_ID + "/content");

        // 凭证字节逐位相等（含 0xff/0xfe/NUL——ofString 解码损坏的回归锚点；PDF 二进制非 UTF-8
        // 文本），内存中转无信封
        assertThat(content.content()).containsExactly(VOUCHER_PDF_BYTES);
        // provider 响应头 raw 值透传（不打解析、不重构文件名——真实媒体类型＋attachment 文件名）
        assertThat(content.contentType()).isEqualTo("application/pdf");
        assertThat(content.contentDisposition()).isEqualTo(VOUCHER_DISPOSITION);
    }

    @Test
    void given_ord045OnDownload_when_downloadVoucherContent_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.paymentAppServiceWithErrorTransport(
                404, ORD_045_ERROR_ENVELOPE, wireUrl);

        // download 的 ≥400 由框架抛 OpenApiClientException（错误信封在 body 里）——AiplatformClient
        // 同款翻译透传，与 JSON 端点错误路径一致；凭证不存在（含畸形 id／伴行字节缺行）同译不顶替
        assertThatThrownBy(() -> appService.getVoucherContent("3829497999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5045);
                    assertThat(upstream.getMessage()).isEqualTo("凭证不存在或不属于该节点");
                });
    }
}
