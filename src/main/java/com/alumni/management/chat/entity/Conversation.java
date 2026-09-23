package com.alumni.management.chat.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Indexed
    private List<String> participants = new ArrayList<>();

    private String lastMessage;
    private String lastMessageSender;
    private MessageStatus lastMessageStatus;
    private LocalDateTime lastMessageAt;

    private List<ParticipantUnread> unreadCounts = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static String generateConversationId(String user1, String user2) {
        String u1 = user1 != null ? user1.trim().toLowerCase() : "";
        String u2 = user2 != null ? user2.trim().toLowerCase() : "";
        if (u1.compareTo(u2) <= 0) {
            return u1 + "_" + u2;
        } else {
            return u2 + "_" + u1;
        }
    }

    public int getUnreadCountForUser(String userEmail) {
        if (userEmail == null || unreadCounts == null) {
            return 0;
        }
        String email = userEmail.trim().toLowerCase();
        for (ParticipantUnread pu : unreadCounts) {
            if (pu != null && pu.getUserEmail() != null && pu.getUserEmail().equalsIgnoreCase(email)) {
                return pu.getUnreadCount();
            }
        }
        return 0;
    }

    public void setUnreadCountForUser(String userEmail, int count) {
        if (userEmail == null) {
            return;
        }
        if (unreadCounts == null) {
            unreadCounts = new ArrayList<>();
        }
        String email = userEmail.trim().toLowerCase();
        for (ParticipantUnread pu : unreadCounts) {
            if (pu != null && pu.getUserEmail() != null && pu.getUserEmail().equalsIgnoreCase(email)) {
                pu.setUnreadCount(count);
                return;
            }
        }
        unreadCounts.add(new ParticipantUnread(email, count));
    }

    public void incrementUnreadCountForUser(String userEmail) {
        setUnreadCountForUser(userEmail, getUnreadCountForUser(userEmail) + 1);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantUnread {
        private String userEmail;
        private int unreadCount;
    }
}
