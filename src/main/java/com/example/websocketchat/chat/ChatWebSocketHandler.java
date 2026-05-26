package com.example.websocketchat.chat;

import com.example.websocketchat.chat.ChatMessage.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private static final int MAX_CONNECTIONS_PER_IP = 1;
    private static final int MAX_MESSAGES_PER_SECOND = 5;


    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> sendersBySessionId = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> sessionIdsByIp = new ConcurrentHashMap<>();
    private final Map<String, String> ipBySessionId = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> messageTimestampsBySessionId = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ip = getClientIp(session);

        Set<String> sessionIds = sessionIdsByIp.computeIfAbsent(
                ip,
                key -> ConcurrentHashMap.newKeySet()
        );

        if (sessionIds.size() >= MAX_CONNECTIONS_PER_IP) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Too many connections from same IP"));
            return;
        }

        sessionIds.add(session.getId());
        ipBySessionId.put(session.getId(), ip);

        sessions.add(session);
        sendTo(session, systemMessage("채팅방에 연결되었습니다."));
        sendUserList();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (isRateLimitExceeded(session)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Too many messages"));
            return;
        }

        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);

        if (chatMessage.type() == MessageType.JOIN) {
            String sender = normalize(chatMessage.sender(), "\uC775\uBA85");
            String senderWithIp = sender + " (" + getClientIp(session) + ")";
            sendersBySessionId.put(session.getId(), senderWithIp);
            broadcast(systemMessage(senderWithIp + "\uB2D8\uC774 \uC785\uC7A5\uD588\uC2B5\uB2C8\uB2E4."));
            sendUserList();
            return;
        }

        if (chatMessage.type() == MessageType.CHAT) {
            String sender = sendersBySessionId.getOrDefault(session.getId(), normalize(chatMessage.sender(), "\uC775\uBA85"));
            String content = normalize(chatMessage.content(), "");

            if (!content.isBlank()) {
                broadcast(new ChatMessage(MessageType.CHAT, sender, content, Instant.now(), null));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        String ip = ipBySessionId.remove(session.getId());

        if (ip != null) {
            Set<String> sessionIds = sessionIdsByIp.get(ip);

            if (sessionIds != null) {
                sessionIds.remove(session.getId());

                if (sessionIds.isEmpty()) {
                    sessionIdsByIp.remove(ip);
                }
            }
        }

        messageTimestampsBySessionId.remove(session.getId());

        String sender = sendersBySessionId.remove(session.getId());

        if (sender != null) {
            broadcast(systemMessage(sender + "님이 퇴장했습니다."));
        }

        sendUserList();
    }

    private void broadcast(ChatMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message.withSentAt(Instant.now()));

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendPayload(session, payload);
            }
        }
    }

    private void sendUserList() throws IOException {
        List<String> users = new ArrayList<>(sendersBySessionId.values());
        users.sort(Comparator.naturalOrder());
        broadcast(new ChatMessage(MessageType.USERS, "system", null, Instant.now(), users));
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
        return new ChatMessage(MessageType.SYSTEM, "system", content, Instant.now(), null);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String getClientIp(WebSocketSession session) {
        String forwardedFor = session.getHandshakeHeaders().getFirst("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = session.getRemoteAddress();

        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

    private boolean isRateLimitExceeded(WebSocketSession session) {
        long now = System.currentTimeMillis();

        List<Long> timestamps = messageTimestampsBySessionId.computeIfAbsent(
                session.getId(),
                key -> new ArrayList<>()
        );

        synchronized (timestamps) {
            timestamps.removeIf(timestamp -> now - timestamp > 1000);

            if (timestamps.size() >= MAX_MESSAGES_PER_SECOND) {
                return true;
            }

            timestamps.add(now);
            return false;
        }
    }
}