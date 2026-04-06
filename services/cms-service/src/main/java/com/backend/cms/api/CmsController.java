package com.backend.cms.api;

import com.backend.cms.api.dto.ArticleRequest;
import com.backend.cms.api.dto.BannerRequest;
import com.backend.cms.api.dto.PageRequest;
import com.backend.cms.model.Article;
import com.backend.cms.model.Banner;
import com.backend.cms.model.Page;
import com.backend.cms.service.CmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms")
@RequiredArgsConstructor
public class CmsController {

    private final CmsService cmsService;

    @GetMapping("/banners")
    public List<Banner> listBanners() {
        return cmsService.listBanners();
    }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public Banner createBanner(@Valid @RequestBody BannerRequest request) {
        return cmsService.createBanner(request);
    }

    @GetMapping("/banners/{id}")
    public Banner getBanner(@PathVariable Long id) {
        return cmsService.getBanner(id);
    }

    @PutMapping("/banners/{id}")
    public Banner updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return cmsService.updateBanner(id, request);
    }

    @DeleteMapping("/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable Long id) {
        cmsService.deleteBanner(id);
    }

    @GetMapping("/articles")
    public List<Article> listArticles() {
        return cmsService.listArticles();
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public Article createArticle(@Valid @RequestBody ArticleRequest request) {
        return cmsService.createArticle(request);
    }

    @GetMapping("/articles/{id}")
    public Article getArticle(@PathVariable Long id) {
        return cmsService.getArticle(id);
    }

    @PutMapping("/articles/{id}")
    public Article updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return cmsService.updateArticle(id, request);
    }

    @DeleteMapping("/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable Long id) {
        cmsService.deleteArticle(id);
    }

    @GetMapping("/pages")
    public List<Page> listPages() {
        return cmsService.listPages();
    }

    @PostMapping("/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public Page createPage(@Valid @RequestBody PageRequest request) {
        return cmsService.createPage(request);
    }

    @GetMapping("/pages/{id}")
    public Page getPage(@PathVariable Long id) {
        return cmsService.getPage(id);
    }

    @PutMapping("/pages/{id}")
    public Page updatePage(@PathVariable Long id, @Valid @RequestBody PageRequest request) {
        return cmsService.updatePage(id, request);
    }

    @DeleteMapping("/pages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable Long id) {
        cmsService.deletePage(id);
    }
}
