package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformAgentConfigUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformAgentConfigTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigTraceWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigUpdateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformAgentConfigWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 智能体配置域 BFF 应用服务——技能线 T5 读写面（issue #84，aiplatform #251）：
 * 两座智能体（main=主智能体 / executor=run 执行体）的 systemPrompt/模型档位运营配置＋
 * 变更留痕读面。智能体运营配置归 aiplatform business.project 上下文——admin 不持业务逻辑、
 * 不持配置数据（与技能库面零代码交集，独立 controller 面）。
 *
 * <p>admin 作为 BFF 忠实透传（spec #79）：寻址键 {@code agentKey} 为字符串路径参数——
 * 透传不解析不预校验，未知键由 provider 判 404 PRJ_033（数字业务码 4033）。PUT 全量语义
 * （请求体即覆盖态终态）；覆盖清空（null/缺省/纯空白回落枚举默认）、同值重写幂等不落痕、
 * 回滚＝留痕旧值快照写回等语义全在 provider 侧，admin 零判断。写口操作者经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（配置变更必留痕，
 * 缺头由 provider 域面守卫 PRJ_034＝4034 拦，admin 不拦）；错误经 {@link AiplatformClient}
 * 统一翻译为 {@link AiplatformUpstreamException} 原样透传，本层不 try/catch。</p>
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
     * 智能体配置读面（透传 aiplatform）——生效值（库覆盖或枚举默认——装配实取值）＋覆盖标记
     * ＋枚举默认预览（清空覆盖前可看落点）＋增强工具开关存储态＋最近写者。无覆盖行＝全回落
     * （覆盖标记 false、写者 null）。{@code agentKey} 字符串路径参数原样出站（main/executor
     * 两把稳定键，spec #79 不解析不预校验），未知键由 provider 判 404 PRJ_033（数字业务码
     * 4033）。
     */
    public AiplatformAgentConfigResponse getConfig(String agentKey) {
        return toConfig(aiplatformClient.getAgentConfig(agentKey));
    }

    /**
     * 智能体配置全量写（透传 aiplatform）——PUT 全量语义：请求体即该智能体覆盖态终态
     * （systemPrompt/modelId 全文替换，null/缺省/纯空白＝清空覆盖回落枚举默认；工具开关缺省
     * true＝开）。变更留痕 append-only（值面有动必落痕——前后全量值快照＋操作者）；同值重写
     * ＝幂等回执不落痕；回滚＝把留痕旧值快照写回（一次新变更、留新痕，不做版本树）。操作者
     * 经框架透传头落痕（缺头由 provider 域面守卫 PRJ_034 拦，admin 不拦）。回执与 GET 同形
     * ——变更后下一轮命令构建自然取新值、进行中 run 不定格。
     */
    public AiplatformAgentConfigResponse updateConfig(String agentKey, AiplatformAgentConfigUpdateCommand command) {
        return toConfig(aiplatformClient.updateAgentConfig(agentKey,
                new AiplatformAgentConfigUpdateWireRequest(
                        command.systemPrompt(), command.modelId(),
                        command.webSearchEnabled(), command.fetchUrlEnabled())));
    }

    /**
     * 配置变更留痕读面（透传 aiplatform）——该智能体全部配置变更留痕（old&#42;/new&#42; 四件对＋
     * 操作者＋时刻），按时间倒序（最近先）。append-only：留痕不随任何动作删除；旧值快照即
     * 回滚写回依据（回滚＝把某痕旧值快照 PUT 回写）。寻址同 GET（未知键由 provider 判 404
     * PRJ_033＝4033）。
     */
    public List<AiplatformAgentConfigTraceResponse> listTraces(String agentKey) {
        return aiplatformClient.listAgentConfigTraces(agentKey).stream()
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
                wire.operatorId(), wire.operatorName(), wire.updatedAt());
    }

    private static AiplatformAgentConfigTraceResponse toTrace(AiplatformAgentConfigTraceWireResponse wire) {
        return new AiplatformAgentConfigTraceResponse(
                wire.id(),
                wire.oldSystemPrompt(), wire.oldModelId(),
                wire.oldWebSearchEnabled(), wire.oldFetchUrlEnabled(),
                wire.newSystemPrompt(), wire.newModelId(),
                wire.newWebSearchEnabled(), wire.newFetchUrlEnabled(),
                wire.operatorId(), wire.operatorName(), wire.operatedAt());
    }
}
