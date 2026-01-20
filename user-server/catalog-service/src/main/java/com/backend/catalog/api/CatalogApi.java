package com.backend.catalog.api;

import com.backend.catalog.api.dto.CategoryRequest;
import com.backend.catalog.api.dto.DrugRequest;
import com.backend.catalog.model.Category;
import com.backend.catalog.model.Drug;
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
    public ResponseEntity<Page<Drug>> publicProducts(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(catalogService.searchPublicProducts(q, categoryId, pageable));
    }

    @GetMapping("/public/products/{idOrSlug}")
    public ResponseEntity<Drug> publicProductDetail(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(catalogService.getPublicProduct(idOrSlug));
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
    public ResponseEntity<Drug> createDrug(@RequestBody @Valid DrugRequest req) {
        return ResponseEntity.ok(catalogService.createDrug(req));
    }

    @GetMapping("/internal/products")
    public ResponseEntity<Page<Drug>> listProducts(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(catalogService.searchProducts(q, categoryId, status, pageable));
    }

    @GetMapping("/internal/products/{id}")
    public ResponseEntity<Drug> getProduct(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(catalogService.getDrug(id));
    }

    @PutMapping("/internal/products/{id}")
    public ResponseEntity<Drug> updateDrug(@PathVariable("id") UUID id, @RequestBody @Valid DrugRequest req) {
        return ResponseEntity.ok(catalogService.updateDrug(id, req));
    }

    @DeleteMapping("/internal/products/{id}")
    public ResponseEntity<Void> deleteDrug(@PathVariable("id") UUID id) {
        catalogService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }
}
