package com.backend.appointment.api.ws;

import com.backend.appointment.api.dto.SignalMessage;
import com.backend.appointment.model.ConsultationSession;
import com.backend.appointment.repo.ConsultationSessionRepository;
import com.backend.appointment.service.AppointmentAccessService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class SignalWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConsultationSessionRepository sessionRepository;
    private final AppointmentAccessService appointmentAccessService;

    public SignalWsController(SimpMessagingTemplate messagingTemplate,
            ConsultationSessionRepository sessionRepository,
            AppointmentAccessService appointmentAccessService) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
        this.appointmentAccessService = appointmentAccessService;
    }

    @MessageMapping("/calls/{roomId}/signal.offer")
    public void contentOffer(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "OFFER", payload, principal.getName());
    }

    @MessageMapping("/calls/{roomId}/signal.answer")
    public void contentAnswer(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "ANSWER", payload, principal.getName());
    }

    @MessageMapping("/calls/{roomId}/signal.ice")
    public void contentIce(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "ICE", payload, principal.getName());
    }

    // Sent by a peer (USER/ADMIN) to request the PHARMACIST to re-send an OFFER
    @MessageMapping("/calls/{roomId}/signal.join")
    public void contentJoin(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "JOIN", payload, principal.getName());
    }

    private void relaySignal(String roomId, String type, Map<String, Object> data, String userId) {
        // Validate room exists and user is a participant of the appointment
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Authorization check only — no call-window check here so ICE candidates
        // are never dropped mid-call due to time-window edge cases
        appointmentAccessService.getAppointmentIfAuthorized(session.getAppointmentId(), userId);

        SignalMessage signal = new SignalMessage();
        signal.setType(type);
        signal.setRoomId(roomId);
        signal.setFromUserId(userId);
        signal.setData(data);

        // Optional: If data contains toUserId, set it
        if (data.containsKey("toUserId")) {
            signal.setToUserId((String) data.get("toUserId"));
        }

        messagingTemplate.convertAndSend("/topic/calls/" + roomId + "/signal", signal);
    }
}
