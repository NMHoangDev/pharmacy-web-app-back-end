package com.backend.content.api;

import com.backend.content.api.dto.*;
import com.backend.content.model.Report;
import com.backend.content.model.TargetType;
import com.backend.content.model.Thread;
import com.backend.content.repo.ThreadRepository;
import com.backend.content.security.SecurityUtils;
import com.backend.content.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/content")
public class ContentApi {

    private final PostService postService;
    private final ThreadService threadService;
    private final AnswerService answerService;
    private final TagService tagService;
    private final ModerationService moderationService;
    private final ReportService reportService;
    private final ThreadRepository threadRepository;

    public ContentApi(PostService postService,
            ThreadService threadService,
            AnswerService answerService,
            TagService tagService,
            ModerationService moderationService,
            ReportService reportService,
            ThreadRepository threadRepository) {
        this.postService = postService;
        this.threadService = threadService;
        this.answerService = answerService;
        this.tagService = tagService;
        this.moderationService = moderationService;
        this.reportService = reportService;
        this.threadRepository = threadRepository;
    }

    @GetMapping("/posts")
    public ResponseEntity<PagedResponse<PostListItem>> listPosts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MOD");
        if (status != null && !status.isBlank() && !isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Forbidden");
        }
        int safePage = page == null ? 1 : page;
        int safeSize = pageSize == null ? 10 : pageSize;
        return ResponseEntity.ok(postService.list(q, tag, topic, type, level, featured, status,
                safePage, safeSize, sortBy, sortDir, isAdmin));
    }

    @GetMapping("/public/posts")
    public ResponseEntity<PagedResponse<PostListItem>> listPublicPosts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir) {
        int safePage = page == null ? 1 : page;
        int safeSize = pageSize == null ? 10 : pageSize;
        return ResponseEntity.ok(postService.list(q, tag, topic, type, level, featured, null,
                safePage, safeSize, sortBy, sortDir, false));
    }

    @GetMapping("/admin/posts")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PagedResponse<PostListItem>> listAdminPosts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir) {
        int safePage = page == null ? 1 : page;
        int safeSize = pageSize == null ? 10 : pageSize;
        return ResponseEntity.ok(postService.list(q, tag, topic, type, level, featured, status,
                safePage, safeSize, sortBy, sortDir, true));
    }

    @GetMapping("/posts/{slug}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable String slug) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MOD");
        return ResponseEntity.ok(postService.getBySlug(slug, isAdmin));
    }

    @GetMapping("/public/posts/{slug}")
    public ResponseEntity<PostDetailResponse> getPublicPost(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getBySlug(slug, false));
    }

    @GetMapping("/admin/posts/{slug}")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PostDetailResponse> getAdminPost(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getBySlug(slug, true));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PostDetailResponse> createPost(@RequestBody @Valid PostCreateRequest req) {
        return ResponseEntity.ok(postService.create(req));
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PostDetailResponse> updatePost(@PathVariable UUID id,
            @RequestBody @Valid PostUpdateRequest req) {
        return ResponseEntity.ok(postService.update(id, req));
    }

    @PostMapping("/posts/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PostDetailResponse> publishPost(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.publish(id));
    }

    @PostMapping("/posts/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PostDetailResponse> unpublishPost(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.unpublish(id));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable UUID id) {
        postService.delete(id);
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }

    @PostMapping("/posts/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<Map<String, Object>> deletePostViaPost(@PathVariable UUID id) {
        postService.delete(id);
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }

    @GetMapping("/posts/{id}/view")
    public ResponseEntity<Map<String, Object>> viewPost(@PathVariable UUID id) {
        postService.incrementView(id);
        return ResponseEntity.ok(Map.of("id", id, "viewed", true));
    }

    @PostMapping("/posts/{id}/view")
    public ResponseEntity<Map<String, Object>> viewPostViaPost(@PathVariable UUID id) {
        postService.incrementView(id);
        return ResponseEntity.ok(Map.of("id", id, "viewed", true));
    }

    @GetMapping("/questions")
    public ResponseEntity<PagedResponse<QuestionListItem>> listQuestions(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "hasPharmacistAnswer", required = false) Boolean hasPharmacistAnswer,
            @RequestParam(name = "askerId", required = false) UUID askerId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir) {
        int safePage = page == null ? 1 : page;
        int safeSize = pageSize == null ? 10 : pageSize;
        return ResponseEntity.ok(threadService.list(q, tag, status, hasPharmacistAnswer, askerId,
                safePage, safeSize, sortBy, sortDir));
    }

    @GetMapping("/admin/questions")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<PagedResponse<QuestionListItem>> listAdminQuestions(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "hasPharmacistAnswer", required = false) Boolean hasPharmacistAnswer,
            @RequestParam(name = "askerId", required = false) UUID askerId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir) {
        int safePage = page == null ? 1 : page;
        int safeSize = pageSize == null ? 10 : pageSize;
        return ResponseEntity.ok(threadService.list(q, tag, status, hasPharmacistAnswer, askerId,
                safePage, safeSize, sortBy, sortDir));
    }

    @GetMapping("/questions/{slug}")
    public ResponseEntity<QuestionDetailResponse> getQuestion(@PathVariable String slug,
            @RequestParam(name = "answerPage", required = false) Integer answerPage,
            @RequestParam(name = "answerPageSize", required = false) Integer answerPageSize,
            @RequestParam(name = "sortAnswersBy", required = false) String sortAnswersBy) {
        int safePage = answerPage == null ? 1 : answerPage;
        int safeSize = answerPageSize == null ? 10 : answerPageSize;
        String sort = sortAnswersBy == null ? "best" : sortAnswersBy;
        return ResponseEntity.ok(threadService.getBySlug(slug, safePage, safeSize, sort));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuestionDetailResponse> createQuestion(@RequestBody @Valid QuestionCreateRequest req) {
        return ResponseEntity.ok(threadService.create(req));
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAnyRole('USER','MOD','ADMIN')")
    public ResponseEntity<QuestionDetailResponse> updateQuestion(@PathVariable UUID id,
            @RequestBody @Valid QuestionUpdateRequest req) {
        boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");
        return ResponseEntity.ok(threadService.update(id, req, isModerator));
    }

    @PostMapping("/questions/{id}/close")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<QuestionDetailResponse> closeQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(threadService.close(id));
    }

    @PostMapping("/questions/{id}/resolve")
    @PreAuthorize("hasAnyRole('USER','MOD','ADMIN')")
    public ResponseEntity<QuestionDetailResponse> resolveQuestion(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getActorId();
        Thread thread = threadRepository.findById(id).orElse(null);
        boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");
        boolean isOwner = thread != null && actorId != null && actorId.equals(thread.getAskerId());
        return ResponseEntity.ok(threadService.resolve(id, isModerator || isOwner));
    }

    @PostMapping("/questions/{id}/answers")
    @PreAuthorize("hasAnyRole('USER','PHARMACIST','MOD','ADMIN')")
    public ResponseEntity<AnswerItem> createAnswer(@PathVariable UUID id,
            @RequestBody @Valid AnswerCreateRequest req) {
        return ResponseEntity.ok(answerService.create(id, req));
    }

    @PutMapping("/answers/{id}")
    @PreAuthorize("hasAnyRole('USER','PHARMACIST','MOD','ADMIN')")
    public ResponseEntity<AnswerItem> updateAnswer(@PathVariable UUID id,
            @RequestBody @Valid AnswerUpdateRequest req) {
        boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");
        return ResponseEntity.ok(answerService.update(id, req, isModerator));
    }

    @PostMapping("/answers/{id}/pin")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<AnswerItem> pinAnswer(@PathVariable UUID id) {
        return ResponseEntity.ok(answerService.pin(id, true));
    }

    @PostMapping("/answers/{id}/best")
    @PreAuthorize("hasAnyRole('USER','PHARMACIST','MOD','ADMIN')")
    public ResponseEntity<AnswerItem> bestAnswer(@PathVariable UUID id) {
        UUID actorId = SecurityUtils.getActorId();
        boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");
        boolean isPharmacist = SecurityUtils.hasRole("PHARMACIST");
        UUID threadId = answerService.getThreadId(id);
        Thread thread = threadRepository.findById(threadId).orElse(null);
        boolean isOwner = thread != null && actorId != null && actorId.equals(thread.getAskerId());
        return ResponseEntity.ok(answerService.best(id, isModerator || isPharmacist || isOwner));
    }

    @PostMapping("/answers/{id}/vote")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> voteAnswer(@PathVariable UUID id,
            @RequestBody @Valid VoteRequest req) {
        answerService.vote(id, req.value());
        return ResponseEntity.ok(Map.of("id", id, "value", req.value()));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TagDto>> listTags(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(tagService.list(q, type));
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TagDto> createTag(@RequestBody @Valid TagCreateRequest req) {
        return ResponseEntity.ok(tagService.create(req));
    }

    @PostMapping("/reports")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody @Valid ReportCreateRequest req) {
        Report report = reportService.create(req);
        return ResponseEntity.ok(Map.of("id", report.getId(), "status", report.getStatus().name()));
    }

    @GetMapping("/moderation/queue")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<List<ModerationQueueItem>> moderationQueue() {
        return ResponseEntity.ok(moderationService.queue());
    }

    @PostMapping("/moderation/{targetType}/{id}/approve")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String targetType, @PathVariable UUID id) {
        moderationService.approve(TargetType.valueOf(targetType.toUpperCase()), id);
        return ResponseEntity.ok(Map.of("id", id, "status", "APPROVED"));
    }

    @PostMapping("/moderation/{targetType}/{id}/reject")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable String targetType, @PathVariable UUID id,
            @RequestBody @Valid ModerationReasonRequest req) {
        moderationService.reject(TargetType.valueOf(targetType.toUpperCase()), id, req.reason());
        return ResponseEntity.ok(Map.of("id", id, "status", "REJECTED"));
    }

    @PostMapping("/moderation/{targetType}/{id}/hide")
    @PreAuthorize("hasAnyRole('MOD','ADMIN')")
    public ResponseEntity<Map<String, Object>> hide(@PathVariable String targetType, @PathVariable UUID id,
            @RequestBody @Valid ModerationReasonRequest req) {
        moderationService.hide(TargetType.valueOf(targetType.toUpperCase()), id, req.reason());
        return ResponseEntity.ok(Map.of("id", id, "status", "HIDDEN"));
    }
}
