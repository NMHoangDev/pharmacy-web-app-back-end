package com.backend.user.api;

import com.backend.user.api.dto.PharmacistProfileRequest;
import com.backend.user.model.PharmacistProfile;
import com.backend.user.model.User;
import com.backend.user.repo.PharmacistProfileRepository;
import com.backend.user.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PharmacistController {

    private final PharmacistProfileRepository profileRepository;
    private final UserRepository userRepository;

    @GetMapping("/pharmacists")
    public List<PharmacistProfile> getPharmacists(
            @RequestParam(required = false) PharmacistProfile.OnlineStatus status,
            @RequestParam(required = false) String specialty) {
        return profileRepository.searchPharmacists(status, specialty);
    }

    @GetMapping("/{userId}/pharmacist")
    public PharmacistProfile getPharmacistProfile(@PathVariable UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist profile not found"));
    }

    @PutMapping("/{userId}/pharmacist")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PHARMACIST') and #userId.toString() == #jwt.subject)")
    public PharmacistProfile updatePharmacistProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody PharmacistProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        PharmacistProfile profile = profileRepository.findById(userId)
                .orElse(new PharmacistProfile());

        profile.setUser(user);
        profile.setUserId(userId);
        profile.setLicenseNo(request.getLicenseNo());
        profile.setSpecialtyTags(request.getSpecialtyTags());
        profile.setYearsExp(request.getYearsExp());
        profile.setBio(request.getBio());
        profile.setLanguages(request.getLanguages());
        if (request.getOnlineStatus() != null) {
            profile.setOnlineStatus(request.getOnlineStatus());
        }

        return profileRepository.save(profile);
    }
}
