package com.alumni.management.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.alumni.management.chat.entity.Conversation;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findById(String id);

    List<Conversation> findByParticipantsContainingOrderByLastMessageAtDesc(String userEmail);
}
