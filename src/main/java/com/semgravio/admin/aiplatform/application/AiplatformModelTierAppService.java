package com.semgravio.admin.aiplatform.application;

import java.util.List;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierCreateCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformModelTierUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierResponse;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformModelTierTraceResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelTierCreateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelTierTraceWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelTierUpdateWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformModelTierWireResponse;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 模型档位域 BFF 应用服务——档位（语义键）全生命周期管理（issue #111 B2，
 * spec #101 B 批，镜像 aiplatform #367 档位 CRUD＋留痕）：档位行的清单/单行/创建/更新/
 * 删除/变更留痕读面。档位配置归 aiplatform business.project 上下文——admin 不持业务
 * 逻辑、不持档位数据（与模型供应商域独立 controller 面，通道/档位分权）。
 *
 * <p>admin 作为 BFF 忠实透传（spec #101）：寻址键 {@code tierKey} 为字符串路径参数
 * ——透传不解析不预校验（寻址键即绑定引用腿），未知键由 provider 判 404 PRJ_066
 * （数字业务码 4066；留痕读面不要求行存在——未知键空清单 200 非 404）。创建守卫
 * （语义键已存在 409 PRJ_068、字段不完整 400 PRJ_070、所属通道不存在 400 PRJ_071）、
 * 删除被绑定引用 409 PRJ_073 等错误经 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException} 原样透传，本层不 try/catch。PUT 缺省肢
 * （enabled null＝保持现值；displayName null＝终态清空回落语义键）在 wire 请求体上
 * 保真（可空肢不臆造默认值）。「勾选入库」（远端清单建议入库条）复用创建写口：
 * modelName 填所选条目 modelId、语义键人工命名。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformModelTierAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformModelTierAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 档位清单读面（透传 aiplatform）——全部档位行按语义键序：语义键/实际模型名/所属
     * 通道/显示名/启用态＋最近写者。清单 {@code enabled=true} 键集＝绑定下拉数据源与
     * 改档写口即时校验放行值域（agent-configs 面的 modelId 语义即档位键）。种子三档：
     * flash/pro（火山 GA 对齐版）/doubao-flagship（备选档）。
     */
    public List<AiplatformModelTierResponse> listTiers() {
        return aiplatformClient.listTiers().stream()
                .map(AiplatformModelTierAppService::toTier)
                .toList();
    }

    /**
     * 档位单行读面（透传 aiplatform）——该档位行。{@code tierKey} 字符串路径参数原样
     * 出站（寻址键即绑定引用腿——flash/pro/doubao-flagship 这类运营短名，不解析不预
     * 校验），未知键由 provider 判 404 PRJ_066（数字业务码 4066）。
     */
    public AiplatformModelTierResponse getTier(String tierKey) {
        return toTier(aiplatformClient.getTier(tierKey));
    }

    /**
     * 档位创建（透传 aiplatform）——新建档位：语义键随体（已存在 409 PRJ_068——语义键
     * 唯一，改既有档走 PUT）；所属通道必须已有行（PRJ_071——先建通道再挂档位）；实际
     * 模型名必填（PRJ_070）；显示名可空（＝回落语义键）；enabled 缺省 true。创建即落痕
     * （old 全 null）；建行即可被绑定。「勾选入库」复用本写口（远端清单建议入库条：
     * modelName 填所选条目 modelId、语义键人工命名）。
     */
    public AiplatformModelTierResponse createTier(AiplatformModelTierCreateCommand command) {
        return toTier(aiplatformClient.createTier(
                new AiplatformModelTierCreateWireRequest(
                        command.tierKey(), command.providerKey(), command.modelName(),
                        command.displayName(), command.enabled())));
    }

    /**
     * 档位更新（透传 aiplatform）——PUT 全量语义：providerKey/modelName/displayName 终态
     * 替换（displayName null/空白＝清显示名回落语义键）；enabled 缺省＝保持现值（终态
     * 字段不因缺省翻转——停用档换模型名/换通道不被意外复启）。缺省肢在 wire 请求体上
     * 保真（可空肢 null 在场＝保持现值）。换通道/升模型版本/停启用皆此一处改指向——
     * 全部引用自动跟上（改指向下一轮命令构建即生效，装配动态查库）。值面无动＝幂等
     * 回执不落痕；回滚＝留痕旧值快照写回（行在＝PUT、行已删＝按旧值重建行）。
     */
    public AiplatformModelTierResponse updateTier(
            String tierKey, AiplatformModelTierUpdateCommand command) {
        return toTier(aiplatformClient.updateTier(tierKey,
                new AiplatformModelTierUpdateWireRequest(
                        command.providerKey(), command.modelName(),
                        command.displayName(), command.enabled())));
    }

    /**
     * 档位删除（透传 aiplatform）——删除档位行；仍被智能体运营配置绑定
     * （model_id/routing_strong_model_id 任一＝该语义键）即 409 PRJ_073（先改绑或清空
     * 绑定再删——悬空引用不留静默坑；临时下线用停用不删行）。删成落痕（new 全 null，
     * 留痕跨行存活）。操作者经框架透传头落痕（缺头由 provider 域面守卫 PRJ_034 拦，
     * admin 不拦）。
     */
    public void deleteTier(String tierKey) {
        aiplatformClient.deleteTier(tierKey);
    }

    /**
     * 档位变更留痕读面（透传 aiplatform）——该档位全部变更留痕按时间倒序（最近先）。
     * 每痕＝一次实际变更（幂等重写不落痕）：前后全量值快照＋操作者＋时刻。创建痕 old
     * 全 null、删除痕 new 全 null、newDisplayName null＝本次清空回落语义键；append-only
     * 跨行存活——行删除后留痕仍可读（留痕读面不要求行存在：空清单＝从未有行，行读面
     * 负责 404 判别）。回滚＝把某痕旧值快照写回（行在＝PUT、行已删＝按旧值重建行，
     * 皆一次新变更留新痕）。
     */
    public List<AiplatformModelTierTraceResponse> listTraces(String tierKey) {
        return aiplatformClient.listTierTraces(tierKey).stream()
                .map(AiplatformModelTierAppService::toTrace)
                .toList();
    }

    /** wire 行 → 北向行（五要素＋最近写者——逐字段直通，BFF 零推导）。 */
    private static AiplatformModelTierResponse toTier(AiplatformModelTierWireResponse wire) {
        return new AiplatformModelTierResponse(
                wire.tierKey(), wire.providerKey(), wire.modelName(), wire.displayName(),
                wire.enabled(), wire.operatorId(), wire.operatorName(), wire.updatedAt());
    }

    /** wire 留痕行 → 北向留痕行（old/new 四件对快照——逐字段直通，BFF 零推导）。 */
    private static AiplatformModelTierTraceResponse toTrace(AiplatformModelTierTraceWireResponse wire) {
        return new AiplatformModelTierTraceResponse(
                wire.id(),
                wire.oldProviderKey(), wire.oldModelName(), wire.oldDisplayName(), wire.oldEnabled(),
                wire.newProviderKey(), wire.newModelName(), wire.newDisplayName(), wire.newEnabled(),
                wire.operatorId(), wire.operatorName(), wire.operatedAt());
    }
}
