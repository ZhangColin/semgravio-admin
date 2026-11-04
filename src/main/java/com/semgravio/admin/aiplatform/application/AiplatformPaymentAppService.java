package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentNodesCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformPaymentVerifyCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentNodeResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentPlanResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentVerificationResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentVoucherContentResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformPaymentVoucherResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentNodeWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentNodesWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentPlanDetailWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentPlanWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentRejectWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVerificationWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVerifyWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformPaymentVoucherWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import com.cartisan.openapi.client.BinaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * aiplatform 付款核验域 BFF 应用服务（issue #106，spec #102 A 批 A3——provider aiplatform
 * #337 凭证核验承接）：付款计划详情（核验承接全量面）＋节点录入/开工门设定＋到账核验＋
 * 打回＋凭证内容二进制直出，五端点。
 *
 * <p><strong>命名注意</strong>：本服务是 aiplatform 付款核验域（合同冻结后的付款计划人工
 * 承接），与既有支付服务 BFF 上下文（{@code com.semgravio.admin.payment}，调
 * semgravio-payment 支付服务）<strong>无关联</strong>——两处 payment 词同形不同域，勿混读
 * （spec #102 Further Notes）。</p>
 *
 * <p>admin 作为 BFF：调接口 + DTO 转换，不持业务逻辑、不持付款数据、不记业务审计
 * （append-only 核验留痕归 provider）。下游错误已由 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException}（provider 信封原样透传），本层不再 try/catch。节点
 * 五态、全额判定（累计核到账 ≥ 节点金额自动转已到账、不足留痕回待支付）、打回不动累计、
 * 重录窗口（支付未开始）等语义全部由 provider 承担，本层不解释不预判不复算。核验/打回
 * 操作者身份经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传
 * （AppService 不经手），provider 落痕核验留痕行（缺头 ORD_043 拒）。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformPaymentAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformPaymentAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 付款计划详情（透传 aiplatform）——核验承接全量面：活跃计划全量（节点五态/凭证清单）
     * ＋核验留痕时间线（新→旧，append-only）＋历史计划行摘要（旧计划随旧合同留痕）。
     */
    public AiplatformPaymentPlanDetailResponse getPlanDetail(String orderId) {
        return toDetail(aiplatformClient.getPaymentPlan(orderId));
    }

    /**
     * 付款节点录入（透传 aiplatform）——整组替换（含开工门设定）：序号按列表序派生、附言码
     * 自合同号定格。重录窗口（支付活动未开始）守卫归 provider。
     */
    public AiplatformPaymentPlanResponse enterNodes(String orderId, AiplatformPaymentNodesCommand command) {
        return toPlan(aiplatformClient.enterPaymentNodes(orderId, toWire(command)));
    }

    /**
     * 到账核验（透传 aiplatform）——实到金额/时间/关联凭证/渠道流水号留痕；全额判定（累计
     * 核到账 ≥ 节点金额自动转已到账、不足留痕回待支付）归 provider 裁决。
     */
    public AiplatformPaymentPlanResponse verify(String orderId, String nodeId,
            AiplatformPaymentVerifyCommand command) {
        return toPlan(aiplatformClient.verifyPayment(orderId, nodeId, new AiplatformPaymentVerifyWireRequest(
                command.actualAmount(), command.arrivedAt(), command.voucherId(),
                command.channelRefNo(), command.basis())));
    }

    /**
     * 打回（透传 aiplatform）——回待支付＋依据必填留痕；打回不动累计已核到账（守卫归 provider）。
     */
    public AiplatformPaymentPlanResponse reject(String orderId, String nodeId,
            AiplatformPaymentRejectCommand command) {
        return toPlan(aiplatformClient.rejectPayment(orderId, nodeId, new AiplatformPaymentRejectWireRequest(
                command.voucherId(), command.basis())));
    }

    /**
     * 凭证内容（透传 aiplatform）——后台核验看证的二进制直出：文件字节＋provider 响应头
     * raw 值（Content-Type 真实媒体类型 + Content-Disposition attachment 文件名），无
     * ApiResponse 信封（照源码包/设计资产包范式）。
     */
    public AiplatformPaymentVoucherContentResponse getVoucherContent(String voucherId) {
        BinaryResponse binary = aiplatformClient.downloadVoucherContent(voucherId);
        // HttpHeaders 大小写不敏感取值；provider 契约恒带两头——缺头兜底为通用二进制附件（HTTP 合法性，
        // 非契约发明），同源码包/设计资产包口径
        String contentType = binary.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String contentDisposition = binary.headers().firstValue(HttpHeaders.CONTENT_DISPOSITION)
                .orElse("attachment");
        return new AiplatformPaymentVoucherContentResponse(binary.body(), contentType, contentDisposition);
    }

    private static AiplatformPaymentNodesWireRequest toWire(AiplatformPaymentNodesCommand command) {
        List<AiplatformPaymentNodesWireRequest.NodeSpecWire> specs = command.nodes() == null ? null
                : command.nodes().stream()
                        .map(spec -> new AiplatformPaymentNodesWireRequest.NodeSpecWire(
                                spec.name(), spec.amount(), spec.kickoffGate()))
                        .toList();
        return new AiplatformPaymentNodesWireRequest(specs);
    }

    /** wire 三段 → 北向三段：plan 逐位直通＋留痕行逐位直通＋历史行逐位直通。 */
    private static AiplatformPaymentPlanDetailResponse toDetail(AiplatformPaymentPlanDetailWireResponse wire) {
        return new AiplatformPaymentPlanDetailResponse(
                toPlan(wire.plan()),
                wire.trails() == null ? null : wire.trails().stream()
                        .map(AiplatformPaymentAppService::toVerification)
                        .toList(),
                wire.history() == null ? null : wire.history().stream()
                        .map(row -> new AiplatformPaymentPlanDetailResponse.HistoryPlanResponse(
                                row.id(), row.contractId(), row.contractNo(), row.nodeCount(),
                                row.totalAmount(), row.totalSettled(), row.createdAt()))
                        .toList());
    }

    private static AiplatformPaymentPlanResponse toPlan(AiplatformPaymentPlanWireResponse wire) {
        return new AiplatformPaymentPlanResponse(
                wire.id(), wire.orderId(), wire.contractId(), wire.contractNo(),
                wire.totalAmount(), wire.totalSettled(), wire.createdAt(),
                wire.nodes() == null ? null : wire.nodes().stream()
                        .map(AiplatformPaymentAppService::toNode)
                        .toList());
    }

    private static AiplatformPaymentNodeResponse toNode(AiplatformPaymentNodeWireResponse wire) {
        return new AiplatformPaymentNodeResponse(
                wire.id(), wire.seq(), wire.name(), wire.amount(), wire.percentage(),
                wire.status(), wire.statusName(), wire.remarkCode(), wire.kickoffGate(),
                wire.channelType(), wire.channelTypeName(), wire.channelRefNo(),
                wire.settledAmount(), wire.remainingAmount(), wire.receivedAt(),
                wire.vouchers() == null ? null : wire.vouchers().stream()
                        .map(AiplatformPaymentAppService::toVoucher)
                        .toList());
    }

    private static AiplatformPaymentVoucherResponse toVoucher(AiplatformPaymentVoucherWireResponse wire) {
        return new AiplatformPaymentVoucherResponse(
                wire.id(), wire.nodeId(), wire.fileName(), wire.contentType(),
                wire.byteSize(), wire.createdAt());
    }

    private static AiplatformPaymentVerificationResponse toVerification(
            AiplatformPaymentVerificationWireResponse wire) {
        return new AiplatformPaymentVerificationResponse(
                wire.id(), wire.nodeId(), wire.action(), wire.actionName(),
                wire.fromStatus(), wire.toStatus(), wire.actualAmount(), wire.arrivedAt(),
                wire.voucherId(), wire.basis(), wire.operatorId(), wire.operatorName(),
                wire.createdAt());
    }
}
