package com.backend.review.repo;

import com.backend.review.model.Review;
import com.backend.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserId(UUID userId, Pageable pageable);
}
