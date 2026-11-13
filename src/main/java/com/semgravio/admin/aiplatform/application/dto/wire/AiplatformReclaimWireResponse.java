package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;

/**
 * aiplatform 后台一键回收回执的 wire 镜像——与 aiplatform
 * {@code BackofficeReclaimResponse}（#490，issue #129 镜像）字段同构：
 * {@code POST /api/backoffice/projects/{id}/reclaim}（项目级——在飞 run 先级联停止
 * 〔承 #477〕→ 工作区销毁〔#485 内核〕→ prj_* 真删级联〔受理即删〕）与
 * {@code POST /api/backoffice/workspaces/{id}/reclaim}（工作区级——单台三清，项目行与其余
 * 资产不动）<strong>同形回执</strong>：逐面 {@code CLEAN}/{@code RESIDUE}/{@code UNKNOWN}
 * （工作区级项目面 {@code UNTOUCHED} 如实分示）＋{@code residueFree} 零残留判据。
 *
 * <p><strong>UNKNOWN 不算清</strong>：销毁未全成/探查未成时各面 UNKNOWN、residueFree＝false
 * ——回执如实红，不当成功静默；{@code RESIDUE} 如实红不报错重试（物理销毁尽力而为不阻断
 * 记录删除〔受理即删〕，残留归扫描轮/手动清扫收编）。{@code runsStopped}＝回收时有在飞
 * run 被级联停止（无在飞 false）。操作者两列＝透传头落痕回显值（缺头落空 NULL——照沙箱动作
 * 面口径，provider 不落强制守卫）；append-only 留痕行另落 {@code wsp_reclaim_actions}
 * （成功落痕、未成不留痕）。错误：项目不存在 404 PRJ_001；工作区不存在（含畸形 id）404
 * WSP_001；销毁自身失败 500（未成不留痕，可重试）——原样透传不做映射。</p>
 *
 * @param scope         回收范围：PROJECT / WORKSPACE
 * @param projectId     项目级＝回收的项目；工作区级＝所属项目引用（无所属 null——在册失联裁决
 *                      面〔#488〕与 fork）
 * @param workspaceId   回收的工作区（三清对象）
 * @param runsStopped   在飞 run 级联停止与否（无在飞 false）
 * @param container     容器面：CLEAN / RESIDUE / UNKNOWN
 * @param volume        卷面：同上
 * @param registryRow   注册表行面：同上
 * @param projectRow    项目行面：项目级 CLEAN / RESIDUE / UNKNOWN；工作区级 UNTOUCHED
 * @param residueFree   零残留判据（scope 内受检面全 CLEAN；任一面 RESIDUE/UNKNOWN 即 false）
 * @param operatorId    操作者标识（admin 侧；缺透传头为 null——落空口径）
 * @param operatorName  操作者昵称（同上）
 * @param reclaimedAt   回收时刻
 */
public record AiplatformReclaimWireResponse(
        String scope,
        String projectId,
        String workspaceId,
        boolean runsStopped,
        String container,
        String volume,
        String registryRow,
        String projectRow,
        boolean residueFree,
        String operatorId,
        String operatorName,
        LocalDateTime reclaimedAt) {
}
