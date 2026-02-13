package com.backend.content.repo;

import com.backend.content.model.Tag;
import com.backend.content.model.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);

    boolean existsBySlugAndType(String slug, TagType type);

    List<Tag> findBySlugIn(List<String> slugs);

    List<Tag> findByType(TagType type);
}
