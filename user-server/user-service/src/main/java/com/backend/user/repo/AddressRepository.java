package com.backend.user.repo;

import com.backend.user.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
