package com.backend.content.repo;

import com.backend.content.model.ContentUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentUserRepository extends JpaRepository<ContentUser, UUID> {
}
