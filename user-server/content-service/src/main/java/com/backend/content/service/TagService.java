package com.backend.content.service;

import com.backend.content.api.dto.TagCreateRequest;
import com.backend.content.api.dto.TagDto;
import com.backend.content.model.Tag;
import com.backend.content.model.TagType;
import com.backend.content.repo.TagRepository;
import com.backend.content.util.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagDto> list(String q, String type) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        TagType parsedType = type == null || type.isBlank() ? null : TagType.valueOf(type.trim().toUpperCase());

        List<Tag> tags = parsedType == null ? tagRepository.findAll() : tagRepository.findByType(parsedType);
        return tags.stream()
                .filter(t -> keyword.isBlank() || t.getName().toLowerCase().contains(keyword)
                        || t.getSlug().toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(Tag::getName))
                .map(t -> new TagDto(t.getId(), t.getName(), t.getSlug()))
                .toList();
    }

    public TagDto create(TagCreateRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tag không hợp lệ");
        }
        TagType type = TagType.valueOf(req.type().trim().toUpperCase());
        String slugInput = req.slug() == null ? "" : req.slug().trim();
        String slugBase = SlugUtils.toSlug(slugInput.isBlank() ? name : slugInput);
        if (slugBase.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        String slug = SlugUtils.uniqueSlug(slugBase, s -> tagRepository.existsBySlugAndType(s, type));

        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        tag.setSlug(slug);
        tag.setType(type);
        tag.setCreatedAt(Instant.now());
        tagRepository.save(tag);

        return new TagDto(tag.getId(), tag.getName(), tag.getSlug());
    }
}
