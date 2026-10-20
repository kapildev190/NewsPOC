package com.example.news.web;

import com.example.news.dto.Dtos.PublicArticleResponse;
import com.example.news.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/** No login needed. Only APPROVED articles are ever returned. */
@RestController
@RequestMapping("/api/public/articles")
public class PublicArticleController {
    private final ArticleService service;
    public PublicArticleController(ArticleService service) { this.service = service; }

    @GetMapping
    public Page<PublicArticleResponse> list(@RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.published(category, p);
    }

    @GetMapping("/{id}")
    public PublicArticleResponse get(@PathVariable Long id) { return service.publishedById(id); }
}
