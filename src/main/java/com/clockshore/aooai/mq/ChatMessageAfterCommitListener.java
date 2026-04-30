package com.clockshore.aooai.mq;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatMessageAfterCommitListener {

    private final ChatMessageEventPublisher chatMessageEventPublisher;

    public ChatMessageAfterCommitListener(ChatMessageEventPublisher chatMessageEventPublisher) {
        this.chatMessageEventPublisher = chatMessageEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageEvent event) {
        chatMessageEventPublisher.publish(event);
    }
}
