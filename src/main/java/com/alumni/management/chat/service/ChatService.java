package com.alumni.management.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.alumni.management.chat.config.ChatClock;
import com.alumni.management.chat.config.PresenceEventListener;
import com.alumni.management.chat.dto.ChatSendRequest;
import com.alumni.management.chat.dto.MessageStatusUpdateEvent;
import com.alumni.management.chat.dto.TypingEvent;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.Conversation;
import com.alumni.management.chat.entity.MessageStatus;
import com.alumni.management.chat.entity.MessageType;
import com.alumni.management.chat.repository.ChatMessageRepository;
import com.alumni.management.chat.repository.ConversationRepository;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatClock chatClock;

    public ChatMessage processAndSendMessage(String authenticatedSender, ChatSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Chat request payload must not be null.");
        }
        String sender = (authenticatedSender != null && !authenticatedSender.trim().isEmpty())
                ? authenticatedSender.trim().toLowerCase()
                : "";

        String receiver = request.getReceiver() != null ? request.getReceiver().trim().toLowerCase() : "";
        if (sender.isEmpty() || receiver.isEmpty()) {
            log.error("❌ CHAT ERROR: Sender ({}) or receiver ({}) email is empty!", sender, receiver);
            throw new IllegalArgumentException("Sender and receiver emails must not be empty.");
        }

        if (sender.equalsIgnoreCase(receiver)) {
            log.error("❌ CHAT ERROR: Sender ({}) is same as receiver ({})!", sender, receiver);
            throw new IllegalArgumentException("Cannot send message to yourself.");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content must not be empty.");
        }

        if (request.getContent().length() > 5000) {
            throw new IllegalArgumentException("Message content exceeds maximum allowed length (5000 characters).");
        }

        String conversationId = Conversation.generateConversationId(sender, receiver);
        log.info("📨 PROCESSING MESSAGE: sender={}, receiver={}, conversationId={}, content={}",
                sender, receiver, conversationId, request.getContent());

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(conversationId);
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setContent(request.getContent());

        MessageType messageType = MessageType.TEXT;
        if (request.getType() != null && !request.getType().trim().isEmpty()) {
            try {
                messageType = MessageType.valueOf(request.getType().trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }
        chatMessage.setType(messageType);
        chatMessage.setTimestamp(chatClock.now());
        chatMessage.setUpdatedAt(chatClock.now());

        // Check if receiver is online to immediately set status to DELIVERED
        boolean receiverOnline = PresenceEventListener.isUserOnline(receiver);
        if (receiverOnline) {
            chatMessage.setStatus(MessageStatus.DELIVERED);
        } else {
            chatMessage.setStatus(MessageStatus.SENT);
        }
        chatMessage.setRead(false);

        // Save ChatMessage in MongoDB
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("💾 MESSAGE SAVED: id={}, sender={}, receiver={}, conversationId={}, status={}",
                saved.getId(), saved.getSender(), saved.getReceiver(), saved.getConversationId(), saved.getStatus());

        // Update or Create Conversation document in MongoDB
        updateConversation(conversationId, sender, receiver, saved);

        // Send message via WebSocket STOMP to receiver & sender
        try {
            log.info("📡 BROADCASTING MESSAGE: to receiver='{}', destination='/queue/messages', messageId={}",
                    receiver, saved.getId());
            messagingTemplate.convertAndSendToUser(receiver, "/queue/messages", saved);

            log.info("📡 BROADCASTING MESSAGE: to sender='{}', destination='/queue/messages', messageId={}",
                    sender, saved.getId());
            messagingTemplate.convertAndSendToUser(sender, "/queue/messages", saved);
        } catch (Exception e) {
            log.error("❌ BROADCAST FAILED for messageId={}: {}", saved.getId(), e.getMessage(), e);
        }

        return saved;
    }

    private void updateConversation(String conversationId, String sender, String receiver, ChatMessage message) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseGet(() -> {
                        Conversation c = new Conversation();
                        c.setId(conversationId);
                        c.setParticipants(List.of(sender, receiver));
                        c.setCreatedAt(chatClock.now());
                        return c;
                    });

            conversation.setLastMessage(message.getContent());
            conversation.setLastMessageSender(sender);
            conversation.setLastMessageStatus(message.getStatus());
            conversation.setLastMessageAt(message.getTimestamp());
            conversation.setUpdatedAt(chatClock.now());

            conversation.incrementUnreadCountForUser(receiver);

            conversationRepository.save(conversation);
            log.info("💾 CONVERSATION UPDATED: conversationId={}, lastSender={}, unreadForReceiver={}",
                    conversationId, sender, conversation.getUnreadCountForUser(receiver));
        } catch (Exception e) {
            log.error("❌ Failed to update conversation for conversationId={}: {}", conversationId, e.getMessage(), e);
        }
    }

    public List<ChatMessage> getChatHistory(String myEmail, String targetEmail) {
        String user1 = myEmail != null ? myEmail.trim().toLowerCase() : "";
        String user2 = targetEmail != null ? targetEmail.trim().toLowerCase() : "";
        String conversationId = Conversation.generateConversationId(user1, user2);

        // Mark unread messages sent by target to me as READ
        markMessagesAsRead(user1, user2);

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        if (messages.isEmpty()) {
            // Fallback for older messages stored before conversationId migration
            List<ChatMessage> sentByMe = chatMessageRepository.findBySenderIgnoreCaseAndReceiverIgnoreCase(user1, user2);
            List<ChatMessage> sentToMe = chatMessageRepository.findBySenderIgnoreCaseAndReceiverIgnoreCase(user2, user1);
            List<ChatMessage> all = new ArrayList<>(sentByMe);
            all.addAll(sentToMe);
            Map<String, ChatMessage> map = new HashMap<>();
            for (ChatMessage msg : all) {
                if (msg.getId() != null) {
                    map.put(msg.getId(), msg);
                }
            }
            messages = map.values().stream()
                    .sorted(Comparator.comparing(ChatMessage::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }
        return messages;
    }

    public void markMessagesAsRead(String myEmail, String senderEmail) {
        String reader = myEmail != null ? myEmail.trim().toLowerCase() : "";
        String sender = senderEmail != null ? senderEmail.trim().toLowerCase() : "";

        List<ChatMessage> unread = chatMessageRepository.findBySenderIgnoreCaseAndReceiverIgnoreCaseAndIsReadFalse(sender, reader);
        if (!unread.isEmpty()) {
            for (ChatMessage msg : unread) {
                msg.setRead(true);
                msg.setStatus(MessageStatus.READ);
                msg.setUpdatedAt(chatClock.now());
            }
            chatMessageRepository.saveAll(unread);

            // Notify original sender via STOMP that messages were READ
            for (ChatMessage msg : unread) {
                MessageStatusUpdateEvent event = new MessageStatusUpdateEvent(
                        msg.getId(), msg.getConversationId(), sender, reader, MessageStatus.READ);
                try {
                    messagingTemplate.convertAndSendToUser(sender, "/queue/status", event);
                } catch (Exception e) {
                }
            }
        }

        // Reset conversation unread count
        String conversationId = Conversation.generateConversationId(reader, sender);
        try {
            conversationRepository.findById(conversationId).ifPresent(c -> {
                c.setUnreadCountForUser(reader, 0);
                conversationRepository.save(c);
            });
        } catch (Exception e) {
            log.error("Failed to reset unread count in conversation for conversationId={}: {}", conversationId, e.getMessage(), e);
        }
    }

    public List<ChatMessage> getConversations(String myEmail) {
        String user = myEmail != null ? myEmail.trim().toLowerCase() : "";
        List<Conversation> convs = conversationRepository.findByParticipantsContainingOrderByLastMessageAtDesc(user);

        if (!convs.isEmpty()) {
            List<ChatMessage> list = new ArrayList<>();
            for (Conversation c : convs) {
                ChatMessage msg = new ChatMessage();
                msg.setConversationId(c.getId());
                msg.setContent(c.getLastMessage());
                msg.setSender(c.getLastMessageSender());
                
                String otherParticipant = (c.getParticipants() != null)
                        ? c.getParticipants().stream()
                            .filter(p -> !p.equalsIgnoreCase(user))
                            .findFirst().orElse(c.getLastMessageSender())
                        : c.getLastMessageSender();
                msg.setReceiver(otherParticipant != null && otherParticipant.equalsIgnoreCase(c.getLastMessageSender()) ? user : otherParticipant);
                msg.setTimestamp(c.getLastMessageAt());
                msg.setStatus(c.getLastMessageStatus() != null ? c.getLastMessageStatus() : MessageStatus.SENT);
                list.add(msg);
            }
            return list;
        }

        // Fallback for historic data
        List<ChatMessage> allMsgs = chatMessageRepository.findBySenderIgnoreCaseOrReceiverIgnoreCase(user, user);
        allMsgs.sort(Comparator.comparing(ChatMessage::getTimestamp, Comparator.nullsFirst(Comparator.reverseOrder())));
        List<ChatMessage> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (ChatMessage msg : allMsgs) {
            String s = msg.getSender() != null ? msg.getSender().trim().toLowerCase() : "";
            String r = msg.getReceiver() != null ? msg.getReceiver().trim().toLowerCase() : "";
            String other = s.equalsIgnoreCase(user) ? r : s;
            if (!other.isEmpty() && !processed.contains(other)) {
                processed.add(other);
                result.add(msg);
            }
        }
        return result;
    }

    public Map<String, Integer> getUnreadCounts(String myEmail) {
        String user = myEmail != null ? myEmail.trim().toLowerCase() : "";
        List<ChatMessage> unreadMessages = chatMessageRepository.findByReceiverIgnoreCaseAndIsReadFalse(user);
        Map<String, Integer> counts = new HashMap<>();
        for (ChatMessage msg : unreadMessages) {
            if (msg.getSender() != null) {
                String senderLower = msg.getSender().trim().toLowerCase();
                counts.put(senderLower, counts.getOrDefault(senderLower, 0) + 1);
            }
        }
        return counts;
    }

    public void processTyping(String authenticatedSender, TypingEvent event) {
        String sender = authenticatedSender != null ? authenticatedSender.trim().toLowerCase() : "";
        if (event == null) return;
        event.setSender(sender);
        if (event.getReceiver() != null && !event.getReceiver().isEmpty()) {
            try {
                messagingTemplate.convertAndSendToUser(
                        event.getReceiver().trim().toLowerCase(), "/queue/typing", event);
            } catch (Exception e) {
            }
        }
    }
}
