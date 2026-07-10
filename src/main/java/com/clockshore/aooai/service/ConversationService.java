package com.clockshore.aooai.service;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.message.ChatMessage;
import com.clockshore.aooai.dto.message.ConversationVO;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.entity.ConversationMessage;
import com.clockshore.aooai.mapper.ConversationMapper;
import com.clockshore.aooai.mapper.ConversationMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;

    private final ConversationMessageMapper conversationMessageMapper;


    //主键回填
    public Long createConversation(Long userId) {

        Conversation conversation = new Conversation();

        conversation.setUserId(userId);
        conversation.setTitle("新对话");

        conversationMapper.insert(conversation);

        return conversation.getId();
    }

    public List<ConversationVO> fetchConversationList(Long userId) {
        List<Conversation> conversations = conversationMapper.selectByUserId(userId);
        List<ConversationVO> conversationVOS = new ArrayList<>();
        for (Conversation conversation : conversations) {
            conversationVOS.add(ConversationVO.from(conversation));
        }

        return conversationVOS;
    }

    public List<ChatMessage> fetchConversationMessages(Long userId, Long conversationId){
        List<ConversationMessage> conversationMessages = conversationMessageMapper.selectByConversationIdAndUserId(conversationId,userId);
        List<ChatMessage> cm = new ArrayList<>();
        for(ConversationMessage conversationMessage : conversationMessages){
            cm.add(conversationMessageMapper.toChatMessage(conversationMessage));
        }
        return cm;
    }
}
