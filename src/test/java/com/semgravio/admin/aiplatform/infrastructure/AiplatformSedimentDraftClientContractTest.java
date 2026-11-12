package com.semgravio.admin.aiplatform.infrastructure;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRejectCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformSedimentDraftRelabelCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformSedimentDraftQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftDetailResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformSedimentDraftSummaryResponse;
import com.cartisan.web.response.PageResponse;

/**
 * aiplatform 沉淀草稿「数据标注」域（标注队列/详情/激活/拒绝/改标签）的 wire 契约测试
 * （对接 aiplatform #464 数据飞轮采集层人审口，issue #124）。
 *
 * <p>契约事实（2026-11-12 对照 aiplatform 源码 {@code BackofficeSedimentDraftController}
 * （43a7966）/{@code SedimentDraftAppService} /{@code BackofficeSedimentDraftSummaryResponse}
 * /{@code BackofficeSedimentDraftDetailResponse} /{@code SedimentDraftStatus} /
 * {@code KnowledgeMessage} 核实）：</p>
 * <ul>
 *   <li>五端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}；队列 data
 *       为 {@code PageResponse{items,total,page,size}}（page 1-based、total JSON string——
 *       cartisan-web 全局 Long→ToStringSerializer，provider {@code Pagination} 契约缺省 1/20
 *       上界 100、越界静默 clamp）；详情 data 为队列行同形字段＋content＋payload＋
 *       scanFindings＋终态留痕四件；三键回执共形＝详情单对象（激活/拒绝回终态、改标签回
 *       现值——与 GET 详情同形）。</li>
 *   <li>行字段：{@code id} String（TSID 十进制串）；{@code kind} 类别稳定键（CASE/MODULE/
 *       RULE）；血统三件 projectId/runId/slot；{@code scanVerdict} SAFE/CAUTION 字符串透传；
 *       {@code inflightSlot} 在途序号 1-3 Integer；{@code status} Integer code（1=在途
 *       2=已激活 3=已拒绝）＋{@code statusName} 中文名随行；{@code createdAt} LocalDateTime
 *       （无 Z 后缀）；{@code payload}/{@code scanFindings} untyped Map/List 透传（一期恒
 *       空面/逐条 patternId-severity-category-file-line-matchText-description）。</li>
 *   <li>队列三过滤可组合、均可缺省（null 出站省略）：projectId <strong>宽松</strong>（畸形值
 *       provider {@code Tsid.parseOrNull} 归 null 即缺省不过滤——与 kind 非法即 400 有意
 *       不同，admin 原样透传不代判）、kind 类别单选（非法 400 KNW_011 带合法取值表）、
 *       status 状态单选 Integer code（未知 code 骑 provider 框架 400 带合法取值表）；排序
 *       服务端定死＝自荐时间倒序（id 倒序稳定）；page 1-based 直传，provider clamp 行为
 *       透传。</li>
 *   <li>激活<strong>无请求体</strong>（{@code post(url, null, …)} 发空 body）；拒绝/改标签
 *       单字段命令体可选绑定不前置拦截（缺 body 由 provider 裁决同码）。</li>
 *   <li>错误透传（数字业务码＝KNW 域码 2×1000＋序号）：KNW_006→2006（400，操作者不能为空
 *       ——缺 X-User-Id/X-User-Name 出站头必被拦，标注动作必留痕）、KNW_008→2008（404，
 *       沉淀草稿不存在——含畸形 id）、KNW_009→2009（409，已审结重按——三键一律要求在途）、
 *       KNW_010→2010（400，拒绝理由空白）、KNW_011→2011（400，类别非法）、KNW_012→2012
 *       （409，目标类别在途满 3）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire
 * 反序列化——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient}
 * 仅替换 HTTP 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformSedimentDraftClientContractTest {

    /** 标注队列首行（在途待审代表面）：inflightSlot=1、status=1、终态留痕不在清单行。 */
    private static final String QUEUE_ROW_PENDING = """
            {
              "id": "3853000001111111",
              "kind": "CASE",
              "title": "排课冲突处理案例",
              "summary": "两名教师同时段占用同一教室的冲突调解流程",
              "slot": "executor",
              "projectId": "3829492007654321",
              "runId": "run-7c31ab",
              "scanVerdict": "SAFE",
              "inflightSlot": 1,
              "status": 1,
              "statusName": "在途",
              "createdAt": "2026-11-10T09:15:30"
            }
            """;

    /**
     * aiplatform GET /api/backoffice/sediment-drafts 的真实成功响应形状（信封＋PageResponse）：
     * 自荐时间倒序两行——首行在途（SAFE），次行已激活终态（终态留档可查：队列缺省含终态行
     * ——待办视角须显式 status=1；inflightSlot=2 代表跨类别占位）。total 为 JSON string。
     */
    private static final String QUEUE_PAGE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "items": [
                  %s,
                  {
                    "id": "3853000002222222",
                    "kind": "RULE",
                    "title": "退款审批业务规则",
                    "summary": "超过 7 天的退款须二级审批",
                    "slot": "main",
                    "projectId": "3829492005555444",
                    "runId": "run-9f22e1",
                    "scanVerdict": "CAUTION",
                    "inflightSlot": 2,
                    "status": 2,
                    "statusName": "已激活",
                    "createdAt": "2026-11-08T14:00:00"
                  }
                ],
                "total": "2",
                "page": 1,
                "size": 20
              },
              "requestId": "req-s1d2r",
              "errors": null
            }
            """.formatted(QUEUE_ROW_PENDING);

    /**
     * aiplatform GET /api/backoffice/sediment-drafts/{id} 的真实成功响应形状（在途代表面）：
     * 队列行同形字段＋正文全文＋payload（一期恒空面）＋scanFindings 逐条留档（line 为数字
     * ——untyped Object 绑定混合类型在场）＋终态留痕四字段在途恒 null（如实绑定）。
     */
    private static final String DETAIL_PENDING_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3853000001111111",
                "kind": "CASE",
                "title": "排课冲突处理案例",
                "summary": "两名教师同时段占用同一教室的冲突调解流程",
                "slot": "executor",
                "projectId": "3829492007654321",
                "runId": "run-7c31ab",
                "scanVerdict": "SAFE",
                "inflightSlot": 1,
                "status": 1,
                "statusName": "在途",
                "createdAt": "2026-11-10T09:15:30",
                "content": "# 排课冲突处理\\n\\n当两名教师同时段占用同一教室时……\\n\\n## 调解流程\\n\\n1. 查双方可调剂时段",
                "payload": {},
                "scanFindings": [
                  {
                    "patternId": "secret-like-token",
                    "severity": "CAUTION",
                    "category": "security",
                    "file": "CASE.md",
                    "line": 42,
                    "matchText": "sk-***",
                    "description": "疑似密钥形态字符串"
                  }
                ],
                "operatorId": null,
                "operatorName": null,
                "reviewedAt": null,
                "rejectReason": null
              },
              "requestId": "req-s3t4a",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/sediment-drafts/{id}/reject 的真实成功响应形状（终态
     * 代表面——三键回执共形＝详情）：已拒绝终态、留痕四字段回填直读（操作者/时刻/理由随行
     * 留档）、inflightSlot 已释放为 null。
     */
    private static final String REJECT_TERMINAL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3853000001111111",
                "kind": "CASE",
                "title": "排课冲突处理案例",
                "summary": "两名教师同时段占用同一教室的冲突调解流程",
                "slot": "executor",
                "projectId": "3829492007654321",
                "runId": "run-7c31ab",
                "scanVerdict": "SAFE",
                "inflightSlot": null,
                "status": 3,
                "statusName": "已拒绝",
                "createdAt": "2026-11-10T09:15:30",
                "content": "# 排课冲突处理\\n\\n……",
                "payload": {},
                "scanFindings": [],
                "operatorId": "3829492001234567",
                "operatorName": "数据标注运营",
                "reviewedAt": "2026-11-12T10:30:00",
                "rejectReason": "与已激活知识「排课冲突案例」内容重叠，无增量价值"
              },
              "requestId": "req-s5j6e",
              "errors": null
            }
            """;

    private static final String DRAFT_ID = "3853000001111111";

    /** 操作者缺（缺 X-User-Id/X-User-Name 出站头）：HTTP 400＋信封 code=2006（KNW_006，域码 2）。 */
    private static final String KNW_006_ERROR_ENVELOPE = """
            {
              "code": 2006,
              "message": "操作者不能为空",
              "data": null,
              "requestId": "req-k6s0p",
              "errors": null
            }
            """;

    /** 沉淀草稿不存在（含畸形 id）：HTTP 404＋信封 code=2008（KNW_008）。 */
    private static final String KNW_008_ERROR_ENVELOPE = """
            {
              "code": 2008,
              "message": "沉淀草稿不存在",
              "data": null,
              "requestId": "req-k8s0p",
              "errors": null
            }
            """;

    /** 已审结重按（三键一律要求在途）：HTTP 409＋信封 code=2009（KNW_009）。 */
    private static final String KNW_009_ERROR_ENVELOPE = """
            {
              "code": 2009,
              "message": "沉淀草稿已审结（已激活/已拒绝），终态不可再审——重提为新草稿",
              "data": null,
              "requestId": "req-k9s0p",
              "errors": null
            }
            """;

    /** 拒绝理由空白：HTTP 400＋信封 code=2010（KNW_010）。 */
    private static final String KNW_010_ERROR_ENVELOPE = """
            {
              "code": 2010,
              "message": "拒绝理由不能为空（终态留档，拒绝须有据）",
              "data": null,
              "requestId": "req-ks10p",
              "errors": null
            }
            """;

    /** 类别非法：HTTP 400＋信封 code=2011（KNW_011，带合法取值表）。 */
    private static final String KNW_011_ERROR_ENVELOPE = """
            {
              "code": 2011,
              "message": "沉淀物类别非法，合法取值：CASE（需求案例）/MODULE（功能模块）/RULE（业务规则）",
              "data": null,
              "requestId": "req-ks11p",
              "errors": null
            }
            """;

    /** 目标类别在途满 3：HTTP 409＋信封 code=2012（KNW_012）。 */
    private static final String KNW_012_ERROR_ENVELOPE = """
            {
              "code": 2012,
              "message": "目标类别在途草稿已满 3 条——先审结该类别在途草稿再改标签",
              "data": null,
              "requestId": "req-ks12p",
              "errors": null
            }
            """;

    // ========== 队列（三过滤透传＋1-based 分页）==========

    @Test
    void given_filtersAndPage_when_list_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                QUEUE_PAGE_ENVELOPE, wireUrl, new String[1]);

        appService.list(new AiplatformSedimentDraftQuery(
                "3829492007654321", "CASE", 1), 1, 20);

        // 出站 wire 形状逐字镜像 provider 契约：page 1-based 直传零换算，三过滤维度
        // projectId/kind/status 逐名透传（projectId 宽松口径＝字符串原样出站，畸形值归
        // provider 裁决不代判；kind/status 同透传不预校验）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/sediment-drafts"
                + "?page=1&size=20&projectId=3829492007654321&kind=CASE&status=1");
    }

    @Test
    void given_nullFilters_when_list_then_filtersOmittedFromWireUrl() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                QUEUE_PAGE_ENVELOPE, wireUrl, new String[1]);

        appService.list(new AiplatformSedimentDraftQuery(null, null, null), 2, 50);

        // 缺省＝全量（含终态行——终态留档可查，待办视角须显式 status=1）：
        // null 过滤维度不出站；page 直传
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/sediment-drafts?page=2&size=50");
    }

    @Test
    void given_queuePageEnvelope_when_list_then_envelopeUnwrappedAndRowsBound() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                QUEUE_PAGE_ENVELOPE, wireUrl, new String[1]);

        PageResponse<AiplatformSedimentDraftSummaryResponse> page = appService.list(
                new AiplatformSedimentDraftQuery(null, null, 1), 1, 20);

        // 信封正确拆开——证明 SEDIMENT_DRAFT_PAGE_TYPEREF 按 ApiResponse<PageResponse<…>>
        // 反序列化；total JSON string→long、分页回显 provider 回报值（1-based 含 clamp 后值）
        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);

        // 首行（最近自荐·在途待审）：kind 类别稳定键＋血统三件＋扫描判定 SAFE＋在途序号
        // inflightSlot 1-3（每项目每 kind 上限 3 的结构性承载）＋status Integer code＋
        // statusName 中文名随行＋createdAt LocalDateTime（无 Z）
        var pending = page.items().get(0);
        assertThat(pending.id()).isEqualTo(DRAFT_ID);
        assertThat(pending.kind()).isEqualTo("CASE");
        assertThat(pending.title()).isEqualTo("排课冲突处理案例");
        assertThat(pending.summary()).isEqualTo("两名教师同时段占用同一教室的冲突调解流程");
        assertThat(pending.slot()).isEqualTo("executor");
        assertThat(pending.projectId()).isEqualTo("3829492007654321");
        assertThat(pending.runId()).isEqualTo("run-7c31ab");
        assertThat(pending.scanVerdict()).isEqualTo("SAFE");
        assertThat(pending.inflightSlot()).isEqualTo(1);
        assertThat(pending.status()).isEqualTo(1);
        assertThat(pending.statusName()).isEqualTo("在途");
        assertThat(pending.createdAt()).isEqualTo(LocalDateTime.of(2026, 11, 10, 9, 15, 30));

        // 次行（终态行代表面）：队列缺省含终态行——status=2 已激活照常绑定（终态留档可查）
        var activated = page.items().get(1);
        assertThat(activated.id()).isEqualTo("3853000002222222");
        assertThat(activated.kind()).isEqualTo("RULE");
        assertThat(activated.scanVerdict()).isEqualTo("CAUTION");
        assertThat(activated.inflightSlot()).isEqualTo(2);
        assertThat(activated.status()).isEqualTo(2);
        assertThat(activated.statusName()).isEqualTo("已激活");
    }

    // ========== 详情（正文＋payload＋findings＋终态留痕）==========

    @Test
    void given_draftId_when_getDetail_then_wireUrlMirrorsProvider() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                DETAIL_PENDING_ENVELOPE, wireUrl, new String[1]);

        appService.getDetail(DRAFT_ID);

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/sediment-drafts/" + DRAFT_ID);
    }

    @Test
    void given_detailEnvelope_when_getDetail_then_contentPayloadFindingsAndNullTraceBound() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                DETAIL_PENDING_ENVELOPE, new String[1], new String[1]);

        AiplatformSedimentDraftDetailResponse detail = appService.getDetail(DRAFT_ID);

        // 详情＝队列行同形字段＋正文全文（激活原样升格不改稿——所见即入库内容）
        assertThat(detail.id()).isEqualTo(DRAFT_ID);
        assertThat(detail.kind()).isEqualTo("CASE");
        assertThat(detail.status()).isEqualTo(1);
        assertThat(detail.statusName()).isEqualTo("在途");
        assertThat(detail.content()).startsWith("# 排课冲突处理");
        assertThat(detail.content()).contains("\n\n## 调解流程\n\n1. 查双方可调剂时段");
        // payload 一期恒空面（untyped Map 逐字透传不做形状断言）
        assertThat(detail.payload()).isEmpty();
        // findings untyped map 逐条留档：line 为数字（Object 绑定混合类型在场）
        assertThat(detail.scanFindings()).hasSize(1);
        java.util.Map<String, Object> finding = detail.scanFindings().get(0);
        assertThat(finding.get("patternId")).isEqualTo("secret-like-token");
        assertThat(((Number) finding.get("line")).intValue()).isEqualTo(42);
        assertThat(finding.get("matchText")).isEqualTo("sk-***");
        // 终态留痕四字段在途恒 null（如实出 JSON null——全局 Jackson 含 null）
        assertThat(detail.operatorId()).isNull();
        assertThat(detail.operatorName()).isNull();
        assertThat(detail.reviewedAt()).isNull();
        assertThat(detail.rejectReason()).isNull();
    }

    // ========== 三键（激活无体／拒绝 reason／改标签 kind）==========

    @Test
    void given_draftId_when_activate_then_wireUrlMirrorsProviderAndNoBody() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                DETAIL_PENDING_ENVELOPE, wireUrl, wireBody);

        appService.activate(DRAFT_ID);

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/sediment-drafts/" + DRAFT_ID + "/activate");
        // 无请求体（provider 端只读路径参数——同技能草稿晋升先例）
        assertThat(wireBody[0]).isNull();
    }

    @Test
    void given_rejectCommand_when_reject_then_wireBodyMirrorsProviderAndTerminalReceiptBound() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                REJECT_TERMINAL_ENVELOPE, wireUrl, wireBody);

        AiplatformSedimentDraftDetailResponse receipt = appService.reject(DRAFT_ID,
                new AiplatformSedimentDraftRejectCommand("与已激活知识「排课冲突案例」内容重叠，无增量价值"));

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/sediment-drafts/" + DRAFT_ID + "/reject");
        // 命令体逐字镜像：单字段 {reason}
        assertThat(wireBody[0])
                .isEqualTo("{\"reason\":\"与已激活知识「排课冲突案例」内容重叠，无增量价值\"}");
        // 回执＝终态详情：status=3 已拒绝、留痕四字段回填直读（操作者/时刻/理由随行留档）、
        // 在途名额已释放（inflightSlot null）
        assertThat(receipt.status()).isEqualTo(3);
        assertThat(receipt.statusName()).isEqualTo("已拒绝");
        assertThat(receipt.operatorId()).isEqualTo("3829492001234567");
        assertThat(receipt.operatorName()).isEqualTo("数据标注运营");
        assertThat(receipt.reviewedAt()).isEqualTo(LocalDateTime.of(2026, 11, 12, 10, 30, 0));
        assertThat(receipt.rejectReason()).isEqualTo("与已激活知识「排课冲突案例」内容重叠，无增量价值");
        assertThat(receipt.inflightSlot()).isNull();
        assertThat(receipt.scanFindings()).isEmpty();
    }

    @Test
    void given_relabelCommand_when_relabel_then_wireBodyMirrorsProvider() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                DETAIL_PENDING_ENVELOPE, wireUrl, wireBody);

        appService.relabel(DRAFT_ID, new AiplatformSedimentDraftRelabelCommand("MODULE"));

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/sediment-drafts/" + DRAFT_ID + "/relabel");
        // 命令体逐字镜像：单字段 {kind}（非法取值由 provider 裁决 400 KNW_011——admin 不预校验）
        assertThat(wireBody[0]).isEqualTo("{\"kind\":\"MODULE\"}");
    }

    @Test
    void given_nullCommand_when_rejectAndRelabel_then_nullLimbPassedThroughForProviderVerdict() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithStubTransport(
                DETAIL_PENDING_ENVELOPE, wireUrl, wireBody);

        // 缺 body 可选绑定不前置拦截：北向 null 命令 → wire 单字段 record 包 null 肢
        // 出站在场（{"reason":null}——同技能草稿先例），裁决归 provider（拒绝缺 reason →
        // KNW_010；改标签缺 kind → KNW_011，缺 body 与 null 肢同语义同码）
        appService.reject(DRAFT_ID, null);
        assertThat(wireBody[0]).isEqualTo("{\"reason\":null}");
        appService.relabel(DRAFT_ID, null);
        assertThat(wireBody[0]).isEqualTo("{\"kind\":null}");
    }

    // ========== 错误透传（KNW_ 数字业务码，不映射）==========

    @Test
    void given_knw006ErrorEnvelope_when_activate_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                400, KNW_006_ERROR_ENVELOPE, new String[1]);

        // 忠实透传：HTTP 400＋数字业务码 2006（KNW_006＝域码 2×1000＋6）＋provider message
        // 原样——缺 X-User-Id/X-User-Name 出站头时 provider 域面守卫的必拦口径（标注动作
        // 必留痕，无落空通道）
        assertThatThrownBy(() -> appService.activate(DRAFT_ID))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(2006);
                    assertThat(upstream.getMessage()).isEqualTo("操作者不能为空");
                });
    }

    @Test
    void given_knw008ErrorEnvelope_when_getDetail_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                404, KNW_008_ERROR_ENVELOPE, new String[1]);

        // 畸形/未寻址 TSID：HTTP 404＋数字业务码 2008（KNW_008）＋message 原样
        assertThatThrownBy(() -> appService.getDetail(DRAFT_ID))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(2008);
                    assertThat(upstream.getMessage()).isEqualTo("沉淀草稿不存在");
                });
    }

    @Test
    void given_knw009ErrorEnvelope_when_reject_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                409, KNW_009_ERROR_ENVELOPE, new String[1]);

        // 已审结重按（三键一律要求在途——CAS 零行命中）：HTTP 409＋数字业务码 2009
        assertThatThrownBy(() -> appService.reject(DRAFT_ID,
                new AiplatformSedimentDraftRejectCommand("理由")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(2009);
                    assertThat(upstream.getMessage())
                            .isEqualTo("沉淀草稿已审结（已激活/已拒绝），终态不可再审——重提为新草稿");
                });
    }

    @Test
    void given_knw010ErrorEnvelope_when_rejectBlankReason_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                400, KNW_010_ERROR_ENVELOPE, new String[1]);

        // 拒绝理由空白（终态留档拒绝须有据——库级 CHECK 双锁）：HTTP 400＋数字业务码 2010
        assertThatThrownBy(() -> appService.reject(DRAFT_ID,
                new AiplatformSedimentDraftRejectCommand("   ")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(2010);
                    assertThat(upstream.getMessage()).isEqualTo("拒绝理由不能为空（终态留档，拒绝须有据）");
                });
    }

    @Test
    void given_knw011ErrorEnvelope_when_relabelInvalidKind_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                400, KNW_011_ERROR_ENVELOPE, new String[1]);

        // 类别非法（带合法取值表）：HTTP 400＋数字业务码 2011——admin 不预校验不代判
        assertThatThrownBy(() -> appService.relabel(DRAFT_ID,
                new AiplatformSedimentDraftRelabelCommand("PRD")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(2011);
                    assertThat(upstream.getMessage())
                            .isEqualTo("沉淀物类别非法，合法取值：CASE（需求案例）/MODULE（功能模块）/RULE（业务规则）");
                });
    }

    @Test
    void given_knw012ErrorEnvelope_when_relabelTargetKindFull_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.sedimentDraftAppServiceWithErrorTransport(
                409, KNW_012_ERROR_ENVELOPE, new String[1]);

        // 目标类别在途满 3（改标签跨类别占目标类别在途名额——结构性约束跨类别生效）：
        // HTTP 409＋数字业务码 2012
        assertThatThrownBy(() -> appService.relabel(DRAFT_ID,
                new AiplatformSedimentDraftRelabelCommand("MODULE")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(2012);
                    assertThat(upstream.getMessage())
                            .isEqualTo("目标类别在途草稿已满 3 条——先审结该类别在途草稿再改标签");
                });
    }
}
