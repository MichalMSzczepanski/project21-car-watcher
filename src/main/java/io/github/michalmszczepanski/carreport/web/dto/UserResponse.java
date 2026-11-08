package io.github.michalmszczepanski.carreport.web.dto;

import io.github.michalmszczepanski.carreport.domain.AuthProvider;
import io.github.michalmszczepanski.carreport.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        boolean emailVerified,
        AuthProvider authProvider,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getAuthProvider(),
                user.getCreatedAt()
        );
    }
}
