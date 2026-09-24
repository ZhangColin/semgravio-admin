package com.aieducenter.admin.aiplatform.application;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformAgentToolToggleCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformAgentToolResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformAgentToolSlotResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolSlotWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolToggleWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformAgentToolWireResponse;
import com.aieducenter.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 工具面域 BFF 应用服务——技能线 T6 清单读面＋增强工具开关（issue #85，
 * aiplatform #252）：按职能槽位列当前挂载工具清单（排障/审计面）＋增强工具窄幅开关
 * （关即退出装配面、开即回归）。工具面归 aiplatform business.project 上下文——admin
 * 不持业务逻辑、不持工具资产（与技能库/智能体配置面零代码交集，独立 controller 面）。
 *
 * <p>admin 作为 BFF 忠实透传（spec #79）：寻址键 {@code toolName} 为字符串路径参数
 * ——透传不解析不预校验，未知工具名由 provider 判 404 PRJ_035（数字业务码 4035，清单
 * 读面即全集）；骨架/harness 内建不开放开关由 provider 判 403 PRJ_036（数字业务码
 * 4036——ADR-0021 编排权不下放配置）；enabled 必填由 provider 判 400 PRJ_037（数字
 * 业务码 4037）。开关与智能体配置共表共留痕（值面有动才落痕、幂等回执零写入、回滚＝
 * 旧值写回）全在 provider 侧，admin 零判断。写口操作者经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传落痕（开关写必留痕，缺头由 provider 域面守卫
 * PRJ_034＝4034 拦，admin 不拦）；错误经 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException} 原样透传，本层不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformAgentToolAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformAgentToolAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 工具面清单读面（透传 aiplatform）——按职能槽位（main=主智能体 / executor=run
     * 执行体 / subagent=子智能体，与技能槽位同键）列当前挂载工具。三呈现源：平台资产
     * （骨架=编排链路＋项目事实只读件；增强=联网搜索/网页抓取，enabled 按运营配置生效态
     * ——false＝在册但退出装配面）、harness 内建编码工具（executor 槽，不可开关；main
     * 只读面结构性无）、子智能体声明工具面。呈现序 provider 定死（平台资产在前、harness
     * 内建在后），admin 原样保序透传。
     */
    public List<AiplatformAgentToolSlotResponse> listToolSlots() {
        return aiplatformClient.listAgentTools().stream()
                .map(AiplatformAgentToolAppService::toSlot)
                .toList();
    }

    /**
     * 增强工具开关（透传 aiplatform）——按工具注册名窄幅开关，仅增强工具
     * （web_search=联网搜索 / fetch_url=网页抓取）接受：enabled=false 该工具退出对应
     * 槽位装配面（模型不可见），true 回归——动态生效：下一轮命令构建即新装配（工厂缓存
     * 键含工具面规格，进行中 run 不定格）。变更走配置留痕（与智能体配置同机制：值面有动
     * 才落痕、幂等回执零写入、回滚＝旧值写回）。{@code toolName} 字符串路径参数原样出站
     * ——admin 不预校验，未知名 404 PRJ_035（4035）、骨架/harness 内建 403 PRJ_036
     * （4036）、缺目标态 400 PRJ_037（4037）均 provider 裁决。回执＝开关后工具行（与
     * 清单嵌套行同形）。
     */
    public AiplatformAgentToolResponse toggleTool(String toolName, AiplatformAgentToolToggleCommand command) {
        // 命令体可选绑定（provider 形态）：缺 body＝null 透传（provider 判 400 PRJ_037），admin 不前置拦截
        return toTool(aiplatformClient.toggleAgentTool(toolName,
                new AiplatformAgentToolToggleWireRequest(command == null ? null : command.enabled())));
    }

    private static AiplatformAgentToolSlotResponse toSlot(AiplatformAgentToolSlotWireResponse wire) {
        return new AiplatformAgentToolSlotResponse(
                wire.slot(), wire.slotName(),
                wire.tools().stream().map(AiplatformAgentToolAppService::toTool).toList());
    }

    private static AiplatformAgentToolResponse toTool(AiplatformAgentToolWireResponse wire) {
        return new AiplatformAgentToolResponse(
                wire.name(), wire.kind(), wire.enabled(), wire.description());
    }
}
