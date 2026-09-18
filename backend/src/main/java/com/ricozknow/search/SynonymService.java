package com.ricozknow.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import com.ricozknow.search.dto.SynonymRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Owns search_synonyms CRUD plus query-time expansion. Expansion happens at
 * the application layer (rather than as an OpenSearch synonym analyzer/filter)
 * so each tenant's synonym list can change without touching index settings or
 * requiring a reindex — a deliberate simplicity trade-off for MVP scale.
 */
@Service
@RequiredArgsConstructor
public class SynonymService {

    private final SearchSynonymRepository repository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SearchSynonym> list() {
        return repository.findByTenantId(TenantContext.get());
    }

    @Transactional
    public SearchSynonym upsert(SynonymRequest request) {
        UUID tenantId = TenantContext.get();
        SearchSynonym synonym = repository.findByTenantIdAndTermIgnoreCase(tenantId, request.term())
                .orElseGet(SearchSynonym::new);
        synonym.setTerm(request.term());
        try {
            synonym.setSynonyms(objectMapper.writeValueAsString(request.synonyms()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize synonyms", ex);
        }
        synonym = repository.save(synonym);
        auditService.record("SearchSynonym", synonym.getId(), "SYNONYM_UPSERTED", null, request.term());
        return synonym;
    }

    @Transactional
    public void delete(UUID id) {
        repository.findByTenantIdAndId(TenantContext.get(), id)
                .orElseThrow(() -> new IllegalArgumentException("Synonym not found: " + id));
        repository.deleteById(id);
        auditService.record("SearchSynonym", id, "SYNONYM_DELETED", null, null);
    }

    /**
     * Expands a raw query string into the original terms plus any configured
     * synonyms, deduplicated, preserving order. E.g. "reset password" with a
     * synonym entry {"term":"reset password","synonyms":["forgot password"]}
     * expands to ["reset password", "forgot password"].
     */
    @Transactional(readOnly = true)
    public Set<String> expand(String rawQuery) {
        Set<String> expanded = new LinkedHashSet<>();
        expanded.add(rawQuery);

        List<SearchSynonym> synonyms = list();
        String normalizedQuery = rawQuery.toLowerCase().trim();

        for (SearchSynonym synonym : synonyms) {
            if (normalizedQuery.contains(synonym.getTerm().toLowerCase())) {
                try {
                    List<String> terms = objectMapper.readerForListOf(String.class).readValue(synonym.getSynonyms());
                    expanded.addAll(terms);
                } catch (Exception ex) {
                    // Malformed synonym data shouldn't break search; just skip that entry.
                }
            }
        }
        return expanded;
    }
}
