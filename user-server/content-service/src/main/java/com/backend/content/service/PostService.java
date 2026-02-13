package com.backend.content.service;

import com.backend.content.api.dto.*;
import com.backend.content.model.*;
import com.backend.content.repo.PostImageRepository;
import com.backend.content.repo.PostRepository;
import com.backend.content.repo.PostTagRepository;
import com.backend.content.repo.TagRepository;
import com.backend.content.security.SecurityUtils;
import com.backend.content.util.ExcerptUtils;
import com.backend.content.util.SlugUtils;
import com.backend.content.util.TocUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final PostImageRepository postImageRepository;
    private final ContentUserService userService;
    private final ObjectMapper objectMapper;

    public PostService(PostRepository postRepository,
            TagRepository tagRepository,
            PostTagRepository postTagRepository,
            PostImageRepository postImageRepository,
            ContentUserService userService,
            ObjectMapper objectMapper) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
        this.postImageRepository = postImageRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public PagedResponse<PostListItem> list(String q, String tag, String topic, String type, String level,
            Boolean featured, String status, int page, int pageSize, String sortBy, String sortDir,
            boolean isAdmin) {

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Sort sort = resolvePostSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, sort);

        final ModerationStatus statusFilter;
        if (status != null && !status.isBlank()) {
            statusFilter = ModerationStatus.valueOf(status.trim().toUpperCase());
        } else {
            statusFilter = null;
        }

        final UUID tagId = resolveTagId(tag, TagType.POST);

        Page<Post> pageResult = postRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdmin) {
                predicates.add(cb.equal(root.get("moderationStatus"), ModerationStatus.PUBLISHED));
            } else if (statusFilter != null) {
                predicates.add(cb.equal(root.get("moderationStatus"), statusFilter));
            }
            if (q != null && !q.isBlank()) {
                String keyword = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("excerpt")), keyword)));
            }
            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.equal(root.get("topic"), topic.trim().toLowerCase()));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type.trim().toLowerCase()));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("level"), level.trim().toLowerCase()));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (tagId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<PostTag> pt = subquery.from(PostTag.class);
                subquery.select(pt.get("id").get("postId"))
                        .where(cb.equal(pt.get("id").get("tagId"), tagId));
                predicates.add(root.get("id").in(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<Post> posts = pageResult.getContent();
        Map<UUID, List<TagDto>> tagMap = loadTagsForPosts(posts);
        Map<UUID, ContentUser> authorMap = userService.findByIds(
                posts.stream().map(Post::getAuthorId).filter(Objects::nonNull).distinct().toList());

        List<PostListItem> items = posts.stream().map(p -> {
            ContentUser author = authorMap.get(p.getAuthorId());
            UserSummaryDto authorDto = userService.toSummary(author, false);
            return new PostListItem(
                    p.getId(),
                    p.getSlug(),
                    p.getTitle(),
                    p.getExcerpt(),
                    p.getCoverImageUrl(),
                    p.getReadingMinutes(),
                    tagMap.getOrDefault(p.getId(), List.of()),
                    authorDto,
                    p.getModerationStatus().name(),
                    p.getPublishedAt(),
                    p.getViews());
        }).toList();

        return new PagedResponse<>(items, new Pagination(safePage, safeSize, pageResult.getTotalElements()));
    }

    public PostDetailResponse getBySlug(String slug, boolean isAdmin) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (!isAdmin && post.getModerationStatus() != ModerationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        List<TagDto> tags = loadTagsForPost(post.getId());
        ContentUser author = userService.findByIds(List.of(post.getAuthorId())).get(post.getAuthorId());
        UserSummaryDto authorDto = userService.toSummary(author, false);
        List<TocItem> toc = TocUtils.generate(post.getContentJson(), post.getContentHtml(), objectMapper);
        List<RelatedPostItem> relatedPosts = postRepository
                .findTop5ByModerationStatusAndIdNotOrderByPublishedAtDesc(ModerationStatus.PUBLISHED, post.getId())
                .stream()
                .map(p -> new RelatedPostItem(p.getId(), p.getSlug(), p.getTitle()))
                .toList();
        Object contentJsonObj = parseJson(post.getContentJson());
        List<PostImageDto> images = loadImagesForPost(post.getId());

        return new PostDetailResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getContentHtml(),
                contentJsonObj,
                toc,
                post.getCoverImageUrl(),
                images,
                tags,
                authorDto,
                post.getDisclaimer(),
                post.getPublishedAt(),
                post.getUpdatedAt(),
                post.getViews(),
                relatedPosts);
    }

    public PostDetailResponse create(PostCreateRequest req) {
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

        String slugBase = SlugUtils.toSlug(req.slug() == null || req.slug().isBlank() ? title : req.slug());
        if (slugBase.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        String slug = SlugUtils.uniqueSlug(slugBase, postRepository::existsBySlug);

        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setTitle(title);
        post.setSlug(slug);
        String contentJson = toJsonString(req.contentJson());
        post.setExcerpt(resolveExcerpt(req.excerpt(), req.contentHtml(), contentJson));
        post.setContentHtml(req.contentHtml());
        post.setContentJson(contentJson);
        post.setCoverImageUrl(req.coverImageUrl());
        post.setType(toLower(req.type()));
        post.setLevel(toLower(req.level()));
        post.setTopic(toLower(req.topic()));
        post.setFeatured(req.featured() != null && req.featured());
        post.setDisclaimer(req.disclaimer());
        post.setReadingMinutes(resolveReadingMinutes(req.readingMinutes(), req.contentHtml(), contentJson));
        post.setAuthorId(actorId);
        post.setModerationStatus(ModerationStatus.DRAFT);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        updatePostImages(post.getId(), req.images());
        updatePostTags(post.getId(), req.tags(), TagType.POST);

        return getBySlug(post.getSlug(), true);
    }

    public PostDetailResponse update(UUID id, PostUpdateRequest req) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        String slugInput = req.slug() == null ? post.getSlug() : req.slug().trim();
        String slugBase = SlugUtils.toSlug(slugInput.isBlank() ? title : slugInput);
        if (slugBase.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        if (!slugBase.equals(post.getSlug())) {
            String slug = SlugUtils.uniqueSlug(slugBase, postRepository::existsBySlug);
            post.setSlug(slug);
        }

        post.setTitle(title);
        String contentJson = toJsonString(req.contentJson());
        post.setExcerpt(resolveExcerpt(req.excerpt(), req.contentHtml(), contentJson));
        post.setContentHtml(req.contentHtml());
        post.setContentJson(contentJson);
        post.setCoverImageUrl(req.coverImageUrl());
        post.setType(toLower(req.type()));
        post.setLevel(toLower(req.level()));
        post.setTopic(toLower(req.topic()));
        post.setFeatured(req.featured() != null && req.featured());
        post.setDisclaimer(req.disclaimer());
        post.setReadingMinutes(resolveReadingMinutes(req.readingMinutes(), req.contentHtml(), contentJson));
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        updatePostImages(post.getId(), req.images());
        updatePostTags(post.getId(), req.tags(), TagType.POST);

        return getBySlug(post.getSlug(), true);
    }

    public PostDetailResponse publish(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setModerationStatus(ModerationStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return getBySlug(post.getSlug(), true);
    }

    public PostDetailResponse unpublish(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setModerationStatus(ModerationStatus.ARCHIVED);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return getBySlug(post.getSlug(), true);
    }

    public void delete(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        postImageRepository.deleteByPostId(id);
        postTagRepository.deleteByIdPostId(id);
        postRepository.delete(post);
    }

    public void incrementView(UUID id) {
        int updated = postRepository.incrementViews(id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
    }

    private String resolveExcerpt(String excerpt, String contentHtml, String contentJson) {
        if (excerpt != null && !excerpt.isBlank()) {
            return excerpt.trim();
        }
        String source = contentHtml != null && !contentHtml.isBlank() ? contentHtml : contentJson;
        return ExcerptUtils.toExcerpt(source == null ? "" : source, 180);
    }

    private int resolveReadingMinutes(Integer readingMinutes, String contentHtml, String contentJson) {
        if (readingMinutes != null && readingMinutes > 0) {
            return readingMinutes;
        }
        String source = contentHtml != null && !contentHtml.isBlank() ? contentHtml : contentJson;
        if (source == null) {
            return 1;
        }
        String plain = source.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        int words = plain.isBlank() ? 0 : plain.split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    private String toLower(String input) {
        return input == null ? null : input.trim().toLowerCase();
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

    private void updatePostTags(UUID postId, List<String> tagSlugs, TagType type) {
        if (tagSlugs == null) {
            return;
        }
        List<Tag> tags = resolveTagsBySlugs(tagSlugs, type);
        postTagRepository.deleteByIdPostId(postId);
        if (!tags.isEmpty()) {
            List<PostTag> links = tags.stream()
                    .map(tag -> new PostTag(new PostTagId(postId, tag.getId())))
                    .toList();
            postTagRepository.saveAll(links);
        }
    }

    private List<PostImageDto> loadImagesForPost(UUID postId) {
        return postImageRepository.findByPostIdOrderByPositionAsc(postId)
                .stream()
                .map(img -> new PostImageDto(img.getId(), img.getImageUrl(), img.getAltText(), img.getPosition()))
                .toList();
    }

    private void updatePostImages(UUID postId, List<PostImageRequest> images) {
        if (images == null) {
            return;
        }
        postImageRepository.deleteByPostId(postId);
        if (images.isEmpty()) {
            return;
        }
        List<PostImage> toSave = new ArrayList<>();
        int index = 0;
        for (PostImageRequest img : images) {
            if (img == null || img.url() == null || img.url().isBlank()) {
                continue;
            }
            PostImage entity = new PostImage();
            entity.setId(UUID.randomUUID());
            entity.setPostId(postId);
            entity.setImageUrl(img.url().trim());
            entity.setAltText(img.altText());
            entity.setPosition(img.position() != null ? img.position() : index);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            toSave.add(entity);
            index += 1;
        }
        if (!toSave.isEmpty()) {
            postImageRepository.saveAll(toSave);
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

    private Map<UUID, List<TagDto>> loadTagsForPosts(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        List<PostTag> links = postTagRepository.findByIdPostIdIn(postIds);
        Set<UUID> tagIds = links.stream().map(link -> link.getId().getTagId()).collect(Collectors.toSet());
        Map<UUID, Tag> tagMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        Map<UUID, List<TagDto>> result = new HashMap<>();
        for (PostTag link : links) {
            Tag tag = tagMap.get(link.getId().getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(link.getId().getPostId(), k -> new ArrayList<>())
                    .add(new TagDto(tag.getId(), tag.getName(), tag.getSlug()));
        }
        return result;
    }

    private List<TagDto> loadTagsForPost(UUID postId) {
        List<PostTag> links = postTagRepository.findByIdPostIdIn(List.of(postId));
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

    private Sort resolvePostSort(String sortBy, String sortDir) {
        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "views" -> "views";
            case "createdAt" -> "createdAt";
            default -> "publishedAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }

    private String toJsonString(Object json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(json);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentJson không hợp lệ");
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }
}
