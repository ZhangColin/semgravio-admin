package com.semgravio.admin.aiplatform.infrastructure;

import com.semgravio.admin.aiplatform.application.AiplatformAccountAppService;
import com.semgravio.admin.aiplatform.application.AiplatformAgentConfigAppService;
import com.semgravio.admin.aiplatform.application.AiplatformAgentToolAppService;
import com.semgravio.admin.aiplatform.application.AiplatformCostAppService;
import com.semgravio.admin.aiplatform.application.AiplatformMaterialAppService;
import com.semgravio.admin.aiplatform.application.AiplatformOrderAppService;
import com.semgravio.admin.aiplatform.application.AiplatformPriceEntryAppService;
import com.semgravio.admin.aiplatform.application.AiplatformProjectAppService;
import com.semgravio.admin.aiplatform.application.AiplatformSkillAppService;
import com.semgravio.admin.aiplatform.application.AiplatformWorkspaceAppService;
import com.cartisan.openapi.client.BinaryResponse;
import com.cartisan.openapi.client.OpenApiClient;
import com.cartisan.openapi.client.OpenApiClientException;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * aiplatform wire 契约测试共享脚手架——子类化 {@link OpenApiClient} 仅替换 HTTP 传输（先例
 * {@code PaymentWireTestSupport}）：用对齐 Spring Boot 默认的 {@link ObjectMapper} 反序列化给定的
 * aiplatform 信封 JSON，其余全真实（真实 {@link AiplatformClient} + 真实 Jackson 反序列化 +
 * 真实 AppService 映射）。唯一被替换的是网络传输——对 wire 反序列化契约无关（方法边界 mock
 * 会绕过反序列化路径，是已知反例）。{@code ObjectMapper} 对齐 Spring Boot 默认
 * （{@code FAIL_ON_UNKNOWN_PROPERTIES=false} + {@code JavaTimeModule}）。
 *
 * <p>供 {@code AiplatformAccountClientContractTest} / {@code AiplatformOrderClientContractTest} /
 * {@code AiplatformProjectClientContractTest} / {@code AiplatformWorkspaceClientContractTest} /
 * {@code AiplatformCostClientContractTest} / {@code AiplatformPriceEntryClientContractTest} /
 * {@code AiplatformMaterialClientContractTest} / {@code AiplatformSkillClientContractTest} /
 * {@code AiplatformAgentConfigClientContractTest} / {@code AiplatformAgentToolClientContractTest} 复用；
 * 后续域 {@code *ContractTest} 沿用本脚手架扩展（各域 AppService 重载）。</p>
 *
 * @since 0.1.0
 */
final class AiplatformWireTestSupport {

    private AiplatformWireTestSupport() {
    }

    /**
     * 用给定 aiplatform 成功信封 JSON 构造一个 HTTP 传输被替换的 {@link AiplatformAccountAppService}。
     * 其 {@link AiplatformClient} 的 {@code get} 忽略 url、把 envelopeBody 按 {@code AiplatformClient}
     * 真实传入的 {@link TypeReference} 反序列化——真实反序列化路径完整保留；wire URL 记入 wireUrlSink
     * （断言出站路径）。
     */
    static AiplatformAccountAppService accountAppServiceWithStubTransport(String envelopeBody, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformAccountAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 用给定的下游错误（HTTP 状态 + 错误信封 body）构造一个传输始终抛 {@link OpenApiClientException}
     * 的 {@link AiplatformAccountAppService}——框架 {@code OpenApiClient.validateResponse} 对 ≥400 响应
     * 抛 {@code OpenApiClientException(statusCode, body)}，本 stub 在传输层复刻该行为，
     * 让 {@link AiplatformClient} 的错误信封解析（透传翻译）打到真实路径。
     */
    static AiplatformAccountAppService accountAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformAccountAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 订单域版 {@link #accountAppServiceWithStubTransport}——{@code get}（清单/详情）走真实
     * {@link TypeReference} 反序列化，{@code download}（源码包）返回给定 {@link BinaryResponse}：
     * 二进制无反序列化路径，stub 只需保全字节与响应头。
     */
    static AiplatformOrderAppService orderAppServiceWithStubTransport(
            String envelopeBody, BinaryResponse downloadStub, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public BinaryResponse download(String url) {
                wireUrlSink[0] = url;
                return downloadStub;
            }
        };
        return new AiplatformOrderAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 订单域版 {@link #accountAppServiceWithErrorTransport}——{@code get}/{@code download}/
     * {@code post} 都始终抛 {@link OpenApiClientException}（框架对 ≥400 的行为复刻：download 的
     * 错误信封在 body 里、以 UTF-8 解码后入异常；写路径三操作的错误信封同读口）。
     */
    static AiplatformOrderAppService orderAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public BinaryResponse download(String url) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformOrderAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 订单域<strong>写路径</strong>版 {@link #orderAppServiceWithStubTransport}——报价/取消/
     * 重试归档走 {@code post}（前两者带命令体、重试归档 null body），按真实 {@code TypeReference}
     * 反序列化；post 额外把命令体按<strong>生产 mapper 口径</strong>序列化进 wireBodySink（出站
     * JSON 形状断言——OpenApiClient 序列化在其 sendWithBody 内部，传输替换于 post 方法边界会
     * 绕过它，故在 stub 里用对齐生产的 mapper 补打这一 seam；null body 原样记 null——钉死
     * 重试归档无请求体契约）。{@code get}/{@code download} 沿用读路径版语义（写测试不触达）。
     */
    static AiplatformOrderAppService orderWriteAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        // cartisan-web 生产 ObjectMapper 对 Long/long 注册 ToStringSerializer（JacksonConfiguration
        // serializerByType，与 BigDecimal WRITE_BIGDECIMAL_AS_PLAIN 同一全局配置）——报价命令体的
        // amount（Long 分）出站为 JSON string，断言须对齐生产口径，而非 Spring Boot 默认（JSON 数字）
        ObjectMapper commandBodyMapper = bootDefaultMapper().registerModule(new SimpleModule()
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance));
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public BinaryResponse download(String url) {
                throw new UnsupportedOperationException("写路径契约测试不触达 download");
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : commandBodyMapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformOrderAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 项目域版 {@link #accountAppServiceWithStubTransport}——读口（清单/详情/对话史/PRD/版本
     * 列表/版本详情/文件树/文件内容）全走 {@code get}，按真实 {@link TypeReference} 反序列化
     * （文件包走 download 版重载）。
     */
    static AiplatformProjectAppService projectAppServiceWithStubTransport(String envelopeBody, String[] wireUrlSink) {
        return projectAppServiceWithStubTransport(envelopeBody, null, wireUrlSink);
    }

    /**
     * 项目域文件包版（订单域 {@link #orderAppServiceWithStubTransport} 同款，issue #71）——读口
     * 走真实 {@code TypeReference} 反序列化，{@code download}（文件包）返回给定
     * {@link BinaryResponse}：二进制无反序列化路径，stub 只需保全字节与响应头。
     */
    static AiplatformProjectAppService projectAppServiceWithStubTransport(
            String envelopeBody, BinaryResponse downloadStub, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public BinaryResponse download(String url) {
                wireUrlSink[0] = url;
                return downloadStub;
            }
        };
        return new AiplatformProjectAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 项目域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 与 {@code download}
     * 都始终抛 {@link OpenApiClientException}（框架对 ≥400 的行为复刻：文件包的错误信封在
     * body 里、同读口）。
     */
    static AiplatformProjectAppService projectAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public BinaryResponse download(String url) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformProjectAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 沙箱域版 {@link #accountAppServiceWithStubTransport}——读口（清单/详情）走 {@code get}、
     * 四干预动作（唤醒/休眠/重建/封存）走 {@code post}（无请求体），均按真实 {@link TypeReference}
     * 反序列化；post 的 wire URL 记入 wireUrlSink 后复用同一 sink（读写同域断言各自调用）。
     */
    static AiplatformWorkspaceAppService workspaceAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformWorkspaceAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 沙箱域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 与 {@code post} 都始终抛
     * {@link OpenApiClientException}（框架对 ≥400 的行为复刻：四动作的错误信封同读口）。
     */
    static AiplatformWorkspaceAppService workspaceAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformWorkspaceAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 成本域版 {@link #accountAppServiceWithStubTransport}——四读口（总览/unpriced/项目成本清单/
     * 单项目下钻）全走 {@code get}，按真实 {@link TypeReference} 反序列化。
     */
    static AiplatformCostAppService costAppServiceWithStubTransport(String envelopeBody, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformCostAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 成本域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 始终抛
     * {@link OpenApiClientException}（框架对 ≥400 的行为复刻）。
     */
    static AiplatformCostAppService costAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformCostAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 单价表域版 {@link #accountAppServiceWithStubTransport}——清单走 {@code get}、改价/停用走
     * {@code post}（改价带命令体、停用 null body），均按真实 {@link TypeReference} 反序列化；
     * post 额外把命令体按<strong>生产 mapper 口径</strong>序列化进 wireBodySink（出站 JSON 形状
     * 断言——OpenApiClient 序列化在其 sendWithBody 内部，传输替换于 post 方法边界会绕过它，
     * 故在 stub 里用对齐生产的 mapper 补打这一 seam；null body 原样记 null）。
     */
    static AiplatformPriceEntryAppService priceEntryAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        // cartisan-web 生产 ObjectMapper 启用 WRITE_BIGDECIMAL_AS_PLAIN（JacksonConfiguration：
        // BigDecimal 禁科学计数）——出站命令体序列化断言须对齐生产口径，而非 Spring Boot 默认
        ObjectMapper commandBodyMapper = bootDefaultMapper()
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : commandBodyMapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformPriceEntryAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 单价表域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 与 {@code post} 都始终抛
     * {@link OpenApiClientException}（框架对 ≥400 的行为复刻：改价/停用的错误信封同读口）。
     */
    static AiplatformPriceEntryAppService priceEntryAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformPriceEntryAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 知识素材域版 {@link #accountAppServiceWithStubTransport}——清单/详情走 {@code get}、
     * 停用/启用走 {@code post}（null body）、删除走 {@code delete}（cartisan-boot#33，无 body
     * 参数——DELETE 动词唯一出口），均按真实 {@link TypeReference} 反序列化；post 额外把命令体
     * 序列化进 wireBodySink（null body 原样记 null——钉死两治理动作无请求体契约）。
     */
    static AiplatformMaterialAppService materialAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T delete(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformMaterialAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 知识素材域版 {@link #accountAppServiceWithErrorTransport}——{@code get}/{@code post}/
     * {@code delete} 都始终抛 {@link OpenApiClientException}（框架对 ≥400 的行为复刻：
     * 三治理动作与读口的错误信封同形）。
     */
    static AiplatformMaterialAppService materialAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T delete(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformMaterialAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域版 {@link #accountAppServiceWithStubTransport}（#247 技能线 T1）——清单/详情
     * 两读口全走 {@code get}，按真实 {@code TypeReference} 反序列化。
     */
    static AiplatformSkillAppService skillAppServiceWithStubTransport(String envelopeBody, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 始终抛
     * {@link OpenApiClientException}（框架对 ≥400 的行为复刻：详情 404 SKL_001 与读口同形）。
     */
    static AiplatformSkillAppService skillAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>写路径</strong>版（#248 技能线 T2，issue #81）——安装走 {@code post}
     * （带命令体）、停用/启用走 {@code post}（null body）、卸载走 {@code delete}（无 body 参数
     * ——DELETE 动词唯一出口），均按真实 {@code TypeReference} 反序列化；post 把命令体序列化进
     * wireBodySink（null body 原样记 null——钉死两启停动作无请求体契约）、delete 入口把
     * wireBodySink 记 null（DELETE 无 body——出站动词形状断言的对照面）。
     */
    static AiplatformSkillAppService skillWriteAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T delete(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                wireBodySink[0] = null;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>卸载动词钉死</strong>版（issue #81 验收：DELETE 动词不可替代）——
     * {@code delete} 正常反序列化，{@code get}/{@code post} 直接抛 {@link AssertionError}
     * （Error 不经 {@code AiplatformClient} 的 OpenApiClientException catch，测试即失败）：
     * 卸载若误用 get/post 出站，本 stub 即刻失败钉死动词契约。
     */
    static AiplatformSkillAppService skillDeleteOnlyAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                throw new AssertionError("卸载必须走 DELETE 动词（openApiClient.delete），误用了 GET: " + url);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                throw new AssertionError("卸载必须走 DELETE 动词（openApiClient.delete），误用了 POST: " + url);
            }

            @Override
            public <T> T delete(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域写路径版 {@link #skillAppServiceWithErrorTransport}——{@code get}/{@code post}/
     * {@code delete} 都始终抛 {@code OpenApiClientException}（框架对 ≥400 的行为复刻：安装
     * fail-fast 失败族 SKL_002–SKL_009 与读口错误信封同形）。
     */
    static AiplatformSkillAppService skillWriteAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T delete(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>槽位指派</strong>版（#249 技能线 T3，issue #82）——读面走 {@code get}、
     * 整包替换写走 {@code put}（带命令体——{@code OpenApiClient.put} 与 {@code post} 同走
     * sendWithBody 序列化路径），均按真实 {@code TypeReference} 反序列化；put 把命令体序列化进
     * wireBodySink（出站 JSON 形状断言——序列化在 OpenApiClient 内部、方法边界替换传输会绕过
     * 它，故在 stub 里用对齐 mapper 口径补打这一 seam；skillIds 为 List&lt;String&gt;，无
     * Long/BigDecimal 特殊口径，boot 默认 mapper 即生产口径）。
     */
    static AiplatformSkillAppService skillAssignAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域槽位指派版 {@link #skillAppServiceWithErrorTransport}——{@code get} 与
     * {@code put} 都始终抛 {@code OpenApiClientException}（框架对 ≥400 的行为复刻：未知槽位
     * SKL_010、内置柄不可指派 SKL_011、操作者缺头 SKL_009 与读口错误信封同形）。
     */
    static AiplatformSkillAppService skillAssignAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>显式更新＋留痕读面</strong>版（#250 技能线 T4，issue #83）——更新写
     * 走 {@code post}（带命令体 {@code {sourcePackage}}）、留痕读面走 {@code get}，均按真实
     * {@code TypeReference} 反序列化；post 把命令体序列化进 wireBodySink（出站 JSON 形状断言
     * ——sourcePackage 为 String 无特殊口径，boot 默认 mapper 即生产口径），get 不触达
     * wireBodySink（留痕读面查询参数在 URL 上）。
     */
    static AiplatformSkillAppService skillUpdateAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域显式更新版 {@link #skillAppServiceWithErrorTransport}——{@code post}（更新写）
     * 与 {@code get}（留痕读面）都始终抛 {@code OpenApiClientException}（框架对 ≥400 的行为
     * 复刻：更新失败族 SKL_004/005/007/009/012/013/014 与读口错误信封同形）。
     */
    static AiplatformSkillAppService skillUpdateAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>指派双头预检</strong>版（#255，技能域第 16 端点，issue #90）——预检
     * 走 {@code post}（带命令体 {@code {skillIds}}——复用指派整包替换命令，与 PUT 同形），
     * 按真实 {@code TypeReference} 反序列化；post 把命令体序列化进 wireBodySink（出站 JSON
     * 形状断言——skillIds 为 List&lt;String&gt; 无特殊口径，boot 默认 mapper 即生产口径）。
     * 仅 {@code post} 一口触达（预检无读面副作用）。
     */
    static AiplatformSkillAppService skillPrecheckAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域指派双头预检版 {@link #skillAppServiceWithErrorTransport}——{@code post}
     * 始终抛 {@link OpenApiClientException}（框架对 ≥400 的行为复刻：预检错误族
     * SKL_001/010/011 与读口错误信封同形）。
     */
    static AiplatformSkillAppService skillPrecheckAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域<strong>草稿审核面</strong>版（#258 自产线，issue #92）——在途清单/详情两读口
     * 走 {@code get}、晋升走 {@code post}（<strong>无命令体</strong>——null body 发空 body，
     * 同启停先例）、拒绝走 {@code post}（带命令体 {@code {reason}}），均按真实
     * {@code TypeReference} 反序列化；post 把命令体序列化进 wireBodySink（null body 原样记
     * null——钉死晋升无请求体契约；reason 为 String 无特殊口径，boot 默认 mapper 即生产口径）。
     */
    static AiplatformSkillAppService skillDraftAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 技能管理域草稿审核面版 {@link #skillAppServiceWithErrorTransport}——{@code get} 与
     * {@code post} 都始终抛 {@code OpenApiClientException}（框架对 ≥400 的行为复刻：草稿
     * 错误族 SKL_009/015/016/017/018 与读口错误信封同形）。
     */
    static AiplatformSkillAppService skillDraftAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T post(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformSkillAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 智能体配置域版（#251 技能线 T5，issue #84）——读面/留痕读面走 {@code get}、全量写走
     * {@code put}（带命令体 {@code {systemPrompt, modelId, webSearchEnabled, fetchUrlEnabled}}——
     * {@code OpenApiClient.put} 与 {@code post} 同走 sendWithBody 序列化路径），均按真实
     * {@code TypeReference} 反序列化；put 把命令体序列化进 wireBodySink（出站 JSON 形状断言
     * ——四字段无 Long/BigDecimal 特殊口径，boot 默认 mapper 即生产口径；Boolean 装箱开关
     * null 出站在场——provider 缺省 true 语义的全局 Jackson 含 null 证据）。
     */
    static AiplatformAgentConfigAppService agentConfigAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformAgentConfigAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 智能体配置域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 与
     * {@code put} 都始终抛 {@code OpenApiClientException}（框架对 ≥400 的行为复刻：
     * 未知键 PRJ_033、操作者缺头 PRJ_034 与读口错误信封同形）。
     */
    static AiplatformAgentConfigAppService agentConfigAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformAgentConfigAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 工具面域版（#252 技能线 T6，issue #85）——清单读面走 {@code get}、增强工具开关走
     * {@code put}（带命令体 {@code {enabled}}——{@code OpenApiClient.put} 与 {@code post}
     * 同走 sendWithBody 序列化路径），均按真实 {@code TypeReference} 反序列化；put 把命令体
     * 序列化进 wireBodySink（出站 JSON 形状断言——enabled 为 Boolean 装箱无 Long/BigDecimal
     * 特殊口径，boot 默认 mapper 即生产口径；null 出站在场——provider 缺目标态 PRJ_037 判定
     * 的全局 Jackson 含 null 证据）。
     */
    static AiplatformAgentToolAppService agentToolAppServiceWithStubTransport(
            String envelopeBody, String[] wireUrlSink, String[] wireBodySink) {
        ObjectMapper mapper = bootDefaultMapper();
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, mapper) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                try {
                    wireBodySink[0] = body == null ? null : mapper.writeValueAsString(body);
                    return mapper.readValue(envelopeBody, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return new AiplatformAgentToolAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    /**
     * 工具面域版 {@link #accountAppServiceWithErrorTransport}——{@code get} 与
     * {@code put} 都始终抛 {@link OpenApiClientException}（框架对 ≥400 的行为复刻：
     * 未知名 PRJ_035、骨架锁死 PRJ_036、缺目标态 PRJ_037、操作者缺头 PRJ_034 与读口
     * 错误信封同形）。
     */
    static AiplatformAgentToolAppService agentToolAppServiceWithErrorTransport(
            int statusCode, String errorBody, String[] wireUrlSink) {
        OpenApiClient stubTransport = new OpenApiClient(new CartisanOpenapiProperties(), null, bootDefaultMapper()) {
            @Override
            public <T> T get(String url, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }

            @Override
            public <T> T put(String url, Object body, TypeReference<T> typeReference) {
                wireUrlSink[0] = url;
                throw new OpenApiClientException(statusCode, errorBody);
            }
        };
        return new AiplatformAgentToolAppService(new AiplatformClient(stubTransport, "http://stub-aiplatform"));
    }

    private static ObjectMapper bootDefaultMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);   // Spring Boot 默认
    }
}
