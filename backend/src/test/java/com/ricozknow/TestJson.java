package com.ricozknow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestJson() {}

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new AssertionError("Invalid test JSON", e);
        }
    }
}
