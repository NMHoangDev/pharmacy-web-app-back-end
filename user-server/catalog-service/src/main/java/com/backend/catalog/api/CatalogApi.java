package com.backend.catalog.api;

import com.backend.catalog.api.dto.BranchSettingRequest;
import com.backend.catalog.api.dto.CategoryRequest;
import com.backend.catalog.api.dto.DrugAdminDto;
import com.backend.catalog.api.dto.DrugPublicDto;
import com.backend.catalog.api.dto.DrugRequest;
import com.backend.catalog.model.Category;
import com.backend.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogApi {

    private final CatalogService catalogService;

    public CatalogApi(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("catalog-service ok");
    }

    // Public endpoints
    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> publicCategories() {
        return ResponseEntity.ok(catalogService.listPublicCategories());
    }

    @GetMapping("/public/products")
    public ResponseEntity<Page<DrugPublicDto>> publicProducts(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(catalogService.searchPublicProducts(q, categoryId, branchId, pageable));
    }

    @GetMapping("/public/products/{idOrSlug}")
    public ResponseEntity<DrugPublicDto> publicProductDetail(@PathVariable String idOrSlug,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return ResponseEntity.ok(catalogService.getPublicProduct(idOrSlug, branchId));
    }

    // Internal CRUD (seed/admin minimal)
    @PostMapping("/internal/categories")
    public ResponseEntity<Category> createCategory(@RequestBody @Valid CategoryRequest req) {
        return ResponseEntity.ok(catalogService.createCategory(req));
    }

    @GetMapping("/internal/categories")
    public ResponseEntity<List<Category>> listCategories() {
        return ResponseEntity.ok(catalogService.listCategories());
    }

    @GetMapping("/internal/categories/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(catalogService.getCategory(id));
    }

    @PutMapping("/internal/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable("id") UUID id,
            @RequestBody @Valid CategoryRequest req) {
        return ResponseEntity.ok(catalogService.updateCategory(id, req));
    }

    @DeleteMapping("/internal/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") UUID id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/products")
    public ResponseEntity<DrugAdminDto> createDrug(@RequestBody @Valid DrugRequest req) {
        return ResponseEntity.ok(catalogService.createDrug(req));
    }

    @GetMapping("/internal/products")
    public ResponseEntity<Page<DrugAdminDto>> listProducts(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(catalogService.searchProducts(q, categoryId, status, branchId, pageable));
    }

    @GetMapping("/internal/products/{id}")
    public ResponseEntity<DrugAdminDto> getProduct(@PathVariable("id") UUID id,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return ResponseEntity.ok(catalogService.getDrug(id, branchId));
    }

    @PutMapping("/internal/products/{id}")
    public ResponseEntity<DrugAdminDto> updateDrug(@PathVariable("id") UUID id,
            @RequestBody @Valid DrugRequest req) {
        return ResponseEntity.ok(catalogService.updateDrug(id, req));
    }

    @PutMapping("/internal/products/{id}/branch-settings")
    public ResponseEntity<DrugAdminDto> upsertBranchSetting(@PathVariable("id") UUID id,
            @RequestBody @Valid BranchSettingRequest req) {
        return ResponseEntity.ok(catalogService.upsertBranchSetting(id, req));
    }

    @DeleteMapping("/internal/products/{id}/branch-settings")
    public ResponseEntity<Void> deleteBranchSetting(@PathVariable("id") UUID id,
            @RequestParam(name = "branchId") UUID branchId) {
        catalogService.deleteBranchSetting(id, branchId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/products/{id}")
    public ResponseEntity<Void> deleteDrug(@PathVariable("id") UUID id) {
        catalogService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }
}
