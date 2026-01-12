package com.backend.review.service;

import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import com.backend.review.api.dto.ReviewRequest;
import com.backend.review.api.dto.ReviewResponse;
import com.backend.review.model.Review;
import com.backend.review.model.ReviewStatus;
import com.backend.review.repo.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    public ReviewService(ReviewRepository reviewRepository,
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.reviewRepository = reviewRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ReviewResponse create(ReviewRequest req) {
        Review r = new Review();
        r.setId(UUID.randomUUID());
        r.setProductId(req.productId());
        r.setUserId(req.userId());
        r.setRating(req.rating());
        r.setTitle(req.title());
        r.setContent(req.content());
        r.setStatus(ReviewStatus.PUBLISHED);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        reviewRepository.save(r);

        EventEnvelope<ReviewResponse> evt = EventEnvelope.of(EventTypes.REVIEW_CREATED, "1", toResponse(r));
        kafkaTemplate.send(TopicNames.REVIEW_EVENTS, evt);

        return toResponse(r);
    }

    public Page<ReviewResponse> listByProduct(UUID productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable)
                .map(this::toResponse);
    }

    public Page<ReviewResponse> listByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public ReviewResponse update(UUID id, ReviewRequest req) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        r.setRating(req.rating());
        r.setTitle(req.title());
        r.setContent(req.content());
        r.setUpdatedAt(Instant.now());
        reviewRepository.save(r);
        return toResponse(r);
    }

    public void delete(UUID id) {
        reviewRepository.deleteById(id);
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getProductId(), r.getUserId(), r.getRating(), r.getTitle(),
                r.getContent(),
                r.getStatus().name(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
