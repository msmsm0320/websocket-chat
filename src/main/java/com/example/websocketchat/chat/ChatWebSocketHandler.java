package com.example.websocketchat.chat;

import com.example.websocketchat.chat.ChatMessage.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> sendersBySessionId = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sendTo(session, systemMessage("채팅방에 연결되었습니다."));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);

        if (chatMessage.type() == MessageType.JOIN) {
            String sender = normalize(chatMessage.sender(), "익명");
            String senderWithIp = sender + " (" + getClientIp(session) + ")";
            sendersBySessionId.put(session.getId(), senderWithIp);
            broadcast(systemMessage(senderWithIp + "님이 입장했습니다."));
            return;
        }

        if (chatMessage.type() == MessageType.CHAT) {
            String sender = sendersBySessionId.getOrDefault(session.getId(), normalize(chatMessage.sender(), "익명"));
            String content = normalize(chatMessage.content(), "");

            if (!content.isBlank()) {
                broadcast(new ChatMessage(MessageType.CHAT, sender, content, Instant.now()));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        String sender = sendersBySessionId.remove(session.getId());

        if (sender != null) {
            broadcast(systemMessage(sender + "님이 퇴장했습니다."));
        }
    }

    private void broadcast(ChatMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message.withSentAt(Instant.now()));

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendPayload(session, payload);
            }
        }
    }

    private void sendTo(WebSocketSession session, ChatMessage message) throws IOException {
        if (session.isOpen()) {
            sendPayload(session, objectMapper.writeValueAsString(message.withSentAt(Instant.now())));
        }
    }

    private void sendPayload(WebSocketSession session, String payload) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private ChatMessage systemMessage(String content) {
        return new ChatMessage(MessageType.SYSTEM, "system", content, Instant.now());
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String getClientIp(WebSocketSession session) {
        InetSocketAddress remoteAddress = session.getRemoteAddress();

        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }
}
