package com.clockshore.aooai.service;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.dto.message.ConversationVO;
import com.clockshore.aooai.entity.Conversation;
import com.clockshore.aooai.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;


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
}
