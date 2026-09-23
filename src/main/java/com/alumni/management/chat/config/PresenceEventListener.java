package com.alumni.management.chat.config;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.alumni.management.chat.dto.PresenceEvent;

@Component
public class PresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatClock chatClock;

    private static final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> userLastSeen = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null && user.getName() != null) {
            String email = user.getName().trim().toLowerCase();
            String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
            if (sessionId == null) {
                sessionId = "session_" + System.currentTimeMillis();
            }

            Set<String> sessions = userSessions.computeIfAbsent(email, k -> ConcurrentHashMap.newKeySet());
            boolean wasOffline = sessions.isEmpty();
            sessions.add(sessionId);

            log.info("🟢 USER CONNECTED: email={}, sessionId={}, activeSessions={}", email, sessionId, sessions.size());

            if (wasOffline) {
                PresenceEvent presence = new PresenceEvent(email, "ONLINE", chatClock.now());
                try {
                    messagingTemplate.convertAndSend("/topic/presence", presence);
                } catch (Exception e) {
                    log.error("Failed to broadcast presence online: {}", e.getMessage());
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null && user.getName() != null) {
            String email = user.getName().trim().toLowerCase();
            String sessionId = event.getSessionId();

            Set<String> sessions = userSessions.get(email);
            if (sessions != null) {
                sessions.remove(sessionId);
                log.info("🔴 USER DISCONNECTED: email={}, sessionId={}, remainingSessions={}", email, sessionId, sessions.size());
                if (sessions.isEmpty()) {
                    userSessions.remove(email);
                    LocalDateTime now = chatClock.now();
                    userLastSeen.put(email, now);
                    PresenceEvent presence = new PresenceEvent(email, "OFFLINE", now);
                    try {
                        messagingTemplate.convertAndSend("/topic/presence", presence);
                    } catch (Exception e) {
                        log.error("Failed to broadcast presence offline: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public static boolean isUserOnline(String email) {
        if (email == null) return false;
        Set<String> sessions = userSessions.get(email.trim().toLowerCase());
        return sessions != null && !sessions.isEmpty();
    }

    public static LocalDateTime getLastSeen(String email) {
        if (email == null) return null;
        return userLastSeen.get(email.trim().toLowerCase());
    }
}
