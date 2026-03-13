package com.backend.appointment.api;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.api.dto.MessageResponse;
import com.backend.appointment.service.ChatService;
import com.backend.appointment.service.CurrentPharmacistResolver;
import com.backend.appointment.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/messages")
public class AppointmentChatController {

    private final ChatService chatService;
    private final CurrentPharmacistResolver pharmacistResolver;

    public AppointmentChatController(ChatService chatService,
            CurrentPharmacistResolver pharmacistResolver) {
        this.chatService = chatService;
        this.pharmacistResolver = pharmacistResolver;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String appointmentId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getHistory(appointmentId, resolveActorId(jwt), limit));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String appointmentId,
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.saveAndBroadcast(appointmentId, resolveActorId(jwt), request));
    }

    private String resolveActorId(Jwt jwt) {
        if (SecurityUtils.isPharmacist()) {
            UUID pharmacistId = pharmacistResolver.resolveForCurrentActor();
            if (pharmacistId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve pharmacist identity");
            }
            return pharmacistId.toString();
        }

        UUID actorId = SecurityUtils.getActorId();
        if (actorId != null) {
            return actorId.toString();
        }

        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve actor identity");
    }
}
