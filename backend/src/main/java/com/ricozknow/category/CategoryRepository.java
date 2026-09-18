package com.ricozknow.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByTenantIdOrderBySortOrderAsc(UUID tenantId);
    Optional<Category> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Category> findByTenantIdAndSlug(UUID tenantId, String slug);
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
}
