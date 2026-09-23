package com.aieducenter.admin.aiplatform.application;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillAssignCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillInstallCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillUpdateCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateTraceResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSlotAssignmentResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillAssignWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillInstallWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillSummaryWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateTraceWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireRequest;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillUpdateWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSlotAssignmentWireResponse;
import com.aieducenter.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 技能管理域 BFF 应用服务——T1 读面（清单/详情，issue #80）＋T2 写面（安装/停用⇄
 * 启用/卸载，issue #81）＋T3 槽位指派读写（issue #82）＋T4 显式更新＋更新留痕读面（issue #83），
 * aiplatform 技能线 #247/#248/#249/#250。技能库归 aiplatform base.skills 上下文——admin 不持业务
 * 逻辑、不持技能数据。
 *
 * <p>admin 作为 BFF 忠实透传（spec #79）：<strong>零查询参数</strong>（清单不分页不过滤——
 * 技能库是有界目录，排序服务端定死：内置在前名称序、安装在后按来源包＋名称序，admin 不造
 * 分页）；寻址柄 {@code id} 为 opaque 串两形制（内置 {@code builtin:<技能名>}／安装 TSID
 * 十进制串）——字符串透传不解析不预校验，畸形/未寻址柄由 provider 判 404 SKL_001（数字业务码
 * 7001）。frontmatter 为自由键值对象逐字透传（不做形状断言）。写口操作者经框架
 * {@code RequestContext}→{@code X-User-Id/X-User-Name} 自动透传落痕（缺头由 provider 域面
 * 守卫 SKL_009 拦，admin 不拦）；错误经 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException} 原样透传，本层不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformSkillAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformSkillAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 技能清单（透传 aiplatform）——内置（classpath 合成）与安装技能同权呈现，空库时仍呈现
     * 内置技能；不分页不过滤（有界目录，spec #79 同形不造分页）。
     */
    public List<AiplatformSkillSummaryResponse> list() {
        return aiplatformClient.listSkills().stream()
                .map(AiplatformSkillAppService::toSummary)
                .toList();
    }

    /**
     * 技能详情（透传 aiplatform）——清单行同形元数据＋frontmatter 全量（解析态键值）＋正文
     * 全文（frontmatter 剥离后），所见即运行时注入面（安装审核承载面）。id 取清单行原值
     * （opaque 串两形制）原样出站，不解析不预校验。
     */
    public AiplatformSkillDetailResponse getDetail(String id) {
        return toDetail(aiplatformClient.getSkill(id));
    }

    /**
     * 安装技能包（透传 aiplatform）——git 仓库地址装时快照固化（版本＝装时 HEAD commit），
     * excludeDirs 任一段命中即整支不入库（v1 手输目录段，provider 无仓库结构预览端点——
     * spec #79）。回执＝本次装入的条目清单（与清单行共形）。失败族（地址空 SKL_002、同源
     * 重复安装 SKL_003、克隆失败 SKL_004、未解析到技能 SKL_005、SKILL.md 不合格 SKL_006、
     * 包内重名 SKL_007、操作者缺头 SKL_009）以 provider 数字业务码透传，admin 不拦截
     * 不翻译。操作者经框架透传头落痕（装者）。
     */
    public List<AiplatformSkillSummaryResponse> install(AiplatformSkillInstallCommand command) {
        return aiplatformClient.installSkills(
                        new AiplatformSkillInstallWireRequest(command.repoUrl(), command.excludeDirs()))
                .stream()
                .map(AiplatformSkillAppService::toSummary)
                .toList();
    }

    /**
     * 停用技能（透传 aiplatform）——可逆开关：退出装配候选、不丢库行；重复停用幂等（操作者
     * 留最近一次）。回执＝翻转后清单行。id 取清单行原值原样出站（状态迁移仅对安装库行有
     * 意义——builtin: 柄/畸形柄由 provider 判 404 SKL_001，admin 不预检）。
     */
    public AiplatformSkillSummaryResponse disable(String id) {
        return toSummary(aiplatformClient.disableSkill(id));
    }

    /**
     * 启用技能（透传 aiplatform）——停用的可逆侧：恢复参与装配合成；重复启用幂等。回执＝
     * 翻转后清单行。寻址与 404 语义同 {@link #disable(String)}（裁决归 provider）。
     */
    public AiplatformSkillSummaryResponse enable(String id) {
        return toSummary(aiplatformClient.enableSkill(id));
    }

    /**
     * 卸载技能（透传 aiplatform）——库行删除即彻底出库（不可逆）；有指派在身被 409 SKL_008
     * 拒绝（先解绑再卸），回执＝删除前终态清单行。内置技能不可卸（builtin: 柄 404 SKL_001
     * 同语义）。无行可留不留痕（admin 侧自有操作日志）。
     */
    public AiplatformSkillSummaryResponse uninstall(String id) {
        return toSummary(aiplatformClient.uninstallSkill(id));
    }

    /**
     * 查询槽位指派读面（透传 aiplatform，技能线 T3 读口）——该职能槽位当前指派的技能清单
     * （与清单行共形、含停用行——启停是可逆开关指派关系随行保留，运营可见「指派了但已停用」
     * 实态；停用行不参与装配合成）。{@code slot} 为字符串路径参数原样出站（main/executor/
     * subagent 三把稳定键，spec #79 不解析不预校验），未知槽位由 provider 判 404 SKL_010
     * （数字业务码 7010）。
     */
    public AiplatformSlotAssignmentResponse getSlotAssignments(String slot) {
        return toSlotAssignment(aiplatformClient.getSlotAssignments(slot));
    }

    /**
     * 槽位指派整包替换（透传 aiplatform，技能线 T3 写口）——PUT 全量语义：{@code skillIds}
     * 即该槽位终态（未列入即解绑、空清单＝清空），支持按来源包勾满后送全量。指派目标只收
     * 安装库行 TSID 柄（内置柄 SKL_011、未寻址/畸形 TSID SKL_001 均由 provider 裁决，admin
     * 不预检）；操作者经框架透传头落痕（整包替换留最近动作者，缺头由 provider 域面守卫
     * SKL_009 拦，admin 不拦）。回执＝替换后读面（与 GET 同形）——变更后智能体下一轮自然
     * 生效、进行中 run 不定格。
     */
    public AiplatformSlotAssignmentResponse assignSlot(String slot, AiplatformSkillAssignCommand command) {
        return toSlotAssignment(aiplatformClient.assignSlot(
                slot, new AiplatformSkillAssignWireRequest(command.skillIds())));
    }

    /**
     * 显式更新技能包（透传 aiplatform，技能线 T4 写口）——按来源包整体重拉快照入库（ADR-0021：
     * 更新永远显式点，平台永不自动跟新远端）。回执＝from/to 版本＋被移除技能名＋更新后该包
     * 终态行集（清单行同形；同名行原地翻新保留 id/状态/指派——TSID 柄稳定）。远端未前进＝
     * 幂等回执（from==to、removedSkillNames 空、不落痕不写行）。失败族（来源包空 SKL_014、
     * 未安装 SKL_012、克隆失败 SKL_004、远端未解析到技能 SKL_005、包内重名 SKL_007、移除项
     * 有指派在身整体拒 SKL_013、操作者缺头 SKL_009）以 provider 数字业务码透传，admin 不拦截
     * 不翻译；{@code sourcePackage} 取清单行原值回传（服务端再规范化，同源身份单源）——admin
     * 不自拼原始 URL、不在命令层重复校验。
     */
    public AiplatformSkillUpdateResponse update(AiplatformSkillUpdateCommand command) {
        return toUpdate(aiplatformClient.updateSkills(
                new AiplatformSkillUpdateWireRequest(command.sourcePackage())));
    }

    /**
     * 更新留痕读面（透传 aiplatform，技能线 T4 读口）——该来源包全部显式更新留痕（from→to
     * 版本＋操作者＋时刻），按时间倒序（最近先）。append-only：卸载不删留痕（历史事实不随
     * 库行消失）。{@code sourcePackage} 保持 provider 形态——可选绑定（spec #79：admin 不在
     * 绑定层前置拦截），空/缺由 provider 手工校验出 400 SKL_014（数字业务码 7014）原样透传。
     */
    public List<AiplatformSkillUpdateTraceResponse> updateTraces(String sourcePackage) {
        return aiplatformClient.listSkillUpdateTraces(sourcePackage).stream()
                .map(AiplatformSkillAppService::toUpdateTrace)
                .toList();
    }

    private static AiplatformSkillSummaryResponse toSummary(AiplatformSkillSummaryWireResponse wire) {
        return new AiplatformSkillSummaryResponse(
                wire.id(), wire.name(), wire.description(),
                wire.source(), wire.sourceName(), wire.sourcePackage(), wire.version(),
                wire.status(), wire.statusName(),
                wire.operatorId(), wire.operatorName(), wire.updateAvailable());
    }

    private static AiplatformSkillDetailResponse toDetail(AiplatformSkillDetailWireResponse wire) {
        return new AiplatformSkillDetailResponse(
                wire.id(), wire.name(), wire.description(),
                wire.source(), wire.sourceName(), wire.sourcePackage(), wire.version(),
                wire.status(), wire.statusName(),
                wire.frontmatter(), wire.content(),
                wire.operatorId(), wire.operatorName(), wire.updateAvailable());
    }

    private static AiplatformSlotAssignmentResponse toSlotAssignment(AiplatformSlotAssignmentWireResponse wire) {
        return new AiplatformSlotAssignmentResponse(
                wire.slot(),
                wire.skills().stream().map(AiplatformSkillAppService::toSummary).toList());
    }

    private static AiplatformSkillUpdateResponse toUpdate(AiplatformSkillUpdateWireResponse wire) {
        return new AiplatformSkillUpdateResponse(
                wire.sourcePackage(), wire.fromVersion(), wire.toVersion(),
                wire.removedSkillNames(), wire.operatorId(), wire.operatorName(),
                wire.skills().stream().map(AiplatformSkillAppService::toSummary).toList());
    }

    private static AiplatformSkillUpdateTraceResponse toUpdateTrace(AiplatformSkillUpdateTraceWireResponse wire) {
        return new AiplatformSkillUpdateTraceResponse(
                wire.sourcePackage(), wire.fromVersion(), wire.toVersion(),
                wire.operatorId(), wire.operatorName(), wire.operatedAt());
    }
}
