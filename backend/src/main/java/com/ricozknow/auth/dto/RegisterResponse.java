package com.ricozknow.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID tenantId,
        UUID userId,
        String tenantSlug,
        String email,
        String role,
        String message
) {
}