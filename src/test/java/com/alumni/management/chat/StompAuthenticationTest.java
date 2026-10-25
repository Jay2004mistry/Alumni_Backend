package com.alumni.management.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetails;

import com.alumni.management.chat.config.WebSocketConfig;
import com.alumni.management.security.JwtUtil;
import com.alumni.management.security.CustomUserDetailsService.CustomUserDetailsService;

@ExtendWith(MockitoExtension.class)
public class StompAuthenticationTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@InjectMocks
	private WebSocketConfig webSocketConfig;

	private ChannelInterceptor interceptor;

	@BeforeEach
	void setUp() {
		ChannelRegistration registration = new ChannelRegistration();
		webSocketConfig.configureClientInboundChannel(registration);
		// Retrieve registered interceptor via reflection because ChannelRegistration.getInterceptors() is protected
		@SuppressWarnings("unchecked")
		java.util.List<ChannelInterceptor> interceptors = (java.util.List<ChannelInterceptor>) org.springframework.test.util.ReflectionTestUtils.getField(registration, "interceptors");
		interceptor = interceptors.get(0);
	}

	@Test
	void testConnectWithValidTokenSetsAuthenticatedPrincipal() {
		String validToken = "valid.jwt.token";
		String userEmail = "testuser@example.com";

		when(jwtUtil.extractEmail(validToken)).thenReturn(userEmail);
		when(jwtUtil.validateToken(validToken)).thenReturn(true);

		UserDetails userDetails = mock(UserDetails.class);
		when(userDetails.getUsername()).thenReturn(userEmail);
		when(userDetails.isEnabled()).thenReturn(true);
		when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + validToken);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, null);
		assertNotNull(result);

		StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
		assertNotNull(resultAccessor.getUser(), "Principal should be set on successful authentication");
		assertEquals(userEmail, resultAccessor.getUser().getName());
	}

	@Test
	void testConnectWithUnverifiedUserThrowsMessageDeliveryException() {
		String validToken = "valid.jwt.token";
		String userEmail = "unverified@example.com";

		when(jwtUtil.extractEmail(validToken)).thenReturn(userEmail);
		when(jwtUtil.validateToken(validToken)).thenReturn(true);

		UserDetails userDetails = mock(UserDetails.class);
		when(userDetails.isEnabled()).thenReturn(false);
		when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + validToken);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Account not verified", ex.getMessage());
	}

	@Test
	void testConnectWithMissingTokenThrowsMessageDeliveryException() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Missing Authorization header", ex.getMessage());
	}

	@Test
	void testConnectWithInvalidTokenThrowsMessageDeliveryException() {
		String invalidToken = "invalid.token";
		when(jwtUtil.extractEmail(invalidToken)).thenReturn("user@example.com");
		when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + invalidToken);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Invalid or expired token", ex.getMessage());
	}

	@Test
	void testConnectWithExpiredTokenThrowsMessageDeliveryException() {
		String expiredToken = "expired.jwt.token";
		when(jwtUtil.extractEmail(expiredToken)).thenThrow(new RuntimeException("JWT expired"));

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + expiredToken);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Token validation error", ex.getMessage());
	}

	@Test
	void testSubscribeUnauthenticatedThrowsException() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/user/queue/presence");
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Authentication required to subscribe", ex.getMessage());
	}

	@Test
	void testSubscribeToTopicPresenceThrowsForbiddenException() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/topic/presence");
		accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@example.com", null));
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Forbidden: Global presence topic is disabled. Subscribe to /user/queue/presence", ex.getMessage());
	}

	@Test
	void testSubscribeToUserQueuePresenceAllowed() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/user/queue/presence");
		accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@example.com", null));
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, null);
		assertNotNull(result);
	}

	@Test
	void testSendUnauthenticatedThrowsException() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/app/chat.send");
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () -> {
			interceptor.preSend(message, null);
		});
		assertEquals("Unauthorized: Authentication required to send", ex.getMessage());
	}
}
