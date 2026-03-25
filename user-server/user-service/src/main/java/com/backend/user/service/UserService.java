package com.backend.user.service;

import com.backend.user.api.dto.AddressRequest;
import com.backend.user.api.dto.AddressResponse;
import com.backend.user.api.dto.HealthProfileRequest;
import com.backend.user.api.dto.HealthProfileResponse;
import com.backend.user.api.dto.ProfileRequest;
import com.backend.user.api.dto.ProfileResponse;
import com.backend.user.cache.CacheConstants;
import com.backend.user.cache.CacheHelper;
import com.backend.user.cache.CacheKeyBuilder;
import com.backend.user.model.Address;
import com.backend.user.model.HealthProfile;
import com.backend.user.model.User;
import com.backend.user.repo.AddressRepository;
import com.backend.user.repo.HealthProfileRepository;
import com.backend.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;

@Service
@Transactional
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final HealthProfileRepository healthRepository;
    @PersistenceContext
    private EntityManager em;
    private final JdbcTemplate jdbc;
    private final CacheHelper cacheHelper;
    private final CacheKeyBuilder cacheKeyBuilder;

    public UserService(UserRepository userRepository,
            AddressRepository addressRepository,
            HealthProfileRepository healthRepository,
            JdbcTemplate jdbc,
            CacheHelper cacheHelper,
            CacheKeyBuilder cacheKeyBuilder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.healthRepository = healthRepository;
        this.jdbc = jdbc;
        this.cacheHelper = cacheHelper;
        this.cacheKeyBuilder = cacheKeyBuilder;
    }

    // Profile
    public ProfileResponse create(ProfileRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(req.email());
        u.setPhone(req.phone());
        u.setFullName(req.fullName());
        u.setAvatarBase64(req.avatarBase64());
        userRepository.save(u);
        invalidateUserCaches(u.getId());
        log.info("User created id={} email={}", u.getId(), u.getEmail());
        return toProfile(u);
    }

    public List<ProfileResponse> list() {
        return cacheHelper.getOrSetCacheByTtlKey(cacheKeyBuilder.list("user"), CacheConstants.TTL_USER_LIST,
                () -> userRepository.findAll().stream().map(this::toProfile).toList());
    }

    public ProfileResponse get(UUID id) {
        String cacheKey = cacheKeyBuilder.detail("user", id);
        return cacheHelper.getOrSetCacheByTtlKey(cacheKey, CacheConstants.TTL_USER_DETAIL, () -> {
            var found = userRepository.findById(id);
            if (found.isPresent()) {
                return toProfile(found.get());
            }

            // Diagnostic: check via native query to see if row exists and what the DB has
            try {
                log.warn("Diagnostic: EntityManager is null? {} | id class: {}", em == null,
                        id == null ? "null" : id.getClass());
                var q = em.createNativeQuery("select id from users where id = :id");
                q.setParameter("id", id.toString());
                var list = q.getResultList();
                log.warn("Native query (param) result count for id={}: {}", id, list.size());
                if (!list.isEmpty()) {
                    log.warn("Native query (param) first value for id={}: {}", id, list.get(0));
                }

                // Try inline SQL to avoid potential binding/type mismatches
                var inline = em.createNativeQuery("select id from users where id = '" + id.toString() + "'")
                        .getResultList();
                log.warn("Native query (inline) result count for id={}: {}", id, inline.size());
                if (!inline.isEmpty()) {
                    log.warn("Native query (inline) first value for id={}: {}", id, inline.get(0));
                }

            } catch (Exception ex) {
                log.error("Native query failed for id={}: {}", id, ex.toString(), ex);
            }

            // Fallback: try direct JDBC query in case JPA conversion/mapping failed
            try {
                var row = jdbc.queryForMap("select id, email, phone, full_name, avatar_base64 from users where id = ?",
                        id.toString());
                log.warn("JDBC fallback found row for id={}: {}", id, row);
                var u = new User();
                u.setId(UUID.fromString((String) row.get("id")));
                u.setEmail((String) row.get("email"));
                u.setPhone((String) row.get("phone"));
                u.setFullName((String) row.get("full_name"));
                u.setAvatarBase64((String) row.get("avatar_base64"));
                return toProfile(u);
            } catch (EmptyResultDataAccessException er) {
                // not found via JDBC either
            }

            return toProfile(userRepository.findById(id).orElseThrow(() -> notFound(id)));
        });
    }

    public ProfileResponse update(UUID id, ProfileRequest req) {
        var existing = userRepository.findById(id);
        User u;
        if (existing.isPresent()) {
            u = existing.get();
            u.setEmail(req.email());
            u.setPhone(req.phone());
            u.setFullName(req.fullName());
            u.setAvatarBase64(req.avatarBase64());
            userRepository.save(u);
            invalidateUserCaches(u.getId());
            log.info("User updated id={} email={}", u.getId(), u.getEmail());
        } else {
            // Upsert behavior: create new user with provided id
            u = new User();
            u.setId(id);
            u.setEmail(req.email());
            u.setPhone(req.phone());
            u.setFullName(req.fullName());
            u.setAvatarBase64(req.avatarBase64());
            try {
                userRepository.save(u);
                invalidateUserCaches(u.getId());
                log.info("User created by upsert id={} email={}", u.getId(), u.getEmail());
            } catch (DataIntegrityViolationException dive) {
                // Likely unique constraint on email - try to return the existing user by email
                log.warn("Upsert create failed due to data integrity for id={} email={}: {}", id, req.email(),
                        dive.toString());
                var existingByEmail = userRepository.findByEmail(req.email());
                if (existingByEmail.isPresent()) {
                    log.info("Returning existing user by email during upsert id={}", existingByEmail.get().getId());
                    return toProfile(existingByEmail.get());
                }
                throw dive;
            }
        }
        return toProfile(u);
    }

    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }
        userRepository.deleteById(id);
        invalidateUserCaches(id);
        log.info("User deleted id={}", id);
    }

    private void invalidateUserCaches(UUID userId) {
        cacheHelper.evictByPattern(cacheKeyBuilder.pattern("user", "list"));
        if (userId != null) {
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("user", "detail", userId));
        }
    }

    private ProfileResponse toProfile(User u) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(), u.getAvatarBase64());
    }

    private ResponseStatusException notFound(UUID id) {
        log.warn("User not found id={}", id);
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    // Addresses
    public List<AddressResponse> listAddresses(UUID userId) {
        ensureUser(userId);
        return addressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toAddress).toList();
    }

    public AddressResponse addAddress(UUID userId, AddressRequest req) {
        ensureUser(userId);
        Address a = new Address();
        a.setId(UUID.randomUUID());
        a.setUserId(userId);
        a.setLabel(req.label());
        a.setLine1(req.line1());
        a.setLine2(req.line2());
        a.setCity(req.city());
        a.setState(req.state());
        a.setPostalCode(req.postalCode());
        a.setCountry(req.country());
        a.setDefault(Boolean.TRUE.equals(req.isDefault()));
        return toAddress(addressRepository.save(a));
    }

    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest req) {
        ensureUser(userId);
        Address a = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        if (!a.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Address does not belong to user");
        }
        a.setLabel(req.label());
        a.setLine1(req.line1());
        a.setLine2(req.line2());
        a.setCity(req.city());
        a.setState(req.state());
        a.setPostalCode(req.postalCode());
        a.setCountry(req.country());
        a.setDefault(Boolean.TRUE.equals(req.isDefault()));
        return toAddress(addressRepository.save(a));
    }

    public void deleteAddress(UUID userId, UUID addressId) {
        ensureUser(userId);
        addressRepository.findById(addressId).ifPresent(address -> {
            if (!address.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Address does not belong to user");
            }
            addressRepository.delete(address);
        });
    }

    private AddressResponse toAddress(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                a.getPostalCode(), a.getCountry(), a.getDefault());
    }

    private void ensureUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw notFound(userId);
        }
    }

    // Health profile
    public HealthProfileResponse getHealth(UUID userId) {
        ensureUser(userId);
        HealthProfile hp = healthRepository.findById(userId).orElseGet(() -> emptyHealth(userId));
        return toHealth(hp);
    }

    public HealthProfileResponse upsertHealth(UUID userId, HealthProfileRequest req) {
        ensureUser(userId);
        HealthProfile hp = healthRepository.findById(userId).orElseGet(() -> emptyHealth(userId));
        hp.setBloodType(req.bloodType());
        hp.setAllergies(req.allergies());
        hp.setChronicConditions(req.chronicConditions());
        hp.setMedications(req.medications());
        hp.setNotes(req.notes());
        hp.setUpdatedAt(Instant.now());
        healthRepository.save(hp);
        return toHealth(hp);
    }

    private HealthProfile emptyHealth(UUID userId) {
        HealthProfile hp = new HealthProfile();
        hp.setUserId(userId);
        return hp;
    }

    private HealthProfileResponse toHealth(HealthProfile hp) {
        return new HealthProfileResponse(hp.getBloodType(), hp.getAllergies(), hp.getChronicConditions(),
                hp.getMedications(), hp.getNotes());
    }
}
