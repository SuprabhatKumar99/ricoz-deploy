package com.ricozknow.auth.dto;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        UUID userId,
        UUID tenantId,
        String email,
        Set<String> roles
) {
}
