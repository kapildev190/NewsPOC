package com.example.news.service;

public record ArticleApprovedEvent(Long articleId, String authorUsername, String title) {}