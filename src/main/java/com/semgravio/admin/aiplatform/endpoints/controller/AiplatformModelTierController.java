package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformModelTierAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierTraceResponse;
import com.semgravio.admin.constants.AdminScopes;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台模型档位控制器——BFF 模型档位域六端点（issue #111 B2，spec #101 B 批，镜像
 * provider aiplatform #367 的 {@code BackofficeModelTierController}）。
 *
 * <p>北向路径逐字镜像 aiplatform {@code /api/backoffice/model-tiers/**}（前缀换
 * {@code /api/admin/aiplatform}）。档位语义（语义键＝引用腿、先建通道再挂档位、版本
 * 升级改指向全部引用自动跟上、停用不删行、删除绑定拦截、留痕 append-only）全由
 * provider 裁决，BFF 零推导零校验。错误经 {@code AiplatformUpstreamErrorAdvice} 原样
 * 透传（如 404 PRJ_066→4066、409 PRJ_068/073→4068/4073、400 PRJ_070/071/034→
 * 4070/4071/4034）。</p>
 *
 * <p>权限码（spec #101 B 批，V23 已一次记账——本票落档位四码）：读
 * {@code model-tier:read} 一码挂读面三端点（清单/单行/留痕读面——traces 复用 read）；
 * 写 {@code model-tier:create/update/delete} 三码——读写分权、每写操作一码惯例
 * （最小授权；与通道四码互不串——通道/档位分权）。写口操作者身份由框架
 * {@code OpenApiClient} 自动透传 {@code X-User-Id/X-User-Name} 出站头（写操作必留痕，
 * 缺头由 provider 域面守卫 PRJ_034 拦，admin 不拦）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/model-tiers")
@Validated
@Tag(name = "Admin Aiplatform Model Tier", description = "AI 平台 · 模型档位维护（语义键 CRUD＋变更留痕）")
public class AiplatformModelTierController {

    private final AiplatformModelTierAppService modelTierAppService;

    public AiplatformModelTierController(AiplatformModelTierAppService modelTierAppService) {
        this.modelTierAppService = modelTierAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #101）；错误码以 description 文档化（PRJ_066 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:read",
            name = "AI 平台 / 模型档位查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位清单读面（绑定下拉数据源）——透传 aiplatform",
            description = "全部档位行按语义键序：语义键/实际模型名/所属通道/显示名/"
                    + "启用态＋最近写者。<strong>enabled=true 键集＝绑定改档（agent-configs "
                    + "面 PUT）的即时校验放行值域</strong>——admin-web 绑定下拉即以本清单"
                    + "为数据源（停用档在下拉中如实呈现停用态提示，非过滤——已停用档不可"
                    + "作新绑定〔400 PRJ_075〕、既有绑定装配明确报错〔PRJ_061〕）。语义键"
                    + "与模型版本解耦：实际模型名（方舟名带日期后缀、季度级更新）升级＝改"
                    + "档位行指向、语义键与全部绑定引用不动。种子三档：flash/pro（火山 GA "
                    + "对齐版）/doubao-flagship（备选档）。")
    public ApiResponse<List<AiplatformModelTierResponse>> list() {
        return ApiResponse.ok(modelTierAppService.listTiers());
    }

    @GetMapping("/{tierKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:read",
            name = "AI 平台 / 模型档位查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位单行读面——透传 aiplatform",
            description = "该档位行。寻址键即绑定引用腿（flash/pro/doubao-flagship 这类"
                    + "运营短名——透传不解析不预校验）。档位不存在 404 <strong>PRJ_066"
                    + "</strong>（数字业务码 4066）。")
    public ApiResponse<AiplatformModelTierResponse> detail(@PathVariable String tierKey) {
        return ApiResponse.ok(modelTierAppService.getTier(tierKey));
    }

    @PostMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:create",
            name = "AI 平台 / 模型档位创建",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位创建（建行即可被绑定）——透传 aiplatform",
            description = "新建档位：语义键随体（已存在 409 <strong>PRJ_068</strong>＝"
                    + "数字业务码 4068——语义键唯一，改既有档走 PUT）；所属通道必须已有行"
                    + "（400 <strong>PRJ_071</strong>＝4071——<strong>先建通道再挂档位</strong>，"
                    + "通道面 POST /model-providers）；实际模型名必填（400 <strong>PRJ_070"
                    + "</strong>＝4070）；显示名可空（＝回落语义键）；enabled 缺省 true"
                    + "（Boolean 装箱——null 出站在场＝provider 缺省 true）。创建即落痕"
                    + "（old 全 null）。<strong>「勾选入库」复用本写口</strong>（远端清单"
                    + "拉取的建议入库条：modelName 填所选条目 modelId、语义键人工命名——"
                    + "运营命名与模型版本解耦）。新供应商接入＝建通道行＋档位行两步。"
                    + "X-User-Id/X-User-Name 透传头必留痕（缺头 400 <strong>PRJ_034</strong>"
                    + "＝4034）。")
    public ApiResponse<AiplatformModelTierResponse> create(
            @RequestBody AiplatformModelTierCreateCommand command) {
        return ApiResponse.ok(modelTierAppService.createTier(command));
    }

    @PutMapping("/{tierKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:update",
            name = "AI 平台 / 模型档位维护",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位更新（换通道/升模型版本/改显示名/停启用；留痕）——透传 aiplatform",
            description = "PUT 全量语义：providerKey/modelName/displayName <strong>终态替换"
                    + "</strong>（displayName 可空＝清显示名回落语义键）；enabled <strong>缺省＝"
                    + "保持现值</strong>（停用档换模型名/换通道不被意外复启，显式 true/false "
                    + "才翻转——缺省肢在 wire 请求体上保真，可空肢 null 在场＝保持现值，"
                    + "不臆造默认值）。<strong>升模型版本</strong>（方舟名带日期后缀、季度级"
                    + "更新）＝改档位行指向，全部引用自动跟上——语义键与模型版本解耦，换"
                    + "供应商只动档位不动绑定。改指向下一轮命令构建即生效（装配动态查库）。"
                    + "<strong>停用＝既有绑定装配报错 PRJ_061、新绑定被即时校验拒绝 "
                    + "PRJ_075（临时下线用停用不删行）</strong>。值面无动＝幂等回执不落痕；"
                    + "回滚＝留痕旧值快照写回（行在＝PUT、行已删＝按旧值重建行，皆一次新"
                    + "变更留新痕）。档位不存在 404 <strong>PRJ_066</strong>（4066）；所属"
                    + "通道不存在 400 <strong>PRJ_071</strong>（4071）；字段不完整 400 "
                    + "<strong>PRJ_070</strong>（4070）；X-User-Id/X-User-Name 必留痕（缺头"
                    + " 400 <strong>PRJ_034</strong>＝4034）。回执与 GET 同形。HTTP "
                    + "<strong>PUT</strong> 动词。")
    public ApiResponse<AiplatformModelTierResponse> update(
            @PathVariable String tierKey, @RequestBody AiplatformModelTierUpdateCommand command) {
        return ApiResponse.ok(modelTierAppService.updateTier(tierKey, command));
    }

    @DeleteMapping("/{tierKey}")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:delete",
            name = "AI 平台 / 模型档位删除",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位删除（被绑定引用即拒）——透传 aiplatform",
            description = "删除档位行；仍被智能体运营配置绑定（model_id/"
                    + "routing_strong_model_id 任一＝该语义键）即 409 <strong>PRJ_073</strong>"
                    + "（数字业务码 4073——先改绑或清空绑定再删，悬空引用不留静默坑；"
                    + "<strong>临时下线用停用不删行</strong>）。删成落痕（new 全 null，留痕"
                    + "跨行存活）。档位不存在 404 <strong>PRJ_066</strong>（4066）；"
                    + "X-User-Id/X-User-Name 必留痕（缺头 400 <strong>PRJ_034</strong>＝"
                    + "4034）。回执 data＝null（信封即回执）。HTTP <strong>DELETE</strong> "
                    + "动词。")
    public ApiResponse<Void> delete(@PathVariable String tierKey) {
        modelTierAppService.deleteTier(tierKey);
        return ApiResponse.ok();
    }

    @GetMapping("/{tierKey}/traces")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:model-tier:read",
            name = "AI 平台 / 模型档位查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "档位变更留痕读面（历史可查，回滚依据）——透传 aiplatform",
            description = "该档位全部变更留痕按时间倒序（最近先）：前后全量值快照＋操作者"
                    + "＋时刻；创建痕 old 全 null、删除痕 new 全 null、newDisplayName null＝"
                    + "本次清空回落语义键；append-only 跨行存活——行删除后留痕仍可读"
                    + "（<strong>留痕读面不要求行存在</strong>——未知键空清单 200 非 404，"
                    + "空清单＝从未有行，行读面负责 404 判别）。回滚＝把某痕旧值快照写回"
                    + "（行在＝PUT、行已删＝按旧值重建行，皆一次新变更留新痕，不做版本树）。")
    public ApiResponse<List<AiplatformModelTierTraceResponse>> traces(
            @PathVariable String tierKey) {
        return ApiResponse.ok(modelTierAppService.listTraces(tierKey));
    }
}
