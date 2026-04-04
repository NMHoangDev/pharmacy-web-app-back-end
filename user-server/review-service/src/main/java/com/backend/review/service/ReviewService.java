package com.backend.review.service;

import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import com.backend.review.api.dto.ReviewImageRequest;
import com.backend.review.api.dto.ReviewImageResponse;
import com.backend.review.api.dto.ReviewAdminStatsResponse;
import com.backend.review.api.dto.ReviewRequest;
import com.backend.review.api.dto.ReviewResponse;
import com.backend.review.api.dto.ReviewSummaryResponse;
import com.backend.review.cache.CacheConstants;
import com.backend.review.model.Review;
import com.backend.review.model.ReviewImage;
import com.backend.review.model.ReviewStatus;
import com.backend.review.cache.CacheHelper;
import com.backend.review.cache.CacheKeyBuilder;
import com.backend.review.repo.ReviewImageRepository;
import com.backend.review.repo.ReviewRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final CacheHelper cacheHelper;
    private final CacheKeyBuilder cacheKeyBuilder;

    public ReviewService(ReviewRepository reviewRepository,
            ReviewImageRepository reviewImageRepository,
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate,
            CacheHelper cacheHelper,
            CacheKeyBuilder cacheKeyBuilder) {
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cacheHelper = cacheHelper;
        this.cacheKeyBuilder = cacheKeyBuilder;
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

        List<ReviewImageResponse> images = saveImages(r.getId(), req.images());

        EventEnvelope<ReviewResponse> evt = EventEnvelope.of(EventTypes.REVIEW_CREATED, "1",
                toResponse(r, images));
        kafkaTemplate.send(TopicNames.REVIEW_EVENTS, evt);
        invalidateProductCaches(r.getProductId());

        return toResponse(r, images);
    }

    public Page<ReviewResponse> listByProduct(UUID productId, int page, int size) {
        String cacheKey = cacheKeyBuilder.build("review", "product", productId, "page", page, "size", size);
        try {
            return cacheHelper.getOrSetCacheByTtlKey(cacheKey, CacheConstants.TTL_REVIEW,
                    () -> loadProductReviews(productId, page, size));
        } catch (RuntimeException ex) {
            log.warn("Failed to read cached product reviews for productId={} page={} size={}. Falling back to DB.",
                    productId, page, size, ex);
            cacheHelper.evict(cacheKey);
            return loadProductReviews(productId, page, size);
        }
    }

    public ReviewSummaryResponse summaryByProduct(UUID productId) {
        String cacheKey = cacheKeyBuilder.build("review", "detail", "product", productId);
        try {
            return cacheHelper.getOrSetCacheByTtlKey(cacheKey, CacheConstants.TTL_REVIEW_DETAIL,
                    () -> loadProductReviewSummary(productId));
        } catch (RuntimeException ex) {
            log.warn("Failed to read cached review summary for productId={}. Falling back to DB.", productId, ex);
            cacheHelper.evict(cacheKey);
            return loadProductReviewSummary(productId);
        }
    }

    public Page<ReviewResponse> listByProductAdmin(UUID productId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status == null || status.isBlank()) {
            Page<Review> result = reviewRepository.findByProductId(productId, pageable);
            Map<UUID, List<ReviewImageResponse>> imageMap = loadImages(result.getContent());
            return result.map(review -> toResponse(review, imageMap.get(review.getId())));
        }

        ReviewStatus parsedStatus = ReviewStatus.valueOf(status.trim().toUpperCase());
        Page<Review> result = reviewRepository.findByProductIdAndStatus(productId, parsedStatus, pageable);
        Map<UUID, List<ReviewImageResponse>> imageMap = loadImages(result.getContent());
        return result.map(review -> toResponse(review, imageMap.get(review.getId())));
    }

    public Page<ReviewResponse> listByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> result = reviewRepository.findByUserId(userId, pageable);
        Map<UUID, List<ReviewImageResponse>> imageMap = loadImages(result.getContent());
        return result.map(review -> toResponse(review, imageMap.get(review.getId())));
    }

    public Page<ReviewResponse> listAdmin(String status, Integer rating, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        ReviewStatus statusFilter = null;
        Boolean repliedFilter = null;

        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toLowerCase();
            switch (normalized) {
                case "visible" -> statusFilter = ReviewStatus.PUBLISHED;
                case "hidden" -> statusFilter = ReviewStatus.REJECTED;
                case "pending" -> {
                    statusFilter = ReviewStatus.PUBLISHED;
                    repliedFilter = false;
                }
                case "responded" -> {
                    statusFilter = ReviewStatus.PUBLISHED;
                    repliedFilter = true;
                }
                default -> statusFilter = ReviewStatus.valueOf(status.trim().toUpperCase());
            }
        }

        String keyword = (query == null || query.isBlank()) ? null : query.trim();
        Page<Review> result = reviewRepository.searchAdmin(statusFilter, rating, keyword, repliedFilter, pageable);
        Map<UUID, List<ReviewImageResponse>> imageMap = loadImages(result.getContent());
        return result.map(review -> toResponse(review, imageMap.get(review.getId())));
    }

    public ReviewResponse update(UUID id, ReviewRequest req) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        r.setRating(req.rating());
        r.setTitle(req.title());
        r.setContent(req.content());
        r.setUpdatedAt(Instant.now());
        reviewRepository.save(r);
        invalidateProductCaches(r.getProductId());
        List<ReviewImageResponse> images = loadImagesForReview(r.getId());
        return toResponse(r, images);
    }

    public ReviewResponse updateStatus(UUID id, String status) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        ReviewStatus parsedStatus = ReviewStatus.valueOf(status.trim().toUpperCase());
        r.setStatus(parsedStatus);
        r.setUpdatedAt(Instant.now());
        reviewRepository.save(r);
        invalidateProductCaches(r.getProductId());
        List<ReviewImageResponse> images = loadImagesForReview(r.getId());
        return toResponse(r, images);
    }

    public ReviewResponse reply(UUID id, String content) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        r.setReplyContent(content);
        r.setRepliedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        reviewRepository.save(r);
        invalidateProductCaches(r.getProductId());
        List<ReviewImageResponse> images = loadImagesForReview(r.getId());
        return toResponse(r, images);
    }

    public ReviewAdminStatsResponse getAdminStats() {
        long total = reviewRepository.count();
        Double avg = reviewRepository.avgRatingByStatus(ReviewStatus.PUBLISHED);
        long pending = reviewRepository.countByStatusAndRepliedAtIsNull(ReviewStatus.PUBLISHED);
        long responded = reviewRepository.countByStatusAndRepliedAtIsNotNull(ReviewStatus.PUBLISHED);
        long hidden = reviewRepository.countByStatus(ReviewStatus.REJECTED);
        return new ReviewAdminStatsResponse(total, avg == null ? 0 : avg, pending, hidden, responded);
    }

    public byte[] exportAdmin(String status, Integer rating, String query) {
        ReviewStatus statusFilter = null;
        Boolean repliedFilter = null;
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toLowerCase();
            switch (normalized) {
                case "visible" -> statusFilter = ReviewStatus.PUBLISHED;
                case "hidden" -> statusFilter = ReviewStatus.REJECTED;
                case "pending" -> {
                    statusFilter = ReviewStatus.PUBLISHED;
                    repliedFilter = false;
                }
                case "responded" -> {
                    statusFilter = ReviewStatus.PUBLISHED;
                    repliedFilter = true;
                }
                default -> statusFilter = ReviewStatus.valueOf(status.trim().toUpperCase());
            }
        }

        String keyword = (query == null || query.isBlank()) ? null : query.trim();
        List<Review> reviews = reviewRepository.searchAdminList(statusFilter, rating, keyword, repliedFilter);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh gia");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "Ma danh gia",
                    "Ma san pham",
                    "Ma khach hang",
                    "So sao",
                    "Tieu de",
                    "Noi dung",
                    "Phan hoi",
                    "Trang thai",
                    "Ngay tao",
                    "Ngay tra loi"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (Review review : reviews) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(review.getId() != null ? review.getId().toString() : "");
                row.createCell(1).setCellValue(review.getProductId() != null ? review.getProductId().toString() : "");
                row.createCell(2).setCellValue(review.getUserId() != null ? review.getUserId().toString() : "");
                row.createCell(3).setCellValue(review.getRating() == null ? 0 : review.getRating());
                row.createCell(4).setCellValue(review.getTitle() == null ? "" : review.getTitle());
                row.createCell(5).setCellValue(review.getContent() == null ? "" : review.getContent());
                row.createCell(6).setCellValue(review.getReplyContent() == null ? "" : review.getReplyContent());
                row.createCell(7).setCellValue(review.getStatus() == null ? "" : review.getStatus().name());
                row.createCell(8).setCellValue(
                        review.getCreatedAt() == null ? "" : formatter.format(review.getCreatedAt()));
                row.createCell(9).setCellValue(
                        review.getRepliedAt() == null ? "" : formatter.format(review.getRepliedAt()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export reviews", ex);
        }
    }

    public void delete(UUID id) {
        UUID productId = reviewRepository.findById(id).map(Review::getProductId).orElse(null);
        reviewImageRepository.deleteByReviewId(id);
        reviewRepository.deleteById(id);
        invalidateProductCaches(productId);
    }

    private void invalidateProductCaches(UUID productId) {
        if (productId != null) {
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("review", "product", productId));
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("review", "detail", "product", productId));
        }
    }

    public void invalidateProductCacheByProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            return;
        }
        try {
            invalidateProductCaches(UUID.fromString(productId));
        } catch (IllegalArgumentException ex) {
            // Ignore malformed event payload.
        }
    }

    private ReviewResponse toResponse(Review r, List<ReviewImageResponse> images) {
        return new ReviewResponse(r.getId(), r.getProductId(), r.getUserId(), r.getRating(), r.getTitle(),
                r.getContent(), r.getReplyContent(),
                r.getStatus().name(), r.getCreatedAt(), r.getUpdatedAt(), r.getRepliedAt(),
                images == null ? List.of() : images);
    }

    private Page<ReviewResponse> loadProductReviews(UUID productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> result = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable);
        Map<UUID, List<ReviewImageResponse>> imageMap = loadImages(result.getContent());
        return result.map(review -> toResponse(review, imageMap.get(review.getId())));
    }

    private ReviewSummaryResponse loadProductReviewSummary(UUID productId) {
        ReviewStatus status = ReviewStatus.PUBLISHED;
        long total = reviewRepository.countByProductIdAndStatus(productId, status);
        Double avg = reviewRepository.avgRatingByProductIdAndStatus(productId, status);
        Map<Integer, Long> counts = new HashMap<>();
        reviewRepository.countRatingsByProductIdAndStatus(productId, status)
                .forEach(row -> counts.put(row.getRating(), row.getTotal()));
        return new ReviewSummaryResponse(productId, avg == null ? 0 : avg, total, counts);
    }

    private List<ReviewImageResponse> saveImages(UUID reviewId, List<ReviewImageRequest> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<ReviewImage> entities = images.stream()
                .filter(image -> image != null && image.url() != null && !image.url().isBlank())
                .map(image -> {
                    ReviewImage entity = new ReviewImage();
                    entity.setId(UUID.randomUUID());
                    entity.setReviewId(reviewId);
                    entity.setImageUrl(image.url());
                    entity.setBucket(image.bucket());
                    entity.setObjectKey(image.key());
                    entity.setCreatedAt(Instant.now());
                    return entity;
                })
                .toList();
        if (entities.isEmpty()) {
            return List.of();
        }
        reviewImageRepository.saveAll(entities);
        return entities.stream().map(this::toImageResponse).toList();
    }

    private Map<UUID, List<ReviewImageResponse>> loadImages(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ids = reviews.stream().map(Review::getId).toList();
        List<ReviewImage> images = reviewImageRepository.findByReviewIdIn(ids);
        Map<UUID, List<ReviewImageResponse>> map = new HashMap<>();
        for (ReviewImage image : images) {
            map.computeIfAbsent(image.getReviewId(), key -> new ArrayList<>())
                    .add(toImageResponse(image));
        }
        return map;
    }

    private List<ReviewImageResponse> loadImagesForReview(UUID reviewId) {
        List<ReviewImage> images = reviewImageRepository.findByReviewIdIn(List.of(reviewId));
        if (images.isEmpty()) {
            return List.of();
        }
        return images.stream().map(this::toImageResponse).toList();
    }

    private ReviewImageResponse toImageResponse(ReviewImage image) {
        return new ReviewImageResponse(image.getId(), image.getImageUrl(), image.getBucket(), image.getObjectKey(),
                image.getCreatedAt());
    }
}
