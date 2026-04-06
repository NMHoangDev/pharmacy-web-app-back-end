package com.backend.appointment.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileDto(
        UUID id,
        String email,
        String phone,
        @JsonAlias("full_name") String fullName,
        String avatarBase64) {
}
