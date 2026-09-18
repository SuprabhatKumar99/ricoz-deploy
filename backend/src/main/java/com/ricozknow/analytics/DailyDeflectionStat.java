package com.ricozknow.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_daily_deflection_stats")
@Getter
@Setter
@NoArgsConstructor
public class DailyDeflectionStat {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "estimated_deflection_count")
    private int estimatedDeflectionCount;

    @Column(name = "confirmed_deflection_count")
    private int confirmedDeflectionCount;

    @Column(name = "support_contact_count")
    private int supportContactCount;
}
