package com.ricozknow.user.dto;

import com.ricozknow.user.User;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String name,
        User.UserStatus status,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getStatus(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()));
    }
}
