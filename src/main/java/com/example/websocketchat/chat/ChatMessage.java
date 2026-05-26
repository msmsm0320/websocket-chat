package com.example.websocketchat.chat;

import java.time.Instant;
import java.util.List;

public record ChatMessage(
        MessageType type,
        String sender,
        String content,
        Instant sentAt,
        List<String> users
) {

    public ChatMessage withSentAt(Instant sentAt) {
        return new ChatMessage(type, sender, content, sentAt, users);
    }

    public enum MessageType {
        JOIN,
        CHAT,
        LEAVE,
        SYSTEM,
        USERS
    }
}