package com.alumni.management.chat.dto;

import com.alumni.management.chat.entity.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateEvent {
    private String messageId;
    private String conversationId;
    private String sender;
    private String receiver;
    private MessageStatus status;
}
