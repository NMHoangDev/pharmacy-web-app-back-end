package com.backend.inventory.repo;

import com.backend.inventory.model.ReservationLine;
import com.backend.inventory.model.ReservationLineId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationLineRepository extends JpaRepository<ReservationLine, ReservationLineId> {
    List<ReservationLine> findByIdReservationId(UUID reservationId);
}
