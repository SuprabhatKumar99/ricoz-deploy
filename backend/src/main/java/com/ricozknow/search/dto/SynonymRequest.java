package com.ricozknow.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SynonymRequest(
        @NotBlank String term,
        @NotEmpty List<String> synonyms
) {
}
