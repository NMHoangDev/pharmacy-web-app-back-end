package com.backend.cms.service;

import com.backend.cms.api.dto.ArticleRequest;
import com.backend.cms.api.dto.BannerRequest;
import com.backend.cms.api.dto.PageRequest;
import com.backend.cms.model.Article;
import com.backend.cms.model.Banner;
import com.backend.cms.model.Page;
import com.backend.cms.repository.ArticleRepository;
import com.backend.cms.repository.BannerRepository;
import com.backend.cms.repository.PageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsService {

    private final BannerRepository bannerRepository;
    private final ArticleRepository articleRepository;
    private final PageRepository pageRepository;

    public List<Banner> listBanners() {
        return bannerRepository.findAll();
    }

    public Banner getBanner(Long id) {
        return bannerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Banner not found"));
    }

    public Banner createBanner(BannerRequest request) {
        Banner banner = new Banner();
        apply(request, banner);
        return bannerRepository.save(banner);
    }

    public Banner updateBanner(Long id, BannerRequest request) {
        Banner banner = getBanner(id);
        apply(request, banner);
        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    public List<Article> listArticles() {
        return articleRepository.findAll();
    }

    public Article getArticle(Long id) {
        return articleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Article not found"));
    }

    public Article createArticle(ArticleRequest request) {
        Article article = new Article();
        apply(request, article);
        return articleRepository.save(article);
    }

    public Article updateArticle(Long id, ArticleRequest request) {
        Article article = getArticle(id);
        apply(request, article);
        return articleRepository.save(article);
    }

    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }

    public List<Page> listPages() {
        return pageRepository.findAll();
    }

    public Page getPage(Long id) {
        return pageRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Page not found"));
    }

    public Page createPage(PageRequest request) {
        Page page = new Page();
        apply(request, page);
        return pageRepository.save(page);
    }

    public Page updatePage(Long id, PageRequest request) {
        Page page = getPage(id);
        apply(request, page);
        return pageRepository.save(page);
    }

    public void deletePage(Long id) {
        pageRepository.deleteById(id);
    }

    private void apply(BannerRequest request, Banner banner) {
        banner.setTitle(request.title());
        banner.setImageUrl(request.imageUrl());
        banner.setTargetUrl(request.targetUrl());
        banner.setActiveFrom(request.activeFrom());
        banner.setActiveTo(request.activeTo());
        banner.setPriority(request.priority() == null ? 0 : request.priority());
        banner.setEnabled(request.enabled() == null || request.enabled());
    }

    private void apply(ArticleRequest request, Article article) {
        article.setTitle(request.title());
        article.setSlug(request.slug());
        article.setBody(request.body());
        article.setAuthor(request.author());
        article.setPublishedAt(request.publishedAt());
        article.setFeatured(request.featured() != null && request.featured());
        article.setEnabled(request.enabled() == null || request.enabled());
    }

    private void apply(PageRequest request, Page page) {
        page.setTitle(request.title());
        page.setSlug(request.slug());
        page.setContent(request.content());
        page.setPublished(request.published() != null && request.published());
    }
}
