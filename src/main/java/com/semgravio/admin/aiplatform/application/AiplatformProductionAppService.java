package com.semgravio.admin.aiplatform.application;

import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionActionCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionSupplementCommand;
import com.semgravio.admin.aiplatform.application.dto.command.AiplatformProductionUpdateCommand;
import com.semgravio.admin.aiplatform.application.dto.response.AiplatformProductionHandoffResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionActionWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionHandoffWireResponse;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionSupplementWireRequest;
import com.semgravio.admin.aiplatform.application.dto.wire.AiplatformProductionUpdateWireRequest;
import com.semgravio.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 生产承接域 BFF 应用服务（issue #107，spec #102 A 批 A4——provider aiplatform
 * #338 交付承接）：承接详情（两态＋留痕时间线）＋信息整组更新（联系人/下一步/进展/
 * 预计开始）＋推进（待安排生产→生产中两态唯一边）＋「需用户补充」置位/清零（前台六态
 * 盖帽标志），四端点。
 *
 * <p>admin 作为 BFF：调接口 + DTO 转换，不持业务逻辑、不持承接数据、不记业务审计
 * （append-only 留痕归 provider）。下游错误已由 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException}（provider 信封原样透传），本层不再 try/catch。
 * 两态推进边、依据必填/超长、字段清空语义、置位语义全由 provider 承担，本层不解释
 * 不预判不复算。推进/更新操作者身份经框架 {@code RequestContext}→
 * {@code X-User-Id/X-User-Name} 自动透传（AppService 不经手），provider 落痕留痕行
 * （缺头 PRJ_059 拒）。</p>
 *
 * <p>项目唯一约束如实文档化：承接<strong>挂项目只建一次</strong>（全生命周期一行，无
 * 终态概念——承接是交付过程的状态承接页）、<strong>创建无人工口</strong>（开工判定
 * 系统触发建档，创建行操作者＝system/开工判定）、admin 不提供创建/删除端点。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformProductionAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformProductionAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 承接详情（透传 aiplatform）——承接全量面：承接字段（可空段如实 null）＋两态
     * （1=待安排生产 2=生产中）＋动作留痕时间线（新→旧，append-only——创建行＝开工
     * 判定系统操作者）。无承接记录 404 PRJ_055（开工判定未触发）。
     */
    public AiplatformProductionHandoffResponse getHandoff(String projectId) {
        return toHandoff(aiplatformClient.getProductionHandoff(projectId));
    }

    /**
     * 信息整组更新（透传 aiplatform）——联系人/下一步/进展/预计开始四段整组替换：
     * 未录入段传 null 即清空（预计开始时间清空＝回待安排呈现）；状态不动（留痕行
     * from=to）。字段合法性归 provider 聚合守卫（PRJ_057/058/059）。
     */
    public AiplatformProductionHandoffResponse update(String projectId,
            AiplatformProductionUpdateCommand command) {
        return toHandoff(aiplatformClient.updateProductionHandoff(projectId,
                new AiplatformProductionUpdateWireRequest(command.contact(), command.nextStep(),
                        command.progressNote(), command.estimatedStartAt(), command.basis())));
    }

    /**
     * 推进（透传 aiplatform）——两态唯一边「待安排生产 → 生产中」：生产中是稳态，
     * 再推进 409 PRJ_056；依据必填留痕（PRJ_057）。守卫归 provider 裁决。
     */
    public AiplatformProductionHandoffResponse advance(String projectId,
            AiplatformProductionActionCommand command) {
        return toHandoff(aiplatformClient.advanceProductionHandoff(projectId,
                new AiplatformProductionActionWireRequest(command.basis())));
    }

    /**
     * 「需用户补充」置位/清零（透传 aiplatform）——前台六态盖帽标志：置位语义归后台
     * 人工判定（何时补/补什么走对话区沟通），provider 只落标志。项目不存在 404
     * PRJ_001。回执 data＝null（无回读面）。
     */
    public void setNeedsUserSupplement(String projectId, AiplatformProductionSupplementCommand command) {
        aiplatformClient.setProductionNeedsUserSupplement(projectId,
                new AiplatformProductionSupplementWireRequest(command.required()));
    }

    /** wire 承接 → 北向承接：字段逐位直通＋留痕行逐位直通（两态/动作 Integer code 直通）。 */
    private static AiplatformProductionHandoffResponse toHandoff(AiplatformProductionHandoffWireResponse wire) {
        return new AiplatformProductionHandoffResponse(
                wire.id(), wire.projectId(), wire.orderId(),
                wire.status(), wire.statusName(),
                wire.contact(), wire.nextStep(), wire.progressNote(),
                wire.estimatedStartAt(), wire.createdAt(),
                wire.transitions() == null ? null : wire.transitions().stream()
                        .map(row -> new AiplatformProductionHandoffResponse.TransitionResponse(
                                row.action(), row.actionName(),
                                row.fromStatus(), row.fromStatusName(),
                                row.toStatus(), row.toStatusName(),
                                row.basis(), row.operatorId(), row.operatorName(), row.createdAt()))
                        .toList());
    }
}
