package com.ricozknow.category;

import com.ricozknow.audit.AuditService;
import com.ricozknow.category.dto.CategoryRequest;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Category> list() {
        return categoryRepository.findByTenantIdOrderBySortOrderAsc(TenantContext.get());
    }

    @Transactional(readOnly = true)
    public Category get(UUID id) {
        return categoryRepository.findByTenantIdAndId(TenantContext.get(), id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        UUID tenantId = TenantContext.get();
        if (categoryRepository.existsByTenantIdAndSlug(tenantId, request.slug())) {
            throw new IllegalStateException("A category with slug '" + request.slug() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        if (request.parentId() != null) {
            category.setParent(get(request.parentId()));
        }
        category = categoryRepository.save(category);

        auditService.record("Category", category.getId(), "CATEGORY_CREATED", null, category.getName());
        return category;
    }

    @Transactional
    public Category update(UUID id, CategoryRequest request) {
        Category category = get(id);
        String oldName = category.getName();

        category.setName(request.name());
        category.setSlug(request.slug());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.parentId() != null && !request.parentId().equals(id)) {
            category.setParent(get(request.parentId()));
        } else if (request.parentId() == null) {
            category.setParent(null);
        }

        auditService.record("Category", id, "CATEGORY_UPDATED", oldName, category.getName());
        return category;
    }

    @Transactional
    public void delete(UUID id) {
        Category category = get(id);
        categoryRepository.delete(category);
        auditService.record("Category", id, "CATEGORY_DELETED", category.getName(), null);
    }
}
