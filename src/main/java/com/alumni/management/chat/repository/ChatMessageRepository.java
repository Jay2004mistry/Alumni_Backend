package com.alumni.management.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.entity.MessageStatus;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

	List<ChatMessage> findByConversationIdOrderByTimestampAsc(String conversationId);

	List<ChatMessage> findBySenderIgnoreCaseAndReceiverIgnoreCase(String sender, String receiver);

	List<ChatMessage> findBySenderIgnoreCaseOrReceiverIgnoreCase(String sender, String receiver);

	List<ChatMessage> findByReceiverIgnoreCaseAndIsReadFalse(String receiver);

	List<ChatMessage> findBySenderIgnoreCaseAndReceiverIgnoreCaseAndIsReadFalse(String sender, String receiver);

	List<ChatMessage> findBySenderIgnoreCaseAndReceiverIgnoreCaseAndStatus(String sender, String receiver, MessageStatus status);

	List<ChatMessage> findByReceiverIgnoreCaseAndStatus(String receiver, MessageStatus status);
}
