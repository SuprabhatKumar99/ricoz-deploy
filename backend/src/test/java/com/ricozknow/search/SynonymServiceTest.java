package com.ricozknow.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynonymServiceTest {

    @Mock
    private SearchSynonymRepository repository;
    @Mock
    private AuditService auditService;

    private SynonymService synonymService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        synonymService = new SynonymService(repository, auditService, new ObjectMapper());
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void expandIncludesOriginalQueryAndMatchingSynonyms() throws Exception {
        SearchSynonym synonym = new SearchSynonym();
        synonym.setTerm("reset password");
        synonym.setSynonyms(new ObjectMapper().writeValueAsString(List.of("forgot password", "change password")));
        when(repository.findByTenantId(tenantId)).thenReturn(List.of(synonym));

        Set<String> expanded = synonymService.expand("how do I reset password");

        assertThat(expanded).contains("how do I reset password", "forgot password", "change password");
    }

    @Test
    void expandReturnsOnlyOriginalQueryWhenNoSynonymMatches() {
        when(repository.findByTenantId(tenantId)).thenReturn(List.of());

        Set<String> expanded = synonymService.expand("billing question");

        assertThat(expanded).containsExactly("billing question");
    }
}
