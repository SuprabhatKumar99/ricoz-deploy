package com.ricozknow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Tenant is identified explicitly by slug at login time, since the request may
 * not carry a resolvable tenant subdomain (e.g. API clients, custom domains later).
 */
public record LoginRequest(
        @NotBlank String tenantSlug,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
