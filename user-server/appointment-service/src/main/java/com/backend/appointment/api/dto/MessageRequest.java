package com.backend.appointment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;
    private MessageNote note;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageNote getNote() {
        return note;
    }

    public void setNote(MessageNote note) {
        this.note = note;
    }
}
