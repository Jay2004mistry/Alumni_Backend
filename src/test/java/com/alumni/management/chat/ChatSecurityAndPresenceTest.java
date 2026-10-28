package com.alumni.management.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.alumni.management.chat.config.ChatClock;
import com.alumni.management.chat.controller.ChatController;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.Conversation;
import com.alumni.management.chat.repository.ChatMessageRepository;
import com.alumni.management.chat.repository.ConversationRepository;
import com.alumni.management.chat.service.ChatService;

@ExtendWith(MockitoExtension.class)
public class ChatSecurityAndPresenceTest {

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ChatClock chatClock;

	@InjectMocks
	private ChatService chatService;

	@Test
	void testGetOrCreateConversationKeyGeneratesAndPersists256BitKey() {
		String u1 = "alice@example.com";
		String u2 = "bob@example.com";
		String conversationId = Conversation.generateConversationId(u1, u2);

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

		String key1 = chatService.getOrCreateConversationKey(u1, u2);
		assertNotNull(key1);
		byte[] decoded = Base64.getDecoder().decode(key1);
		assertEquals(32, decoded.length, "Generated key must be 256 bits (32 bytes)");
		verify(conversationRepository).save(any(Conversation.class));

		Conversation existingConv = new Conversation();
		existingConv.setId(conversationId);
		existingConv.setEncryptionKey(key1);
		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(existingConv));

		String key2 = chatService.getOrCreateConversationKey(u1, u2);
		assertEquals(key1, key2, "Subsequent calls must retrieve existing key");
	}

	@Test
	void testHasConversationRelationshipReturnsExpectedBoolean() {
		String u1 = "alice@example.com";
		String u2 = "bob@example.com";
		String stranger = "eve@example.com";
		String conversationId = Conversation.generateConversationId(u1, u2);

		assertTrue(chatService.hasConversationRelationship(u1, u1), "Self always has relationship");

		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(new Conversation()));
		assertTrue(chatService.hasConversationRelationship(u1, u2), "Existing conversation implies relationship");

		String strangerConvId = Conversation.generateConversationId(u1, stranger);
		when(conversationRepository.findById(strangerConvId)).thenReturn(Optional.empty());
		when(chatMessageRepository.findByConversationIdOrderByTimestampAsc(strangerConvId)).thenReturn(Collections.emptyList());
		assertFalse(chatService.hasConversationRelationship(u1, stranger), "Stranger has no relationship");
	}

	@Test
	void testGetConversationPartnersExtractsUniquePartners() {
		String user = "alice@example.com";
		Conversation c1 = new Conversation();
		c1.setParticipants(List.of("alice@example.com", "bob@example.com"));

		Conversation c2 = new Conversation();
		c2.setParticipants(List.of("alice@example.com", "charlie@example.com"));

		when(conversationRepository.findByParticipantsContainingOrderByLastMessageAtDesc(user))
				.thenReturn(List.of(c1, c2));

		var partners = chatService.getConversationPartners(user);
		assertEquals(2, partners.size());
		assertTrue(partners.contains("bob@example.com"));
		assertTrue(partners.contains("charlie@example.com"));
	}

	@Test
	void testControllerGetConversationKeyAuthorization() {
		ChatController controller = new ChatController();
		try {
			var field = ChatController.class.getDeclaredField("chatService");
			field.setAccessible(true);
			field.set(controller, chatService);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// 1. Unauthenticated request returns 401
		ResponseEntity<?> unauthRes = controller.getConversationKey(null, "bob@example.com");
		assertEquals(HttpStatus.UNAUTHORIZED, unauthRes.getStatusCode());

		// 2. Request with invalid/empty partner returns 400
		Principal principal = () -> "alice@example.com";
		ResponseEntity<?> emptyPartnerRes = controller.getConversationKey(principal, "   ");
		assertEquals(HttpStatus.BAD_REQUEST, emptyPartnerRes.getStatusCode());

		// 3. Request with self returns 400
		ResponseEntity<?> selfRes = controller.getConversationKey(principal, "alice@example.com");
		assertEquals(HttpStatus.BAD_REQUEST, selfRes.getStatusCode());

		// 4. Valid partner returns 200 with key
		String conversationId = Conversation.generateConversationId("alice@example.com", "bob@example.com");
		when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(new Conversation() {{
			setEncryptionKey("dGVzdEtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");
		}}));

		ResponseEntity<?> okRes = controller.getConversationKey(principal, "bob@example.com");
		assertEquals(HttpStatus.OK, okRes.getStatusCode());
		assertNotNull(okRes.getBody());
		assertTrue(((Map<?, ?>) okRes.getBody()).containsKey("key"));
	}
}
