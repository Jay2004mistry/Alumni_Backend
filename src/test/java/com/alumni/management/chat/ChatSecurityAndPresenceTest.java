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
		assertFalse(partners.contains("alice@example.com"), "Self must not be in partners list");
	}

	@Test
	void testControllerGetConversationKeyAuthorization() {
		ChatController controller = new ChatController();
		org.springframework.test.util.ReflectionTestUtils.setField(controller, "chatService", chatService);

		// 1. Unauthenticated -> 401
		ResponseEntity<?> resUnauth = controller.getConversationKey(null, "bob@example.com");
		assertEquals(HttpStatus.UNAUTHORIZED, resUnauth.getStatusCode());

		// 2. Bad partner email (self or empty) -> 400
		Principal principal = () -> "alice@example.com";
		ResponseEntity<?> resSelf = controller.getConversationKey(principal, "alice@example.com");
		assertEquals(HttpStatus.BAD_REQUEST, resSelf.getStatusCode());

		// 3. Authorized -> 200 with key
		when(conversationRepository.findById(any())).thenReturn(Optional.of(new Conversation()));
		ResponseEntity<?> resOk = controller.getConversationKey(principal, "bob@example.com");
		assertEquals(HttpStatus.OK, resOk.getStatusCode());
		assertTrue(((Map<?, ?>) resOk.getBody()).containsKey("key"));
	}

	@Test
	void testControllerGetPresenceAuthorization() {
		ChatController controller = new ChatController();
		org.springframework.test.util.ReflectionTestUtils.setField(controller, "chatService", chatService);

		// 1. Unauthenticated -> 401
		ResponseEntity<?> resUnauth = controller.getPresence(null, "bob@example.com");
		assertEquals(HttpStatus.UNAUTHORIZED, resUnauth.getStatusCode());

		// 2. Stranger -> 403 Forbidden
		Principal principal = () -> "alice@example.com";
		String strangerConvId = Conversation.generateConversationId("alice@example.com", "stranger@example.com");
		when(conversationRepository.findById(strangerConvId)).thenReturn(Optional.empty());
		when(chatMessageRepository.findByConversationIdOrderByTimestampAsc(strangerConvId)).thenReturn(Collections.emptyList());

		ResponseEntity<?> resForbidden = controller.getPresence(principal, "stranger@example.com");
		assertEquals(HttpStatus.FORBIDDEN, resForbidden.getStatusCode());

		// 3. Self -> 200 OK
		ResponseEntity<?> resSelf = controller.getPresence(principal, "alice@example.com");
		assertEquals(HttpStatus.OK, resSelf.getStatusCode());
	}
}
