package com.backend.user.service;

import com.backend.user.api.dto.AddressRequest;
import com.backend.user.api.dto.AddressResponse;
import com.backend.user.api.dto.HealthProfileRequest;
import com.backend.user.api.dto.HealthProfileResponse;
import com.backend.user.api.dto.ProfileRequest;
import com.backend.user.api.dto.ProfileResponse;
import com.backend.user.model.Address;
import com.backend.user.model.HealthProfile;
import com.backend.user.model.User;
import com.backend.user.repo.AddressRepository;
import com.backend.user.repo.HealthProfileRepository;
import com.backend.user.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final HealthProfileRepository healthRepository;

    public UserService(UserRepository userRepository,
            AddressRepository addressRepository,
            HealthProfileRepository healthRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.healthRepository = healthRepository;
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
        userRepository.save(u);
        return toProfile(u);
    }

    public List<ProfileResponse> list() {
        return userRepository.findAll().stream().map(this::toProfile).toList();
    }

    public ProfileResponse get(UUID id) {
        return toProfile(userRepository.findById(id).orElseThrow(() -> notFound()));
    }

    public ProfileResponse update(UUID id, ProfileRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> notFound());
        u.setEmail(req.email());
        u.setPhone(req.phone());
        u.setFullName(req.fullName());
        userRepository.save(u);
        return toProfile(u);
    }

    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    private ProfileResponse toProfile(User u) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName());
    }

    private ResponseStatusException notFound() {
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
            throw notFound();
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
