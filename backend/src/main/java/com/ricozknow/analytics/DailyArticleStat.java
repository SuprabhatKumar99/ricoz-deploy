package com.ricozknow.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_daily_article_stats")
@Getter
@Setter
@NoArgsConstructor
public class DailyArticleStat {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    private int views;

    @Column(name = "helpful_count")
    private int helpfulCount;

    @Column(name = "unhelpful_count")
    private int unhelpfulCount;
}
