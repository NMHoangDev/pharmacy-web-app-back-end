package com.backend.catalog.service;

import com.backend.catalog.api.dto.CategoryRequest;
import com.backend.catalog.api.dto.DrugRequest;
import com.backend.catalog.model.Category;
import com.backend.catalog.model.Drug;
import com.backend.catalog.repo.CategoryRepository;
import com.backend.catalog.repo.DrugRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final DrugRepository drugRepository;

    public CatalogService(CategoryRepository categoryRepository, DrugRepository drugRepository) {
        this.categoryRepository = categoryRepository;
        this.drugRepository = drugRepository;
    }

    // Public queries
    public List<Category> listPublicCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public Page<Drug> searchPublicProducts(String q, UUID categoryId, Pageable pageable) {
        return drugRepository.searchActive(q == null || q.isBlank() ? null : q.trim(), categoryId, pageable);
    }

    public Drug getPublicProduct(String idOrSlug) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return drugRepository.findById(id).orElseThrow();
        } catch (IllegalArgumentException ex) {
            return drugRepository.findBySlug(idOrSlug).orElseThrow();
        }
    }

    // Category
    public Category createCategory(CategoryRequest req) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName(req.name());
        c.setSlug(req.slug());
        c.setDescription(req.description());
        c.setActive(req.active() == null || req.active());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        c.setCreatedAt(Instant.now());
        return categoryRepository.save(c);
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    public Category updateCategory(UUID id, CategoryRequest req) {
        Category c = categoryRepository.findById(id).orElseThrow();
        c.setName(req.name());
        c.setSlug(req.slug());
        c.setDescription(req.description());
        c.setActive(req.active() == null || req.active());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        return categoryRepository.save(c);
    }

    public void deleteCategory(UUID id) {
        categoryRepository.deleteById(id);
    }

    // Drug
    public Drug createDrug(DrugRequest req) {
        Drug d = new Drug();
        d.setId(UUID.randomUUID());
        d.setSku(req.sku());
        d.setName(req.name());
        d.setSlug(req.slug());
        d.setCategoryId(req.categoryId());
        d.setPrice(req.price());
        d.setStatus(req.status());
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return drugRepository.save(d);
    }

    public List<Drug> listDrugs() {
        return drugRepository.findAll();
    }

    public Drug getDrug(UUID id) {
        return drugRepository.findById(id).orElseThrow();
    }

    public Drug updateDrug(UUID id, DrugRequest req) {
        Drug d = drugRepository.findById(id).orElseThrow();
        d.setSku(req.sku());
        d.setName(req.name());
        d.setSlug(req.slug());
        d.setCategoryId(req.categoryId());
        d.setPrice(req.price());
        d.setStatus(req.status());
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setUpdatedAt(Instant.now());
        return drugRepository.save(d);
    }

    public void deleteDrug(UUID id) {
        drugRepository.deleteById(id);
    }
}
