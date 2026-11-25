package com.example.news.repo;

import com.example.news.entity.ArticleStatus;

public interface StatusCount {
    ArticleStatus getStatus();
    long getTotal();
}