package com.ricozknow.user.dto;

import com.ricozknow.role.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Size(min = 12) String password,
        @NotEmpty Set<RoleName> roles
) {
}
