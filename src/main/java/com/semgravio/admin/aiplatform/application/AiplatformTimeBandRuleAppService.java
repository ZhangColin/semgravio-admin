package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleOpenCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeBandRuleRewindowCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformTimeWindowCommand;
import com.semgravio.admin.aiplatform.application.dto.query.AiplatformTimeBandRuleQuery;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformTimeBandRuleRewindowResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandRuleListWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandRuleOpenWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandRuleRewindowWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandRuleWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformTimeBandWindowWireRequest;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import com.cartisan.web.response.PageResponse;
import org.springframework.stereotype.Service;

/**
 * aiplatform 时段规则域 BFF 应用服务——清单（含历史版本，分页）＋开行（一单 N 窗口＝一个
 * 版本）＋原子改窗（关当前版本全部行＋开新版本，回执 closed/opened 双段）＋停用（关版本
 * 不接新行）四端点，issue #130（镜像 aiplatform #497／ADR-0044 峰谷 1/4 后台机机面，
 * 变更清单 aiplatform#505）。
 *
 * <p>admin 作为 BFF：调接口 + DTO 转换，不持业务逻辑、不持时段数据（版本生命周期与重叠
 * 校验归 aiplatform 计量上下文）。窗不跨午夜、字段守卫（METER_013）、关行时点（METER_014）、
 * 行不存在（METER_015）、非当前行（METER_016）、同通道区间重叠（METER_008）全归 provider
 * 裁决，错误信封已由 {@link AiplatformClient} 统一翻译为 {@link AiplatformUpstreamException}
 * 原样透传（spec #62 定稿），本层不再 try/catch。操作者身份经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传（AppService 不经手），provider 落痕新行/被关行。</p>
 *
 * <p>分页（spec #62 平台分页统一决议目标态）：北向请求 page <strong>1-based</strong>，出站直传
 * 零换算；回显取 provider 回报的 page/size 原值（含 provider clamp 后的值），BFF 不重复夹取。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformTimeBandRuleAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformTimeBandRuleAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 时段规则行清单（透传 aiplatform，含历史版本——规则史全貌）——provider 精确等值过滤
     * （可缺省＝全量行）；排序服务端定死＝生效起点倒序（新版本在前，同起点 id 倒序稳定——
     * 一版本多窗口行同起点相邻）；effectiveTo 为 null 即当前行。行带操作者两列（该行最近
     * 管理动作留痕；存量行/种子脚本种入行落 null）。
     */
    public PageResponse<AiplatformTimeBandRuleResponse> list(AiplatformTimeBandRuleQuery query,
                                                             int page, int size) {
        PageResponse<AiplatformTimeBandRuleWireResponse> wirePage = aiplatformClient.listTimeBandRules(
                new AiplatformTimeBandRuleListWireRequest(query.provider()), page, size);
        List<AiplatformTimeBandRuleResponse> items = wirePage.items().stream()
                .map(AiplatformTimeBandRuleAppService::toRule)
                .toList();
        // 回显 provider 的 page/size 原值（1-based，含 clamp 后值）——不是北向入参回声
        return new PageResponse<>(items, wirePage.total(), wirePage.page(), wirePage.size());
    }

    /**
     * 开行（透传 aiplatform）——空通道首开：一单 N 时段窗口行原子落（每项＝规则表一行，
     * 同单各窗口共享生效起点）＝一个规则版本。effectiveFrom 可回溯（种子敞口覆盖存量事件）、
     * 可指定未来时点（预发布），缺省即时（provider 裁决）。回执＝新版本全部行。
     */
    public List<AiplatformTimeBandRuleResponse> open(AiplatformTimeBandRuleOpenCommand command) {
        List<AiplatformTimeBandRuleWireResponse> opened = aiplatformClient.openTimeBandRule(
                new AiplatformTimeBandRuleOpenWireRequest(
                        command.provider(),
                        command.windows().stream().map(AiplatformTimeBandRuleAppService::toWireWindow).toList(),
                        command.effectiveFrom()));
        return opened.stream().map(AiplatformTimeBandRuleAppService::toRule).toList();
    }

    /**
     * 原子改窗（透传 aiplatform）——单调用关当前版本全部行＋开新版本（同事务，中途任一
     * 守卫失败两段都不动）：寻址行须当前版本成员（版本任一行皆可达——关行在版本级非单行），
     * 被关版本全部行落 effectiveTo＝新版本起点（保留其原开行操作者，不被改写）；新版本窗口
     * 清单整版本替换语义（清单即终态）。回执 {@code {closed, opened}} 双段。
     */
    public AiplatformTimeBandRuleRewindowResponse rewindow(
            String id, AiplatformTimeBandRuleRewindowCommand command) {
        var receipt = aiplatformClient.rewindowTimeBandRule(
                id,
                new AiplatformTimeBandRuleRewindowWireRequest(
                        command.windows().stream().map(AiplatformTimeBandRuleAppService::toWireWindow).toList(),
                        command.effectiveFrom()));
        return new AiplatformTimeBandRuleRewindowResponse(
                receipt.closed().stream().map(AiplatformTimeBandRuleAppService::toRule).toList(),
                receipt.opened().stream().map(AiplatformTimeBandRuleAppService::toRule).toList());
    }

    /**
     * 停用（透传 aiplatform）——即时生效：关当前版本全部行（effectiveTo＝现在）不开新版本——
     * 此后该通道 ts 无窗口命中＝不归带。对未生效的预发布版本停用＝钳到自身起点成空区间
     * （从未生效）。回执＝被关版本全部行（停用不接新行，被关行是唯一落点）。
     */
    public List<AiplatformTimeBandRuleResponse> deactivate(String id) {
        return aiplatformClient.deactivateTimeBandRule(id).stream()
                .map(AiplatformTimeBandRuleAppService::toRule)
                .toList();
    }

    private static AiplatformTimeBandWindowWireRequest toWireWindow(AiplatformTimeWindowCommand window) {
        return new AiplatformTimeBandWindowWireRequest(
                window.band(), window.dayMask(), window.startMinute(), window.endMinute());
    }

    private static AiplatformTimeBandRuleResponse toRule(AiplatformTimeBandRuleWireResponse wire) {
        return new AiplatformTimeBandRuleResponse(
                wire.id(), wire.provider(), wire.band(), wire.bandName(),
                wire.dayMask(), wire.dayMaskName(), wire.startMinute(), wire.endMinute(),
                wire.effectiveFrom(), wire.effectiveTo(),
                wire.operatorId(), wire.operatorName());
    }
}
