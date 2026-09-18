package com.ricozknow.search;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SearchAdminController {

    private final IndexRebuildService indexRebuildService;

    /** Spec section 23 "index rebuild tooling" — an explicit, synchronous operator action. */
    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        int count = indexRebuildService.rebuildForCurrentTenant();
        return Map.of("indexed", count);
    }
}
