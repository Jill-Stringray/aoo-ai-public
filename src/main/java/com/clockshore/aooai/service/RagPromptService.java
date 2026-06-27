package com.clockshore.aooai.service;

import org.springframework.stereotype.Service;

@Service
public class RagPromptService {

    public String buildPrompt(String context) {
        return """
                以下内容来自企业知识库。

                请优先依据知识库内容回答用户问题。

                如果知识库中不存在相关信息，
                请明确说明：
                "知识库中没有找到相关内容。"

                ===== 知识库内容 =====

                %s
                """
                .formatted(context);
    }

}