package com.example.news.web;

import com.example.news.dto.Dtos.*;
import com.example.news.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/editor/articles")
public class EditorArticleController {
    private final ArticleService service;
    public EditorArticleController(ArticleService service) { this.service = service; }

    @GetMapping("/pending")
    public Page<ArticleResponse> pending(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable p) {
        return service.pending(p);
    }

    @PostMapping("/{id}/approve")
    public ArticleResponse approve(Authentication auth, @PathVariable Long id) { return service.approve(auth.getName(), id); }

    @PostMapping("/{id}/reject")
    public ArticleResponse reject(Authentication auth, @PathVariable Long id, @Valid @RequestBody RejectRequest r) {
        return service.reject(auth.getName(), id, r.comment());
    }
}
