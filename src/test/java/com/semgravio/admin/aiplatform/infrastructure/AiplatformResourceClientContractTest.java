package com.semgravio.admin.aiplatform.infrastructure;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformResourceAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceOverviewResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceSweepResponse;

/**
 * aiplatform 后台「资源面」（资源大盘 overview＋手动清扫 sweep）的 wire 契约测试
 * （对接 aiplatform #489，issue #128）。
 *
 * <p>契约事实（2026-11-13 对照 aiplatform 源码 {@code BackofficeResourceController}
 * /{@code WorkspaceResourceAppService} /{@code BackofficeResourceOverviewResponse}
 * /{@code BackofficeResourceSweepResponse} /V64 迁移 {@code wsp_host_observation}＋
 * {@code wsp_sweep_actions} 核实）：</p>
 * <ul>
 *   <li>两端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}；overview
 *       data 为单对象（host/docker/orphans/lostWorkspaceCount/sampledAt/lastSweep 六面），
 *       sweep data 同为单对象（操作者两列/registryReachable/graceLine/四计数/items 逐项）。</li>
 *   <li>overview 纯读库零 docker 调用秒级返回；<strong>null＝缺测、0＝真零</strong>：从未采样
 *       （首个扫描窗内）host/docker/orphans 整面 null、sampledAt null——如实绑定不造缺省；
 *       docker 不可达轮 docker 面各列 null（夹具覆盖代表面）。lostWorkspaceCount（primitive
 *       long）与体量/计数列（装箱 Long）同出 JSON string——cartisan-web 全局
 *       Long→ToStringSerializer 覆盖包装与 primitive 同源（出口口径同订单金额/分页 total）。</li>
 *   <li>sweep <strong>无请求体</strong>（{@code post(url, null, …)} 发空 body——同沙箱动作/
 *       重试归档先例）；回执 items 逐项处置：kind＝CONTAINER/VOLUME、disposition＝
 *       SWEPT/SKIPPED/FAILED、skipReason＝REGISTERED/GRACE_WINDOW/TIMESTAMP_UNREADABLE
 *       （SWEPT/FAILED 为 null）、resourceCreatedAt＝docker 回执时刻（在册项未起 inspect 恒
 *       null）；registryReachable=false＝库不可达一刀未动如实呈报（正常回执 200 非错误）。</li>
 *   <li>操作者两列（operatorId/operatorName）＝provider 落痕回显（X-User-Id/X-User-Name
 *       透传头缺头落空 NULL——照沙箱动作面口径，provider 不落强制守卫）。</li>
 *   <li>错误透传（数字业务码＝WSP 域码 1×1000＋序号）：WSP_002→1002（500，docker CLI
 *       自身失败——清扫未执行完不定格不留痕）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire
 * 反序列化——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient}
 * 仅替换 HTTP 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformResourceClientContractTest {

    /**
     * aiplatform GET /api/backoffice/resources/overview 的真实成功响应形状（信封＋单对象）：
     * 采样帧全量面——主机盘（path＋total/usable/used 三字节——装箱 Long 出 JSON string）、
     * docker 四类用量（image/container/volume/buildCache 计数与体量）、无主计数、失联计数
     * （primitive long 同出 JSON string——全局 ToStringSerializer 覆盖包装与 primitive
     * 同源）、sampledAt 新鲜度、最近手动清扫留痕直读（操作者两列＋收编计数）。
     * totalBytes−usableBytes＝usedBytes 派生口径在场。
     */
    private static final String OVERVIEW_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "host": {
                  "path": "/var/lib/docker",
                  "totalBytes": "494384795648",
                  "usableBytes": "301989888000",
                  "usedBytes": "192394907648"
                },
                "docker": {
                  "imageCount": "12",
                  "imageSizeBytes": "4442639553",
                  "containerCount": "8",
                  "containerSizeBytes": "2147483648",
                  "volumeCount": "9",
                  "volumeSizeBytes": "12884901888",
                  "buildCacheCount": "20",
                  "buildCacheSizeBytes": "3221225472"
                },
                "orphans": {
                  "containerCount": "1",
                  "volumeCount": "1"
                },
                "lostWorkspaceCount": "2",
                "sampledAt": "2026-11-13T10:30:00",
                "lastSweep": {
                  "sweptAt": "2026-11-13T09:00:00",
                  "operatorId": "700101",
                  "operatorName": "运维·老张",
                  "registryReachable": true,
                  "sweptContainers": 1,
                  "sweptVolumes": 1,
                  "skippedCount": 3,
                  "failedCount": 0
                }
              },
              "requestId": "req-r3s0u",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/resources/overview 的「从未采样」成功形状：首个扫描窗内
     * 各面如实 null（「没有帧」≠「零」——呈现「采样中」）；lostWorkspaceCount 恒可算（0 ＝真零）。
     */
    private static final String OVERVIEW_NEVER_SAMPLED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "host": null,
                "docker": null,
                "orphans": null,
                "lostWorkspaceCount": "0",
                "sampledAt": null,
                "lastSweep": null
              },
              "requestId": "req-r3s0n",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/resources/sweep 的真实成功响应形状（逐项处置报告）：
     * 三行代表面——收编容器（SWEPT，skipReason null）、在册跳过容器（REGISTERED——在册一律
     * 不动含休眠保卷，resourceCreatedAt 恒 null）、宽限窗内跳过卷（GRACE_WINDOW）；计数面
     * 一源＝域报告（sweptContainers 1/sweptVolumes 0/skippedCount 2/failedCount 0）；
     * graceLine＝触发时刻减 10 分钟宽限线；操作者两列回显落痕值。
     */
    private static final String SWEEP_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "sweptAt": "2026-11-13T09:05:00",
                "operatorId": "700101",
                "operatorName": "运维·老张",
                "registryReachable": true,
                "graceLine": "2026-11-13T08:55:00",
                "sweptContainers": 1,
                "sweptVolumes": 0,
                "skippedCount": 2,
                "failedCount": 0,
                "items": [
                  {
                    "kind": "CONTAINER",
                    "identifier": "ws-3829492007654321",
                    "disposition": "SWEPT",
                    "skipReason": null,
                    "resourceCreatedAt": "2026-11-13T08:40:00"
                  },
                  {
                    "kind": "CONTAINER",
                    "identifier": "ws-730101",
                    "disposition": "SKIPPED",
                    "skipReason": "REGISTERED",
                    "resourceCreatedAt": null
                  },
                  {
                    "kind": "VOLUME",
                    "identifier": "vol-ws-3829492007654999",
                    "disposition": "SKIPPED",
                    "skipReason": "GRACE_WINDOW",
                    "resourceCreatedAt": "2026-11-13T09:01:00"
                  }
                ]
              },
              "requestId": "req-r3swp",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/resources/sweep 的「库不可达」成功形状：registryReachable
     * false＝一刀未动如实呈报（准绳无从构成绝不按「全部无主」动手）——正常回执 200 非错误，
     * 跳过原因不在 items（items 如实为空表），操作者两列仍落痕留档。
     */
    private static final String SWEEP_REGISTRY_UNREACHABLE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "sweptAt": "2026-11-13T09:06:00",
                "operatorId": null,
                "operatorName": null,
                "registryReachable": false,
                "graceLine": "2026-11-13T08:56:00",
                "sweptContainers": 0,
                "sweptVolumes": 0,
                "skippedCount": 0,
                "failedCount": 0,
                "items": []
              },
              "requestId": "req-r3swu",
              "errors": null
            }
            """;

    @Test
    void given_sampledFrame_when_overview_then_urlEnvelopeAndAllFacesMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformResourceAppService appService =
                AiplatformWireTestSupport.resourceAppServiceWithStubTransport(
                        OVERVIEW_ENVELOPE, urlSink, bodySink);

        AiplatformResourceOverviewResponse overview = appService.overview();

        assertThat(urlSink[0]).isEqualTo("http://stub-aiplatform/api/backoffice/resources/overview");
        // 大盘读口零请求体触达
        assertThat(bodySink[0]).isNull();

        // 主机盘面：锚路径＋三字节（装箱 Long 出 JSON string 如实绑定）＋派生口径在场
        assertThat(overview.host().path()).isEqualTo("/var/lib/docker");
        assertThat(overview.host().totalBytes()).isEqualTo(494384795648L);
        assertThat(overview.host().usableBytes()).isEqualTo(301989888000L);
        assertThat(overview.host().usedBytes()).isEqualTo(192394907648L);
        // total−usable＝used 派生口径（provider 侧派生，BFF 照实绑定不复算）
        assertThat(overview.host().totalBytes() - overview.host().usableBytes())
                .isEqualTo(overview.host().usedBytes());

        // docker 四类用量全八列（计数与体量——体量 docker 显示同基十进制近似值）
        assertThat(overview.docker().imageCount()).isEqualTo(12L);
        assertThat(overview.docker().imageSizeBytes()).isEqualTo(4442639553L);
        assertThat(overview.docker().containerCount()).isEqualTo(8L);
        assertThat(overview.docker().containerSizeBytes()).isEqualTo(2147483648L);
        assertThat(overview.docker().volumeCount()).isEqualTo(9L);
        assertThat(overview.docker().volumeSizeBytes()).isEqualTo(12884901888L);
        assertThat(overview.docker().buildCacheCount()).isEqualTo(20L);
        assertThat(overview.docker().buildCacheSizeBytes()).isEqualTo(3221225472L);

        // 无主计数（ADR-0045 准绳——「可回收」口径不进本面）＋失联计数（primitive long 数字）
        assertThat(overview.orphans().containerCount()).isEqualTo(1L);
        assertThat(overview.orphans().volumeCount()).isEqualTo(1L);
        assertThat(overview.lostWorkspaceCount()).isEqualTo(2L);

        // 新鲜度标注＋最近手动清扫留痕直读（收编结果面）
        assertThat(overview.sampledAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 10, 30, 0));
        assertThat(overview.lastSweep().sweptAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 9, 0, 0));
        assertThat(overview.lastSweep().operatorId()).isEqualTo("700101");
        assertThat(overview.lastSweep().operatorName()).isEqualTo("运维·老张");
        assertThat(overview.lastSweep().registryReachable()).isTrue();
        assertThat(overview.lastSweep().sweptContainers()).isEqualTo(1);
        assertThat(overview.lastSweep().sweptVolumes()).isEqualTo(1);
        assertThat(overview.lastSweep().skippedCount()).isEqualTo(3);
        assertThat(overview.lastSweep().failedCount()).isEqualTo(0);
    }

    @Test
    void given_neverSampled_when_overview_then_facesAbsentAsIs() {
        String[] urlSink = new String[1];
        AiplatformResourceAppService appService =
                AiplatformWireTestSupport.resourceAppServiceWithStubTransport(
                        OVERVIEW_NEVER_SAMPLED_ENVELOPE, urlSink, new String[1]);

        AiplatformResourceOverviewResponse overview = appService.overview();

        // 未采样的首个扫描窗内：各面如实 null（「没有帧」≠「零」——前端呈现「采样中」而非零）
        assertThat(overview.host()).isNull();
        assertThat(overview.docker()).isNull();
        assertThat(overview.orphans()).isNull();
        assertThat(overview.sampledAt()).isNull();
        assertThat(overview.lastSweep()).isNull();
        // lostWorkspaceCount 恒可算（0＝真零——失联判定读注册表不依赖采样帧）
        assertThat(overview.lostWorkspaceCount()).isZero();
    }

    @Test
    void given_orphanReport_when_sweep_then_postNoBodyAndItemizedDispositionMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformResourceAppService appService =
                AiplatformWireTestSupport.resourceAppServiceWithStubTransport(
                        SWEEP_ENVELOPE, urlSink, bodySink);

        AiplatformResourceSweepResponse sweep = appService.sweep();

        assertThat(urlSink[0]).isEqualTo("http://stub-aiplatform/api/backoffice/resources/sweep");
        // 钉死无请求体契约（null body 发空 body——同沙箱动作/重试归档先例）
        assertThat(bodySink[0]).isNull();

        assertThat(sweep.sweptAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 9, 5, 0));
        assertThat(sweep.operatorId()).isEqualTo("700101");
        assertThat(sweep.operatorName()).isEqualTo("运维·老张");
        assertThat(sweep.registryReachable()).isTrue();
        assertThat(sweep.graceLine()).isEqualTo(LocalDateTime.of(2026, 11, 13, 8, 55, 0));
        // 计数面一源＝域报告计数（如实绑定）
        assertThat(sweep.sweptContainers()).isEqualTo(1);
        assertThat(sweep.sweptVolumes()).isZero();
        assertThat(sweep.skippedCount()).isEqualTo(2);
        assertThat(sweep.failedCount()).isZero();

        // 逐项处置三行代表面：SWEPT（skipReason null＋docker 时刻在场）/SKIPPED REGISTERED
        //（在册一律不动——resourceCreatedAt 恒 null 未起 inspect）/SKIPPED GRACE_WINDOW
        assertThat(sweep.items()).hasSize(3);
        AiplatformResourceSweepResponse.SweepItem swept = sweep.items().get(0);
        assertThat(swept.kind()).isEqualTo("CONTAINER");
        assertThat(swept.identifier()).isEqualTo("ws-3829492007654321");
        assertThat(swept.disposition()).isEqualTo("SWEPT");
        assertThat(swept.skipReason()).isNull();
        assertThat(swept.resourceCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 8, 40, 0));

        AiplatformResourceSweepResponse.SweepItem registered = sweep.items().get(1);
        assertThat(registered.kind()).isEqualTo("CONTAINER");
        assertThat(registered.identifier()).isEqualTo("ws-730101");
        assertThat(registered.disposition()).isEqualTo("SKIPPED");
        assertThat(registered.skipReason()).isEqualTo("REGISTERED");
        assertThat(registered.resourceCreatedAt()).isNull();

        AiplatformResourceSweepResponse.SweepItem grace = sweep.items().get(2);
        assertThat(grace.kind()).isEqualTo("VOLUME");
        assertThat(grace.identifier()).isEqualTo("vol-ws-3829492007654999");
        assertThat(grace.disposition()).isEqualTo("SKIPPED");
        assertThat(grace.skipReason()).isEqualTo("GRACE_WINDOW");
        assertThat(grace.resourceCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 13, 9, 1, 0));
    }

    @Test
    void given_registryUnreachable_when_sweep_then_oneKnifeUntouchedReportedAsIs() {
        String[] urlSink = new String[1];
        AiplatformResourceAppService appService =
                AiplatformWireTestSupport.resourceAppServiceWithStubTransport(
                        SWEEP_REGISTRY_UNREACHABLE_ENVELOPE, urlSink, new String[1]);

        AiplatformResourceSweepResponse sweep = appService.sweep();

        // 库不可达一刀未动如实呈报：正常 200 回执（非错误），registryReachable false＋
        // 计数全零＋items 空表——准绳无从构成绝不按「全部无主」动手
        assertThat(sweep.registryReachable()).isFalse();
        assertThat(sweep.sweptContainers()).isZero();
        assertThat(sweep.sweptVolumes()).isZero();
        assertThat(sweep.skippedCount()).isZero();
        assertThat(sweep.failedCount()).isZero();
        assertThat(sweep.items()).isEmpty();
        // 缺透传头操作者落空 NULL（照沙箱动作面口径——provider 不落强制守卫）
        assertThat(sweep.operatorId()).isNull();
        assertThat(sweep.operatorName()).isNull();
    }

    @Test
    void given_wsp002FromDownstream_when_sweep_then_errorEnvelopePassedThrough() {
        String[] urlSink = new String[1];
        AiplatformResourceAppService appService =
                AiplatformWireTestSupport.resourceAppServiceWithErrorTransport(
                        500,
                        "{\"code\":1002,\"message\":\"docker CLI 调用失败（docker system df 非零退出）\","
                                + "\"data\":null}",
                        urlSink);

        // 错误信封原样透传（数字业务码＝WSP 域码 1×1000＋2），不映射 BaseCodeMessage
        assertThatThrownBy(appService::sweep)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(500);
                    assertThat(upstream.envelopeCode()).isEqualTo(1002);
                    assertThat(upstream.getMessage())
                            .contains("docker CLI 调用失败（docker system df 非零退出）");
                });
        assertThat(urlSink[0]).isEqualTo("http://stub-aiplatform/api/backoffice/resources/sweep");
    }
}
