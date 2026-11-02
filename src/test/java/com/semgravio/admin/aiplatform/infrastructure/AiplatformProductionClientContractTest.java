package com.semgravio.admin.aiplatform.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformProductionAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionSupplementCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionUpdateCommand;

/**
 * aiplatform 生产承接域（issue #107，spec #102 A 批 A4——provider aiplatform #338 交付承接）
 * 四端点的 wire 契约测试：承接详情（两态＋留痕时间线）/ 信息整组更新（联系人次/下一步/
 * 进展/预计开始，null 即清空）/ 推进（待安排生产→生产中两态唯一边）/ 「需用户补充」
 * 置位/清零（前台六态盖帽标志）。
 *
 * <p>契约事实（对照 aiplatform 源码 {@code BackofficeProductionController} /
 * {@code BackofficeProductionHandoffResponse} /
 * {@code UpdateProductionHandoffCommand} / {@code ProductionHandoffActionCommand} /
 * {@code SetNeedsUserSupplementCommand} / {@code ProductionHandoffStatus} /
 * {@code ProductionHandoffAction} / {@code ProjectMessage}；fixture 实值对照 provider
 * {@code BackofficeProductionControllerTest} 兜底）：</p>
 * <ul>
 *   <li>四端点均包 {@code ApiResponse<T>} 信封，须按信封反序列化并取 {@code .data()}；
 *       详情/更新/推进回执 data＝BackofficeProductionHandoffResponse（承接现值＋留痕
 *       时间线），「需用户补充」回执 data＝null（ApiResponse&lt;Void&gt;——置位类动作无
 *       回读面，回执即信封本身）。</li>
 *   <li>两态 {@code status} Integer code + {@code statusName} 中文名（BaseEnum 约定，
 *       provider 出 JSON number）：<strong>1=待安排生产 2=生产中</strong>——单向推进
 *       （两态唯一边），无终态概念（承接是交付过程的状态承接页）；admin Integer code
 *       直通不建 typed enum（合同六态/付款五态先例）。留痕动作 code：<strong>1=创建
 *       2=推进 3=信息更新</strong>——创建行由开工判定系统触发落（operatorId=
 *       {@code system}/operatorName=开工判定），非迁移类（创建/信息更新）在留痕行上
 *       fromStatus = toStatus（在场快照），迁移类（推进）如实记录两侧。</li>
 *   <li>三写口命令体逐字镜像：更新 {@code {contact, nextStep, progressNote,
 *       estimatedStartAt, basis}}（provider UpdateProductionHandoffCommand 同序）、推进
 *       {@code {basis}}、置位 {@code {required}}——命令字段全 String/LocalDateTime/
 *       boolean，无 Long 金额特殊口径（boot 默认 mapper 即生产口径）。</li>
 *   <li>错误透传（数字业务码＝域码 4×1000＋序号）：无承接记录 PRJ_055→4055（404）、
 *       状态守卫 PRJ_056→4056（409）、依据必填 PRJ_057→4057（400）、字段超长
 *       PRJ_058→4058（400）、<strong>操作者必携 PRJ_059→4059（400——缺
 *       X-User-Id/X-User-Name 透传头即拒，承接动作以操作者为审计正身）</strong>、
 *       项目不存在 PRJ_001→4001（404）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformProductionClientContractTest {

    /** 项目标识（TSID 十进制串——透传不解析不预校验，畸形/未寻址由 provider 判 404 PRJ_001）。 */
    private static final String PROJECT_ID = "3829409001234567";

    /**
     * aiplatform GET /api/backoffice/projects/{projectId}/production-handoff 的真实成功
     * 响应形状：两态第一实值（1=待安排生产）＋留痕时间线两行——最新行＝信息更新
     * （action=3，非迁移类 from=to=1 在场快照，操作者＝后台管理员），旧行＝创建
     * （action=1，<strong>系统触发建档</strong>——operatorId=system/operatorName=开工判定，
     * provider {@code BackofficeProductionControllerTest} 实值口径）。两态/动作 Integer
     * 出 JSON number。
     */
    private static final String HANDOFF_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829410000000001",
                "projectId": "3829409001234567",
                "orderId": "3829492001234567",
                "status": 1,
                "statusName": "待安排生产",
                "contact": "李经理（微信 ligoods）",
                "nextStep": "准备服务器环境与域名备案",
                "progressNote": "已完成技术方案拆解",
                "estimatedStartAt": "2026-10-20T09:00:00",
                "createdAt": "2026-10-17T10:00:00",
                "transitions": [
                  {
                    "action": 3,
                    "actionName": "信息更新",
                    "fromStatus": 1,
                    "fromStatusName": "待安排生产",
                    "toStatus": 1,
                    "toStatusName": "待安排生产",
                    "basis": "首次排期录入",
                    "operatorId": "700100",
                    "operatorName": "生产承接_老王",
                    "createdAt": "2026-10-18T14:30:00"
                  },
                  {
                    "action": 1,
                    "actionName": "创建",
                    "fromStatus": 1,
                    "fromStatusName": "待安排生产",
                    "toStatus": 1,
                    "toStatusName": "待安排生产",
                    "basis": "开工条件满足（合同签署完成且开工门节点全部到账），创建生产承接记录",
                    "operatorId": "system",
                    "operatorName": "开工判定",
                    "createdAt": "2026-10-17T10:00:00"
                  }
                ]
              }
            }
            """;

    /**
     * 推进回执形状：两态第二实值（2=生产中）——两态唯一边走完后的承接现值（provider
     * {@code BackofficeProductionControllerTest} advance 用例口径：信息段全 null 如实透传）。
     */
    private static final String ADVANCE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3829410000000001",
                "projectId": "3829409001234567",
                "orderId": "3829492001234567",
                "status": 2,
                "statusName": "生产中",
                "contact": null,
                "nextStep": null,
                "progressNote": null,
                "estimatedStartAt": null,
                "createdAt": "2026-10-17T10:00:00",
                "transitions": []
              }
            }
            """;

    /** 「需用户补充」置位回执形状：ApiResponse&lt;Void&gt;——data null（无回读面，信封即回执）。 */
    private static final String SUPPLEMENT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": null
            }
            """;

    @Test
    void given_handoffEnvelope_when_getHandoff_then_firstStatePinnedAndTrailMapped() {
        String[] wireUrl = new String[1];
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithStubTransport(HANDOFF_ENVELOPE, wireUrl, new String[1]);

        var handoff = appService.getHandoff(PROJECT_ID);

        // 出站路径逐字镜像 provider backoffice 路由（承接挂项目唯一，按项目寻址）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/" + PROJECT_ID + "/production-handoff");

        // 承接字段逐位映射：两态第一实值 1=待安排生产＋可空段如实非 null 值
        assertThat(handoff.status()).isEqualTo(1);
        assertThat(handoff.statusName()).isEqualTo("待安排生产");
        assertThat(handoff.contact()).isEqualTo("李经理（微信 ligoods）");
        assertThat(handoff.nextStep()).isEqualTo("准备服务器环境与域名备案");
        assertThat(handoff.progressNote()).isEqualTo("已完成技术方案拆解");
        assertThat(handoff.estimatedStartAt()).isEqualTo(java.time.LocalDateTime.of(2026, 10, 20, 9, 0, 0));

        // 留痕时间线（新→旧）：最新行＝信息更新（action=3，非迁移类 from=to 在场快照）
        assertThat(handoff.transitions()).hasSize(2);
        var update = handoff.transitions().get(0);
        assertThat(update.action()).isEqualTo(3);
        assertThat(update.actionName()).isEqualTo("信息更新");
        assertThat(update.fromStatus()).isEqualTo(1);
        assertThat(update.toStatus()).isEqualTo(1);
        assertThat(update.operatorId()).isEqualTo("700100");
        assertThat(update.basis()).isEqualTo("首次排期录入");

        // 旧行＝创建（action=1，系统触发建档——操作者＝system/开工判定，非后台管理员）
        var create = handoff.transitions().get(1);
        assertThat(create.action()).isEqualTo(1);
        assertThat(create.actionName()).isEqualTo("创建");
        assertThat(create.operatorId()).isEqualTo("system");
        assertThat(create.operatorName()).isEqualTo("开工判定");
        assertThat(create.basis()).contains("开工条件满足");
    }

    @Test
    void given_updateCommand_when_update_then_wireBodyMirroredAndReceiptMapped() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        AiplatformProductionAppService appService = AiplatformWireTestSupport
                .productionAppServiceWithStubTransport(HANDOFF_ENVELOPE, wireUrl, wireBody);

        var receipt = appService.update(PROJECT_ID, new AiplatformProductionUpdateCommand(
                "李经理（微信 ligoods）", "准备服务器环境与域名备案", "已完成技术方案拆解",
                java.time.LocalDateTime.of(2026, 10, 20, 9, 0, 0), "首次排期录入"));

        // 出站路径＋命令体逐字镜像（五字段同序：contact/nextStep/progressNote/estimatedStartAt/basis）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/" + PROJECT_ID + "/production-handoff/update");
        assertThat(wireBody[0]).isEqualTo("""
                {"contact":"李经理（微信 ligoods）","nextStep":"准备服务器环境与域名备案",\
                "progressNote":"已完成技术方案拆解","estimatedStartAt":"2026-10-20T09:00:00","basis":"首次排期录入"}""");

        // 更新回执＝承接现值（整组替换后）
        assertThat(receipt.status()).isEqualTo(1);
        assertThat(receipt.contact()).isEqualTo("李经理（微信 ligoods）");
    }

    @Test
    void given_advanceCommand_when_advance_then_wireBodyBasisOnly_andSecondStatePinned() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        AiplatformProductionAppService appService = AiplatformWireTestSupport
                .productionAppServiceWithStubTransport(ADVANCE_ENVELOPE, wireUrl, wireBody);

        var receipt = appService.advance(PROJECT_ID, new AiplatformProductionActionCommand("排期落定，今日开工"));

        // 出站路径＋命令体逐字镜像（单字段 {basis}）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/" + PROJECT_ID + "/production-handoff/advance");
        assertThat(wireBody[0]).isEqualTo("{\"basis\":\"排期落定，今日开工\"}");

        // 两态第二实值：2=生产中（唯一边走完；可空段 null 如实透传）
        assertThat(receipt.status()).isEqualTo(2);
        assertThat(receipt.statusName()).isEqualTo("生产中");
        assertThat(receipt.contact()).isNull();
        assertThat(receipt.estimatedStartAt()).isNull();
    }

    @Test
    void given_supplementCommand_when_setNeedsUserSupplement_then_wireBodyRequiredOnly() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        AiplatformProductionAppService appService = AiplatformWireTestSupport
                .productionAppServiceWithStubTransport(SUPPLEMENT_ENVELOPE, wireUrl, wireBody);

        appService.setNeedsUserSupplement(PROJECT_ID, new AiplatformProductionSupplementCommand(true));

        // 出站路径＋命令体逐字镜像（单字段 {required}）；回执 data null 无回读面（方法 void）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/" + PROJECT_ID
                        + "/production-handoff/needs-user-supplement");
        assertThat(wireBody[0]).isEqualTo("{\"required\":true}");
    }

    @Test
    void given_prj055NoHandoff_when_getHandoff_then_errorEnvelopePassedThrough() {
        String[] wireUrl = new String[1];
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(404,
                        "{\"code\":4055,\"message\":\"生产承接记录不存在\",\"data\":null}", wireUrl);

        // 忠实透传（spec #102）：HTTP 404 + 数字业务码 4055（PRJ_055＝域码 4×1000＋55）
        // + provider message 原样——无承接记录（开工判定未触发——项目尚在签署/付款前段）
        assertThatThrownBy(() -> appService.getHandoff(PROJECT_ID))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4055);
                    assertThat(upstream.getMessage()).isEqualTo("生产承接记录不存在");
                });
        assertThat(wireUrl[0]).contains("/api/backoffice/projects/" + PROJECT_ID + "/production-handoff");
    }

    @Test
    void given_prj056AlreadyInProduction_when_advance_then_errorEnvelopePassedThrough() {
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(409,
                        "{\"code\":4056,\"message\":\"生产承接当前状态不允许该操作\",\"data\":null}", new String[1]);

        // 生产中是稳态（两态唯一边 1→2，再推进 409 PRJ_056→4056）——守卫归 provider，BFF 不预判
        assertThatThrownBy(() -> appService.advance(PROJECT_ID,
                new AiplatformProductionActionCommand("再推")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(4056);
                    assertThat(upstream.getMessage()).isEqualTo("生产承接当前状态不允许该操作");
                });
    }

    @Test
    void given_prj057BlankBasis_when_update_then_errorEnvelopePassedThrough() {
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(400,
                        "{\"code\":4057,\"message\":\"生产承接动作依据必填（人工推进留痕须填写依据）\",\"data\":null}",
                        new String[1]);

        // 依据必填 400 PRJ_057→4057（留痕正身缺失）——BFF 不前置拦截，透传错误信封
        assertThatThrownBy(() -> appService.update(PROJECT_ID, new AiplatformProductionUpdateCommand(
                null, null, null, null, " ")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4057);
                    assertThat(upstream.getMessage()).isEqualTo("生产承接动作依据必填（人工推进留痕须填写依据）");
                });
    }

    @Test
    void given_prj058FieldTooLong_when_update_then_errorEnvelopePassedThrough() {
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(400,
                        "{\"code\":4058,\"message\":\"生产承接字段超长\",\"data\":null}", new String[1]);

        // 字段超长 400 PRJ_058→4058（联系人 200/下一步 1000/进展 2000/依据 1000 字上限归 provider）
        assertThatThrownBy(() -> appService.update(PROJECT_ID, new AiplatformProductionUpdateCommand(
                        "联", "下", "进", null, "据".repeat(1001))))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4058);
                    assertThat(upstream.getMessage()).isEqualTo("生产承接字段超长");
                });
    }

    @Test
    void given_prj059OperatorMissing_when_advance_then_errorEnvelopePassedThrough() {
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(400,
                        "{\"code\":4059,\"message\":\"操作者必填（生产承接动作须携带操作者身份）\",\"data\":null}",
                        new String[1]);

        // 缺操作者透传头（X-User-Id/X-User-Name）400 PRJ_059→4059——承接动作以操作者为审计
        // 正身，缺头拒不留无名痕（admin 侧登录运营恒带头，此负例钉透传契约的兜底面）
        assertThatThrownBy(() -> appService.advance(PROJECT_ID,
                new AiplatformProductionActionCommand("排期落定")))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(4059);
                    assertThat(upstream.getMessage()).isEqualTo("操作者必填（生产承接动作须携带操作者身份）");
                });
    }

    @Test
    void given_prj001ProjectMissing_when_setNeedsUserSupplement_then_errorEnvelopePassedThrough() {
        AiplatformProductionAppService appService =
                AiplatformWireTestSupport.productionAppServiceWithErrorTransport(404,
                        "{\"code\":4001,\"message\":\"项目不存在\",\"data\":null}", new String[1]);

        // 项目不存在 404 PRJ_001→4001（置位类动作同守卫——畸形 id 同译）
        assertThatThrownBy(() -> appService.setNeedsUserSupplement(
                "not-a-tsid", new AiplatformProductionSupplementCommand(true)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4001);
                    assertThat(upstream.getMessage()).isEqualTo("项目不存在");
                });
    }
}
