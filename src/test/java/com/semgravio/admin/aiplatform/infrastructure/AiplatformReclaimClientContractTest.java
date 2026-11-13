package com.semgravio.admin.aiplatform.infrastructure;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformProjectAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.AiplatformWorkspaceAppService;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformReclaimResponse;

/**
 * aiplatform 后台「一键回收」（项目级 projects/{id}/reclaim＋工作区级
 * workspaces/{id}/reclaim，同形回执）的 wire 契约测试（对接 aiplatform #490，
 * issue #129）。
 *
 * <p>契约事实（2026-11-13 对照 aiplatform 源码 {@code BackofficeReclaimController}
 * /{@code BackofficeReclaimAppService} /{@code BackofficeReclaimResponse} /V65 迁移
 * {@code wsp_reclaim_actions} 核实）：</p>
 * <ul>
 *   <li>两端点均包 {@code ApiResponse<BackofficeReclaimResponse>} 单对象信封，按信封
 *       反序列化取 {@code .data()}；回执同形：scope/projectId/workspaceId/runsStopped/
 *       container/volume/registryRow/projectRow/residueFree/operatorId/operatorName/
 *       reclaimedAt 十二字段。</li>
 *   <li>两 POST 均<strong>无请求体</strong>（{@code post(url, null, …)} 发空 body——同沙箱
 *       动作/手动清扫先例）；URL＝{@code /api/backoffice/projects/{id}/reclaim} 与
 *       {@code /api/backoffice/workspaces/{id}/reclaim}。</li>
 *   <li>面态语义：容器/卷/注册表行三态 CLEAN/RESIDUE/UNKNOWN（UNKNOWN 不算清）；项目面
 *       项目级 CLEAN/RESIDUE、工作区级恒 UNTOUCHED（不动项目面如实分示）；residueFree＝
 *       scope 内受检面全 CLEAN（UNKNOWN/RESIDUE 任一面即 false）。</li>
 *   <li>runsStopped＝回收时有在飞 run 被级联停止（无在飞 false）；工作区级无所属项目
 *       （在册失联裁决/fork）projectId null。</li>
 *   <li>操作者两列（operatorId/operatorName）＝provider 落痕回显（X-User-Id/X-User-Name
 *       透传头缺头落空 NULL——照沙箱动作面口径，provider 不落强制守卫）。</li>
 *   <li>错误透传（数字业务码＝域码×1000＋序号）：项目不存在 404 PRJ_001→4001；工作区
 *       不存在（含畸形 id）404 WSP_001→1001；销毁自身失败 500（未成不留痕可重试）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire
 * 反序列化——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient}
 * 仅替换 HTTP 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformReclaimClientContractTest {

    /**
     * aiplatform POST /api/backoffice/projects/{id}/reclaim 的真实成功响应形状（信封＋
     * 单对象）：四面全 CLEAN＋runsStopped true（有在飞 run 被级联停止）＋residueFree
     * true＋操作者两列回显落痕值。
     */
    private static final String PROJECT_RECLAIM_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "scope": "PROJECT",
                "projectId": "3829492007654001",
                "workspaceId": "3829492007654321",
                "runsStopped": true,
                "container": "CLEAN",
                "volume": "CLEAN",
                "registryRow": "CLEAN",
                "projectRow": "CLEAN",
                "residueFree": true,
                "operatorId": "700101",
                "operatorName": "运维·老张",
                "reclaimedAt": "2026-11-13T09:30:00"
              },
              "requestId": "req-rclmp",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/workspaces/{id}/reclaim 的「失联裁决」成功形状：
     * 在册失联行（#488 lostAt——项目行已删故无所属）回收回执——projectId null＋三面
     * 全 CLEAN＋项目面 UNTOUCHED（如实分示：非清非残留）＋runsStopped false（无所属
     * 无停止面可挂）＋residueFree true。
     */
    private static final String WORKSPACE_RECLAIM_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "scope": "WORKSPACE",
                "projectId": null,
                "workspaceId": "3829492007654999",
                "runsStopped": false,
                "container": "CLEAN",
                "volume": "CLEAN",
                "registryRow": "CLEAN",
                "projectRow": "UNTOUCHED",
                "residueFree": true,
                "operatorId": null,
                "operatorName": null,
                "reclaimedAt": "2026-11-13T09:35:00"
              },
              "requestId": "req-rclmw",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/projects/{id}/reclaim 的「残留如实红」成功形状：
     * 销毁尽力而未全成——卷面 RESIDUE（归收编通道）、项目行 RESIDUE（受理即删未成）、
     * 容器面 UNKNOWN（探查未成）——residueFree false（UNKNOWN 不算清、RESIDUE 如实红），
     * 回执为 200 非错误（不得当成功静默、也不报错重试——残留归扫描轮/手动清扫收编）。
     */
    private static final String PROJECT_RECLAIM_RESIDUE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "scope": "PROJECT",
                "projectId": "3829492007654002",
                "workspaceId": "3829492007654330",
                "runsStopped": false,
                "container": "UNKNOWN",
                "volume": "RESIDUE",
                "registryRow": "CLEAN",
                "projectRow": "RESIDUE",
                "residueFree": false,
                "operatorId": "700102",
                "operatorName": "值班·小李",
                "reclaimedAt": "2026-11-13T09:40:00"
              },
              "requestId": "req-rclmr",
              "errors": null
            }
            """;

    @Test
    void given_projectReclaim_when_post_then_urlNoBodyAndFourFacesMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformProjectAppService appService =
                AiplatformWireTestSupport.reclaimProjectAppServiceWithStubTransport(
                        PROJECT_RECLAIM_ENVELOPE, urlSink, bodySink);

        AiplatformReclaimResponse reclaim = appService.reclaim("3829492007654001");

        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/3829492007654001/reclaim");
        // 钉死无请求体契约（null body 发空 body——同沙箱动作/手动清扫先例）
        assertThat(bodySink[0]).isNull();

        // 项目级同形回执：scope＋双 id＋在飞 run 级联停止＋四面全 CLEAN＋零残留判据
        assertThat(reclaim.scope()).isEqualTo("PROJECT");
        assertThat(reclaim.projectId()).isEqualTo("3829492007654001");
        assertThat(reclaim.workspaceId()).isEqualTo("3829492007654321");
        assertThat(reclaim.runsStopped()).isTrue();
        assertThat(reclaim.container()).isEqualTo("CLEAN");
        assertThat(reclaim.volume()).isEqualTo("CLEAN");
        assertThat(reclaim.registryRow()).isEqualTo("CLEAN");
        assertThat(reclaim.projectRow()).isEqualTo("CLEAN");
        assertThat(reclaim.residueFree()).isTrue();
        // 操作者两列回显落痕值
        assertThat(reclaim.operatorId()).isEqualTo("700101");
        assertThat(reclaim.operatorName()).isEqualTo("运维·老张");
        assertThat(reclaim.reclaimedAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 9, 30, 0));
    }

    @Test
    void given_lostWorkspaceReclaim_when_post_then_untouchedProjectFaceAndNullProjectIdMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformWorkspaceAppService appService =
                AiplatformWireTestSupport.reclaimWorkspaceAppServiceWithStubTransport(
                        WORKSPACE_RECLAIM_ENVELOPE, urlSink, bodySink);

        AiplatformReclaimResponse reclaim = appService.reclaim("3829492007654999");

        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/workspaces/3829492007654999/reclaim");
        assertThat(bodySink[0]).isNull();

        // 失联裁决面：无所属 projectId null（项目行已删）＋项目面 UNTOUCHED 如实分示
        //（单台三清不动项目行——非清非残留）＋无所属无停止面可挂 runsStopped false
        assertThat(reclaim.scope()).isEqualTo("WORKSPACE");
        assertThat(reclaim.projectId()).isNull();
        assertThat(reclaim.workspaceId()).isEqualTo("3829492007654999");
        assertThat(reclaim.runsStopped()).isFalse();
        assertThat(reclaim.container()).isEqualTo("CLEAN");
        assertThat(reclaim.volume()).isEqualTo("CLEAN");
        assertThat(reclaim.registryRow()).isEqualTo("CLEAN");
        assertThat(reclaim.projectRow()).isEqualTo("UNTOUCHED");
        assertThat(reclaim.residueFree()).isTrue();
        // 缺透传头操作者落空 NULL（照沙箱动作面口径——provider 不落强制守卫）
        assertThat(reclaim.operatorId()).isNull();
        assertThat(reclaim.operatorName()).isNull();
        assertThat(reclaim.reclaimedAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 9, 35, 0));
    }

    @Test
    void given_residueReport_when_reclaim_then_residueReportedAsIsAndResidueFreeFalse() {
        String[] urlSink = new String[1];
        AiplatformProjectAppService appService =
                AiplatformWireTestSupport.reclaimProjectAppServiceWithStubTransport(
                        PROJECT_RECLAIM_RESIDUE_ENVELOPE, urlSink, new String[1]);

        AiplatformReclaimResponse reclaim = appService.reclaim("3829492007654002");

        // RESIDUE 如实红不报错重试（回执 200 非错误）＋UNKNOWN 不算清——residueFree false，
        // 残留归扫描轮/手动清扫收编，前端呈现「残留待收编」提示
        assertThat(reclaim.container()).isEqualTo("UNKNOWN");
        assertThat(reclaim.volume()).isEqualTo("RESIDUE");
        assertThat(reclaim.registryRow()).isEqualTo("CLEAN");
        assertThat(reclaim.projectRow()).isEqualTo("RESIDUE");
        assertThat(reclaim.residueFree()).isFalse();
        assertThat(reclaim.runsStopped()).isFalse();
        assertThat(reclaim.operatorId()).isEqualTo("700102");
        assertThat(reclaim.operatorName()).isEqualTo("值班·小李");
    }

    @Test
    void given_prj001FromDownstream_when_reclaimProject_then_errorEnvelopePassedThrough() {
        String[] urlSink = new String[1];
        AiplatformProjectAppService appService =
                AiplatformWireTestSupport.reclaimProjectAppServiceWithErrorTransport(
                        404,
                        "{\"code\":4001,\"message\":\"项目不存在\","
                                + "\"data\":null}",
                        urlSink);

        // 错误信封原样透传（数字业务码＝PRJ 域码 4×1000＋1），不映射 BaseCodeMessage
        assertThatThrownBy(() -> appService.reclaim("3829492007654099"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(4001);
                    assertThat(upstream.getMessage()).contains("项目不存在");
                });
        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/projects/3829492007654099/reclaim");
    }

    @Test
    void given_wsp001FromDownstream_when_reclaimWorkspace_then_errorEnvelopePassedThrough() {
        String[] urlSink = new String[1];
        AiplatformWorkspaceAppService appService =
                AiplatformWireTestSupport.reclaimWorkspaceAppServiceWithErrorTransport(
                        404,
                        "{\"code\":1001,\"message\":\"工作区不存在\","
                                + "\"data\":null}",
                        urlSink);

        // 工作区不存在（含畸形 id）404 WSP_001→1001 原样透传
        assertThatThrownBy(() -> appService.reclaim("not-a-tsid"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(1001);
                    assertThat(upstream.getMessage()).contains("工作区不存在");
                });
        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/workspaces/not-a-tsid/reclaim");
    }
}
