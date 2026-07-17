package com.clockshore.aooai.service;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.entity.QueryRewriteResult;
import com.clockshore.aooai.enumType.Prompt;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryRewriteService {


    private final ChatClient.Builder chatClientBuilder;

    private final JsonUtil jsonUtil;

    public QueryRewriteResult rewrite(String query){

        String result = chatClientBuilder.build()
                .prompt()
                .system(Prompt.USER_QUERY_REWRITE.getPrompt())
                .user(query)
                .call()
                .content();


        return jsonUtil.fromJson(result, new TypeReference<QueryRewriteResult>() {});
    }
}
