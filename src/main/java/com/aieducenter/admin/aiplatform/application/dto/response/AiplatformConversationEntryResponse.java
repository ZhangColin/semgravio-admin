package com.aieducenter.admin.aiplatform.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 平台对话史条目——北向出口，逐字镜像 aiplatform {@code ConversationEntryResponse}
 * （#89 水合载荷），无增删字段、无换型（spec #62 忠实透传）。
 *
 * <p>{@code kind} 为 Integer code（1=用户发言 2=智能体回复 3=问答卡 4=问答作答 5=收尾卡
 * 6=平台轻引导 7=报价卡）+ {@code kindName} 中文名随行（aiplatform#186 已落，provider 出口提供、
 * BFF 透传不臆造映射）。{@code question}/{@code closing}/{@code quote} 为事件载荷 JSON 原样透传；
 * {@code quote} 为报价卡载荷（aiplatform #203 报价感知，视镜语义不含金额——ADR-0017）。</p>
 *
 * @since 0.1.0
 */
public record AiplatformConversationEntryResponse(

        /** 条目标识（Long；JSON string 出口） */
        Long id,

        /** 条目类型（1=用户发言 2=智能体回复 3=问答卡 4=问答作答 5=收尾卡 6=平台轻引导 7=报价卡） */
        Integer kind,

        /** 条目类型中文名（provider 出口提供，aiplatform#186） */
        String kindName,

        /** 锚定编码 run（无 run 语境为 null） */
        String runId,

        /** 正文（用户发言/智能体回复/作答内容；问答卡/收尾卡/报价卡为 null） */
        String text,

        /** 问答卡载荷原样（answered=false 即挂起待答；非问答卡为 null） */
        @Schema(description = "问答卡载荷（kind=3 question 携带，其余 kind 为 null）："
                + "question-raised 事件 payload 原样——{ runId, sessionId, engine, summary, "
                + "engineRef, data }；data.toolCalls=[{id,name,input}]（答复续跑重建所需的"
                + "待确认工具最小面）、data.questions=[{header,question,multiple,custom,"
                + "options[{label}]}]（问答卡投影，custom 恒 true 可自由输入）。"
                + "answered=false 即挂起待答，据此可重建可作答",
                example = "{\"runId\":\"r-01\",\"sessionId\":\"s-01\",\"engine\":\"agentscope\","
                        + "\"summary\":\"目标用户是谁？\",\"engineRef\":\"req-77\","
                        + "\"data\":{\"toolCalls\":[{\"id\":\"tc-1\",\"name\":\"ask_user\","
                        + "\"input\":{\"question\":\"目标用户是谁？\",\"header\":\"目标用户\"}}],"
                        + "\"questions\":[{\"header\":\"目标用户\",\"question\":\"目标用户是谁？\","
                        + "\"multiple\":false,\"custom\":true,"
                        + "\"options\":[{\"label\":\"个人开发者\"}]}]}}")
        Map<String, Object> question,

        /** 收尾卡载荷原样（版本详情锚定的权威事实，#88 同载荷；非收尾卡为 null） */
        @Schema(description = "收尾卡载荷（kind=5 closing 携带，其余 kind 为 null）：run-finish "
                + "收口扩载原样（#88 收尾卡服务端权威事实，版本详情锚定复用；schema 正本＝"
                + "aiplatform docs/spec/SSE事件清单·收口扩载节）——{ summary, prdChanged, prdNote?, "
                + "systemChanged, systemNote?, files=[{path,added,removed}], durationMs, "
                + "selfTest?{total}, version?, durationBreakdown{llmMs,toolsMs,selfTestMs,"
                + "closingMs,attempts[]} }（可缺省键＝该轮无该事实；durationBreakdown 为"
                + "平台分析口径，呈现可忽略）",
                example = "{\"summary\":\"完成切片：用户能登录\",\"prdChanged\":false,"
                        + "\"systemChanged\":true,\"systemNote\":\"起服了登录页\","
                        + "\"files\":[{\"path\":\"/src/App.jsx\",\"added\":40,\"removed\":0}],"
                        + "\"durationMs\":183420,\"selfTest\":{\"total\":3},"
                        + "\"version\":\"a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2\"}")
        Map<String, Object> closing,

        /** 用户发言随带的圈注附件 JSON 数组（无附件为 null/空） */
        @Schema(description = "圈注附件数组（kind=1 user 可携带，其余 kind 为 null，#97）："
                + "元素＝{attachmentType:\"annotation\", annotation:{kind, anchor:{selector?,"
                + "text?,region?{x,y,width,height}}, note?}}——结构化定位＋标注类型"
                + "（kind=select 点选锚定 / circle 拖框圈区域 / comment 历史兼容），"
                + "据此重建圈注 chip（非截图）",
                example = "[{\"attachmentType\":\"annotation\",\"annotation\":{\"kind\":\"select\","
                        + "\"anchor\":{\"selector\":\"#login-btn\",\"text\":\"登录\"}}}]")
        List<Map<String, Object>> attachments,

        /** 报价卡载荷原样（仅事件+订单引用，不含金额——视镜语义；非报价卡为 null） */
        @Schema(description = "报价卡载荷（kind=7 quote 携带，其余 kind 为 null，aiplatform #203）："
                + "仅事件＋订单引用——{ orderId（字符串，防 JS 精度丢失）, event（quoted=报价已出 / "
                + "repriced=报价已更新）}，不含金额（视镜非快照，aiplatform ADR-0017——金额/备注/"
                + "状态渲染时取订单当前态）",
                example = "{\"orderId\":\"3897654321098765432\",\"event\":\"quoted\"}")
        Map<String, Object> quote,

        /** 问答卡是否已作答（非问答卡恒 false） */
        boolean answered,

        /** 写入时点（id 升序 = 对话序） */
        LocalDateTime at
) {
}
