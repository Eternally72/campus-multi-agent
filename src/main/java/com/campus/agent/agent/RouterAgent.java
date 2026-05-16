package com.campus.agent.agent;

import org.springframework.stereotype.Component;

@Component
public class RouterAgent {

    public AgentType route(String message) {
        String text = message.toLowerCase();
        if (containsAny(text, "总结", "概括", "摘要", "课件")) {
            return AgentType.MATERIAL_SUMMARY;
        }
        if (containsAny(text, "计划", "复习", "备考", "规划", "时间表")) {
            return AgentType.STUDY_PLAN;
        }
        if (containsAny(text, "待办", "提醒", "deadline", "ddl", "作业")) {
            return AgentType.STUDY_PLAN;
        }
        if (containsAny(text, "校园", "教务", "图书馆", "宿舍", "食堂", "空教室")) {
            return AgentType.CAMPUS_AFFAIRS;
        }
        return AgentType.COURSE_QA;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
