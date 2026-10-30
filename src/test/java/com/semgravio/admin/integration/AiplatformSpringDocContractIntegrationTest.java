package com.semgravio.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform BFF swagger 自描述契约（issue #75，admin 侧镜像 aiplatform #242）：拉
 * {@code /v3/api-docs}（springdoc 真实输出）断 components.schemas——钉住三处此前只能
 * 反向读源码的真实结构（wire 零变化，纯 @Schema 注解的自描述面；quote 载荷除外——其为
 * provider #203 后新增字段，wire/北向镜像同步补齐）：
 *
 * <ul>
 *   <li>成本金额字段（四个承载面）：币种分桶结构说明（键 = ISO 4217 币种码、值 = 金额）
 *       ＋示例——不再是 additionalProperties 裸空对象，前端按 api-docs 即可渲染「按币种成本」
 *       （admin-web #56）；</li>
 *   <li>对话条目四载荷（question / closing / attachments / quote）：描述＋示例非空
 *       （quote 须注明不含金额——视镜语义，aiplatform ADR-0017）；</li>
 *   <li>unitPrice 出入参：响应侧 string（精确十进制串交接，规避 BigDecimal 直出 JSON 落
 *       科学计数）、改价入参侧 number——两侧类型有意不对称、照实各自显式呈现
 *       （provider #160 定稿口径，BFF 忠实透传不做类型统一）；</li>
 *   <li>技能管理六端点 @Operation 描述镜像（issue #80 读面镜像 aiplatform #247 技能线 T1；
 *       issue #81 写面镜像 #248 技能线 T2）：清单/详情/安装/停用⇄启用/卸载的 summary＋
 *       description 逐字镜像 provider——opaque 串两形制柄
 *       （builtin:&lt;技能名&gt;／TSID 十进制串）、有界目录不分页、审核面「所见即运行时注入面」、
 *       404 SKL_001＝数字业务码 7001、安装 fail-fast 失败族 SKL_002–SKL_007/009 数字码、
 *       卸载守卫 SKL_008＝7008＋DELETE 动词等关键语义须在 api-docs 在场；北向 schema 的
 *       id/frontmatter/content 与安装命令体 repoUrl/excludeDirs 同步自描述；</li>
 *   <li>槽位指派两端点 @Operation 描述镜像（issue #82 镜像 aiplatform #249 技能线 T3）：
 *       读面（三职能槽位、含停用行实态、动态查库生效）与写面（PUT 整包替换——清单即终态、
 *       空清单＝清空）的 summary＋description 逐字镜像 provider——三槽位键
 *       main/executor/subagent、未知槽位 SKL_010＝7010、内置柄不可指派 SKL_011＝7011 等
 *       关键语义须在 api-docs 在场；北向 schema 的 slot/skills 与指派命令体 skillIds 同步
 *       自描述；清单来源 code 三值（1=内置 2=安装 3=自产——#90 纠偏，自产行 sourcePackage/
 *       version＝固定虚拟值 self）在场；</li>
 *   <li>指派双头预检端点 @Operation 描述镜像（issue #90 镜像 aiplatform #255，技能域第 16
 *       端点）：双头对照（生效工作协议∪已指派启用技能∪候选集内其他技能）、<strong>两态纪律</strong>
 *       （executed=false＝未执行≠无重叠、不得当「确认无重叠」呈现）、非阻断（不拦 PUT、
 *       组件故障不阻塞照常提交）、只读不留痕（无操作者头要求）、与 PUT 同形命令体
 *       （skillIds＝候选终态清单）、错误族 SKL_011/001/010 数字码、权限复用 skill:assign
 *       等关键语义须在 api-docs 在场；北向 schema 的 executed/hints 与 hint 行四字段
 *       （三 String 可 null 容差）同步自描述；</li>
 *   <li>显式更新＋更新留痕读面两端点 @Operation 描述镜像（issue #83 镜像 aiplatform #250
 *       技能线 T4）：更新写面（重拉快照＋版本留痕、永不自动跟新、同名行原地翻新柄稳定、
 *       远端未前进＝幂等回执不落痕不写行）与留痕读面（时间倒序、append-only 卸载不删留痕）
 *       的 summary＋description 逐字镜像 provider——SKL_013＝7013（移除项有指派在身整体拒）、
 *       SKL_012＝7012（来源包未安装）、SKL_014＝7014（来源包标识不能为空）等关键语义须在
 *       api-docs 在场；北向 schema 的更新命令体 sourcePackage、更新回执 fromVersion/
 *       removedSkillNames/skills 与留痕行 operatedAt 同步自描述；</li>
 *   <li>智能体配置三端点 @Operation 描述镜像（issue #84 镜像 aiplatform #251 技能线 T5）：
 *       读面（生效值＋覆盖标记＋枚举默认预览＋工具开关存储态＋最近写者、无覆盖行＝全回落、
 *       main/executor 两把稳定键、未知键 PRJ_033＝4033）、全量写（PUT 全量语义——请求体即
 *       覆盖态终态、null/缺省/纯空白＝清空回落、开关缺省 true、同值重写幂等不落痕、回滚＝
 *       留痕旧值快照写回、档位不做白名单、缺头 PRJ_034＝4034）与留痕读面（old&#42;/new&#42; 四件对、
 *       时间倒序、append-only）的 summary＋description 逐字镜像 provider；北向 schema 的
 *       agentKey/覆盖标记/默认预览与更新命令体四件、留痕行 operatedAt 同步自描述；</li>
 *   <li>工具面两端点 @Operation 描述镜像（issue #85 镜像 aiplatform #252 技能线 T6）：
 *       清单读面（三职能槽位、三呈现源——平台资产骨架/增强＋harness 内建＋子智能体声明面、
 *       骨架结构性锁死 ADR-0021、开关面只收增强两件、清单读面即全集）与增强工具开关
 *       （窄幅：关即退出装配面、开即回归、动态生效进行中 run 不定格、配置留痕同机制、
 *       PRJ_035＝4035／PRJ_036＝4036／PRJ_037＝4037／PRJ_034＝4034）的 summary＋description
 *       逐字镜像 provider；北向 schema 的 slot 三槽位/kind 三类别/enabled 挂载态与开关命令体
 *       enabled（必填、无缺省）同步自描述。</li>
 *   <li>草稿审核面四端点 @Operation 描述镜像（issue #92 镜像 aiplatform #258 自产线，技能域
 *       第 17–20 端点）：在途清单（活跃面恒在途、不分页、血统三件、扫描判定）、详情判读
 *       （正文全文＋scanFindings untyped map＋终态留痕、任意状态可查）、晋升（无请求体、
 *       撞名复查、入池同权<strong>不自动指派</strong>——入池与生效分离、回执即库行）、拒绝
 *       （理由必填终态留档、回执即终态详情）的 summary＋description 逐字镜像 provider——
 *       审核面纪律（活跃面恒在途、终态留档可查、晋升回执即库行、晋升不自动指派）与
 *       SKL_009/015/016/017/018 数字码须在 api-docs 在场；北向 schema 的草稿行血统三件/
 *       scanVerdict/status 三态、详情 scanFindings（空列表＝无发现）与终态留痕四字段、
 *       拒绝命令体 reason（可选绑定不前置拦截）同步自描述。</li>
 *   <li>订单设计面三处 @Operation 自描述（issue #96 镜像 aiplatform #297/#298，#95 设计
 *       能力线 T1）：清单/详情的交付物类型三值语义（1=设计 2=系统 3=系统＋设计、下单冻结
 *       自项目终点类型）＋设计资产包取件端点（下单冻结件直取、<strong>不按支付态门控</strong>、
 *       守卫序、ORD_016＝5016／PRJ_051＝4051 数字码）须在 api-docs 在场；北向 schema 的
 *       deliverableType（三个订单出口面：清单/详情/写回执）同步自描述。</li>
 *   <li>按张档位第 6 档自描述（issue #98 镜像 aiplatform #288，#95 设计能力线 T3）：单价行
 *       tokenKind 六档枚举（6=按张——开行/改价/停用对六档同一套语义）＋成本总览图片生成成本
 *       到场路径（cost 桶同桶混算、张不进五档分解——张与 token 并列不混算）＋unpriced 按张档
 *       （tokens＝张数）须在 api-docs 在场；北向 schema 的 tokenKind 同步自描述。</li>
 * </ul>
 *
 * <p>载具照 {@link AiplatformNorthboundSmokeIntegrationTest} 先例（RANDOM_PORT 真实 MVC
 * 拉 api-docs，不在认证拦截范围）；断言形制照 provider {@code SpringDocBackofficeContractTest}
 * （aiplatform #242）。与冒烟（端点全量可见）互补——此处钉 schema 形状自描述，防注解
 * 漂移回「裸对象」。</p>
 *
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiplatformSpringDocContractIntegrationTest {

    /** 四个成本承载面 schema 名（CostSummary 为 AiplatformProjectDetailResponse 嵌套 record，springdoc 平铺命名）。 */
    private static final List<String> COST_SURFACES = List.of(
            "AiplatformCostOverviewResponse",
            "AiplatformProjectCostResponse",
            "AiplatformProjectCostDetailResponse",
            "CostSummary");

    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final int port;

    @Autowired
    AiplatformSpringDocContractIntegrationTest(
            TestRestTemplate restTemplate, ObjectMapper objectMapper, @Value("${local.server.port}") int port) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.port = port;
    }

    @Test
    @DisplayName("swagger 契约：四个成本承载面的 cost 均自描述币种分桶（键 = ISO 4217 币种码）＋附示例")
    void given_costSurfaces_when_readSchemas_then_currencyBucketsSelfDescribed() throws Exception {
        for (String schema : COST_SURFACES) {
            JsonNode cost = property(schema, "cost");
            assertThat(cost.path("description").asText(""))
                    .as("%s.cost 应写明币种分桶结构（键 = ISO 4217 币种码、值 = 金额）", schema)
                    .contains("ISO 4217")
                    .contains("币种");
            assertThat(exampleText(cost))
                    .as("%s.cost 应附结构示例", schema)
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("swagger 契约：对话条目四载荷（question/closing/attachments/quote）结构说明＋示例齐备")
    void given_conversationEntry_when_readPayloads_then_fourPayloadsDocumented() throws Exception {
        JsonNode entry = schemas().path("AiplatformConversationEntryResponse");
        assertThat(entry.isMissingNode())
                .as("api-docs 应含 AiplatformConversationEntryResponse（缺 schema 视为契约漂移）")
                .isFalse();
        for (String field : List.of("question", "closing", "attachments", "quote")) {
            JsonNode payload = entry.path("properties").path(field);
            assertThat(payload.isMissingNode())
                    .as("AiplatformConversationEntryResponse.%s 缺字段（契约漂移）", field)
                    .isFalse();
            assertThat(payload.path("description").asText(""))
                    .as("AiplatformConversationEntryResponse.%s 应有结构说明", field)
                    .isNotBlank();
            assertThat(exampleText(payload))
                    .as("AiplatformConversationEntryResponse.%s 应附示例", field)
                    .isNotBlank();
        }
        // quote 专项：视镜语义不含金额（aiplatform ADR-0017）——消费方不得误以为对话史里有报价金额可取
        assertThat(entry.path("properties").path("quote").path("description").asText(""))
                .as("quote 载荷说明须注明不含金额")
                .contains("不含金额");
        // sessionSource 专项（#298 会话来源标识）：说明须自描述判据（2=设计会话＋drafts 判据＋
        // null=主会话缺省＋用户面前端可忽略），示例须两态语义可用（2）；sessionSourceName 中文名随行
        String sourceDescription = entry.path("properties").path("sessionSource").path("description").asText("");
        assertThat(entry.path("properties").path("sessionSource").isMissingNode())
                .as("AiplatformConversationEntryResponse.sessionSource 缺字段（契约漂移，#298）")
                .isFalse();
        assertThat(entry.path("properties").path("sessionSourceName").isMissingNode())
                .as("AiplatformConversationEntryResponse.sessionSourceName 缺字段（契约漂移，#298）")
                .isFalse();
        assertThat(sourceDescription)
                .as("sessionSource 说明须携 #298 判据原文（设计会话语义＋drafts 判据＋null 缺省）")
                .contains("设计会话")
                .contains("drafts")
                .contains("主会话缺省");
        assertThat(exampleText(entry.path("properties").path("sessionSource")))
                .as("sessionSource 应附示例")
                .isNotBlank();
        assertThat(entry.path("properties").path("sessionSourceName").path("description").asText(""))
                .as("sessionSourceName 应有结构说明（中文名随行）")
                .isNotBlank();
    }

    @Test
    @DisplayName("swagger 契约：unitPrice 响应侧 string、改价入参侧 number——类型照实呈现且两侧均说明")
    void given_unitPriceSurfaces_when_readSchemas_then_typesAsIsWithNotes() throws Exception {
        // 响应侧：十进制原串（BigDecimal 直出 JSON 会落科学计数）；清单行/停用回执/改价回执共形同 schema
        JsonNode response = property("AiplatformUnitPriceEntryResponse", "unitPrice");
        assertThat(response.path("type").asText(null))
                .as("unitPrice 响应侧类型照实 = string")
                .isEqualTo("string");
        // 入参侧（改价）：number，类型不统一、各自如实
        JsonNode command = property("AiplatformPriceEntryRepriceCommand", "unitPrice");
        assertThat(command.path("type").asText(null))
                .as("AiplatformPriceEntryRepriceCommand.unitPrice 入参侧类型照实 = number")
                .isEqualTo("number");
        // 两处均须显式说明＋示例（类型不对称是坑，说明是本票要点）
        for (Map.Entry<String, JsonNode> unitPrice : Map.of(
                "AiplatformUnitPriceEntryResponse", response,
                "AiplatformPriceEntryRepriceCommand", command
        ).entrySet()) {
            assertThat(unitPrice.getValue().path("description").asText(""))
                    .as("%s.unitPrice 应有类型说明", unitPrice.getKey())
                    .isNotBlank();
            assertThat(exampleText(unitPrice.getValue()))
                    .as("%s.unitPrice 应附示例", unitPrice.getKey())
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("swagger 契约：按张档位第 6 档自描述（#98 镜像 aiplatform #288）——tokenKind 六档＋总览图片生成成本到场路径")
    void given_perImageTier_when_readSchemasAndOperations_then_perImageTierDocumented() throws Exception {
        // 单价行 tokenKind：六档枚举自描述（1-5 token 档＋6=按张——#288 图片生成按张计价档，
        // 开行/改价/停用对六档同一套语义），Integer code 直通零 typed enum（tokenKindName 中文名随行）
        JsonNode tokenKind = property("AiplatformUnitPriceEntryResponse", "tokenKind");
        assertThat(tokenKind.path("description").asText(""))
                .as("tokenKind 应自描述六档（含 6=按张，#288）")
                .contains("1=输入")
                .contains("6=按张")
                .contains("同一套语义");
        assertThat(exampleText(tokenKind))
                .as("tokenKind 应附示例")
                .isNotBlank();

        JsonNode paths = apiDocs().path("paths");

        // 单价行清单 @Operation：六档枚举在场＋unitPrice 档位单价语义（token 档每 token/按张档每张）
        JsonNode entries = paths.path("/api/admin/aiplatform/price-entries").path("get");
        assertThat(entries.isMissingNode()).as("api-docs 应含单价行清单端点").isFalse();
        assertThat(entries.path("description").asText(""))
                .contains("6=按张")
                .contains("每张");

        // 成本总览 @Operation：图片生成成本到场路径在场——cost 桶同桶混算（按张×时点生效单价，
        // 与 token 成本不单列）＋张不进 total/byModel/byAgentKind 五档分解（张与 token 并列不混算）
        JsonNode overview = paths.path("/api/admin/aiplatform/costs/overview").path("get");
        assertThat(overview.isMissingNode()).as("api-docs 应含成本总览端点").isFalse();
        assertThat(overview.path("description").asText(""))
                .contains("图片生成成本")
                .contains("按张")
                .contains("并列不混算");

        // unpriced @Operation：六档枚举在场（按张档未配价同公式进警示，tokens 对该档＝张数）
        JsonNode unpriced = paths.path("/api/admin/aiplatform/costs/unpriced").path("get");
        assertThat(unpriced.isMissingNode()).as("api-docs 应含 unpriced 端点").isFalse();
        assertThat(unpriced.path("description").asText(""))
                .contains("6=按张")
                .contains("张数");
    }

    @Test
    @DisplayName("swagger 契约：技能清单/详情 @Operation 描述镜像 provider（#80，opaque 两形制柄 + 审核面语义在场）")
    void given_skillEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 清单：summary 与 description 逐字镜像 provider（#247）——同权呈现、字段语义、
        // 服务端定死排序、有界目录不分页不过滤、opaque 串两形制柄
        JsonNode list = paths.path("/api/admin/aiplatform/skills").path("get");
        assertThat(list.isMissingNode()).as("api-docs 应含技能清单端点").isFalse();
        assertThat(list.path("summary").asText(""))
                .contains("内置＋安装同权").contains("不分页");
        assertThat(list.path("description").asText(""))
                .contains("空库时仍呈现内置技能")
                .contains("来源 code（1=内置 2=安装 3=自产）")
                .contains("固定虚拟值 self")
                .contains("内置在前").contains("来源包、名称序")
                .contains("有界目录")
                .contains("不分页不过滤")
                .contains("opaque 串两形制")
                .contains("builtin:<技能名>").contains("TSID 十进制串");

        // 详情：审核面语义（所见即运行时注入面）＋ frontmatter/正文分面＋ 404 SKL_001 数字码
        JsonNode detail = paths.path("/api/admin/aiplatform/skills/{id}").path("get");
        assertThat(detail.isMissingNode()).as("api-docs 应含技能详情端点").isFalse();
        assertThat(detail.path("summary").asText(""))
                .contains("元数据＋frontmatter＋正文").contains("审核面");
        assertThat(detail.path("description").asText(""))
                .contains("所见即运行时注入面")
                .contains("frontmatter 全量")
                .contains("SKL_001").contains("7001");

        // 北向行 schema 自描述：id 柄 opaque 两形制（前端勿做数值假设——#75 自描述先例同款）
        JsonNode skillId = property("AiplatformSkillSummaryResponse", "id");
        assertThat(skillId.path("description").asText(""))
                .contains("opaque 串两形制")
                .contains("builtin:<技能名>").contains("TSID 十进制串");
        assertThat(exampleText(skillId)).isEqualTo("builtin:prd-writing");

        // 详情 schema 自描述：frontmatter 自由键值（逐字透传、不做形状断言）＋正文审核承载面
        JsonNode frontmatter = property("AiplatformSkillDetailResponse", "frontmatter");
        assertThat(frontmatter.path("description").asText("")).contains("frontmatter 全量");
        assertThat(exampleText(frontmatter)).isNotBlank();
        assertThat(property("AiplatformSkillDetailResponse", "content")
                .path("description").asText("")).contains("审核承载面");

        // 使用计数观测面 schema 自描述（#93 T2 追平）：loadCount 口径（load 实际发生、
        // 指派/启停/清单重建不计数；0＝从未加载；内置恒 null 不适用）＋JSON string 形态提示、
        // lastLoadedAt 时刻语义
        JsonNode loadCount = property("AiplatformSkillSummaryResponse", "loadCount");
        assertThat(loadCount.path("description").asText(""))
                .contains("load 实际发生")
                .contains("指派/启停/清单重建不计数")
                .contains("0＝从未被加载过")
                .contains("内置技能恒 null 不适用")
                .contains("string 形态");
        assertThat(exampleText(loadCount)).isEqualTo("3");
        assertThat(property("AiplatformSkillSummaryResponse", "lastLoadedAt")
                .path("description").asText("")).contains("从未被加载过");

        // resources 资源面 schema 自描述（#253 漂移补齐）：相对路径→内容全文、
        // 空对象＝无 scripts／内置
        JsonNode resources = property("AiplatformSkillDetailResponse", "resources");
        assertThat(resources.path("description").asText(""))
                .contains("内容全文")
                .contains("空对象＝无 scripts／内置技能");
        assertThat(exampleText(resources)).isNotBlank();
    }

    @Test
    @DisplayName("swagger 契约：技能四写端点 @Operation 描述镜像 provider（#81，失败族数字码 + DELETE 动词在场）")
    void given_skillWriteEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 安装：快照固化语义＋excludeDirs 排除＋SKL_002–SKL_007/009 失败族数字码＋回执共形
        JsonNode install = paths.path("/api/admin/aiplatform/skills/install").path("post");
        assertThat(install.isMissingNode()).as("api-docs 应含技能安装端点").isFalse();
        assertThat(install.path("summary").asText(""))
                .contains("安装技能包").contains("快照固化");
        assertThat(install.path("description").asText(""))
                .contains("ADR-0021")
                .contains("excludeDirs")
                .contains("段名精确匹配")
                .contains("fail-fast")
                .contains("SKL_002").contains("7002")
                .contains("SKL_003").contains("7003")
                .contains("SKL_004").contains("7004")
                .contains("SKL_005").contains("7005")
                .contains("SKL_006").contains("7006")
                .contains("SKL_007").contains("7007")
                .contains("SKL_009").contains("7009")
                .contains("回执＝本次装入的条目清单");

        // 停用：可逆开关＋幂等＋缺头守卫数字码＋404 SKL_001
        JsonNode disable = paths.path("/api/admin/aiplatform/skills/{id}/disable").path("post");
        assertThat(disable.isMissingNode()).as("api-docs 应含技能停用端点").isFalse();
        assertThat(disable.path("summary").asText("")).contains("停用技能").contains("可逆开关");
        assertThat(disable.path("description").asText(""))
                .contains("退出装配候选")
                .contains("重复停用幂等")
                .contains("SKL_009").contains("7009")
                .contains("SKL_001").contains("7001")
                .contains("回执＝翻转后清单行");

        // 启用：停用的可逆侧＋口径同停用
        JsonNode enable = paths.path("/api/admin/aiplatform/skills/{id}/enable").path("post");
        assertThat(enable.isMissingNode()).as("api-docs 应含技能启用端点").isFalse();
        assertThat(enable.path("summary").asText("")).contains("启用技能").contains("恢复装配候选");
        assertThat(enable.path("description").asText(""))
                .contains("重复启用幂等")
                .contains("SKL_009").contains("7009")
                .contains("SKL_001").contains("7001");

        // 卸载：不可逆＋指派守卫 SKL_008 数字码＋删除前终态回执＋DELETE 动词
        JsonNode uninstall = paths.path("/api/admin/aiplatform/skills/{id}").path("delete");
        assertThat(uninstall.isMissingNode()).as("api-docs 应含技能卸载端点（DELETE 动词）").isFalse();
        assertThat(uninstall.path("summary").asText("")).contains("卸载技能").contains("不可逆");
        assertThat(uninstall.path("description").asText(""))
                .contains("SKL_008").contains("7008")
                .contains("先解绑再卸")
                .contains("回执＝删除前终态")
                .contains("内置技能不可卸")
                .contains("SKL_001").contains("7001")
                .contains("DELETE");

        // 安装命令 schema 自描述：repoUrl 装时固化快照语义＋excludeDirs 任一段命中即不入库
        JsonNode repoUrl = property("AiplatformSkillInstallCommand", "repoUrl");
        assertThat(repoUrl.path("description").asText("")).contains("git 地址");
        assertThat(exampleText(repoUrl)).isNotBlank();
        JsonNode excludeDirs = property("AiplatformSkillInstallCommand", "excludeDirs");
        assertThat(excludeDirs.path("description").asText("")).contains("任一段命中即不入库");
        assertThat(exampleText(excludeDirs)).isNotBlank();
    }

    @Test
    @DisplayName("swagger 契约：槽位指派两端点 @Operation 描述镜像 provider（#82，整包替换 + 三槽位 + SKL_010/011 数字码在场）")
    void given_slotAssignmentEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 读面：三职能槽位语义＋含停用行实态＋动态查库生效＋未知槽位 404 SKL_010 数字码
        JsonNode read = paths.path("/api/admin/aiplatform/skills/assignments/{slot}").path("get");
        assertThat(read.isMissingNode()).as("api-docs 应含槽位指派读面端点").isFalse();
        assertThat(read.path("summary").asText(""))
                .contains("槽位指派读面").contains("三职能槽位各自独立");
        assertThat(read.path("description").asText(""))
                .contains("与技能清单行共形")
                .contains("含停用行")
                .contains("main=主智能体")
                .contains("executor=run 执行体")
                .contains("subagent=子智能体")
                .contains("SKL_010").contains("7010")
                .contains("动态查库")
                .contains("进行中 run 不定格不打断")
                .contains("回执与 PUT 同形");

        // 写面：PUT 全量语义（清单即终态）＋内置柄不可指派 SKL_011＋柄/缺头/槽位错误族数字码＋PUT 动词
        JsonNode put = paths.path("/api/admin/aiplatform/skills/assignments/{slot}").path("put");
        assertThat(put.isMissingNode()).as("api-docs 应含槽位指派写面端点（PUT 动词）").isFalse();
        assertThat(put.path("summary").asText(""))
                .contains("槽位指派整包替换").contains("清单即终态");
        assertThat(put.path("description").asText(""))
                .contains("skillIds 即该槽位终态")
                .contains("未列入即解绑")
                .contains("空清单＝清空")
                .contains("SKL_011").contains("7011")
                .contains("SKL_001").contains("7001")
                .contains("SKL_009").contains("7009")
                .contains("SKL_010").contains("7010")
                .contains("回执＝替换后读面")
                .contains("PUT");

        // 指派命令 schema 自描述：skillIds 整包替换语义＋空清单清空＋内置柄拒
        JsonNode skillIds = property("AiplatformSkillAssignCommand", "skillIds");
        assertThat(skillIds.path("description").asText(""))
                .contains("整包替换")
                .contains("清单即终态")
                .contains("空清单＝清空该槽位")
                .contains("SKL_011");
        assertThat(exampleText(skillIds)).isNotBlank();

        // 回执 schema 自描述：slot 三把稳定键＋skills 含停用行实态
        JsonNode slot = property("AiplatformSlotAssignmentResponse", "slot");
        assertThat(slot.path("description").asText(""))
                .contains("main=主智能体")
                .contains("SKL_010");
        assertThat(exampleText(slot)).isEqualTo("executor");
        assertThat(property("AiplatformSlotAssignmentResponse", "skills")
                .path("description").asText("")).contains("含停用行");
    }

    @Test
    @DisplayName("swagger 契约：指派双头预检端点 @Operation 描述镜像 provider（#90，两态纪律 + 非阻断 + 只读不留痕 + 错误族数字码在场）")
    void given_slotPrecheckEndpoint_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 预检（技能域第 16 端点，镜像 #255）：双头对照＋两态纪律＋非阻断＋只读不留痕＋
        // 与 PUT 同形命令体＋错误族数字码＋权限复用 skill:assign
        JsonNode precheck = paths.path("/api/admin/aiplatform/skills/assignments/{slot}/precheck").path("post");
        assertThat(precheck.isMissingNode()).as("api-docs 应含指派双头预检端点").isFalse();
        assertThat(precheck.path("summary").asText(""))
                .contains("指派双头预检").contains("非阻断提示").contains("只读");
        assertThat(precheck.path("description").asText(""))
                .contains("生效工作协议")
                .contains("已指派启用技能")
                .contains("候选集内其他技能")
                .contains("确认无重叠")
                .contains("不得当「确认无重叠」呈现")
                .contains("如实标注「预检未执行」")
                .contains("非阻断")
                .contains("不拦 PUT 指派")
                .contains("照常提交")
                .contains("无操作者头要求")
                .contains("skillIds＝候选终态清单")
                .contains("复用")
                .contains("skill:assign")
                .contains("SKL_011").contains("7011")
                .contains("SKL_001").contains("7001")
                .contains("SKL_010").contains("7010");

        // 回执 schema 自描述：executed 两态纪律（admin-web 消费契约——#90 验收钉两态纪律进
        // 北向自描述）＋hints 空/非空语义
        assertThat(property("AiplatformSkillPrecheckResponse", "executed")
                .path("description").asText(""))
                .contains("不得当")
                .contains("不拦 PUT")
                .contains("预检未执行");
        assertThat(property("AiplatformSkillPrecheckResponse", "hints")
                .path("description").asText(""))
                .contains("确认无重叠")
                .contains("可 null");

        // hint 行 schema 自描述（springdoc 平铺嵌套 record 命名 PrecheckHint——CostSummary 先例）：
        // 四字段语义＋三 String 字段 null 容差
        assertThat(property("PrecheckHint", "skills")
                .path("description").asText("")).contains("候选技能名");
        assertThat(property("PrecheckHint", "counterpart")
                .path("description").asText(""))
                .contains("对照侧").contains("可 null");
        assertThat(property("PrecheckHint", "overlap")
                .path("description").asText("")).contains("重叠内容").contains("可 null");
        assertThat(property("PrecheckHint", "resolution")
                .path("description").asText("")).contains("消解方向").contains("可 null");
    }

    @Test
    @DisplayName("swagger 契约：显式更新＋留痕读面两端点 @Operation 描述镜像 provider（#83，幂等语义 + SKL_012/013/014 数字码在场）")
    void given_skillUpdateEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 显式更新：来源包原值回传＋翻新语义（柄稳定）＋幂等回执＋失败族数字码＋回执三肢
        JsonNode update = paths.path("/api/admin/aiplatform/skills/update").path("post");
        assertThat(update.isMissingNode()).as("api-docs 应含技能显式更新端点").isFalse();
        assertThat(update.path("summary").asText(""))
                .contains("显式更新技能包").contains("重拉快照＋版本留痕");
        assertThat(update.path("description").asText(""))
                .contains("永不自动跟新")
                .contains("sourcePackage 取清单行原值回传")
                .contains("TSID 柄")
                .contains("SKL_013").contains("7013")
                .contains("幂等回执").contains("from==to").contains("不落痕不写行")
                .contains("SKL_009").contains("7009")
                .contains("SKL_014").contains("7014")
                .contains("SKL_012").contains("7012")
                .contains("SKL_004").contains("7004")
                .contains("SKL_005").contains("7005")
                .contains("SKL_007").contains("7007")
                .contains("from/to 版本＋被移除技能名＋更新后终态行集");

        // 留痕读面：时间倒序＋append-only（卸载不删留痕）＋sourcePackage 必填校验数字码
        JsonNode traces = paths.path("/api/admin/aiplatform/skills/update-traces").path("get");
        assertThat(traces.isMissingNode()).as("api-docs 应含更新留痕读面端点").isFalse();
        assertThat(traces.path("summary").asText(""))
                .contains("更新留痕读面").contains("历史版本可查");
        assertThat(traces.path("description").asText(""))
                .contains("from→to 版本＋操作者＋时刻")
                .contains("时间倒序")
                .contains("append-only")
                .contains("卸载不删留痕")
                .contains("装时版本经链条首个 from 回溯")
                .contains("SKL_014").contains("7014");

        // 更新命令 schema 自描述：sourcePackage 取清单行原值回传（同源身份单源）
        JsonNode sourcePackage = property("AiplatformSkillUpdateCommand", "sourcePackage");
        assertThat(sourcePackage.path("description").asText(""))
                .contains("取技能清单行 sourcePackage 原值回传")
                .contains("永不自动跟新");
        assertThat(exampleText(sourcePackage)).isNotBlank();

        // 更新回执 schema 自描述：幂等语义（from==to）＋移除名＋终态行集同形
        JsonNode fromVersion = property("AiplatformSkillUpdateResponse", "fromVersion");
        assertThat(fromVersion.path("description").asText("")).contains("幂等回执");
        assertThat(property("AiplatformSkillUpdateResponse", "removedSkillNames")
                .path("description").asText("")).contains("SKL_013");
        assertThat(property("AiplatformSkillUpdateResponse", "skills")
                .path("description").asText("")).contains("清单行同形");

        // 留痕行 schema 自描述：时刻倒序语义
        assertThat(property("AiplatformSkillUpdateTraceResponse", "operatedAt")
                .path("description").asText("")).contains("时间倒序");
    }

    @Test
    @DisplayName("swagger 契约：智能体配置三端点 @Operation 描述镜像 provider（#84，覆盖标记/默认预览 + 幂等回滚语义 + PRJ_033/034 数字码在场）")
    void given_agentConfigEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 读面：生效值＋覆盖标记＋默认预览＋工具开关＋最近写者——无覆盖行全回落、两把稳定键、
        // 未知键 404 PRJ_033 数字码
        JsonNode config = paths.path("/api/admin/aiplatform/agent-configs/{agentKey}").path("get");
        assertThat(config.isMissingNode()).as("api-docs 应含智能体配置读面端点").isFalse();
        assertThat(config.path("summary").asText(""))
                .contains("智能体配置读面").contains("生效值＋覆盖标记＋默认预览");
        assertThat(config.path("description").asText(""))
                .contains("库覆盖值或枚举默认")
                .contains("systemPromptOverridden/modelIdOverridden")
                .contains("defaultSystemPrompt")
                .contains("清空覆盖即落此值")
                .contains("webSearchEnabled/fetchUrlEnabled")
                .contains("最近写者")
                .contains("无覆盖行＝全回落")
                .contains("main=主智能体").contains("executor=run 执行体")
                .contains("classify/naming")
                .contains("PRJ_033").contains("4033");

        // 全量写：PUT 全量语义（请求体即覆盖态终态）＋清空回落＋开关缺省 true＋幂等不落痕＋
        // 回滚＝留痕旧值写回＋不做白名单＋缺头 PRJ_034 数字码＋回执同形＋PUT 动词
        JsonNode update = paths.path("/api/admin/aiplatform/agent-configs/{agentKey}").path("put");
        assertThat(update.isMissingNode()).as("api-docs 应含智能体配置全量写端点（PUT 动词）").isFalse();
        assertThat(update.path("summary").asText(""))
                .contains("智能体配置全量写").contains("覆盖态终态＋变更留痕");
        assertThat(update.path("description").asText(""))
                .contains("PUT 全量语义")
                .contains("null/缺省/纯空白＝清空覆盖")
                .contains("缺省 true＝开")
                .contains("进行中 run 不定格")
                .contains("append-only")
                .contains("同值重写＝幂等回执不落痕")
                .contains("回滚＝把留痕旧值快照写回")
                .contains("不做白名单校验")
                .contains("PRJ_034").contains("4034")
                .contains("PRJ_033").contains("4033")
                .contains("回执与 GET 同形")
                .contains("PUT");

        // 留痕读面：时间倒序＋old*/new* 四件对（回滚依据）＋append-only＋未知键数字码
        JsonNode traces = paths.path("/api/admin/aiplatform/agent-configs/{agentKey}/traces").path("get");
        assertThat(traces.isMissingNode()).as("api-docs 应含配置变更留痕读面端点").isFalse();
        assertThat(traces.path("summary").asText(""))
                .contains("配置变更留痕读面").contains("回滚依据");
        assertThat(traces.path("description").asText(""))
                .contains("时间倒序")
                .contains("同值幂等重写不落痕")
                .contains("old*/new* 四件对")
                .contains("append-only")
                .contains("留痕不随任何动作删除")
                .contains("旧值快照 PUT 回写")
                .contains("PRJ_033").contains("4033");

        // 读面 schema 自描述：agentKey 两把稳定键＋覆盖标记语义＋默认预览落点＋写者可空
        JsonNode agentKey = property("AiplatformAgentConfigResponse", "agentKey");
        assertThat(agentKey.path("description").asText(""))
                .contains("main=主智能体").contains("executor=run 执行体");
        assertThat(exampleText(agentKey)).isEqualTo("main");
        assertThat(property("AiplatformAgentConfigResponse", "systemPromptOverridden")
                .path("description").asText("")).contains("是否库覆盖值");
        assertThat(property("AiplatformAgentConfigResponse", "defaultSystemPrompt")
                .path("description").asText("")).contains("清空覆盖即落此值");
        assertThat(property("AiplatformAgentConfigResponse", "operatorId")
                .path("description").asText("")).contains("null＝从未配置");

        // 全量写命令 schema 自描述：清空回落语义＋开关缺省 true（装箱——缺省≠关）
        JsonNode systemPrompt = property("AiplatformAgentConfigUpdateCommand", "systemPrompt");
        assertThat(systemPrompt.path("description").asText("")).contains("null/缺省/纯空白＝清空覆盖");
        assertThat(property("AiplatformAgentConfigUpdateCommand", "modelId")
                .path("description").asText("")).contains("不做白名单校验");
        assertThat(exampleText(property("AiplatformAgentConfigUpdateCommand", "modelId")))
                .isEqualTo("deepseek-v4-pro");
        assertThat(property("AiplatformAgentConfigUpdateCommand", "webSearchEnabled")
                .path("description").asText("")).contains("缺省 true＝开");

        // 留痕行 schema 自描述：old 侧可空语义（回滚写回依据）＋时刻倒序
        assertThat(property("AiplatformAgentConfigTraceResponse", "oldSystemPrompt")
                .path("description").asText("")).contains("回滚即把此值写回");
        assertThat(property("AiplatformAgentConfigTraceResponse", "operatedAt")
                .path("description").asText("")).contains("时间倒序");
    }

    @Test
    @DisplayName("swagger 契约：工具面两端点 @Operation 描述镜像 provider（#85，三槽位三呈现源 + PRJ_035/036/037/034 数字码在场）")
    void given_agentToolEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 清单读面：三职能槽位（与技能槽位同键）＋三呈现源＋骨架结构性锁死＋开关面只收增强两件
        JsonNode inventory = paths.path("/api/admin/aiplatform/agent-tools").path("get");
        assertThat(inventory.isMissingNode()).as("api-docs 应含工具面清单端点").isFalse();
        assertThat(inventory.path("summary").asText(""))
                .contains("工具面清单").contains("按职能槽位列当前挂载工具");
        assertThat(inventory.path("description").asText(""))
                .contains("排障/审计面")
                .contains("main=主智能体").contains("executor=run 执行体").contains("subagent=子智能体")
                .contains("与技能槽位同键")
                .contains("三呈现源")
                .contains("骨架=编排链路＋项目事实只读件")
                .contains("enabled 按运营配置生效态")
                .contains("false＝在册但退出装配面")
                .contains("harness 内建编码工具")
                .contains("不可开关")
                .contains("子智能体声明工具面")
                .contains("ADR-0021")
                .contains("开关面只收增强两件")
                .contains("清单读面即全集");

        // 增强工具开关：窄幅语义＋动态生效＋配置留痕同机制＋四错误族数字码＋回执同形＋PUT 动词
        JsonNode toggle = paths.path("/api/admin/aiplatform/agent-tools/{toolName}").path("put");
        assertThat(toggle.isMissingNode()).as("api-docs 应含增强工具开关端点（PUT 动词）").isFalse();
        assertThat(toggle.path("summary").asText(""))
                .contains("增强工具开关").contains("窄幅：关即退出装配面、开即回归");
        assertThat(toggle.path("description").asText(""))
                .contains("web_search").contains("fetch_url")
                .contains("退出对应槽位装配面")
                .contains("进行中 run 不定格")
                .contains("配置留痕")
                .contains("幂等回执零写入")
                .contains("回滚＝旧值写回")
                .contains("PRJ_036").contains("4036")
                .contains("PRJ_035").contains("4035")
                .contains("PRJ_037").contains("4037")
                .contains("PRJ_034").contains("4034")
                .contains("回执＝开关后工具行")
                .contains("PUT");

        // 工具行 schema 自描述：注册名＝按名寻址键（清单读面即全集）＋kind 三类别（开关判定正本）
        // ＋enabled 挂载态（骨架恒 true、增强按生效态）
        JsonNode name = property("AiplatformAgentToolResponse", "name");
        assertThat(name.path("description").asText(""))
                .contains("按名寻址键")
                .contains("清单读面即全集");
        assertThat(exampleText(name)).isEqualTo("web_search");
        JsonNode kind = property("AiplatformAgentToolResponse", "kind");
        assertThat(kind.path("description").asText(""))
                .contains("SKELETON").contains("ENHANCEMENT").contains("HARNESS_BUILTIN")
                .contains("开关判定正本");
        assertThat(exampleText(kind)).isEqualTo("ENHANCEMENT");
        assertThat(property("AiplatformAgentToolResponse", "enabled")
                .path("description").asText(""))
                .contains("在册但退出装配面")
                .contains("恒 true");

        // 槽位行 schema 自描述：三槽位稳定键（与技能槽位同键）＋呈现序 provider 定死
        JsonNode slot = property("AiplatformAgentToolSlotResponse", "slot");
        assertThat(slot.path("description").asText(""))
                .contains("main=主智能体")
                .contains("与技能槽位同键");
        assertThat(exampleText(slot)).isEqualTo("main");
        assertThat(property("AiplatformAgentToolSlotResponse", "tools")
                .path("description").asText("")).contains("平台资产在前");

        // 开关命令 schema 自描述：enabled 必填、无缺省（缺省 null 透传＝PRJ_037——provider 裁决）
        JsonNode enabled = property("AiplatformAgentToolToggleCommand", "enabled");
        assertThat(enabled.path("description").asText(""))
                .contains("必填")
                .contains("无缺省")
                .contains("PRJ_037");
        assertThat(exampleText(enabled)).isEqualTo("false");
    }

    @Test
    @DisplayName("swagger 契约：草稿审核面四端点 @Operation 描述镜像 provider（#92，活跃面纪律 + 晋升不自动指派 + SKL_009/015/016/017/018 数字码在场）")
    void given_skillDraftEndpoints_when_readOperations_then_descriptionsMirrorProvider() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 在途清单：活跃面＝在途（人审队列）＋不分页＋血统三件＋扫描判定 SAFE/CAUTION
        JsonNode list = paths.path("/api/admin/aiplatform/skills/drafts").path("get");
        assertThat(list.isMissingNode()).as("api-docs 应含草稿在途清单端点").isFalse();
        assertThat(list.path("summary").asText(""))
                .contains("技能草稿清单").contains("活跃面＝在途").contains("人审队列");
        assertThat(list.path("description").asText(""))
                .contains("自荐时间倒序")
                .contains("终态（已晋升/已拒绝）不列活跃面")
                .contains("详情仍可按 id 查")
                .contains("血统三件")
                .contains("main/executor/subagent")
                .contains("SAFE/CAUTION")
                .contains("DANGEROUS")
                .contains("草稿不参与任何装配")
                .contains("不分页")
                .contains("TSID 十进制串");

        // 详情判读：审核面（正文全文＋findings 逐条＋终态留痕）＋任意状态可查＋404 SKL_015 数字码
        JsonNode detail = paths.path("/api/admin/aiplatform/skills/drafts/{id}").path("get");
        assertThat(detail.isMissingNode()).as("api-docs 应含草稿详情端点").isFalse();
        assertThat(detail.path("summary").asText(""))
                .contains("技能草稿详情").contains("正文＋扫描回执＋血统").contains("审核面");
        assertThat(detail.path("description").asText(""))
                .contains("真伪")
                .contains("血统可溯")
                .contains("外部资料接触史必查")
                .contains("a-only 无 scripts")
                .contains("patternId/severity/category/file/line/matchText/description")
                .contains("空列表＝无发现")
                .contains("untyped map 逐字透传")
                .contains("在途恒 null")
                .contains("任意状态可查")
                .contains("SKL_015").contains("7015");

        // 晋升：无请求体＋撞名复查＋入池同权不自动指派（入池与生效分离）＋回执即库行＋错误族数字码
        JsonNode promote = paths.path("/api/admin/aiplatform/skills/drafts/{id}/promote").path("post");
        assertThat(promote.isMissingNode()).as("api-docs 应含草稿晋升端点").isFalse();
        assertThat(promote.path("summary").asText(""))
                .contains("晋升草稿").contains("一键入技能库").contains("单级晋升");
        assertThat(promote.path("description").asText(""))
                .contains("撞名复查")
                .contains("SKL_016").contains("7016")
                .contains("来源＝3 自产")
                .contains("固定虚拟值 self")
                .contains("审核者不改稿")
                .contains("操作者两处留痕")
                .contains("不自动指派")
                .contains("入池与生效分离")
                .contains("无 scripts")
                .contains("SKL_017").contains("7017")
                .contains("SKL_015").contains("7015")
                .contains("SKL_009").contains("7009")
                .contains("无请求体")
                .contains("回执＝新库行清单行");

        // 拒绝：理由必填终态留档＋拒绝即终态（不改稿不复活）＋可选绑定不前置拦截＋回执即终态详情
        JsonNode reject = paths.path("/api/admin/aiplatform/skills/drafts/{id}/reject").path("post");
        assertThat(reject.isMissingNode()).as("api-docs 应含草稿拒绝端点").isFalse();
        assertThat(reject.path("summary").asText(""))
                .contains("拒绝草稿").contains("终态留档").contains("理由必填");
        assertThat(reject.path("description").asText(""))
                .contains("SKL_018").contains("7018")
                .contains("BFF 可选绑定不前置拦截")
                .contains("拒绝即终态")
                .contains("不改稿不复活")
                .contains("重提只能靠未来会话产生新草稿")
                .contains("已拒绝不占名")
                .contains("SKL_017").contains("7017")
                .contains("SKL_015").contains("7015")
                .contains("SKL_009").contains("7009")
                .contains("body 仅 reason")
                .contains("终态草稿详情")
                .contains("留痕形状直读");

        // 草稿行 schema 自描述：id TSID 十进制串（勿做数值假设）＋slot 三稳定键＋scanVerdict
        // 判定语义＋status 三态（列表＝活跃面恒 1）
        JsonNode draftId = property("AiplatformSkillDraftSummaryResponse", "id");
        assertThat(draftId.path("description").asText(""))
                .contains("TSID 十进制串")
                .contains("勿做数值假设")
                .contains("SKL_015");
        JsonNode slot = property("AiplatformSkillDraftSummaryResponse", "slot");
        assertThat(slot.path("description").asText(""))
                .contains("main=主智能体")
                .contains("subagent=子智能体");
        assertThat(exampleText(slot)).isEqualTo("executor");
        assertThat(property("AiplatformSkillDraftSummaryResponse", "scanVerdict")
                .path("description").asText(""))
                .contains("SAFE/CAUTION")
                .contains("DANGEROUS");
        assertThat(property("AiplatformSkillDraftSummaryResponse", "status")
                .path("description").asText(""))
                .contains("1=在途 2=已晋升 3=已拒绝")
                .contains("活跃面恒 1");

        // 详情 schema 自描述：scanFindings untyped 留档（空列表＝无发现）＋终态留痕四字段可空
        assertThat(property("AiplatformSkillDraftDetailResponse", "content")
                .path("description").asText(""))
                .contains("a-only 无 scripts")
                .contains("审核者不改稿");
        assertThat(property("AiplatformSkillDraftDetailResponse", "scanFindings")
                .path("description").asText(""))
                .contains("空列表＝无发现")
                .contains("untyped")
                .contains("不做形状断言");
        assertThat(property("AiplatformSkillDraftDetailResponse", "operatorId")
                .path("description").asText(""))
                .contains("在途为 null");
        assertThat(property("AiplatformSkillDraftDetailResponse", "rejectReason")
                .path("description").asText(""))
                .contains("已拒绝态携带");

        // 拒绝命令 schema 自描述：reason 终态留档语义＋空白 SKL_018（provider 裁决不前置拦截）
        JsonNode reason = property("AiplatformSkillDraftRejectCommand", "reason");
        assertThat(reason.path("description").asText(""))
                .contains("终态留档")
                .contains("拒绝须有据")
                .contains("SKL_018")
                .contains("不前置拦截");
        assertThat(exampleText(reason)).isNotBlank();
    }

    @Test
    @DisplayName("swagger 契约：订单设计面三处 @Operation 自描述（#96，类型三值语义 + 冻结件直取不按支付态门控 + ORD_016/PRJ_051 数字码在场）")
    void given_orderDesignSurfaces_when_readOperations_then_descriptionsSelfDescribed() throws Exception {
        JsonNode paths = apiDocs().path("paths");

        // 清单：交付物类型字段对在场（类型列）＋取件二选一＋无类型筛选维度（裁决已备案）
        JsonNode list = paths.path("/api/admin/aiplatform/orders").path("get");
        assertThat(list.isMissingNode()).as("api-docs 应含订单清单端点").isFalse();
        assertThat(list.path("description").asText(""))
                .contains("交付物类型字段对")
                .contains("deliverableType/deliverableTypeName")
                .contains("1=设计 2=系统 3=系统＋设计")
                .contains("下单冻结自项目终点类型")
                .contains("下载按钮按类型二选一")
                .contains("design-package")
                .contains("source-package")
                .contains("无类型筛选维度");

        // 详情：基本信息类型 tag 数据面＋设计单 PRD 快照即含设计物清单章
        JsonNode detail = paths.path("/api/admin/aiplatform/orders/{id}").path("get");
        assertThat(detail.isMissingNode()).as("api-docs 应含订单详情端点").isFalse();
        assertThat(detail.path("description").asText(""))
                .contains("交付物类型")
                .contains("deliverableType/deliverableTypeName")
                .contains("1=设计 2=系统 3=系统＋设计")
                .contains("设计单 PRD 快照即含设计物清单章");

        // 设计资产包端点（#96 新增）：下单冻结件直取＋不按支付态门控＋守卫序＋错误族数字码
        JsonNode design = paths.path("/api/admin/aiplatform/orders/{id}/design-package").path("get");
        assertThat(design.isMissingNode()).as("api-docs 应含设计资产包端点").isFalse();
        assertThat(design.path("summary").asText(""))
                .contains("设计资产包")
                .contains("下单冻结件直取")
                .contains("无 ApiResponse 信封");
        assertThat(design.path("description").asText(""))
                .contains("对偶源码包")
                .contains("选件式 tar")
                .contains("DESIGN.md")
                .contains("按单定格")
                .contains("不按支付态门控")
                .contains("未支付/已取消照取")
                .contains("守卫序")
                .contains("ORD_001").contains("5001")
                .contains("ORD_016").contains("5016")
                .contains("PRJ_051").contains("4051")
                .contains("WSP_002").contains("1002")
                .contains("provider 原值透传")
                .contains("复用 order:read");

        // 交付物类型 schema 自描述（三个订单出口面同款）：三值语义＋冻结口径＋取件指向
        for (String schema : List.of("AiplatformOrderSummaryResponse",
                "AiplatformOrderDetailResponse", "AiplatformOrderResponse")) {
            JsonNode type = property(schema, "deliverableType");
            assertThat(type.path("description").asText(""))
                    .as("%s.deliverableType 应写明三值语义与冻结口径", schema)
                    .contains("1=设计 2=系统 3=系统＋设计")
                    .contains("下单冻结自项目终点类型")
                    .contains("不随项目变更");
            assertThat(exampleText(type)).as("%s.deliverableType 应附示例", schema).isNotBlank();
        }
        // 清单/详情两承载面另写明取件端点二选一（写回执面不写——回执无取件语义）
        assertThat(property("AiplatformOrderSummaryResponse", "deliverableType")
                .path("description").asText(""))
                .contains("design-package")
                .contains("source-package");
    }

    // ---------- 装载 ----------
    private JsonNode apiDocs() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).as("api-docs 应可匿名拉取：%s", response.getBody())
                .isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode schemas() throws Exception {
        JsonNode schemas = apiDocs().path("components").path("schemas");
        assertThat(schemas.isObject()).as("api-docs 应含 components.schemas").isTrue();
        return schemas;
    }

    /** components.schemas.{schema}.properties.{field}（缺失即契约漂移，直接失败）。 */
    private JsonNode property(String schema, String field) throws Exception {
        JsonNode property = schemas().path(schema).path("properties").path(field);
        assertThat(property.isMissingNode())
                .as("api-docs 应含 %s.%s（缺 schema 视为契约漂移）", schema, field)
                .isFalse();
        return property;
    }

    /** 示例文本（值节点取文本、结构节点取 JSON 串；缺示例 = 空串）。 */
    private static String exampleText(JsonNode property) {
        JsonNode example = property.path("example");
        if (example.isMissingNode() || example.isNull()) {
            return "";
        }
        return example.isValueNode() ? example.asText() : example.toString();
    }
}
