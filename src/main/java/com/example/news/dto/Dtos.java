package com.example.news.dto;

import com.example.news.entity.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record CreateUserRequest(@NotBlank String username, @NotBlank @Size(min = 6) String password,
                                    @NotBlank String fullName, @NotNull Role role) {}
    public record UserResponse(Long id, String username, String fullName, Role role, boolean enabled) {
        public static UserResponse of(AppUser u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled());
        }
    }

    public record ArticleRequest(@NotBlank String title, @Size(max = 500) String summary,
                                 @NotBlank String content, String category) {}
    public record RejectRequest(@NotBlank String comment) {}

    /** For journalists and editors: includes workflow info. */
    public record ArticleResponse(Long id, String title, String summary, String content, String category,
                                  String author, ArticleStatus status, String reviewComment,
                                  Instant createdAt, Instant publishedAt) {
        public static ArticleResponse of(Article a) {
            return new ArticleResponse(a.getId(), a.getTitle(), a.getSummary(), a.getContent(), a.getCategory(),
                    a.getAuthor().getFullName(), a.getStatus(), a.getReviewComment(), a.getCreatedAt(), a.getPublishedAt());
        }
    }

    /** For end users: no workflow internals. */
    public record PublicArticleResponse(Long id, String title, String summary, String content,
                                        String category, String author, Instant publishedAt) {
        public static PublicArticleResponse of(Article a) {
            return new PublicArticleResponse(a.getId(), a.getTitle(), a.getSummary(), a.getContent(),
                    a.getCategory(), a.getAuthor().getFullName(), a.getPublishedAt());
        }
    }
}
