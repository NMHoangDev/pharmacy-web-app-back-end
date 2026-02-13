package com.backend.user.api.dto;

import com.backend.user.model.PharmacistProfile;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PharmacistProfileRequest {
    @NotBlank
    private String licenseNo;

    @NotEmpty
    private List<String> specialtyTags;

    @Min(0)
    private int yearsExp;

    private String bio;

    private List<String> languages;

    private PharmacistProfile.OnlineStatus onlineStatus;
}

@Data
class PharmacistProfileResponse {
    private UUID userId;
    private String licenseNo;
    private List<String> specialtyTags;
    private int yearsExp;
    private String bio;
    private List<String> languages;
    private PharmacistProfile.OnlineStatus onlineStatus;
}
