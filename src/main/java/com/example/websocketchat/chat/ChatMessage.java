package com.example.websocketchat.chat;

import java.time.Instant;

public record ChatMessage(
        MessageType type,
        String sender,
        String content,
        Instant sentAt
) {

    public ChatMessage withSentAt(Instant sentAt) {
        return new ChatMessage(type, sender, content, sentAt);
    }

    public enum MessageType {
        JOIN,
        CHAT,
        LEAVE,
        SYSTEM
    }
}
