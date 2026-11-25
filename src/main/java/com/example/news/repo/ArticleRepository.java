package com.example.news.repo;

import com.example.news.entity.Article;
import com.example.news.entity.ArticleStatus;
import com.example.news.repo.StatusCount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatusAndCategoryIgnoreCase(ArticleStatus status, String category, Pageable pageable);

    @EntityGraph(attributePaths ={"author", "reviewer"})
    Page<Article> findByAuthorUsername(String username, Pageable pageable);

    Optional<Article> findByIdAndAuthorUsername(Long id, String username);   // ownership check
    Optional<Article> findByIdAndStatus(Long id, ArticleStatus status);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatusAndTitleContainingIgnoreCase(ArticleStatus status, String title, Pageable pageable);

    @Query("select a.status as status, count(a) as total from Article a where a.author.username = :username group by a.status")
    List<StatusCount> countByStatus(@Param("username") String username);
}
