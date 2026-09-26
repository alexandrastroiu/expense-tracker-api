package com.project.expensemanager.dto.recurringexpense;

import com.project.expensemanager.entity.Frequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 100 characters")
        String description,

        @NotNull(message = "Expense amount is required")
        @Digits(integer = 7, fraction = 2)
        @DecimalMin("0.01")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        Integer categoryId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Frequency is required")
        Frequency frequency
) {
}
