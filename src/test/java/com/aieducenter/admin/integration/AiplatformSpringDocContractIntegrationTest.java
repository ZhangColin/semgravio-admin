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
 *       （provider #160 定稿口径，BFF 忠实透传不做类型统一）。</li>
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

    // ---------- 装载 ----------

    private JsonNode schemas() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).as("api-docs 应可匿名拉取：%s", response.getBody())
                .isEqualTo(HttpStatus.OK);
        JsonNode schemas = objectMapper.readTree(response.getBody())
                .path("components").path("schemas");
        assertThat(schemas.isObject()).as("api-docs 应含 components.schemas：%s", response.getBody()).isTrue();
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
