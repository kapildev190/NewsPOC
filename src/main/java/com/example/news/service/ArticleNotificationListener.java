package com.example.news.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ArticleNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(ArticleNotificationListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(ArticleApprovedEvent e) {
        log.info("Notify {}: your article '{}' (id={}) was approved",
                e.authorUsername(), e.title(), e.articleId());
    }
}