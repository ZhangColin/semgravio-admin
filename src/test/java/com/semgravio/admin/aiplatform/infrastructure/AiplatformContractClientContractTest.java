package com.semgravio.admin.aiplatform.infrastructure;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractRecordCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractSignCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformContractQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractSummaryResponse;
import com.cartisan.web.response.PageResponse;

/**
 * aiplatform 合同域（issue #104 A1＋issue #105 A2，spec #102 A 批——provider aiplatform #336
 * 合同域六态承接）九端点的 wire 契约测试：清单（多维检索分页）/ 详情（全量面＋PRD 全文冗余
 * 拷贝＋动作时间线）/ 创建重建（与用户 confirmScope 共用创建正本）＋六态推进写口
 * advance/fail/withdraw/void/sign/record（依据必填留痕、守卫归 provider）。
 *
 * <p>契约事实（对照 aiplatform 源码 {@code BackofficeContractController} /
 * {@code BackofficeContractSummaryResponse} / {@code BackofficeContractDetailResponse} /
 * {@code ContractResponse} / {@code ContractTransitionResponse} / {@code ContractStatus} /
 * {@code ContractTransitionAction} / {@code ContractActionCommand} / {@code SignContractCommand} /
 * {@code RecordContractCommand} / {@code OrderMessage}＋provider 本地 /v3/api-docs 实拉
 * （2026-11-02，字段序与参数集程序化核对一致；fixture 实值对照 provider
 * {@code ContractAppServiceTest} 兜底）：</p>
 * <ul>
 *   <li>九端点均包 {@code ApiResponse<T>} 信封，须按信封反序列化并取 {@code .data()}；
 *       清单 data 为 {@code PageResponse{items,total,page,size}}（page 1-based）。</li>
 *   <li>合同六态 {@code status} Integer code + {@code statusName} 中文名（BaseEnum 约定，
 *       provider 出 JSON number）：1=准备中 2=待核对与签署 3=签署进行中 4=签署完成
 *       5=签署失败或过期 6=已撤回或作废——admin Integer code 直通不建 typed enum
 *       （provider 枚举非本应用领域枚举，对齐 tokenKind/deliverableType 先例）。</li>
 *   <li>金额 {@code amount} Long（分）→ JSON <strong>string</strong>（cartisan-web 全局
 *       Long→ToStringSerializer 出口口径，同订单域）；{@code version} primitive int →
 *       JSON number；时间 ISO。</li>
 *   <li>清单 {@code status} 多选为<strong>逗号分隔单值</strong>（{@code status=2,4}）——
 *       aiplatform 签名协议按 query 参数名去重（同订单清单先例）。</li>
 *   <li>详情三段：{@code contract}（全字段 ContractResponse——含「需更新」派生标志与范围
 *       快照冻结面）＋{@code prdSnapshot}（PRD 确认版全文冗余拷贝，合同自含）＋
 *       {@code transitions}（动作留痕新→旧，append-only 全量）。</li>
 *   <li>留痕行动作 {@code action} Integer code：1=推进 2=撤回 3=作废 4=签署标记
 *       5=草稿重录 6=失败或过期；非迁移动作（签署标记/草稿重录）from = to。
 *       合同动作操作者必填（provider ORD_035 拒无名留痕）——留痕行操作者两肢恒有值。</li>
 *   <li>创建重建 {@code POST /api/backoffice/contracts/orders/{orderId}}：<strong>无请求体</strong>
 *       （provider 端只读路径参数），回执 {@code ApiResponse<ContractResponse>}（准备中起步，
 *       四个后台录入预览字段 null 在场——BFF 不代填）。</li>
 *   <li>六态推进写口 {@code POST /api/backoffice/contracts/{id}/advance|fail|withdraw|void|
 *       sign|record}：命令体逐字镜像——advance/fail/withdraw/void 共用 {@code {basis}}、
 *       sign {@code {party, basis}}（party Integer code 出 JSON number，provider BaseEnum
 *       按 code 反序列化）、record {@code {partyA, partyB, projectScope, paymentPlan,
 *       basis}}（provider RecordContractCommand 同序）；回执均为 {@code ContractResponse}
 *       （动作后合同现值——advance 不动版本、record version +1 且「需更新」清零）。</li>
 *   <li>错误透传（数字业务码＝域码 5×1000＋序号）：合同不存在 ORD_020→5020（404）、
 *       订单不存在 ORD_001→5001（404）、已有活跃合同 ORD_024→5024（409）、尚无报价
 *       ORD_025→5025（409）、范围未录入 ORD_026→5026（409）、状态守卫 ORD_021→5021（409）、
 *       依据必填 ORD_022→5022（400）、依据超长 ORD_023→5023（400）、双方未均签
 *       ORD_027→5027（409）、该方已签 ORD_028→5028（409）、重录窗口 ORD_031→5031（409）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformContractClientContractTest {

    /** 详情用合同标识（TSID 十进制串——签署进行中单，与详情 fixture 对应）。 */
    private static final String CONTRACT_ID = "3829493004444444";

    /**
     * aiplatform GET /api/backoffice/contracts 的真实成功响应形状（信封 + PageResponse：
     * <strong>六行恰钉六态</strong>——1 准备中/2 待核对与签署/3 签署进行中/4 签署完成/
     * 5 签署失败或过期/6 已撤回或作废，provider 定死 TSID 倒序＝新合同在前）。
     * Long 字段（amount/total）为 <strong>JSON string</strong>；version primitive int 为
     * JSON number。
     */
    private static final String CONTRACT_PAGE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "items": [
                  {
                    "id": "3829493006666666",
                    "contractNo": "SG-l5f2xq9wn1",
                    "orderId": "3829492001234567",
                    "status": 1,
                    "statusName": "准备中",
                    "version": 1,
                    "amount": "128000",
                    "currency": "CNY",
                    "createdAt": "2026-10-22T09:00:00"
                  },
                  {
                    "id": "3829493005555555",
                    "contractNo": "SG-l5eq8m2k7c",
                    "orderId": "3829492005555444",
                    "status": 2,
                    "statusName": "待核对与签署",
                    "version": 3,
                    "amount": "1999000",
                    "currency": "CNY",
                    "createdAt": "2026-10-20T14:30:00"
                  },
                  {
                    "id": "3829493004444444",
                    "contractNo": "SG-l5d3vt0h4b",
                    "orderId": "3829492007777777",
                    "status": 3,
                    "statusName": "签署进行中",
                    "version": 1,
                    "amount": "128000",
                    "currency": "CNY",
                    "createdAt": "2026-10-18T09:30:00"
                  },
                  {
                    "id": "3829493003333333",
                    "contractNo": "SG-l5b7wr9d2a",
                    "orderId": "3829492009999999",
                    "status": 4,
                    "statusName": "签署完成",
                    "version": 1,
                    "amount": "128000",
                    "currency": "CNY",
                    "createdAt": "2026-10-15T11:00:00"
                  },
                  {
                    "id": "3829493002222222",
                    "contractNo": "SG-l5a1pk6f8z",
                    "orderId": "3829492008888888",
                    "status": 5,
                    "statusName": "签署失败或过期",
                    "version": 2,
                    "amount": "1999000",
                    "currency": "CNY",
                    "createdAt": "2026-10-12T16:20:00"
                  },
                  {
                    "id": "3829493001111111",
                    "contractNo": "SG-l590hd3t5y",
                    "orderId": "3829492001234567",
                    "status": 6,
                    "statusName": "已撤回或作废",
                    "version": 1,
                    "amount": "128000",
                    "currency": "CNY",
                    "createdAt": "2026-10-10T10:10:00"
                  }
                ],
                "total": "6",
                "page": 3,
                "size": 20
              },
              "requestId": "req-c1a2b3",
              "errors": null
            }
            """;

    /** aiplatform 合同不存在的真实错误形状：HTTP 404 + 信封 code=5020（ORD_020 数字业务码）。 */
    private static final String ORD_020_ERROR_ENVELOPE = """
            {
              "code": 5020,
              "message": "合同不存在",
              "data": null,
              "requestId": "req-c2b3c4",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/contracts/{id} 的真实成功响应形状（详情三段：
     * contract 全字段＋PRD 确认版全文冗余拷贝＋动作留痕时间线新→旧）。签署进行中单：
     * 甲方已落签（partyASigned true＋时点）、乙方未签（false＋null）；「需更新」非草稿恒
     * false；范围快照三段＝创建时冻结的订单范围文本工作副本（provider
     * {@code ContractAppServiceTest} 实值口径）。留痕三行＝推进 1→2 → 推进 2→3 →
     * 甲方签署标记（非迁移动作 from=to）——append-only 全量，操作者两肢恒有值
     * （provider ORD_035 拒无名留痕）。
     */
    private static final String CONTRACT_DETAIL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "contract": {
                  "id": "3829493004444444",
                  "contractNo": "SG-l5d3vt0h4b",
                  "orderId": "3829492007777777",
                  "status": 3,
                  "statusName": "签署进行中",
                  "version": 1,
                  "needsUpdate": false,
                  "partyA": "语义引力（上海）科技有限公司",
                  "partyB": "文野",
                  "projectScope": "对公官网建设：企业门户＋产品展示",
                  "paymentPlan": "签约后 3 日内付 50%，验收后付 50%",
                  "scopeDeliverables": "交付清单：管理后台＋小程序",
                  "scopeAcceptance": "验收方式：按 PRD v3 验收",
                  "scopeTimeline": "工期口径：4 周",
                  "amount": "128000",
                  "currency": "CNY",
                  "prdVersionNo": 3,
                  "demoVersionNo": 2,
                  "partyASigned": true,
                  "partyASignedAt": "2026-10-20T10:00:00",
                  "partyBSigned": false,
                  "partyBSignedAt": null,
                  "createdAt": "2026-10-18T09:30:00"
                },
                "prdSnapshot": "# PRD v3\\n\\n已确认版本全文",
                "transitions": [
                  {
                    "action": 4,
                    "actionName": "签署标记",
                    "fromStatus": 3,
                    "fromStatusName": "签署进行中",
                    "toStatus": 3,
                    "toStatusName": "签署进行中",
                    "basis": "甲方已线下用印",
                    "operatorId": "700100",
                    "operatorName": "运营·小刘",
                    "createdAt": "2026-10-20T10:00:00"
                  },
                  {
                    "action": 1,
                    "actionName": "推进",
                    "fromStatus": 2,
                    "fromStatusName": "待核对与签署",
                    "toStatus": 3,
                    "toStatusName": "签署进行中",
                    "basis": "双方核对完毕，进入签署",
                    "operatorId": "700100",
                    "operatorName": "运营·小刘",
                    "createdAt": "2026-10-19T14:00:00"
                  },
                  {
                    "action": 1,
                    "actionName": "推进",
                    "fromStatus": 1,
                    "fromStatusName": "准备中",
                    "toStatus": 2,
                    "toStatusName": "待核对与签署",
                    "basis": "合同内容录入完成，提交核对",
                    "operatorId": "700200",
                    "operatorName": "商务·小王",
                    "createdAt": "2026-10-18T16:00:00"
                  }
                ]
              },
              "requestId": "req-c3d4e5",
              "errors": null
            }
            """;

    // ========== 清单（多维检索 + 分页 1-based 透传）==========

    @Test
    void given_fullFourDimensionFilter_when_listContracts_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.contractAppServiceWithStubTransport(
                CONTRACT_PAGE_ENVELOPE, wireUrl);

        appService.list(new AiplatformContractQuery(
                List.of(2, 4),
                LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                "3829492001234567", "SG-l5eq8m2k7c"), 3, 20);

        // 出站 wire 形状逐字镜像 provider 契约：page 1-based 直传（无 ±1）；status 逗号分隔
        // 单值（签名按参数名去重——同订单清单先例）；时间为全秒 ISO；SG- 串原样（URL 安全字符）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/contracts"
                + "?page=3&size=20"
                + "&status=2%2C4"
                + "&createdFrom=2026-10-01T00%3A00%3A00"
                + "&createdTo=2026-10-31T23%3A59%3A59"
                + "&orderId=3829492001234567"
                + "&contractNo=SG-l5eq8m2k7c");
    }

    @Test
    void given_pageEnvelope_when_listContracts_then_sixStatesBoundVerbatim() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.contractAppServiceWithStubTransport(
                CONTRACT_PAGE_ENVELOPE, wireUrl);

        PageResponse<AiplatformContractSummaryResponse> page =
                appService.list(new AiplatformContractQuery(null, null, null, null, null), 1, 20);

        // 信封正确拆开（items 非 null）——证明 CONTRACT_PAGE_TYPEREF 按
        // ApiResponse<PageResponse<…>> 反序列化并取 .data()
        assertThat(page.items()).hasSize(6);

        // 分页回显：provider 回报值原样透传（1-based，含 clamp 后值）——BFF 不夹取不换算
        assertThat(page.total()).isEqualTo(6L);
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);

        // 六态实值逐行钉死（code + 中文名随行，Integer code 直通不建 typed enum）
        assertThat(page.items())
                .extracting(AiplatformContractSummaryResponse::status, AiplatformContractSummaryResponse::statusName)
                .containsExactly(
                        tuple(1, "准备中"),
                        tuple(2, "待核对与签署"),
                        tuple(3, "签署进行中"),
                        tuple(4, "签署完成"),
                        tuple(5, "签署失败或过期"),
                        tuple(6, "已撤回或作废"));

        // 首行全字段：id/contractNo/orderId String、version int、金额 Long（分）、时间 ISO
        var preparing = page.items().get(0);
        assertThat(preparing.id()).isEqualTo("3829493006666666");
        assertThat(preparing.contractNo()).isEqualTo("SG-l5f2xq9wn1");
        assertThat(preparing.orderId()).isEqualTo("3829492001234567");
        assertThat(preparing.version()).isEqualTo(1);
        assertThat(preparing.amount()).isEqualTo(128000L);
        assertThat(preparing.currency()).isEqualTo("CNY");
        assertThat(preparing.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 9, 0, 0));

        // 版本随草稿重录递增（version 3——重录 +1 两次）；撤回行保留冻结事实
        var reRecorded = page.items().get(1);
        assertThat(reRecorded.version()).isEqualTo(3);
        assertThat(reRecorded.amount()).isEqualTo(1999000L);
        var withdrawn = page.items().get(5);
        assertThat(withdrawn.contractNo()).isEqualTo("SG-l590hd3t5y");
        assertThat(withdrawn.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 10, 10, 0));
    }

    @Test
    void given_ord020ErrorEnvelope_when_listContracts_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                404, ORD_020_ERROR_ENVELOPE, wireUrl);

        // 忠实透传（spec #102）：HTTP 404 + 数字业务码 5020（ORD_020＝域码 5×1000＋20）
        // + provider message 原样
        assertThatThrownBy(() -> appService.list(new AiplatformContractQuery(null, null, null, null, null), 1, 20))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5020);
                    assertThat(upstream.getMessage()).isEqualTo("合同不存在");
                });
    }

    /**
     * aiplatform POST /api/backoffice/contracts/orders/{orderId} 的真实成功响应形状
     * （创建重建回执＝ContractResponse，<strong>准备中起步</strong>）：冻结集取现值
     * （范围快照三段＋金额＋双版本引用），四个后台录入预览字段
     * （partyA/partyB/projectScope/paymentPlan）null 在场——BFF 不代填；双方签署位
     * false/null；「需更新」新建恒 false。
     */
    private static final String CONTRACT_CREATE_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493007777777",
                "contractNo": "SG-l5h6zy2g3x",
                "orderId": "3829492001234567",
                "status": 1,
                "statusName": "准备中",
                "version": 1,
                "needsUpdate": false,
                "partyA": null,
                "partyB": null,
                "projectScope": null,
                "paymentPlan": null,
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "128000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": false,
                "partyASignedAt": null,
                "partyBSigned": false,
                "partyBSignedAt": null,
                "createdAt": "2026-10-23T09:00:00"
              },
              "requestId": "req-c4e5f6",
              "errors": null
            }
            """;

    /** 创建前置门①：尚无报价——HTTP 409 + 信封 code=5025（ORD_025 数字业务码）。 */
    private static final String ORD_025_ERROR_ENVELOPE = """
            {
              "code": 5025,
              "message": "尚无报价，无法确认范围（后台落定报价后再确认）",
              "data": null,
              "requestId": "req-c5f6a7",
              "errors": null
            }
            """;

    /** 创建前置门②：范围文本未录入——HTTP 409 + 信封 code=5026（ORD_026 数字业务码）。 */
    private static final String ORD_026_ERROR_ENVELOPE = """
            {
              "code": 5026,
              "message": "范围文本未录入（后台录入交付清单/验收方式/工期口径后再确认）",
              "data": null,
              "requestId": "req-c6a7b8",
              "errors": null
            }
            """;

    /** 创建前置门③：已有活跃合同（撤回/作废前不可重建）——HTTP 409 + 信封 code=5024（ORD_024）。 */
    private static final String ORD_024_ERROR_ENVELOPE = """
            {
              "code": 5024,
              "message": "该订单已有活跃合同（至多一份非终态合同）",
              "data": null,
              "requestId": "req-c7b8c9",
              "errors": null
            }
            """;

    // ========== 详情（三段：全字段 contract＋PRD 全文冗余拷贝＋动作时间线）==========

    @Test
    void given_detailEnvelope_when_getDetail_then_threeSegmentsBoundWithTimeline() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.contractAppServiceWithStubTransport(
                CONTRACT_DETAIL_ENVELOPE, wireUrl);

        AiplatformContractDetailResponse detail = appService.getDetail(CONTRACT_ID);

        // 出站路径逐字镜像 provider backoffice 路由
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID);

        // 第一段 contract：六态/需更新派生标志/范围快照冻结面/双版本引用/双方签署位全字段
        var contract = detail.contract();
        assertThat(contract.id()).isEqualTo("3829493004444444");
        assertThat(contract.contractNo()).isEqualTo("SG-l5d3vt0h4b");
        assertThat(contract.orderId()).isEqualTo("3829492007777777");
        assertThat(contract.status()).isEqualTo(3);
        assertThat(contract.statusName()).isEqualTo("签署进行中");
        assertThat(contract.version()).isEqualTo(1);
        assertThat(contract.needsUpdate()).isFalse();
        assertThat(contract.partyA()).isEqualTo("语义引力（上海）科技有限公司");
        assertThat(contract.partyB()).isEqualTo("文野");
        assertThat(contract.projectScope()).isEqualTo("对公官网建设：企业门户＋产品展示");
        assertThat(contract.paymentPlan()).isEqualTo("签约后 3 日内付 50%，验收后付 50%");
        // 范围快照冻结面三段（创建时定格的订单范围文本工作副本——provider 测试实值）
        assertThat(contract.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序");
        assertThat(contract.scopeAcceptance()).isEqualTo("验收方式：按 PRD v3 验收");
        assertThat(contract.scopeTimeline()).isEqualTo("工期口径：4 周");
        assertThat(contract.amount()).isEqualTo(128000L);
        assertThat(contract.currency()).isEqualTo("CNY");
        assertThat(contract.prdVersionNo()).isEqualTo(3);
        assertThat(contract.demoVersionNo()).isEqualTo(2);
        // 双方签署位：甲方已签（true＋时点）、乙方未签（false＋null）——不自动迁移，推进归 advance
        assertThat(contract.partyASigned()).isTrue();
        assertThat(contract.partyASignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0, 0));
        assertThat(contract.partyBSigned()).isFalse();
        assertThat(contract.partyBSignedAt()).isNull();
        assertThat(contract.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 18, 9, 30, 0));

        // 第二段 PRD 确认版全文冗余拷贝（合同自含，后台依据面——provider 测试实值）
        assertThat(detail.prdSnapshot()).isEqualTo("# PRD v3\n\n已确认版本全文");

        // 第三段动作时间线：append-only 全量，新 → 旧；非迁移动作（签署标记）from = to
        assertThat(detail.transitions()).hasSize(3);
        var newest = detail.transitions().get(0);
        assertThat(newest.action()).isEqualTo(4);
        assertThat(newest.actionName()).isEqualTo("签署标记");
        assertThat(newest.fromStatus()).isEqualTo(3);
        assertThat(newest.fromStatusName()).isEqualTo("签署进行中");
        assertThat(newest.toStatus()).isEqualTo(3);
        assertThat(newest.toStatusName()).isEqualTo("签署进行中");
        assertThat(newest.basis()).isEqualTo("甲方已线下用印");
        assertThat(newest.operatorId()).isEqualTo("700100");
        assertThat(newest.operatorName()).isEqualTo("运营·小刘");
        assertThat(newest.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0, 0));
        var advanceToSigning = detail.transitions().get(1);
        assertThat(advanceToSigning.action()).isEqualTo(1);
        assertThat(advanceToSigning.actionName()).isEqualTo("推进");
        assertThat(advanceToSigning.fromStatus()).isEqualTo(2);
        assertThat(advanceToSigning.toStatus()).isEqualTo(3);
        var advanceToReview = detail.transitions().get(2);
        assertThat(advanceToReview.fromStatus()).isEqualTo(1);
        assertThat(advanceToReview.toStatus()).isEqualTo(2);
        assertThat(advanceToReview.operatorId()).isEqualTo("700200");
        assertThat(advanceToReview.operatorName()).isEqualTo("商务·小王");
    }

    @Test
    void given_ord020ErrorEnvelope_when_getDetail_then_passThroughWithoutMapping() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                404, ORD_020_ERROR_ENVELOPE, wireUrl);

        assertThatThrownBy(() -> appService.getDetail("3829493999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5020);
                    assertThat(upstream.getMessage()).isEqualTo("合同不存在");
                });
    }

    // ========== 创建重建（无请求体＋准备中起步回执＋前置门错误族）==========

    @Test
    void given_createForOrder_when_create_then_wireMirrorsProviderAndPreparingReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_CREATE_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.createForOrder("3829492001234567");

        // 出站路径逐字镜像 provider backoffice 路由（重建口挂在 orders/{orderId} 下——非 /{id}）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/orders/3829492001234567");
        // 无请求体（provider 端只读路径参数——冻结集取现值由 provider 落，同重试归档先例）
        assertThat(wireBody[0]).isNull();

        // 准备中起步回执：冻结集取现值（范围快照三段＋金额＋双版本引用）
        assertThat(receipt.id()).isEqualTo("3829493007777777");
        assertThat(receipt.contractNo()).isEqualTo("SG-l5h6zy2g3x");
        assertThat(receipt.orderId()).isEqualTo("3829492001234567");
        assertThat(receipt.status()).isEqualTo(1);
        assertThat(receipt.statusName()).isEqualTo("准备中");
        assertThat(receipt.version()).isEqualTo(1);
        assertThat(receipt.needsUpdate()).isFalse();
        // 四个后台录入预览字段 null 在场（全局 Jackson 含 null）——BFF 不代填空串
        assertThat(receipt.partyA()).isNull();
        assertThat(receipt.partyB()).isNull();
        assertThat(receipt.projectScope()).isNull();
        assertThat(receipt.paymentPlan()).isNull();
        assertThat(receipt.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序");
        assertThat(receipt.scopeAcceptance()).isEqualTo("验收方式：按 PRD v3 验收");
        assertThat(receipt.scopeTimeline()).isEqualTo("工期口径：4 周");
        assertThat(receipt.amount()).isEqualTo(128000L);
        assertThat(receipt.currency()).isEqualTo("CNY");
        assertThat(receipt.prdVersionNo()).isEqualTo(3);
        assertThat(receipt.demoVersionNo()).isEqualTo(2);
        // 双方签署位未落（false/null）——签署标记是独立动作
        assertThat(receipt.partyASigned()).isFalse();
        assertThat(receipt.partyASignedAt()).isNull();
        assertThat(receipt.partyBSigned()).isFalse();
        assertThat(receipt.partyBSignedAt()).isNull();
        assertThat(receipt.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 23, 9, 0, 0));
    }

    @Test
    void given_ord025ErrorEnvelope_when_createForOrder_then_gateErrorPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_025_ERROR_ENVELOPE, new String[1]);

        // 前置门①（报价先在）：ORD_025＝域码 5×1000＋25——BFF 不预判不拦截，provider 裁决
        assertThatThrownBy(() -> appService.createForOrder("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5025);
                    assertThat(upstream.getMessage()).isEqualTo("尚无报价，无法确认范围（后台落定报价后再确认）");
                });
    }

    @Test
    void given_ord026ErrorEnvelope_when_createForOrder_then_gateErrorPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_026_ERROR_ENVELOPE, new String[1]);

        // 前置门②（范围文本先录）：ORD_026＝域码 5×1000＋26——scope 录入端点归 A5（#108），
        // 本票零推导零校验，守卫全在 provider
        assertThatThrownBy(() -> appService.createForOrder("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5026);
                    assertThat(upstream.getMessage()).isEqualTo("范围文本未录入（后台录入交付清单/验收方式/工期口径后再确认）");
                });
    }

    @Test
    void given_ord024ErrorEnvelope_when_createForOrder_then_activeContractGuardPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_024_ERROR_ENVELOPE, new String[1]);

        // 前置门③（活跃唯一——撤回/作废后才能重建）：ORD_024＝域码 5×1000＋24，
        // 并发撞部分唯一索引同译（provider 预检或索引兜底）——BFF 不复算活跃判定
        assertThatThrownBy(() -> appService.createForOrder("3829492001234567"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5024);
                    assertThat(upstream.getMessage()).isEqualTo("该订单已有活跃合同（至多一份非终态合同）");
                });
    }

    // ========== 六态推进写口（#105 A2）· wire URL/命令体镜像 + 动作后回执绑定 ==========

    /**
     * aiplatform POST /api/backoffice/contracts/{id}/advance 的真实成功响应形状（推进回执＝
     * ContractResponse，<strong>签署完成终态</strong>——provider {@code ContractAppServiceTest}
     * 全链实值：准备中→待核对与签署→签署进行中→甲乙双方落签→advance 推进至 4）。双方签署位
     * true＋落签时点俱全；advance 不动版本（version 1——版本递增归 record）；推进到签署完成
     * 即触发 provider 侧开工判定（#338 双门禁之合同腿，BFF 不感知）。
     */
    private static final String CONTRACT_ADVANCE_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493004444444",
                "contractNo": "SG-l5d3vt0h4b",
                "orderId": "3829492007777777",
                "status": 4,
                "statusName": "签署完成",
                "version": 1,
                "needsUpdate": false,
                "partyA": "语义引力（上海）科技有限公司",
                "partyB": "文野",
                "projectScope": "对公官网建设：企业门户＋产品展示",
                "paymentPlan": "签约后 3 日内付 50%，验收后付 50%",
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "128000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": true,
                "partyASignedAt": "2026-10-20T10:00:00",
                "partyBSigned": true,
                "partyBSignedAt": "2026-10-21T09:05:00",
                "createdAt": "2026-10-18T09:30:00"
              },
              "requestId": "req-c8d9e0",
              "errors": null
            }
            """;

    @Test
    void given_advanceCommand_when_advance_then_wireMirrorsProviderAndSignedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_ADVANCE_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.advance(CONTRACT_ID,
                new AiplatformContractActionCommand("双方落签，合同完成"));

        // 出站路径逐字镜像 provider backoffice 路由（推进口挂 /{id}/advance）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/advance");
        // 命令体逐字镜像 {basis}（依据必填——操作者不在 body，经透传头落痕）
        assertThat(wireBody[0]).isEqualTo("{\"basis\":\"双方落签，合同完成\"}");

        // 签署完成回执：末边双方均签守卫（ORD_027）在 provider 过——双方签署位 true＋时点俱全
        assertThat(receipt.status()).isEqualTo(4);
        assertThat(receipt.statusName()).isEqualTo("签署完成");
        // advance 不动版本（version 递增归 record 草稿重录）；终态 needsUpdate 恒 false
        assertThat(receipt.version()).isEqualTo(1);
        assertThat(receipt.needsUpdate()).isFalse();
        assertThat(receipt.partyASigned()).isTrue();
        assertThat(receipt.partyASignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0, 0));
        assertThat(receipt.partyBSigned()).isTrue();
        assertThat(receipt.partyBSignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 9, 5, 0));
        assertThat(receipt.contractNo()).isEqualTo("SG-l5d3vt0h4b");
        assertThat(receipt.amount()).isEqualTo(128000L);
    }

    /** fail 回执（旁落终态①：签署失败或过期）——自待核对与签署/签署进行中可达，其余态 ORD_021。 */
    private static final String CONTRACT_FAIL_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493004444444",
                "contractNo": "SG-l5d3vt0h4b",
                "orderId": "3829492007777777",
                "status": 5,
                "statusName": "签署失败或过期",
                "version": 1,
                "needsUpdate": false,
                "partyA": "语义引力（上海）科技有限公司",
                "partyB": "文野",
                "projectScope": "对公官网建设：企业门户＋产品展示",
                "paymentPlan": "签约后 3 日内付 50%，验收后付 50%",
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "128000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": true,
                "partyASignedAt": "2026-10-20T10:00:00",
                "partyBSigned": false,
                "partyBSignedAt": null,
                "createdAt": "2026-10-18T09:30:00"
              },
              "requestId": "req-c9e0f1",
              "errors": null
            }
            """;

    /** withdraw/void 回执共形（旁落终态②：已撤回或作废——同守卫同去向，区别只在留痕行 action）。 */
    private static final String CONTRACT_TERMINAL_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493004444444",
                "contractNo": "SG-l5d3vt0h4b",
                "orderId": "3829492007777777",
                "status": 6,
                "statusName": "已撤回或作废",
                "version": 1,
                "needsUpdate": false,
                "partyA": "语义引力（上海）科技有限公司",
                "partyB": "文野",
                "projectScope": "对公官网建设：企业门户＋产品展示",
                "paymentPlan": "签约后 3 日内付 50%，验收后付 50%",
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "128000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": false,
                "partyASignedAt": null,
                "partyBSigned": false,
                "partyBSignedAt": null,
                "createdAt": "2026-10-18T09:30:00"
              },
              "requestId": "req-ca0f2",
              "errors": null
            }
            """;

    @Test
    void given_failCommand_when_fail_then_wireMirrorsProviderAndFailedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_FAIL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.fail(CONTRACT_ID,
                new AiplatformContractActionCommand("超期未签，如实旁落失败终态"));

        // 出站路径逐字镜像（fail 口挂 /{id}/fail）；命令体同 advance 共形 {basis}
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/fail");
        assertThat(wireBody[0]).isEqualTo("{\"basis\":\"超期未签，如实旁落失败终态\"}");

        // 签署失败或过期终态回执：status 5；单方已签的签署位如实保留（终态定格冻结事实）
        assertThat(receipt.status()).isEqualTo(5);
        assertThat(receipt.statusName()).isEqualTo("签署失败或过期");
        assertThat(receipt.needsUpdate()).isFalse();
        assertThat(receipt.partyASigned()).isTrue();
        assertThat(receipt.partyBSigned()).isFalse();
    }

    @Test
    void given_withdrawCommand_when_withdraw_then_wireMirrorsProviderAndWithdrawnReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_TERMINAL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.withdraw(CONTRACT_ID,
                new AiplatformContractActionCommand("范围录错，整案回炉"));

        // 撤回：任何非终态可达（provider 裁决）；此后换新号重建（POST contracts/orders/{orderId}）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/withdraw");
        assertThat(wireBody[0]).isEqualTo("{\"basis\":\"范围录错，整案回炉\"}");
        assertThat(receipt.status()).isEqualTo(6);
        assertThat(receipt.statusName()).isEqualTo("已撤回或作废");
    }

    @Test
    void given_voidCommand_when_void_then_wireMirrorsProviderAndVoidedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_TERMINAL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.voidContract(CONTRACT_ID,
                new AiplatformContractActionCommand("主体信息录错，作废重建"));

        // 作废：守卫与去向同撤回（同一终态 6）、留痕 action 分立可审计——回执形态与 withdraw 共形，
        // 区别只在 provider 留痕行（本回执不可见，详情时间线 action 2 撤回 / 3 作废）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/void");
        assertThat(wireBody[0]).isEqualTo("{\"basis\":\"主体信息录错，作废重建\"}");
        assertThat(receipt.status()).isEqualTo(6);
        assertThat(receipt.statusName()).isEqualTo("已撤回或作废");
    }

    /**
     * aiplatform POST /api/backoffice/contracts/{id}/sign 的真实成功响应形状（签署标记回执）：
     * 乙方落签（party=2）——仅签署进行中态可达，落一方签署位＋时点，<strong>状态不迁移</strong>
     * （「签署完成」由 advance 显式推进）。甲方先落（true＋时点）保留，乙方翻 true＋时点落定。
     */
    private static final String CONTRACT_SIGN_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493004444444",
                "contractNo": "SG-l5d3vt0h4b",
                "orderId": "3829492007777777",
                "status": 3,
                "statusName": "签署进行中",
                "version": 1,
                "needsUpdate": false,
                "partyA": "语义引力（上海）科技有限公司",
                "partyB": "文野",
                "projectScope": "对公官网建设：企业门户＋产品展示",
                "paymentPlan": "签约后 3 日内付 50%，验收后付 50%",
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "128000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": true,
                "partyASignedAt": "2026-10-20T10:00:00",
                "partyBSigned": true,
                "partyBSignedAt": "2026-10-21T09:05:00",
                "createdAt": "2026-10-18T09:30:00"
              },
              "requestId": "req-cb1a3",
              "errors": null
            }
            """;

    @Test
    void given_signCommand_when_sign_then_wireMirrorsProviderAndPartyReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_SIGN_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.sign(CONTRACT_ID,
                new AiplatformContractSignCommand(2, "乙方盖章"));

        // 出站路径逐字镜像（sign 口挂 /{id}/sign）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/sign");
        // 命令体逐字镜像 {party, basis}：party Integer code 出 JSON number（provider BaseEnum
        // 反序列化按 code 寻址——1=甲方 2=乙方；admin 不建 typed enum 不预校验，非法值归 provider 400）
        assertThat(wireBody[0]).isEqualTo("{\"party\":2,\"basis\":\"乙方盖章\"}");

        // 乙方落签回执：签署位翻 true＋时点落定；状态仍签署进行中（签署标记不自动迁移——
        // 「签署完成」由 advance 显式推进，双方均签守卫 ORD_027 在 provider）
        assertThat(receipt.status()).isEqualTo(3);
        assertThat(receipt.statusName()).isEqualTo("签署进行中");
        assertThat(receipt.partyASigned()).isTrue();
        assertThat(receipt.partyBSigned()).isTrue();
        assertThat(receipt.partyBSignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 9, 5, 0));
    }

    /**
     * aiplatform POST /api/backoffice/contracts/{id}/record 的真实成功响应形状（草稿重录回执）：
     * 「需更新」处置正路（provider {@code ContractAppServiceTest} 实值——改价 99000 后重录）：
     * 预览字段集整组录入（张三/语义引力科技有限公司/管理后台＋小程序/开工款 50%，验收 50%）
     * ＋冻结集取订单现值重新定格（amount 翻 99000）＋<strong>version +1（1→2）</strong>＋
     * <strong>「需更新」清零</strong>＋同号重录（contractNo 不变）＋状态不迁移（仍准备中）。
     */
    private static final String CONTRACT_RECORD_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829493006666666",
                "contractNo": "SG-l5f2xq9wn1",
                "orderId": "3829492001234567",
                "status": 1,
                "statusName": "准备中",
                "version": 2,
                "needsUpdate": false,
                "partyA": "张三",
                "partyB": "语义引力科技有限公司",
                "projectScope": "管理后台＋小程序",
                "paymentPlan": "开工款 50%，验收 50%",
                "scopeDeliverables": "交付清单：管理后台＋小程序",
                "scopeAcceptance": "验收方式：按 PRD v3 验收",
                "scopeTimeline": "工期口径：4 周",
                "amount": "99000",
                "currency": "CNY",
                "prdVersionNo": 3,
                "demoVersionNo": 2,
                "partyASigned": false,
                "partyASignedAt": null,
                "partyBSigned": false,
                "partyBSignedAt": null,
                "createdAt": "2026-10-22T09:00:00"
              },
              "requestId": "req-cc2b4",
              "errors": null
            }
            """;

    @Test
    void given_recordCommand_when_record_then_wireMirrorsProviderAndVersionBumpedReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.contractWriteAppServiceWithStubTransport(
                CONTRACT_RECORD_RECEIPT_ENVELOPE, wireUrl, wireBody);

        AiplatformContractResponse receipt = appService.record(CONTRACT_ID,
                new AiplatformContractRecordCommand("张三", "语义引力科技有限公司",
                        "管理后台＋小程序", "开工款 50%，验收 50%", "改价后重录"));

        // 出站路径逐字镜像（record 口挂 /{id}/record）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/contracts/" + CONTRACT_ID + "/record");
        // 命令体逐字镜像五字段（四个预览字段可空整组录入＋依据必填——provider RecordContractCommand 同序）
        assertThat(wireBody[0]).isEqualTo("{\"partyA\":\"张三\",\"partyB\":\"语义引力科技有限公司\","
                + "\"projectScope\":\"管理后台＋小程序\",\"paymentPlan\":\"开工款 50%，验收 50%\","
                + "\"basis\":\"改价后重录\"}");

        // 重录回执（provider 实值）：version +1（1→2）＋「需更新」清零（重冻即清零）＋
        // 冻结集取现值（改价 99000 翻新）＋预览字段整组录入＋同号重录＋状态不迁移（仍准备中）
        assertThat(receipt.version()).isEqualTo(2);
        assertThat(receipt.needsUpdate()).isFalse();
        assertThat(receipt.amount()).isEqualTo(99000L);
        assertThat(receipt.partyA()).isEqualTo("张三");
        assertThat(receipt.partyB()).isEqualTo("语义引力科技有限公司");
        assertThat(receipt.projectScope()).isEqualTo("管理后台＋小程序");
        assertThat(receipt.paymentPlan()).isEqualTo("开工款 50%，验收 50%");
        assertThat(receipt.status()).isEqualTo(1);
        assertThat(receipt.statusName()).isEqualTo("准备中");
        assertThat(receipt.contractNo()).isEqualTo("SG-l5f2xq9wn1");
    }

    // ========== 六态推进守卫错误族（provider 裁决原样透传——BFF 零状态机复算） ==========

    /** 依据必填守卫：HTTP 400 + 信封 code=5022（ORD_022——留痕行正身缺失，provider 聚合守卫）。 */
    private static final String ORD_022_ERROR_ENVELOPE = """
            {
              "code": 5022,
              "message": "操作依据必填（人工推进须填写依据留痕）",
              "data": null,
              "requestId": "req-cd3c5",
              "errors": null
            }
            """;

    /** 双方均签守卫（advance 3→4 末边）：HTTP 409 + 信封 code=5027（ORD_027）。 */
    private static final String ORD_027_ERROR_ENVELOPE = """
            {
              "code": 5027,
              "message": "双方尚未全部签署，不能标记签署完成",
              "data": null,
              "requestId": "req-ce4d6",
              "errors": null
            }
            """;

    /** 签署位幂等守卫（sign 已落签的签署位再落）：HTTP 409 + 信封 code=5028（ORD_028）。 */
    private static final String ORD_028_ERROR_ENVELOPE = """
            {
              "code": 5028,
              "message": "该方已签署（签署位已落）",
              "data": null,
              "requestId": "req-cf5e7",
              "errors": null
            }
            """;

    /** 草稿重录窗口守卫（record 签署进行中起）：HTTP 409 + 信封 code=5031（ORD_031）。 */
    private static final String ORD_031_ERROR_ENVELOPE = """
            {
              "code": 5031,
              "message": "内容重录仅限草稿态（签署进行中起走撤回/作废重建）",
              "data": null,
              "requestId": "req-d0a1b8",
              "errors": null
            }
            """;

    @Test
    void given_ord022ErrorEnvelope_when_advance_then_basisRequiredGuardPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                400, ORD_022_ERROR_ENVELOPE, new String[1]);

        // 依据必填（六动作共用守卫）：ORD_022＝域码 5×1000＋22——BFF 不前置拦截，provider 裁决
        assertThatThrownBy(() -> appService.advance(CONTRACT_ID,
                new AiplatformContractActionCommand(" ")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(5022);
                    assertThat(upstream.getMessage()).isEqualTo("操作依据必填（人工推进须填写依据留痕）");
                });
    }

    @Test
    void given_ord027ErrorEnvelope_when_advance_then_bothSignedGuardPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_027_ERROR_ENVELOPE, new String[1]);

        // 双方均签守卫（advance 末边 3→4）：ORD_027＝域码 5×1000＋27——签署位状态归 provider，
        // BFF 不读签署位不预判可推进性
        assertThatThrownBy(() -> appService.advance(CONTRACT_ID,
                new AiplatformContractActionCommand("乙方未签就想完成")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5027);
                    assertThat(upstream.getMessage()).isEqualTo("双方尚未全部签署，不能标记签署完成");
                });
    }

    @Test
    void given_ord028ErrorEnvelope_when_sign_then_partyGuardPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_028_ERROR_ENVELOPE, new String[1]);

        // 签署位幂等守卫（sign）：ORD_028＝域码 5×1000＋28——已落签的签署位不可重复落
        assertThatThrownBy(() -> appService.sign(CONTRACT_ID,
                new AiplatformContractSignCommand(1, "甲方重复盖章")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5028);
                    assertThat(upstream.getMessage()).isEqualTo("该方已签署（签署位已落）");
                });
    }

    @Test
    void given_ord031ErrorEnvelope_when_record_then_draftWindowGuardPassedThrough() {
        var appService = AiplatformWireTestSupport.contractAppServiceWithErrorTransport(
                409, ORD_031_ERROR_ENVELOPE, new String[1]);

        // 草稿重录窗口守卫（record）：ORD_031＝域码 5×1000＋31——签署进行中起变动走撤回/作废重建
        assertThatThrownBy(() -> appService.record(CONTRACT_ID,
                new AiplatformContractRecordCommand(null, null, null, null, "签署中重录")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5031);
                    assertThat(upstream.getMessage()).isEqualTo("内容重录仅限草稿态（签署进行中起走撤回/作废重建）");
                });
    }
}
