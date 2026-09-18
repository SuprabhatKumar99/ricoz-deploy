package com.ricozknow.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.search.dto.SynonymRequest;
import com.ricozknow.search.dto.SynonymResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/synonyms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SynonymController {

    private final SynonymService synonymService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<SynonymResponse> list() {
        return synonymService.list().stream().map(s -> SynonymResponse.from(s, objectMapper)).toList();
    }

    @PostMapping
    public ResponseEntity<SynonymResponse> upsert(@Valid @RequestBody SynonymRequest request) {
        return ResponseEntity.ok(SynonymResponse.from(synonymService.upsert(request), objectMapper));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        synonymService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
