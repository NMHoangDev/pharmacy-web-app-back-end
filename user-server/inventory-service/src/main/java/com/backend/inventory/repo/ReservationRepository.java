package com.backend.inventory.repo;

import com.backend.inventory.model.Reservation;
import com.backend.inventory.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByOrderId(UUID orderId);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
