package com.backend.user.api;

import com.backend.user.model.PharmacistProfile;
import com.backend.user.repo.PharmacistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacists")
@RequiredArgsConstructor
public class PublicPharmacistApi {

    private final PharmacistProfileRepository profileRepository;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "true") boolean verified,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) Integer ratingGte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<PharmacistProfile> filtered = profileRepository.findAll().stream()
                .filter(profile -> !verified || hasLicense(profile))
                .filter(profile -> specialty == null || specialty.isBlank()
                        || (profile.getSpecialtyTags() != null && profile.getSpecialtyTags().stream()
                                .anyMatch(tag -> specialty.equalsIgnoreCase(tag))))
                .filter(profile -> mode == null || mode.isBlank() || "all".equalsIgnoreCase(mode)
                        || matchesMode(profile, mode))
                .filter(profile -> experience == null || experience.isBlank() || "all".equalsIgnoreCase(experience)
                        || matchesExperience(profile, experience))
                .sorted(Comparator.comparing(
                        PharmacistProfile::getYearsExp,
                        Comparator.reverseOrder()))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());

        List<Map<String, Object>> content = filtered.subList(from, to).stream()
                .map(this::toCard)
                .toList();

        return Map.of(
                "content", content,
                "totalElements", filtered.size(),
                "page", safePage,
                "size", safeSize,
                "ratingGteApplied", ratingGte == null ? 0 : ratingGte);
    }

    @GetMapping("/online")
    public List<Map<String, Object>> online(@RequestParam(defaultValue = "4") int limit) {
        int safeLimit = Math.max(limit, 1);
        return profileRepository.findAll().stream()
                .filter(profile -> profile.getOnlineStatus() == PharmacistProfile.OnlineStatus.ONLINE)
                .filter(this::hasLicense)
                .sorted(Comparator.comparing(
                        PharmacistProfile::getYearsExp,
                        Comparator.reverseOrder()))
                .limit(safeLimit)
                .map(this::toCard)
                .toList();
    }

    private boolean hasLicense(PharmacistProfile profile) {
        return profile.getLicenseNo() != null && !profile.getLicenseNo().isBlank();
    }

    private boolean matchesMode(PharmacistProfile profile, String mode) {
        if ("online".equalsIgnoreCase(mode)) {
            return profile.getOnlineStatus() == PharmacistProfile.OnlineStatus.ONLINE;
        }
        if ("offline".equalsIgnoreCase(mode)) {
            return profile.getOnlineStatus() != PharmacistProfile.OnlineStatus.ONLINE;
        }
        return true;
    }

    private boolean matchesExperience(PharmacistProfile profile, String experience) {
        int years = profile.getYearsExp();
        return switch (experience.toLowerCase()) {
            case "junior" -> years < 3;
            case "mid" -> years >= 3 && years < 7;
            case "senior" -> years >= 7;
            default -> true;
        };
    }

    private Map<String, Object> toCard(PharmacistProfile profile) {
        String specialty = profile.getSpecialtyTags() == null || profile.getSpecialtyTags().isEmpty()
                ? ""
                : profile.getSpecialtyTags().get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", profile.getUserId());
        result.put("name", profile.getUser() != null && profile.getUser().getFullName() != null
                ? profile.getUser().getFullName()
                : "Dược sĩ");
        result.put("specialty", specialty);
        result.put("experienceYears", profile.getYearsExp());
        result.put("rating", 5);
        result.put("reviewCount", 0);
        result.put("status", profile.getOnlineStatus() == null
                ? "offline"
                : profile.getOnlineStatus().name().toLowerCase());
        result.put("verified", hasLicense(profile));
        result.put("avatarUrl", profile.getUser() != null && profile.getUser().getAvatarBase64() != null
                ? profile.getUser().getAvatarBase64()
                : "");
        return result;
    }
}
