package com.alumni.management.chat.config;

import java.security.Principal;
import java.time.Instant;
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

    @Autowired
    private com.alumni.management.chat.service.ChatService chatService;

    private static final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, Instant> userLastSeen = new ConcurrentHashMap<>();

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
                PresenceEvent presence = new PresenceEvent(email, "ONLINE", chatClock.nowInstant());
                dispatchPresenceToPartners(email, presence);
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
                    Instant now = chatClock.nowInstant();
                    userLastSeen.put(email, now);
                    PresenceEvent presence = new PresenceEvent(email, "OFFLINE", now);
                    dispatchPresenceToPartners(email, presence);
                }
            }
        }
    }

    private void dispatchPresenceToPartners(String email, PresenceEvent presence) {
        try {
            messagingTemplate.convertAndSendToUser(email, "/queue/presence", presence);
            Set<String> partners = chatService.getConversationPartners(email);
            if (partners != null) {
                for (String partner : partners) {
                    try {
                        messagingTemplate.convertAndSendToUser(partner, "/queue/presence", presence);
                    } catch (Exception e) {
                        log.error("Failed to send presence to partner {}: {}", partner, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch presence for {}: {}", email, e.getMessage());
        }
    }

    public static boolean isUserOnline(String email) {
        if (email == null) return false;
        Set<String> sessions = userSessions.get(email.trim().toLowerCase());
        return sessions != null && !sessions.isEmpty();
    }

    public static Instant getLastSeen(String email) {
        if (email == null) return null;
        return userLastSeen.get(email.trim().toLowerCase());
    }
}
