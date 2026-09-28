package com.project.expensemanager.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username cannot exceed 50 characters")
        String username,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @Schema(
                minLength = 8,
                maxLength = 255,
                description = "At least 8 characters and at most 72 bytes in UTF-8.",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Size(max = 255, message = "Password cannot exceed 255 characters")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email address is required")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email
) {
}