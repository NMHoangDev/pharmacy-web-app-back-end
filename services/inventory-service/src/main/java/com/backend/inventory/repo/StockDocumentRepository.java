package com.backend.inventory.repo;

import com.backend.inventory.model.StockDocumentEntity;
import com.backend.inventory.model.StockDocumentStatus;
import com.backend.inventory.model.StockDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StockDocumentRepository extends JpaRepository<StockDocumentEntity, UUID> {

    @EntityGraph(attributePaths = "lines")
    Optional<StockDocumentEntity> findWithLinesById(UUID id);

    @Query("""
            SELECT d FROM StockDocumentEntity d
            WHERE (:type IS NULL OR d.type = :type)
              AND (:status IS NULL OR d.status = :status)
              AND (:branchId IS NULL OR d.branchId = :branchId)
              AND (:fromDate IS NULL OR d.createdAt >= :fromDate)
              AND (:toDate IS NULL OR d.createdAt <= :toDate)
              AND (:keyword IS NULL OR d.invoiceNo LIKE %:keyword% OR d.supplierName LIKE %:keyword%)
            ORDER BY d.createdAt DESC
            """)
    Page<StockDocumentEntity> search(
            @Param("type") StockDocumentType type,
            @Param("status") StockDocumentStatus status,
            @Param("branchId") UUID branchId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("keyword") String keyword,
            Pageable pageable);
}
