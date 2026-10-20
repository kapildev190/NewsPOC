package com.example.news.web;

import com.example.news.dto.Dtos.*;
import com.example.news.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journalist/articles")
public class JournalistArticleController {
    private final ArticleService service;
    public JournalistArticleController(ArticleService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(Authentication auth, @Valid @RequestBody ArticleRequest r) {
        return service.create(auth.getName(), r);
    }

    @PutMapping("/{id}")
    public ArticleResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ArticleRequest r) {
        return service.update(auth.getName(), id, r);
    }

    @PostMapping("/{id}/submit")
    public ArticleResponse submit(Authentication auth, @PathVariable Long id) { return service.submit(auth.getName(), id); }

    @GetMapping
    public Page<ArticleResponse> mine(Authentication auth,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.mine(auth.getName(), p);
    }
}
