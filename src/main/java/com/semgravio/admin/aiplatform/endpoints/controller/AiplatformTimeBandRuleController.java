package com.semgravio.admin.aiplatform.endpoints.controller;

import java.util.List;

import com.semgravio.admin.aiplatform.application.AiplatformTimeBandRuleAppService;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleOpenCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleRewindowCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformTimeBandRuleQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleRewindowResponse;
import com.semgravio.admin.constants.AdminScopes;
import com.cartisan.security.annotation.RequireAuth;
import com.cartisan.security.annotation.RequirePermission;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台时段规则控制器——清单（含历史版本分页）＋开行（一单 N 窗口＝一个版本）＋原子改窗
 * （关当前版本全部行＋开新版本，回执 closed/opened 双段）＋停用（关版本不接新行）四端点，
 * issue #130（镜像 aiplatform #497／ADR-0044 峰谷 1/4 后台机机面——时段规则＝供应商峰谷
 * 定价的时段定义：通道 × 生效区间 × 星期口径 × UTC 窗口行）。
 *
 * <p>北向 {@code /api/admin/aiplatform/time-band-rules/**} 逐字镜像 aiplatform
 * {@code /api/backoffice/time-band-rules/**}——查询参数名、缺省值、分页基准（page 1-based，
 * 缺省 1/20）与单价表 controller 同形。错误经 {@code AiplatformUpstreamErrorAdvice} 原样
 * 透传（如 400 METER_013→3013、400 METER_014→3014、404 METER_015→3015、409 METER_016→3016、
 * 409 METER_008→3008）。窗不跨午夜（v1 不支持——表单侧建议提示后拆两行提交：22:00–02:00＝
 * 22:00–24:00＋00:00–02:00，服务端拦 start≥end 出 METER_013）。</p>
 *
 * <p>权限码（照单价表域先例——读写分权、每写操作一码）：读 {@code time-band-rule:read} 一码；
 * 写操作各自独立成码（{@code time-band-rule:open|rewindow|deactivate}）——最小授权（如只给
 * 调窗审核员配 rewindow 而不给停用）。操作者身份由框架 OpenApiClient 自动透传
 * {@code X-User-Id/X-User-Name} 出站头（identity 同款），provider 落痕开新版本/改窗新行/
 * 停用被关行——北向命令体不含操作者字段，前端无法伪造留痕。</p>
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/admin/aiplatform/time-band-rules")
@Validated
@Tag(name = "Admin Aiplatform Time Band Rule", description = "AI 平台 · 时段规则（峰谷运营位）")
public class AiplatformTimeBandRuleController {

    private final AiplatformTimeBandRuleAppService timeBandRuleAppService;

    public AiplatformTimeBandRuleController(AiplatformTimeBandRuleAppService timeBandRuleAppService) {
        this.timeBandRuleAppService = timeBandRuleAppService;
    }

    // 不挂 @ErrorCodes：框架 ErrorCodesValidator 要求码在应用自身 CodeMessageRegistry 注册，admin 不镜像
    // provider 错误码目录（忠实透传、零加戏——spec #62）；错误码以 description 文档化（METER_013 等）。
    @GetMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:time-band-rule:read",
            name = "AI 平台 / 时段规则查看",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "时段规则行清单（含历史版本，分页 1-based）——透传 aiplatform",
            description = "峰谷运营工作面：现行与历史行全量（规则史全貌），排序服务端定死＝生效起点"
                    + "倒序（新版本在前，同起点 id 倒序稳定——一版本多窗口行同起点相邻）。provider 为"
                    + "匹配键＝精确等值过滤、可缺省（缺省＝全量行）；effectiveTo 为 null 即当前行。"
                    + "行字段：band Integer code（1=峰 2=谷）＋bandName 中文名随行；dayMask 星期口径 "
                    + "bitmask（ISO 周序 Mon=1 Tue=2 … Sun=64，合法域 1..127）＋dayMaskName 连续段"
                    + "压缩直读名（31→「周一至周五」）；startMinute/endMinute 为 UTC 分钟（end 不含、"
                    + "须 start<end——跨午夜窗口 v1 不支持、拆两行）；operatorId/operatorName 留痕"
                    + "（该行最近管理动作——开行/改窗落新行、停用落被关行；存量行/种子脚本种入行为 "
                    + "null）。page 1 基（缺省 1）、size 缺省 20（上界 100），clamp 由 provider 执行、"
                    + "回显 provider 回报值。绑定裁决两段：非整数 page/size 在本服务绑定层 404（框架"
                    + "类型不匹配口径，不到 provider）；provider 侧绑定失败（非法分页值）400 带字段"
                    + "明细仅在 provider 契约演进时可能出现，错误信封忠实透传。")
    public ApiResponse<PageResponse<AiplatformTimeBandRuleResponse>> entries(
            @RequestParam(required = false) String provider,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(timeBandRuleAppService.list(
                new AiplatformTimeBandRuleQuery(provider), page, size));
    }

    @PostMapping
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:time-band-rule:open",
            name = "AI 平台 / 时段规则开行",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "开行（空通道首开，一单 N 窗口＝一个规则版本）——透传 aiplatform",
            description = "对指定通道新开一个规则版本：一单 N 时段窗口行原子落（每项＝规则表一行"
                    + "〔通道 × 生效区间 × 星期口径 × UTC 窗口〕，同单各窗口共享生效起点）——DeepSeek"
                    + " 峰窗 01:00–04:00＋06:00–10:00 工作日（dayMask 31）两窗口一单种入即本语义。"
                    + "命令体逐字镜像 {provider, windows:[{band, dayMask, startMinute, endMinute}], "
                    + "effectiveFrom}：band 契约为 Integer code（1=峰 2=谷）；dayMask＝星期口径 "
                    + "bitmask（ISO 周序 Mon=1…Sun=64，合法域 1..127——复选周一～周日编码）；"
                    + "startMinute/endMinute＝UTC 分钟（end 不含，须 start<end——跨午夜窗口 v1 不"
                    + "支持，表单侧建议提示后拆两行提交〔22:00–02:00＝22:00–24:00＋00:00–02:00〕，"
                    + "服务端拦 start≥end 出 METER_013）；effectiveFrom ISO-8601 Instant（UTC 带 Z）"
                    + "可空＝缺省即时，可回溯（种子敞口覆盖存量事件）、含未来时点＝预发布（对齐供应"
                    + "商调窗口公告期）——预发布与同通道区间重叠校验语义由 provider 承担，BFF 不代填"
                    + "时点不预判。回执＝新版本全部行。X-User-Id/X-User-Name 透传头自动落痕新行"
                    + "（缺头落空——种子脚本即落空口径）。窗口项字段不完整/星期口径越界/跨午夜窗口 "
                    + "400 METER_013（3013）；同通道区间重叠（跨区间或同起点）409 METER_008（3008）。")
    public ApiResponse<List<AiplatformTimeBandRuleResponse>> open(
            @RequestBody AiplatformTimeBandRuleOpenCommand command) {
        return ApiResponse.ok(timeBandRuleAppService.open(command));
    }

    @PostMapping("/{id}/rewindow")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:time-band-rule:rewindow",
            name = "AI 平台 / 时段规则改窗",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "原子改窗（单调用关当前版本全部行＋开新版本，回执 closed/opened 双段）——透传 aiplatform",
            description = "同事务两段：寻址行须当前版本成员（版本任一行皆可达——关行在版本级非单行，"
                    + "同版本多窗口行共享区间），被关版本全部行落 effectiveTo＝新版本起点（保留其原"
                    + "开行操作者，不被改写）；新版本窗口清单整版本替换语义（清单即终态——被关版本的"
                    + "全部窗口不再延续）。effectiveFrom 可指定（ISO-8601 Instant，含未来时点＝预发布，"
                    + "对齐供应商调窗口公告期）；缺省＝即时。服务端补同通道生效区间重叠校验（本表无唯一"
                    + "约束兜底——同版本多窗口行共享起点，同起点拒由服务端拦）。X-User-Id/X-User-Name "
                    + "透传头自动落痕新行（缺头落空）。中途任一守卫失败两段都不动。回执 {closed, "
                    + "opened} 双段（库内事实）。行不存在（含畸形 id）404 METER_015（3015）；窗口项"
                    + "字段不完整/星期口径越界/跨午夜窗口 400 METER_013（3013）；起点早于被关版本起点 "
                    + "400 METER_014（3014）；目标非当前行 409 METER_016（3016）；区间重叠（跨区间或"
                    + "同起点）409 METER_008（3008）。")
    public ApiResponse<AiplatformTimeBandRuleRewindowResponse> rewindow(
            @PathVariable String id, @RequestBody AiplatformTimeBandRuleRewindowCommand command) {
        return ApiResponse.ok(timeBandRuleAppService.rewindow(id, command));
    }

    @PostMapping("/{id}/deactivate")
    @RequireAuth
    @RequirePermission(
            value = "admin:aiplatform:time-band-rule:deactivate",
            name = "AI 平台 / 时段规则停用",
            scope = AdminScopes.ADMIN
    )
    @Operation(summary = "停用（即时生效，关版本不接新行）——透传 aiplatform",
            description = "关当前版本全部行（effectiveTo＝现在），不开新版本——此后该通道 ts 无窗口"
                    + "命中＝不归带（引擎归段票接线后生效；读时归段未接线前不影响现行成本面）。对未生效的"
                    + "预发布版本停用＝钳到自身起点成空区间（从未生效）。<strong>无请求体</strong>。"
                    + "X-User-Id/X-User-Name 透传头自动落痕被关版本各行（停用不接新行，被关行是唯一"
                    + "落点；缺头落空）。回执＝被关版本全部行。行不存在（含畸形 id）404 METER_015"
                    + "（3015）；目标非当前行 409 METER_016（3016）。")
    public ApiResponse<List<AiplatformTimeBandRuleResponse>> deactivate(@PathVariable String id) {
        return ApiResponse.ok(timeBandRuleAppService.deactivate(id));
    }
}
