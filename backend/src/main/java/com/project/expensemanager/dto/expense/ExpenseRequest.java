package com.project.expensemanager.dto.expense;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest (
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        String description,

        @NotNull(message = "Expense amount is required")
        @Digits(integer = 7, fraction = 2)
        @DecimalMin("0.01")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        Integer categoryId,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate
) {}

