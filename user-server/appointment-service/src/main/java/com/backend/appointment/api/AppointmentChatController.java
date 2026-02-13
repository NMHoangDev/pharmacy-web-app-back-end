package com.backend.appointment.api;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.api.dto.MessageResponse;
import com.backend.appointment.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/messages")
public class AppointmentChatController {

    private final ChatService chatService;

    public AppointmentChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String appointmentId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        // Assuming principal.getName() returns userId
        return ResponseEntity.ok(chatService.getHistory(appointmentId, jwt.getSubject(), limit));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String appointmentId,
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.saveAndBroadcast(appointmentId, jwt.getSubject(), request));
    }
}
