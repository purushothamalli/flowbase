package com.flowbase.engine.auth.dto;

import com.flowbase.engine.auth.domain.User;
import com.flowbase.engine.auth.domain.UserRole;

public record UserResponse(String id, String email, UserRole role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.role());
    }
}
