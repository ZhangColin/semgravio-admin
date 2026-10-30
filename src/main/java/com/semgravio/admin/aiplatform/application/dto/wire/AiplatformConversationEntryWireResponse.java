package com.semgravio.admin.aiplatform.application.dto.wire;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * aiplatform 对话史条目的 wire 镜像——与 aiplatform {@code ConversationEntryResponse}
 * （#89 水合载荷）字段同构：对话面全量（用户发言/智能体回复/问答卡/问答作答/收尾卡/平台轻引导），
 * 过程明细（解说段/动作卡流水）不在其中。
 *
 * <p>{@code kind} 为 <strong>Integer code</strong>（1=用户发言 2=智能体回复 3=问答卡 4=问答作答
 * 5=收尾卡 6=平台轻引导 7=报价卡）+ {@code kindName} 中文名随行（aiplatform#186 已落——2026-09-15 对照
 * provider 源码 f01d984 核实：backoffice 出口已配 *Name，BFF 透传 provider 值、不做本地映射；
 * 2026-09-23 对照 provider 800ad9e（#203 报价感知）补 {@code quote} 载荷肢）。
 * {@code sessionSource}/{@code sessionSourceName} 会话来源对（2026-10-08 对照 provider #298 补——
 * 2=设计会话〔每设计物一个独立会话流〕；判据＝收尾卡携 {@code drafts} 稿清单〔首产/改稿/定稿收尾卡〕；
 * null＝主会话缺省——含作用域改稿发言〔无结构标记不标注〕；设计会话完整稿本在智能体会话存储
 * 〔与主会话存储不同构的降级口径〕；用户面前端可忽略此字段）。provider 读口按结构标记推导
 * （设计收尾卡 ⇔ 2），null 是缺省态非标识值位——照透不臆造默认值。
 * {@code question}/{@code closing}/{@code quote} 为事件载荷 JSON 原样（{@code Map} 逐字透传、
 * BFF 不解读）；{@code closing} 是版本详情锚定的权威事实（#88 同载荷）；{@code quote} 为
 * 报价卡载荷（仅事件＋订单引用，不含金额——视镜语义，aiplatform ADR-0017）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformConversationEntryWireResponse(

        /** 条目标识（Long；provider Long→JSON string 出口，反序列化回 Long） */
        Long id,

        /** 条目类型（1=用户发言 2=智能体回复 3=问答卡 4=问答作答 5=收尾卡 6=平台轻引导 7=报价卡） */
        Integer kind,

        /** 条目类型中文名（provider 出口提供，aiplatform#186） */
        String kindName,

        /**
         * 会话来源标识（provider #298）：2=设计会话（每设计物一个——判据＝收尾卡携 drafts 稿清单：
         * 首产/改稿/定稿收尾卡）、null=主会话缺省（作用域改稿发言无结构标记不标注、设计会话
         * 完整稿本在智能体会话存储——存储不同构降级口径）。后台对话史按来源分组或标注；
         * 用户面前端可忽略。
         */
        Integer sessionSource,

        /** 会话来源中文名（provider 出口提供，sessionSource=null 时同 null） */
        String sessionSourceName,

        /** 锚定编码 run（用户发言/收尾卡等无 run 语境为 null） */
        String runId,

        /** 正文（用户发言/智能体回复/作答内容；问答卡/收尾卡为 null） */
        String text,

        /** 问答卡载荷原样（question-raised 事件；answered=false 即挂起待答；非问答卡为 null） */
        Map<String, Object> question,

        /** 收尾卡载荷原样（run-finish 收口扩载，#88 同载荷；非收尾卡为 null） */
        Map<String, Object> closing,

        /** 用户发言随带的圈注附件 JSON 数组（#97 圈注 B 档；无附件为 null/空） */
        List<Map<String, Object>> attachments,

        /** 报价卡载荷原样（仅事件＋订单引用，不含金额——视镜语义 ADR-0017；非报价卡为 null） */
        Map<String, Object> quote,

        /** 问答卡是否已作答（非问答卡恒 false） */
        boolean answered,

        /** 写入时点（id 升序 = 对话序） */
        LocalDateTime at
) {
}
