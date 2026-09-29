package com.aieducenter.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.AiplatformSkillAppService;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillAssignCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillInstallCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillUpdateCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillPrecheckResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateTraceResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSlotAssignmentResponse;
import com.aieducenter.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台技能管理控制器——技能线 T1 读面（issue #80，清单/详情）＋T2 写面（issue #81，
 * 安装/停用⇄启用/卸载）＋T3 槽位指派读写（issue #82）＋T4 显式更新＋更新留痕读面
 * （issue #83）＋指派双头预检（issue #90，技能域第 16 端点），镜像 aiplatform
 * #247/#248/#249/#250 技能线＋#255 预检端点。
 *
 * <p>北向 {@code /api/admin/aiplatform/skills/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/skills/**}（机机签名出站）。技能寻址柄 {@code id} 为 opaque 串
 * 两形制（内置 {@code builtin:<技能名>}／安装 TSID 十进制串）——admin 字符串透传不解析
 * 不预校验（spec #79），畸形/未寻址柄由 provider 判 404 SKL_001（数字业务码 7001）经
 * {@code AiplatformUpstreamErrorAdvice} 原样还原。清单不分页不过滤（provider 有界目录、
 * 排序服务端定死——同形不造分页，ADR-0012 分页决议不适用于无分页端点）。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码）：读 {@code skill:read} 一码挂清单/详情/
 * 指派读面/更新留痕读面四端点；写码 {@code skill:install|disable|enable|uninstall|assign|update}
 * 各自独立成码逐端点挂载（最小授权——如只给内容审核员配启停而不给装/卸/更新、只给装配运营
 * 配指派）。指派双头预检<strong>复用 {@code skill:assign}</strong>（issue #90：预检触发模型
 * 调用有真实成本——flash 档 skillcheck 计量，只读角色不得烧 token；它是指派写流程的附属
 * 检查，assign-only 用户语义自洽，30 码总数不变）。
 * {@code slot} 为职能槽位稳定键（main/executor/subagent）字符串路径参数——透传不预校验，
 * 未知键由 provider 判 404 SKL_010（数字业务码 7010）。写口操作者经框架
 * {@code OpenApiClient} 自动透传 {@code X-User-Id/X-User-Name} 出站头（identity 同款），
 * provider 落痕（安装＝装者、启停＝最近动作者、指派整包替换留最近动作者）——技能库写
 * 操作必留痕，缺头由 provider 域面守卫 400 SKL_009（数字业务码 7009）拦截，admin 不拦。
 * 卸载经框架 {@code OpenApiClient.delete}（cartisan-boot#33）出站——HTTP DELETE 动词，
 * 不可被 get/post 替代；指派整包替换经 {@code OpenApiClient.put} 出站——HTTP PUT 动词
 * （PUT 全量语义，请求体即终态）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/skills")
@Validated
@Tag(name = "Admin Aiplatform Skill", description = "AI 平台 · 技能管理")
public class AiplatformSkillController {

    private final AiplatformSkillAppService skillAppService;

    public AiplatformSkillController(AiplatformSkillAppService skillAppService) {
        this.skillAppService = skillAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #79）；错误码以 description 文档化（SKL_001 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:read",
            name = "AI 平台 / 技能管理查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "技能清单（内置＋安装同权，不分页）——透传 aiplatform",
            description = "平台全部技能：内置（classpath 合成，来源=1）与库中技能"
                    + "（安装＝来源 2、自产＝来源 3）同权呈现，空库时仍呈现内置技能。"
                    + "条目字段：名称、description、来源 code（1=内置 2=安装 3=自产）"
                    + "与来源名、来源包（安装仓库；内置 null、自产＝固定虚拟值 self）、"
                    + "版本标识（装时 commit；自产＝self；内置 null）、状态"
                    + "（1=启用 2=停用，内置恒 1）、最近管理动作操作者（安装＝装者、"
                    + "启停＝最近动作者，内置 null）、远端有新版标记 updateAvailable"
                    + "（来源包级：定期只读检查远端 HEAD 与装时版本不同即 true——"
                    + "更新走显式 POST /update，平台永不自动跟新；null＝未检查过，"
                    + "内置恒 null）。排序服务端定死：内置在前"
                    + "（名称序）、安装在后（来源包、名称序）。技能库是有界目录"
                    + "（装什么是运营决策），不分页不过滤。id 为 opaque 串两形制"
                    + "（builtin:<技能名>／TSID 十进制串），作详情/写口寻址柄。")
    public ApiResponse<List<AiplatformSkillSummaryResponse>> skills() {
        return ApiResponse.ok(skillAppService.list());
    }

    @GetMapping("/{id}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:read",
            name = "AI 平台 / 技能管理查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "技能详情（元数据＋frontmatter＋正文，审核面）——透传 aiplatform",
            description = "读全文判安装审核（注入面＋方法论重叠把关）：元数据（与"
                    + "清单行同形）＋ frontmatter 全量（解析态键值，含 name/"
                    + "description）＋正文全文（frontmatter 剥离后的 SKILL.md "
                    + "body）——所见即运行时注入面。id 取清单行原值（opaque 串"
                    + "两形制）。技能不存在（含未寻址内置名/TSID、畸形柄）"
                    + "404 SKL_001（数字业务码 7001）。")
    public ApiResponse<AiplatformSkillDetailResponse> detail(@PathVariable String id) {
        return ApiResponse.ok(skillAppService.getDetail(id));
    }

    // 不挂 @ErrorCodes（同上两口）：安装失败族 SKL_002–SKL_009 以 description 文档化。
    @PostMapping("/install")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:install",
            name = "AI 平台 / 技能包安装",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "安装技能包（git 仓库快照固化）——透传 aiplatform",
            description = "装时 clone 解析全部 SKILL.md 固化入库（ADR-0021 快照安装，"
                    + "不订阅远端）：来源包＝规范化仓库地址（trim、去尾斜杠与 .git——"
                    + "同源去重键）、版本标识＝装时 HEAD commit（快照锚）、状态＝启用、"
                    + "操作者＝装者（X-User-Id/X-User-Name 透传头，缺头 400 SKL_009＝"
                    + "数字业务码 7009）。excludeDirs 可勾选排除目录段：技能的仓库相对"
                    + "路径任一段命中即不入库（如 deprecated 类目整支排除；段名精确匹配）。"
                    + "解析管道与内置同源（审核面所见即运行时注入面）。失败 fail-fast 整体"
                    + "不入库：地址空 400 SKL_002（7002）、同源重复安装 409 SKL_003（7003，"
                    + "更新走显式更新动作）、克隆失败/超时 502 SKL_004（7004）、未解析到技能"
                    + " 400 SKL_005（7005）、SKILL.md 不合格 400 SKL_006（7006）、包内重名"
                    + " 400 SKL_007（7007）。回执＝本次装入的条目清单（清单行同形）。")
    public ApiResponse<List<AiplatformSkillSummaryResponse>> install(@RequestBody AiplatformSkillInstallCommand command) {
        return ApiResponse.ok(skillAppService.install(command));
    }

    @PostMapping("/{id}/disable")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:disable",
            name = "AI 平台 / 技能停用",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "停用技能（可逆开关）——透传 aiplatform",
            description = "退出装配候选（装配合成只收启用行）、不丢库行；误伤可经"
                    + " enable 恢复。X-User-Id/X-User-Name 透传头自动落痕（最近"
                    + "管理动作操作者）——缺头 400 SKL_009（数字业务码 7009，技能库"
                    + "写操作必留痕，无落空通道）。重复停用幂等（操作者留最近一次）。"
                    + "寻址 TSID 柄；内置技能非库行无状态迁移——builtin: 柄/畸形柄/"
                    + "未寻址 TSID 同语义 404 SKL_001（7001）。无请求体。回执＝翻转后"
                    + "清单行。")
    public ApiResponse<AiplatformSkillSummaryResponse> disable(@PathVariable String id) {
        return ApiResponse.ok(skillAppService.disable(id));
    }

    @PostMapping("/{id}/enable")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:enable",
            name = "AI 平台 / 技能启用",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "启用技能（恢复装配候选）——透传 aiplatform",
            description = "停用的可逆侧：恢复参与装配合成。X-User-Id/X-User-Name"
                    + " 透传头自动落痕（口径同 disable，缺头 400 SKL_009＝7009）。"
                    + "重复启用幂等。寻址与 404 语义同 disable（TSID 柄；内置柄 404"
                    + " SKL_001＝7001）。无请求体。回执＝翻转后清单行。")
    public ApiResponse<AiplatformSkillSummaryResponse> enable(@PathVariable String id) {
        return ApiResponse.ok(skillAppService.enable(id));
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:uninstall",
            name = "AI 平台 / 技能卸载",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "卸载技能（库行删除，不可逆）——透传 aiplatform",
            description = "快照行删除即彻底出库（快照物无历史不漂移约束）；清单/"
                    + "详情/装配均不可见。卸载守卫：有指派在身（任一职能槽位）"
                    + "的技能拒绝卸载（409 SKL_008＝数字业务码 7008，先解绑再卸）。"
                    + "无行可留不留痕（admin 侧自有操作日志）。回执＝删除前终态"
                    + "（确认移除了什么）。寻址 TSID 柄；内置技能不可卸——builtin: 柄/"
                    + "畸形柄/未寻址 TSID 同语义 404 SKL_001（7001）、重复卸载 404。"
                    + "HTTP <strong>DELETE</strong> 动词。")
    public ApiResponse<AiplatformSkillSummaryResponse> uninstall(@PathVariable String id) {
        return ApiResponse.ok(skillAppService.uninstall(id));
    }

    @GetMapping("/assignments/{slot}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:read",
            name = "AI 平台 / 技能管理查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "槽位指派读面（三职能槽位各自独立）——透传 aiplatform",
            description = "该职能槽位当前指派的技能清单（与技能清单行共形、含停用行"
                    + "——启停是可逆开关指派关系随行保留，运营可见「指派了但已停用」"
                    + "实态；停用行不参与装配合成）。槽位三把：main=主智能体 / "
                    + "executor=run 执行体 / subagent=子智能体，未知槽位 404 SKL_010"
                    + "（数字业务码 7010）。装配生效语义（ADR-0021）：装配合成＝内置∪"
                    + "该槽位已指派且启用，动态查库——指派/启停变更后智能体下一轮自然"
                    + "生效、进行中 run 不定格不打断。回执与 PUT 同形。")
    public ApiResponse<AiplatformSlotAssignmentResponse> slotAssignments(@PathVariable String slot) {
        return ApiResponse.ok(skillAppService.getSlotAssignments(slot));
    }

    @PutMapping("/assignments/{slot}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:assign",
            name = "AI 平台 / 技能指派",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "槽位指派整包替换（清单即终态）——透传 aiplatform",
            description = "PUT 全量语义：skillIds 即该槽位终态（未列入即解绑、空清单"
                    + "＝清空），支持整包批量勾选（admin 侧按来源包勾满后送全量）。"
                    + "指派目标只收安装库行 TSID 柄：内置 builtin: 柄不可指派（400 "
                    + "SKL_011＝数字业务码 7011——内置随平台发版，装配按配置挂载）；"
                    + "未寻址/畸形 TSID 404 SKL_001（7001，先卸载后指派的不变窗口同"
                    + "语义）。X-User-Id/X-User-Name 透传头落痕（整包替换留最近动作者，"
                    + "缺头 400 SKL_009＝7009）。槽位寻址同 GET（未知 404 SKL_010＝"
                    + "7010）。生效语义＝动态查库：变更后下一轮自然生效，不新增智能体"
                    + "实例（工厂缓存键不含技能集）。回执＝替换后读面（与 GET 同形）。"
                    + "HTTP <strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformSlotAssignmentResponse> assignSlot(
            @PathVariable String slot, @RequestBody AiplatformSkillAssignCommand command) {
        return ApiResponse.ok(skillAppService.assignSlot(slot, command));
    }

    // 不挂 @ErrorCodes（同上各口）：预检错误族 SKL_001/010/011 以 description 文档化（与 PUT 指派
    // 同族；无 SKL_009——预检只读不留痕，无操作者头要求）。
    @PostMapping("/assignments/{slot}/precheck")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:assign",
            name = "AI 平台 / 技能指派",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "指派双头预检（非阻断提示，只读）——透传 aiplatform",
            description = "PUT 指派提交前对候选集做方法论重叠判定：对照双头——该槽位"
                    + "生效工作协议（智能体运营配置生效值）∪ 该槽位已指派启用技能 ∪ "
                    + "候选集内其他技能（subagent 槽无运营配置正本：对照＝已指派＋候选集，"
                    + "机制同一不特判），由 provider 侧模型判定（flash 档专用配置键、专用"
                    + "计量标记 skillcheck、15s 超时）。<strong>两态纪律</strong>：回执"
                    + "两件同读——executed=true 且 hints 空＝确认无重叠；executed=true 且"
                    + " hints 非空＝发现重叠（每条：skills 涉及的候选技能、counterpart"
                    + " 对照侧〔「工作协议」或对侧技能名〕、overlap 重叠内容、resolution"
                    + " 消解方向；三 String 字段可 null——模型输出解析态，出 JSON null 如实"
                    + "在场不兜底）；executed=false＝<strong>未执行</strong>（判定失败/"
                    + "超时/输出不可解析，hints 恒空）——<strong>不得当「确认无重叠」呈现</strong>，"
                    + "如实标注「预检未执行」（降级是预期内态、不必自动重试；候选清单未变"
                    + "不必重调）。<strong>非阻断</strong>：提示是输入不是门，判定权留管理员"
                    + "——预检结果不拦 PUT 指派、预检组件故障不阻塞指派照常提交。全程只读"
                    + "不留痕（不落库、不改指派、无操作者头要求——区别于 PUT 指派的缺头"
                    + "SKL_009）。请求体与 PUT 指派同形（skillIds＝候选终态清单，直接传"
                    + "确认框里将要提交的清单）；slot 寻址同 PUT（未知 404 SKL_010＝数字"
                    + "业务码 7010）。错误族与 PUT 同源同码：内置柄 400 SKL_011（7011）、"
                    + "未寻址/畸形 TSID 404 SKL_001（7001）——均原样透传。权限复用"
                    + " skill:assign：预检触发模型调用有真实成本，只读角色不得调用（指派"
                    + "配置页菜单 230 承载，触发时机归前端 grill）。")
    public ApiResponse<AiplatformSkillPrecheckResponse> precheckSlot(
            @PathVariable String slot, @RequestBody AiplatformSkillAssignCommand command) {
        return ApiResponse.ok(skillAppService.precheckSlot(slot, command));
    }

    // 不挂 @ErrorCodes（同上各口）：更新失败族 SKL_004/005/007/009/012/013/014 以 description 文档化。
    @PostMapping("/update")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:update",
            name = "AI 平台 / 技能包更新",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "显式更新技能包（重拉快照＋版本留痕）——透传 aiplatform",
            description = "按来源包整体重拉快照入库（ADR-0021：更新永远显式点，"
                    + "平台永不自动跟新远端——清单「有新版」标记只提示、不动作）。"
                    + "sourcePackage 取清单行原值回传（服务端再规范化，同源身份"
                    + "单源）；排除名单＝装时持久化口径（更新不还魂装时排除的目录）。"
                    + "翻新语义：同名行原地翻新（id/状态/指派跨更新保留——TSID 柄"
                    + "稳定）、新技能插入（启用）、远端已删行移除——移除有指派在身"
                    + "的技能整体拒绝（409 SKL_013＝数字业务码 7013，先解绑再更新）。"
                    + "远端未前进＝幂等回执（from==to、不落痕不写行）。每次前进必留"
                    + "版本痕（from→to＋操作者，经 update-traces 可查）。X-User-Id/"
                    + "X-User-Name 透传头落痕（缺头 400 SKL_009＝7009）。失败族：来源包空"
                    + " 400 SKL_014（7014）、未安装 404 SKL_012（7012）、克隆失败/超时"
                    + " 502 SKL_004（7004）、远端未解析到技能 400 SKL_005（7005）、"
                    + "包内重名 400 SKL_007（7007）。回执＝from/to 版本＋被移除技能名"
                    + "＋更新后终态行集。")
    public ApiResponse<AiplatformSkillUpdateResponse> update(@RequestBody AiplatformSkillUpdateCommand command) {
        return ApiResponse.ok(skillAppService.update(command));
    }

    // 不挂 @ErrorCodes（同上各口）：SKL_014 以 description 文档化。
    @GetMapping("/update-traces")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:skill:read",
            name = "AI 平台 / 技能管理查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "更新留痕读面（历史版本可查）——透传 aiplatform",
            description = "该来源包全部显式更新留痕（from→to 版本＋操作者＋时刻），"
                    + "按时间倒序（最近先）。append-only：卸载不删留痕——历史事实"
                    + "不随库行消失，装时版本经链条首个 from 回溯。sourcePackage"
                    + " 取清单行原值（必填，空/缺 400 SKL_014＝数字业务码 7014）。")
    public ApiResponse<List<AiplatformSkillUpdateTraceResponse>> updateTraces(
            @RequestParam(required = false) String sourcePackage) {
        return ApiResponse.ok(skillAppService.updateTraces(sourcePackage));
    }
}
