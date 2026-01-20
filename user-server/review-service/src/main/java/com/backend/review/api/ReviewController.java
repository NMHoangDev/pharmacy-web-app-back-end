package com.backend.review.api;

import com.backend.review.api.dto.ReviewRequest;
import com.backend.review.api.dto.ReviewResponse;
import com.backend.review.api.dto.ReviewStatusRequest;
import com.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("review-service ok");
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@RequestBody @Valid ReviewRequest req) {
        return ResponseEntity.ok(reviewService.create(req));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> listByProduct(@PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.listByProduct(productId, page, size));
    }

    @GetMapping("/internal/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> listByProductAdmin(@PathVariable UUID productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.listByProductAdmin(productId, status, page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> listByUser(@PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.listByUser(userId, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(@PathVariable UUID id, @RequestBody @Valid ReviewRequest req) {
        return ResponseEntity.ok(reviewService.update(id, req));
    }

    @PutMapping("/internal/{id}/status")
    public ResponseEntity<ReviewResponse> updateStatus(@PathVariable UUID id,
            @RequestBody @Valid ReviewStatusRequest req) {
        return ResponseEntity.ok(reviewService.updateStatus(id, req.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
