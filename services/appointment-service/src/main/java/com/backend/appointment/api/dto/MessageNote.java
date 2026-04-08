package com.backend.appointment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageNote {
    private String type; // PRESCRIPTION, SYMPTOM, VITALS, ORDER, GENERAL
    private String title;
    private Map<String, Object> payload;
}
