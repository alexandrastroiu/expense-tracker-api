package com.project.expensemanager.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required for login")
        @Size(max = 50, message = "Username cannot exceed 50 characters")
        String username,

        @NotBlank(message = "Password is required for login")
        @Size(max = 255, message = "Password cannot exceed 255 characters")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password
) {
}