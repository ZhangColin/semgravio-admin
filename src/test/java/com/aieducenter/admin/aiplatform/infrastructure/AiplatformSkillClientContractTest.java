package com.aieducenter.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aieducenter.admin.aiplatform.application.AiplatformUpstreamException;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;

/**
 * aiplatform 技能管理域（清单/详情）的 wire 契约测试（对接 aiplatform #247 技能线 T1，
 * issue #80）。
 *
 * <p>契约事实（2026-09-23 对照 aiplatform 源码 {@code BackofficeSkillController} /
 * {@code BackofficeSkillAppService} / {@code BackofficeSkillSummaryResponse} /
 * {@code BackofficeSkillDetailResponse} / {@code SkillSource} / {@code SkillStatus} /
 * {@code SkillMessage} / {@code ErrorCodePrefix} 核实）：</p>
 * <ul>
 *   <li>两端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}；清单 data 为
 *       <strong>裸 List</strong>（有界目录不分页不过滤——对话史 List 直出同形；排序服务端定死：
 *       内置在前名称序、安装在后按来源包＋名称序），详情 data 为单对象。</li>
 *   <li>行字段：{@code id} String——<strong>opaque 串两形制</strong>（内置
 *       {@code builtin:<技能名>}／安装 TSID 十进制串），admin 字符串透传不解析不预校验；
 *       {@code source} Integer code（1=内置 2=安装）+ {@code sourceName} 中文名、
 *       {@code status} Integer code（1=启用 2=停用）+ {@code statusName} 中文名随行；
 *       {@code sourcePackage}/{@code version} 仅安装行有值（内置 null）；{@code updateAvailable}
 *       来源包级新版标记（null＝未检查过，内置恒 null）。</li>
 *   <li>详情多 {@code frontmatter}（自由键值对象逐字透传——不做形状断言）＋{@code content}
 *       （frontmatter 剥离后正文全文，审核承载面）两字段，位于 statusName 与 operator 两肢之间
 *       （provider 字段序逐字镜像）。</li>
 *   <li>错误透传（数字业务码＝SKL 域码 7×1000＋序号）：SKL_001→7001（404，技能不存在——含
 *       未寻址内置名/TSID、畸形柄，provider 侧两形制柄单点分解裁决，admin 不预检）。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformSkillClientContractTest {

    /** 内置行（provider classpath 合成）：builtin: 柄、来源 1、来源包/版本/操作者/新版标记全 null。 */
    private static final String BUILTIN_ROW = """
            {
              "id": "builtin:prd-writing",
              "name": "prd-writing",
              "description": "撰写或修订 PRD 时使用",
              "source": 1,
              "sourceName": "内置",
              "sourcePackage": null,
              "version": null,
              "status": 1,
              "statusName": "启用",
              "operatorId": null,
              "operatorName": null,
              "updateAvailable": null
            }
            """;

    /** 安装行（库行）：TSID 十进制串柄、来源 2、来源包/装时 commit/治理留痕/新版标记俱全。 */
    private static final String INSTALLED_ROW = """
            {
              "id": "3852300001111111",
              "name": "web-research",
              "description": "联网检索与网页抓取整理",
              "source": 2,
              "sourceName": "安装",
              "sourcePackage": "https://github.com/acme/agent-skills",
              "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
              "status": 2,
              "statusName": "停用",
              "operatorId": "700200",
              "operatorName": "运营·技能管理员",
              "updateAvailable": true
            }
            """;

    /**
     * aiplatform GET /api/backoffice/skills 的真实成功响应形状（信封 + 裸 List）：
     * 排序服务端定死的代表面——内置在前（名称序）、安装在后。
     */
    private static final String SKILL_LIST_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                %s,
                %s
              ],
              "requestId": "req-s1k2l",
              "errors": null
            }
            """.formatted(BUILTIN_ROW, INSTALLED_ROW);

    /**
     * aiplatform GET /api/backoffice/skills/{id} 的真实成功响应形状：安装行详情——frontmatter
     * 自由键值（含 name/description 之外的键，逐字透传）＋正文全文（多段形态代表面）。
     */
    private static final String SKILL_DETAIL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3852300001111111",
                "name": "web-research",
                "description": "联网检索与网页抓取整理",
                "source": 2,
                "sourceName": "安装",
                "sourcePackage": "https://github.com/acme/agent-skills",
                "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "status": 1,
                "statusName": "启用",
                "frontmatter": {
                  "name": "web-research",
                  "description": "联网检索与网页抓取整理",
                  "license": "MIT",
                  "requires": ["network"]
                },
                "content": "# Web Research\\n\\n先检索后作答……\\n\\n## 步骤\\n\\n1. 拆解问题\\n2. 检索来源",
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "updateAvailable": false
              },
              "requestId": "req-s3d4e",
              "errors": null
            }
            """;

    /** 技能不存在（含未寻址内置名/TSID、畸形柄）：HTTP 404 + 信封 code=7001（SKL_001，域码 7）。 */
    private static final String SKL_001_ERROR_ENVELOPE = """
            {
              "code": 7001,
              "message": "技能不存在",
              "data": null,
              "requestId": "req-s5n0f",
              "errors": null
            }
            """;

    private static final String INSTALLED_SKILL_ID = "3852300001111111";

    /** 内置柄（opaque 两形制之一）：出站 URL 编码后 ':' → %3A（provider 侧 @PathVariable 解码还原）。 */
    private static final String BUILTIN_SKILL_HANDLE = "builtin:prd-writing";

    // ========== 清单（零查询参数 + 裸 List 信封 + 两形制柄）==========

    @Test
    void given_skillCatalog_when_list_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_LIST_ENVELOPE, wireUrl);

        appService.list();

        // 出站 wire 形状逐字镜像 provider 契约：零查询参数——不分页（有界目录）、不过滤、
        // 排序服务端定死（spec #79：同形不造分页）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/skills");
    }

    @Test
    void given_skillListEnvelope_when_list_then_envelopeUnwrappedAndBothHandleFormsBound() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_LIST_ENVELOPE, new String[1]);

        List<AiplatformSkillSummaryResponse> skills = appService.list();

        // 信封正确拆开——证明 SKILL_LIST_TYPEREF 按 ApiResponse<List<…>> 裸 List 反序列化
        // （误用分页载体 typeref 会把 List 绑不进来）
        assertThat(skills).hasSize(2);

        // 首行：内置行——builtin: 柄原样到达（opaque 串，admin 不解析）、来源 1/内置、
        // 状态恒启用、来源包/版本/操作者/新版标记全 null 如实绑定（全局 Jackson 含 null）
        var builtin = skills.get(0);
        assertThat(builtin.id()).isEqualTo(BUILTIN_SKILL_HANDLE);
        assertThat(builtin.name()).isEqualTo("prd-writing");
        assertThat(builtin.description()).isEqualTo("撰写或修订 PRD 时使用");
        assertThat(builtin.source()).isEqualTo(1);
        assertThat(builtin.sourceName()).isEqualTo("内置");
        assertThat(builtin.sourcePackage()).isNull();
        assertThat(builtin.version()).isNull();
        assertThat(builtin.status()).isEqualTo(1);
        assertThat(builtin.statusName()).isEqualTo("启用");
        assertThat(builtin.operatorId()).isNull();
        assertThat(builtin.operatorName()).isNull();
        assertThat(builtin.updateAvailable()).isNull();

        // 次行：安装行——TSID 十进制串柄、来源 2/安装、来源包＋装时 commit、停用态＋
        // 治理留痕两肢、新版标记 true
        var installed = skills.get(1);
        assertThat(installed.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(installed.source()).isEqualTo(2);
        assertThat(installed.sourceName()).isEqualTo("安装");
        assertThat(installed.sourcePackage()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(installed.version()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(installed.status()).isEqualTo(2);
        assertThat(installed.statusName()).isEqualTo("停用");
        assertThat(installed.operatorId()).isEqualTo("700200");
        assertThat(installed.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(installed.updateAvailable()).isTrue();
    }

    // ========== 详情（opaque 柄出站 + frontmatter/正文全文）==========

    @Test
    void given_builtinHandle_when_getDetail_then_wireUrlCarriesEncodedOpaqueHandle() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, wireUrl);

        appService.getDetail(BUILTIN_SKILL_HANDLE);

        // 内置柄原样出站（字符串透传不解析不预校验——spec #79）；':' 经 URL 编码为 %3A
        // （provider 侧 @PathVariable 解码还原，同账号 externalId '|' 编码先例）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/builtin%3Aprd-writing");
    }

    @Test
    void given_tsidHandle_when_getDetail_then_wireUrlMirrorsProvider() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, wireUrl);

        appService.getDetail(INSTALLED_SKILL_ID);

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/" + INSTALLED_SKILL_ID);
    }

    @Test
    void given_skillDetailEnvelope_when_getDetail_then_metadataFrontmatterAndContentBound() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, new String[1]);

        AiplatformSkillDetailResponse detail = appService.getDetail(INSTALLED_SKILL_ID);

        // 元数据与清单行同形
        assertThat(detail.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(detail.source()).isEqualTo(2);
        assertThat(detail.sourceName()).isEqualTo("安装");
        assertThat(detail.version()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");

        // frontmatter 自由键值对象逐字透传：name/description 之外的键（license/requires）
        // 原样到达，不做形状断言（spec #79）；数组值保持原形
        assertThat(detail.frontmatter())
                .containsEntry("name", "web-research")
                .containsEntry("description", "联网检索与网页抓取整理")
                .containsEntry("license", "MIT")
                .containsKey("requires");
        assertThat(detail.frontmatter().get("requires")).isEqualTo(List.of("network"));

        // 正文全文（frontmatter 剥离后）原样到达——审核承载面，不经任何截断/改写
        assertThat(detail.content()).startsWith("# Web Research");
        assertThat(detail.content()).contains("\n\n## 步骤\n\n1. 拆解问题\n2. 检索来源");
        assertThat(detail.updateAvailable()).isFalse();
    }

    // ========== 错误透传（SKL_001 数字业务码，不映射）==========

    @Test
    void given_skl001ErrorEnvelope_when_getDetail_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithErrorTransport(
                404, SKL_001_ERROR_ENVELOPE, new String[1]);

        // 忠实透传（spec #79）：HTTP 404 + 数字业务码 7001（SKL_001＝域码 7×1000＋1）+ provider
        // message 原样——畸形/未寻址柄由 provider 裁决，admin 不预检不解释
        assertThatThrownBy(() -> appService.getDetail("builtin:no-such-skill"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(7001);
                    assertThat(upstream.getMessage()).isEqualTo("技能不存在");
                });
    }
}
