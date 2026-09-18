package com.ricozknow.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_daily_search_stats")
@Getter
@Setter
@NoArgsConstructor
public class DailySearchStat {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "query_text", nullable = false)
    private String queryText;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "volume_count")
    private int volumeCount;

    @Column(name = "zero_result_count")
    private int zeroResultCount;

    @Column(name = "unsuccessful_count")
    private int unsuccessfulCount;
}
