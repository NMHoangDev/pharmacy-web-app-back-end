package com.backend.appointment.repo;

import com.backend.appointment.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByAppointmentIdOrderByCreatedAtDesc(String appointmentId, Pageable pageable);
    
    // For cursor-based pagination (simple version using createdAt)
    // List<ChatMessage> findByAppointmentIdAndCreatedAtBeforeOrderByCreatedAtDesc(String appointmentId, Timestamp before, Pageable pageable);
}
