package com.alumni.management.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.alumni.management.chat.config.ChatClock;
import com.alumni.management.chat.dto.MessageStatusUpdateEvent;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.Conversation;
import com.alumni.management.chat.entity.MessageStatus;
import com.alumni.management.chat.repository.ChatMessageRepository;
import com.alumni.management.chat.repository.ConversationRepository;
import com.alumni.management.chat.service.ChatService;

@ExtendWith(MockitoExtension.class)
public class ChatHistoryServiceTest {

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private ChatClock chatClock;

	@InjectMocks
	private ChatService chatService;

	private final String user1 = "alice@example.com";
	private final String user2 = "bob@example.com";
	private String conversationId;

	@BeforeEach
	void setUp() {
		conversationId = Conversation.generateConversationId(user1, user2);
	}

	@Test
	void testGetChatHistoryDoesNotMutateReadStatus() {
		ChatMessage msg = new ChatMessage();
		msg.setId("msg101");
		msg.setConversationId(conversationId);
		msg.setSender(user2);
		msg.setReceiver(user1);
		msg.setContent("Hello Alice");
		msg.setRead(false);
		msg.setStatus(MessageStatus.SENT);

		when(chatMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId))
				.thenReturn(List.of(msg));

		List<ChatMessage> result = chatService.getChatHistory(user1, user2);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertFalse(result.get(0).isRead(), "Message read status must remain false on history fetch");
		assertEquals(MessageStatus.SENT, result.get(0).getStatus());

		// Verify that no repository saves or status broadcasts were triggered
		verify(chatMessageRepository, never()).saveAll(any());
		verify(messagingTemplate, never()).convertAndSendToUser(any(), eq("/queue/status"), any());
	}

	@Test
	void testExplicitMarkMessagesAsReadMutatesStatusAndBroadcasts() {
		ChatMessage unreadMsg = new ChatMessage();
		unreadMsg.setId("msg102");
		unreadMsg.setConversationId(conversationId);
		unreadMsg.setSender(user2);
		unreadMsg.setReceiver(user1);
		unreadMsg.setRead(false);
		unreadMsg.setStatus(MessageStatus.DELIVERED);

		when(chatMessageRepository.findBySenderIgnoreCaseAndReceiverIgnoreCaseAndIsReadFalse(user2, user1))
				.thenReturn(List.of(unreadMsg));
		when(chatClock.nowInstant()).thenReturn(Instant.parse("2026-10-20T12:00:00Z"));
		when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

		chatService.markMessagesAsRead(user1, user2);

		assertTrue(unreadMsg.isRead(), "Message should now be marked as read");
		assertEquals(MessageStatus.READ, unreadMsg.getStatus());
		verify(chatMessageRepository).saveAll(List.of(unreadMsg));
		verify(messagingTemplate).convertAndSendToUser(eq(user2), eq("/queue/status"), any(MessageStatusUpdateEvent.class));
	}

	@Test
	void testSendMessageWithDuplicateClientMessageIdReturnsExistingWithoutResaving() {
		ChatMessage existingMsg = new ChatMessage();
		existingMsg.setId("msg-existing-1");
		existingMsg.setClientMessageId("client-uuid-12345");
		existingMsg.setSender(user1);
		existingMsg.setReceiver(user2);
		existingMsg.setContent("Hello duplicate");

		com.alumni.management.chat.dto.ChatSendRequest request = new com.alumni.management.chat.dto.ChatSendRequest();
		request.setClientMessageId("client-uuid-12345");
		request.setReceiver(user2);
		request.setContent("Hello duplicate");

		when(chatMessageRepository.findBySenderAndClientMessageId(user1, "client-uuid-12345")).thenReturn(Optional.of(existingMsg));

		ChatMessage result = chatService.processAndSendMessage(user1, request);

		assertNotNull(result);
		assertEquals("msg-existing-1", result.getId());
		verify(chatMessageRepository, never()).save(any(ChatMessage.class));
		verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(ChatMessage.class));
	}

	@Test
	void testSendMessageConcurrentDuplicateKeyCaughtRecoversExisting() {
		com.alumni.management.chat.dto.ChatSendRequest request = new com.alumni.management.chat.dto.ChatSendRequest();
		request.setClientMessageId("concurrent-uuid-999");
		request.setReceiver(user2);
		request.setContent("Hello concurrent");

		// Initial lookup finds nothing
		when(chatMessageRepository.findBySenderAndClientMessageId(user1, "concurrent-uuid-999")).thenReturn(Optional.empty());

		// Concurrent insert triggers DuplicateKeyException on save
		when(chatMessageRepository.save(any(ChatMessage.class)))
				.thenThrow(new org.springframework.dao.DuplicateKeyException("E11000 duplicate key error"));

		ChatMessage recoveredMsg = new ChatMessage();
		recoveredMsg.setId("recovered-msg-999");
		recoveredMsg.setClientMessageId("concurrent-uuid-999");
		recoveredMsg.setSender(user1);
		recoveredMsg.setReceiver(user2);

		// Secondary lookup recovers the concurrently saved message
		when(chatMessageRepository.findBySenderAndClientMessageId(user1, "concurrent-uuid-999"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(recoveredMsg));

		ChatMessage result = chatService.processAndSendMessage(user1, request);
		assertNotNull(result);
		assertEquals("recovered-msg-999", result.getId());
	}
}
