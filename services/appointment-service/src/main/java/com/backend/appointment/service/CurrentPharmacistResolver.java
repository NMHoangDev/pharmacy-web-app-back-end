package com.backend.appointment.service;

import com.backend.appointment.client.PharmacistClient;
import com.backend.appointment.client.dto.PharmacistListItemDto;
import com.backend.appointment.client.dto.PharmacistPageDto;
import com.backend.appointment.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CurrentPharmacistResolver {

    private final PharmacistClient pharmacistClient;

    public CurrentPharmacistResolver(PharmacistClient pharmacistClient) {
        this.pharmacistClient = pharmacistClient;
    }

    public UUID resolveForCurrentActor() {
        UUID actorId = SecurityUtils.getActorId();
        String email = SecurityUtils.getActorEmail();
        return resolveForActor(
                actorId != null ? actorId.toString() : null,
                email);
    }

    public UUID resolveForActor(String actorSubject, String actorEmail) {
        UUID actorId = null;
        if (actorSubject != null && !actorSubject.isBlank()) {
            try {
                actorId = UUID.fromString(actorSubject.trim());
            } catch (IllegalArgumentException ignored) {
                actorId = null;
            }
        }

        if (actorId != null) {
            try {
                var byId = pharmacistClient.getPharmacist(actorId);
                if (byId != null && byId.id() != null) {
                    return byId.id();
                }
            } catch (Exception ignored) {
            }
        }

        String email = actorEmail;
        if (email == null || email.isBlank()) {
            return null;
        }

        try {
            PharmacistPageDto page = pharmacistClient.searchPharmacists(email.trim(), 0, 20);
            List<PharmacistListItemDto> list = page != null && page.content() != null ? page.content() : List.of();
            String normalized = email.trim().toLowerCase();

            for (PharmacistListItemDto item : list) {
                if (item == null || item.id() == null) {
                    continue;
                }
                String itemEmail = item.email();
                if (itemEmail != null && itemEmail.trim().equalsIgnoreCase(normalized)) {
                    return item.id();
                }
            }

            if (!list.isEmpty() && list.get(0) != null && list.get(0).id() != null) {
                return list.get(0).id();
            }
        } catch (Exception ignored) {
        }

        // Final fallback for environments where appointment.pharmacistId stores
        // the same UUID as the authenticated Keycloak subject.
        return actorId;
    }

    public boolean canAccessPharmacistId(UUID pharmacistId) {
        if (pharmacistId == null) {
            return false;
        }

        UUID actorId = SecurityUtils.getActorId();
        if (actorId != null && actorId.equals(pharmacistId)) {
            return true;
        }

        String actorEmail = normalize(SecurityUtils.getActorEmail());
        if (actorEmail != null) {
            try {
                var pharmacist = pharmacistClient.getPharmacist(pharmacistId);
                if (pharmacist != null && normalize(pharmacist.email()) != null
                        && normalize(pharmacist.email()).equals(actorEmail)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        UUID resolved = resolveForCurrentActor();
        return resolved != null && resolved.equals(pharmacistId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }
}
