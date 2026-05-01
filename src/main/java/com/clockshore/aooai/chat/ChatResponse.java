package com.clockshore.aooai.chat;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponse {

    private String reply;
    private String additionMsg;
    private Long conversationId;

    public ChatResponse() {
    }

    public ChatResponse(String reply,String additionMsg) {
        this.reply = reply;
        this.additionMsg = additionMsg;
    }

    public ChatResponse(String reply, String additionMsg, Long conversationId) {
        this.reply = reply;
        this.additionMsg = additionMsg;
        this.conversationId = conversationId;
    }

}
