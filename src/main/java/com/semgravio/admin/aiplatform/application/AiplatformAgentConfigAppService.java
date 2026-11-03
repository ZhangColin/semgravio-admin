package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformEdgeModelUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformEdgeModelConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigTraceWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigUpdateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformEdgeModelConfigWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformEdgeModelUpdateWireRequest;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 智能体配置域 BFF 应用服务——技能线 T5 读写面（issue #84，aiplatform #251）＋
 * 边缘调用点档位三面（issue #112 B3，aiplatform #366 边缘四点收编）：三座智能体（main=
 * 主智能体 / executor=run 执行体 / designer=设计执行体）的 systemPrompt/模型档位运营配置
 * ＋路由强档件（B3 忠实跟随）＋变更留痕读面；边缘四点（naming=项目取名 / compaction=
 * 上下文压缩 / classification=入口分类 / precheck=技能预检——一次性轻调用，值面仅档位键
 * 无 prompt）的档位读/写/留痕。智能体运营配置归 aiplatform business.project 上下文——
 * admin 不持业务逻辑、不持配置数据（与技能库面零代码交集，独立 controller 面）。
 *
 * <p>admin 作为 BFF 忠实透传（spec #79）：寻址键 {@code agentKey}/{@code edgeKey} 为字符串
 * 路径参数——透传不解析不预校验，未知键由 provider 判 404 PRJ_033（数字业务码 4033——三座
 * 面）／404 PRJ_064（4064——边缘面只有四点）。modelId/routingStrongModelId 语义＝模型档位键
 * （ADR-0031 语义键，V44 存量已刷档）；写口档位键即时校验（aiplatform #367：非空须在模型
 * 档位面有行且启用——缺行 400 PRJ_074＝4074、停用 400 PRJ_075＝4075）归 provider 裁决。
 * PUT 全量语义（请求体即覆盖态终态）；覆盖清空（null/缺省/纯空白回落枚举/代码默认）、同值
 * 重写幂等不落痕、回滚＝留痕旧值快照写回等语义全在 provider 侧，admin 零判断。写口操作者
 * 经框架 {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（配置变更必
 * 留痕，缺头由 provider 域面守卫 PRJ_034＝4034 拦，admin 不拦）；错误经
 * {@link AiplatformClient} 统一翻译为 {@link AiplatformUpstreamException} 原样透传，本层
 * 不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformAgentConfigAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformAgentConfigAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 智能体配置读面（透传 aiplatform）——生效值（库覆盖或枚举默认——装配实取值；modelId
     * 语义＝档位键）＋覆盖标记＋枚举默认预览（清空覆盖前可看落点）＋增强工具开关存储态＋
     * 路由强档三件（B3 忠实跟随——主档/强档同范式）＋最近写者。无覆盖行＝全回落（覆盖标记
     * false、写者 null）。{@code agentKey} 字符串路径参数原样出站（main/executor/designer
     * 三把稳定键，spec #79 不解析不预校验），未知键由 provider 判 404 PRJ_033（数字业务码
     * 4033）。
     */
    public AiplatformAgentConfigResponse getConfig(String agentKey) {
        return toConfig(aiplatformClient.getAgentConfig(agentKey));
    }

    /**
     * 智能体配置全量写（透传 aiplatform）——PUT 全量语义：请求体即该智能体覆盖态终态
     * （systemPrompt/modelId/routingStrongModelId 全文替换，null/缺省/纯空白＝清空覆盖回落
     * 枚举/代码默认；工具开关缺省 true＝开）。档位键即时校验（缺行 400 PRJ_074、停用 400
     * PRJ_075——错值写不进去；null 不校验）归 provider 写用例裁决。变更留痕 append-only
     * （值面有动必落痕——前后全量值快照＋操作者）；同值重写＝幂等回执不落痕；回滚＝把留痕
     * 旧值快照写回（一次新变更、留新痕，不做版本树）。操作者经框架透传头落痕（缺头由
     * provider 域面守卫 PRJ_034 拦，admin 不拦）。回执与 GET 同形——变更后下一轮命令构建
     * 自然取新值、进行中 run 不定格。
     */
    public AiplatformAgentConfigResponse updateConfig(String agentKey, AiplatformAgentConfigUpdateCommand command) {
        return toConfig(aiplatformClient.updateAgentConfig(agentKey,
                new AiplatformAgentConfigUpdateWireRequest(
                        command.systemPrompt(), command.modelId(),
                        command.webSearchEnabled(), command.fetchUrlEnabled(),
                        command.routingStrongModelId())));
    }

    /**
     * 配置变更留痕读面（透传 aiplatform）——该智能体全部配置变更留痕（old&#42;/new&#42; 对＋
     * 操作者＋时刻；prompt/档位/路由强档三件可空——强档件是读面可改就必须可回滚的必然配套），
     * 按时间倒序（最近先）。append-only：留痕不随任何动作删除；旧值快照即回滚写回依据
     * （回滚＝把某痕旧值快照 PUT 回写）。寻址同 GET（未知键由 provider 判 404 PRJ_033＝4033）。
     */
    public List<AiplatformAgentConfigTraceResponse> listTraces(String agentKey) {
        return aiplatformClient.listAgentConfigTraces(agentKey).stream()
                .map(AiplatformAgentConfigAppService::toTrace)
                .toList();
    }

    /**
     * 边缘调用点档位读面（透传 aiplatform，#366 边缘四点收编——issue #112 B3）——边缘调用点
     * （一次性轻调用：naming=项目取名 / compaction=上下文压缩 / classification=入口分类 /
     * precheck=技能预检）的模型档位生效值（库覆盖值或代码缺省 flash——轻调用不烧重档）＋
     * 覆盖标记＋缺省预览＋最近写者。值面仅档位键——无 prompt/工具开关/路由件（字段名对齐
     * 三座面）。{@code edgeKey} 字符串路径参数原样出站（admin 不解析不预校验），未知键由
     * provider 判 404 PRJ_064（数字业务码 4064）。改档下一轮命令构建即生效（装配动态查库，
     * 进行中 run 不定格）。
     */
    public AiplatformEdgeModelConfigResponse getEdgeModelConfig(String edgeKey) {
        return toEdgeConfig(aiplatformClient.getEdgeModelConfig(edgeKey));
    }

    /**
     * 边缘调用点档位写（透传 aiplatform，#366——issue #112 B3 写口）——PUT 全量语义、值面
     * 仅模型档位键：请求体 {@code {modelId}} 即该边缘点覆盖态终态（null/缺省/纯空白＝清空
     * 覆盖回落代码缺省 flash）。档位键即时校验与三座同口径（#367：非空须在模型档位面有行
     * 且启用——缺行 400 PRJ_074、停用 400 PRJ_075，错值写不进去；null 不校验），归 provider
     * 写用例裁决。留痕与三座全量写同机制（值面有动必落痕、同值幂等重写不落痕、回滚＝旧值
     * 快照写回留新痕）；操作者经框架透传头落痕（缺头由 provider 域面守卫 PRJ_034 拦，admin
     * 不拦）。回执与 GET 同形。
     */
    public AiplatformEdgeModelConfigResponse updateEdgeModel(
            String edgeKey, AiplatformEdgeModelUpdateCommand command) {
        return toEdgeConfig(aiplatformClient.updateEdgeModel(edgeKey,
                new AiplatformEdgeModelUpdateWireRequest(command.modelId())));
    }

    /**
     * 边缘调用点档位留痕读面（透传 aiplatform，#366——issue #112 B3 读口）——该边缘点全部
     * 配置变更留痕（快照列与三座同形——边缘行档位列之外快照件恒缺省态：prompt null、开关
     * true、路由件缺省态），按时间倒序（最近先）。回滚＝把某痕旧值快照的 modelId PUT 回写
     * （即一次新变更、留新痕，不做版本树）。寻址同读面（未知键由 provider 判 404
     * PRJ_064＝4064）。
     */
    public List<AiplatformAgentConfigTraceResponse> listEdgeModelTraces(String edgeKey) {
        return aiplatformClient.listEdgeModelTraces(edgeKey).stream()
                .map(AiplatformAgentConfigAppService::toTrace)
                .toList();
    }

    private static AiplatformAgentConfigResponse toConfig(AiplatformAgentConfigWireResponse wire) {
        return new AiplatformAgentConfigResponse(
                wire.agentKey(), wire.agentName(),
                wire.systemPrompt(), wire.modelId(),
                wire.systemPromptOverridden(), wire.modelIdOverridden(),
                wire.defaultSystemPrompt(), wire.defaultModelId(),
                wire.webSearchEnabled(), wire.fetchUrlEnabled(),
                wire.routingStrongModelId(), wire.routingStrongModelIdOverridden(),
                wire.defaultRoutingStrongModelId(),
                wire.operatorId(), wire.operatorName(), wire.updatedAt());
    }

    private static AiplatformAgentConfigTraceResponse toTrace(AiplatformAgentConfigTraceWireResponse wire) {
        return new AiplatformAgentConfigTraceResponse(
                wire.id(),
                wire.oldSystemPrompt(), wire.oldModelId(),
                wire.oldWebSearchEnabled(), wire.oldFetchUrlEnabled(),
                wire.oldRoutingStrongModelId(),
                wire.newSystemPrompt(), wire.newModelId(),
                wire.newWebSearchEnabled(), wire.newFetchUrlEnabled(),
                wire.newRoutingStrongModelId(),
                wire.operatorId(), wire.operatorName(), wire.operatedAt());
    }

    private static AiplatformEdgeModelConfigResponse toEdgeConfig(AiplatformEdgeModelConfigWireResponse wire) {
        return new AiplatformEdgeModelConfigResponse(
                wire.agentKey(), wire.agentName(),
                wire.modelId(), wire.modelIdOverridden(), wire.defaultModelId(),
                wire.operatorId(), wire.operatorName(), wire.updatedAt());
    }
}
