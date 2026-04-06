package com.backend.appointment.api.dto;

import jakarta.validation.constraints.Pattern;

public class ConsultationRequest {
    @Pattern(regexp = "VIDEO|VOICE")
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
