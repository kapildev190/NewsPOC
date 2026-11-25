package com.example.news.service;

import com.example.news.dto.Dtos.*;
import com.example.news.entity.*;
import com.example.news.repo.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleRepository articles;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public ArticleService(ArticleRepository articles, UserRepository users,ApplicationEventPublisher events) { this.articles = articles; this.users = users; this.events = events; }

    // ---------------- journalist ----------------
    @Transactional
    public ArticleResponse create(String username, ArticleRequest r) {
        AppUser author = users.findByUsername(username).orElseThrow(() -> new NotFoundException("User"));
        return ArticleResponse.of(articles.save(new Article(author, r.title(), r.summary(), r.content(), r.category())));
    }

    @Transactional
    public ArticleResponse update(String username, Long id, ArticleRequest r) {
        Article a = own(username, id);
        a.edit(r.title(), r.summary(), r.content(), r.category());   // dirty checking saves it
        return ArticleResponse.of(a);
    }

    @Transactional
    public ArticleResponse submit(String username, Long id) {
        Article a = own(username, id);
        a.submit();
        return ArticleResponse.of(a);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> mine(String username, Pageable p) {
        return articles.findByAuthorUsername(username, p).map(ArticleResponse::of);
    }

    // ---------------- editor ----------------
    @Transactional(readOnly = true)
    public Page<ArticleResponse> pending(Pageable p) {
        return articles.findByStatus(ArticleStatus.PENDING_REVIEW, p).map(ArticleResponse::of);
    }

    @Transactional
    public ArticleResponse approve(String editorUsername, Long id) {
        Article a = find(id);
        a.approve(editor(editorUsername));
        events.publishEvent(new ArticleApprovedEvent(
                a.getId(), a.getAuthor().getUsername(), a.getTitle()));
        return ArticleResponse.of(a);
    }

    @Transactional
    public BulkApproveResponse approveAll(String editorUsername, List<Long> ids) {
        AppUser editor = findUser(editorUsername);
        Map<Long, Article> found = articles.findAllById(ids).stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<Long> approved = new ArrayList<>();
        List<Long> skipped = new ArrayList<>();
        for (Long id : ids) {
            Article a = found.get(id);
            if (a != null && a.getStatus() == ArticleStatus.PENDING_REVIEW) {
                a.approve(editor);
                approved.add(id);
            } else {
                skipped.add(id);
            }
        }
        return new BulkApproveResponse(approved, skipped);
    }

    private AppUser findUser(String editorUsername) {
        return users.findByUsername(editorUsername).orElseThrow(() -> new NotFoundException("User"));
    }

    @Transactional
    public ArticleResponse reject(String editorUsername, Long id, String comment) {
        Article a = find(id);
        a.reject(editor(editorUsername), comment);
        return ArticleResponse.of(a);
    }

    // ---------------- public (end users) ----------------
    @Transactional(readOnly = true)
    public Page<PublicArticleResponse> published(String category, Pageable p) {
        Page<Article> page = (category == null || category.isBlank())
                ? articles.findByStatus(ArticleStatus.APPROVED, p)
                : articles.findByStatusAndCategoryIgnoreCase(ArticleStatus.APPROVED, category, p);
        return page.map(PublicArticleResponse::of);
    }

    @Transactional(readOnly = true)
    public PublicArticleResponse publishedById(Long id) {
        return PublicArticleResponse.of(articles.findByIdAndStatus(id, ArticleStatus.APPROVED)
                .orElseThrow(() -> new NotFoundException("Article " + id)));
    }

    // ---------------- helpers ----------------
    private Article own(String username, Long id) {   // journalist can only touch own articles
        return articles.findByIdAndAuthorUsername(id, username).orElseThrow(() -> new NotFoundException("Article " + id));
    }
    private Article find(Long id) { return articles.findById(id).orElseThrow(() -> new NotFoundException("Article " + id)); }
    private AppUser editor(String username) { return users.findByUsername(username).orElseThrow(() -> new NotFoundException("User")); }


    @Transactional(readOnly = true)
    public Page<PublicArticleResponse> search(String title, Pageable pageable) {
        return articles.findByStatusAndTitleContainingIgnoreCase(ArticleStatus.APPROVED, title, pageable)
                .map(PublicArticleResponse::of);
    }

    @Transactional(readOnly = true)
    public Map<ArticleStatus, Long> stats(String username) {
        Map<ArticleStatus, Long> result = new EnumMap<>(ArticleStatus.class);
        for (ArticleStatus s : ArticleStatus.values()) result.put(s, 0L);   // zero defaults
        articles.countByStatus(username).forEach(c -> result.put(c.getStatus(), c.getTotal()));
        return result;
    }

    private Article findArticle(Long id) {
        return articles.findById(id)
                .orElseThrow(() -> new NotFoundException("Article " + id));
    }

    @Transactional
    public ArticleResponse archive(Long id) {
        Article a = findArticle(id);
        a.archive();
        return ArticleResponse.of(a);
    }
}
