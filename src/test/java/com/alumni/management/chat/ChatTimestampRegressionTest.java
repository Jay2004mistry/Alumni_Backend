package com.alumni.management.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.alumni.management.chat.config.ChatClock;
import com.alumni.management.chat.dto.ChatSendRequest;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.repository.ChatMessageRepository;
import com.alumni.management.chat.repository.ConversationRepository;
import com.alumni.management.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
public class ChatTimestampRegressionTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ChatClock chatClock = new ChatClock();

    @InjectMocks
    private ChatService chatService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ChatClock.resetClock();
    }

    @AfterEach
    void tearDown() {
        ChatClock.resetClock();
    }

    @Test
    void testMessageTimestampPreservesAuthoritativeInstant() {
        // Deterministic instant representing 12:13:00 IST (06:43:00 UTC)
        Instant fixedInstant = Instant.parse("2026-10-28T06:43:00.123Z");
        ChatClock.setFixedClock(Clock.fixed(fixedInstant, ZoneOffset.UTC));

        ChatSendRequest request = new ChatSendRequest();
        request.setReceiver("bob@example.com");
        request.setContent("Meeting at 12:13");
        request.setClientMessageId("cm_test_101");

        when(chatMessageRepository.findBySenderAndClientMessageId("alice@example.com", "cm_test_101"))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatService.processAndSendMessage("alice@example.com", request);

        assertNotNull(result);
        assertEquals(fixedInstant, result.getTimestamp(), "Authoritative timestamp must match fixed instant exactly");

        // Verify that in IST (+05:30) this represents exactly 12:13:00
        ZonedDateTime istTime = result.getTimestamp().atZone(ZoneId.of("Asia/Kolkata"));
        assertEquals(12, istTime.getHour());
        assertEquals(13, istTime.getMinute());
    }

    @Test
    void testMessageSerializationFormatsWithExplicitUtcZIndicator() throws Exception {
        ChatMessage msg = new ChatMessage();
        msg.setId("msg_serial_1");
        msg.setSender("alice@example.com");
        msg.setReceiver("bob@example.com");
        msg.setContent("Testing UTC Z format");
        Instant testInstant = Instant.parse("2026-10-28T06:43:00.456Z");
        msg.setTimestamp(testInstant);

        String json = objectMapper.writeValueAsString(msg);

        // Ensure serialized JSON contains explicit ISO-8601 UTC 'Z' indicator
        assertTrue(json.contains("\"timestamp\":\"2026-10-28T06:43:00.456Z\"")
                || json.contains("2026-10-28T06:43:00.456Z"),
                "Timestamp must serialize with explicit ISO-8601 UTC 'Z' indicator: " + json);
    }

    @Test
    void testSenderAndRecipientReceiveIdenticalAuthoritativeTimestamp() {
        Instant fixedInstant = Instant.parse("2026-10-28T06:43:00.789Z");
        ChatClock.setFixedClock(Clock.fixed(fixedInstant, ZoneOffset.UTC));

        ChatSendRequest request = new ChatSendRequest();
        request.setReceiver("bob@example.com");
        request.setContent("Same instant for both users");
        request.setClientMessageId("cm_test_sync");

        when(chatMessageRepository.findBySenderAndClientMessageId("alice@example.com", "cm_test_sync"))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.processAndSendMessage("alice@example.com", request);

        ArgumentCaptor<ChatMessage> bobCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        ArgumentCaptor<ChatMessage> aliceCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        verify(messagingTemplate).convertAndSendToUser(org.mockito.ArgumentMatchers.eq("bob@example.com"),
                org.mockito.ArgumentMatchers.eq("/queue/messages"), bobCaptor.capture());
        verify(messagingTemplate).convertAndSendToUser(org.mockito.ArgumentMatchers.eq("alice@example.com"),
                org.mockito.ArgumentMatchers.eq("/queue/messages"), aliceCaptor.capture());

        assertEquals(fixedInstant, bobCaptor.getValue().getTimestamp());
        assertEquals(fixedInstant, aliceCaptor.getValue().getTimestamp());
        assertEquals(bobCaptor.getValue().getTimestamp(), aliceCaptor.getValue().getTimestamp(),
                "Sender and recipient must receive the exact same instant");
    }

    @Test
    void testChatClockSkewCorrectionOffsetsDriftedClockCorrectly() {
        // If host clock is lagging by 73 seconds (73000 ms behind network time)
        ChatClock.setClockSkewMillis(73000L);

        Instant rawSystemNow = Instant.now();
        Instant calibratedNow = chatClock.nowInstant();

        long diffSeconds = Duration.between(rawSystemNow, calibratedNow).toSeconds();
        assertTrue(diffSeconds >= 72 && diffSeconds <= 74,
                "Calibrated instant must advance drifted clock by skew amount (~73s). Diff=" + diffSeconds);
    }
}

