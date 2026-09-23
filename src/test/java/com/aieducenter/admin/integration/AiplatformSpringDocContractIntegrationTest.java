package com.aieducenter.admin.integration;

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
 *       自描述；</li>
 *   <li>显式更新＋更新留痕读面两端点 @Operation 描述镜像（issue #83 镜像 aiplatform #250
 *       技能线 T4）：更新写面（重拉快照＋版本留痕、永不自动跟新、同名行原地翻新柄稳定、
 *       远端未前进＝幂等回执不落痕不写行）与留痕读面（时间倒序、append-only 卸载不删留痕）
 *       的 summary＋description 逐字镜像 provider——SKL_013＝7013（移除项有指派在身整体拒）、
 *       SKL_012＝7012（来源包未安装）、SKL_014＝7014（来源包标识不能为空）等关键语义须在
 *       api-docs 在场；北向 schema 的更新命令体 sourcePackage、更新回执 fromVersion/
 *       removedSkillNames/skills 与留痕行 operatedAt 同步自描述。</li>
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
                .contains("来源 code（1=内置 2=安装）")
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
