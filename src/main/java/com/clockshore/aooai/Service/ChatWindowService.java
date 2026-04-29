package com.clockshore.aooai.Service;

import com.clockshore.aooai.Entity.Conversation;
import com.clockshore.aooai.Mapper.ChatResourceMapper;
import com.clockshore.aooai.enums.ConversationStatus;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
public class ChatWindowService {

    @Resource
    private ChatResourceMapper chatResourceMapper;

    @Resource
    private SnowflakeService snowflakeService;

    private ConversationStatus status;

    @Transactional
    @Validated
    public Conversation createConversation(@NotNull Long uid, @NotBlank String firstMsg){
        String conversationKey = snowflakeService.nextIdStr();

        //builder构造
        Conversation cvs = Conversation.builder()
                .userId(uid)
                .conversationKey(conversationKey)
                .status(ConversationStatus.ACTIVE.getValue())
                .build();

        chatResourceMapper.insert(cvs);

        return cvs;
    }

}
