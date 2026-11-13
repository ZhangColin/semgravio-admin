package com.semgravio.admin.aiplatform.endpoints.controller;

import com.semgravio.admin.aiplatform.application.AiplatformResourceAppService;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceOverviewResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformResourceSweepResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台「资源面」控制器——资源大盘＋手动清扫（issue #128，镜像 aiplatform #489
 * 后台资源面：#473/ADR-0045 资源回收通道的观测与手动处置面，两枚全新端点；先例
 * #126〔#487 采样面〕/#127〔#488 失联标记〕）。
 *
 * <p>北向 {@code /api/admin/aiplatform/resources/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/resources/**}（机机签名出站）——无查询参数、无请求体、无
 * 路径参数，观测面秒级返回（provider 纯读库零 docker 调用）。错误经
 * {@code AiplatformUpstreamErrorAdvice} 原样透传（docker CLI 自身失败 500 WSP_002
 * →数字业务码 1002）。</p>
 *
 * <p>权限码两枚（62→64）：读 {@code resource:read} 挂大盘；写 {@code resource:sweep}
 * 挂手动清扫——观测权与动刀权分权（动刀类最小授权，照 ADR-0045 动刀走 admin 手动＋
 * 留痕）。sweep 操作者经框架 {@code OpenApiClient} 自动透传 {@code X-User-Id/
 * X-User-Name} 出站头（provider 落 append-only 留痕行＋报告 JSON 落档，缺头落空 NULL——
 * 照沙箱动作面口径，provider 不落强制守卫）。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/resources")
@Validated
@Tag(name = "Admin Aiplatform Resource", description = "AI 平台 · 资源面")
public class AiplatformResourceController {

    private final AiplatformResourceAppService resourceAppService;

    public AiplatformResourceController(AiplatformResourceAppService resourceAppService) {
        this.resourceAppService = resourceAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——同各域先例）；错误码以 description 文档化（WSP_002 等）。
    @GetMapping("/overview")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:resource:read",
            name = "AI 平台 / 资源大盘查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "资源大盘（主机盘/docker 用量/无主计数/失联计数）——透传 aiplatform",
            description = "「资源面」观测面（#489）：主机磁盘（平台进程所在文件系统，host.path"
                    + "＝与 df 对账的路径锚；usedBytes＝total−usable 派生，与 df 的 Used 列含"
                    + "保留块口径略有差异）＋docker 四类用量（镜像/容器/本地卷/build cache 的"
                    + "计数与体量——体量十进制单位换算与 docker 显示同基，近似值不当精确字节"
                    + "求和）＋无主资源计数（docker 侧 ws-<id>/vol-ws-<id> 对注册表全量求差，"
                    + "ADR-0045 准绳——docker system df「可回收」口径把休眠卷一并计入，永不进"
                    + "本面）＋在册失联计数 lostWorkspaceCount（#488——项目行已删而物理资源仍在，"
                    + "只标记待人工裁决，<strong>出口＝一键回收 reclaim，在册≠无主，不得当无主"
                    + "自动清</strong>）＋最近一次手动清扫 lastSweep（收编结果面直读：操作者/"
                    + "时刻/收编计数；从未手动清扫为 null）。<strong>纯读库零 docker 调用、秒级"
                    + "返回</strong>（数据全部来自扫描轮采样落库与留痕表）。<strong>null＝缺测、"
                    + "0＝真零</strong>两态必须分开展示：从未采样（首个扫描窗内）host/docker/"
                    + "orphans 整面 null——呈现「采样中」而非零；docker 不可达轮 docker 面各列"
                    + "null——「缺测」≠「零」。sampledAt＝采样时刻新鲜度标注（未采样 null）。"
                    + "无查询参数。")
    public ApiResponse<AiplatformResourceOverviewResponse> overview() {
        return ApiResponse.ok(resourceAppService.overview());
    }

    // 不挂 @ErrorCodes（同上）：WSP_002 以 description 文档化。
    @PostMapping("/sweep")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:resource:sweep",
            name = "AI 平台 / 资源清扫",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "手动清扫（无主收编，逐项处置报告）——透传 aiplatform",
            description = "「资源面」动刀口（#489，#473 故事 8：按需对账不等扫描轮）：触发一次"
                    + "无主收编——<strong>与扫描轮自动收编同一实现同一准绳</strong>（保留集＝"
                    + "注册表全量编号——在册一律不动〔休眠保卷是用户数据 ADR-0016〕；宽限线"
                    + "＝触发时刻减 10 分钟〔graceLine——新建「资源已建、记录未落」窗口保护〕；"
                    + "点名删非 prune——docker system df「可回收」口径不作判定、volume prune "
                    + "永不进任何通道）。回执＝逐项处置报告 items：收编了什么（disposition"
                    + "=SWEPT）、跳过什么为什么（SKIPPED：skipReason=REGISTERED 在册〔含休眠"
                    + "保卷〕/GRACE_WINDOW 宽限窗内/TIMESTAMP_UNREADABLE 时刻解析不出无从判"
                    + "宽限）、删除未成什么（FAILED——下轮扫描自愈，不虚报）；resourceCreatedAt"
                    + "＝docker 回执的启动/创建时刻（在册项未起 inspect 恒 null）。<strong>"
                    + "registryReachable=false＝库不可达一刀未动</strong>——准绳无从构成时绝不按"
                    + "「全部无主」动手，如实呈报不重试。计数面 sweptContainers/sweptVolumes/"
                    + "skippedCount/failedCount 一源＝域报告计数。操作者两列（operatorId/"
                    + "operatorName）＝X-User-Id/X-User-Name 透传头落痕回显（缺头落空 NULL——"
                    + "照沙箱动作面口径，provider 不落强制守卫）；append-only 留痕行＋报告 JSON "
                    + "全量落档在 provider 侧。手动清扫不受休眠总开关静默（管理员显式动作，"
                    + "ADR-0045 动刀类走 admin 手动＋留痕）。<strong>无请求体</strong>。docker "
                    + "CLI 自身失败 500 WSP_002（数字业务码 1002——未执行完的清扫不留痕不定格）"
                    + "原样透传。")
    public ApiResponse<AiplatformResourceSweepResponse> sweep() {
        return ApiResponse.ok(resourceAppService.sweep());
    }
}
