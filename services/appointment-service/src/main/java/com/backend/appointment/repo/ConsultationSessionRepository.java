package com.backend.appointment.repo;

import com.backend.appointment.model.ConsultationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, String> {
    Optional<ConsultationSession> findByRoomId(String roomId);

    List<ConsultationSession> findByAppointmentIdOrderByCreatedAtDesc(String appointmentId);

    Optional<ConsultationSession> findTopByAppointmentIdOrderByCreatedAtDesc(String appointmentId);
}
