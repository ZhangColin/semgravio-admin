package com.aieducenter.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.AiplatformSkillAppService;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
import com.aieducenter.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台技能管理控制器——技能线 T1 读面（issue #80，镜像 aiplatform #247 技能线：
 * 清单/详情两端点）。
 *
 * <p>北向 {@code /api/admin/aiplatform/skills/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/skills/**}（机机签名出站）。技能寻址柄 {@code id} 为 opaque 串
 * 两形制（内置 {@code builtin:<技能名>}／安装 TSID 十进制串）——admin 字符串透传不解析
 * 不预校验（spec #79），畸形/未寻址柄由 provider 判 404 SKL_001（数字业务码 7001）经
 * {@code AiplatformUpstreamErrorAdvice} 原样还原。清单不分页不过滤（provider 有界目录、
 * 排序服务端定死——同形不造分页，ADR-0012 分页决议不适用于无分页端点）。</p>
 *
 * <p>权限码（spec #79，技能线 per-verb 11 码之首）：读 {@code skill:read} 一码挂两端点；
 * 写码（install/disable/enable/uninstall/update/assign）随 T2–T4 票各自独立成码。读口无
 * 操作者语义——操作者透传头只在写口落痕（provider 域面守卫 SKL_009，非本票面）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/skills")
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
}
