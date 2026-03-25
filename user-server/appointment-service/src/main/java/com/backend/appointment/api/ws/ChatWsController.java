package com.backend.appointment.api.ws;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.security.SecurityUtils;
import com.backend.appointment.service.ChatService;
import com.backend.appointment.service.CurrentPharmacistResolver;
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
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing websocket principal");
        }
        chatService.saveAndBroadcast(appointmentId, resolveActorId(principal), request);
    }

    private String resolveActorId(Principal principal) {
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

        return principal.getName();
    }
}
