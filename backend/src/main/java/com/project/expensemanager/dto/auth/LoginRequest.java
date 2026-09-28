package com.project.expensemanager.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required for login")
        @Size(max = 50, message = "Username cannot exceed 50 characters")
        String username,

        @Schema(
                minLength = 8,
                maxLength = 255,
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required for login")
        @Size(max = 255, message = "Password cannot exceed 255 characters")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password
) {
}