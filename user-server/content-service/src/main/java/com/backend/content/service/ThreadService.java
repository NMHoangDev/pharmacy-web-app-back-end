package com.backend.content.service;

import com.backend.content.api.dto.AnswerItem;
import com.backend.content.api.dto.ModerationQueueItem;
import com.backend.content.api.dto.PagedResponse;
import com.backend.content.api.dto.Pagination;
import com.backend.content.api.dto.QuestionContext;
import com.backend.content.api.dto.QuestionCreateRequest;
import com.backend.content.api.dto.QuestionDetailResponse;
import com.backend.content.api.dto.QuestionListItem;
import com.backend.content.api.dto.QuestionUpdateRequest;
import com.backend.content.api.dto.TagDto;
import com.backend.content.api.dto.UserSummaryDto;
import com.backend.content.model.Answer;
import com.backend.content.model.ContentUser;
import com.backend.content.model.ModerationStatus;
import com.backend.content.model.Tag;
import com.backend.content.model.TagType;
import com.backend.content.model.Thread;
import com.backend.content.model.ThreadStatus;
import com.backend.content.model.ThreadTag;
import com.backend.content.model.ThreadTagId;
import com.backend.content.repo.AnswerRepository;
import com.backend.content.repo.ThreadAnswerCount;
import com.backend.content.repo.TagRepository;
import com.backend.content.repo.ThreadRepository;
import com.backend.content.repo.ThreadTagRepository;
import com.backend.content.security.SecurityUtils;
import com.backend.content.util.ExcerptUtils;
import com.backend.content.util.SlugUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final ThreadTagRepository threadTagRepository;
    private final TagRepository tagRepository;
    private final AnswerRepository answerRepository;
    private final ContentUserService userService;
    private final ObjectMapper objectMapper;
    private final int ownerEditMinutes;

    public ThreadService(ThreadRepository threadRepository,
            ThreadTagRepository threadTagRepository,
            TagRepository tagRepository,
            AnswerRepository answerRepository,
            ContentUserService userService,
            ObjectMapper objectMapper,
            @Value("${content.thread.owner-edit-minutes:15}") int ownerEditMinutes) {
        this.threadRepository = threadRepository;
        this.threadTagRepository = threadTagRepository;
        this.tagRepository = tagRepository;
        this.answerRepository = answerRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.ownerEditMinutes = ownerEditMinutes;
    }

    public PagedResponse<QuestionListItem> list(String q, String tag, String status, Boolean hasPharmacistAnswer,
            UUID askerId, int page, int pageSize, String sortBy, String sortDir) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Sort sort = resolveThreadSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, sort);

        UUID actorId = SecurityUtils.getActorId();
        boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");

        if (askerId != null && !isModerator) {
            if (actorId == null || !actorId.equals(askerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
            }
        }

        ModerationStatus moderationFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                moderationFilter = ModerationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                moderationFilter = null;
            }
        }
        final ModerationStatus finalModerationFilter = moderationFilter;

        UUID tagId = resolveTagId(tag, TagType.THREAD);

        Page<Thread> pageResult = threadRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isModerator) {
                if (actorId != null) {
                    Predicate published = cb.equal(root.get("moderationStatus"), ModerationStatus.PUBLISHED);
                    Predicate ownPending = cb.and(
                            cb.equal(root.get("moderationStatus"), ModerationStatus.PENDING),
                            cb.equal(root.get("askerId"), actorId));
                    predicates.add(cb.or(published, ownPending));
                } else {
                    predicates.add(cb.equal(root.get("moderationStatus"), ModerationStatus.PUBLISHED));
                }
            }

            if (q != null && !q.isBlank()) {
                String keyword = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("content")), keyword)));
            }
            if (finalModerationFilter != null) {
                predicates.add(cb.equal(root.get("moderationStatus"), finalModerationFilter));
            }
            if (hasPharmacistAnswer != null) {
                predicates.add(cb.equal(root.get("hasPharmacistAnswer"), hasPharmacistAnswer));
            }
            if (askerId != null) {
                predicates.add(cb.equal(root.get("askerId"), askerId));
            }
            if (tagId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ThreadTag> tt = subquery.from(ThreadTag.class);
                subquery.select(tt.get("id").get("threadId"))
                        .where(cb.equal(tt.get("id").get("tagId"), tagId));
                predicates.add(root.get("id").in(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<Thread> threads = pageResult.getContent();
        Map<UUID, List<TagDto>> tagMap = loadTagsForThreads(threads);
        Map<UUID, Long> answerCountMap = loadAnswerCounts(threads, isModerator);
        Map<UUID, ContentUser> askerMap = userService.findByIds(
                threads.stream().map(Thread::getAskerId).filter(Objects::nonNull).distinct().toList());

        List<QuestionListItem> items = threads.stream().map(t -> {
            ContentUser asker = askerMap.get(t.getAskerId());
            UserSummaryDto askerDto = userService.toSummary(asker, t.isAnonymous());
            return new QuestionListItem(
                    t.getId(),
                    t.getTitle(),
                    t.getSlug(),
                    ExcerptUtils.toExcerpt(t.getContent(), 180),
                    tagMap.getOrDefault(t.getId(), List.of()),
                    askerDto,
                    t.getThreadStatus().name(),
                    t.getModerationStatus().name(),
                    safeAnswerCount(answerCountMap.get(t.getId())),
                    t.isHasPharmacistAnswer(),
                    t.getCreatedAt(),
                    t.getLastActivityAt());
        }).toList();

        return new PagedResponse<>(items, new Pagination(safePage, safeSize, pageResult.getTotalElements()));
    }

    private Map<UUID, Long> loadAnswerCounts(List<Thread> threads, boolean includeUnpublished) {
        if (threads == null || threads.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = threads.stream().map(Thread::getId).toList();
        List<ThreadAnswerCount> counts = includeUnpublished
                ? answerRepository.countByThreadIds(ids)
                : answerRepository.countByThreadIdsAndStatus(ids, ModerationStatus.PUBLISHED);
        Map<UUID, Long> result = new HashMap<>();
        for (ThreadAnswerCount count : counts) {
            result.put(count.getThreadId(), count.getTotal());
        }
        return result;
    }

    private int safeAnswerCount(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    public QuestionDetailResponse getBySlug(String slug, int answerPage, int answerPageSize, String sortAnswersBy) {
        Thread thread = threadRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        if (thread.getModerationStatus() != ModerationStatus.PUBLISHED) {
            UUID actorId = SecurityUtils.getActorId();
            boolean isModerator = SecurityUtils.hasRole("MOD") || SecurityUtils.hasRole("ADMIN");
            boolean isOwner = actorId != null && actorId.equals(thread.getAskerId());
            if (!isModerator && !isOwner) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
            }
            return buildDetail(thread, answerPage, answerPageSize, sortAnswersBy, true);
        }
        return buildDetail(thread, answerPage, answerPageSize, sortAnswersBy, false);
    }

    public QuestionDetailResponse create(QuestionCreateRequest req) {
        UUID actorId = SecurityUtils.getActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String displayName = SecurityUtils.getDisplayName();
        String role = SecurityUtils.getPrimaryRole();
        userService.ensureUser(actorId, displayName, role);

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        String slugBase = SlugUtils.toSlug(title);
        if (slugBase.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        String slug = SlugUtils.uniqueSlug(slugBase, threadRepository::existsBySlug);

        Thread thread = new Thread();
        thread.setId(UUID.randomUUID());
        thread.setTitle(title);
        thread.setSlug(slug);
        thread.setContent(req.content());
        thread.setContextJson(toJson(req.context()));
        thread.setAskerId(actorId);
        thread.setAnonymous(req.isAnonymous() != null && req.isAnonymous());
        thread.setThreadStatus(ThreadStatus.OPEN);
        thread.setModerationStatus(ModerationStatus.PENDING);
        thread.setCreatedAt(Instant.now());
        thread.setUpdatedAt(Instant.now());
        thread.setLastActivityAt(Instant.now());
        thread.setAnswerCount(0);
        thread.setHasPharmacistAnswer(false);
        threadRepository.save(thread);

        updateThreadTags(thread.getId(), req.tags(), TagType.THREAD);

        return buildDetail(thread, 1, 10, "best", true);
    }

    public QuestionDetailResponse update(UUID id, QuestionUpdateRequest req, boolean isModerator) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

        UUID actorId = SecurityUtils.getActorId();
        if (!isModerator) {
            if (actorId == null || !actorId.equals(thread.getAskerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
            }
            Instant deadline = thread.getCreatedAt().plus(ownerEditMinutes, ChronoUnit.MINUTES);
            if (Instant.now().isAfter(deadline)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Edit window expired");
            }
        }

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        thread.setTitle(title);
        thread.setContent(req.content());
        thread.setContextJson(toJson(req.context()));
        thread.setAnonymous(req.isAnonymous() != null && req.isAnonymous());
        thread.setUpdatedAt(Instant.now());
        threadRepository.save(thread);

        updateThreadTags(thread.getId(), req.tags(), TagType.THREAD);

        return buildDetail(thread, 1, 10, "best", true);
    }

    public QuestionDetailResponse close(UUID id) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        thread.setThreadStatus(ThreadStatus.CLOSED);
        thread.setUpdatedAt(Instant.now());
        threadRepository.save(thread);
        return buildDetail(thread, 1, 10, "best", true);
    }

    public QuestionDetailResponse resolve(UUID id, boolean isOwnerOrMod) {
        Thread thread = threadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        if (!isOwnerOrMod) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        thread.setThreadStatus(ThreadStatus.RESOLVED);
        thread.setUpdatedAt(Instant.now());
        threadRepository.save(thread);
        return buildDetail(thread, 1, 10, "best", true);
    }

    private QuestionDetailResponse buildDetail(Thread thread, int answerPage, int answerPageSize,
            String sortAnswersBy, boolean includeUnpublished) {
        if (!includeUnpublished && thread.getModerationStatus() != ModerationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }
        int safePage = Math.max(1, answerPage);
        int safeSize = Math.min(Math.max(answerPageSize, 1), 100);
        Pageable pageable = buildAnswerPageable(safePage, safeSize, sortAnswersBy);

        Page<Answer> answersPage = "best".equalsIgnoreCase(sortAnswersBy)
                ? answerRepository.findBestByThreadId(thread.getId(), pageable)
                : answerRepository.findByThreadId(thread.getId(), pageable);

        Map<UUID, ContentUser> authorMap = userService.findByIds(
                answersPage.getContent().stream().map(Answer::getAuthorId).filter(Objects::nonNull).distinct()
                        .toList());

        List<AnswerItem> answers = answersPage.getContent().stream().map(a -> {
            ContentUser author = authorMap.get(a.getAuthorId());
            UserSummaryDto authorDto = userService.toSummary(author, false);
            return new AnswerItem(a.getId(), a.getContent(), authorDto, a.isPinned(), a.isBestAnswer(),
                    a.getCreatedAt());
        }).toList();

        PagedResponse<AnswerItem> answerResponse = new PagedResponse<>(
                answers, new Pagination(safePage, safeSize, answersPage.getTotalElements()));

        ContentUser asker = userService.findByIds(List.of(thread.getAskerId())).get(thread.getAskerId());
        UserSummaryDto askerDto = userService.toSummary(asker, thread.isAnonymous());
        List<TagDto> tags = loadTagsForThread(thread.getId());

        QuestionContext context = parseContext(thread.getContextJson());

        return new QuestionDetailResponse(
                thread.getId(),
                thread.getTitle(),
                thread.getContent(),
                context,
                tags,
                askerDto,
                thread.getThreadStatus().name(),
                thread.getModerationStatus().name(),
                thread.getCreatedAt(),
                answerResponse);
    }

    public void touchAfterAnswer(Thread thread, boolean isPharmacist) {
        thread.setAnswerCount(thread.getAnswerCount() + 1);
        thread.setLastActivityAt(Instant.now());
        if (isPharmacist) {
            thread.setHasPharmacistAnswer(true);
            if (thread.getThreadStatus() == ThreadStatus.OPEN || thread.getThreadStatus() == ThreadStatus.NEED_INFO) {
                thread.setThreadStatus(ThreadStatus.ANSWERED);
            }
        }
        thread.setUpdatedAt(Instant.now());
        threadRepository.save(thread);
    }

    private QuestionContext parseContext(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, QuestionContext.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(QuestionContext context) {
        if (context == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Context không hợp lệ");
        }
    }

    private UUID resolveTagId(String slug, TagType type) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Tag tag = tagRepository.findBySlug(slug.trim().toLowerCase())
                .filter(t -> t.getType() == type)
                .orElse(null);
        return tag == null ? null : tag.getId();
    }

    private void updateThreadTags(UUID threadId, List<String> tagSlugs, TagType type) {
        if (tagSlugs == null) {
            return;
        }
        List<Tag> tags = resolveTagsBySlugs(tagSlugs, type);
        threadTagRepository.deleteByIdThreadId(threadId);
        if (!tags.isEmpty()) {
            List<ThreadTag> links = tags.stream()
                    .map(tag -> new ThreadTag(new ThreadTagId(threadId, tag.getId())))
                    .toList();
            threadTagRepository.saveAll(links);
        }
    }

    private List<Tag> resolveTagsBySlugs(List<String> tagSlugs, TagType type) {
        if (tagSlugs == null || tagSlugs.isEmpty()) {
            return List.of();
        }
        List<String> normalized = tagSlugs.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<Tag> tags = tagRepository.findBySlugIn(normalized).stream()
                .filter(t -> t.getType() == type)
                .toList();
        if (tags.size() != normalized.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một hoặc nhiều tag không tồn tại");
        }
        return tags;
    }

    private Map<UUID, List<TagDto>> loadTagsForThreads(List<Thread> threads) {
        if (threads.isEmpty()) {
            return Map.of();
        }
        List<UUID> threadIds = threads.stream().map(Thread::getId).toList();
        List<ThreadTag> links = threadTagRepository.findByIdThreadIdIn(threadIds);
        Set<UUID> tagIds = links.stream().map(link -> link.getId().getTagId()).collect(Collectors.toSet());
        Map<UUID, Tag> tagMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        Map<UUID, List<TagDto>> result = new HashMap<>();
        for (ThreadTag link : links) {
            Tag tag = tagMap.get(link.getId().getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(link.getId().getThreadId(), k -> new ArrayList<>())
                    .add(new TagDto(tag.getId(), tag.getName(), tag.getSlug()));
        }
        return result;
    }

    private List<TagDto> loadTagsForThread(UUID threadId) {
        List<ThreadTag> links = threadTagRepository.findByIdThreadIdIn(List.of(threadId));
        if (links.isEmpty()) {
            return List.of();
        }
        Set<UUID> tagIds = links.stream().map(link -> link.getId().getTagId()).collect(Collectors.toSet());
        Map<UUID, Tag> tagMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));
        return links.stream()
                .map(link -> tagMap.get(link.getId().getTagId()))
                .filter(Objects::nonNull)
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getSlug()))
                .toList();
    }

    private Pageable buildAnswerPageable(int page, int size, String sortAnswersBy) {
        if ("oldest".equalsIgnoreCase(sortAnswersBy)) {
            return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Sort resolveThreadSort(String sortBy, String sortDir) {
        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "lastActivityAt" -> "lastActivityAt";
            case "answerCount" -> "answerCount";
            default -> "createdAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }
}
