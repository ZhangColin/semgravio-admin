package com.aieducenter.admin.aiplatform.infrastructure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aieducenter.admin.aiplatform.application.AiplatformUpstreamException;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillAssignCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillInstallCommand;
import com.aieducenter.admin.aiplatform.application.dto.command.AiplatformSkillUpdateCommand;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillDetailResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillPrecheckResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillSummaryResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSkillUpdateTraceResponse;
import com.aieducenter.admin.aiplatform.application.dto.response.AiplatformSlotAssignmentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * aiplatform 技能管理域（清单/详情＋安装/停用⇄启用/卸载＋槽位指派读写＋显式更新＋更新留痕
 * 读面＋指派双头预检）的 wire 契约测试（对接 aiplatform #247 技能线 T1、#248 技能线 T2、
 * #249 技能线 T3、#250 技能线 T4、#255 指派双头预检，issue #80/#81/#82/#83/#90）。
 *
 * <p>契约事实（2026-09-23 对照 aiplatform 源码；2026-09-29 预检组对照 #255 落地实况
 * {@code BackofficeSkillController} /
 * {@code BackofficeSkillAppService} / {@code BackofficeSkillPrecheckAppService} /
 * {@code BackofficeSkillSummaryResponse} /
 * {@code BackofficeSkillDetailResponse} / {@code SkillInstallCommand} /
 * {@code SkillSlotAssignCommand} / {@code BackofficeSlotAssignmentResponse} /
 * {@code BackofficeSkillPrecheckResponse}（嵌套 {@code PrecheckHint}——hint 三 String 字段
 * 经 {@code textOrNull} 解析可 null）/
 * {@code SkillSource} / {@code SkillStatus} / {@code SkillSlot} / {@code SkillMessage} /
 * {@code ErrorCodePrefix} 核实）：</p>
 * <ul>
 *   <li>各端点均包 {@code ApiResponse<T>} 信封，按信封反序列化取 {@code .data()}；清单 data 为
 *       <strong>裸 List</strong>（有界目录不分页不过滤——对话史 List 直出同形；排序服务端定死：
 *       内置在前名称序、安装在后按来源包＋名称序），详情 data 为单对象。</li>
 *   <li>行字段：{@code id} String——<strong>opaque 串两形制</strong>（内置
 *       {@code builtin:<技能名>}／安装 TSID 十进制串），admin 字符串透传不解析不预校验；
 *       {@code source} Integer code（1=内置 2=安装 3=自产——自产行 sourcePackage/version＝
 *       固定虚拟值 {@code self}）+ {@code sourceName} 中文名、
 *       {@code status} Integer code（1=启用 2=停用）+ {@code statusName} 中文名随行；
 *       {@code sourcePackage}/{@code version} 仅安装行有值（内置 null）；{@code updateAvailable}
 *       来源包级新版标记（null＝未检查过，内置恒 null）。</li>
 *   <li>详情多 {@code frontmatter}（自由键值对象逐字透传——不做形状断言）＋{@code content}
 *       （frontmatter 剥离后正文全文，审核承载面）两字段，位于 statusName 与 operator 两肢之间
 *       （provider 字段序逐字镜像）。</li>
 *   <li>写口四端点（T2，#248）：安装 {@code POST /skills/install} 带 {@code {repoUrl,
 *       excludeDirs}} 命令体（excludeDirs 可缺省）、回执＝裸 List（与清单行共形——本次装入的
 *       条目清单）；停用/启用 {@code POST /skills/{id}/disable|enable} <strong>无请求体</strong>
 *       （null body）、回执＝翻转后清单行单行；卸载 {@code DELETE /skills/{id}}——HTTP
 *       <strong>DELETE 动词不可替代</strong>（get/post 即失败的 stub 钉死）、回执＝删除前终态
 *       清单行。</li>
 *   <li>槽位指派两端点（T3，#249）：GET/PUT {@code /skills/assignments/{slot}}——data 为
 *       {@code {slot, skills[]}}（槽位键＋与清单行共形的指派行集，<strong>含停用行</strong>——
 *       启停是可逆开关指派关系随行保留）；{@code slot} 为字符串路径参数（main/executor/subagent
 *       三把稳定键，admin 透传不预校验——未知键由 provider 判 404 SKL_010）；PUT 带
 *       {@code {skillIds}} 命令体（整包替换——清单即终态、空清单＝清空），HTTP <strong>PUT
 *       动词</strong>（框架 {@code OpenApiClient.put}），回执＝替换后读面（与 GET 同形）。</li>
 *   <li>显式更新＋版本留痕两端点（T4，#250）：POST {@code /skills/update} 带
 *       {@code {sourcePackage}} 命令体（取清单行原值回传，服务端再规范化），回执为单对象
 *       {@code {sourcePackage, fromVersion, toVersion, removedSkillNames, operatorId,
 *       operatorName, skills[]}}——远端未前进＝幂等回执（from==to、removedSkillNames 空、
 *       不落痕不写行）；GET {@code /skills/update-traces} 查询参数 {@code sourcePackage}
 *       <strong>可选绑定</strong>（spec #79：admin 不在绑定层前置拦截，空/缺由 provider 手工
 *       校验出 400 SKL_014），data 为裸 List（append-only 时间倒序、卸载不删留痕——与清单
 *       List 直出同形，不造分页）。</li>
 *   <li>指派双头预检（#255，技能域第 16 端点）：POST {@code /skills/assignments/{slot}/precheck}
 *       ——请求体复用 PUT 指派命令 {@code {skillIds}}（候选终态清单，与 PUT 同形——provider
 *       同款复用 {@code SkillSlotAssignCommand} 不另造命令型）；回执 {@code {executed, hints[]}}
 *       两件同读的<strong>两态纪律</strong>载体：executed=true＋hints 空＝确认无重叠、
 *       executed=true＋hints 非空＝发现重叠（每条四字段：skills 候选侧技能名清单、
 *       counterpart/overlap/resolution 三 String——经模型输出解析<strong>可 null</strong>，
 *       BFF 照透传不臆造兜底文案）、executed=false＝<strong>未执行</strong>（判定失败/
 *       超时/输出不可解析，hints 恒空——provider 侧 primitive boolean 恒非 null、hints
 *       构造器归一恒至少空表）；非阻断（不拦 PUT、组件故障不阻塞指派）；只读不留痕
 *       （无 SKL_009 操作者头要求——区别于 PUT 指派）。</li>
 *   <li>错误透传（数字业务码＝SKL 域码 7×1000＋序号）：SKL_001→7001（404，技能不存在——含
 *       未寻址内置名/TSID、畸形柄，provider 侧两形制柄单点分解裁决，admin 不预检）；
 *       SKL_002→7002（400 地址空）、SKL_003→7003（409 同源重复安装）、SKL_004→7004（502
 *       克隆失败）、SKL_005→7005（400 未解析到技能）、SKL_006→7006（400 SKILL.md 不合格）、
 *       SKL_007→7007（400 包内重名）、SKL_008→7008（409 有指派在身拒卸载）、SKL_009→7009
 *       （400 操作者缺头）、SKL_010→7010（404 职能槽位不存在）、SKL_011→7011（400 内置技能
 *       不可指派）、SKL_012→7012（404 来源包未安装——更新寻址已装来源包，未装先走安装）、
 *       SKL_013→7013（409 更新将移除有指派在身的技能——先解绑再更新）、SKL_014→7014
 *       （400 来源包标识不能为空——取清单行 sourcePackage 值）——fail-fast 失败族整体不入库，
 *       admin 不拦截不翻译。</li>
 * </ul>
 *
 * <p>本测试 <strong>不</strong> mock {@code AiplatformClient}（方法边界 mock 会绕过 wire 反序列化
 * ——已知反例），经 {@link AiplatformWireTestSupport} 子类化 {@code OpenApiClient} 仅替换 HTTP
 * 传输，真实 {@code TypeReference} 反序列化 + 真实 AppService 映射完整保留。</p>
 *
 * @since 0.1.0
 */
class AiplatformSkillClientContractTest {

    /** 内置行（provider classpath 合成）：builtin: 柄、来源 1、来源包/版本/操作者/新版标记全 null。 */
    private static final String BUILTIN_ROW = """
            {
              "id": "builtin:prd-writing",
              "name": "prd-writing",
              "description": "撰写或修订 PRD 时使用",
              "source": 1,
              "sourceName": "内置",
              "sourcePackage": null,
              "version": null,
              "status": 1,
              "statusName": "启用",
              "operatorId": null,
              "operatorName": null,
              "updateAvailable": null
            }
            """;

    /** 安装行（库行）：TSID 十进制串柄、来源 2、来源包/装时 commit/治理留痕/新版标记俱全。 */
    private static final String INSTALLED_ROW = """
            {
              "id": "3852300001111111",
              "name": "web-research",
              "description": "联网检索与网页抓取整理",
              "source": 2,
              "sourceName": "安装",
              "sourcePackage": "https://github.com/acme/agent-skills",
              "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
              "status": 2,
              "statusName": "停用",
              "operatorId": "700200",
              "operatorName": "运营·技能管理员",
              "updateAvailable": true
            }
            """;

    /**
     * aiplatform GET /api/backoffice/skills 的真实成功响应形状（信封 + 裸 List）：
     * 排序服务端定死的代表面——内置在前（名称序）、安装在后。
     */
    private static final String SKILL_LIST_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                %s,
                %s
              ],
              "requestId": "req-s1k2l",
              "errors": null
            }
            """.formatted(BUILTIN_ROW, INSTALLED_ROW);

    /**
     * aiplatform GET /api/backoffice/skills/{id} 的真实成功响应形状：安装行详情——frontmatter
     * 自由键值（含 name/description 之外的键，逐字透传）＋正文全文（多段形态代表面）。
     */
    private static final String SKILL_DETAIL_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "id": "3852300001111111",
                "name": "web-research",
                "description": "联网检索与网页抓取整理",
                "source": 2,
                "sourceName": "安装",
                "sourcePackage": "https://github.com/acme/agent-skills",
                "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "status": 1,
                "statusName": "启用",
                "frontmatter": {
                  "name": "web-research",
                  "description": "联网检索与网页抓取整理",
                  "license": "MIT",
                  "requires": ["network"]
                },
                "content": "# Web Research\\n\\n先检索后作答……\\n\\n## 步骤\\n\\n1. 拆解问题\\n2. 检索来源",
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "updateAvailable": false
              },
              "requestId": "req-s3d4e",
              "errors": null
            }
            """;

    /** 技能不存在（含未寻址内置名/TSID、畸形柄）：HTTP 404 + 信封 code=7001（SKL_001，域码 7）。 */
    private static final String SKL_001_ERROR_ENVELOPE = """
            {
              "code": 7001,
              "message": "技能不存在",
              "data": null,
              "requestId": "req-s5n0f",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/skills/install 的真实成功响应形状：回执＝本次装入的
     * 条目清单（裸 List，与清单行共形）——同源包两行（排除目录段命中其余条目的代表面）。
     */
    private static final String SKILL_INSTALL_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "id": "3852300002222222",
                  "name": "web-research",
                  "description": "联网检索与网页抓取整理",
                  "source": 2,
                  "sourceName": "安装",
                  "sourcePackage": "https://github.com/acme/agent-skills",
                  "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                  "status": 1,
                  "statusName": "启用",
                  "operatorId": "700200",
                  "operatorName": "运营·技能管理员",
                  "updateAvailable": null
                },
                {
                  "id": "3852300003333333",
                  "name": "prd-writing",
                  "description": "撰写或修订 PRD 时使用",
                  "source": 2,
                  "sourceName": "安装",
                  "sourcePackage": "https://github.com/acme/agent-skills",
                  "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                  "status": 1,
                  "statusName": "启用",
                  "operatorId": "700200",
                  "operatorName": "运营·技能管理员",
                  "updateAvailable": null
                }
              ],
              "requestId": "req-s7i9a",
              "errors": null
            }
            """;

    /**
     * 启停/卸载回执的真实成功响应形状：清单行单行（停用/启用＝翻转后行；卸载＝删除前终态——
     * provider 对两动作共形回执，此处取停用态行作代表面）。
     */
    private static final String SKILL_ACTION_RECEIPT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data":
                {
                  "id": "3852300001111111",
                  "name": "web-research",
                  "description": "联网检索与网页抓取整理",
                  "source": 2,
                  "sourceName": "安装",
                  "sourcePackage": "https://github.com/acme/agent-skills",
                  "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                  "status": 2,
                  "statusName": "停用",
                  "operatorId": "700200",
                  "operatorName": "运营·技能管理员",
                  "updateAvailable": true
                },
              "requestId": "req-s9a0c",
              "errors": null
            }
            """;

    private static final String INSTALLED_SKILL_ID = "3852300001111111";

    /** 指派目标柄二号（executor 槽指派行集的第二行——整包替换命令体的另一目标）。 */
    private static final String SECOND_INSTALLED_SKILL_ID = "3852300003333333";

    /** 职能槽位键（SkillSlot 稳定键三把之一）：字符串路径参数透传，admin 不预校验。 */
    private static final String SLOT_KEY = "executor";

    /**
     * aiplatform GET/PUT /api/backoffice/skills/assignments/{slot} 的真实成功响应形状：
     * {@code {slot, skills[]}}——指派行集与清单行共形且<strong>含停用行</strong>（启停是可逆开关、
     * 指派关系随行保留——「指派了但已停用」实态的代表面）；GET 读面与 PUT 回执同形。
     */
    private static final String SLOT_ASSIGNMENT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "slot": "executor",
                "skills": [
                  {
                    "id": "3852300001111111",
                    "name": "web-research",
                    "description": "联网检索与网页抓取整理",
                    "source": 2,
                    "sourceName": "安装",
                    "sourcePackage": "https://github.com/acme/agent-skills",
                    "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                    "status": 1,
                    "statusName": "启用",
                    "operatorId": "700200",
                    "operatorName": "运营·技能管理员",
                    "updateAvailable": false
                  },
                  {
                    "id": "3852300003333333",
                    "name": "prd-writing",
                    "description": "撰写或修订 PRD 时使用",
                    "source": 2,
                    "sourceName": "安装",
                    "sourcePackage": "https://github.com/acme/agent-skills",
                    "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                    "status": 2,
                    "statusName": "停用",
                    "operatorId": "700200",
                    "operatorName": "运营·技能管理员",
                    "updateAvailable": null
                  }
                ]
              },
              "requestId": "req-s11t3",
              "errors": null
            }
            """;

    /** 未知槽位：HTTP 404 + 信封 code=7010（SKL_010，域码 7）。 */
    private static final String SKL_010_ERROR_ENVELOPE = """
            {
              "code": 7010,
              "message": "职能槽位不存在",
              "data": null,
              "requestId": "req-s13l0",
              "errors": null
            }
            """;

    /** 内置柄不可指派：HTTP 400 + 信封 code=7011（SKL_011，域码 7）。 */
    private static final String SKL_011_ERROR_ENVELOPE = """
            {
              "code": 7011,
              "message": "内置技能不可指派（内置随平台发版，装配合成按配置挂载，无需指派）",
              "data": null,
              "requestId": "req-s15l1",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/skills/assignments/{slot}/precheck 的真实成功响应形状
     * （发现重叠代表面）：executed=true＋hints 非空——首条四字段俱全（skills 单技能、
     * counterpart＝「工作协议」双头对照侧）、次条 counterpart/overlap 为 null（hint 三 String
     * 字段经模型输出解析可 null——BFF 照透传，null 容差代表面）。
     */
    private static final String PRECHECK_HINTS_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "executed": true,
                "hints": [
                  {
                    "skills": ["报告生成"],
                    "counterpart": "工作协议",
                    "overlap": "都在 PRD 产出环节做结构化写作，方法论高度重叠",
                    "resolution": "二选一或收窄 description"
                  },
                  {
                    "skills": ["web-research", "site-fetch"],
                    "counterpart": null,
                    "overlap": null,
                    "resolution": "可不指派其一"
                  }
                ]
              },
              "requestId": "req-s33p1",
              "errors": null
            }
            """;

    /**
     * 预检确认无重叠回执（executed=true＋hints 空表——provider 构造器归一恒至少空表，
     * 空表照常出 {@code []} 在场）。
     */
    private static final String PRECHECK_NO_OVERLAP_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "executed": true,
                "hints": []
              },
              "requestId": "req-s35p3",
              "errors": null
            }
            """;

    /**
     * 预检未执行回执（executed=false——判定失败/超时/输出不可解析的降级态，hints 恒空；
     * 与「确认无重叠」靠 executed 区分，BFF 不解释不重试）。
     */
    private static final String PRECHECK_NOT_EXECUTED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "executed": false,
                "hints": []
              },
              "requestId": "req-s37p5",
              "errors": null
            }
            """;


    /** 内置柄（opaque 两形制之一）：出站 URL 编码后 ':' → %3A（provider 侧 @PathVariable 解码还原）。 */
    private static final String BUILTIN_SKILL_HANDLE = "builtin:prd-writing";

    /** 更新目标来源包（清单行 sourcePackage 原值——规范化仓库地址身份，服务端再规范化）。 */
    private static final String UPDATE_SOURCE_PACKAGE = "https://github.com/acme/agent-skills";

    /**
     * aiplatform POST /api/backoffice/skills/update 的真实成功响应形状（远端已前进代表面）：
     * from→to 版本不同＋removedSkillNames 含被移除技能名（远端已删、无指派在身的行）＋更新后
     * 终态行集（与清单行共形：同名行原地翻新保留 id/状态/指派——TSID 柄稳定）。
     */
    private static final String SKILL_UPDATE_ADVANCED_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "sourcePackage": "https://github.com/acme/agent-skills",
                "fromVersion": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "toVersion": "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                "removedSkillNames": ["legacy-summary"],
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "skills": [
                  {
                    "id": "3852300001111111",
                    "name": "web-research",
                    "description": "联网检索与网页抓取整理",
                    "source": 2,
                    "sourceName": "安装",
                    "sourcePackage": "https://github.com/acme/agent-skills",
                    "version": "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                    "status": 1,
                    "statusName": "启用",
                    "operatorId": "700200",
                    "operatorName": "运营·技能管理员",
                    "updateAvailable": false
                  },
                  {
                    "id": "3852300004444444",
                    "name": "code-review",
                    "description": "代码评审时使用",
                    "source": 2,
                    "sourceName": "安装",
                    "sourcePackage": "https://github.com/acme/agent-skills",
                    "version": "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                    "status": 1,
                    "statusName": "启用",
                    "operatorId": "700200",
                    "operatorName": "运营·技能管理员",
                    "updateAvailable": false
                  }
                ]
              },
              "requestId": "req-s19u4",
              "errors": null
            }
            """;

    /**
     * aiplatform POST /api/backoffice/skills/update 的幂等回执形状（远端未前进）：from==to、
     * removedSkillNames 为空、skills 即原样行集——不落痕不写行（历史里没有噪音，spec #79
     * 用户故事 17）。
     */
    private static final String SKILL_UPDATE_IDEMPOTENT_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "sourcePackage": "https://github.com/acme/agent-skills",
                "fromVersion": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "toVersion": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "removedSkillNames": [],
                "operatorId": "700200",
                "operatorName": "运营·技能管理员",
                "skills": [
                  {
                    "id": "3852300001111111",
                    "name": "web-research",
                    "description": "联网检索与网页抓取整理",
                    "source": 2,
                    "sourceName": "安装",
                    "sourcePackage": "https://github.com/acme/agent-skills",
                    "version": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                    "status": 1,
                    "statusName": "启用",
                    "operatorId": "700200",
                    "operatorName": "运营·技能管理员",
                    "updateAvailable": false
                  }
                ]
              },
              "requestId": "req-s21u5",
              "errors": null
            }
            """;

    /**
     * aiplatform GET /api/backoffice/skills/update-traces 的真实成功响应形状：裸 List，
     * append-only 时间倒序（最近先——首条 operatedAt 晚于次条；装时版本经链条首个 from 回溯）。
     */
    private static final String SKILL_UPDATE_TRACES_ENVELOPE = """
            {
              "code": 0,
              "message": "ok",
              "data": [
                {
                  "sourcePackage": "https://github.com/acme/agent-skills",
                  "fromVersion": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                  "toVersion": "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5",
                  "operatorId": "700200",
                  "operatorName": "运营·技能管理员",
                  "operatedAt": "2026-09-22T14:30:00"
                },
                {
                  "sourcePackage": "https://github.com/acme/agent-skills",
                  "fromVersion": "0f1e2d3c4b5a6978f1e2d3c4b5a6978f1e2d3c4b",
                  "toVersion": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                  "operatorId": "700100",
                  "operatorName": "运营·平台管理员",
                  "operatedAt": "2026-09-10T09:05:00"
                }
              ],
              "requestId": "req-s23t6",
              "errors": null
            }
            """;

    /** 来源包未安装：HTTP 404 + 信封 code=7012（SKL_012，域码 7）。 */
    private static final String SKL_012_ERROR_ENVELOPE = """
            {
              "code": 7012,
              "message": "来源包未安装（更新寻址已装来源包，未装先走安装）",
              "data": null,
              "requestId": "req-s25n2",
              "errors": null
            }
            """;

    /** 更新将移除有指派在身的技能：HTTP 409 + 信封 code=7013（SKL_013，域码 7）。 */
    private static final String SKL_013_ERROR_ENVELOPE = """
            {
              "code": 7013,
              "message": "更新将移除有指派在身的技能，先解绑再更新",
              "data": null,
              "requestId": "req-s27r3",
              "errors": null
            }
            """;

    /** 来源包标识不能为空：HTTP 400 + 信封 code=7014（SKL_014，域码 7）。 */
    private static final String SKL_014_ERROR_ENVELOPE = """
            {
              "code": 7014,
              "message": "来源包标识不能为空（取清单行 sourcePackage 值）",
              "data": null,
              "requestId": "req-s29p4",
              "errors": null
            }
            """;

    // ========== 清单（零查询参数 + 裸 List 信封 + 两形制柄）==========

    @Test
    void given_skillCatalog_when_list_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_LIST_ENVELOPE, wireUrl);

        appService.list();

        // 出站 wire 形状逐字镜像 provider 契约：零查询参数——不分页（有界目录）、不过滤、
        // 排序服务端定死（spec #79：同形不造分页）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/skills");
    }

    @Test
    void given_skillListEnvelope_when_list_then_envelopeUnwrappedAndBothHandleFormsBound() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_LIST_ENVELOPE, new String[1]);

        List<AiplatformSkillSummaryResponse> skills = appService.list();

        // 信封正确拆开——证明 SKILL_LIST_TYPEREF 按 ApiResponse<List<…>> 裸 List 反序列化
        // （误用分页载体 typeref 会把 List 绑不进来）
        assertThat(skills).hasSize(2);

        // 首行：内置行——builtin: 柄原样到达（opaque 串，admin 不解析）、来源 1/内置、
        // 状态恒启用、来源包/版本/操作者/新版标记全 null 如实绑定（全局 Jackson 含 null）
        var builtin = skills.get(0);
        assertThat(builtin.id()).isEqualTo(BUILTIN_SKILL_HANDLE);
        assertThat(builtin.name()).isEqualTo("prd-writing");
        assertThat(builtin.description()).isEqualTo("撰写或修订 PRD 时使用");
        assertThat(builtin.source()).isEqualTo(1);
        assertThat(builtin.sourceName()).isEqualTo("内置");
        assertThat(builtin.sourcePackage()).isNull();
        assertThat(builtin.version()).isNull();
        assertThat(builtin.status()).isEqualTo(1);
        assertThat(builtin.statusName()).isEqualTo("启用");
        assertThat(builtin.operatorId()).isNull();
        assertThat(builtin.operatorName()).isNull();
        assertThat(builtin.updateAvailable()).isNull();

        // 次行：安装行——TSID 十进制串柄、来源 2/安装、来源包＋装时 commit、停用态＋
        // 治理留痕两肢、新版标记 true
        var installed = skills.get(1);
        assertThat(installed.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(installed.source()).isEqualTo(2);
        assertThat(installed.sourceName()).isEqualTo("安装");
        assertThat(installed.sourcePackage()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(installed.version()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(installed.status()).isEqualTo(2);
        assertThat(installed.statusName()).isEqualTo("停用");
        assertThat(installed.operatorId()).isEqualTo("700200");
        assertThat(installed.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(installed.updateAvailable()).isTrue();
    }

    // ========== 详情（opaque 柄出站 + frontmatter/正文全文）==========

    @Test
    void given_builtinHandle_when_getDetail_then_wireUrlCarriesEncodedOpaqueHandle() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, wireUrl);

        appService.getDetail(BUILTIN_SKILL_HANDLE);

        // 内置柄原样出站（字符串透传不解析不预校验——spec #79）；':' 经 URL 编码为 %3A
        // （provider 侧 @PathVariable 解码还原，同账号 externalId '|' 编码先例）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/builtin%3Aprd-writing");
    }

    @Test
    void given_tsidHandle_when_getDetail_then_wireUrlMirrorsProvider() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, wireUrl);

        appService.getDetail(INSTALLED_SKILL_ID);

        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/" + INSTALLED_SKILL_ID);
    }

    @Test
    void given_skillDetailEnvelope_when_getDetail_then_metadataFrontmatterAndContentBound() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithStubTransport(
                SKILL_DETAIL_ENVELOPE, new String[1]);

        AiplatformSkillDetailResponse detail = appService.getDetail(INSTALLED_SKILL_ID);

        // 元数据与清单行同形
        assertThat(detail.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(detail.source()).isEqualTo(2);
        assertThat(detail.sourceName()).isEqualTo("安装");
        assertThat(detail.version()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");

        // frontmatter 自由键值对象逐字透传：name/description 之外的键（license/requires）
        // 原样到达，不做形状断言（spec #79）；数组值保持原形
        assertThat(detail.frontmatter())
                .containsEntry("name", "web-research")
                .containsEntry("description", "联网检索与网页抓取整理")
                .containsEntry("license", "MIT")
                .containsKey("requires");
        assertThat(detail.frontmatter().get("requires")).isEqualTo(List.of("network"));

        // 正文全文（frontmatter 剥离后）原样到达——审核承载面，不经任何截断/改写
        assertThat(detail.content()).startsWith("# Web Research");
        assertThat(detail.content()).contains("\n\n## 步骤\n\n1. 拆解问题\n2. 检索来源");
        assertThat(detail.updateAvailable()).isFalse();
    }

    // ========== 错误透传（SKL_001 数字业务码，不映射）==========

    @Test
    void given_skl001ErrorEnvelope_when_getDetail_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.skillAppServiceWithErrorTransport(
                404, SKL_001_ERROR_ENVELOPE, new String[1]);

        // 忠实透传（spec #79）：HTTP 404 + 数字业务码 7001（SKL_001＝域码 7×1000＋1）+ provider
        // message 原样——畸形/未寻址柄由 provider 裁决，admin 不预检不解释
        assertThatThrownBy(() -> appService.getDetail("builtin:no-such-skill"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(7001);
                    assertThat(upstream.getMessage()).isEqualTo("技能不存在");
                });
    }

    // ========== 安装（POST /install 带命令体 + 回执与清单行共形）==========

    @Test
    void given_installCommand_when_install_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillWriteAppServiceWithStubTransport(
                SKILL_INSTALL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        appService.install(new AiplatformSkillInstallCommand(
                "https://github.com/acme/agent-skills.git", List.of("deprecated", "in-progress")));

        // 出站 wire 形状逐字镜像 provider 契约：POST /api/backoffice/skills/install，命令体
        // {repoUrl, excludeDirs} 两字段——逐字镜像 SkillInstallCommand，无增删
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/skills/install");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactlyInAnyOrder("repoUrl", "excludeDirs");
        assertThat(body.path("repoUrl").asText()).isEqualTo("https://github.com/acme/agent-skills.git");
        assertThat(body.path("excludeDirs").get(0).asText()).isEqualTo("deprecated");
        assertThat(body.path("excludeDirs").get(1).asText()).isEqualTo("in-progress");
    }

    @Test
    void given_installReceiptEnvelope_when_install_then_receiptConformsToSummaryRows() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillWriteAppServiceWithStubTransport(
                SKILL_INSTALL_RECEIPT_ENVELOPE, wireUrl, wireBody);

        List<AiplatformSkillSummaryResponse> installed = appService.install(
                new AiplatformSkillInstallCommand("https://github.com/acme/agent-skills", null));

        // excludeDirs 可缺省（null 出站在场——全局 Jackson 含 null，provider 侧 null＝不排除）
        assertThat(wireBody[0]).contains("\"excludeDirs\":null");

        // 回执＝本次装入的条目清单（与清单行共形）：裸 List 信封正确拆开、两行全字段绑定——
        // 安装行形态（来源 2、启用态、装者留痕、新版标记未检查过 null）
        assertThat(installed).hasSize(2);
        var first = installed.get(0);
        assertThat(first.id()).isEqualTo("3852300002222222");
        assertThat(first.name()).isEqualTo("web-research");
        assertThat(first.source()).isEqualTo(2);
        assertThat(first.sourceName()).isEqualTo("安装");
        assertThat(first.sourcePackage()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(first.version()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(first.status()).isEqualTo(1);
        assertThat(first.statusName()).isEqualTo("启用");
        assertThat(first.operatorId()).isEqualTo("700200");
        assertThat(first.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(first.updateAvailable()).isNull();
        var second = installed.get(1);
        assertThat(second.id()).isEqualTo("3852300003333333");
        assertThat(second.name()).isEqualTo("prd-writing");
        assertThat(second.status()).isEqualTo(1);
    }

    // ========== 停用⇄启用（POST {id}/disable|enable，无请求体）==========

    @Test
    void given_tsidHandle_when_disableAndEnable_then_wireUrlMirrorsProviderAndBodyEmpty() {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillWriteAppServiceWithStubTransport(
                SKILL_ACTION_RECEIPT_ENVELOPE, wireUrl, wireBody);

        appService.disable(INSTALLED_SKILL_ID);
        // 启停无请求体（provider 端只读路径参数）：URL 镜像 POST /{id}/disable、body 记 null
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/" + INSTALLED_SKILL_ID + "/disable");
        assertThat(wireBody[0]).isNull();

        var disabled = appService.disable(INSTALLED_SKILL_ID);
        // 回执＝翻转后清单行单行（停用态＋最近动作者留痕）
        assertThat(disabled.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(disabled.status()).isEqualTo(2);
        assertThat(disabled.statusName()).isEqualTo("停用");
        assertThat(disabled.operatorId()).isEqualTo("700200");

        appService.enable(INSTALLED_SKILL_ID);
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/" + INSTALLED_SKILL_ID + "/enable");
        assertThat(wireBody[0]).isNull();
    }

    // ========== 卸载（DELETE 动词不可替代 + 删除前终态回执）==========

    @Test
    void given_uninstall_when_invoked_then_deleteVerbOnly_stubsForGetOrPostFail() {
        String[] wireUrl = new String[1];
        // get/post 即失败的 stub（issue #81 验收钉死）：卸载若误用 GET/POST 出站，AssertionError
        // 立刻炸测试——DELETE 是唯一可接受动词
        var appService = AiplatformWireTestSupport.skillDeleteOnlyAppServiceWithStubTransport(
                SKILL_ACTION_RECEIPT_ENVELOPE, wireUrl);

        var removed = appService.uninstall(INSTALLED_SKILL_ID);

        // 出站 DELETE /api/backoffice/skills/{id}（无请求体），回执＝删除前终态清单行
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/" + INSTALLED_SKILL_ID);
        assertThat(removed.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(removed.status()).isEqualTo(2);
        assertThat(removed.statusName()).isEqualTo("停用");
    }

    // ========== 错误族透传（SKL_002–SKL_009 数字业务码，不映射）==========

    @Test
    void given_sklErrorFamily_when_writeActions_then_passThroughWithoutMapping() {
        // 安装 fail-fast 失败族（002–007＋缺头 009）：HTTP 状态/数字码/message 均取 provider 原值
        assertSkillWriteErrorPassthrough(400, 7002, "安装仓库地址不能为空", "install");
        assertSkillWriteErrorPassthrough(409, 7003,
                "该技能仓库已安装过，不能重复安装（同源去重，更新走显式更新）", "install");
        assertSkillWriteErrorPassthrough(502, 7004, "技能仓库克隆失败", "install");
        assertSkillWriteErrorPassthrough(400, 7005, "仓库未解析到任何技能（无 SKILL.md 或全部被排除）", "install");
        assertSkillWriteErrorPassthrough(400, 7006, "仓库内 SKILL.md 不合格（须含 name、description 与正文）", "install");
        assertSkillWriteErrorPassthrough(400, 7007, "同一仓库内存在重名技能", "install");
        // 操作者缺头守卫（009）：启停同受域面守卫拦截（admin 不拦——透传断言即证据）
        assertSkillWriteErrorPassthrough(400, 7009, "操作者不能为空", "disable");
        // 卸载守卫（008）：有指派在身拒卸（先解绑再卸）
        assertSkillWriteErrorPassthrough(409, 7008, "技能有指派在身，先解绑再卸载", "uninstall");
    }

    /** 按端点族（install/disable/uninstall）断言错误信封透传：HTTP 状态＋数字业务码＋message 原样。 */
    private static void assertSkillWriteErrorPassthrough(
            int httpStatus, int envelopeCode, String message, String action) {
        String errorEnvelope = """
                {
                  "code": %d,
                  "message": "%s",
                  "data": null,
                  "requestId": "req-s7x%d",
                  "errors": null
                }
                """.formatted(envelopeCode, message, envelopeCode);
        var appService = AiplatformWireTestSupport.skillWriteAppServiceWithErrorTransport(
                httpStatus, errorEnvelope, new String[1]);

        var command = new AiplatformSkillInstallCommand("https://github.com/acme/agent-skills", null);
        // 按端点族触发出站（disable/enable/uninstall 同一守卫族，取代表面各一）
        var invocation = switch (action) {
            case "install" -> (Runnable) () -> appService.install(command);
            case "disable" -> (Runnable) () -> appService.disable(INSTALLED_SKILL_ID);
            case "uninstall" -> (Runnable) () -> appService.uninstall(INSTALLED_SKILL_ID);
            default -> throw new IllegalArgumentException("未知端点族: " + action);
        };

        assertThatThrownBy(invocation::run)
                .as("SKL_%d（数字业务码 %d）应经 %s 透传", envelopeCode - 7000, envelopeCode, action)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(httpStatus);
                    assertThat(upstream.envelopeCode()).isEqualTo(envelopeCode);
                    assertThat(upstream.getMessage()).isEqualTo(message);
                });
    }

    // ========== 槽位指派读面（GET /assignments/{slot}：slot 字符串路径参数透传 + 含停用行）==========

    @Test
    void given_slotKey_when_getSlotAssignments_then_wireUrlMirrorsProviderContract() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithStubTransport(
                SLOT_ASSIGNMENT_ENVELOPE, wireUrl, new String[1]);

        appService.getSlotAssignments(SLOT_KEY);

        // slot 为字符串路径参数透传（main/executor/subagent 三把稳定键），admin 不解析不预校验——
        // 未知键由 provider 判 404 SKL_010（spec #79）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/assignments/" + SLOT_KEY);
    }

    @Test
    void given_assignmentEnvelope_when_getSlotAssignments_then_slotKeyAndDisabledRowsBound() {
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithStubTransport(
                SLOT_ASSIGNMENT_ENVELOPE, new String[1], new String[1]);

        AiplatformSlotAssignmentResponse assignment = appService.getSlotAssignments(SLOT_KEY);

        // 信封正确拆开：data 为 {slot, skills[]} 单对象（slot 键原样回显）
        assertThat(assignment.slot()).isEqualTo(SLOT_KEY);
        assertThat(assignment.skills()).hasSize(2);

        // 次行为停用行——「指派了但已停用」实态如实绑定（启停是可逆开关、指派关系随行保留），
        // 全字段与清单行共形（停用行不参与装配合成的裁决在 provider，admin 不过滤不注解）
        var disabled = assignment.skills().get(1);
        assertThat(disabled.id()).isEqualTo(SECOND_INSTALLED_SKILL_ID);
        assertThat(disabled.status()).isEqualTo(2);
        assertThat(disabled.statusName()).isEqualTo("停用");
        assertThat(disabled.source()).isEqualTo(2);
        assertThat(disabled.sourcePackage()).isEqualTo("https://github.com/acme/agent-skills");
        assertThat(disabled.updateAvailable()).isNull();

        // 首行启用行——指派行集正常面
        var enabled = assignment.skills().get(0);
        assertThat(enabled.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(enabled.status()).isEqualTo(1);
        assertThat(enabled.statusName()).isEqualTo("启用");
    }

    // ========== 槽位指派整包替换（PUT /assignments/{slot}：{skillIds} 命令体 + 回执与 GET 同形）==========

    @Test
    void given_assignCommand_when_assignSlot_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithStubTransport(
                SLOT_ASSIGNMENT_ENVELOPE, wireUrl, wireBody);

        appService.assignSlot(SLOT_KEY, new AiplatformSkillAssignCommand(
                List.of(INSTALLED_SKILL_ID, SECOND_INSTALLED_SKILL_ID)));

        // 出站 wire 形状逐字镜像 provider 契约：PUT /api/backoffice/skills/assignments/{slot}，
        // 命令体 {skillIds} 单字段——逐字镜像 SkillSlotAssignCommand，无增删（整包替换：清单即终态）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/assignments/" + SLOT_KEY);
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("skillIds");
        assertThat(body.path("skillIds").get(0).asText()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(body.path("skillIds").get(1).asText()).isEqualTo(SECOND_INSTALLED_SKILL_ID);
    }

    @Test
    void given_emptySkillIds_when_assignSlot_then_emptyListSerializedNotOmitted() throws Exception {
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithStubTransport(
                SLOT_ASSIGNMENT_ENVELOPE, new String[1], wireBody);

        // 空清单＝清空该槽位（PUT 全量语义）：[] 照常出站在场——不是 null 不是缺省
        appService.assignSlot(SLOT_KEY, new AiplatformSkillAssignCommand(List.of()));

        assertThat(wireBody[0]).contains("\"skillIds\":[]");
    }

    @Test
    void given_assignReceiptEnvelope_when_assignSlot_then_receiptSameShapeAsGet() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithStubTransport(
                SLOT_ASSIGNMENT_ENVELOPE, wireUrl, new String[1]);

        AiplatformSlotAssignmentResponse receipt = appService.assignSlot(SLOT_KEY,
                new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID, SECOND_INSTALLED_SKILL_ID)));

        // 回执＝替换后读面（与 GET 同形）：slot 键＋指派行集（含停用行实态）——provider 换行集后
        // 重读 GET 同一视图，admin 同形镜像
        assertThat(receipt.slot()).isEqualTo(SLOT_KEY);
        assertThat(receipt.skills()).hasSize(2);
        assertThat(receipt.skills().get(0).id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(receipt.skills().get(1).statusName()).isEqualTo("停用");
    }

    // ========== 指派错误族透传（SKL_010/011/001/009 数字业务码，不映射）==========

    @Test
    void given_skl010UnknownSlot_when_getSlotAssignments_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithErrorTransport(
                404, SKL_010_ERROR_ENVELOPE, new String[1]);

        // 未知槽位由 provider 裁决（admin 不预检 slot 字符串路径参数）：HTTP 404 + 数字业务码
        // 7010（SKL_010＝域码 7×1000＋10）+ provider message 原样
        assertThatThrownBy(() -> appService.getSlotAssignments("unknown-slot"))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(404);
                    assertThat(upstream.envelopeCode()).isEqualTo(7010);
                    assertThat(upstream.getMessage()).isEqualTo("职能槽位不存在");
                });
    }

    @Test
    void given_sklErrorFamily_when_assignSlot_then_passThroughWithoutMapping() {
        // 指派失败族：内置柄不可指派（011）、未寻址/畸形 TSID（001）、操作者缺头（009）——
        // HTTP 状态/数字码/message 均取 provider 原值
        assertSlotAssignErrorPassthrough(400, SKL_011_ERROR_ENVELOPE, 7011,
                "内置技能不可指派（内置随平台发版，装配合成按配置挂载，无需指派）");
        assertSlotAssignErrorPassthrough(404, SKL_001_ERROR_ENVELOPE, 7001, "技能不存在");
        assertSlotAssignErrorPassthrough(400, """
                {
                  "code": 7009,
                  "message": "操作者不能为空",
                  "data": null,
                  "requestId": "req-s17o9",
                  "errors": null
                }
                """, 7009, "操作者不能为空");
    }

    /** 断言 PUT /assignments/{slot} 的错误信封透传：HTTP 状态＋数字业务码＋message 原样。 */
    private static void assertSlotAssignErrorPassthrough(
            int httpStatus, String errorEnvelope, int envelopeCode, String message) {
        var appService = AiplatformWireTestSupport.skillAssignAppServiceWithErrorTransport(
                httpStatus, errorEnvelope, new String[1]);

        assertThatThrownBy(() -> appService.assignSlot(SLOT_KEY,
                        new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID))))
                .as("数字业务码 %d 应经 PUT /assignments/{slot} 透传", envelopeCode)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(httpStatus);
                    assertThat(upstream.envelopeCode()).isEqualTo(envelopeCode);
                    assertThat(upstream.getMessage()).isEqualTo(message);
                });
    }

    // ========== 指派双头预检（POST /assignments/{slot}/precheck：与 PUT 同形命令体 + 两态纪律回执）==========

    @Test
    void given_precheckCommand_when_precheckSlot_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillPrecheckAppServiceWithStubTransport(
                PRECHECK_HINTS_ENVELOPE, wireUrl, wireBody);

        appService.precheckSlot(SLOT_KEY, new AiplatformSkillAssignCommand(
                List.of(INSTALLED_SKILL_ID, SECOND_INSTALLED_SKILL_ID)));

        // 出站 wire 形状逐字镜像 provider 契约：POST /api/backoffice/skills/assignments/{slot}/precheck，
        // 命令体复用 {skillIds}（候选终态清单，与 PUT 同形——provider 同款复用
        // SkillSlotAssignCommand 不另造命令型，无增删）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/assignments/" + SLOT_KEY + "/precheck");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("skillIds");
        assertThat(body.path("skillIds").get(0).asText()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(body.path("skillIds").get(1).asText()).isEqualTo(SECOND_INSTALLED_SKILL_ID);
    }

    @Test
    void given_hintsEnvelope_when_precheckSlot_then_hintRowsBoundWithNullTolerance() {
        var appService = AiplatformWireTestSupport.skillPrecheckAppServiceWithStubTransport(
                PRECHECK_HINTS_ENVELOPE, new String[1], new String[1]);

        AiplatformSkillPrecheckResponse precheck = appService.precheckSlot(SLOT_KEY,
                new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID, SECOND_INSTALLED_SKILL_ID)));

        // 信封正确拆开：executed=true（判定已执行）＋hints 非空＝发现方法论重叠——两态纪律的
        // 「重叠态」（与「确认无重叠」「未执行」靠 executed×hints 两件区分）
        assertThat(precheck.executed()).isTrue();
        assertThat(precheck.hints()).hasSize(2);

        // 首条四字段俱全：skills（候选侧技能名）、counterpart（「工作协议」——双头对照侧）、
        // overlap（重叠内容）、resolution（消解方向）
        var full = precheck.hints().get(0);
        assertThat(full.skills()).containsExactly("报告生成");
        assertThat(full.counterpart()).isEqualTo("工作协议");
        assertThat(full.overlap()).isEqualTo("都在 PRD 产出环节做结构化写作，方法论高度重叠");
        assertThat(full.resolution()).isEqualTo("二选一或收窄 description");

        // 次条 null 容差：hint 三 String 字段经模型输出解析可 null——如实绑定 null（不臆造
        // 兜底文案），skills 多技能清单原样到达
        var partial = precheck.hints().get(1);
        assertThat(partial.skills()).containsExactly("web-research", "site-fetch");
        assertThat(partial.counterpart()).isNull();
        assertThat(partial.overlap()).isNull();
        assertThat(partial.resolution()).isEqualTo("可不指派其一");
    }

    @Test
    void given_noOverlapEnvelope_when_precheckSlot_then_executedTrueWithEmptyHints() {
        var appService = AiplatformWireTestSupport.skillPrecheckAppServiceWithStubTransport(
                PRECHECK_NO_OVERLAP_ENVELOPE, new String[1], new String[1]);

        AiplatformSkillPrecheckResponse precheck = appService.precheckSlot(SLOT_KEY,
                new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID)));

        // 确认无重叠态：executed=true＋hints 空（provider 构造器归一恒至少空表——空表在场
        // 而非 null/缺省）
        assertThat(precheck.executed()).isTrue();
        assertThat(precheck.hints()).isEmpty();
    }

    @Test
    void given_notExecutedEnvelope_when_precheckSlot_then_executedFalseDistinctFromNoOverlap() {
        var appService = AiplatformWireTestSupport.skillPrecheckAppServiceWithStubTransport(
                PRECHECK_NOT_EXECUTED_ENVELOPE, new String[1], new String[1]);

        AiplatformSkillPrecheckResponse precheck = appService.precheckSlot(SLOT_KEY,
                new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID)));

        // 未执行态（降级）：executed=false＋hints 恒空——与「确认无重叠」（executed=true）
        // 靠 executed 区分；BFF 照透传不解释不重试、不伪装成无重叠
        assertThat(precheck.executed()).isFalse();
        assertThat(precheck.hints()).isEmpty();
    }

    // ========== 预检错误族透传（SKL_011/001/010 数字业务码，与 PUT 指派同源同码，不映射）==========

    @Test
    void given_sklErrorFamily_when_precheckSlot_then_passThroughWithoutMapping() {
        // 预检失败族与 PUT 指派同源同码（#87/#90）：内置柄不可指派（011）、未寻址/畸形 TSID
        // （001）、未知槽位（010）——HTTP 状态/数字码/message 均取 provider 原值
        assertPrecheckErrorPassthrough(400, SKL_011_ERROR_ENVELOPE, 7011,
                "内置技能不可指派（内置随平台发版，装配合成按配置挂载，无需指派）");
        assertPrecheckErrorPassthrough(404, SKL_001_ERROR_ENVELOPE, 7001, "技能不存在");
        assertPrecheckErrorPassthrough(404, SKL_010_ERROR_ENVELOPE, 7010, "职能槽位不存在");
    }

    /** 断言 POST /assignments/{slot}/precheck 的错误信封透传：HTTP 状态＋数字业务码＋message 原样。 */
    private static void assertPrecheckErrorPassthrough(
            int httpStatus, String errorEnvelope, int envelopeCode, String message) {
        var appService = AiplatformWireTestSupport.skillPrecheckAppServiceWithErrorTransport(
                httpStatus, errorEnvelope, new String[1]);

        assertThatThrownBy(() -> appService.precheckSlot(SLOT_KEY,
                        new AiplatformSkillAssignCommand(List.of(INSTALLED_SKILL_ID))))
                .as("SKL_%d（数字业务码 %d）应经 POST /assignments/{slot}/precheck 透传",
                        envelopeCode - 7000, envelopeCode)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(httpStatus);
                    assertThat(upstream.envelopeCode()).isEqualTo(envelopeCode);
                    assertThat(upstream.getMessage()).isEqualTo(message);
                });
    }

    // ========== 显式更新（POST /update 带命令体 + from/to＋removedSkillNames＋终态行集）==========

    @Test
    void given_updateCommand_when_update_then_wireUrlAndBodyMirrorProviderContract() throws Exception {
        String[] wireUrl = new String[1];
        String[] wireBody = new String[1];
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_ADVANCED_ENVELOPE, wireUrl, wireBody);

        appService.update(new AiplatformSkillUpdateCommand(UPDATE_SOURCE_PACKAGE));

        // 出站 wire 形状逐字镜像 provider 契约：POST /api/backoffice/skills/update，命令体
        // {sourcePackage} 单字段——逐字镜像 SkillUpdateCommand（取清单行原值回传，无增删）
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/skills/update");
        var body = new ObjectMapper().readTree(wireBody[0]);
        assertThat(body.fieldNames()).toIterable().containsExactly("sourcePackage");
        assertThat(body.path("sourcePackage").asText()).isEqualTo(UPDATE_SOURCE_PACKAGE);
    }

    @Test
    void given_advancedUpdateEnvelope_when_update_then_fromToRemovedAndFinalRowsBound() {
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_ADVANCED_ENVELOPE, new String[1], new String[1]);

        AiplatformSkillUpdateResponse receipt = appService.update(
                new AiplatformSkillUpdateCommand(UPDATE_SOURCE_PACKAGE));

        // 远端已前进回执：from→to 版本不同（from＝装时 commit、to＝更新时刻远端 HEAD）
        assertThat(receipt.sourcePackage()).isEqualTo(UPDATE_SOURCE_PACKAGE);
        assertThat(receipt.fromVersion()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(receipt.toVersion()).isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        // 被移除技能名（远端已删、无指派在身的行——确认更新动了什么）
        assertThat(receipt.removedSkillNames()).containsExactly("legacy-summary");
        // 操作者两肢（显式动作必留痕）
        assertThat(receipt.operatorId()).isEqualTo("700200");
        assertThat(receipt.operatorName()).isEqualTo("运营·技能管理员");
        // 更新后终态行集（清单行同形）：首行同名行原地翻新——id 稳定、版本换新、状态/指派保留
        assertThat(receipt.skills()).hasSize(2);
        var refreshed = receipt.skills().get(0);
        assertThat(refreshed.id()).isEqualTo(INSTALLED_SKILL_ID);
        assertThat(refreshed.name()).isEqualTo("web-research");
        assertThat(refreshed.version()).isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        assertThat(refreshed.status()).isEqualTo(1);
        // 次行新技能插入（启用）
        var inserted = receipt.skills().get(1);
        assertThat(inserted.id()).isEqualTo("3852300004444444");
        assertThat(inserted.name()).isEqualTo("code-review");
        assertThat(inserted.status()).isEqualTo(1);
        assertThat(inserted.statusName()).isEqualTo("启用");
    }

    @Test
    void given_idempotentUpdateEnvelope_when_update_then_fromEqualsToAndRemovedEmpty() {
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_IDEMPOTENT_ENVELOPE, new String[1], new String[1]);

        AiplatformSkillUpdateResponse receipt = appService.update(
                new AiplatformSkillUpdateCommand(UPDATE_SOURCE_PACKAGE));

        // 幂等回执（远端未前进）：from==to、removedSkillNames 空、行集即原样——不落痕不写行
        assertThat(receipt.fromVersion()).isEqualTo(receipt.toVersion());
        assertThat(receipt.removedSkillNames()).isEmpty();
        assertThat(receipt.skills()).hasSize(1);
        assertThat(receipt.skills().get(0).id()).isEqualTo(INSTALLED_SKILL_ID);
    }

    // ========== 更新错误族透传（SKL_004/005/007/009/012/013/014 数字业务码，不映射）==========

    @Test
    void given_sklErrorFamily_when_update_then_passThroughWithoutMapping() {
        // 更新失败族（issue #83 验收清单逐码）：克隆失败 004、远端未解析到技能 005、包内重名
        // 007、操作者缺头 009、来源包未安装 012、移除项有指派在身整体拒 013、来源包空 014——
        // HTTP 状态/数字码/message 均取 provider 原值
        assertSkillUpdateErrorPassthrough(502, 7004, "技能仓库克隆失败");
        assertSkillUpdateErrorPassthrough(400, 7005, "仓库未解析到任何技能（无 SKILL.md 或全部被排除）");
        assertSkillUpdateErrorPassthrough(400, 7007, "同一仓库内存在重名技能");
        assertSkillUpdateErrorPassthrough(400, 7009, "操作者不能为空");
        assertSkillUpdateErrorPassthrough(404, SKL_012_ERROR_ENVELOPE, 7012,
                "来源包未安装（更新寻址已装来源包，未装先走安装）");
        assertSkillUpdateErrorPassthrough(409, SKL_013_ERROR_ENVELOPE, 7013,
                "更新将移除有指派在身的技能，先解绑再更新");
        assertSkillUpdateErrorPassthrough(400, SKL_014_ERROR_ENVELOPE, 7014,
                "来源包标识不能为空（取清单行 sourcePackage 值）");
    }

    private static void assertSkillUpdateErrorPassthrough(int httpStatus, int envelopeCode, String message) {
        assertSkillUpdateErrorPassthrough(httpStatus, """
                {
                  "code": %d,
                  "message": "%s",
                  "data": null,
                  "requestId": "req-s31x%d",
                  "errors": null
                }
                """.formatted(envelopeCode, message, envelopeCode), envelopeCode, message);
    }

    /** 断言 POST /skills/update 的错误信封透传：HTTP 状态＋数字业务码＋message 原样。 */
    private static void assertSkillUpdateErrorPassthrough(
            int httpStatus, String errorEnvelope, int envelopeCode, String message) {
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithErrorTransport(
                httpStatus, errorEnvelope, new String[1]);

        assertThatThrownBy(() -> appService.update(new AiplatformSkillUpdateCommand(UPDATE_SOURCE_PACKAGE)))
                .as("SKL_%d（数字业务码 %d）应经 POST /skills/update 透传", envelopeCode - 7000, envelopeCode)
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(httpStatus);
                    assertThat(upstream.envelopeCode()).isEqualTo(envelopeCode);
                    assertThat(upstream.getMessage()).isEqualTo(message);
                });
    }

    // ========== 更新留痕读面（GET /update-traces：sourcePackage 可选绑定 + append-only 时间倒序裸 List）==========

    @Test
    void given_sourcePackage_when_updateTraces_then_wireUrlCarriesEncodedQueryParam() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.updateTraces(UPDATE_SOURCE_PACKAGE);

        // 查询参数透传（URL 编码——':' → %3A、'/' → %2F，签名按编码后值入签）；admin 不预检，
        // 空/缺由 provider 判 400 SKL_014（spec #79：不在绑定层前置拦截）
        assertThat(wireUrl[0]).isEqualTo(
                "http://stub-aiplatform/api/backoffice/skills/update-traces?sourcePackage="
                        + "https%3A%2F%2Fgithub.com%2Facme%2Fagent-skills");
    }

    @Test
    void given_nullSourcePackage_when_updateTraces_then_queryParamOmittedNotBlocked() {
        String[] wireUrl = new String[1];
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_TRACES_ENVELOPE, wireUrl, new String[1]);

        appService.updateTraces(null);

        // 可选绑定形态（provider 契约逐字镜像）：null 即缺省——查询参数整体省略（不发送），
        // 而非空串入签；admin 不在绑定层前置拦截
        assertThat(wireUrl[0]).isEqualTo("http://stub-aiplatform/api/backoffice/skills/update-traces");
    }

    @Test
    void given_updateTracesEnvelope_when_updateTraces_then_traceRowsBoundInDescOrder() {
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithStubTransport(
                SKILL_UPDATE_TRACES_ENVELOPE, new String[1], new String[1]);

        List<AiplatformSkillUpdateTraceResponse> traces = appService.updateTraces(UPDATE_SOURCE_PACKAGE);

        // 信封正确拆开——data 为裸 List（append-only 时间倒序，最近先；同清单 List 直出同形）
        assertThat(traces).hasSize(2);

        // 首条最近：from→to 版本链＋操作者两肢＋时刻（LocalDateTime ISO 反序列化）
        var latest = traces.get(0);
        assertThat(latest.sourcePackage()).isEqualTo(UPDATE_SOURCE_PACKAGE);
        assertThat(latest.fromVersion()).isEqualTo("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
        assertThat(latest.toVersion()).isEqualTo("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        assertThat(latest.operatorId()).isEqualTo("700200");
        assertThat(latest.operatorName()).isEqualTo("运营·技能管理员");
        assertThat(latest.operatedAt()).isEqualTo("2026-09-22T14:30:00");

        // 次条更早：版本链衔接（其 toVersion＝首条 fromVersion——append-only 链可回溯装时版本）
        var earlier = traces.get(1);
        assertThat(earlier.toVersion()).isEqualTo(latest.fromVersion());
        assertThat(earlier.operatorId()).isEqualTo("700100");
        assertThat(earlier.operatedAt()).isEqualTo("2026-09-10T09:05:00");
    }

    @Test
    void given_skl014ErrorEnvelope_when_updateTraces_then_passThroughWithoutMapping() {
        var appService = AiplatformWireTestSupport.skillUpdateAppServiceWithErrorTransport(
                400, SKL_014_ERROR_ENVELOPE, new String[1]);

        // 来源包空/缺由 provider 手工校验出 400 SKL_014（admin 不在绑定层前置拦截）：
        // HTTP 400 + 数字业务码 7014 + provider message 原样
        assertThatThrownBy(() -> appService.updateTraces(null))
                .isInstanceOf(AiplatformUpstreamException.class)
                .satisfies(e -> {
                    var upstream = (AiplatformUpstreamException) e;
                    assertThat(upstream.httpStatus()).isEqualTo(400);
                    assertThat(upstream.envelopeCode()).isEqualTo(7014);
                    assertThat(upstream.getMessage()).isEqualTo("来源包标识不能为空（取清单行 sourcePackage 值）");
                });
    }
}
