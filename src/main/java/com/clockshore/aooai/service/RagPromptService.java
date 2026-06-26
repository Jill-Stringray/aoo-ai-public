package com.clockshore.aooai.service;

import org.springframework.stereotype.Service;

@Service
public class RagPromptService {

    public String buildPrompt(
            String context,
            String question
    ) {
        return """
                你是一个知识库问答助手。

                请严格依据提供的知识回答问题。

                如果知识中不存在答案，请明确回答：
                "知识库中没有找到相关内容。"

                ===== 知识 =====
                %s

                ===== 问题 =====
                %s
                """
                .formatted(
                        context,
                        question
                );
    }
}