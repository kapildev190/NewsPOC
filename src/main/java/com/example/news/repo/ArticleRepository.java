package com.example.news.repo;

import com.example.news.entity.Article;
import com.example.news.entity.ArticleStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatusAndCategoryIgnoreCase(ArticleStatus status, String category, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByAuthorUsername(String username, Pageable pageable);

    Optional<Article> findByIdAndAuthorUsername(Long id, String username);   // ownership check
    Optional<Article> findByIdAndStatus(Long id, ArticleStatus status);
}
