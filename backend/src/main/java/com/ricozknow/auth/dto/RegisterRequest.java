package com.ricozknow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(max = 255)
        String organizationName,

        @NotBlank
        @Size(min = 3, max = 100)
        String tenantSlug,

        @NotBlank
        @Size(min = 3, max = 100)
        String subdomain,

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 255)
        String name
) {
}