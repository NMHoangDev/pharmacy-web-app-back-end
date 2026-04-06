package com.backend.review.repo;

import com.backend.review.model.Review;
import com.backend.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
        Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

        Page<Review> findByProductId(UUID productId, Pageable pageable);

        Page<Review> findByUserId(UUID userId, Pageable pageable);

        long countByStatus(ReviewStatus status);

        long countByStatusAndRepliedAtIsNull(ReviewStatus status);

        long countByStatusAndRepliedAtIsNotNull(ReviewStatus status);

        @Query("SELECT AVG(r.rating) FROM Review r WHERE r.status = :status")
        Double avgRatingByStatus(@Param("status") ReviewStatus status);

        @Query("SELECT r FROM Review r WHERE (:status IS NULL OR r.status = :status) "
                        + "AND (:rating IS NULL OR r.rating = :rating) "
                        + "AND (:query IS NULL OR LOWER(r.content) LIKE LOWER(CONCAT('%', :query, '%')) "
                        + "OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))) "
                        + "AND (:replied IS NULL OR (:replied = true AND r.repliedAt IS NOT NULL) "
                        + "OR (:replied = false AND r.repliedAt IS NULL))")
        Page<Review> searchAdmin(@Param("status") ReviewStatus status,
                        @Param("rating") Integer rating,
                        @Param("query") String query,
                        @Param("replied") Boolean replied,
                        Pageable pageable);

        @Query("SELECT r FROM Review r WHERE (:status IS NULL OR r.status = :status) "
                        + "AND (:rating IS NULL OR r.rating = :rating) "
                        + "AND (:query IS NULL OR LOWER(r.content) LIKE LOWER(CONCAT('%', :query, '%')) "
                        + "OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))) "
                        + "AND (:replied IS NULL OR (:replied = true AND r.repliedAt IS NOT NULL) "
                        + "OR (:replied = false AND r.repliedAt IS NULL))")
        List<Review> searchAdminList(@Param("status") ReviewStatus status,
                        @Param("rating") Integer rating,
                        @Param("query") String query,
                        @Param("replied") Boolean replied);

        long countByProductIdAndStatus(UUID productId, ReviewStatus status);

        @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = :status")
        Double avgRatingByProductIdAndStatus(@Param("productId") UUID productId,
                        @Param("status") ReviewStatus status);

        @Query("SELECT r.rating as rating, COUNT(r) as total FROM Review r "
                        + "WHERE r.productId = :productId AND r.status = :status GROUP BY r.rating")
        List<RatingCountView> countRatingsByProductIdAndStatus(@Param("productId") UUID productId,
                        @Param("status") ReviewStatus status);

        interface RatingCountView {
                Integer getRating();

                Long getTotal();
        }
}
