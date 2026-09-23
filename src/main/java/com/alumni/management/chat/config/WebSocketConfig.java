package com.alumni.management.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.alumni.management.security.JwtUtil;
import com.alumni.management.security.CustomUserDetailsService.CustomUserDetailsService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private CustomUserDetailsService userDetailsService;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/queue", "/topic");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/chat")
				.setAllowedOriginPatterns("*")
				.withSockJS();
		registry.addEndpoint("/chat")
				.setAllowedOriginPatterns("*");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor == null) {
					accessor = StompHeaderAccessor.wrap(message);
				}

				if (StompCommand.CONNECT.equals(accessor.getCommand())) {
					String authHeader = accessor.getFirstNativeHeader("Authorization");
					if (authHeader == null) {
						authHeader = accessor.getFirstNativeHeader("authorization");
					}
					if (authHeader != null && authHeader.startsWith("Bearer ")) {
						String token = authHeader.substring(7);
						try {
							String email = jwtUtil.extractEmail(token);
							if (email != null && jwtUtil.validateToken(token)) {
								UserDetails userDetails = userDetailsService.loadUserByUsername(email);
								UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
										userDetails, null, userDetails.getAuthorities());
								accessor.setUser(auth);
								log.info("🔐 STOMP CONNECT AUTHENTICATED: Principal = {}", auth.getName());
							} else {
								log.warn("⚠️ STOMP CONNECT INVALID TOKEN: email={}", email);
							}
						} catch (Exception e) {
							log.error("❌ STOMP CONNECT AUTH EXCEPTION: {}", e.getMessage());
						}
					} else {
						log.warn("⚠️ STOMP CONNECT MISSING Authorization header");
					}
				} else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
					log.info("👂 STOMP SUBSCRIBE: user={}, destination={}",
							accessor.getUser() != null ? accessor.getUser().getName() : "ANONYMOUS",
							accessor.getDestination());
				} else if (StompCommand.SEND.equals(accessor.getCommand())) {
					log.info("📤 STOMP INBOUND SEND: user={}, destination={}",
							accessor.getUser() != null ? accessor.getUser().getName() : "ANONYMOUS",
							accessor.getDestination());
				}
				return message;
			}
		});
	}
}
