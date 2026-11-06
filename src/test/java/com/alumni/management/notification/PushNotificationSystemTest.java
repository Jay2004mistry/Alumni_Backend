package com.alumni.management.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.MessageStatus;
import com.alumni.management.chat.entity.MessageType;
import com.alumni.management.notification.config.FirebaseConfig;
import com.alumni.management.notification.dto.DeviceRegistrationRequest;
import com.alumni.management.notification.entity.DeviceToken;
import com.alumni.management.notification.repository.DeviceTokenRepository;
import com.alumni.management.notification.service.PushNotificationService;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class PushNotificationSystemTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseConfig firebaseConfig;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setName("Vivek Patel");
        userA.setEmail("vivek@example.com");

        userB = new User();
        userB.setId(2L);
        userB.setName("Jay Shah");
        userB.setEmail("jay@example.com");
    }

    @Test
    @DisplayName("TC-01: Register device token for authenticated user")
    void testRegisterDeviceToken() {
        when(userRepository.findByEmailIgnoreCase("vivek@example.com")).thenReturn(Optional.of(userA));
        when(deviceTokenRepository.findByToken("token_123")).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceRegistrationRequest request = new DeviceRegistrationRequest("token_123", "ANDROID", "device_pixel_8");
        DeviceToken token = pushNotificationService.registerDeviceToken("vivek@example.com", request);

        assertNotNull(token);
        assertEquals("vivek@example.com", token.getUserEmail());
        assertEquals("token_123", token.getToken());
        assertEquals("ANDROID", token.getDeviceType());
        assertEquals("device_pixel_8", token.getDeviceId());
        assertTrue(token.isActive());
    }

    @Test
    @DisplayName("TC-02 & TC-03: Register same token or refreshed token updates existing record")
    void testRegisterSameTokenAgain() {
        DeviceToken existing = new DeviceToken();
        existing.setId(10L);
        existing.setUser(userA);
        existing.setUserEmail("vivek@example.com");
        existing.setToken("token_123");
        existing.setActive(true);

        when(userRepository.findByEmailIgnoreCase("vivek@example.com")).thenReturn(Optional.of(userA));
        when(deviceTokenRepository.findByToken("token_123")).thenReturn(Optional.of(existing));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceRegistrationRequest request = new DeviceRegistrationRequest("token_123", "ANDROID", "device_pixel_8_v2");
        DeviceToken updated = pushNotificationService.registerDeviceToken("vivek@example.com", request);

        assertEquals("device_pixel_8_v2", updated.getDeviceId());
        assertTrue(updated.isActive());
        verify(deviceTokenRepository).save(existing);
    }

    @Test
    @DisplayName("TC-06 & TC-32: Token reassignment on account switch (prevents notification leakage)")
    void testTokenAccountSwitchReassignment() {
        // Device token previously belonged to userA
        DeviceToken existing = new DeviceToken();
        existing.setId(10L);
        existing.setUser(userA);
        existing.setUserEmail("vivek@example.com");
        existing.setToken("shared_device_token");
        existing.setActive(true);

        // Now userB logs into the same device
        when(userRepository.findByEmailIgnoreCase("jay@example.com")).thenReturn(Optional.of(userB));
        when(deviceTokenRepository.findByToken("shared_device_token")).thenReturn(Optional.of(existing));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceRegistrationRequest request = new DeviceRegistrationRequest("shared_device_token", "ANDROID", "phone_1");
        DeviceToken reassigned = pushNotificationService.registerDeviceToken("jay@example.com", request);

        // Verified: Token is now associated ONLY with userB
        assertEquals("jay@example.com", reassigned.getUserEmail());
        assertEquals(userB, reassigned.getUser());
        assertTrue(reassigned.isActive());
    }

    @Test
    @DisplayName("TC-05: Unregister device token removes token for user")
    void testUnregisterDeviceToken() {
        pushNotificationService.unregisterDeviceToken("vivek@example.com", "token_123");
        verify(deviceTokenRepository).deleteByUserEmailAndToken("vivek@example.com", "token_123");
    }

    @Test
    @DisplayName("TC-09 & TC-26: Sender sending to self does NOT generate notification")
    void testSelfNotificationPrevented() {
        ChatMessage message = new ChatMessage();
        message.setSender("vivek@example.com");
        message.setReceiver("vivek@example.com");
        message.setContent("Note to self");

        pushNotificationService.sendChatMessageNotification(message);

        verify(deviceTokenRepository, never()).findByUserEmailIgnoreCaseAndIsActiveTrue(any());
    }

    @Test
    @DisplayName("TC-10, TC-11, TC-12: Message preview generation by type")
    void testMessagePreviewGeneration() {
        ChatMessage textMsg = new ChatMessage();
        textMsg.setType(MessageType.TEXT);
        textMsg.setContent("Hey! How are you?");
        assertEquals("Hey! How are you?", pushNotificationService.generateMessagePreview(textMsg));

        ChatMessage imgMsg = new ChatMessage();
        imgMsg.setType(MessageType.IMAGE);
        imgMsg.setContent("photo.jpg");
        assertEquals("📷 Photo", pushNotificationService.generateMessagePreview(imgMsg));

        ChatMessage videoMsg = new ChatMessage();
        videoMsg.setType(MessageType.VIDEO);
        videoMsg.setContent("clip.mp4");
        assertEquals("🎥 Video", pushNotificationService.generateMessagePreview(videoMsg));

        ChatMessage docMsg = new ChatMessage();
        docMsg.setType(MessageType.DOCUMENT);
        docMsg.setContent("document.pdf");
        assertEquals("📄 Document", pushNotificationService.generateMessagePreview(docMsg));

        ChatMessage audioMsg = new ChatMessage();
        audioMsg.setType(MessageType.AUDIO);
        audioMsg.setContent("voice.m4a");
        assertEquals("🎵 Voice message", pushNotificationService.generateMessagePreview(audioMsg));
    }

    @Test
    @DisplayName("TC-14: Firebase unavailable does not throw or crash")
    void testFirebaseUnavailableGracefulHandling() {
        ChatMessage msg = new ChatMessage();
        msg.setId("msg_1");
        msg.setSender("vivek@example.com");
        msg.setReceiver("jay@example.com");
        msg.setContent("Hello Jay!");
        msg.setConversationId("jay@example.com_vivek@example.com");

        when(firebaseConfig.isFirebaseAvailable()).thenReturn(false);

        // Must complete normally without throwing exception
        pushNotificationService.sendChatMessageNotification(msg);
        verify(deviceTokenRepository, never()).findByUserEmailIgnoreCaseAndIsActiveTrue(any());
    }

    @Test
    @DisplayName("Sender display name retrieval returns user's full name")
    void testSenderDisplayName() {
        when(userRepository.findByEmailIgnoreCase("vivek@example.com")).thenReturn(Optional.of(userA));
        String name = pushNotificationService.getSenderDisplayName("vivek@example.com");
        assertEquals("Vivek Patel", name);
    }
}

