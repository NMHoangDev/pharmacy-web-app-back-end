package com.backend.appointment.api.ws;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.service.ChatService;
import com.backend.appointment.service.CurrentPharmacistResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatWsController {

    private final ChatService chatService;
    private final CurrentPharmacistResolver pharmacistResolver;

    public ChatWsController(ChatService chatService,
            CurrentPharmacistResolver pharmacistResolver) {
        this.chatService = chatService;
        this.pharmacistResolver = pharmacistResolver;
    }

    @MessageMapping("/appointments/{appointmentId}/chat.send")
    public void sendMessage(
            @DestinationVariable String appointmentId,
            @Payload MessageRequest request,
            Principal principal) {
        chatService.saveAndBroadcast(appointmentId, resolveActorId(principal), request);
    }

    private String resolveActorId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing websocket principal");
        }

        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email == null || email.isBlank()) {
                email = jwtAuth.getToken().getClaimAsString("preferred_username");
            }

            boolean pharmacistLike = jwtAuth.getAuthorities().stream()
                    .map(authority -> authority == null ? null : authority.getAuthority())
                    .filter(authority -> authority != null && !authority.isBlank())
                    .map(this::normalizeRole)
                    .anyMatch(role -> "PHARMACIST".equals(role) || "STAFF".equals(role));

            if (pharmacistLike) {
                UUID pharmacistId = pharmacistResolver.resolveForActor(subject, email);
                if (pharmacistId == null) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve pharmacist identity");
                }
                return pharmacistId.toString();
            }

            if (subject != null && !subject.isBlank()) {
                return subject;
            }
        }

        return principal.getName();
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring("ROLE_".length());
        }
        return normalized;
    }
}
