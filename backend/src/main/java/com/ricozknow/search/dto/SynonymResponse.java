package com.ricozknow.search.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.search.SearchSynonym;

import java.util.List;
import java.util.UUID;

public record SynonymResponse(UUID id, String term, List<String> synonyms) {
    public static SynonymResponse from(SearchSynonym entity, ObjectMapper mapper) {
        try {
            List<String> list = mapper.readerForListOf(String.class).readValue(entity.getSynonyms());
            return new SynonymResponse(entity.getId(), entity.getTerm(), list);
        } catch (Exception ex) {
            throw new IllegalStateException("Corrupt synonym data for " + entity.getId(), ex);
        }
    }
}
