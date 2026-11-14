package com.alumni.management.notification.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.MessageType;
import com.alumni.management.notification.config.FirebaseConfig;
import com.alumni.management.notification.dto.DeviceRegistrationRequest;
import com.alumni.management.notification.entity.DeviceToken;
import com.alumni.management.notification.repository.DeviceTokenRepository;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    public static final String CHAT_CHANNEL_ID = "chat_messages";

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirebaseConfig firebaseConfig;

    private String maskToken(String token) {
        if (token == null) return "null";
        int len = token.length();
        if (len <= 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(len - 4);
    }

    @Transactional
    public DeviceToken registerDeviceToken(String authenticatedEmail, DeviceRegistrationRequest request) {
        if (authenticatedEmail == null || authenticatedEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Authenticated user email must not be empty");
        }
        if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Device token must not be empty");
        }

        String userEmail = authenticatedEmail.trim().toLowerCase();
        String tokenStr = request.getToken().trim();
        String deviceType = (request.getDeviceType() != null && !request.getDeviceType().trim().isEmpty())
                ? request.getDeviceType().trim().toUpperCase()
                : "ANDROID";
        String deviceId = request.getDeviceId() != null ? request.getDeviceId().trim() : null;

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(userEmail);
        User user = userOpt.orElse(null);

        Optional<DeviceToken> existingOpt = deviceTokenRepository.findByToken(tokenStr);
        DeviceToken deviceToken;

        if (existingOpt.isPresent()) {
            deviceToken = existingOpt.get();
            // Account isolation: If token belonged to another user previously, reassign to current authenticated user
            if (!userEmail.equalsIgnoreCase(deviceToken.getUserEmail())) {
                log.info("🔄 Reassigning device token {} from {} to {}",
                        maskToken(tokenStr), deviceToken.getUserEmail(), userEmail);
            }
            deviceToken.setUser(user);
            deviceToken.setUserEmail(userEmail);
            deviceToken.setDeviceType(deviceType);
            if (deviceId != null) {
                deviceToken.setDeviceId(deviceId);
            }
            deviceToken.setActive(true);
            deviceToken.setUpdatedAt(Instant.now());
        } else {
            deviceToken = new DeviceToken();
            deviceToken.setUser(user);
            deviceToken.setUserEmail(userEmail);
            deviceToken.setToken(tokenStr);
            deviceToken.setDeviceType(deviceType);
            deviceToken.setDeviceId(deviceId);
            deviceToken.setActive(true);
            deviceToken.setCreatedAt(Instant.now());
            deviceToken.setUpdatedAt(Instant.now());
        }

        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("📱 Device token registered successfully: userEmail={}, token={}", userEmail, maskToken(tokenStr));
        return saved;
    }

    @Transactional
    public void unregisterDeviceToken(String authenticatedEmail, String token) {
        if (authenticatedEmail == null || authenticatedEmail.trim().isEmpty()) {
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String userEmail = authenticatedEmail.trim().toLowerCase();
        String tokenStr = token.trim();

        deviceTokenRepository.deleteByUserEmailAndToken(userEmail, tokenStr);
        log.info("🗑️ Device token unregistered: userEmail={}, token={}", userEmail, maskToken(tokenStr));
    }

    @Transactional
    public void unregisterAllDevicesForUser(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.trim().isEmpty()) {
            return;
        }
        String userEmail = authenticatedEmail.trim().toLowerCase();
        deviceTokenRepository.deleteAllByUserEmail(userEmail);
        log.info("🗑️ All device tokens cleared for user: {}", userEmail);
    }

    public String generateMessagePreview(ChatMessage message) {
        if (message == null) return "New message";
        MessageType type = message.getType() != null ? message.getType() : MessageType.TEXT;

        switch (type) {
            case IMAGE:
                return "📷 Photo";
            case VIDEO:
                return "🎥 Video";
            case AUDIO:
                return "🎵 Voice message";
            case DOCUMENT:
                return "📄 Document";
            case TEXT:
            default:
                String content = message.getContent() != null ? message.getContent().trim() : "";
                if (content.length() > 150) {
                    return content.substring(0, 147) + "...";
                }
                return content.isEmpty() ? "New message" : content;
        }
    }

    public String getSenderDisplayName(String senderEmail) {
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            return "Alumni Connect";
        }
        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(senderEmail.trim());
            if (userOpt.isPresent() && userOpt.get().getName() != null && !userOpt.get().getName().trim().isEmpty()) {
                return userOpt.get().getName().trim();
            }
        } catch (Exception e) {
            log.warn("Could not look up sender display name: {}", e.getMessage());
        }
        // Fallback: extract username part of email
        int atIdx = senderEmail.indexOf('@');
        return atIdx > 0 ? senderEmail.substring(0, atIdx) : senderEmail;
    }

    @Async
    public CompletableFuture<Void> sendChatMessageNotificationAsync(ChatMessage message) {
        sendChatMessageNotification(message);
        return CompletableFuture.completedFuture(null);
    }

    public void sendChatMessageNotification(ChatMessage message) {
        if (message == null) return;

        String sender = message.getSender() != null ? message.getSender().trim().toLowerCase() : "";
        String receiver = message.getReceiver() != null ? message.getReceiver().trim().toLowerCase() : "";

        // Rules: No empty users, no self-notifications
        if (sender.isEmpty() || receiver.isEmpty() || sender.equalsIgnoreCase(receiver)) {
            return;
        }

        if (!firebaseConfig.isFirebaseAvailable()) {
            log.debug("ℹ️ Firebase not available or credentials missing. Skipping push notification for messageId={}", message.getId());
            return;
        }

        try {
            List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserEmailIgnoreCaseAndIsActiveTrue(receiver);
            if (deviceTokens.isEmpty()) {
                log.debug("ℹ️ No active FCM device tokens found for recipient: {}", receiver);
                return;
            }

            List<String> tokens = new ArrayList<>();
            for (DeviceToken dt : deviceTokens) {
                if (dt.getToken() != null && !dt.getToken().trim().isEmpty()) {
                    tokens.add(dt.getToken().trim());
                }
            }

            if (tokens.isEmpty()) {
                return;
            }

            String senderName = getSenderDisplayName(sender);
            String previewText = "You received a new message.";
            String conversationId = message.getConversationId() != null ? message.getConversationId() : "";
            String messageId = message.getId() != null ? message.getId() : "";
            String messageType = message.getType() != null ? message.getType().name() : "TEXT";
            String timestampStr = message.getTimestamp() != null ? message.getTimestamp().toString() : Instant.now().toString();

            // Construct payload data map
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("notificationType", "CHAT_MESSAGE");
            dataPayload.put("conversationId", conversationId);
            dataPayload.put("senderEmail", sender);
            dataPayload.put("senderName", senderName);
            dataPayload.put("receiverEmail", receiver);
            dataPayload.put("messageId", messageId);
            dataPayload.put("messageType", messageType);
            dataPayload.put("content", previewText);
            dataPayload.put("timestamp", timestampStr);

            // Construct Android Config for high priority and conversation grouping
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId(CHAT_CHANNEL_ID)
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .setSound("default")
                            .setTag(conversationId) // Group notifications by conversation
                            .build())
                    .build();

            com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(senderName)
                    .setBody(previewText)
                    .build();

            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(fcmNotification)
                    .putAllData(dataPayload)
                    .setAndroidConfig(androidConfig)
                    .build();

            log.info("🚀 Sending FCM push notification: receiver={}, sender={}, tokensCount={}",
                    receiver, sender, tokens.size());

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastMessage);

            log.info("📬 FCM Multicast result: {} success, {} failures",
                    response.getSuccessCount(), response.getFailureCount());

            // Clean up invalid or unregistered tokens
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse res = responses.get(i);
                    if (!res.isSuccessful()) {
                        String failedToken = tokens.get(i);
                        MessagingErrorCode errorCode = res.getException() != null ? res.getException().getMessagingErrorCode() : null;
                        log.warn("⚠️ FCM delivery failed for token {}: errorCode={}, message={}",
                                maskToken(failedToken), errorCode, res.getException() != null ? res.getException().getMessage() : "unknown");

                        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                            try {
                                deviceTokenRepository.deleteByToken(failedToken);
                                log.info("🧹 Cleaned up invalid/unregistered device token: {}", maskToken(failedToken));
                            } catch (Exception ex) {
                                log.error("Failed to delete invalid token from repository: {}", ex.getMessage());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("⚠️ Error sending FCM chat notification for messageId={}: {}", message.getId(), e.getMessage(), e);
            // Must not throw or crash chat flow
        }
    }
}
