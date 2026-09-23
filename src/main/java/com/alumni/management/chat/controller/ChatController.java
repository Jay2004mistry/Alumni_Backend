package com.alumni.management.chat.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.management.chat.config.PresenceEventListener;
import com.alumni.management.chat.dto.ChatSendRequest;
import com.alumni.management.chat.dto.TypingEvent;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	@Autowired
	private ChatService chatService;

	private String getCurrentUserEmail(Principal principal) {
		if (principal != null && principal.getName() != null && !principal.getName().trim().isEmpty()) {
			return principal.getName().trim().toLowerCase();
		}
		String auth = SecurityContextHolder.getContext().getAuthentication() != null
				? SecurityContextHolder.getContext().getAuthentication().getName()
				: null;
		return (auth != null && !auth.equalsIgnoreCase("anonymousUser")) ? auth.trim().toLowerCase() : "";
	}

	@MessageMapping("/chat.send")
	public void sendMessage(Principal principal, @Payload ChatSendRequest request) {
		String authEmail = getCurrentUserEmail(principal);
		log.info("📥 BACKEND CHAT RECEIVED: authenticatedUser={}, receiver={}, content={}",
				authEmail,
				request != null ? request.getReceiver() : null,
				request != null ? request.getContent() : null);
		chatService.processAndSendMessage(authEmail, request);
	}

	@MessageMapping("/chat.typing")
	public void handleTyping(Principal principal, @Payload TypingEvent event) {
		String authEmail = getCurrentUserEmail(principal);
		chatService.processTyping(authEmail, event);
	}

	@PostMapping("/send")
	public ChatMessage sendRestMessage(Principal principal, @RequestBody ChatSendRequest request) {
		String authEmail = getCurrentUserEmail(principal);
		log.info("📥 REST CHAT RECEIVED: authenticatedUser={}, receiver={}, content={}",
				authEmail,
				request != null ? request.getReceiver() : null,
				request != null ? request.getContent() : null);
		return chatService.processAndSendMessage(authEmail, request);
	}

	@GetMapping("/history/{receiverEmail}")
	public List<ChatMessage> getChatHistory(Principal principal, @PathVariable String receiverEmail) {
		String authEmail = getCurrentUserEmail(principal);
		return chatService.getChatHistory(authEmail, receiverEmail);
	}

	@PostMapping("/mark-read/{senderEmail}")
	public Map<String, String> markRead(Principal principal, @PathVariable String senderEmail) {
		String authEmail = getCurrentUserEmail(principal);
		chatService.markMessagesAsRead(authEmail, senderEmail);
		Map<String, String> response = new HashMap<>();
		response.put("status", "success");
		return response;
	}

	@GetMapping("/unread")
	public Map<String, Integer> getUnreadCounts(Principal principal) {
		String authEmail = getCurrentUserEmail(principal);
		return chatService.getUnreadCounts(authEmail);
	}

	@GetMapping("/conversations")
	public List<ChatMessage> getConversations(Principal principal) {
		String authEmail = getCurrentUserEmail(principal);
		return chatService.getConversations(authEmail);
	}

	@GetMapping("/presence/{userEmail}")
	public Map<String, Object> getPresence(@PathVariable String userEmail) {
		Map<String, Object> presence = new HashMap<>();
		boolean isOnline = PresenceEventListener.isUserOnline(userEmail);
		LocalDateTime lastSeen = PresenceEventListener.getLastSeen(userEmail);

		presence.put("userEmail", userEmail != null ? userEmail.trim().toLowerCase() : "");
		presence.put("online", isOnline);
		presence.put("lastSeen", lastSeen != null ? lastSeen.toString() : null);
		return presence;
	}
}
