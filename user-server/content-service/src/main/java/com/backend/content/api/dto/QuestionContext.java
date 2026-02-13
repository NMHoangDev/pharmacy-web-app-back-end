package com.backend.content.api.dto;

import java.util.List;

public record QuestionContext(
        Integer age,
        String gender,
        Boolean isPregnant,
        Boolean isBreastfeeding,
        String allergies,
        List<String> conditions,
        List<String> currentMedications,
        String urgency) {
}
