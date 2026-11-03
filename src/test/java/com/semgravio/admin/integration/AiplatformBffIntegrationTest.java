package com.semgravio.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.http.HttpHeaders;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.semgravio.admin.aiplatform.application.AiplatformAccountAppService;
import com.semgravio.admin.aiplatform.application.AiplatformContractAppService;
import com.semgravio.admin.aiplatform.application.AiplatformCostAppService;
import com.semgravio.admin.aiplatform.application.AiplatformMaterialAppService;
import com.semgravio.admin.aiplatform.application.AiplatformOrderAppService;
import com.semgravio.admin.aiplatform.application.AiplatformPaymentAppService;
import com.semgravio.admin.aiplatform.application.AiplatformProductionAppService;
import com.semgravio.admin.aiplatform.application.AiplatformPriceEntryAppService;
import com.semgravio.admin.aiplatform.application.AiplatformProjectAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.AiplatformWorkspaceAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentNodesCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionSupplementCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentVerifyCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractRecordCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformContractSignCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformOrderCancelCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformOrderQuoteCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformOrderScopeCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPriceEntryRepriceCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformCostQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformContractQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformMaterialQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformOrderQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformPriceEntryQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformProjectQuery;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformWorkspaceQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAccountProfileResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformContractSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformCostOverviewResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformMaterialDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformMaterialSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformOrderResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformOrderSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProjectCostDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProjectCostResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformUnitPriceEntryRepriceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformUnitPriceEntryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformUnpricedUsageResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProjectDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProjectSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformWorkspaceDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformWorkspaceSummaryResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAccountProfileWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformCostOverviewWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformCostWindowWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformConversationEntryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformMaterialDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformMaterialListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformMaterialSummaryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractActionWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractRecordWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractSignWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractSummaryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractTransitionWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformContractWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentNodeWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentNodesWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentPlanDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentPlanWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentRejectWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVerificationWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVerifyWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVoucherWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionActionWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionHandoffWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionSupplementWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionUpdateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderBriefWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderCancelWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderQuoteWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderScopeWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderSummaryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformOrderWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPriceEntryListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPriceEntryRepriceWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTokenUsageWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryRepriceWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformUnpricedUsageWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPrdWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectCostDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectCostWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectFileContentWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectFilesWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProjectSummaryWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformVersionDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformVersionWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformWorkspaceDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformWorkspaceListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformWorkspaceSummaryWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import com.cartisan.openapi.client.BinaryResponse;
import com.cartisan.openapi.client.OpenApiClientException;
import com.cartisan.web.response.PageResponse;

/**
 * aiplatform BFF 集成测试（issue #63 T1 账号首批 + #64 订单读路径 + #65 项目核心读路径 +
 * #66 沙箱观测与四干预动作 + #67 成本四读口 + #68 单价表清单/原子改价/停用 + #69 知识素材
 * 清单/详情/停用⇄启用/删除 + #70 订单写路径报价/改价/取消/重试归档 + #71 项目文件区
 * 文件树/文件内容/文件包 + #104 合同域清单/详情/创建重建 + #105 合同域六态推进写口
 * advance/fail/withdraw/void/sign/record + #106 付款核验域计划详情/节点录入/核验/打回/
 * 凭证二进制 + #107 生产承接域详情/整组更新/推进/需用户补充）——mock {@link AiplatformClient}，
 * 验证 AppService 在 Spring 上下文
 * 中的完整接线（DI、query→wire 映射、wire→response DTO 映射、分页 1-based 透传、二进制载体
 * 保全、下游错误透传不映射）。
 *
 * <p>镜像 {@code AccountBffIntegrationTest} / {@code PaymentBffIntegrationTest}。不模拟安全层
 * （权限在 {@code AiplatformRbacEnforcementIntegrationTest} 覆盖）；不直测 {@link AiplatformClient}
 * （wire 反序列化契约在 {@code AiplatformAccountClientContractTest} /
 * {@code AiplatformOrderClientContractTest} 以真实传输 stub 钉死，方法边界 mock 会绕过
 * 反序列化——已知反例）。</p>
 *
 * <p>mock 数据按 aiplatform 真实响应形状构造（{@code id} 为 String TSID 十进制串、
 * {@code status} Integer code + {@code statusName}、金额 Long 分、时间 ISO）。错误路径断言
 * <strong>透传</strong>（{@link AiplatformUpstreamException} 携带 provider 的 HTTP 状态 +
 * 数字业务码 + message），而非 payment/identity 式的 {@code BaseCodeMessage} 映射
 * （spec #62 定稿：aiplatform 不做映射翻译）。</p>
 *
 * @since 0.1.0
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiplatformBffIntegrationTest {

    @Autowired
    private AiplatformAccountAppService accountAppService;

    @Autowired
    private AiplatformOrderAppService orderAppService;

    @Autowired
    private AiplatformProjectAppService projectAppService;

    @Autowired
    private AiplatformWorkspaceAppService workspaceAppService;

    @Autowired
    private AiplatformCostAppService costAppService;

    @Autowired
    private AiplatformPriceEntryAppService priceEntryAppService;

    @Autowired
    private AiplatformMaterialAppService materialAppService;

    @Autowired
    private AiplatformContractAppService contractAppService;

    @Autowired
    private AiplatformPaymentAppService paymentAppService;

    @Autowired
    private AiplatformProductionAppService productionAppService;

    @MockBean
    private AiplatformClient aiplatformClient;

    // ========== 账号档案 · DTO 映射 ==========

    @Test
    void given_accountProfile_when_get_then_returnMappedProfile() {
        when(aiplatformClient.getAccountProfile("auth0|65f2c8a1")).thenReturn(
                new AiplatformAccountProfileWireResponse(
                        "3829492001234567", "auth0|65f2c8a1", "文野",
                        LocalDateTime.of(2026, 1, 15, 10, 30, 0)));

        AiplatformAccountProfileResponse profile = accountAppService.getAccountProfile("auth0|65f2c8a1");

        // DTO 映射：wire → response 逐字段（逐字镜像——无增删字段、无换型）
        assertThat(profile.id()).isEqualTo("3829492001234567");           // String（TSID 十进制串）
        assertThat(profile.externalId()).isEqualTo("auth0|65f2c8a1");
        assertThat(profile.displayName()).isEqualTo("文野");
        assertThat(profile.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30, 0));
    }

    @Test
    void given_wireTranslationThrowsUpstream_when_get_then_propagateWithoutMapping() {
        // 错误信封已由 AiplatformClient 翻译为透传异常（IDN_004→HTTP 404 + 数字业务码 6004），
        // AppService 不再二次翻译——原样上抛，由北向 advice 还原 provider 信封
        when(aiplatformClient.getAccountProfile("auth0|unknown")).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":6004,\"message\":\"账号不存在\",\"data\":null}")));

        assertThatThrownBy(() -> accountAppService.getAccountProfile("auth0|unknown"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(6004);
                    assertThat(upstream.getMessage()).isEqualTo("账号不存在");
                });
    }

    // ========== 订单清单 · query→wire 映射 + 分页 1-based 透传 ==========

    @Test
    void given_orderQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        when(aiplatformClient.listOrders(any(), eq(3), eq(20))).thenReturn(
                new PageResponse<>(List.of(
                        new AiplatformOrderSummaryWireResponse(
                                "3829492001234567", "3829492007654321", "英语学习助手", "auth0|65f2c8a1", "文野",
                                3, "已支付", 3, "系统＋设计", 1999000L, "CNY",
                                LocalDateTime.of(2026, 9, 10, 14, 20, 0),
                                LocalDateTime.of(2026, 9, 11, 9, 0, 0))), 42, 3, 20));

        PageResponse<AiplatformOrderSummaryResponse> page = orderAppService.list(
                new AiplatformOrderQuery(List.of(1, 5),
                        LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                        LocalDateTime.of(2026, 9, 15, 23, 59, 59),
                        "auth0|65f2c8a1", "3829492001234567"), 3, 20);

        // 出站参数：北向 query → wire 过滤记录逐字段映射；page 1-based 直传（3→3，无 ±1）
        ArgumentCaptor<AiplatformOrderListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformOrderListWireRequest.class);
        verify(aiplatformClient).listOrders(wireCaptor.capture(), eq(3), eq(20));
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformOrderListWireRequest(
                List.of(1, 5),
                LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                LocalDateTime.of(2026, 9, 15, 23, 59, 59),
                "auth0|65f2c8a1", "3829492001234567"));

        // 回显取 provider 回报值（含 clamp 后值）——不是北向入参回声；wire → response 逐字段
        assertThat(page.total()).isEqualTo(42L);
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);
        var row = page.items().get(0);
        assertThat(row.id()).isEqualTo("3829492001234567");
        assertThat(row.status()).isEqualTo(3);
        assertThat(row.statusName()).isEqualTo("已支付");
        // 交付物类型字段对（#96）：wire → response 逐字段（Integer code + 中文名）
        assertThat(row.deliverableType()).isEqualTo(3);
        assertThat(row.deliverableTypeName()).isEqualTo("系统＋设计");
        assertThat(row.amount()).isEqualTo(1999000L);
        assertThat(row.currency()).isEqualTo("CNY");
        assertThat(row.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 20, 0));
    }

    // ========== 订单详情错误 · 透传不映射 ==========

    @Test
    void given_ord001FromDownstream_when_getDetail_then_propagateWithoutMapping() {
        when(aiplatformClient.getOrder("3829499999999999")).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":5001,\"message\":\"订单不存在\",\"data\":null}")));

        assertThatThrownBy(() -> orderAppService.getDetail("3829499999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(5001);
                    assertThat(upstream.getMessage()).isEqualTo("订单不存在");
                });
    }

    // ========== 源码包 · 二进制载体保全 ==========

    @Test
    void given_tarGzFromDownstream_when_getSourcePackage_then_bytesAndRawHeadersPreserved() {
        byte[] tarGz = {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00};
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/gzip"),
                "Content-Disposition", List.of("attachment; filename=\"3829492001234567-source.tar.gz\"")),
                (a, b) -> true);
        when(aiplatformClient.downloadSourcePackage("3829492001234567"))
                .thenReturn(new BinaryResponse(200, providerHeaders, tarGz));

        var pkg = orderAppService.getSourcePackage("3829492001234567");

        // 字节逐位保全 + provider 响应头 raw 值透传（application/gzip + attachment 文件名）
        assertThat(pkg.content()).containsExactly(tarGz);
        assertThat(pkg.contentType()).isEqualTo("application/gzip");
        assertThat(pkg.contentDisposition()).isEqualTo("attachment; filename=\"3829492001234567-source.tar.gz\"");
    }

    // ========== 设计资产包 · 二进制载体保全（#96，对偶源码包）==========

    @Test
    void given_tarGzFromDownstream_when_getDesignPackage_then_bytesAndRawHeadersPreserved() {
        byte[] tarGz = {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00};
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/gzip"),
                "Content-Disposition", List.of("attachment; filename=\"3829492001234567-design.tar.gz\"")),
                (a, b) -> true);
        when(aiplatformClient.downloadDesignPackage("3829492001234567"))
                .thenReturn(new BinaryResponse(200, providerHeaders, tarGz));

        var pkg = orderAppService.getDesignPackage("3829492001234567");

        // 字节逐位保全 + provider 响应头 raw 值透传（design 后缀区别于 source——下单冻结件直取）
        assertThat(pkg.content()).containsExactly(tarGz);
        assertThat(pkg.contentType()).isEqualTo("application/gzip");
        assertThat(pkg.contentDisposition()).isEqualTo("attachment; filename=\"3829492001234567-design.tar.gz\"");
    }

    // ========== 写路径三操作 · 命令映射 + 用户面同构回执映射 ==========

    @Test
    void given_quoteReceipt_when_quote_then_wireCommandMappedAndReceiptMapped() {
        when(aiplatformClient.quoteOrder(eq("3829492001234567"), any())).thenReturn(orderReceiptWire(
                2, "已报价", null, null, null, List.of(
                        new AiplatformOrderWireResponse.PriceEntry(
                                "3829492011111111", 2199000L, "CNY", "含加急费用",
                                LocalDateTime.of(2026, 9, 12, 10, 0, 0)),
                        new AiplatformOrderWireResponse.PriceEntry(
                                "3829492009999999", 1999000L, "CNY", "首次报价",
                                LocalDateTime.of(2026, 9, 11, 9, 0, 0)))));

        AiplatformOrderResponse receipt = orderAppService.quote("3829492001234567",
                new AiplatformOrderQuoteCommand(2199000L, "含加急费用"));

        // 出站参数：北向命令 → wire 命令体逐字段映射（Long 分/null note 原值，不代填）
        ArgumentCaptor<AiplatformOrderQuoteWireRequest> commandCaptor =
                ArgumentCaptor.forClass(AiplatformOrderQuoteWireRequest.class);
        verify(aiplatformClient).quoteOrder(eq("3829492001234567"), commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .isEqualTo(new AiplatformOrderQuoteWireRequest(2199000L, "含加急费用"));

        // wire → response 逐字段：现值取最新价目行；quotedAt 仍是首次报价时点（改价不刷新）；
        // 价目历史五字段（无操作者留痕——用户面同构回执）
        assertThat(receipt.status()).isEqualTo(2);
        assertThat(receipt.statusName()).isEqualTo("已报价");
        // 交付物类型字段对（#96）：写回执 OrderResponse 同步镜像
        assertThat(receipt.deliverableType()).isEqualTo(3);
        assertThat(receipt.deliverableTypeName()).isEqualTo("系统＋设计");
        // 双版本引用＋范围文本三段（#108：provider #335/#336——OrderResponse 20 字段镜像）
        assertThat(receipt.prdVersionNo()).isEqualTo(3);
        assertThat(receipt.demoVersionNo()).isEqualTo(2);
        assertThat(receipt.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序（含 5 个核心页面）");
        assertThat(receipt.scopeAcceptance()).isEqualTo("验收方式：按 PRD v3 逐项验收");
        assertThat(receipt.scopeTimeline()).isEqualTo("工期口径：4 周（含 1 周联调）");
        assertThat(receipt.amount()).isEqualTo(2199000L);
        assertThat(receipt.note()).isEqualTo("含加急费用");
        assertThat(receipt.quotedAt()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0, 0));
        assertThat(receipt.priceEntries()).hasSize(2);
        assertThat(receipt.priceEntries().get(0).id()).isEqualTo("3829492011111111");
        assertThat(receipt.priceEntries().get(0).amount()).isEqualTo(2199000L);
        assertThat(receipt.priceEntries().get(1).note()).isEqualTo("首次报价");
        assertThat(receipt.cancelledAt()).isNull();
        assertThat(receipt.paidAt()).isNull();
        assertThat(receipt.archivedAt()).isNull();
    }

    @Test
    void given_cancelReceipt_when_cancel_then_wireCommandMappedAndCancelledReceiptMapped() {
        when(aiplatformClient.cancelOrder(eq("3829492001234567"), any())).thenReturn(orderReceiptWire(
                5, "已取消", LocalDateTime.of(2026, 9, 16, 15, 0, 0), null, null, List.of(
                        new AiplatformOrderWireResponse.PriceEntry(
                                "3829492011111111", 2199000L, "CNY", "含加急费用",
                                LocalDateTime.of(2026, 9, 12, 10, 0, 0)))));

        AiplatformOrderResponse receipt = orderAppService.cancel("3829492001234567",
                new AiplatformOrderCancelCommand("重复下单，用户要求取消"));

        // 出站参数：北向命令 → wire 命令体逐字段映射（reason 原值）
        ArgumentCaptor<AiplatformOrderCancelWireRequest> commandCaptor =
                ArgumentCaptor.forClass(AiplatformOrderCancelWireRequest.class);
        verify(aiplatformClient).cancelOrder(eq("3829492001234567"), commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .isEqualTo(new AiplatformOrderCancelWireRequest("重复下单，用户要求取消"));

        // 已取消终态回执：cancelledAt 落定，报价事实保留
        assertThat(receipt.status()).isEqualTo(5);
        assertThat(receipt.statusName()).isEqualTo("已取消");
        assertThat(receipt.cancelledAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 15, 0, 0));
        assertThat(receipt.amount()).isEqualTo(2199000L);
        assertThat(receipt.priceEntries()).hasSize(1);
    }

    @Test
    void given_archiveReceipt_when_retryArchive_then_archivedReceiptMapped() {
        when(aiplatformClient.retryArchiveOrder("3829492001234567")).thenReturn(orderReceiptWire(
                4, "已归档", null, LocalDateTime.of(2026, 9, 15, 20, 0, 0),
                LocalDateTime.of(2026, 9, 16, 9, 30, 0), List.of()));

        AiplatformOrderResponse receipt = orderAppService.retryArchive("3829492001234567");

        // 已归档终态回执：paidAt/archivedAt 双时点（支付成功在前、补归档在后）；无请求体无命令映射
        assertThat(receipt.status()).isEqualTo(4);
        assertThat(receipt.statusName()).isEqualTo("已归档");
        assertThat(receipt.paidAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 20, 0, 0));
        assertThat(receipt.archivedAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 9, 30, 0));
        assertThat(receipt.cancelledAt()).isNull();
        assertThat(receipt.priceEntries()).isEmpty();
    }

    @Test
    void given_scopeReceipt_when_enterScope_then_wireCommandMappedAndWorkingCopyEchoed() {
        when(aiplatformClient.enterOrderScope(eq("3829492001234567"), any())).thenReturn(orderReceiptWire(
                2, "已报价", null, null, null, List.of(
                        new AiplatformOrderWireResponse.PriceEntry(
                                "3829492011111111", 2199000L, "CNY", "含加急费用",
                                LocalDateTime.of(2026, 9, 12, 10, 0, 0)))));

        AiplatformOrderResponse receipt = orderAppService.enterScope("3829492001234567",
                new AiplatformOrderScopeCommand(
                        "交付清单：管理后台＋小程序（含 5 个核心页面）",
                        "验收方式：按 PRD v3 逐项验收",
                        "工期口径：4 周（含 1 周联调）"));

        // 出站参数：北向命令 → wire 命令体逐字段映射（三段原值——半组/超长归 provider 聚合守卫）
        ArgumentCaptor<AiplatformOrderScopeWireRequest> commandCaptor =
                ArgumentCaptor.forClass(AiplatformOrderScopeWireRequest.class);
        verify(aiplatformClient).enterOrderScope(eq("3829492001234567"), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new AiplatformOrderScopeWireRequest(
                "交付清单：管理后台＋小程序（含 5 个核心页面）",
                "验收方式：按 PRD v3 逐项验收",
                "工期口径：4 周（含 1 周联调）"));

        // wire → response：scope 三段＝工作副本现值回显、双版本引用照携、报价事实不动（工作副本
        // 录入不改金额/价目行/状态）
        assertThat(receipt.status()).isEqualTo(2);
        assertThat(receipt.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序（含 5 个核心页面）");
        assertThat(receipt.scopeAcceptance()).isEqualTo("验收方式：按 PRD v3 逐项验收");
        assertThat(receipt.scopeTimeline()).isEqualTo("工期口径：4 周（含 1 周联调）");
        assertThat(receipt.prdVersionNo()).isEqualTo(3);
        assertThat(receipt.demoVersionNo()).isEqualTo(2);
        assertThat(receipt.amount()).isEqualTo(2199000L);
        assertThat(receipt.priceEntries()).hasSize(1);
    }

    @Test
    void given_ord007FromDownstream_when_quote_then_propagateWithoutMapping() {
        when(aiplatformClient.quoteOrder(eq("3829492001234567"), any())).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":5007,\"message\":\"订单已支付或已终结，无法报价或改价\",\"data\":null}")));

        assertThatThrownBy(() -> orderAppService.quote("3829492001234567",
                new AiplatformOrderQuoteCommand(2199000L, "含加急费用")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // ORD_007 → HTTP 409 + 数字业务码 5007（域码 5×1000＋7）
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5007);
                    assertThat(upstream.getMessage()).isEqualTo("订单已支付或已终结，无法报价或改价");
                });
    }

    /**
     * 四写操作共用回执 wire 工厂：按动作后的终态（status/时点组）参数化，固定订单标识、
     * 报价事实（金额/备注取最新价目行、quotedAt 首次报价时点）、双版本引用（#335：3/2）
     * 与范围文本三段（#336 工作副本现值）。paidAt/cancelledAt/archivedAt 全参数化——
     * 各终态只落自己的时点（改价/取消单未支付 paidAt=null，归档单双时点俱全）。
     */
    private static AiplatformOrderWireResponse orderReceiptWire(
            Integer status, String statusName,
            LocalDateTime cancelledAt, LocalDateTime paidAt, LocalDateTime archivedAt,
            List<AiplatformOrderWireResponse.PriceEntry> priceEntries) {
        return new AiplatformOrderWireResponse(
                "3829492001234567", "3829492007654321", status, statusName,
                3, "系统＋设计", 3, 2,
                2199000L, "CNY", "含加急费用",
                "交付清单：管理后台＋小程序（含 5 个核心页面）",
                "验收方式：按 PRD v3 逐项验收",
                "工期口径：4 周（含 1 周联调）",
                LocalDateTime.of(2026, 9, 11, 9, 0, 0),
                priceEntries, LocalDateTime.of(2026, 9, 10, 14, 20, 0),
                cancelledAt, paidAt, archivedAt);
    }

    // ========== 合同域（#104 A1）· query→wire 映射 + 三段详情映射 + 创建回执映射 ==========

    @Test
    void given_contractQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        when(aiplatformClient.listContracts(any(), eq(3), eq(20))).thenReturn(
                new PageResponse<>(List.of(
                        new AiplatformContractSummaryWireResponse(
                                "3829493004444444", "SG-l5d3vt0h4b", "3829492007777777",
                                3, "签署进行中", 1, 128000L, "CNY",
                                LocalDateTime.of(2026, 10, 18, 9, 30, 0))), 6, 3, 20));

        PageResponse<AiplatformContractSummaryResponse> page = contractAppService.list(
                new AiplatformContractQuery(List.of(3, 6),
                        LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                        LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                        "3829492007777777", "SG-l5d3vt0h4b"), 3, 20);

        // 出站参数：北向 query → wire 过滤记录逐字段映射（第四维＝contractNo，区别于订单的 externalId）；
        // page 1-based 直传（3→3，无 ±1）
        ArgumentCaptor<AiplatformContractListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformContractListWireRequest.class);
        verify(aiplatformClient).listContracts(wireCaptor.capture(), eq(3), eq(20));
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformContractListWireRequest(
                List.of(3, 6),
                LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                "3829492007777777", "SG-l5d3vt0h4b"));

        // 回显取 provider 回报值；wire → response 逐字段（六态 code+中文名/version/金额直通）
        assertThat(page.total()).isEqualTo(6L);
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);
        var row = page.items().get(0);
        assertThat(row.id()).isEqualTo("3829493004444444");
        assertThat(row.contractNo()).isEqualTo("SG-l5d3vt0h4b");
        assertThat(row.orderId()).isEqualTo("3829492007777777");
        assertThat(row.status()).isEqualTo(3);
        assertThat(row.statusName()).isEqualTo("签署进行中");
        assertThat(row.version()).isEqualTo(1);
        assertThat(row.amount()).isEqualTo(128000L);
        assertThat(row.currency()).isEqualTo("CNY");
        assertThat(row.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 18, 9, 30, 0));
    }

    @Test
    void given_contractDetailWire_when_getDetail_then_threeSegmentsMapped() {
        when(aiplatformClient.getContract("3829493004444444")).thenReturn(
                new AiplatformContractDetailWireResponse(
                        new AiplatformContractWireResponse(
                                "3829493004444444", "SG-l5d3vt0h4b", "3829492007777777",
                                3, "签署进行中", 1, false,
                                "语义引力（上海）科技有限公司", "文野",
                                "对公官网建设：企业门户＋产品展示", "签约后 3 日内付 50%，验收后付 50%",
                                "交付清单：管理后台＋小程序", "验收方式：按 PRD v3 验收", "工期口径：4 周",
                                128000L, "CNY", 3, 2,
                                true, LocalDateTime.of(2026, 10, 20, 10, 0, 0),
                                false, null,
                                LocalDateTime.of(2026, 10, 18, 9, 30, 0)),
                        "# PRD v3\n\n已确认版本全文",
                        List.of(new AiplatformContractTransitionWireResponse(
                                4, "签署标记", 3, "签署进行中", 3, "签署进行中",
                                "甲方已线下用印", "700100", "运营·小刘",
                                LocalDateTime.of(2026, 10, 20, 10, 0, 0)))));

        AiplatformContractDetailResponse detail = contractAppService.getDetail("3829493004444444");

        // 三段逐字段映射：contract 全字段（含 needsUpdate/范围快照冻结面/双方签署位）
        var contract = detail.contract();
        assertThat(contract.contractNo()).isEqualTo("SG-l5d3vt0h4b");
        assertThat(contract.status()).isEqualTo(3);
        assertThat(contract.needsUpdate()).isFalse();
        assertThat(contract.partyA()).isEqualTo("语义引力（上海）科技有限公司");
        assertThat(contract.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序");
        assertThat(contract.prdVersionNo()).isEqualTo(3);
        assertThat(contract.demoVersionNo()).isEqualTo(2);
        assertThat(contract.partyASigned()).isTrue();
        assertThat(contract.partyBSignedAt()).isNull();
        // PRD 全文冗余拷贝直通；留痕行逐字段直通（动作/两侧状态/依据/操作者两肢）
        assertThat(detail.prdSnapshot()).isEqualTo("# PRD v3\n\n已确认版本全文");
        var transition = detail.transitions().get(0);
        assertThat(transition.action()).isEqualTo(4);
        assertThat(transition.fromStatus()).isEqualTo(3);
        assertThat(transition.toStatus()).isEqualTo(3);
        assertThat(transition.basis()).isEqualTo("甲方已线下用印");
        assertThat(transition.operatorId()).isEqualTo("700100");
    }

    @Test
    void given_createReceiptWire_when_createForOrder_then_preparingReceiptMapped() {
        when(aiplatformClient.createContractForOrder("3829492007777777")).thenReturn(
                new AiplatformContractWireResponse(
                        "3829493007777777", "SG-l5h6zy2g3x", "3829492007777777",
                        1, "准备中", 1, false,
                        null, null, null, null,
                        "交付清单：管理后台＋小程序", "验收方式：按 PRD v3 验收", "工期口径：4 周",
                        128000L, "CNY", 3, 2,
                        false, null, false, null,
                        LocalDateTime.of(2026, 10, 23, 9, 0, 0)));

        AiplatformContractResponse receipt = contractAppService.createForOrder("3829492007777777");

        // 创建回执映射：准备中起步、四预览字段 null 直通（BFF 不代填）、冻结集已取现值
        assertThat(receipt.status()).isEqualTo(1);
        assertThat(receipt.statusName()).isEqualTo("准备中");
        assertThat(receipt.version()).isEqualTo(1);
        assertThat(receipt.partyA()).isNull();
        assertThat(receipt.paymentPlan()).isNull();
        assertThat(receipt.scopeDeliverables()).isEqualTo("交付清单：管理后台＋小程序");
        assertThat(receipt.amount()).isEqualTo(128000L);
        assertThat(receipt.partyASigned()).isFalse();
    }

    @Test
    void given_ord024FromDownstream_when_createForOrder_then_propagateWithoutMapping() {
        when(aiplatformClient.createContractForOrder("3829492007777777")).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":5024,\"message\":\"该订单已有活跃合同（至多一份非终态合同）\",\"data\":null}")));

        assertThatThrownBy(() -> contractAppService.createForOrder("3829492007777777"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // ORD_024 → HTTP 409 + 数字业务码 5024（域码 5×1000＋24——活跃唯一前置门归 provider）
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5024);
                    assertThat(upstream.getMessage()).isEqualTo("该订单已有活跃合同（至多一份非终态合同）");
                });
    }

    // ========== 合同域六态推进写口（#105 A2）· 命令映射 + 动作后回执映射 + 守卫透传 ==========

    /** 六态推进回执 wire 载荷工厂：按动作后状态参数化（advance 4 签署完成/fail 5/withdraw·void 6）。 */
    private static AiplatformContractWireResponse contractActionReceiptWire(
            Integer status, String statusName, boolean partyASigned, boolean partyBSigned) {
        return new AiplatformContractWireResponse(
                "3829493004444444", "SG-l5d3vt0h4b", "3829492007777777",
                status, statusName, 1, false,
                "语义引力（上海）科技有限公司", "文野",
                "对公官网建设：企业门户＋产品展示", "签约后 3 日内付 50%，验收后付 50%",
                "交付清单：管理后台＋小程序", "验收方式：按 PRD v3 验收", "工期口径：4 周",
                128000L, "CNY", 3, 2,
                partyASigned, partyASigned ? LocalDateTime.of(2026, 10, 20, 10, 0, 0) : null,
                partyBSigned, partyBSigned ? LocalDateTime.of(2026, 10, 21, 9, 5, 0) : null,
                LocalDateTime.of(2026, 10, 18, 9, 30, 0));
    }

    @Test
    void given_advanceCommand_when_advance_then_wireCommandMappedAndSignedReceiptMapped() {
        when(aiplatformClient.advanceContract(eq("3829493004444444"),
                any(AiplatformContractActionWireRequest.class)))
                .thenReturn(contractActionReceiptWire(4, "签署完成", true, true));

        AiplatformContractResponse receipt = contractAppService.advance("3829493004444444",
                new AiplatformContractActionCommand("双方落签，合同完成"));

        // 北向命令 → wire 命令逐字段映射（{basis}——操作者不在 body，经透传头落痕）
        ArgumentCaptor<AiplatformContractActionWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformContractActionWireRequest.class);
        verify(aiplatformClient).advanceContract(eq("3829493004444444"), wireCaptor.capture());
        assertThat(wireCaptor.getValue())
                .isEqualTo(new AiplatformContractActionWireRequest("双方落签，合同完成"));

        // 签署完成回执映射：双方签署位 true＋时点、版本不动（version 递增归 record）
        assertThat(receipt.status()).isEqualTo(4);
        assertThat(receipt.statusName()).isEqualTo("签署完成");
        assertThat(receipt.version()).isEqualTo(1);
        assertThat(receipt.partyASigned()).isTrue();
        assertThat(receipt.partyBSignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 9, 5, 0));
    }

    @Test
    void given_failWithdrawVoidCommands_when_act_then_sharedActionWireCommandMapped() {
        // fail：旁落「签署失败或过期」（5）——单方已签如实定格
        when(aiplatformClient.failContract(eq("3829493004444444"),
                any(AiplatformContractActionWireRequest.class)))
                .thenReturn(contractActionReceiptWire(5, "签署失败或过期", true, false));
        AiplatformContractResponse failed = contractAppService.fail("3829493004444444",
                new AiplatformContractActionCommand("超期未签，如实旁落失败终态"));
        assertThat(failed.status()).isEqualTo(5);
        assertThat(failed.statusName()).isEqualTo("签署失败或过期");
        assertThat(failed.partyASigned()).isTrue();

        // withdraw：旁落「已撤回或作废」（6）——此后换新号重建
        when(aiplatformClient.withdrawContract(eq("3829493004444444"),
                any(AiplatformContractActionWireRequest.class)))
                .thenReturn(contractActionReceiptWire(6, "已撤回或作废", false, false));
        AiplatformContractResponse withdrawn = contractAppService.withdraw("3829493004444444",
                new AiplatformContractActionCommand("范围录错，整案回炉"));
        assertThat(withdrawn.status()).isEqualTo(6);
        assertThat(withdrawn.statusName()).isEqualTo("已撤回或作废");

        // void：守卫与去向同撤回、留痕 action 分立——回执形态共形（区别只在 provider 留痕行）
        when(aiplatformClient.voidContract(eq("3829493004444444"),
                any(AiplatformContractActionWireRequest.class)))
                .thenReturn(contractActionReceiptWire(6, "已撤回或作废", false, false));
        AiplatformContractResponse voided = contractAppService.voidContract("3829493004444444",
                new AiplatformContractActionCommand("主体信息录错，作废重建"));
        assertThat(voided.status()).isEqualTo(6);

        // 三旁落口共用动作命令 wire 形制（{basis}），逐口各自映射
        verify(aiplatformClient).failContract(eq("3829493004444444"),
                eq(new AiplatformContractActionWireRequest("超期未签，如实旁落失败终态")));
        verify(aiplatformClient).withdrawContract(eq("3829493004444444"),
                eq(new AiplatformContractActionWireRequest("范围录错，整案回炉")));
        verify(aiplatformClient).voidContract(eq("3829493004444444"),
                eq(new AiplatformContractActionWireRequest("主体信息录错，作废重建")));
    }

    @Test
    void given_signCommand_when_sign_then_partyCodeMappedAndPartyReceiptMapped() {
        when(aiplatformClient.signContract(eq("3829493004444444"),
                any(AiplatformContractSignWireRequest.class)))
                .thenReturn(contractActionReceiptWire(3, "签署进行中", true, true));

        AiplatformContractResponse receipt = contractAppService.sign("3829493004444444",
                new AiplatformContractSignCommand(2, "乙方盖章"));

        // party Integer code 直通映射（1=甲方 2=乙方——不建 typed enum 不预校验）
        ArgumentCaptor<AiplatformContractSignWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformContractSignWireRequest.class);
        verify(aiplatformClient).signContract(eq("3829493004444444"), wireCaptor.capture());
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformContractSignWireRequest(2, "乙方盖章"));

        // 落签回执映射：乙方签署位翻 true＋时点；状态不迁移（仍签署进行中——推进归 advance）
        assertThat(receipt.status()).isEqualTo(3);
        assertThat(receipt.partyBSigned()).isTrue();
        assertThat(receipt.partyBSignedAt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 9, 5, 0));
    }

    @Test
    void given_recordCommand_when_record_then_fiveFieldsMappedAndVersionBumpedReceiptMapped() {
        when(aiplatformClient.recordContract(eq("3829493004444444"),
                any(AiplatformContractRecordWireRequest.class)))
                .thenReturn(new AiplatformContractWireResponse(
                        "3829493006666666", "SG-l5f2xq9wn1", "3829492001234567",
                        1, "准备中", 2, false,
                        "张三", "语义引力科技有限公司",
                        "管理后台＋小程序", "开工款 50%，验收 50%",
                        "交付清单：管理后台＋小程序", "验收方式：按 PRD v3 验收", "工期口径：4 周",
                        99000L, "CNY", 3, 2,
                        false, null, false, null,
                        LocalDateTime.of(2026, 10, 22, 9, 0, 0)));

        AiplatformContractResponse receipt = contractAppService.record("3829493004444444",
                new AiplatformContractRecordCommand("张三", "语义引力科技有限公司",
                        "管理后台＋小程序", "开工款 50%，验收 50%", "改价后重录"));

        // 北向五字段 → wire 五字段逐字段映射（四预览字段可空直通＋依据必填）
        ArgumentCaptor<AiplatformContractRecordWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformContractRecordWireRequest.class);
        verify(aiplatformClient).recordContract(eq("3829493004444444"), wireCaptor.capture());
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformContractRecordWireRequest(
                "张三", "语义引力科技有限公司", "管理后台＋小程序", "开工款 50%，验收 50%", "改价后重录"));

        // 重录回执映射：version +1（1→2）＋「需更新」清零＋冻结集取现值（改价 99000）
        assertThat(receipt.version()).isEqualTo(2);
        assertThat(receipt.needsUpdate()).isFalse();
        assertThat(receipt.amount()).isEqualTo(99000L);
        assertThat(receipt.partyA()).isEqualTo("张三");
        assertThat(receipt.paymentPlan()).isEqualTo("开工款 50%，验收 50%");
    }

    @Test
    void given_ord027FromDownstream_when_advance_then_bothSignedGuardPropagatedWithoutMapping() {
        when(aiplatformClient.advanceContract(eq("3829493004444444"),
                any(AiplatformContractActionWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":5027,\"message\":\"双方尚未全部签署，不能标记签署完成\",\"data\":null}")));

        assertThatThrownBy(() -> contractAppService.advance("3829493004444444",
                new AiplatformContractActionCommand("乙方未签就想完成")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // ORD_027 → HTTP 409 + 数字业务码 5027（域码 5×1000＋27——双方均签守卫归 provider）
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5027);
                    assertThat(upstream.getMessage()).isEqualTo("双方尚未全部签署，不能标记签署完成");
                });
    }

    // ========== 付款核验域（#106 A3——与既有支付服务 BFF 上下文无关勿混读）· 命令映射 + 回执映射 + 二进制载体保全 ==========

    /** 付款计划详情 wire 工厂：三节点（待支付/待核验/已到账）+ 留痕一行 + 历史一行。 */
    private static AiplatformPaymentPlanDetailWireResponse paymentPlanDetailWire() {
        return new AiplatformPaymentPlanDetailWireResponse(
                new AiplatformPaymentPlanWireResponse(
                        "3829495000000001", "3829492001234567", "3829493005555555", "SG-l5eq8m2k7c",
                        3900000L, 195000L, LocalDateTime.of(2026, 10, 20, 9, 0, 0),
                        List.of(
                                new AiplatformPaymentNodeWireResponse(
                                        "3829496001000001", 1, "首期开工款", 1950000L, 50,
                                        1, "待支付", "SG-l5eq8m2k7c-K1", true,
                                        1, "线下转账", null, 0L, 1950000L, null, List.of()),
                                new AiplatformPaymentNodeWireResponse(
                                        "3829496001000004", 4, "尾款结算一", 585000L, 15,
                                        4, "线下转账待核验", "SG-l5eq8m2k7c-K4", false,
                                        1, "线下转账", null, 0L, 585000L, null,
                                        List.of(new AiplatformPaymentVoucherWireResponse(
                                                "3829497000000001", "3829496001000004",
                                                "receipt-1.pdf", "application/pdf", 102400L,
                                                LocalDateTime.of(2026, 10, 16, 15, 0, 0)))),
                                new AiplatformPaymentNodeWireResponse(
                                        "3829496001000005", 5, "尾款结算二", 195000L, 5,
                                        5, "已到账", "SG-l5eq8m2k7c-K5", false,
                                        1, "线下转账", "BANK-20261017-0001", 195000L, 0L,
                                        LocalDateTime.of(2026, 10, 17, 10, 0, 0), List.of()))),
                List.of(new AiplatformPaymentVerificationWireResponse(
                        "3829498000000002", "3829496001000005", 1, "到账核验", 4, 5,
                        195000L, LocalDateTime.of(2026, 10, 17, 10, 0, 0), "3829497000000003",
                        "银企对账一致", "700100", "运营·小刘",
                        LocalDateTime.of(2026, 10, 17, 10, 5, 0))),
                List.of(new AiplatformPaymentPlanDetailWireResponse.HistoryPlanWireResponse(
                        "3829495000000000", "3829493001111111", "SG-l590hd3t5y", 3,
                        4800000L, 0L, LocalDateTime.of(2026, 10, 12, 11, 0, 0))));
    }

    @Test
    void given_paymentPlanDetailWire_when_getPlanDetail_then_threeSegmentsMapped() {
        when(aiplatformClient.getPaymentPlan("3829492001234567")).thenReturn(paymentPlanDetailWire());

        var detail = paymentAppService.getPlanDetail("3829492001234567");

        // wire 三段 → 北向三段逐字段：plan（五态/附言码/开工门/渠道流水号/凭证清单）＋
        // trails（留痕行）＋history（历史行）——逐字镜像无增删无换型
        assertThat(detail.plan().id()).isEqualTo("3829495000000001");
        assertThat(detail.plan().totalAmount()).isEqualTo(3900000L);
        assertThat(detail.plan().nodes()).hasSize(3);
        var awaiting = detail.plan().nodes().get(1);
        assertThat(awaiting.status()).isEqualTo(4);
        assertThat(awaiting.statusName()).isEqualTo("线下转账待核验");
        assertThat(awaiting.vouchers()).hasSize(1);
        assertThat(awaiting.vouchers().get(0).fileName()).isEqualTo("receipt-1.pdf");
        var received = detail.plan().nodes().get(2);
        assertThat(received.status()).isEqualTo(5);
        assertThat(received.channelRefNo()).isEqualTo("BANK-20261017-0001");
        assertThat(received.remainingAmount()).isEqualTo(0L);
        assertThat(detail.trails()).hasSize(1);
        assertThat(detail.trails().get(0).action()).isEqualTo(1);
        assertThat(detail.trails().get(0).operatorName()).isEqualTo("运营·小刘");
        assertThat(detail.history()).hasSize(1);
        assertThat(detail.history().get(0).contractNo()).isEqualTo("SG-l590hd3t5y");
        assertThat(detail.history().get(0).nodeCount()).isEqualTo(3);
    }

    @Test
    void given_nodesCommand_when_enterNodes_then_wireCommandMappedAndReceiptMapped() {
        when(aiplatformClient.enterPaymentNodes(eq("3829492001234567"),
                any(AiplatformPaymentNodesWireRequest.class)))
                .thenReturn(paymentPlanDetailWire().plan());

        var receipt = paymentAppService.enterNodes("3829492001234567",
                new AiplatformPaymentNodesCommand(List.of(
                        new AiplatformPaymentNodesCommand.NodeSpec("首期开工款", 1950000L, true),
                        new AiplatformPaymentNodesCommand.NodeSpec("尾款结算一", 585000L, false),
                        new AiplatformPaymentNodesCommand.NodeSpec("尾款结算二", 195000L, false))));

        // 北向节点规格 → wire 规格逐字段映射（{name, amount, kickoffGate} 整组——序号不在录入面）
        ArgumentCaptor<AiplatformPaymentNodesWireRequest> nodesCaptor =
                ArgumentCaptor.forClass(AiplatformPaymentNodesWireRequest.class);
        verify(aiplatformClient).enterPaymentNodes(eq("3829492001234567"), nodesCaptor.capture());
        assertThat(nodesCaptor.getValue()).isEqualTo(new AiplatformPaymentNodesWireRequest(List.of(
                new AiplatformPaymentNodesWireRequest.NodeSpecWire("首期开工款", 1950000L, true),
                new AiplatformPaymentNodesWireRequest.NodeSpecWire("尾款结算一", 585000L, false),
                new AiplatformPaymentNodesWireRequest.NodeSpecWire("尾款结算二", 195000L, false))));

        // 录入回执映射（录入后计划现值）
        assertThat(receipt.totalAmount()).isEqualTo(3900000L);
        assertThat(receipt.nodes().get(0).kickoffGate()).isTrue();
        assertThat(receipt.nodes().get(0).remarkCode()).isEqualTo("SG-l5eq8m2k7c-K1");
    }

    @Test
    void given_verifyCommand_when_verify_then_wireCommandMappedAndReceivedReceiptMapped() {
        when(aiplatformClient.verifyPayment(eq("3829492001234567"), eq("3829496001000004"),
                any(AiplatformPaymentVerifyWireRequest.class)))
                .thenReturn(paymentPlanDetailWire().plan());

        var receipt = paymentAppService.verify("3829492001234567", "3829496001000004",
                new AiplatformPaymentVerifyCommand(585000L, LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                        "3829497000000001", "BANK-20261017-0001", "银企对账一致"));

        // 北向五字段 → wire 五字段逐字段映射（全额判定归 provider——BFF 不复算不预判）
        ArgumentCaptor<AiplatformPaymentVerifyWireRequest> verifyCaptor =
                ArgumentCaptor.forClass(AiplatformPaymentVerifyWireRequest.class);
        verify(aiplatformClient).verifyPayment(eq("3829492001234567"), eq("3829496001000004"),
                verifyCaptor.capture());
        assertThat(verifyCaptor.getValue()).isEqualTo(new AiplatformPaymentVerifyWireRequest(
                585000L, LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                "3829497000000001", "BANK-20261017-0001", "银企对账一致"));

        // 核验回执映射（动作后计划现值——留痕行不在写口回执，归详情读面）
        assertThat(receipt.orderId()).isEqualTo("3829492001234567");
        assertThat(receipt.nodes()).hasSize(3);
    }

    @Test
    void given_rejectCommand_when_reject_then_wireCommandMappedAndPendingReceiptMapped() {
        when(aiplatformClient.rejectPayment(eq("3829492001234567"), eq("3829496001000004"),
                any(AiplatformPaymentRejectWireRequest.class)))
                .thenReturn(paymentPlanDetailWire().plan());

        var receipt = paymentAppService.reject("3829492001234567", "3829496001000004",
                new AiplatformPaymentRejectCommand("3829497000000001", "回单模糊无法对账，请重新上传"));

        // 北向两字段 → wire 两字段映射（依据必填归 provider ORD_042——BFF 不前置拦截）
        ArgumentCaptor<AiplatformPaymentRejectWireRequest> rejectCaptor =
                ArgumentCaptor.forClass(AiplatformPaymentRejectWireRequest.class);
        verify(aiplatformClient).rejectPayment(eq("3829492001234567"), eq("3829496001000004"),
                rejectCaptor.capture());
        assertThat(rejectCaptor.getValue()).isEqualTo(new AiplatformPaymentRejectWireRequest(
                "3829497000000001", "回单模糊无法对账，请重新上传"));

        // 打回回执映射（回待支付后的计划现值）
        assertThat(receipt.totalSettled()).isEqualTo(195000L);
        assertThat(receipt.nodes().get(1).status()).isEqualTo(4);
    }

    @Test
    void given_voucherBinaryFromDownstream_when_getVoucherContent_then_bytesAndRawHeadersPreserved() {
        byte[] pdf = {(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2d,
                (byte) 0x31, (byte) 0x2e, (byte) 0x34, 0x20, (byte) 0xff, 0x41, 0x00};
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/pdf"),
                "Content-Disposition", List.of("attachment; filename=\"receipt-1.pdf\"")),
                (a, b) -> true);
        when(aiplatformClient.downloadVoucherContent("3829497000000001"))
                .thenReturn(new BinaryResponse(200, providerHeaders, pdf));

        var content = paymentAppService.getVoucherContent("3829497000000001");

        // 字节逐位保全（含 0xff/NUL——ofString 解码损坏的回归锚点）+ provider 响应头 raw 值透传
        // （真实媒体类型＋attachment 文件名——照源码包/设计资产包范式）
        assertThat(content.content()).containsExactly(pdf);
        assertThat(content.contentType()).isEqualTo("application/pdf");
        assertThat(content.contentDisposition()).isEqualTo("attachment; filename=\"receipt-1.pdf\"");
    }

    @Test
    void given_ord038FromDownstream_when_verify_then_stateGuardPropagatedWithoutMapping() {
        when(aiplatformClient.verifyPayment(eq("3829492001234567"), eq("3829496001000001"),
                any(AiplatformPaymentVerifyWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":5038,\"message\":\"付款节点当前状态不允许该操作\",\"data\":null}")));

        assertThatThrownBy(() -> paymentAppService.verify("3829492001234567", "3829496001000001",
                new AiplatformPaymentVerifyCommand(100L, LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                        null, null, null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // ORD_038 → HTTP 409 + 数字业务码 5038（域码 5×1000＋38——状态守卫归 provider）
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(5038);
                    assertThat(upstream.getMessage()).isEqualTo("付款节点当前状态不允许该操作");
                });
    }

    // ========== 生产承接域（#107 A4）· 命令映射 + 回执映射 + 错误信封透传 ==========

    /** 生产承接 wire 工厂：两态第一实值（1=待安排生产）＋留痕两行（信息更新最新/创建系统操作者最旧）。 */
    private static AiplatformProductionHandoffWireResponse productionHandoffWire() {
        return new AiplatformProductionHandoffWireResponse(
                "3829410000000001", "3829409001234567", "3829492001234567",
                1, "待安排生产",
                "李经理（微信 ligoods）", "准备服务器环境与域名备案", "已完成技术方案拆解",
                LocalDateTime.of(2026, 10, 20, 9, 0, 0), LocalDateTime.of(2026, 10, 17, 10, 0, 0),
                List.of(
                        new AiplatformProductionHandoffWireResponse.TransitionWireResponse(
                                3, "信息更新", 1, "待安排生产", 1, "待安排生产",
                                "首次排期录入", "700100", "生产承接_老王",
                                LocalDateTime.of(2026, 10, 18, 14, 30, 0)),
                        new AiplatformProductionHandoffWireResponse.TransitionWireResponse(
                                1, "创建", 1, "待安排生产", 1, "待安排生产",
                                "开工条件满足（合同签署完成且开工门节点全部到账），创建生产承接记录",
                                "system", "开工判定",
                                LocalDateTime.of(2026, 10, 17, 10, 0, 0))));
    }

    @Test
    void given_handoffWire_when_getHandoff_then_fieldsAndTrailMapped() {
        when(aiplatformClient.getProductionHandoff("3829409001234567")).thenReturn(productionHandoffWire());

        var handoff = productionAppService.getHandoff("3829409001234567");

        // wire → 北向逐字段：两态 Integer code 直通＋承接可空段＋留痕时间线（新→旧）
        assertThat(handoff.status()).isEqualTo(1);
        assertThat(handoff.statusName()).isEqualTo("待安排生产");
        assertThat(handoff.contact()).isEqualTo("李经理（微信 ligoods）");
        assertThat(handoff.estimatedStartAt()).isEqualTo(LocalDateTime.of(2026, 10, 20, 9, 0, 0));
        assertThat(handoff.transitions()).hasSize(2);
        // 最新行＝信息更新（action=3，非迁移类 from=to 在场快照，操作者＝后台管理员）
        assertThat(handoff.transitions().get(0).action()).isEqualTo(3);
        assertThat(handoff.transitions().get(0).fromStatus()).isEqualTo(1);
        assertThat(handoff.transitions().get(0).toStatus()).isEqualTo(1);
        assertThat(handoff.transitions().get(0).operatorId()).isEqualTo("700100");
        // 最旧行＝创建（action=1，系统触发建档——操作者＝system/开工判定，非人工创建口）
        assertThat(handoff.transitions().get(1).action()).isEqualTo(1);
        assertThat(handoff.transitions().get(1).operatorId()).isEqualTo("system");
        assertThat(handoff.transitions().get(1).operatorName()).isEqualTo("开工判定");
    }

    @Test
    void given_updateCommand_when_update_then_wireCommandMappedAndReceiptMapped() {
        when(aiplatformClient.updateProductionHandoff(eq("3829409001234567"),
                any(AiplatformProductionUpdateWireRequest.class)))
                .thenReturn(productionHandoffWire());

        var receipt = productionAppService.update("3829409001234567",
                new AiplatformProductionUpdateCommand("李经理（微信 ligoods）",
                        "准备服务器环境与域名备案", "已完成技术方案拆解",
                        LocalDateTime.of(2026, 10, 20, 9, 0, 0), "首次排期录入"));

        // 北向五字段 → wire 五字段逐字段映射（整组替换——null 即清空语义归 provider）
        ArgumentCaptor<AiplatformProductionUpdateWireRequest> updateCaptor =
                ArgumentCaptor.forClass(AiplatformProductionUpdateWireRequest.class);
        verify(aiplatformClient).updateProductionHandoff(eq("3829409001234567"), updateCaptor.capture());
        assertThat(updateCaptor.getValue()).isEqualTo(new AiplatformProductionUpdateWireRequest(
                "李经理（微信 ligoods）", "准备服务器环境与域名备案", "已完成技术方案拆解",
                LocalDateTime.of(2026, 10, 20, 9, 0, 0), "首次排期录入"));

        // 更新回执映射（更新后承接现值——状态不动留痕行 from=to）
        assertThat(receipt.status()).isEqualTo(1);
        assertThat(receipt.contact()).isEqualTo("李经理（微信 ligoods）");
    }

    @Test
    void given_advanceCommand_when_advance_then_wireCommandMappedAndInProductionReceiptMapped() {
        when(aiplatformClient.advanceProductionHandoff(eq("3829409001234567"),
                any(AiplatformProductionActionWireRequest.class)))
                .thenReturn(new AiplatformProductionHandoffWireResponse(
                        "3829410000000001", "3829409001234567", "3829492001234567",
                        2, "生产中", null, null, null, null,
                        LocalDateTime.of(2026, 10, 17, 10, 0, 0), List.of()));

        var receipt = productionAppService.advance("3829409001234567",
                new AiplatformProductionActionCommand("排期落定，今日开工"));

        // 北向单字段 → wire 单字段映射（依据必填归 provider PRJ_057——BFF 不前置拦截）
        ArgumentCaptor<AiplatformProductionActionWireRequest> advanceCaptor =
                ArgumentCaptor.forClass(AiplatformProductionActionWireRequest.class);
        verify(aiplatformClient).advanceProductionHandoff(eq("3829409001234567"), advanceCaptor.capture());
        assertThat(advanceCaptor.getValue()).isEqualTo(
                new AiplatformProductionActionWireRequest("排期落定，今日开工"));

        // 推进回执映射（两态第二实值 2=生产中——唯一边走完，可空段 null 如实透传）
        assertThat(receipt.status()).isEqualTo(2);
        assertThat(receipt.statusName()).isEqualTo("生产中");
        assertThat(receipt.contact()).isNull();
        assertThat(receipt.estimatedStartAt()).isNull();
    }

    @Test
    void given_supplementCommand_when_setNeedsUserSupplement_then_wireCommandMappedAndVoidReturn() {
        productionAppService.setNeedsUserSupplement("3829409001234567",
                new AiplatformProductionSupplementCommand(true));

        // 北向单布尔 → wire 单布尔映射（盖帽置位语义归后台人工判定——provider 只落标志）
        ArgumentCaptor<AiplatformProductionSupplementWireRequest> supplementCaptor =
                ArgumentCaptor.forClass(AiplatformProductionSupplementWireRequest.class);
        verify(aiplatformClient).setProductionNeedsUserSupplement(eq("3829409001234567"),
                supplementCaptor.capture());
        assertThat(supplementCaptor.getValue()).isEqualTo(new AiplatformProductionSupplementWireRequest(true));
    }

    @Test
    void given_prj059FromDownstream_when_advance_then_operatorGuardPropagatedWithoutMapping() {
        when(aiplatformClient.advanceProductionHandoff(eq("3829409001234567"),
                any(AiplatformProductionActionWireRequest.class)))
                .thenThrow(AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":4059,\"message\":\"操作者必填（生产承接动作须携带操作者身份）\",\"data\":null}")));

        assertThatThrownBy(() -> productionAppService.advance("3829409001234567",
                new AiplatformProductionActionCommand("排期落定")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // PRJ_059 → HTTP 400 + 数字业务码 4059（域码 4×1000＋59——缺操作者透传头，
                    // 承接动作以操作者为审计正身归 provider 裁决）
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4059);
                    assertThat(upstream.getMessage()).isEqualTo("操作者必填（生产承接动作须携带操作者身份）");
                });
    }

    // ========== 项目清单 · query→wire 映射（status 单选）+ 分页 1-based 透传 ==========

    @Test
    void given_projectQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        when(aiplatformClient.listProjects(any(), eq(3), eq(20))).thenReturn(new PageResponse<>(List.of(
                new AiplatformProjectSummaryWireResponse(
                        "3829492007654321", "英语学习助手", "auth0|65f2c8a1", "文野",
                        1, "官网", 3, "已归档", true,
                        LocalDateTime.of(2026, 8, 1, 9, 0, 0),
                        LocalDateTime.of(2026, 9, 1, 12, 0, 0))), 7, 3, 20));

        PageResponse<AiplatformProjectSummaryResponse> page = projectAppService.list(
                new AiplatformProjectQuery(3,
                        LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                        LocalDateTime.of(2026, 9, 15, 23, 59, 59),
                        "auth0|65f2c8a1", "3829492007654321"), 3, 20);

        // 出站参数：北向 query → wire 过滤记录逐字段映射（status 三档单选单值）；page 1-based 直传（3→3，无 ±1）
        ArgumentCaptor<AiplatformProjectListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformProjectListWireRequest.class);
        verify(aiplatformClient).listProjects(wireCaptor.capture(), eq(3), eq(20));
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformProjectListWireRequest(
                3,
                LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                LocalDateTime.of(2026, 9, 15, 23, 59, 59),
                "auth0|65f2c8a1", "3829492007654321"));

        // 回显取 provider 回报值；wire → response 逐字段（type/status Integer + *Name、archived Boolean）
        assertThat(page.total()).isEqualTo(7L);
        assertThat(page.page()).isEqualTo(3);
        var row = page.items().get(0);
        assertThat(row.id()).isEqualTo("3829492007654321");
        assertThat(row.type()).isEqualTo(1);
        assertThat(row.typeName()).isEqualTo("官网");
        assertThat(row.status()).isEqualTo(3);
        assertThat(row.statusName()).isEqualTo("已归档");
        assertThat(row.archived()).isTrue();
    }

    // ========== 项目详情 · 订单引用 + 成本指针映射 ==========

    @Test
    void given_projectDetailWire_when_getDetail_then_orderRefsAndCostSummaryMapped() {
        when(aiplatformClient.getProject("3829492007654321")).thenReturn(
                new AiplatformProjectDetailWireResponse(
                        "3829492007654321", "英语学习助手", "auth0|65f2c8a1", "文野", "3829492009999999",
                        1, "官网", 1, "进行中", false,
                        LocalDateTime.of(2026, 9, 10, 14, 20, 0),
                        LocalDateTime.of(2026, 9, 14, 18, 30, 0),
                        LocalDateTime.of(2026, 9, 10, 20, 0, 0),
                        LocalDateTime.of(2026, 9, 11, 8, 0, 0),
                        new AiplatformOrderBriefWireResponse("3829492001234567", 2, "已报价"),
                        new AiplatformOrderBriefWireResponse("3829492005555444", 4, "已归档"),
                        new AiplatformProjectDetailWireResponse.CostSummary(
                                Map.of("CNY", new BigDecimal("12.3456")), true)));

        AiplatformProjectDetailResponse detail = projectAppService.getDetail("3829492007654321");

        // wire → response 逐字段：订单引用（activeOrder 未终结/latestOrder 最近）+ 成本指针（Map 原样 + unpriced）
        assertThat(detail.workspaceId()).isEqualTo("3829492009999999");
        assertThat(detail.activeOrder().id()).isEqualTo("3829492001234567");
        assertThat(detail.activeOrder().status()).isEqualTo(2);
        assertThat(detail.latestOrder().statusName()).isEqualTo("已归档");
        assertThat(detail.costSummary().cost()).containsEntry("CNY", new BigDecimal("12.3456"));
        assertThat(detail.costSummary().unpriced()).isTrue();
        assertThat(detail.prdProducedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 20, 0, 0));
    }

    // ========== 深读三组 · 对话史 / PRD / 版本 ==========

    @Test
    void given_conversationWire_when_getConversation_then_kindsAndPayloadsMapped() {
        when(aiplatformClient.getConversation("3829492007654321")).thenReturn(List.of(
                new AiplatformConversationEntryWireResponse(
                        90001L, 1, "用户发言", null, null, null, "首页加一个轮播图",
                        null, null, List.of(Map.of("type", "circle")), null, false,
                        LocalDateTime.of(2026, 9, 12, 10, 0, 0)),
                new AiplatformConversationEntryWireResponse(
                        90003L, 5, "收尾卡", null, null, "run-abc123", null,
                        null, Map.of("runId", "run-abc123", "commitHash", "abc"), null, null, false,
                        LocalDateTime.of(2026, 9, 12, 11, 30, 0)),
                new AiplatformConversationEntryWireResponse(
                        90004L, 7, "报价卡", null, null, null, null,
                        null, null, null, Map.of("orderId", "3897654321098765432", "event", "quoted"), false,
                        LocalDateTime.of(2026, 9, 12, 12, 0, 0)),
                new AiplatformConversationEntryWireResponse(
                        90005L, 5, "收尾卡", 2, "设计会话", "run-d1", null,
                        null, Map.of("summary", "完成设计物：活动海报", "drafts",
                                List.of(Map.of("item", "活动海报", "media", "image", "path", "/design/poster.png"))),
                        null, null, false,
                        LocalDateTime.of(2026, 9, 12, 12, 30, 0))));

        var entries = projectAppService.getConversation("3829492007654321");

        // kind Integer code + kindName 随行（aiplatform#186 provider 出口提供）；载荷 Map/List 原样透传
        assertThat(entries).hasSize(4);
        var user = entries.get(0);
        assertThat(user.kind()).isEqualTo(1);
        assertThat(user.kindName()).isEqualTo("用户发言");
        assertThat(user.attachments()).hasSize(1);
        var closing = entries.get(1);
        assertThat(closing.kind()).isEqualTo(5);
        assertThat(closing.closing()).containsEntry("commitHash", "abc");
        // 报价卡（kind=7，provider #203）：quote 载荷原样透传（仅事件＋订单引用，不含金额——视镜 ADR-0017）
        var quote = entries.get(2);
        assertThat(quote.kind()).isEqualTo(7);
        assertThat(quote.kindName()).isEqualTo("报价卡");
        assertThat(quote.quote())
                .containsEntry("orderId", "3897654321098765432")
                .containsEntry("event", "quoted");
        // 会话来源对映射（provider #298）：主会话条目 null/null 直通（含编码收尾卡）、设计会话
        // 收尾卡（closing 携 drafts）2/"设计会话"——AppService 纯委托零推导
        assertThat(user.sessionSource()).isNull();
        assertThat(user.sessionSourceName()).isNull();
        assertThat(closing.sessionSource()).isNull();
        assertThat(closing.sessionSourceName()).isNull();
        var designClosing = entries.get(3);
        assertThat(designClosing.sessionSource()).isEqualTo(2);
        assertThat(designClosing.sessionSourceName()).isEqualTo("设计会话");
    }

    @Test
    void given_prdAndVersionsWire_when_deepRead_then_mappedVerbatim() {
        when(aiplatformClient.getPrd("3829492007654321")).thenReturn(new AiplatformPrdWireResponse(
                "3829492007654321", "# PRD\n\n做一个英语学习助手……", Instant.parse("2026-09-12T08:30:00Z")));
        when(aiplatformClient.listVersions("3829492007654321")).thenReturn(List.of(
                new AiplatformVersionWireResponse(
                        "abc", "轮播图上线", "run-abc123", null, LocalDateTime.of(2026, 9, 12, 11, 30, 0))));
        when(aiplatformClient.getVersion("3829492007654321", "abc")).thenReturn(
                new AiplatformVersionDetailWireResponse(
                        "abc", "轮播图上线", "run-abc123", null,
                        LocalDateTime.of(2026, 9, 12, 11, 30, 0), Map.of("runId", "run-abc123")));

        // PRD：markdown 正文 + updatedAt Instant 原样
        var prd = projectAppService.getPrd("3829492007654321");
        assertThat(prd.content()).startsWith("# PRD");
        assertThat(prd.updatedAt()).isEqualTo(Instant.parse("2026-09-12T08:30:00Z"));

        // 版本列表：run 版本 runId 非空 / rollbackFrom null
        var versions = projectAppService.listVersions("3829492007654321");
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).runId()).isEqualTo("run-abc123");
        assertThat(versions.get(0).rollbackFrom()).isNull();

        // 版本详情：closing 载荷原样
        var version = projectAppService.getVersion("3829492007654321", "abc");
        assertThat(version.closing()).containsEntry("runId", "run-abc123");
    }

    // ========== 项目详情错误 · 透传不映射 ==========

    @Test
    void given_prj001FromDownstream_when_getProject_then_propagateWithoutMapping() {
        when(aiplatformClient.getProject("3829499999999999")).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(404, "{\"code\":4001,\"message\":\"项目不存在\",\"data\":null}")));

        assertThatThrownBy(() -> projectAppService.getDetail("3829499999999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4001);
                    assertThat(upstream.getMessage()).isEqualTo("项目不存在");
                });
    }

    // ========== 项目文件区（#71）· 文件树/文件内容映射 + 文件包二进制载体保全 ==========

    @Test
    void given_filesWire_when_getFiles_then_fileTreeMapped() {
        when(aiplatformClient.getProjectFiles("3829492007654321")).thenReturn(
                new AiplatformProjectFilesWireResponse("3829492007654321", List.of(
                        new AiplatformProjectFilesWireResponse.FileEntry("README.md", 512L),
                        new AiplatformProjectFilesWireResponse.FileEntry("docs/PRD.md", 4096L))));

        var files = projectAppService.getFiles("3829492007654321");

        // wire → response 逐字段：{projectId, files:[{path, size}]}——size long（字节）
        assertThat(files.projectId()).isEqualTo("3829492007654321");
        assertThat(files.files()).hasSize(2);
        assertThat(files.files().get(0).path()).isEqualTo("README.md");
        assertThat(files.files().get(0).size()).isEqualTo(512L);
        assertThat(files.files().get(1).path()).isEqualTo("docs/PRD.md");
        assertThat(files.files().get(1).size()).isEqualTo(4096L);
    }

    @Test
    void given_fileContentWire_when_getFileContent_then_pathAndContentMapped() {
        when(aiplatformClient.getProjectFileContent("3829492007654321", "docs/PRD.md"))
                .thenReturn(new AiplatformProjectFileContentWireResponse(
                        "docs/PRD.md", "# PRD\n\n做一个英语学习助手……"));

        var content = projectAppService.getFileContent("3829492007654321", "docs/PRD.md");

        // {path, content} 逐字段（path 原样回显）；只读策略归 provider，BFF 不预检不解释
        assertThat(content.path()).isEqualTo("docs/PRD.md");
        assertThat(content.content()).startsWith("# PRD");
    }

    @Test
    void given_tarGzFromDownstream_when_getFilesPackage_then_bytesAndRawHeadersPreserved() {
        byte[] tarGz = {(byte) 0x1f, (byte) 0x8b, 0x08, 0x00, (byte) 0xff, 0x41, 0x00};
        HttpHeaders providerHeaders = HttpHeaders.of(Map.of(
                "Content-Type", List.of("application/gzip"),
                "Content-Disposition", List.of("attachment; filename=\"3829492007654321-archive.tar.gz\"")),
                (a, b) -> true);
        when(aiplatformClient.downloadProjectFilesPackage("3829492007654321"))
                .thenReturn(new BinaryResponse(200, providerHeaders, tarGz));

        var pkg = projectAppService.getFilesPackage("3829492007654321");

        // 字节逐位保全 + provider 响应头 raw 值透传（sealed/source 两态文件名由 provider 决定）
        assertThat(pkg.content()).containsExactly(tarGz);
        assertThat(pkg.contentType()).isEqualTo("application/gzip");
        assertThat(pkg.contentDisposition()).isEqualTo("attachment; filename=\"3829492007654321-archive.tar.gz\"");
    }

    @Test
    void given_prj020FromDownstream_when_getFileContent_then_propagateWithoutMapping() {
        when(aiplatformClient.getProjectFileContent(eq("3829492007654321"), eq(".env"))).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(400,
                                "{\"code\":4020,\"message\":\"该文件不在可浏览范围\",\"data\":null}")));

        assertThatThrownBy(() -> projectAppService.getFileContent("3829492007654321", ".env"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4020);
                    assertThat(upstream.getMessage()).isEqualTo("该文件不在可浏览范围");
                });
    }

    // ========== 沙箱清单 · query→wire 映射（期望态/实态两维）+ 分页 1-based 透传 ==========

    @Test
    void given_workspaceQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        when(aiplatformClient.listWorkspaces(any(), eq(2), eq(20))).thenReturn(new PageResponse<>(List.of(
                new AiplatformWorkspaceSummaryWireResponse(
                        "3829492009999999", "ws-3829492009999999",
                        1, "开发", 2, "就绪", 1, "运行", 3, "无容器",
                        LocalDateTime.of(2026, 9, 14, 22, 10, 0),
                        2147483648L, null, null,
                        new AiplatformWorkspaceSummaryWireResponse.ProjectRef(
                                "3829492007654321", "英语学习助手", false))), 17, 2, 20));

        PageResponse<AiplatformWorkspaceSummaryResponse> page = workspaceAppService.list(
                new AiplatformWorkspaceQuery(1, 3), 2, 20);

        // 出站参数：北向 query → wire 过滤记录逐字段映射（漂移清单口径：desired=1 期望运行 + actual=3 实态无容器）；
        // page 1-based 直传（2→2，无 ±1）
        ArgumentCaptor<AiplatformWorkspaceListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformWorkspaceListWireRequest.class);
        verify(aiplatformClient).listWorkspaces(wireCaptor.capture(), eq(2), eq(20));
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformWorkspaceListWireRequest(1, 3));

        // 回显取 provider 回报值；wire → response 逐字段（四枚举 code+*Name、卷大小 Long、项目引用嵌套）
        assertThat(page.total()).isEqualTo(17L);
        assertThat(page.page()).isEqualTo(2);
        var drift = page.items().get(0);
        assertThat(drift.workspaceId()).isEqualTo("3829492009999999");
        assertThat(drift.desiredState()).isEqualTo(1);
        assertThat(drift.desiredStateName()).isEqualTo("运行");
        assertThat(drift.containerState()).isEqualTo(3);
        assertThat(drift.containerStateName()).isEqualTo("无容器");
        assertThat(drift.volumeSizeBytes()).isEqualTo(2147483648L);
        assertThat(drift.project().projectId()).isEqualTo("3829492007654321");
        assertThat(drift.project().archived()).isFalse();
    }

    // ========== 沙箱详情 · 资源观测 + 项目引用映射 ==========

    @Test
    void given_workspaceDetailWire_when_getDetail_then_resourcesAndProjectRefMapped() {
        when(aiplatformClient.getWorkspace("3829492007777777")).thenReturn(
                new AiplatformWorkspaceDetailWireResponse(
                        "3829492007777777", "ws-3829492007777777", "net-3829492007777777",
                        1, "开发", 2, "就绪", null, 3, "封存", 3, "无容器",
                        LocalDateTime.of(2026, 9, 13, 18, 0, 0), null,
                        LocalDateTime.of(2026, 9, 13, 18, 0, 0),
                        "workspace-archives/3829492007777777.tar.gz", 89128960L,
                        LocalDateTime.of(2026, 9, 8, 10, 0, 0),
                        LocalDateTime.of(2026, 9, 13, 18, 0, 0),
                        List.of(new AiplatformWorkspaceDetailWireResponse.MiddlewareResourceObservation(
                                1, "mw-3829492007777777-pg",
                                "postgresql://semgravio:secret@mw-3829492007777777-pg:5432/semgravio")),
                        new AiplatformWorkspaceSummaryWireResponse.ProjectRef(
                                "3829492007654321", "英语学习助手", false)));

        AiplatformWorkspaceDetailResponse detail = workspaceAppService.getDetail("3829492007777777");

        // wire → response 逐字段：封存态全量（封存包寻址键/审计列）+ 资源观测 + 项目引用
        assertThat(detail.networkName()).isEqualTo("net-3829492007777777");
        assertThat(detail.provisionError()).isNull();
        assertThat(detail.desiredStateName()).isEqualTo("封存");
        assertThat(detail.archivePath()).isEqualTo("workspace-archives/3829492007777777.tar.gz");
        assertThat(detail.archiveSizeBytes()).isEqualTo(89128960L);
        assertThat(detail.resources()).hasSize(1);
        assertThat(detail.resources().get(0).kind()).isEqualTo(1);
        assertThat(detail.resources().get(0).internalUrl()).startsWith("postgresql://");
        assertThat(detail.project().name()).isEqualTo("英语学习助手");
    }

    // ========== 四干预动作 · 委托 + 回执映射 + 错误透传 ==========

    @Test
    void given_wakeReceipt_when_wake_then_delegatedAndDetailMapped() {
        when(aiplatformClient.wakeWorkspace("3829492009999999")).thenReturn(
                new AiplatformWorkspaceDetailWireResponse(
                        "3829492009999999", "ws-3829492009999999", "net-3829492009999999",
                        1, "开发", 2, "就绪", null, 1, "运行", 1, "运行中",
                        LocalDateTime.of(2026, 9, 16, 9, 0, 0), 1073741824L,
                        null, null, null,
                        LocalDateTime.of(2026, 9, 8, 10, 0, 0),
                        LocalDateTime.of(2026, 9, 16, 9, 0, 0),
                        List.of(), null));

        AiplatformWorkspaceDetailResponse receipt = workspaceAppService.wake("3829492009999999");

        // 回执＝动作后的观测详情：期望运行 + 实态运行中（收敛完成的新事实）
        assertThat(receipt.desiredState()).isEqualTo(1);
        assertThat(receipt.containerState()).isEqualTo(1);
        assertThat(receipt.containerStateName()).isEqualTo("运行中");
        assertThat(receipt.project()).isNull();
    }

    @Test
    void given_wsp015FromDownstream_when_hibernate_then_propagateWithoutMapping() {
        when(aiplatformClient.hibernateWorkspace("3829492009999999")).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(409,
                                "{\"code\":1015,\"message\":\"编码 run 进行中，沙箱动作被拒（先取消 run 或等收口）\",\"data\":null}")));

        assertThatThrownBy(() -> workspaceAppService.hibernate("3829492009999999"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(1015);
                    assertThat(upstream.getMessage()).isEqualTo("编码 run 进行中，沙箱动作被拒（先取消 run 或等收口）");
                });
    }

    // ========== 成本总览 · query→wire 映射 + 分解嵌套映射 ==========

    @Test
    void given_costWindow_when_getOverview_then_windowMappedAndBreakdownsMapped() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-16T00:00:00Z");
        when(aiplatformClient.getCostOverview(new AiplatformCostWindowWireRequest(from, to)))
                .thenReturn(new AiplatformCostOverviewWireResponse(
                        from, to,
                        new AiplatformTokenUsageWireResponse(5000, 1200, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")),
                        List.of(new AiplatformCostOverviewWireResponse.ModelUsage(
                                "anthropic", "claude-fable-5",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800))),
                        List.of(new AiplatformCostOverviewWireResponse.AgentKindUsage(
                                "naming", null,
                                new AiplatformTokenUsageWireResponse(1000, 200, 0, 0, 0)))));

        AiplatformCostOverviewResponse overview = costAppService.overview(new AiplatformCostQuery(from, to));

        // 出站参数：北向 query → wire 时间窗逐字段映射（from/to Instant 原值）
        verify(aiplatformClient).getCostOverview(new AiplatformCostWindowWireRequest(from, to));

        // wire → response 逐字段：窗口回显 + 五档总量 + 币种分桶 + 双分解
        // （agentKind 裸维度串原值透传、辅助标记 agentKindName=null 不臆造）
        assertThat(overview.from()).isEqualTo(from);
        assertThat(overview.to()).isEqualTo(to);
        assertThat(overview.total().input()).isEqualTo(5000L);
        assertThat(overview.total().reasoning()).isEqualTo(800L);
        assertThat(overview.cost()).containsEntry("CNY", new BigDecimal("12.3456"));
        assertThat(overview.byModel().get(0).model()).isEqualTo("claude-fable-5");
        assertThat(overview.byModel().get(0).tokens().cacheRead()).isEqualTo(300L);
        assertThat(overview.byAgentKind().get(0).agentKind()).isEqualTo("naming");
        assertThat(overview.byAgentKind().get(0).agentKindName()).isNull();
    }

    // ========== unpriced 警示 · 档位映射 ==========

    @Test
    void given_unpricedWire_when_getUnpriced_then_tiersMapped() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-16T00:00:00Z");
        when(aiplatformClient.getUnpricedUsage(new AiplatformCostWindowWireRequest(from, to)))
                .thenReturn(new AiplatformUnpricedUsageWireResponse(
                        from, to,
                        List.of(new AiplatformUnpricedUsageWireResponse.UnpricedTier(
                                "openai", "gpt-5.2", 1, "输入", 700),
                                new AiplatformUnpricedUsageWireResponse.UnpricedTier(
                                        "imgprov", "qwen-image", 6, "按张", 3))));

        AiplatformUnpricedUsageResponse unpriced = costAppService.unpriced(new AiplatformCostQuery(from, to));

        // tokenKind Integer code + tokenKindName + tokens 只计无价分量；按张档（#98 第 6 档）
        // 直通——tokens 语义对该档＝张数，BFF 零推导零换算
        assertThat(unpriced.items()).hasSize(2);
        assertThat(unpriced.items().get(0).tokenKind()).isEqualTo(1);
        assertThat(unpriced.items().get(0).tokenKindName()).isEqualTo("输入");
        assertThat(unpriced.items().get(0).tokens()).isEqualTo(700L);
        assertThat(unpriced.items().get(1).tokenKind()).isEqualTo(6);
        assertThat(unpriced.items().get(1).tokenKindName()).isEqualTo("按张");
        assertThat(unpriced.items().get(1).tokens()).isEqualTo(3L);
    }

    // ========== 项目成本清单 · query→wire + 分页 1-based 透传 ==========

    @Test
    void given_costWindowAndPage_when_listProjectCosts_then_pageEchoedFromProvider() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-16T00:00:00Z");
        when(aiplatformClient.listProjectCosts(any(), eq(1), eq(20))).thenReturn(new PageResponse<>(List.of(
                new AiplatformProjectCostWireResponse(
                        "3829492007654321",
                        new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")), false),
                new AiplatformProjectCostWireResponse(
                        "3829492005555444",
                        new AiplatformTokenUsageWireResponse(1000, 200, 0, 0, 0),
                        Map.of(), true)), 2, 1, 20));

        PageResponse<AiplatformProjectCostResponse> page =
                costAppService.listProjectCosts(new AiplatformCostQuery(from, to), 1, 20);

        // 出站参数：北向 query → wire 时间窗映射；page 1-based 直传（1→1，无 ±1）
        verify(aiplatformClient).listProjectCosts(
                eq(new AiplatformCostWindowWireRequest(from, to)), eq(1), eq(20));

        // 回显取 provider 回报值；wire → response 逐字段（全未配价行 cost 空 + allUnpriced=true）
        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.page()).isEqualTo(1);
        var costly = page.items().get(0);
        assertThat(costly.projectId()).isEqualTo("3829492007654321");
        assertThat(costly.total().input()).isEqualTo(3000L);
        assertThat(costly.allUnpriced()).isFalse();
        var allUnpriced = page.items().get(1);
        assertThat(allUnpriced.cost()).isEmpty();
        assertThat(allUnpriced.allUnpriced()).isTrue();
    }

    // ========== 单项目下钻 · 分解 + unpriced 档位（无计数）映射 ==========

    @Test
    void given_projectCostDetailWire_when_getProjectCostDetail_then_breakdownsMapped() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-16T00:00:00Z");
        when(aiplatformClient.getProjectCostDetail(eq("3829492007654321"),
                eq(new AiplatformCostWindowWireRequest(from, to))))
                .thenReturn(new AiplatformProjectCostDetailWireResponse(
                        "3829492007654321", from, to,
                        new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800),
                        Map.of("CNY", new BigDecimal("12.3456")),
                        List.of(new AiplatformProjectCostDetailWireResponse.UnpricedTier(
                                "openai", "gpt-5.2", 1, "输入")),
                        List.of(new AiplatformProjectCostDetailWireResponse.ModelUsage(
                                "anthropic", "claude-fable-5",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800))),
                        List.of(new AiplatformProjectCostDetailWireResponse.AgentKindUsage(
                                "executor", "执行智能体",
                                new AiplatformTokenUsageWireResponse(3000, 1000, 300, 0, 800)))));

        AiplatformProjectCostDetailResponse detail =
                costAppService.getProjectCostDetail("3829492007654321", new AiplatformCostQuery(from, to));

        // wire → response 逐字段：subject/窗口回显 + unpriced 档位（bySubject 口径无计数）+ 双分解
        assertThat(detail.projectId()).isEqualTo("3829492007654321");
        assertThat(detail.from()).isEqualTo(from);
        assertThat(detail.cost()).containsEntry("CNY", new BigDecimal("12.3456"));
        assertThat(detail.unpriced().get(0).tokenKind()).isEqualTo(1);
        assertThat(detail.unpriced().get(0).tokenKindName()).isEqualTo("输入");
        assertThat(detail.byModel().get(0).provider()).isEqualTo("anthropic");
        assertThat(detail.byAgentKind().get(0).agentKind()).isEqualTo("executor");
        assertThat(detail.byAgentKind().get(0).agentKindName()).isEqualTo("执行智能体");
    }

    // ========== 成本域错误 · 透传不映射 ==========

    @Test
    void given_meter011FromDownstream_when_getOverview_then_propagateWithoutMapping() {
        when(aiplatformClient.getCostOverview(any())).thenThrow(
                AiplatformUpstreamException.from(
                        new OpenApiClientException(400, "{\"code\":3011,\"message\":\"无效的成本查询参数\",\"data\":null}")));

        assertThatThrownBy(() -> costAppService.overview(
                new AiplatformCostQuery(Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-09-16T00:00:00Z"))))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // METER_011 → HTTP 400 + 数字业务码 3011（域码 3×1000＋11）
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(3011);
                    assertThat(upstream.getMessage()).isEqualTo("无效的成本查询参数");
                });
    }

    // ========== 单价行清单 · query→wire 映射（provider/model 精确过滤）+ 分页 1-based 透传 ==========

    @Test
    void given_priceEntryQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        when(aiplatformClient.listPriceEntries(any(), eq(2), eq(50))).thenReturn(new PageResponse<>(List.of(
                new AiplatformUnitPriceEntryWireResponse(
                        "3830100002222222", "anthropic", "claude-fable-5", 1, "输入",
                        "0.000002", "USD",
                        Instant.parse("2026-08-01T00:00:00Z"), null, "700160", "运营·单价管理员"),
                new AiplatformUnitPriceEntryWireResponse(
                        "3830100001111111", "anthropic", "claude-fable-5", 1, "输入",
                        "0.00000132", "USD",
                        Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
                        null, null),
                new AiplatformUnitPriceEntryWireResponse(
                        "3830100004444444", "imgprov", "qwen-image", 6, "按张",
                        "0.18", "CNY",
                        Instant.parse("2026-01-01T00:00:00Z"), null, null, null)), 12, 2, 50));

        PageResponse<AiplatformUnitPriceEntryResponse> page = priceEntryAppService.list(
                new AiplatformPriceEntryQuery("anthropic", "claude-fable-5"), 2, 50);

        // 出站参数：北向 query → wire 过滤记录逐字段映射；page 1-based 直传（2→2，无 ±1）
        ArgumentCaptor<AiplatformPriceEntryListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformPriceEntryListWireRequest.class);
        verify(aiplatformClient).listPriceEntries(wireCaptor.capture(), eq(2), eq(50));
        assertThat(wireCaptor.getValue())
                .isEqualTo(new AiplatformPriceEntryListWireRequest("anthropic", "claude-fable-5"));

        // 回显取 provider 回报值；wire → response 逐字段——首行当前行（effectiveTo=null、留痕两肢），
        // 次行历史行（区间两端俱全、存量形制操作者 null），第三行按张档当前行（#98 第 6 档：
        // tokenKind=6/按张 直通、unitPrice 每张单价十进制串）
        assertThat(page.total()).isEqualTo(12L);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(50);
        var current = page.items().get(0);
        assertThat(current.id()).isEqualTo("3830100002222222");
        assertThat(current.tokenKind()).isEqualTo(1);
        assertThat(current.tokenKindName()).isEqualTo("输入");
        assertThat(current.unitPrice()).isEqualTo("0.000002");
        assertThat(current.effectiveTo()).isNull();
        assertThat(current.operatorName()).isEqualTo("运营·单价管理员");
        var historical = page.items().get(1);
        assertThat(historical.effectiveTo()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(historical.operatorId()).isNull();
        var perImage = page.items().get(2);
        assertThat(perImage.provider()).isEqualTo("imgprov");
        assertThat(perImage.tokenKind()).isEqualTo(6);
        assertThat(perImage.tokenKindName()).isEqualTo("按张");
        assertThat(perImage.unitPrice()).isEqualTo("0.18");
        assertThat(perImage.currency()).isEqualTo("CNY");
    }

    // ========== 原子改价 · 命令映射 + closed/opened 双行回执映射 ==========

    @Test
    void given_repriceCommand_when_reprice_then_wireCommandMappedAndReceiptMapped() {
        when(aiplatformClient.repricePriceEntry(eq("3830100002222222"), any())).thenReturn(
                new AiplatformUnitPriceEntryRepriceWireResponse(
                        new AiplatformUnitPriceEntryWireResponse(
                                "3830100002222222", "anthropic", "claude-fable-5", 1, "输入",
                                "0.000002", "USD",
                                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-20T00:00:00Z"),
                                "700160", "运营·单价管理员"),
                        new AiplatformUnitPriceEntryWireResponse(
                                "3830100003333333", "anthropic", "claude-fable-5", 1, "输入",
                                "0.0000018", "USD",
                                Instant.parse("2026-09-20T00:00:00Z"), null,
                                "3829492001234567", "调价运营")));

        AiplatformUnitPriceEntryRepriceResponse receipt = priceEntryAppService.reprice(
                "3830100002222222", new AiplatformPriceEntryRepriceCommand(
                        new BigDecimal("0.0000018"), "USD", Instant.parse("2026-09-20T00:00:00Z")));

        // 出站参数：北向命令 → wire 命令体逐字段映射（BigDecimal/Instant 原值；null 不代填）
        ArgumentCaptor<AiplatformPriceEntryRepriceWireRequest> commandCaptor =
                ArgumentCaptor.forClass(AiplatformPriceEntryRepriceWireRequest.class);
        verify(aiplatformClient).repricePriceEntry(eq("3830100002222222"), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new AiplatformPriceEntryRepriceWireRequest(
                new BigDecimal("0.0000018"), "USD", Instant.parse("2026-09-20T00:00:00Z")));

        // wire → response 逐字段：closed 落 effectiveTo、保留原开行留痕；opened 新 id、敞口、带改价操作者
        assertThat(receipt.closed().effectiveTo()).isEqualTo(Instant.parse("2026-09-20T00:00:00Z"));
        assertThat(receipt.closed().operatorId()).isEqualTo("700160");
        assertThat(receipt.opened().id()).isEqualTo("3830100003333333");
        assertThat(receipt.opened().unitPrice()).isEqualTo("0.0000018");
        assertThat(receipt.opened().effectiveTo()).isNull();
        assertThat(receipt.opened().operatorName()).isEqualTo("调价运营");
    }

    // ========== 停用 · 单行回执映射 ==========

    @Test
    void given_deactivateReceipt_when_deactivate_then_singleRowMapped() {
        when(aiplatformClient.deactivatePriceEntry("3830100002222222")).thenReturn(
                new AiplatformUnitPriceEntryWireResponse(
                        "3830100002222222", "anthropic", "claude-fable-5", 1, "输入",
                        "0.000002", "USD",
                        Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-16T10:30:00Z"),
                        "3829492001234567", "调价运营"));

        AiplatformUnitPriceEntryResponse closed = priceEntryAppService.deactivate("3830100002222222");

        // 被关行单行回执：effectiveTo 已落、停用操作者落被关行（唯一落点）
        assertThat(closed.effectiveTo()).isEqualTo(Instant.parse("2026-09-16T10:30:00Z"));
        assertThat(closed.operatorId()).isEqualTo("3829492001234567");
        assertThat(closed.operatorName()).isEqualTo("调价运营");
    }

    // ========== 单价表域错误 · 透传不映射 ==========

    @Test
    void given_meter008FromDownstream_when_reprice_then_propagateWithoutMapping() {
        when(aiplatformClient.repricePriceEntry(eq("3830100002222222"), any())).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(409,
                        "{\"code\":3008,\"message\":\"同匹配键生效区间重叠（跨区间或同起点）\",\"data\":null}")));

        assertThatThrownBy(() -> priceEntryAppService.reprice("3830100002222222",
                new AiplatformPriceEntryRepriceCommand(
                        new BigDecimal("0.0000018"), "USD", Instant.parse("2026-09-20T00:00:00Z"))))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // METER_008 → HTTP 409 + 数字业务码 3008（域码 3×1000＋8）
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(3008);
                    assertThat(upstream.getMessage()).isEqualTo("同匹配键生效区间重叠（跨区间或同起点）");
                });
    }

    // ========== 知识素材清单 · query→wire 映射（三维过滤）+ 分页 1-based 透传 ==========

    @Test
    void given_materialQueryAndPage_when_list_then_wireRequestMappedAndPageEchoedFromProvider() {
        Instant sunkFrom = Instant.parse("2026-09-01T00:00:00Z");
        Instant sunkTo = Instant.parse("2026-09-15T23:59:59Z");
        when(aiplatformClient.listMaterials(any(), eq(2), eq(20))).thenReturn(new PageResponse<>(List.of(
                new AiplatformMaterialSummaryWireResponse(
                        "3840600001111111", "PRD", "3829492007654321", "英语学习助手",
                        "英语学习助手 · PRD", 2, "停用",
                        Instant.parse("2026-09-01T08:30:00Z"),
                        "3829492001234567", "内容治理运营"),
                new AiplatformMaterialSummaryWireResponse(
                        "3840600002222222", "PRD", "3829492005555444", "待办清单应用",
                        "待办清单应用 · PRD", 1, "启用",
                        Instant.parse("2026-08-20T12:00:00Z"),
                        null, null)), 9, 2, 20));

        PageResponse<AiplatformMaterialSummaryResponse> page = materialAppService.list(
                new AiplatformMaterialQuery(2, sunkFrom, sunkTo, "3829492007654321"), 2, 20);

        // 出站参数：北向 query → wire 过滤记录逐字段映射（status 单选 + 沉淀时间闭区间 + projectId 精确）；
        // page 1-based 直传（2→2，无 ±1）
        ArgumentCaptor<AiplatformMaterialListWireRequest> wireCaptor =
                ArgumentCaptor.forClass(AiplatformMaterialListWireRequest.class);
        verify(aiplatformClient).listMaterials(wireCaptor.capture(), eq(2), eq(20));
        assertThat(wireCaptor.getValue()).isEqualTo(new AiplatformMaterialListWireRequest(
                2, sunkFrom, sunkTo, "3829492007654321"));

        // 回显取 provider 回报值；wire → response 逐字段——首行已治理（status Integer + *Name、
        // 操作者留痕两肢），次行未治理（操作者两列 null 如实映射）
        assertThat(page.total()).isEqualTo(9L);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        var governed = page.items().get(0);
        assertThat(governed.id()).isEqualTo("3840600001111111");
        assertThat(governed.kind()).isEqualTo("PRD");
        assertThat(governed.projectId()).isEqualTo("3829492007654321");
        assertThat(governed.projectName()).isEqualTo("英语学习助手");
        assertThat(governed.status()).isEqualTo(2);
        assertThat(governed.statusName()).isEqualTo("停用");
        assertThat(governed.sunkAt()).isEqualTo(Instant.parse("2026-09-01T08:30:00Z"));
        assertThat(governed.operatorId()).isEqualTo("3829492001234567");
        var untreated = page.items().get(1);
        assertThat(untreated.status()).isEqualTo(1);
        assertThat(untreated.statusName()).isEqualTo("启用");
        assertThat(untreated.operatorId()).isNull();
        assertThat(untreated.operatorName()).isNull();
    }

    // ========== 素材详情 · 元数据 + PRD 全文映射 ==========

    @Test
    void given_materialDetailWire_when_getDetail_then_metadataAndContentMapped() {
        when(aiplatformClient.getMaterial("3840600001111111")).thenReturn(
                new AiplatformMaterialDetailWireResponse(
                        "3840600001111111", "PRD", "3829492007654321", "英语学习助手",
                        "英语学习助手 · PRD", 1, "启用",
                        Instant.parse("2026-09-01T08:30:00Z"),
                        null, null,
                        "# PRD\n\n做一个英语学习助手……\n\n## 功能范围\n\n1. 单词卡片\n2. 复习计划"));

        AiplatformMaterialDetailResponse detail = materialAppService.getDetail("3840600001111111");

        // wire → response 逐字段：元数据（与清单行同形）＋全文 content 原样到达（块拼接形态不改写）
        assertThat(detail.id()).isEqualTo("3840600001111111");
        assertThat(detail.kind()).isEqualTo("PRD");
        assertThat(detail.status()).isEqualTo(1);
        assertThat(detail.statusName()).isEqualTo("启用");
        assertThat(detail.sunkAt()).isEqualTo(Instant.parse("2026-09-01T08:30:00Z"));
        assertThat(detail.operatorId()).isNull();
        assertThat(detail.content()).startsWith("# PRD");
        assertThat(detail.content()).contains("\n\n## 功能范围\n\n1. 单词卡片\n2. 复习计划");
    }

    // ========== 三治理动作 · 委托 + 回执映射（删除＝删除前终态） ==========

    @Test
    void given_governanceReceipts_when_disableEnableDelete_then_summaryReceiptsMapped() {
        when(aiplatformClient.disableMaterial("3840600001111111")).thenReturn(
                new AiplatformMaterialSummaryWireResponse(
                        "3840600001111111", "PRD", "3829492007654321", "英语学习助手",
                        "英语学习助手 · PRD", 2, "停用",
                        Instant.parse("2026-09-01T08:30:00Z"),
                        "3829492001234567", "内容治理运营"));
        when(aiplatformClient.enableMaterial("3840600001111111")).thenReturn(
                new AiplatformMaterialSummaryWireResponse(
                        "3840600001111111", "PRD", "3829492007654321", "英语学习助手",
                        "英语学习助手 · PRD", 1, "启用",
                        Instant.parse("2026-09-01T08:30:00Z"),
                        "3829492001234567", "内容治理运营"));
        when(aiplatformClient.deleteMaterial("3840600001111111")).thenReturn(
                new AiplatformMaterialSummaryWireResponse(
                        "3840600001111111", "PRD", "3829492007654321", "英语学习助手",
                        "英语学习助手 · PRD", 2, "停用",
                        Instant.parse("2026-09-01T08:30:00Z"),
                        "3829492001234567", "内容治理运营"));

        // 停用回执：provider 重读登记行——已停用 + 操作者落素材级
        AiplatformMaterialSummaryResponse disabled = materialAppService.disable("3840600001111111");
        assertThat(disabled.status()).isEqualTo(2);
        assertThat(disabled.statusName()).isEqualTo("停用");
        assertThat(disabled.operatorId()).isEqualTo("3829492001234567");
        assertThat(disabled.operatorName()).isEqualTo("内容治理运营");

        // 启用回执：恢复启用（可逆开关的另一侧）
        AiplatformMaterialSummaryResponse enabled = materialAppService.enable("3840600001111111");
        assertThat(enabled.status()).isEqualTo(1);
        assertThat(enabled.statusName()).isEqualTo("启用");

        // 删除回执：删除前终态（本例是一行已停用素材——留痕原样在场，确认移除了什么）
        AiplatformMaterialSummaryResponse deleted = materialAppService.delete("3840600001111111");
        assertThat(deleted.id()).isEqualTo("3840600001111111");
        assertThat(deleted.status()).isEqualTo(2);
        assertThat(deleted.operatorName()).isEqualTo("内容治理运营");
    }

    // ========== 知识素材域错误 · 透传不映射 ==========

    @Test
    void given_knw006FromDownstream_when_disable_then_propagateWithoutMapping() {
        when(aiplatformClient.disableMaterial("3840600001111111")).thenThrow(
                AiplatformUpstreamException.from(new OpenApiClientException(400,
                        "{\"code\":2006,\"message\":\"操作者不能为空\",\"data\":null}")));

        assertThatThrownBy(() -> materialAppService.disable("3840600001111111"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    // KNW_006 → HTTP 400 + 数字业务码 2006（域码 2×1000＋6，操作者必留痕守卫）
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(2006);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }
}
