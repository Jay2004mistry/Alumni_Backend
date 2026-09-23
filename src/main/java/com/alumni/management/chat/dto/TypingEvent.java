package com.alumni.management.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {
    private String sender;
    private String receiver;

    @JsonProperty("isTyping")
    private boolean isTyping;

    @JsonProperty("isTyping")
    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        this.isTyping = typing;
    }
}
