package com.aieducenter.admin.aiplatform.infrastructure;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.aieducenter.admin.aiplatform.application.AiplatformUpstreamException;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAccountProfileWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigTraceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigUpdateWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentConfigWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolSlotWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolToggleWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformConversationEntryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformCostOverviewWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformCostWindowWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformMaterialDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformMaterialListWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformMaterialSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderCancelWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderListWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderQuoteWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformOrderWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPriceEntryListWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPriceEntryRepriceWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformPrdWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillAssignWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillInstallWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillPrecheckWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateTraceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSlotAssignmentWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectCostDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectCostWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectFileContentWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectFilesWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectListWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformProjectSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryRepriceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnitPriceEntryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformUnpricedUsageWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformVersionDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformVersionWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformWorkspaceDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformWorkspaceListWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformWorkspaceSummaryWireResponse;
import com.cartisan.openapi.client.BinaryResponse;
import com.cartisan.openapi.client.OpenApiClient;
import com.cartisan.openapi.client.OpenApiClientException;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * aiplatform 签名 HTTP 客户端——封装 {@link OpenApiClient}，屏蔽 wire 层细节（issue #63 T1 基座）。
 *
 * <p>所有对 aiplatform 后台管理面（{@code /api/backoffice/**}，机机签名）的调用都经此客户端发起，
 * 自动签名（admin 自身 apiKey/admin-console，复用框架 {@link OpenApiClient} 既有 HMAC-SHA256
 * 五头签名，无需新凭据——见 ADR-0007）。操作者身份经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传，aiplatform 据此落审计列。</p>
 *
 * <p>为 infrastructure 包内裸 {@code @Component}（BFF 出站客户端，不走 {@code @Port/@Adapter}，
 * 详见 ADR-0007，与 {@code PaymentClient} / {@code AccountClient} / {@code AppRegistryClient} 同款）。
 * 六域（订单/项目/沙箱/成本/单价表/知识素材）外加账号读口与技能管理域（#78 第七域）共用本
 * 客户端——同一 downstream、同一签名身份，各域只加方法。</p>
 *
 * <p>信封与错误契约（spec #62 定稿）：aiplatform 出口统一 {@code ApiResponse<T>}，本客户端解包取
 * {@code .data()}；下游 ≥400 时框架抛 {@link OpenApiClientException}，此处统一翻译为
 * {@link AiplatformUpstreamException}（provider 数字业务码 + HTTP 状态 + message 原样透传，
 * 不做 BaseCodeMessage 映射）——六域 AppService 无需逐处 try/catch。</p>
 *
 * @since 0.1.0
 */
@Component
public class AiplatformClient {

    private static final Logger log = LoggerFactory.getLogger(AiplatformClient.class);

    // aiplatform 账号读口返回 ApiResponse<BackofficeAccountProfileResponse> 信封（{code,message,data}），
    // 须按信封反序列化并取 .data()（与 payment/identity 列表端点同款，#63 wire 契约测试钉死）。
    private static final TypeReference<ApiResponse<AiplatformAccountProfileWireResponse>> ACCOUNT_PROFILE_TYPEREF =
            new TypeReference<>() {};

    // 订单清单返回 ApiResponse<PageResponse<BackofficeOrderSummaryResponse>> 信封（data 内 {items,total,page,size}，
    // page 1-based）——须按信封反序列化并取 .data()（payment PAYMENT_PAGE_TYPEREF 同款隐患口径）。
    private static final TypeReference<ApiResponse<PageResponse<AiplatformOrderSummaryWireResponse>>> ORDER_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformOrderDetailWireResponse>> ORDER_DETAIL_TYPEREF =
            new TypeReference<>() {};

    // 订单写路径（#29 交易环②报价改价 + #157 运营取消 + #158 重试归档）三操作共用回执：
    // ApiResponse<OrderResponse>（provider 用户面同构 DTO——五字段价目行，区别于后台详情七字段行）
    private static final TypeReference<ApiResponse<AiplatformOrderWireResponse>> ORDER_RECEIPT_TYPEREF =
            new TypeReference<>() {};

    // 项目域（#159/#162）同为 ApiResponse<T> 信封；对话史/版本列表的 data 是裸 List（非分页载体）
    private static final TypeReference<ApiResponse<PageResponse<AiplatformProjectSummaryWireResponse>>> PROJECT_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformProjectDetailWireResponse>> PROJECT_DETAIL_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<List<AiplatformConversationEntryWireResponse>>> CONVERSATION_LIST_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformPrdWireResponse>> PROJECT_PRD_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<List<AiplatformVersionWireResponse>>> VERSION_LIST_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformVersionDetailWireResponse>> VERSION_DETAIL_TYPEREF =
            new TypeReference<>() {};

    // 项目文件区（#163）：文件树/文件内容同为 ApiResponse<T> 信封；文件包为二进制流无信封（download）
    private static final TypeReference<ApiResponse<AiplatformProjectFilesWireResponse>> PROJECT_FILES_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformProjectFileContentWireResponse>> PROJECT_FILE_CONTENT_TYPEREF =
            new TypeReference<>() {};

    // 沙箱域（#173 观测 + #174 动作）同为 ApiResponse<T> 信封；四干预动作无请求体、回执＝详情 DTO
    private static final TypeReference<ApiResponse<PageResponse<AiplatformWorkspaceSummaryWireResponse>>> WORKSPACE_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformWorkspaceDetailWireResponse>> WORKSPACE_DETAIL_TYPEREF =
            new TypeReference<>() {};

    // 成本域（#161/#164 观测）同为 ApiResponse<T> 信封；四端点均为读口（时间窗 + 可选分页）
    private static final TypeReference<ApiResponse<AiplatformCostOverviewWireResponse>> COST_OVERVIEW_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformUnpricedUsageWireResponse>> UNPRICED_USAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<PageResponse<AiplatformProjectCostWireResponse>>> PROJECT_COST_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformProjectCostDetailWireResponse>> PROJECT_COST_DETAIL_TYPEREF =
            new TypeReference<>() {};

    // 单价表域（#160 成本运营＋#165 写口唯一化）同为 ApiResponse<T> 信封；清单 data 为 PageResponse
    // （page 1-based、total JSON string），改价回执为 {closed, opened} 双行——provider 的「开行」端点
    // 不建北向（种子脚本通道，spec #62），本客户端也不加对应方法
    private static final TypeReference<ApiResponse<PageResponse<AiplatformUnitPriceEntryWireResponse>>> PRICE_ENTRY_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformUnitPriceEntryWireResponse>> PRICE_ENTRY_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformUnitPriceEntryRepriceWireResponse>> PRICE_ENTRY_REPRICE_TYPEREF =
            new TypeReference<>() {};

    // 知识素材域（#166 知识库管理）同为 ApiResponse<T> 信封；清单 data 为 PageResponse（page 1-based），
    // 详情含素材全文，三治理动作（停用/启用/删除）回执共形＝summary 单行（删除回执＝删除前终态）
    private static final TypeReference<ApiResponse<PageResponse<AiplatformMaterialSummaryWireResponse>>> MATERIAL_PAGE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformMaterialDetailWireResponse>> MATERIAL_DETAIL_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformMaterialSummaryWireResponse>> MATERIAL_SUMMARY_TYPEREF =
            new TypeReference<>() {};

    // 技能管理域（#247 技能线 T1/T2）同为 ApiResponse<T> 信封；清单 data 为裸 List（有界目录不分页
    // ——对话史 List 直出同形），详情含 frontmatter 全量＋正文全文（审核承载面）。安装回执与清单
    // 裸 List 共形（#248——本次装入的条目清单），启停/卸载回执为清单行单行
    private static final TypeReference<ApiResponse<List<AiplatformSkillSummaryWireResponse>>> SKILL_LIST_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformSkillDetailWireResponse>> SKILL_DETAIL_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformSkillSummaryWireResponse>> SKILL_SUMMARY_TYPEREF =
            new TypeReference<>() {};

    // 槽位指派（#249 技能线 T3）：GET 读面与 PUT 回执同形——{slot, skills[]} 单对象（skills
    // 与清单行共形、含停用行）
    private static final TypeReference<ApiResponse<AiplatformSlotAssignmentWireResponse>> SLOT_ASSIGNMENT_TYPEREF =
            new TypeReference<>() {};

    // 指派双头预检（#255，技能域第 16 端点）：POST 回执为单对象 {executed, hints[]}——两态
    // 纪律回执（executed=false＝未执行，非无重叠），hints 恒至少空表（provider 构造器归一）
    private static final TypeReference<ApiResponse<AiplatformSkillPrecheckWireResponse>> SKILL_PRECHECK_TYPEREF =
            new TypeReference<>() {};

    // 显式更新＋版本留痕（#250 技能线 T4）：POST /update 回执为单对象（from/to＋removedSkillNames
    // ＋终态行集——skills 与清单行共形）；GET /update-traces data 为裸 List（append-only 时间倒序，
    // 有界——同技能清单 List 直出同形）
    private static final TypeReference<ApiResponse<AiplatformSkillUpdateWireResponse>> SKILL_UPDATE_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<List<AiplatformSkillUpdateTraceWireResponse>>> SKILL_UPDATE_TRACE_LIST_TYPEREF =
            new TypeReference<>() {};

    // 智能体配置域（#251 技能线 T5）：GET 读面与 PUT 回执同形——单对象（生效值＋覆盖标记＋
    // 默认预览＋工具开关＋最近写者）；GET /traces data 为裸 List（append-only 时间倒序——
    // 与技能更新留痕 List 直出同形，不造分页）
    private static final TypeReference<ApiResponse<AiplatformAgentConfigWireResponse>> AGENT_CONFIG_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<List<AiplatformAgentConfigTraceWireResponse>>> AGENT_CONFIG_TRACE_LIST_TYPEREF =
            new TypeReference<>() {};

    // 工具面域（#252 技能线 T6）：GET 清单 data 为裸 List（三槽位有界——main/executor/subagent）；
    // PUT 开关回执为单工具行（与清单嵌套行同形——provider 回执即该工具行）
    private static final TypeReference<ApiResponse<List<AiplatformAgentToolSlotWireResponse>>> AGENT_TOOL_SLOT_LIST_TYPEREF =
            new TypeReference<>() {};

    private static final TypeReference<ApiResponse<AiplatformAgentToolWireResponse>> AGENT_TOOL_TYPEREF =
            new TypeReference<>() {};

    // 出站时间参数定长格式（秒恒在场）：LocalDateTime.toString() 会省略零秒（"T00:00"），
    // provider 的 @DateTimeFormat(iso=DATE_TIME) 可解析两种形式，但 wire 形状取确定性的全秒形
    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private final OpenApiClient openApiClient;
    private final String baseUrl;

    public AiplatformClient(OpenApiClient openApiClient,
                            @Value("${admin.aiplatform.base-url}") String baseUrl) {
        this.openApiClient = openApiClient;
        this.baseUrl = baseUrl;
    }

    /**
     * 查询账号极简档案（透传 aiplatform）——按 externalId 查 id/externalId/displayName/createdAt 四字段。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/accounts/{externalId}}（#154 已冻结）：返回
     * {@code ApiResponse<BackofficeAccountProfileResponse>}；externalId 未命中时 HTTP 404 +
     * 数字业务码 6004（IDN_004）——原样经 {@link AiplatformUpstreamException} 透传北向。</p>
     *
     * @param externalId 外部身份标识（OIDC sub＝identity 账户 Id）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 IDN_004 等，不做映射）
     */
    public AiplatformAccountProfileWireResponse getAccountProfile(String externalId) {
        String url = baseUrl + "/api/backoffice/accounts/" + encode(externalId);
        log.debug("AiplatformClient.getAccountProfile: {}", url);
        try {
            ApiResponse<AiplatformAccountProfileWireResponse> resp = openApiClient.get(url, ACCOUNT_PROFILE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询订单清单（透传 aiplatform，四维检索）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/orders}（#156 四维已冻结）：返回
     * {@code ApiResponse<PageResponse<BackofficeOrderSummaryResponse>>}。四维可组合、均可缺省；
     * externalId 换算不到/非数值 orderId 时 provider 如实返回空清单 200（无命非错误）；
     * 过滤参数绑定失败 400 ORD_010（数字业务码 5010）原样透传。</p>
     *
     * <p>分页（spec #62，平台分页统一决议目标态；#73 起全平台收口同语义，见 ADR-0012）：
     * aiplatform wire 本就 <strong>1-based</strong>，page 直传 <strong>零换算</strong>（与 payment
     * #22 迁移后的同值直传同语义）；provider 侧 {@code BackofficePages} clamp（page≥1、size∈[1,100]）
     * 行为透传，本客户端不重复夹取。
     * {@code status} 多选拼为逗号分隔单值（{@code status=1,5}）——签名协议按参数名去重，
     * 禁用重复参数展开。</p>
     *
     * @param filter wire 层过滤参数（由应用层从 {@code AiplatformOrderQuery} 映射而来）
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 ORD_010 等，不做映射）
     */
    public PageResponse<AiplatformOrderSummaryWireResponse> listOrders(AiplatformOrderListWireRequest filter,
                                                                       int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/orders?page=").append(page)
                .append("&size=").append(size);
        if (filter.status() != null && !filter.status().isEmpty()) {
            // 逗号分隔单值（provider 签名按参数名去重，status=1&status=2 只有末值入签——禁用）
            appendParam(url, "status",
                    filter.status().stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        appendParam(url, "createdFrom", filter.createdFrom());
        appendParam(url, "createdTo", filter.createdTo());
        appendParam(url, "externalId", filter.externalId());
        appendParam(url, "orderId", filter.orderId());
        log.debug("AiplatformClient.listOrders: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformOrderSummaryWireResponse>> resp =
                    openApiClient.get(url.toString(), ORDER_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询订单详情（透传 aiplatform）——报价依据全量：PRD 快照、价目历史（append-only 带操作者）、
     * 状态时点组。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/orders/{id}}（#155 已冻结）：返回
     * {@code ApiResponse<BackofficeOrderDetailResponse>}；订单不存在时 HTTP 404 + 数字业务码
     * 5001（ORD_001）原样透传。</p>
     *
     * @param id 订单标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 ORD_001 等，不做映射）
     */
    public AiplatformOrderDetailWireResponse getOrder(String id) {
        String url = baseUrl + "/api/backoffice/orders/" + encode(id);
        log.debug("AiplatformClient.getOrder: {}", url);
        try {
            ApiResponse<AiplatformOrderDetailWireResponse> resp = openApiClient.get(url, ORDER_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 下载订单源码包（透传 aiplatform）——tar.gz 二进制流，经项目工作区实时打包。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/orders/{id}/source-package}：<strong>无
     * ApiResponse 信封</strong>（{@code application/gzip} + Content-Disposition 文件名），经
     * cartisan-openapi {@link OpenApiClient#download}（cartisan-boot#30）取回原始字节 + 响应头——
     * ofByteArray 保全 gzip 字节（ofString 会经 UTF-8 解码不可逆损坏）。内存中转、不落盘。</p>
     *
     * <p>订单不存在 404 ORD_001（5001）、打包失败 500 WSP_002——错误信封在 body 里，
     * {@code download} 内部抛 {@link OpenApiClientException}，此处同款翻译透传。</p>
     *
     * @param id 订单标识（TSID 十进制字符串）
     * @return 二进制响应载体（HTTP 状态 + 响应头 raw 值 + 原始字节）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 ORD_001 / 500 WSP_002 等，不做映射）
     */
    public BinaryResponse downloadSourcePackage(String id) {
        String url = baseUrl + "/api/backoffice/orders/" + encode(id) + "/source-package";
        log.debug("AiplatformClient.downloadSourcePackage: {}", url);
        try {
            return openApiClient.download(url);
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 提交报价/改价（透传 aiplatform）——「已报价态重复提交＝改价」语义由 provider 承担
     * （待报价态首次提交＝报价→已报价；已报价态重复提交＝改价，状态不变、append-only 价目行
     * 留痕、订单现值取最新行），BFF 不解释不预判。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/orders/{id}/quote}（#29 交易环②）：请求体
     * {@code {amount, note}} 逐字镜像（amount Long 分 → JSON string，全局 Long→ToStringSerializer
     * 出口口径——provider 默认 string→Long 强转可回读；note 可空在场），返回
     * {@code ApiResponse<OrderResponse>}（用户面同构回执，含 append-only 改价历史）。操作者身份
     * 经框架 {@code OpenApiClient} 自动带 {@code X-User-Id/X-User-Name} 头（RequestContext→
     * provider 落痕价目行，缺头落空）。订单不存在 404 ORD_001（5001）；已支付/已终结
     * 409 ORD_007（5007）；金额非正 400 ORD_008（5008）；备注超长 400 ORD_009（5009）
     * ——均原样透传。</p>
     *
     * @param id      订单标识（TSID 十进制字符串，provider 侧 lenient 解析：非数值同 404 ORD_001）
     * @param command 报价命令体（由应用层从北向命令映射而来，字段合法性归 provider 聚合守卫）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（ORD_001/007/008/009 等，不做映射）
     */
    public AiplatformOrderWireResponse quoteOrder(String id, AiplatformOrderQuoteWireRequest command) {
        String url = baseUrl + "/api/backoffice/orders/" + encode(id) + "/quote";
        log.debug("AiplatformClient.quoteOrder: {}", url);
        try {
            ApiResponse<AiplatformOrderWireResponse> resp = openApiClient.post(url, command, ORDER_RECEIPT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 运营取消订单（透传 aiplatform）——限未支付态，语义与用户取消完全一致：订单落已取消、
     * 项目解冻回迭代、用户可继续对话与再次下单。取消原因必填（运营内部口径留档，不呈现
     * 任何用户面读面）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/orders/{id}/cancel}（#157）：请求体
     * {@code {reason}} 逐字镜像，返回 {@code ApiResponse<OrderResponse>}（已取消终态回执，
     * cancelledAt 落定）。操作者身份经框架 {@code OpenApiClient} 自动带
     * {@code X-User-Id/X-User-Name} 头（RequestContext→provider 落痕订单行，缺头落空）。
     * 订单不存在 404 ORD_001（5001）；已支付/已归档/已取消 409 ORD_005（5005，退款/售后
     * 另议）；原因缺失 400 ORD_013（5013）；原因超长（至多 1000 字）400 ORD_014（5014）
     * ——均原样透传。</p>
     *
     * @param id      订单标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @param command 取消命令体（由应用层从北向命令映射而来，字段合法性归 provider 聚合守卫）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（ORD_001/005/013/014 等，不做映射）
     */
    public AiplatformOrderWireResponse cancelOrder(String id, AiplatformOrderCancelWireRequest command) {
        String url = baseUrl + "/api/backoffice/orders/" + encode(id) + "/cancel";
        log.debug("AiplatformClient.cancelOrder: {}", url);
        try {
            ApiResponse<AiplatformOrderWireResponse> resp = openApiClient.post(url, command, ORDER_RECEIPT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 重试归档（透传 aiplatform）——已支付未归档卡单的手动补完结：一事务内订单落已归档＋
     * 项目归档，成功后补发「已归档」通知并触发知识沉淀（成交 PRD 入知识库，best-effort
     * 不炸主流程）。幂等由既有守卫保证。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/orders/{id}/retry-archive}（#158）：
     * <strong>无请求体</strong>（框架 {@code OpenApiClient.post} 对 null body 发空 body，
     * provider 端只读路径参数——同沙箱四动作/单价表停用先例），返回
     * {@code ApiResponse<OrderResponse>}（已归档终态回执，paidAt/archivedAt 双时点）。
     * 操作者身份经框架 {@code OpenApiClient} 自动带 {@code X-User-Id/X-User-Name} 头
     * （RequestContext→provider 落痕订单行，缺头落空；支付链自动归档操作者为空）。订单
     * 不存在 404 ORD_001（5001）；重复触发/非已支付态 409 ORD_012（5012）；项目已归档
     * 409 <strong>PRJ_013</strong>（4013，跨 BC 既有码透传——不产生重复素材）——均原样
     * 透传。</p>
     *
     * @param id 订单标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（ORD_001/012、PRJ_013 等，不做映射）
     */
    public AiplatformOrderWireResponse retryArchiveOrder(String id) {
        String url = baseUrl + "/api/backoffice/orders/" + encode(id) + "/retry-archive";
        log.debug("AiplatformClient.retryArchiveOrder: {}", url);
        try {
            ApiResponse<AiplatformOrderWireResponse> resp = openApiClient.post(url, null, ORDER_RECEIPT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询项目清单（透传 aiplatform，四维检索——项目域口径）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects}（#159 已冻结）：返回
     * {@code ApiResponse<PageResponse<BackofficeProjectSummaryResponse>>}。与订单清单的差异：
     * {@code status} 为<strong>三档单选</strong>（1=进行中 3=已归档；缺省＝全部、归档项目缺省含），
     * 单值直传无逗号拼接；精确维度是 {@code projectId}。externalId 换算不到/非数值 projectId 时
     * provider 如实返回空清单 200（无命非错误）；过滤参数绑定失败 400 PRJ_014（数字业务码 4014）
     * 原样透传。归档项目照读、已删项目不可见（provider 真删无墓碑——语义透传，本客户端不二次过滤）。</p>
     *
     * <p>page 1-based 直传零换算（同订单），provider clamp（page≥1、size∈[1,100]）行为透传。</p>
     *
     * @param filter wire 层过滤参数（由应用层从 {@code AiplatformProjectQuery} 映射而来）
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 PRJ_014 等，不做映射）
     */
    public PageResponse<AiplatformProjectSummaryWireResponse> listProjects(AiplatformProjectListWireRequest filter,
                                                                           int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/projects?page=").append(page)
                .append("&size=").append(size);
        // status 单选（1=进行中 3=已归档）：单值直传——与订单域多选拼逗号有意不同
        appendParam(url, "status", filter.status());
        appendParam(url, "createdFrom", filter.createdFrom());
        appendParam(url, "createdTo", filter.createdTo());
        appendParam(url, "externalId", filter.externalId());
        appendParam(url, "projectId", filter.projectId());
        log.debug("AiplatformClient.listProjects: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformProjectSummaryWireResponse>> resp =
                    openApiClient.get(url.toString(), PROJECT_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询项目详情（透传 aiplatform）——清单字段全量 + 订单引用（activeOrder/latestOrder）
     * + 成本指针（costSummary：按币种 BigDecimal + unpriced 标记）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}}（#159 + #164 成本指针）：
     * 返回 {@code ApiResponse<BackofficeProjectDetailResponse>}；项目不存在（含已删——真删无墓碑）
     * 时 HTTP 404 + 数字业务码 4001（PRJ_001）原样透传。归档项目照读。</p>
     *
     * @param id 项目标识（TSID 十进制字符串，provider 侧 lenient 解析：非数值同 404 PRJ_001）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001 等，不做映射）
     */
    public AiplatformProjectDetailWireResponse getProject(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id);
        log.debug("AiplatformClient.getProject: {}", url);
        try {
            ApiResponse<AiplatformProjectDetailWireResponse> resp = openApiClient.get(url, PROJECT_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目对话史（透传 aiplatform）——对话面全量同序（用户发言/智能体回复/问答卡/问答作答/
     * 收尾卡/平台轻引导，id 升序＝对话序），过程明细不在其中。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/conversation}（#162 深读三组）：
     * 返回 {@code ApiResponse<List<ConversationEntryResponse>>}（data 为裸 List，非分页载体）。
     * {@code kind} 为 Integer code + {@code kindName} 中文名随行（aiplatform#186 已落，provider
     * 出口提供——BFF 透传 provider 值）；{@code question}/{@code closing} 载荷 JSON 原样透传。
     * 项目不存在 404 PRJ_001（4001）原样透传。</p>
     *
     * @param id 项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001 等，不做映射）
     */
    public List<AiplatformConversationEntryWireResponse> getConversation(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/conversation";
        log.debug("AiplatformClient.getConversation: {}", url);
        try {
            ApiResponse<List<AiplatformConversationEntryWireResponse>> resp =
                    openApiClient.get(url, CONVERSATION_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目 PRD（透传 aiplatform）——直读项目 dev 工作区 {@code docs/PRD.md}（事实源，
     * v1 无版本链只最新版），markdown 正文 + updatedAt（文件 mtime，秒精度 Instant）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/prd}（#162 深读三组，口径照
     * 用户面 #41）：返回 {@code ApiResponse<PrdResponse>}。未产出（工作区无该文件）HTTP 404 +
     * 数字业务码 4015（PRJ_015，与项目不存在 PRJ_001 区分）；环境故障 500 WSP_002（1002）——
     * 均原样透传。</p>
     *
     * @param id 项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001/PRJ_015、500 WSP_002 等，不做映射）
     */
    public AiplatformPrdWireResponse getPrd(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/prd";
        log.debug("AiplatformClient.getPrd: {}", url);
        try {
            ApiResponse<AiplatformPrdWireResponse> resp = openApiClient.get(url, PROJECT_PRD_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目版本列表（透传 aiplatform）——正本＝容器内 git log（无库表），每轮编码 run 收口
     * 自动成版；排序新→旧定死；零版本（尚无收口）＝空列表非错误。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/versions}（#162 深读三组，
     * 口径照用户面 #91/#93）：返回 {@code ApiResponse<List<VersionResponse>>}（data 为裸 List）。
     * 项目不存在 404 PRJ_001（4001）；环境故障 500 WSP_002（1002）——均原样透传。</p>
     *
     * @param id 项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001、500 WSP_002 等，不做映射）
     */
    public List<AiplatformVersionWireResponse> listVersions(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/versions";
        log.debug("AiplatformClient.listVersions: {}", url);
        try {
            ApiResponse<List<AiplatformVersionWireResponse>> resp = openApiClient.get(url, VERSION_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目版本详情（透传 aiplatform）——版本元数据 + 锚定收尾卡载荷（Run-Id 联接对话史
     * closing 条目，#88 同载荷；收尾卡缺位时 closing 为 null）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/versions/{ref}}（#162 深读三组）：
     * 返回 {@code ApiResponse<VersionDetailResponse>}。ref＝commit hash（hex 40 位）——非 hash
     * 形态 HTTP 404 + 数字业务码 4028（PRJ_028，provider 侧裁决且不触工作区，shell 注入防线）；
     * 项目不存在 404 PRJ_001（4001）；环境故障 500 WSP_002（1002）——均原样透传。</p>
     *
     * @param id  项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @param ref 版本 ref（commit hash，hex 40 位；形态裁决归 provider）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001/PRJ_028、500 WSP_002 等，不做映射）
     */
    public AiplatformVersionDetailWireResponse getVersion(String id, String ref) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/versions/" + encode(ref);
        log.debug("AiplatformClient.getVersion: {}", url);
        try {
            ApiResponse<AiplatformVersionDetailWireResponse> resp = openApiClient.get(url, VERSION_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目文件树（透传 aiplatform）——交付文件视图＝项目 dev 工作区剔除非交付物（data/、
     * .platform/、node_modules/ 与 .env——与源码包同口径）后的只读文件清单 [{path, size}]，
     * 按路径稳定排序；只列文件（目录由调用方按路径段合成）。文件区挂项目不挂订单——未下单
     * 项目可浏览（排障不依赖成交）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/files}（#163 文件区，口径照
     * 用户面文件树读口）：返回 {@code ApiResponse<ProjectFilesResponse>}。归档项目照读（工作区
     * 保留）；项目不存在 404 PRJ_001（4001）、环境故障 500 WSP_002（1002）——均原样透传。</p>
     *
     * @param id 项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001、500 WSP_002 等，不做映射）
     */
    public AiplatformProjectFilesWireResponse getProjectFiles(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/files";
        log.debug("AiplatformClient.getProjectFiles: {}", url);
        try {
            ApiResponse<AiplatformProjectFilesWireResponse> resp = openApiClient.get(url, PROJECT_FILES_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 读项目文本文件内容（透传 aiplatform，「点看」）——path 为工作区相对路径（文件树条目原样
     * 回传），provider 只收文本且限大小。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/files/content?path=}（#163）：
     * 返回 {@code ApiResponse<ProjectFileContentResponse>}。只读策略归 provider 裁决、本客户端
     * <strong>不预检不解释</strong>（issue #71）：非交付物/机密（根级 .env）/逃逸路径 400
     * PRJ_020（4020，判定层拒绝、工作区不被触达）；文件不存在 404 PRJ_021（4021）；超在线
     * 查看上限（1 MiB，容器侧拦截不读取）400 PRJ_022（4022）；非文本（正文含 NUL）400
     * PRJ_023（4023）——均原样透传。归档项目照读；未下单项目照读。</p>
     *
     * @param id   项目标识（TSID 十进制字符串，provider 侧 lenient 解析）
     * @param path 工作区相对路径（文件树条目原样回传；裁决归 provider）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（PRJ_020/021/022/023 等，不做映射）
     */
    public AiplatformProjectFileContentWireResponse getProjectFileContent(String id, String path) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/projects/").append(encode(id)).append("/files/content");
        appendParam(url, "path", path);
        log.debug("AiplatformClient.getProjectFileContent: {}", url);
        try {
            ApiResponse<AiplatformProjectFileContentWireResponse> resp =
                    openApiClient.get(url.toString(), PROJECT_FILE_CONTENT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 下载项目文件包（透传 aiplatform）——tar.gz 二进制流，两态文件名由 provider 决定：已封存
     * 项目直取封存包（整卷口径——含数据库与机密，卷已删、包是唯一事实，{id}-archive.tar.gz）；
     * 未封存项目即时导出源码包（交付口径：排 node_modules/.env 等，与订单源码包同一导出实现，
     * {id}-source.tar.gz）。沙箱休眠中会先同步唤醒重建再打包（分钟内）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/projects/{id}/files/package}（#174）：
     * <strong>无 ApiResponse 信封</strong>（{@code application/gzip} + Content-Disposition 文件名），
     * 经 cartisan-openapi {@link OpenApiClient#download} 取回原始字节 + 响应头——ofByteArray
     * 保全 gzip 字节（ofString 会经 UTF-8 解码不可逆损坏）。内存中转、不落盘。BFF 不判封存态
     * ——sealed/source 两态由 provider 的 Content-Disposition 原值表达、原样透传。</p>
     *
     * <p>项目不存在 404 PRJ_001（4001）；封存态无包记录/包不可读 404 WSP_016（1016）；环境
     * 故障 500 WSP_002（1002）——错误信封在 body 里，{@code download} 内部抛
     * {@link OpenApiClientException}，此处同款翻译透传。</p>
     *
     * @param id 项目标识（TSID 十进制字符串）
     * @return 二进制响应载体（HTTP 状态 + 响应头 raw 值 + 原始字节）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_001/WSP_016、500 WSP_002 等，不做映射）
     */
    public BinaryResponse downloadProjectFilesPackage(String id) {
        String url = baseUrl + "/api/backoffice/projects/" + encode(id) + "/files/package";
        log.debug("AiplatformClient.downloadProjectFilesPackage: {}", url);
        try {
            return openApiClient.download(url);
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询沙箱清单（透传 aiplatform，期望态/实态两维过滤——漂移发现口径）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/workspaces}（#173 观测面已冻结）：返回
     * {@code ApiResponse<PageResponse<BackofficeWorkspaceSummaryResponse>>}。两维均<strong>单选</strong>
     * Integer code、可组合、可缺省（null＝该维不过滤）：{@code desired} 期望态（1=运行 2=休眠 3=封存，
     * DB 意图侧）、{@code actual} 容器实态（1=运行中 2=已停止 3=无容器 4=未知——docker 现场探查、
     * 探查后内存过滤，total 如实＝筛后计数；{@code actual=3} 即捞「期望运行而实态已亡」漂移清单）。
     * 过滤参数绑定失败 400 WSP_014（数字业务码 1014）原样透传。</p>
     *
     * <p>page 1-based 直传零换算（同订单/项目），provider clamp（page≥1、size∈[1,100]）行为透传。
     * 实态与卷大小逐行现场探查（docker 子进程）——页越大越慢，provider 侧语义、本客户端不干预。</p>
     *
     * @param filter wire 层过滤参数（由应用层从 {@code AiplatformWorkspaceQuery} 映射而来）
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 WSP_014 等，不做映射）
     */
    public PageResponse<AiplatformWorkspaceSummaryWireResponse> listWorkspaces(
            AiplatformWorkspaceListWireRequest filter, int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/workspaces?page=").append(page)
                .append("&size=").append(size);
        appendParam(url, "desired", filter.desired());
        appendParam(url, "actual", filter.actual());
        log.debug("AiplatformClient.listWorkspaces: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformWorkspaceSummaryWireResponse>> resp =
                    openApiClient.get(url.toString(), WORKSPACE_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询沙箱详情（透传 aiplatform）——清单行超集：全量字段＋所属项目引用＋中间件资源清单
     * （连接串原文，容器内回环形态）＋置备失败原因/封存包寻址键/审计列。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/workspaces/{id}}（#173 观测面）：返回
     * {@code ApiResponse<BackofficeWorkspaceDetailResponse>}；工作区不存在（含畸形 id——provider 侧
     * lenient 解析）时 HTTP 404 + 数字业务码 1001（WSP_001）原样透传。</p>
     *
     * @param id 工作区标识（TSID 十进制字符串，provider 侧 lenient 解析：非数值同 404 WSP_001）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 WSP_001 等，不做映射）
     */
    public AiplatformWorkspaceDetailWireResponse getWorkspace(String id) {
        String url = baseUrl + "/api/backoffice/workspaces/" + encode(id);
        log.debug("AiplatformClient.getWorkspace: {}", url);
        try {
            ApiResponse<AiplatformWorkspaceDetailWireResponse> resp = openApiClient.get(url, WORKSPACE_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 唤醒沙箱（透传 aiplatform）——收敛到 READY＋（已生成项目）应用在服，同步等结果；容器缺失/
     * 被杀走幂等重建（卷保留）；封存态走深度唤醒（解包回卷＋依赖重装，分钟级）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/workspaces/{id}/wake}（#174 动作面）：<strong>无
     * 请求体</strong>，返回 {@code ApiResponse<BackofficeWorkspaceDetailResponse>}（动作后的观测详情）。
     * 操作者身份经框架 {@code OpenApiClient} 自动带 {@code X-User-Id/X-User-Name} 头（RequestContext
     * →provider 落痕动作行，缺头落空）。工作区不存在 404 WSP_001（1001）、非 DEV 400 WSP_007（1007）、
     * 封存包不可读 404 WSP_016（1016）、置备等待超时 500 WSP_011（1011）——均原样透传。</p>
     *
     * @param id 工作区标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 WSP_001 等，不做映射）
     */
    public AiplatformWorkspaceDetailWireResponse wakeWorkspace(String id) {
        return postWorkspaceAction(id, "wake");
    }

    /**
     * 强制休眠沙箱（透传 aiplatform）——管理员的即时止损口：删容器保卷、期望态置休眠（不等闲置
     * 阈值）；已休眠＝幂等成功（补删残留容器）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/workspaces/{id}/hibernate}（#174）：无请求体，
     * 回执＝动作后的观测详情。封存态拒 400 WSP_009（1009，卷已删先唤醒）、run 在途拒 409
     * WSP_015（1015）、收敛任务在途 409 WSP_017（1017）——均原样透传。操作者透传头落痕同唤醒。</p>
     *
     * @param id 工作区标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 WSP_009、409 WSP_015 等，不做映射）
     */
    public AiplatformWorkspaceDetailWireResponse hibernateWorkspace(String id) {
        return postWorkspaceAction(id, "hibernate");
    }

    /**
     * 强制重建沙箱（透传 aiplatform）——「预览死了」型漂移的标准化处置（替代手工 docker 拉）：
     * 在跑但坏了的容器也杀（卷保留、数据不动），走唤醒内核同一重建路径收敛回 READY。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/workspaces/{id}/rebuild}（#174）：无请求体，
     * 回执＝动作后的观测详情。封存态拒 400 WSP_009（1009，空卷重建＝掩埋数据丢失——先唤醒）、
     * run 在途拒 409 WSP_015（1015）、收敛任务在途 409 WSP_017（1017）、重建重试上限落 FAILED
     * 时 500 WSP_010（1010，可再触发）——均原样透传。操作者透传头落痕同唤醒。</p>
     *
     * @param id 工作区标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 WSP_009、500 WSP_010 等，不做映射）
     */
    public AiplatformWorkspaceDetailWireResponse rebuildWorkspace(String id) {
        return postWorkspaceAction(id, "rebuild");
    }

    /**
     * 封存沙箱（透传 aiplatform）——动作序同闲置满期的自动封存：删容器→整卷打包（仅排可重建缓存）
     * 落平台存储→期望态置封存＋包元数据→删卷（失败由扫描下轮收敛）；RUNNING 起点可用（即时深回收）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/workspaces/{id}/seal}（#174）：无请求体，回执＝
     * 动作后的观测详情（含封存包元数据）。重复封存拒 400 WSP_009（1009，走「唤醒→休眠→封存」周期）、
     * run 在途拒 409 WSP_015（1015）、收敛任务在途 409 WSP_017（1017）——均原样透传。
     * 操作者透传头落痕同唤醒。</p>
     *
     * @param id 工作区标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 WSP_009、409 WSP_015 等，不做映射）
     */
    public AiplatformWorkspaceDetailWireResponse sealWorkspace(String id) {
        return postWorkspaceAction(id, "seal");
    }

    /**
     * 四干预动作共用的出站形状（#174）：{@code POST /api/backoffice/workspaces/{id}/{action}}——
     * 无请求体（框架 {@code OpenApiClient.post} 对 null body 发空 body），返回
     * {@code ApiResponse<BackofficeWorkspaceDetailResponse>} 信封取 {@code .data()}；操作者身份由
     * 框架经 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传（identity 同款）。
     */
    private AiplatformWorkspaceDetailWireResponse postWorkspaceAction(String id, String action) {
        String url = baseUrl + "/api/backoffice/workspaces/" + encode(id) + "/" + action;
        log.debug("AiplatformClient.workspaceAction[{}]: {}", action, url);
        try {
            // 无请求体：框架 OpenApiClient.post 对 null body 发空 body（POST 仍带 application/json），
            // provider 端只读路径参数（同 payment 主动查行先例）
            ApiResponse<AiplatformWorkspaceDetailWireResponse> resp =
                    openApiClient.post(url, null, WORKSPACE_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 平台成本全局总览（透传 aiplatform）——全平台跨项目观测模型开销构成：五档总量 + 平台成本
     * （token × 事件时点生效单价，币种分桶直读不折算）+ 分模型 + 分智能体（agentKind 裸维度串
     * + agentKindName 随行——aiplatform#186 已落）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/costs/overview}（#161 成本运营）：返回
     * {@code ApiResponse<BackofficeCostOverviewResponse>}。空窗/无数据返回全零 total 与空分桶
     * （不是错误）；查询参数绑定失败 400 METER_011（数字业务码 3011）原样透传。</p>
     *
     * <p>时间窗 {@code [from, to)} 半开、ISO-8601 Instant（UTC 带 Z）；provider 侧可缺省，北向
     * <strong>必填</strong>（issue #67：不设默认窗口）——出站恒带双参。Instant 出站取
     * {@code Instant.toString()}（ISO_INSTANT 确定形，秒恒在场、UTC 带 Z），provider 的
     * {@code @RequestParam Instant} 同形解析。</p>
     *
     * @param window wire 层时间窗（由应用层从 {@code AiplatformCostQuery} 映射而来）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 METER_011 等，不做映射）
     */
    public AiplatformCostOverviewWireResponse getCostOverview(AiplatformCostWindowWireRequest window) {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/backoffice/costs/overview");
        appendWindow(url, window);
        log.debug("AiplatformClient.getCostOverview: {}", url);
        try {
            ApiResponse<AiplatformCostOverviewWireResponse> resp =
                    openApiClient.get(url.toString(), COST_OVERVIEW_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * unpriced 全局警示（透传 aiplatform，用量驱动）——窗口内有 token 用量且事件时点无生效单价
     * 的 (provider, model, 档位) 按档位汇总 token（只计无价分量）。静态配价缺口不做（无用量＝
     * 无实际损失）；据此发现漏配价并及时补价（历史成本不漂移）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/costs/unpriced}（#161）：返回
     * {@code ApiResponse<BackofficeUnpricedUsageResponse>}；空窗/无未配价用量返回空 items
     * （不是错误）；绑定失败 400 METER_011（3011）原样透传。</p>
     *
     * @param window wire 层时间窗
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 METER_011 等，不做映射）
     */
    public AiplatformUnpricedUsageWireResponse getUnpricedUsage(AiplatformCostWindowWireRequest window) {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/backoffice/costs/unpriced");
        appendWindow(url, window);
        log.debug("AiplatformClient.getUnpricedUsage: {}", url);
        try {
            ApiResponse<AiplatformUnpricedUsageWireResponse> resp =
                    openApiClient.get(url.toString(), UNPRICED_USAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询项目成本清单（透传 aiplatform，成本降序）——窗口内有 token 用量的各项目成本汇总；
     * 排序服务端定死成本降序（全未配价项目排后且 allUnpriced=true、同序按 projectId 升序稳定）；
     * 已删项目的历史花费照列（成本观测不抹历史）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/costs/projects}（#164）：返回
     * {@code ApiResponse<PageResponse<BackofficeProjectCostResponse>>}（page 1-based、total JSON
     * string——cartisan-web 全局 Long→ToStringSerializer）。page 1-based 直传零换算（同订单/
     * 项目/沙箱），provider clamp（page≥1、size∈[1,100] 默认 20）行为透传、本客户端不重复夹取。</p>
     *
     * @param window wire 层时间窗
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 METER_011 等，不做映射）
     */
    public PageResponse<AiplatformProjectCostWireResponse> listProjectCosts(
            AiplatformCostWindowWireRequest window, int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/costs/projects?page=").append(page)
                .append("&size=").append(size);
        appendWindow(url, window);
        log.debug("AiplatformClient.listProjectCosts: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformProjectCostWireResponse>> resp =
                    openApiClient.get(url.toString(), PROJECT_COST_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 单项目成本下钻（透传 aiplatform）——byModel/byAgentKind 分解 + 未配价档位（与 cost 互补
     * 不重叠，bySubject 口径无 token 计数）。{@code projectId} 为计量 subject 原值（写侧口径
     * projectId 十进制串，provider 不解释存在性）：无用量/查无此号返回全零 total 与空结构
     * （明确空态，非错误、不 404）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/costs/projects/{projectId}}（#164）：返回
     * {@code ApiResponse<BackofficeProjectCostDetailResponse>}；绑定失败 400 METER_011（3011）
     * 原样透传。</p>
     *
     * @param projectId 项目标识（计量 subject 原值，provider 不解释存在性）
     * @param window    wire 层时间窗
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 METER_011 等，不做映射）
     */
    public AiplatformProjectCostDetailWireResponse getProjectCostDetail(
            String projectId, AiplatformCostWindowWireRequest window) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/costs/projects/").append(encode(projectId));
        appendWindow(url, window);
        log.debug("AiplatformClient.getProjectCostDetail: {}", url);
        try {
            ApiResponse<AiplatformProjectCostDetailWireResponse> resp =
                    openApiClient.get(url.toString(), PROJECT_COST_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询单价行清单（透传 aiplatform，含历史行——价史全貌）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/price-entries}（#160 已冻结）：返回
     * {@code ApiResponse<PageResponse<UnitPriceEntryResponse>>}。provider/model 均为匹配键成分＝
     * 精确等值过滤、均可缺省（缺省＝全量行）；排序服务端定死＝生效起点倒序（新段在前，同起点
     * id 倒序稳定）；effectiveTo 为 null 即当前行。行带操作者两列（最近管理动作留痕；存量行/
     * 种子脚本种入行落 null）。绑定裁决两段：非整数 page/size 在<strong>北向绑定层</strong>即 404
     * （框架类型不匹配口径，到不了本方法）；provider 侧绑定失败 400 METER_009（数字业务码 3009）
     * 仅在 provider 契约演进（如过滤维度加类型化标量）时可能出现——错误信封忠实透传。</p>
     *
     * <p>page 1-based 直传零换算（同订单/项目/沙箱/成本），provider clamp（page≥1、size∈[1,100]
     * 默认 20）行为透传、本客户端不重复夹取。</p>
     *
     * @param filter wire 层过滤参数（由应用层从 {@code AiplatformPriceEntryQuery} 映射而来）
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 METER_009 等，不做映射）
     */
    public PageResponse<AiplatformUnitPriceEntryWireResponse> listPriceEntries(
            AiplatformPriceEntryListWireRequest filter, int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/price-entries?page=").append(page)
                .append("&size=").append(size);
        appendParam(url, "provider", filter.provider());
        appendParam(url, "model", filter.model());
        log.debug("AiplatformClient.listPriceEntries: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformUnitPriceEntryWireResponse>> resp =
                    openApiClient.get(url.toString(), PRICE_ENTRY_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 原子改价（透传 aiplatform）——单调用关当前行＋开新行（同事务，中途任一守卫失败两行都不动）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/price-entries/{id}/reprice}（#160）：请求体
     * {@code {unitPrice, currency, effectiveFrom}} 逐字镜像（unitPrice BigDecimal→JSON 明文小数；
     * effectiveFrom 可空＝缺省即时、含未来时点＝预发布——预发布与重叠校验语义由 provider 承担，
     * BFF 不代填时点不预判），返回 {@code ApiResponse<UnitPriceEntryRepriceResponse>}——
     * {@code {closed, opened}} 双行回执（被关行落 effectiveTo＝新起点且保留其原开行留痕，新行
     * 沿用匹配键、带改价操作者）。操作者身份经框架 {@code OpenApiClient} 自动带
     * {@code X-User-Id/X-User-Name} 头（RequestContext→provider 落痕新行，缺头落空）。</p>
     *
     * <p>行不存在（含畸形 id——provider 侧 lenient 解析）404 METER_006（3006）；字段不完整/
     * 单价负数 400 METER_004（3004）；币种非 ISO 4217 400 METER_010（3010）；起点早于被关行起点
     * 400 METER_005（3005）；目标非当前行 409 METER_007（3007）；区间重叠（跨区间或同起点）
     * 409 METER_008（3008）——均原样透传。</p>
     *
     * @param id      单价行标识（TSID 十进制字符串）
     * @param command 改价命令体（由应用层从北向命令映射而来，字段合法性归 provider 聚合守卫）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（METER_004/005/006/007/008/010 等，不做映射）
     */
    public AiplatformUnitPriceEntryRepriceWireResponse repricePriceEntry(
            String id, AiplatformPriceEntryRepriceWireRequest command) {
        String url = baseUrl + "/api/backoffice/price-entries/" + encode(id) + "/reprice";
        log.debug("AiplatformClient.repricePriceEntry: {}", url);
        try {
            ApiResponse<AiplatformUnitPriceEntryRepriceWireResponse> resp =
                    openApiClient.post(url, command, PRICE_ENTRY_REPRICE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 停用单价行（透传 aiplatform）——即时生效：关当前行（effectiveTo＝现在）不接新行，此后该匹配键
     * 用量进 unpriced（缺价不伪装 0、不阻断聚合）。对未生效的预发布行停用＝钳到自身起点成空区间
     * （从未生效）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/price-entries/{id}/deactivate}（#160）：<strong>无
     * 请求体</strong>（框架 {@code OpenApiClient.post} 对 null body 发空 body，provider 端只读路径参数——
     * 同沙箱四动作先例），返回 {@code ApiResponse<UnitPriceEntryResponse>}（被关行单行回执）。操作者
     * 透传头自动落痕被关行（停用不接新行，被关行是唯一落点；缺头落空）。行不存在（含畸形 id）
     * 404 METER_006（3006）；目标非当前行 409 METER_007（3007）——均原样透传。</p>
     *
     * @param id 单价行标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 METER_006、409 METER_007 等，不做映射）
     */
    public AiplatformUnitPriceEntryWireResponse deactivatePriceEntry(String id) {
        String url = baseUrl + "/api/backoffice/price-entries/" + encode(id) + "/deactivate";
        log.debug("AiplatformClient.deactivatePriceEntry: {}", url);
        try {
            ApiResponse<AiplatformUnitPriceEntryWireResponse> resp =
                    openApiClient.post(url, null, PRICE_ENTRY_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 分页查询知识素材清单（透传 aiplatform，三维过滤——治理工作清单）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/materials}（#166 知识库管理）：返回
     * {@code ApiResponse<PageResponse<BackofficeMaterialSummaryResponse>>}。三维度可组合、均可缺省
     * （缺省＝全量）：{@code status} 状态<strong>单选</strong> Integer code（1=启用 2=停用——与订单
     * 多选有意不同）；{@code sunkFrom}/{@code sunkTo} 沉淀时间闭区间（首沉淀时间，ISO-8601
     * Instant UTC 带 Z——{@code appendParam} 默认分支取 {@code Instant.toString()} 确定形）；
     * {@code projectId} 来源项目 id 精确（查无＝空清单 200）。排序服务端定死＝沉淀时间倒序
     * （新沉淀在前，id 倒序稳定）。绑定裁决两段：非整数 status/非 Instant 时间在<strong>北向
     * 绑定层</strong>即 404（框架类型不匹配口径，到不了本方法）；provider 侧绑定失败
     * 400 KNW_007（数字业务码 2007，含数值但未知的状态 code）原样透传。</p>
     *
     * <p>page 1-based 直传零换算（同订单/项目/沙箱/成本/单价表），provider clamp
     * （page≥1、size∈[1,100] 默认 20）行为透传、本客户端不重复夹取。</p>
     *
     * @param filter wire 层过滤参数（由应用层从 {@code AiplatformMaterialQuery} 映射而来）
     * @param page   页码，<strong>1-based</strong>（北向原样直传）
     * @param size   每页大小
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 KNW_007 等，不做映射）
     */
    public PageResponse<AiplatformMaterialSummaryWireResponse> listMaterials(
            AiplatformMaterialListWireRequest filter, int page, int size) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/api/backoffice/materials?page=").append(page)
                .append("&size=").append(size);
        // status 单选（1=启用 2=停用）：单值直传（项目/沙箱同款），无订单域多选拼逗号
        appendParam(url, "status", filter.status());
        appendParam(url, "sunkFrom", filter.sunkFrom());
        appendParam(url, "sunkTo", filter.sunkTo());
        appendParam(url, "projectId", filter.projectId());
        log.debug("AiplatformClient.listMaterials: {}", url);
        try {
            ApiResponse<PageResponse<AiplatformMaterialSummaryWireResponse>> resp =
                    openApiClient.get(url.toString(), MATERIAL_PAGE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询知识素材详情（透传 aiplatform）——元数据（与清单行同形）＋素材全文：{@code content}
     * ＝块按 seq 以空行拼接（段落级重组，内容无损）。来源项目引用容缺直读登记面。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/materials/{id}}（#166）：返回
     * {@code ApiResponse<BackofficeMaterialDetailResponse>}；素材不存在（含畸形 id——provider
     * 侧 lenient 解析）时 HTTP 404 + 数字业务码 2005（KNW_005）原样透传。</p>
     *
     * @param id 素材标识（TSID 十进制字符串，provider 侧 lenient 解析：非数值同 404 KNW_005）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 KNW_005 等，不做映射）
     */
    public AiplatformMaterialDetailWireResponse getMaterial(String id) {
        String url = baseUrl + "/api/backoffice/materials/" + encode(id);
        log.debug("AiplatformClient.getMaterial: {}", url);
        try {
            ApiResponse<AiplatformMaterialDetailWireResponse> resp =
                    openApiClient.get(url, MATERIAL_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 停用素材（透传 aiplatform）——可逆开关：素材全部块退出生成命中（重沉淀不复活），误伤可经
     * enable 恢复；重复停用幂等（操作者留最近一次）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/materials/{id}/disable}（#166）：<strong>无
     * 请求体</strong>（框架 {@code OpenApiClient.post} 对 null body 发空 body，provider 端只读
     * 路径参数——同沙箱四动作/单价表停用先例），返回 {@code ApiResponse<BackofficeMaterialSummaryResponse>}
     * （provider 重读登记行的最新状态与操作者）。操作者身份经框架 {@code OpenApiClient} 自动带
     * {@code X-User-Id/X-User-Name} 头（RequestContext→provider 落痕素材级）——<strong>缺头
     * 400 KNW_006</strong>（数字业务码 2006，知识治理动作必留痕，与单价表缺头落空有意不同——
     * 知识治理无种子脚本无头通道）。素材不存在（含畸形 id）404 KNW_005（2005）——均原样透传。</p>
     *
     * @param id 素材标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 KNW_006、404 KNW_005 等，不做映射）
     */
    public AiplatformMaterialSummaryWireResponse disableMaterial(String id) {
        return postMaterialAction(id, "disable");
    }

    /**
     * 启用素材（透传 aiplatform）——停用的可逆侧：素材全部块恢复参与生成命中；重复启用幂等。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/materials/{id}/enable}（#166）：无请求体，
     * 回执＝summary（同停用口径）。操作者透传头落痕同停用（缺头 400 KNW_006＝2006）；
     * 素材不存在（含畸形 id）404 KNW_005（2005）——均原样透传。</p>
     *
     * @param id 素材标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 KNW_006、404 KNW_005 等，不做映射）
     */
    public AiplatformMaterialSummaryWireResponse enableMaterial(String id) {
        return postMaterialAction(id, "enable");
    }

    /**
     * 删除素材（透传 aiplatform）——治理移除、不可逆：登记行与全部块同事务移除、不动来源项目
     * （管理删除与项目删除级联正交）；无行可留、不留痕。回执＝<strong>删除前终态</strong>
     * summary（provider 契约如此，逐字镜像——确认移除了什么）。
     *
     * <p>对接 aiplatform {@code DELETE /api/backoffice/materials/{id}}（#166）：HTTP <strong>DELETE</strong>
     * 动词——经框架 {@link OpenApiClient#delete}（cartisan-boot#33：空 body digest + query 入签
     * + RequestContext 透传，与 {@code get()} 同形），返回 {@code ApiResponse<BackofficeMaterialSummaryResponse>}。
     * 素材不存在（含畸形 id、重复删除）404 KNW_005（2005）原样透传。删除不落操作者
     * （无行可留），操作者透传头在发（无操作者参数可落）。</p>
     *
     * @param id 素材标识（TSID 十进制字符串）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 KNW_005 等，不做映射）
     */
    public AiplatformMaterialSummaryWireResponse deleteMaterial(String id) {
        String url = baseUrl + "/api/backoffice/materials/" + encode(id);
        log.debug("AiplatformClient.deleteMaterial: {}", url);
        try {
            ApiResponse<AiplatformMaterialSummaryWireResponse> resp =
                    openApiClient.delete(url, MATERIAL_SUMMARY_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 停用/启用两治理动作共用的出站形状（#166）：{@code POST /api/backoffice/materials/{id}/{action}}
     * ——无请求体（框架 {@code OpenApiClient.post} 对 null body 发空 body，provider 端只读路径参数），
     * 返回 {@code ApiResponse<BackofficeMaterialSummaryResponse>} 信封取 {@code .data()}（provider
     * 重读登记行的最新状态与操作者）；操作者身份由框架经 {@code RequestContext}→
     * {@code X-User-Id/X-User-Name} 自动透传（缺头会被 provider 域面守卫 KNW_006 拦截——
     * 治理动作必留痕）。
     */
    private AiplatformMaterialSummaryWireResponse postMaterialAction(String id, String action) {
        String url = baseUrl + "/api/backoffice/materials/" + encode(id) + "/" + action;
        log.debug("AiplatformClient.materialAction[{}]: {}", action, url);
        try {
            ApiResponse<AiplatformMaterialSummaryWireResponse> resp =
                    openApiClient.post(url, null, MATERIAL_SUMMARY_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 技能清单（透传 aiplatform，技能线 T1 读面）——内置（classpath 合成）与安装技能同权呈现，
     * 空库时仍呈现内置技能；<strong>零查询参数</strong>（有界目录不分页不过滤，排序服务端定死：
     * 内置在前名称序、安装在后按来源包＋名称序）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/skills}（#247 技能库）：返回
     * {@code ApiResponse<List<BackofficeSkillSummaryResponse>>}——data 为裸 List（非分页载体，
     * 对话史 List 直出同形）。行 {@code id} 为 opaque 串两形制（内置 {@code builtin:<技能名>}／
     * 安装 TSID 十进制串），admin 按字符串透传（spec #79：不解析不预校验）。</p>
     *
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（不做映射）
     */
    public List<AiplatformSkillSummaryWireResponse> listSkills() {
        String url = baseUrl + "/api/backoffice/skills";
        log.debug("AiplatformClient.listSkills: {}", url);
        try {
            ApiResponse<List<AiplatformSkillSummaryWireResponse>> resp =
                    openApiClient.get(url, SKILL_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询技能详情（透传 aiplatform）——清单行同形元数据＋frontmatter 全量（解析态键值）＋
     * 正文全文（frontmatter 剥离后），安装审核承载面（所见即运行时注入面）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/skills/{id}}（#247）：返回
     * {@code ApiResponse<BackofficeSkillDetailResponse>}；技能不存在（含未寻址内置名/TSID、
     * 畸形柄——provider 侧两形制柄单点分解）时 HTTP 404 + 数字业务码 7001（SKL_001）原样透传。
     * {@code id} 取清单行原值原样出站（opaque 串，URL 编码仅传输层——{@code builtin:}
     * 冒号与技能名整体入路径）。</p>
     *
     * @param id 技能柄（opaque 串两形制：builtin:&lt;技能名&gt;／TSID 十进制串，admin 不预检）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 SKL_001 等，不做映射）
     */
    public AiplatformSkillDetailWireResponse getSkill(String id) {
        String url = baseUrl + "/api/backoffice/skills/" + encode(id);
        log.debug("AiplatformClient.getSkill: {}", url);
        try {
            ApiResponse<AiplatformSkillDetailWireResponse> resp =
                    openApiClient.get(url, SKILL_DETAIL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 安装技能包（透传 aiplatform，技能线 T2 写口）——git 仓库地址装时快照固化（clone 解析
     * 全部 SKILL.md 入库，版本＝装时 HEAD commit，ADR-0021 快照安装不订阅远端）；
     * {@code excludeDirs} 任一段命中技能仓库相对路径即整支不入库（段名精确匹配）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/skills/install}（#248）：请求体
     * {@code {repoUrl, excludeDirs}} 逐字镜像（excludeDirs 可缺省），返回
     * {@code ApiResponse<List<BackofficeSkillSummaryResponse>>}——data 为裸 List，与清单行
     * 共形（本次装入的条目清单）。操作者身份经框架 {@code OpenApiClient} 自动带
     * {@code X-User-Id/X-User-Name} 头（RequestContext→provider 落痕装者）——<strong>缺头
     * 400 SKL_009</strong>（数字业务码 7009，技能库写操作必留痕）。失败 fail-fast 整体不入库：
     * 地址空 400 SKL_002（7002）、同源重复安装 409 SKL_003（7003）、克隆失败/超时 502
     * SKL_004（7004）、未解析到技能 400 SKL_005（7005）、SKILL.md 不合格 400 SKL_006
     * （7006）、包内重名 400 SKL_007（7007）——均原样透传。</p>
     *
     * @param command 安装命令体（由应用层从北向命令映射而来，字段合法性归 provider 安装用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（SKL_002–SKL_009 等，不做映射）
     */
    public List<AiplatformSkillSummaryWireResponse> installSkills(AiplatformSkillInstallWireRequest command) {
        String url = baseUrl + "/api/backoffice/skills/install";
        log.debug("AiplatformClient.installSkills: {}", url);
        try {
            // 回执与清单裸 List 共形（SKILL_LIST_TYPEREF 复用——同 TypeReference 同形）
            ApiResponse<List<AiplatformSkillSummaryWireResponse>> resp =
                    openApiClient.post(url, command, SKILL_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 停用技能（透传 aiplatform，技能线 T2 写口）——可逆开关：退出装配候选（装配合成只收
     * 启用行）、不丢库行，误伤可经 enable 恢复；重复停用幂等（操作者留最近一次）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/skills/{id}/disable}（#248）：<strong>无
     * 请求体</strong>（框架 {@code OpenApiClient.post} 对 null body 发空 body，provider 端只读
     * 路径参数——同素材停用先例），返回 {@code ApiResponse<BackofficeSkillSummaryResponse>}
     * （翻转后清单行）。操作者透传头自动落痕（最近管理动作操作者），缺头 400 SKL_009（7009）。
     * 寻址 TSID 柄；内置技能非库行无状态迁移——builtin: 柄/畸形柄/未寻址 TSID 同语义 404
     * SKL_001（7001）——均原样透传。</p>
     *
     * @param id 技能柄（opaque 串原样出站；状态迁移仅对安装库行有意义，裁决归 provider）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 SKL_001、400 SKL_009 等，不做映射）
     */
    public AiplatformSkillSummaryWireResponse disableSkill(String id) {
        return postSkillAction(id, "disable");
    }

    /**
     * 启用技能（透传 aiplatform，技能线 T2 写口）——停用的可逆侧：恢复参与装配合成；
     * 重复启用幂等。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/skills/{id}/enable}（#248）：无请求体，
     * 回执＝翻转后清单行。操作者透传头落痕口径同 disable（缺头 400 SKL_009＝7009）；
     * 寻址与 404 语义同 disable（TSID 柄；内置柄 404 SKL_001＝7001）——均原样透传。</p>
     *
     * @param id 技能柄（opaque 串原样出站；裁决归 provider）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 SKL_001、400 SKL_009 等，不做映射）
     */
    public AiplatformSkillSummaryWireResponse enableSkill(String id) {
        return postSkillAction(id, "enable");
    }

    /**
     * 卸载技能（透传 aiplatform，技能线 T2 写口）——库行删除即彻底出库（不可逆），清单/详情/
     * 装配均不可见。卸载守卫：有指派在身（任一职能槽位）的技能拒绝卸载（409 SKL_008，先解绑
     * 再卸）。回执＝<strong>删除前终态</strong>清单行（provider 契约如此，逐字镜像——确认移除了
     * 什么）。无行可留不留痕（admin 侧自有操作日志）。
     *
     * <p>对接 aiplatform {@code DELETE /api/backoffice/skills/{id}}（#248）：HTTP <strong>DELETE</strong>
     * 动词——经框架 {@link OpenApiClient#delete}（cartisan-boot#33：空 body digest + query 入签
     * + RequestContext 透传，与 {@code get()} 同形——DELETE 动词不可被 get/post 替代，契约测试
     * 钉死），返回 {@code ApiResponse<BackofficeSkillSummaryResponse>}。寻址 TSID 柄；内置技能
     * 不可卸——builtin: 柄/畸形柄/未寻址 TSID 同语义 404 SKL_001（7001）、重复卸载 404
     * ——均原样透传。</p>
     *
     * @param id 技能柄（opaque 串原样出站；可卸性裁决归 provider）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 SKL_001、409 SKL_008 等，不做映射）
     */
    public AiplatformSkillSummaryWireResponse uninstallSkill(String id) {
        String url = baseUrl + "/api/backoffice/skills/" + encode(id);
        log.debug("AiplatformClient.uninstallSkill: {}", url);
        try {
            ApiResponse<AiplatformSkillSummaryWireResponse> resp =
                    openApiClient.delete(url, SKILL_SUMMARY_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 停用/启用两治理动作共用的出站形状（技能线 T2，#248）：
     * {@code POST /api/backoffice/skills/{id}/{action}}——无请求体（框架
     * {@code OpenApiClient.post} 对 null body 发空 body，provider 端只读路径参数），返回
     * {@code ApiResponse<BackofficeSkillSummaryResponse>} 信封取 {@code .data()}（翻转后
     * 清单行）；操作者身份由框架经 {@code RequestContext}→{@code X-User-Id/X-User-Name}
     * 自动透传（缺头会被 provider 域面守卫 SKL_009 拦截——技能库写操作必留痕）。
     */
    private AiplatformSkillSummaryWireResponse postSkillAction(String id, String action) {
        String url = baseUrl + "/api/backoffice/skills/" + encode(id) + "/" + action;
        log.debug("AiplatformClient.skillAction[{}]: {}", action, url);
        try {
            ApiResponse<AiplatformSkillSummaryWireResponse> resp =
                    openApiClient.post(url, null, SKILL_SUMMARY_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 查询槽位指派读面（透传 aiplatform，技能线 T3 读口）——该职能槽位当前指派的技能清单
     * （与清单行共形、<strong>含停用行</strong>——启停是可逆开关指派关系随行保留，运营可见
     * 「指派了但已停用」实态；停用行不参与装配合成）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/skills/assignments/{slot}}（#249）：返回
     * {@code ApiResponse<BackofficeSlotAssignmentResponse>}（{@code {slot, skills[]}}）。{@code slot}
     * 为字符串路径参数（main/executor/subagent 三把稳定键）原样出站——admin 透传不预校验，
     * 未知槽位由 provider 判 404 SKL_010（数字业务码 7010）原样透传。</p>
     *
     * @param slot 职能槽位键（main/executor/subagent，admin 不预检）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 SKL_010 等，不做映射）
     */
    public AiplatformSlotAssignmentWireResponse getSlotAssignments(String slot) {
        String url = baseUrl + "/api/backoffice/skills/assignments/" + encode(slot);
        log.debug("AiplatformClient.getSlotAssignments: {}", url);
        try {
            ApiResponse<AiplatformSlotAssignmentWireResponse> resp =
                    openApiClient.get(url, SLOT_ASSIGNMENT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 槽位指派整包替换（透传 aiplatform，技能线 T3 写口）——PUT 全量语义：{@code skillIds}
     * 即该槽位终态（未列入即解绑、空清单＝清空），支持整包批量勾选（admin 侧按来源包勾满后
     * 送全量）。指派目标只收安装库行 TSID 柄：内置 {@code builtin:} 柄不可指派（400 SKL_011
     * ＝7011——内置随平台发版，装配按配置挂载）；未寻址/畸形 TSID 404 SKL_001（7001，先卸载
     * 后指派的不变窗口同语义）。操作者透传头落痕（整包替换留最近动作者，缺头 400 SKL_009
     * ＝7009）。回执＝替换后读面（与 GET 同形）。
     *
     * <p>对接 aiplatform {@code PUT /api/backoffice/skills/assignments/{slot}}（#249）：请求体
     * {@code {skillIds}} 逐字镜像，HTTP <strong>PUT</strong> 动词——经框架
     * {@link OpenApiClient#put}（与 {@code post} 同走 sendWithBody 序列化＋签名路径），返回
     * {@code ApiResponse<BackofficeSlotAssignmentResponse>}。{@code slot} 寻址同 GET（未知
     * 404 SKL_010＝7010）。生效语义＝动态查库：变更后智能体下一轮自然生效、进行中 run
     * 不定格。</p>
     *
     * @param slot    职能槽位键（main/executor/subagent，admin 不预检）
     * @param command 指派命令体（由应用层从北向命令映射而来，柄合法性归 provider 指派用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（SKL_001/009/010/011 等，不做映射）
     */
    public AiplatformSlotAssignmentWireResponse assignSlot(String slot, AiplatformSkillAssignWireRequest command) {
        String url = baseUrl + "/api/backoffice/skills/assignments/" + encode(slot);
        log.debug("AiplatformClient.assignSlot: {}", url);
        try {
            ApiResponse<AiplatformSlotAssignmentWireResponse> resp =
                    openApiClient.put(url, command, SLOT_ASSIGNMENT_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 指派双头预检（透传 aiplatform，技能域第 16 端点）——PUT 指派提交前的<strong>非阻断</strong>
     * 方法论重叠检查（只读）：对照双头「该槽位生效工作协议（智能体运营配置生效值）∪ 该槽位
     * 已指派启用技能 ∪ 候选集内其他技能」，由 provider 侧模型判定（flash 档专用配置键、15s
     * 超时——BFF 出站默认 read 30s 不会先掐）。回执两件同读：{@code executed}=true 且 hints
     * 空＝确认无重叠；executed=true 且 hints 非空＝发现重叠（逐条四字段，判定权留管理员）；
     * executed=false＝<strong>未执行</strong>（判定失败/超时/输出不可解析，hints 恒空）——
     * 不得当无重叠呈现。非阻断：预检结果不拦 PUT 指派、组件故障不阻塞指派照常提交。全程
     * 只读不留痕（不落库、不改指派、无操作者头要求——区别于 PUT 指派的 SKL_009）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/skills/assignments/{slot}/precheck}（#255）：
     * 请求体复用指派整包替换命令 {@code {skillIds}}（候选终态清单，与 PUT 同形——provider 同款
     * 复用 {@code SkillSlotAssignCommand}，不另造 Precheck 命令型），经框架 {@code OpenApiClient.post}
     * （install/update 已立的 bodySink 序列化 seam），返回
     * {@code ApiResponse<BackofficeSkillPrecheckResponse>}。{@code slot} 寻址同 GET/PUT
     * （main/executor/subagent 字符串路径参数透传不预校验，未知 404 SKL_010＝7010）。错误族与
     * PUT 指派同源同码：内置柄 400 SKL_011（7011）、未寻址/畸形 TSID 404 SKL_001（7001）——
     * 均原样透传。</p>
     *
     * @param slot    职能槽位键（main/executor/subagent，admin 不预检）
     * @param command 指派命令体（候选终态清单——与 PUT 同形复用，柄合法性归 provider 指派用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（SKL_001/010/011 等，不做映射）
     */
    public AiplatformSkillPrecheckWireResponse precheckSlot(String slot, AiplatformSkillAssignWireRequest command) {
        String url = baseUrl + "/api/backoffice/skills/assignments/" + encode(slot) + "/precheck";
        log.debug("AiplatformClient.precheckSlot: {}", url);
        try {
            ApiResponse<AiplatformSkillPrecheckWireResponse> resp =
                    openApiClient.post(url, command, SKILL_PRECHECK_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 显式更新技能包（透传 aiplatform，技能线 T4 写口）——按来源包整体重拉快照入库
     * （ADR-0021：更新永远显式点，平台永不自动跟新远端——清单「有新版」标记只提示、不动作）；
     * 排除名单＝装时持久化口径（更新不还魂装时排除的目录）。
     *
     * <p>对接 aiplatform {@code POST /api/backoffice/skills/update}（#250）：请求体
     * {@code {sourcePackage}} 逐字镜像（取清单行原值回传，服务端再规范化，同源身份单源），
     * 返回 {@code ApiResponse<BackofficeSkillUpdateResponse>}——from/to 版本＋被移除技能名＋
     * 更新后该包终态行集（skills 与清单行共形；同名行原地翻新保留 id/状态/指派——TSID 柄
     * 稳定）。远端未前进＝幂等回执（from==to、removedSkillNames 空、不落痕不写行）。操作者
     * 透传头自动落痕（显式动作必留痕，缺头 400 SKL_009＝7009）。失败族：来源包空 400
     * SKL_014（7014）、未安装 404 SKL_012（7012）、克隆失败/超时 502 SKL_004（7004）、远端
     * 未解析到技能 400 SKL_005（7005）、包内重名 400 SKL_007（7007）、移除项有指派在身整体
     * 拒绝 409 SKL_013（7013，先解绑再更新）——均原样透传。</p>
     *
     * @param command 更新命令体（由应用层从北向命令映射而来，字段合法性归 provider 更新用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（SKL_004/005/007/009/012/013/014 等，不做映射）
     */
    public AiplatformSkillUpdateWireResponse updateSkills(AiplatformSkillUpdateWireRequest command) {
        String url = baseUrl + "/api/backoffice/skills/update";
        log.debug("AiplatformClient.updateSkills: {}", url);
        try {
            ApiResponse<AiplatformSkillUpdateWireResponse> resp =
                    openApiClient.post(url, command, SKILL_UPDATE_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 更新留痕读面（透传 aiplatform，技能线 T4 读口）——该来源包全部显式更新留痕
     * （from→to 版本＋操作者＋时刻），按时间倒序（最近先）。append-only：卸载不删留痕
     * （历史事实不随库行消失），装时版本经链条首个 from 可回溯。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/skills/update-traces}（#250）：查询参数
     * {@code sourcePackage} 保持 provider 形态——<strong>可选绑定</strong>（spec #79：
     * admin 不在绑定层前置拦截），空/缺由 provider 手工校验出 400 SKL_014（7014）原样透传；
     * 返回 {@code ApiResponse<List<BackofficeSkillUpdateTraceResponse>>}——data 为裸 List
     * （append-only 时间倒序，有界——与技能清单 List 直出同形，不造分页）。</p>
     *
     * @param sourcePackage 来源包标识（清单行原值回传；可缺省——空/缺由 provider 判 SKL_014，admin 不拦）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（400 SKL_014 等，不做映射）
     */
    public List<AiplatformSkillUpdateTraceWireResponse> listSkillUpdateTraces(String sourcePackage) {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/backoffice/skills/update-traces");
        appendParam(url, "sourcePackage", sourcePackage);
        log.debug("AiplatformClient.listSkillUpdateTraces: {}", url);
        try {
            ApiResponse<List<AiplatformSkillUpdateTraceWireResponse>> resp =
                    openApiClient.get(url.toString(), SKILL_UPDATE_TRACE_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 智能体配置读面（透传 aiplatform，技能线 T5 读口）——该智能体生效配置：systemPrompt／
     * 模型档位生效值（库覆盖值或枚举默认——装配实取值）＋覆盖标记（现在跑的是覆盖还是默认）
     * ＋枚举默认预览（清空覆盖即落此值）＋增强工具开关存储态＋最近写者。无覆盖行＝全回落
     * （覆盖标记 false、写者 null）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/agent-configs/{agentKey}}（#251）：返回
     * {@code ApiResponse<BackofficeAgentConfigResponse>}。{@code agentKey} 为字符串路径参数
     * （main/executor 两把稳定键）原样出站——admin 透传不预校验，未知键由 provider 判 404
     * PRJ_033（数字业务码 4033，classify/naming 等一次性判定不属配置面）原样透传。</p>
     *
     * @param agentKey 智能体稳定键（main/executor，admin 不预检）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_033 等，不做映射）
     */
    public AiplatformAgentConfigWireResponse getAgentConfig(String agentKey) {
        String url = baseUrl + "/api/backoffice/agent-configs/" + encode(agentKey);
        log.debug("AiplatformClient.getAgentConfig: {}", url);
        try {
            ApiResponse<AiplatformAgentConfigWireResponse> resp = openApiClient.get(url, AGENT_CONFIG_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 智能体配置全量写（透传 aiplatform，技能线 T5 写口）——PUT 全量语义：请求体即该智能体
     * 覆盖态终态（systemPrompt/modelId 全文替换，null/缺省/纯空白＝清空覆盖回落枚举默认；
     * 工具开关缺省 true＝开）。装配生效＝动态查库（变更后下一轮命令构建自然取新值——智能体
     * 工厂缓存键含 sysPrompt、模型串与工具面规格，新值即新实例，进行中 run 不定格）。
     *
     * <p>对接 aiplatform {@code PUT /api/backoffice/agent-configs/{agentKey}}（#251）：请求体
     * {@code {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled}} 逐字镜像（开关两件
     * Boolean 装箱——null 出站在场＝provider 缺省 true），HTTP <strong>PUT</strong> 动词
     * ——经框架 {@link OpenApiClient#put}（与 {@code post} 同走 sendWithBody 序列化＋签名路径，
     * 槽位指派先例），返回 {@code ApiResponse<BackofficeAgentConfigResponse>}（回执与 GET
     * 同形）。变更留痕 append-only（值面有动必落痕——前后全量值快照＋操作者，traces 可查）；
     * 同值重写＝幂等回执不落痕；回滚＝留痕旧值快照写回（一次新变更、留新痕）。档位名不做
     * 白名单校验（错值致新会话失败经留痕写回可回滚）。操作者身份经框架 {@code OpenApiClient}
     * 自动带 {@code X-User-Id/X-User-Name} 头（RequestContext→provider 落痕——配置变更
     * 必留痕，<strong>缺头 400 PRJ_034</strong>＝数字业务码 4034）；寻址同 GET（未知键 404
     * PRJ_033＝4033）——均原样透传。</p>
     *
     * @param agentKey 智能体稳定键（main/executor，admin 不预检）
     * @param command  配置命令体（由应用层从北向命令映射而来，字段合法性归 provider 写用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_033、400 PRJ_034 等，不做映射）
     */
    public AiplatformAgentConfigWireResponse updateAgentConfig(
            String agentKey, AiplatformAgentConfigUpdateWireRequest command) {
        String url = baseUrl + "/api/backoffice/agent-configs/" + encode(agentKey);
        log.debug("AiplatformClient.updateAgentConfig: {}", url);
        try {
            ApiResponse<AiplatformAgentConfigWireResponse> resp =
                    openApiClient.put(url, command, AGENT_CONFIG_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 配置变更留痕读面（透传 aiplatform，技能线 T5 读口）——该智能体全部配置变更留痕
     * （变更前后全量值快照 old&#42;/new&#42; 四件对——旧值快照即回滚写回依据），按时间倒序
     * （最近先）。append-only：留痕不随任何动作删除（历史事实不随配置行消失）。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/agent-configs/{agentKey}/traces}（#251）：
     * 返回 {@code ApiResponse<List<BackofficeAgentConfigTraceResponse>>}——data 为裸 List
     * （append-only 时间倒序，有界——与技能更新留痕 List 直出同形，不造分页）。寻址同 GET
     * （未知键 404 PRJ_033＝4033）原样透传。</p>
     *
     * @param agentKey 智能体稳定键（main/executor，admin 不预检）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_033 等，不做映射）
     */
    public List<AiplatformAgentConfigTraceWireResponse> listAgentConfigTraces(String agentKey) {
        String url = baseUrl + "/api/backoffice/agent-configs/" + encode(agentKey) + "/traces";
        log.debug("AiplatformClient.listAgentConfigTraces: {}", url);
        try {
            ApiResponse<List<AiplatformAgentConfigTraceWireResponse>> resp =
                    openApiClient.get(url, AGENT_CONFIG_TRACE_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 工具面清单读面（透传 aiplatform，技能线 T6 读口）——按职能槽位（main=主智能体 /
     * executor=run 执行体 / subagent=子智能体，与技能槽位同键）列当前挂载工具。三呈现源：
     * 平台资产（骨架/增强——增强 enabled 按运营配置生效态，false＝在册但退出装配面）、
     * harness 内建编码工具（executor 槽，不可开关；main 只读面结构性无）、子智能体声明
     * 工具面。呈现序 provider 定死（平台资产在前、harness 内建在后），admin 原样保序透传。
     *
     * <p>对接 aiplatform {@code GET /api/backoffice/agent-tools}（#252）：返回
     * {@code ApiResponse<List<BackofficeAgentToolSlotResponse>>}——data 为裸 List（三槽位
     * 有界，List 直出同形不造分页）。清单读面即工具全集（PUT 开关按注册名寻址的正本）。</p>
     *
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（不做映射）
     */
    public List<AiplatformAgentToolSlotWireResponse> listAgentTools() {
        String url = baseUrl + "/api/backoffice/agent-tools";
        log.debug("AiplatformClient.listAgentTools: {}", url);
        try {
            ApiResponse<List<AiplatformAgentToolSlotWireResponse>> resp =
                    openApiClient.get(url, AGENT_TOOL_SLOT_LIST_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 增强工具开关（透传 aiplatform，技能线 T6 写口）——按工具注册名窄幅开关，仅增强工具
     * （web_search/fetch_url）接受：enabled=false 该工具退出对应槽位装配面（模型不可见），
     * true 回归——动态生效（下一轮命令构建即新装配，进行中 run 不定格）。变更走配置留痕
     * （与智能体配置同机制：值面有动才落痕、幂等回执零写入、回滚＝旧值写回）。
     *
     * <p>对接 aiplatform {@code PUT /api/backoffice/agent-tools/{toolName}}（#252）：请求体
     * {@code {enabled}} 单字段逐字镜像（Boolean 装箱——null 出站在场＝provider 判缺目标态
     * 400 PRJ_037＝数字业务码 4037，无缺省翻转语义），HTTP <strong>PUT</strong> 动词
     * ——经框架 {@link OpenApiClient#put}（与 {@code post} 同走 sendWithBody 序列化＋签名
     * 路径，槽位指派/智能体配置先例），返回 {@code ApiResponse<BackofficeAgentToolResponse>}
     * （回执＝开关后工具行，与清单嵌套行同形）。{@code toolName} 为字符串路径参数
     * （工具注册名）原样出站——admin 透传不预校验：未知名 404 PRJ_035（4035——清单读面
     * 即全集）、骨架/harness 内建 403 PRJ_036（4036——ADR-0021 编排权不下放配置）均
     * provider 裁决原样透传。操作者身份经框架 {@code OpenApiClient} 自动带
     * {@code X-User-Id/X-User-Name} 头（开关写必留痕，<strong>缺头 400 PRJ_034</strong>
     * ＝数字业务码 4034）。</p>
     *
     * @param toolName 工具注册名（增强两件 web_search/fetch_url；admin 不预检）
     * @param command  开关命令体（由应用层从北向命令映射而来——缺 body 传 null enabled，目标态合法性归 provider 写用例）
     * @throws AiplatformUpstreamException aiplatform 错误信封透传（404 PRJ_035、403 PRJ_036、400 PRJ_037/034 等，不做映射）
     */
    public AiplatformAgentToolWireResponse toggleAgentTool(
            String toolName, AiplatformAgentToolToggleWireRequest command) {
        String url = baseUrl + "/api/backoffice/agent-tools/" + encode(toolName);
        log.debug("AiplatformClient.toggleAgentTool: {}", url);
        try {
            ApiResponse<AiplatformAgentToolWireResponse> resp =
                    openApiClient.put(url, command, AGENT_TOOL_TYPEREF);
            return resp.data();
        } catch (OpenApiClientException e) {
            throw AiplatformUpstreamException.from(e);
        }
    }

    /**
     * 成本域四读口共用的时间窗拼接：{@code from}/{@code to} 半开 [from, to)。纯委托
     * {@link #appendParam}——Instant 取其默认分支 {@code Instant.toString()}（ISO_INSTANT：
     * 秒恒在场、UTC 带 Z），null 统一省略；总览/unpriced/单项目下钻无既有查询参数（首参前缀
     * {@code ?}）、项目成本清单在 page/size 之后（前缀 {@code &}），分隔符由 appendParam 裁决。
     */
    private static void appendWindow(StringBuilder url, AiplatformCostWindowWireRequest window) {
        appendParam(url, "from", window.from());
        appendParam(url, "to", window.to());
    }

    private static void appendParam(StringBuilder url, String name, Object value) {
        if (value != null) {
            // 首个查询参数前缀 ?、后续前缀 &（各域清单端点 page/size 恒在先，成本域时间窗可在首）
            url.append(url.indexOf("?") < 0 ? '?' : '&');
            if (value instanceof LocalDateTime time) {
                url.append(name).append('=').append(encode(time.format(ISO_SECONDS)));
            } else {
                url.append(name).append('=').append(encode(value.toString()));
            }
        }
    }

    private static String encode(String value) {
        // 与 PaymentClient / AccountClient 一致：简单 URL 编码，避免特殊字符问题（OIDC sub 可含 | 等）；
        // 编码后值与 provider 签名侧（request.getQueryString() raw 值）同形入签
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
