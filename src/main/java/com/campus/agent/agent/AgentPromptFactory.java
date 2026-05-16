package com.campus.agent.agent;

import com.campus.agent.rag.RagSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentPromptFactory {

    public String systemPrompt(AgentType type) {
        return switch (type) {
            case COURSE_QA -> "你是课程问答 Agent。基于用户上传资料和校园学习场景回答，优先引用检索上下文；不知道时明确说明。";
            case STUDY_PLAN -> "你是学习规划 Agent。把目标拆成可执行任务，给出优先级、时间安排和待办建议。";
            case MATERIAL_SUMMARY -> "你是资料总结 Agent。输出结构化摘要、重点概念、易错点和复习问题。";
            case CAMPUS_AFFAIRS -> "你是校园事务 Agent。回答校园事务问题，涉及 MIS、教务等系统时提示用户需要后续接入正式工具。";
            case REFLECTION -> "你是 Reflection Agent。审查答案是否完整、是否过度推断，并给出更稳妥的最终表达。";
            case ROUTER -> "你是 Router Agent。识别用户意图并选择合适 Agent。";
        };
    }

    public String userPrompt(String message, List<RagSearchResult> references) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：\n").append(message).append("\n\n");
        if (references.isEmpty()) {
            builder.append("知识库检索结果：暂无可用上下文。\n");
        } else {
            builder.append("知识库检索结果：\n");
            for (int i = 0; i < references.size(); i++) {
                builder.append("[").append(i + 1).append("] ")
                    .append(references.get(i).content()).append("\n");
            }
        }
        builder.append("\n请用中文回答，结构清晰，避免编造。");
        return builder.toString();
    }
}
