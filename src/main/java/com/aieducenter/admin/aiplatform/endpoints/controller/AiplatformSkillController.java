package com.aieducenter.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.AiplatformSkillAppService;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillInstallCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台技能管理控制器——技能线 T1 读面（issue #80，清单/详情）＋T2 写面（issue #81，
 * 安装/停用⇄启用/卸载），镜像 aiplatform #247/#248 技能线。
 *
 * <p>北向 {@code /api/admin/aiplatform/skills/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/skills/**}（机机签名出站）。技能寻址柄 {@code id} 为 opaque 串
 * 两形制（内置 {@code builtin:<技能名>}／安装 TSID 十进制串）——admin 字符串透传不解析
 * 不预校验（spec #79），畸形/未寻址柄由 provider 判 404 SKL_001（数字业务码 7001）经
 * {@code AiplatformUpstreamErrorAdvice} 原样还原。清单不分页不过滤（provider 有界目录、
 * 排序服务端定死——同形不造分页，ADR-0012 分页决议不适用于无分页端点）。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码）：读 {@code skill:read} 一码挂两端点；本票
 * 四写码 {@code skill:install|disable|enable|uninstall} 各自独立成码逐端点挂载（最小授权
 * ——如只给内容审核员配启停而不给装/卸）。写口操作者经框架 {@code OpenApiClient} 自动透传
 * {@code X-User-Id/X-User-Name} 出站头（identity 同款），provider 落痕（安装＝装者、启停＝
 * 最近动作者）——技能库写操作必留痕，缺头由 provider 域面守卫 400 SKL_009（数字业务码
 * 7009）拦截，admin 不拦。卸载经框架 {@code OpenApiClient.delete}（cartisan-boot#33）
 * 出站——HTTP DELETE 动词，不可被 get/post 替代。</p>
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
            description = "平台全部技能：内置（classpath 合成，来源=1）与库中安装"
                    + "（来源=2）同权呈现，空库时仍呈现内置技能。条目字段：名称、"
                    + "description、来源 code（1=内置 2=安装）与来源名、来源包"
                    + "（内置 null）、版本标识（装时 commit，内置 null）、状态"
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
}
