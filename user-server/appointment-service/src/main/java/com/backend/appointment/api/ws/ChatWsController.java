package com.backend.appointment.api.ws;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;

    public ChatWsController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/appointments/{appointmentId}/chat.send")
    public void sendMessage(
            @DestinationVariable String appointmentId,
            @Payload MessageRequest request,
            Principal principal) {
        // Principal is set by WebSocketAuthInterceptor
        chatService.saveAndBroadcast(appointmentId, principal.getName(), request);
    }
}
