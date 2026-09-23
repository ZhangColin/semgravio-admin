package com.aieducenter.admin.aiplatform.application;

import java.util.List;

import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillDetailWireResponse;
import com.aieducenter.admin.aiplatform.application.dto.wire.AiplatformSkillSummaryWireResponse;
import com.aieducenter.admin.aiplatform.infrastructure.AiplatformClient;
import org.springframework.stereotype.Service;

/**
 * aiplatform 技能管理域 BFF 应用服务——T1 读面：清单（内置＋安装同权，不分页）＋详情
 * （元数据＋frontmatter 全量＋正文全文，安装审核承载面），issue #80（aiplatform #247 技能线）。
 * 技能库归 aiplatform base.skills 上下文——admin 不持业务逻辑、不持技能数据。
 *
 * <p>admin 作为 BFF 忠实透传（spec #79）：<strong>零查询参数</strong>（清单不分页不过滤——
 * 技能库是有界目录，排序服务端定死：内置在前名称序、安装在后按来源包＋名称序，admin 不造
 * 分页）；详情寻址柄 {@code id} 为 opaque 串两形制（内置 {@code builtin:<技能名>}／安装 TSID
 * 十进制串）——字符串透传不解析不预校验，畸形/未寻址柄由 provider 判 404 SKL_001（数字业务码
 * 7001）。frontmatter 为自由键值对象逐字透传（不做形状断言）。读口无操作者语义（操作者留痕
 * 只在写口——T2+/T4 票），错误经 {@link AiplatformClient} 统一翻译为
 * {@link AiplatformUpstreamException} 原样透传，本层不 try/catch。</p>
 *
 * @since 0.1.0
 */
@Service
public class AiplatformSkillAppService {

    private final AiplatformClient aiplatformClient;

    public AiplatformSkillAppService(AiplatformClient aiplatformClient) {
        this.aiplatformClient = aiplatformClient;
    }

    /**
     * 技能清单（透传 aiplatform）——内置（classpath 合成）与安装技能同权呈现，空库时仍呈现
     * 内置技能；不分页不过滤（有界目录，spec #79 同形不造分页）。
     */
    public List<AiplatformSkillSummaryResponse> list() {
        return aiplatformClient.listSkills().stream()
                .map(AiplatformSkillAppService::toSummary)
                .toList();
    }

    /**
     * 技能详情（透传 aiplatform）——清单行同形元数据＋frontmatter 全量（解析态键值）＋正文
     * 全文（frontmatter 剥离后），所见即运行时注入面（安装审核承载面）。id 取清单行原值
     * （opaque 串两形制）原样出站，不解析不预校验。
     */
    public AiplatformSkillDetailResponse getDetail(String id) {
        return toDetail(aiplatformClient.getSkill(id));
    }

    private static AiplatformSkillSummaryResponse toSummary(AiplatformSkillSummaryWireResponse wire) {
        return new AiplatformSkillSummaryResponse(
                wire.id(), wire.name(), wire.description(),
                wire.source(), wire.sourceName(), wire.sourcePackage(), wire.version(),
                wire.status(), wire.statusName(),
                wire.operatorId(), wire.operatorName(), wire.updateAvailable());
    }

    private static AiplatformSkillDetailResponse toDetail(AiplatformSkillDetailWireResponse wire) {
        return new AiplatformSkillDetailResponse(
                wire.id(), wire.name(), wire.description(),
                wire.source(), wire.sourceName(), wire.sourcePackage(), wire.version(),
                wire.status(), wire.statusName(),
                wire.frontmatter(), wire.content(),
                wire.operatorId(), wire.operatorName(), wire.updateAvailable());
    }
}
