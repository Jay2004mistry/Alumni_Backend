package com.alumni.management.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendRequest {
    private String receiver;
    private String content;
    private String type;
    private String timestamp;

    public ChatSendRequest(String receiver, String content, String type) {
        this.receiver = receiver;
        this.content = content;
        this.type = type;
    }
}
