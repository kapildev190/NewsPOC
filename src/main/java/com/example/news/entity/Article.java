package com.example.news.entity;

import static com.example.news.entity.ArticleStatus.*;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Article extends BaseEntity {
    @Column(nullable = false)
    private String title;
    @Column(length = 500)
    private String summary;
    @Column(nullable = false, length = 20000)
    private String content;
    private String category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser author;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ArticleStatus status = DRAFT;

    // review audit
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser reviewer;
    private String reviewComment;
    private Instant reviewedAt;
    private Instant publishedAt;

    protected Article() {}
    public Article(AppUser author, String title, String summary, String content, String category) {
        this.author = author; this.title = title; this.summary = summary;
        this.content = content; this.category = category;
    }

    // ---- workflow (state machine lives in the entity) ----
    public void edit(String title, String summary, String content, String category) {
        if (status != DRAFT && status != REJECTED)
            throw new IllegalStateException("Only DRAFT or REJECTED articles can be edited (current: " + status + ")");
        this.title = title; this.summary = summary; this.content = content; this.category = category;
    }
    public void submit() {
        if (status != DRAFT && status != REJECTED)
            throw new IllegalStateException("Cannot submit an article in status " + status);
        status = PENDING_REVIEW;
    }
    public void approve(AppUser editor) {
        requirePending();
        status = APPROVED; reviewer = editor; reviewComment = null;
        reviewedAt = Instant.now(); publishedAt = reviewedAt;
    }
    public void reject(AppUser editor, String comment) {
        requirePending();
        status = REJECTED; reviewer = editor; reviewComment = comment; reviewedAt = Instant.now();
    }
    private void requirePending() {
        if (status != PENDING_REVIEW)
            throw new IllegalStateException("Only PENDING_REVIEW articles can be reviewed (current: " + status + ")");
    }

    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public AppUser getAuthor() { return author; }
    public ArticleStatus getStatus() { return status; }
    public String getReviewComment() { return reviewComment; }
    public Instant getPublishedAt() { return publishedAt; }
}
