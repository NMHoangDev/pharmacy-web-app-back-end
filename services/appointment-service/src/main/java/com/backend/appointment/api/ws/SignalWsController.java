package com.backend.appointment.api.ws;

import com.backend.appointment.api.dto.SignalMessage;
import com.backend.appointment.model.ConsultationSession;
import com.backend.appointment.repo.ConsultationSessionRepository;
import com.backend.appointment.service.AppointmentAccessService;
import com.backend.appointment.service.CurrentPharmacistResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class SignalWsController {

    private static final Logger log = LoggerFactory.getLogger(SignalWsController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ConsultationSessionRepository sessionRepository;
    private final AppointmentAccessService appointmentAccessService;
    private final CurrentPharmacistResolver pharmacistResolver;

    public SignalWsController(SimpMessagingTemplate messagingTemplate,
            ConsultationSessionRepository sessionRepository,
            AppointmentAccessService appointmentAccessService,
            CurrentPharmacistResolver pharmacistResolver) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
        this.appointmentAccessService = appointmentAccessService;
        this.pharmacistResolver = pharmacistResolver;
    }

    @MessageMapping("/calls/{roomId}/signal.offer")
    public void contentOffer(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "OFFER", payload, resolveActorContext(principal));
    }

    @MessageMapping("/calls/{roomId}/signal.answer")
    public void contentAnswer(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "ANSWER", payload, resolveActorContext(principal));
    }

    @MessageMapping("/calls/{roomId}/signal.ice")
    public void contentIce(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "ICE", payload, resolveActorContext(principal));
    }

    // Sent by a peer (USER/ADMIN) to request the PHARMACIST to re-send an OFFER
    @MessageMapping("/calls/{roomId}/signal.join")
    public void contentJoin(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
            Principal principal) {
        relaySignal(roomId, "JOIN", payload, resolveActorContext(principal));
    }

    private ActorContext resolveActorContext(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing websocket principal");
        }

        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email == null || email.isBlank()) {
                email = jwtAuth.getToken().getClaimAsString("preferred_username");
            }

            Set<String> roles = new HashSet<>();
            jwtAuth.getAuthorities().forEach((authority) -> {
                if (authority == null || authority.getAuthority() == null) {
                    return;
                }
                roles.add(normalizeRole(authority.getAuthority()));
            });

            boolean isAdmin = roles.contains("ADMIN");
            boolean isPharmacist = roles.contains("PHARMACIST") || roles.contains("STAFF");

            String actorId = subject;
            if (isPharmacist) {
                UUID pharmacistId = pharmacistResolver.resolveForActor(subject, email);
                if (pharmacistId == null) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve pharmacist identity");
                }
                actorId = pharmacistId.toString();
            }

            if (actorId == null || actorId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve websocket actor");
            }

            return new ActorContext(actorId, isAdmin, subject);
        }

        return new ActorContext(principal.getName(), false, principal.getName());
    }

    private void relaySignal(String roomId, String type, Map<String, Object> data, ActorContext actorContext) {
        // Validate room exists and user is a participant of the appointment
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Map<String, Object> safeData = data == null ? Map.of() : data;

        log.info("[Signal][in] roomId={} appointmentId={} type={} actorId={} principalSub={} signalingKeys={}",
                roomId,
                session.getAppointmentId(),
                type,
                actorContext.actorId(),
                actorContext.principalSubject(),
                safeData.keySet());

        // Authorization check only — no call-window check here so ICE candidates
        // are never dropped mid-call due to time-window edge cases
        appointmentAccessService.getAppointmentIfAuthorized(
                session.getAppointmentId(),
                actorContext.actorId(),
                actorContext.isAdmin());

        SignalMessage signal = new SignalMessage();
        signal.setType(type);
        signal.setRoomId(roomId);
        signal.setFromUserId(actorContext.actorId());
        signal.setData(safeData);

        // Optional: If data contains toUserId, set it
        Object toUserId = safeData.get("toUserId");
        if (toUserId != null) {
            signal.setToUserId(String.valueOf(toUserId));
        }

        log.info("[Signal][out] roomId={} appointmentId={} type={} from={} to={} destination={}",
                roomId,
                session.getAppointmentId(),
                type,
                signal.getFromUserId(),
                signal.getToUserId(),
                "/topic/calls/" + roomId + "/signal");
        messagingTemplate.convertAndSend("/topic/calls/" + roomId + "/signal", signal);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    private record ActorContext(String actorId, boolean isAdmin, String principalSubject) {
    }
}
