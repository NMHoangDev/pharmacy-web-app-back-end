package com.backend.review.repo;

import com.backend.review.model.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    List<ReviewImage> findByReviewIdIn(List<UUID> reviewIds);

    void deleteByReviewId(UUID reviewId);
}
