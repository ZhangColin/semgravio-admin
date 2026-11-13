package com.semgravio.admin.aiplatform.infrastructure;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.semgravio.admin.aiplatform.application.AiplatformTimeBandRuleAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleRewindowResponse;
import com.semgravio.admin.aiplatform.application.AiplatformTimeBandRuleAppService;
import com.semgravio.admin.aiplatform.application.AiplatformUpstreamException;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleOpenCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleRewindowCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeWindowCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformTimeBandRuleQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleRewindowResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandWindowWireRequest;

/**
 * aiplatform 后台「时段规则」（time-band-rules 清单/开行/原子改窗/停用四端点，
 * 峰谷运营位）的 wire 契约测试（对接 aiplatform #497／ADR-0044 峰谷 1/4，issue #130）。
 *
 * <p>契约事实（2026-11-13 对照 aiplatform 源码 {@code BackofficeTimeBandRuleController}
 * /{@code BackofficeTimeBandRuleAppService} /{@code TimeBandRuleResponse} /
 * {@code TimeBandRuleRewindowResponse} /V66 迁移 {@code met_time_band_rules} 核实）：</p>
 * <ul>
 *   <li>清单＝{@code ApiResponse<PageResponse<TimeBandRuleResponse>>} 分页信封（page 1 基、
 *       size 缺省 20，provider 精确过滤可缺省）；开行/停用＝
 *       {@code ApiResponse<List<TimeBandRuleResponse>>}（一单 N 窗口行＝一个版本）；
 *       改窗＝{@code ApiResponse<TimeBandRuleRewindowResponse>} {@code {closed, opened}}
 *       双段回执。</li>
 *   <li>行形十二字段：id/provider/band(Integer code 1=峰 2=谷)/bandName/dayMask(星期口径
 *       bitmask Mon=1…Sun=64)/dayMaskName(连续段压缩如「周一至周五」)/startMinute/
 *       endMinute(UTC 分钟，end 不含)/effectiveFrom/effectiveTo(null＝当前行)/
 *       operatorId/operatorName（X-User-Id/X-User-Name 透传头落痕，缺头落空 NULL）。</li>
 *   <li>写口 URL：{@code POST /api/backoffice/time-band-rules}（开行，有请求体）、
 *       {@code POST /{id}/rewindow}（改窗，有请求体）、{@code POST /{id}/deactivate}
 *       （停用，<strong>无请求体</strong>——同沙箱动作/手动清扫先例）。</li>
 *   <li>窗不跨午夜（v1 不支持，start&lt;end 服务端拦 METER_013）；错误码 METER_013（窗口项
 *       非法）400／METER_014（关行时点）400／METER_015（行不存在）404／METER_016（非当前行）
 *       409／METER_008（同通道区间重叠）409——数字业务码＝域码 3×1000＋序号，原样透传。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire
 * 反序列化——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient}
 * 仅替换 HTTP 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformTimeBandRuleClientContractTest {

    /**
     * aiplatform GET /api/backoffice/time-band-rules 的真实成功响应形状（分页信封＋两行：
     * DeepSeek 峰窗现行两行＋一行历史谷段）：band/bandName、dayMask 31→「周一至周五」、
     * effectiveTo null＝当前行与历史行两端在场、操作者两列回显。
     */
    private static final String LIST_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "items": [
                  {
                    "id": "3829492007655001",
                    "provider": "deepseek",
                    "band": 1,
                    "bandName": "峰",
                    "dayMask": 31,
                    "dayMaskName": "周一至周五",
                    "startMinute": 60,
                    "endMinute": 240,
                    "effectiveFrom": "2026-08-16T00:00:00Z",
                    "effectiveTo": null,
                    "operatorId": "700101",
                    "operatorName": "运维·老张"
                  },
                  {
                    "id": "3829492007655002",
                    "provider": "deepseek",
                    "band": 1,
                    "bandName": "峰",
                    "dayMask": 31,
                    "dayMaskName": "周一至周五",
                    "startMinute": 360,
                    "endMinute": 600,
                    "effectiveFrom": "2026-08-16T00:00:00Z",
                    "effectiveTo": null,
                    "operatorId": "700101",
                    "operatorName": "运维·老张"
                  },
                  {
                    "id": "3829492007654990",
                    "provider": "deepseek",
                    "band": 2,
                    "bandName": "谷",
                    "dayMask": 31,
                    "dayMaskName": "周一至周五",
                    "startMinute": 1200,
                    "endMinute": 1320,
                    "effectiveFrom": "2026-05-01T00:00:00Z",
                    "effectiveTo": "2026-08-16T00:00:00Z",
                    "operatorId": null,
                    "operatorName": null
                  }
                ],
                "total": 3,
                "page": 1,
                "size": 20
              },
              "requestId": "req-tbrl",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/time-band-rules 的开行回执形状：一单 N 窗口行＝一个
     * 版本（两窗口共享生效起点，行元素与清单行共形）。
     */
    private static final String OPEN_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3829492007655011",
                  "provider": "deepseek",
                  "band": 1,
                  "bandName": "峰",
                  "dayMask": 31,
                  "dayMaskName": "周一至周五",
                  "startMinute": 60,
                  "endMinute": 240,
                  "effectiveFrom": "2026-11-14T00:00:00Z",
                  "effectiveTo": null,
                  "operatorId": "700103",
                  "operatorName": "计费·小王"
                },
                {
                  "id": "3829492007655012",
                  "provider": "deepseek",
                  "band": 1,
                  "bandName": "峰",
                  "dayMask": 31,
                  "dayMaskName": "周一至周五",
                  "startMinute": 360,
                  "endMinute": 600,
                  "effectiveFrom": "2026-11-14T00:00:00Z",
                  "effectiveTo": null,
                  "operatorId": "700103",
                  "operatorName": "计费·小王"
                }
              ],
              "requestId": "req-tbro",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/time-band-rules/{id}/rewindow 的原子改窗回执形状：
     * 双段——被关版本全部行（保留原操作者留痕）＋新版本全部行（操作者取本次请求）。
     */
    private static final String REWINDOW_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "closed": [
                  {
                    "id": "3829492007655001",
                    "provider": "deepseek",
                    "band": 1,
                    "bandName": "峰",
                    "dayMask": 31,
                    "dayMaskName": "周一至周五",
                    "startMinute": 60,
                    "endMinute": 240,
                    "effectiveFrom": "2026-08-16T00:00:00Z",
                    "effectiveTo": "2026-11-14T00:00:00Z",
                    "operatorId": "700101",
                    "operatorName": "运维·老张"
                  }
                ],
                "opened": [
                  {
                    "id": "3829492007655021",
                    "provider": "deepseek",
                    "band": 1,
                    "bandName": "峰",
                    "dayMask": 31,
                    "dayMaskName": "周一至周五",
                    "startMinute": 90,
                    "endMinute": 270,
                    "effectiveFrom": "2026-11-14T00:00:00Z",
                    "effectiveTo": null,
                    "operatorId": "700103",
                    "operatorName": "计费·小王"
                  }
                ]
              },
              "requestId": "req-tbrr",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/time-band-rules/{id}/deactivate 的停用回执形状：
     * 被关版本全部行（关版本不接新行，被关行是唯一落点）。
     */
    private static final String DEACTIVATE_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3829492007655011",
                  "provider": "deepseek",
                  "band": 1,
                  "bandName": "峰",
                  "dayMask": 31,
                  "dayMaskName": "周一至周五",
                  "startMinute": 60,
                  "endMinute": 240,
                  "effectiveFrom": "2026-11-14T00:00:00Z",
                  "effectiveTo": "2026-11-13T10:00:00Z",
                  "operatorId": "700103",
                  "operatorName": "计费·小王"
                },
                {
                  "id": "3829492007655012",
                  "provider": "deepseek",
                  "band": 1,
                  "bandName": "峰",
                  "dayMask": 31,
                  "dayMaskName": "周一至周五",
                  "startMinute": 360,
                  "endMinute": 600,
                  "effectiveFrom": "2026-11-14T00:00:00Z",
                  "effectiveTo": "2026-11-13T10:00:00Z",
                  "operatorId": "700103",
                  "operatorName": "计费·小王"
                }
              ],
              "requestId": "req-tbrd",
              "errors": null
            }
            """;

    @Test
    void given_timeBandRuleList_when_get_then_pageEnvelopeAndTwelveFieldsMirrored() {
        String[] urlSink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithStubTransport(
                        LIST_ENVELOPE, urlSink, new String[1]);

        var page = appService.list(new AiplatformTimeBandRuleQuery("deepseek"), 1, 20);

        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/time-band-rules?page=1&size=20&provider=deepseek");
        // 回显 provider 的 page/size 原值（1-based），BFF 不重复夹取
        assertThat(page.items()).hasSize(3);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);

        // 现行峰段行：band/bandName 对、dayMask 31→「周一至周五」、UTC 分钟（end 不含）、
        // effectiveTo null＝当前行、操作者两列回显
        AiplatformTimeBandRuleResponse peak = page.items().get(0);
        assertThat(peak.id()).isEqualTo("3829492007655001");
        assertThat(peak.provider()).isEqualTo("deepseek");
        assertThat(peak.band()).isEqualTo(1);
        assertThat(peak.bandName()).isEqualTo("峰");
        assertThat(peak.dayMask()).isEqualTo(31);
        assertThat(peak.dayMaskName()).isEqualTo("周一至周五");
        assertThat(peak.startMinute()).isEqualTo(60);
        assertThat(peak.endMinute()).isEqualTo(240);
        assertThat(peak.effectiveFrom()).isEqualTo(Instant.parse("2026-08-16T00:00:00Z"));
        assertThat(peak.effectiveTo()).isNull();
        assertThat(peak.operatorId()).isEqualTo("700101");
        assertThat(peak.operatorName()).isEqualTo("运维·老张");

        // 历史谷段行：区间两端在场＋种子/无头操作者落空 NULL
        AiplatformTimeBandRuleResponse valley = page.items().get(2);
        assertThat(valley.band()).isEqualTo(2);
        assertThat(valley.bandName()).isEqualTo("谷");
        assertThat(valley.effectiveTo()).isEqualTo(Instant.parse("2026-08-16T00:00:00Z"));
        assertThat(valley.operatorId()).isNull();
        assertThat(valley.operatorName()).isNull();
    }

    @Test
    void given_timeBandRuleOpen_when_post_thenSingleVersionRowsMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithStubTransport(
                        OPEN_ENVELOPE, urlSink, bodySink);

        List<AiplatformTimeBandRuleResponse> opened = appService.open(
                new AiplatformTimeBandRuleOpenCommand(
                        "deepseek",
                        List.of(new AiplatformTimeWindowCommand(
                                        1, 31, 60, 240),
                                new AiplatformTimeWindowCommand(
                                        1, 31, 360, 600)),
                        Instant.parse("2026-11-14T00:00:00Z")));

        assertThat(urlSink[0]).isEqualTo("http://stub-aiplatform/api/backoffice/time-band-rules");
        // 命令体逐字镜像出站：band Integer code＋dayMask bitmask＋UTC 分钟＋共享生效起点
        assertThat(bodySink[0]).isEqualTo(
                "{\"provider\":\"deepseek\","
                        + "\"windows\":["
                        + "{\"band\":1,\"dayMask\":31,\"startMinute\":60,\"endMinute\":240},"
                        + "{\"band\":1,\"dayMask\":31,\"startMinute\":360,\"endMinute\":600}],"
                        + "\"effectiveFrom\":\"2026-11-14T00:00:00Z\"}");
        // 一单 N 窗口行＝一个版本：两行共享生效起点、操作者取本次请求
        assertThat(opened).hasSize(2);
        assertThat(opened.get(0).id()).isEqualTo("3829492007655011");
        assertThat(opened.get(0).effectiveFrom()).isEqualTo(Instant.parse("2026-11-14T00:00:00Z"));
        assertThat(opened.get(0).effectiveTo()).isNull();
        assertThat(opened.get(1).startMinute()).isEqualTo(360);
        assertThat(opened.get(1).operatorId()).isEqualTo("700103");
        assertThat(opened.get(1).operatorName()).isEqualTo("计费·小王");
    }

    @Test
    void given_timeBandRuleRewindow_when_post_thenClosedOpenedTwoSegmentReceiptMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithStubTransport(
                        REWINDOW_ENVELOPE, urlSink, bodySink);

        AiplatformTimeBandRuleRewindowResponse receipt = appService.rewindow("3829492007655001",
                new AiplatformTimeBandRuleRewindowCommand(
                        List.of(new AiplatformTimeWindowCommand(
                                1, 31, 90, 270)),
                        Instant.parse("2026-11-14T00:00:00Z")));

        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/time-band-rules/3829492007655001/rewindow");
        assertThat(bodySink[0]).isEqualTo(
                "{\"windows\":[{\"band\":1,\"dayMask\":31,\"startMinute\":90,\"endMinute\":270}],"
                        + "\"effectiveFrom\":\"2026-11-14T00:00:00Z\"}");

        // 双段回执：被关行保留原操作者留痕（不被改写）、区间两端落定；新行操作者取本次请求
        assertThat(receipt.closed()).hasSize(1);
        assertThat(receipt.closed().get(0).effectiveTo())
                .isEqualTo(Instant.parse("2026-11-14T00:00:00Z"));
        assertThat(receipt.closed().get(0).operatorId()).isEqualTo("700101");
        assertThat(receipt.closed().get(0).operatorName()).isEqualTo("运维·老张");
        assertThat(receipt.opened()).hasSize(1);
        assertThat(receipt.opened().get(0).startMinute()).isEqualTo(90);
        assertThat(receipt.opened().get(0).endMinute()).isEqualTo(270);
        assertThat(receipt.opened().get(0).operatorId()).isEqualTo("700103");
    }

    @Test
    void given_timeBandRuleDeactivate_when_post_thenNoBodyAndClosedRowsMirrored() {
        String[] urlSink = new String[1];
        String[] bodySink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithStubTransport(
                        DEACTIVATE_ENVELOPE, urlSink, bodySink);

        List<AiplatformTimeBandRuleResponse> closed = appService.deactivate("3829492007655011");

        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/time-band-rules/3829492007655011/deactivate");
        // 钉死无请求体契约（null body 发空 body——同沙箱动作/手动清扫先例）
        assertThat(bodySink[0]).isNull();

        // 关版本不接新行：被关版本全部行回执、effectiveTo＝停用时刻、操作者落被关各行
        assertThat(closed).hasSize(2);
        assertThat(closed.get(0).effectiveTo()).isEqualTo(Instant.parse("2026-11-13T10:00:00Z"));
        assertThat(closed.get(1).id()).isEqualTo("3829492007655012");
        assertThat(closed.get(1).operatorName()).isEqualTo("计费·小王");
    }

    @Test
    void given_meter016FromDownstream_when_rewindow_then_errorEnvelopePassedThrough() {
        String[] urlSink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithErrorTransport(
                        409,
                        "{\"code\":3016,\"message\":\"目标行非当前版本成员\","
                                + "\"data\":null}",
                        urlSink);

        // 错误信封原样透传（数字业务码＝METER 域码 3×1000＋16），不映射 BaseCodeMessage
        assertThatThrownBy(() -> appService.rewindow("3829492007654990",
                new AiplatformTimeBandRuleRewindowCommand(
                        List.of(new AiplatformTimeWindowCommand(
                                1, 31, 90, 270)),
                        null)))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(409);
                    assertThat(upstream.envelopeCode()).isEqualTo(3016);
                    assertThat(upstream.getMessage()).contains("目标行非当前版本成员");
                });
        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/time-band-rules/3829492007654990/rewindow");
    }

    @Test
    void given_meter015FromDownstream_when_deactivate_then_errorEnvelopePassedThrough() {
        String[] urlSink = new String[1];
        AiplatformTimeBandRuleAppService appService =
                AiplatformWireTestSupport.timeBandRuleAppServiceWithErrorTransport(
                        404,
                        "{\"code\":3015,\"message\":\"时段规则行不存在\","
                                + "\"data\":null}",
                        urlSink);

        // 行不存在（含畸形 id——provider 侧 lenient 解析）404 METER_015→3015 原样透传
        assertThatThrownBy(() -> appService.deactivate("not-a-tsid"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(3015);
                    assertThat(upstream.getMessage()).contains("时段规则行不存在");
                });
        assertThat(urlSink[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/time-band-rules/not-a-tsid/deactivate");
    }
}
