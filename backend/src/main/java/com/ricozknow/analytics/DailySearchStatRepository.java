package com.ricozknow.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailySearchStatRepository extends JpaRepository<DailySearchStat, UUID> {

    @Modifying
    @Query("delete from DailySearchStat s where s.tenantId = :tenantId and s.statDate = :date")
    void deleteByTenantIdAndStatDate(UUID tenantId, LocalDate date);

    List<DailySearchStat> findByTenantIdAndStatDateBetween(UUID tenantId, LocalDate from, LocalDate to);
}
