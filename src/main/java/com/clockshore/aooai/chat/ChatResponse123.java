package com.clockshore.aooai.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 聊天接口响应。注意：Jackson 按 getter 序列化，{@code reply} 必须在 getter 上标注
 * {@link JsonInclude.Include#ALWAYS}，否则在全局 NON_EMPTY 等策略下空串会被省略，前端会拿不到 {@code reply} 键。
 */
@Getter
@Setter
public class ChatResponse123 {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String reply = "";

    private String additionMsg;
    private Long conversationId;

    public ChatResponse123() {
    }

    public ChatResponse123(String reply, String additionMsg) {
        this.reply = reply == null ? "" : reply;
        this.additionMsg = additionMsg;
    }

    public ChatResponse123(String reply, String additionMsg, Long conversationId) {
        this.reply = reply == null ? "" : reply;
        this.additionMsg = additionMsg;
        this.conversationId = conversationId;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getReply() {
        return reply == null ? "" : reply;
    }

    public void setReply(String reply) {
        this.reply = reply == null ? "" : reply;
    }

}
