package com.project.expensemanager.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetRequest(
        @NotNull(message = "Amount is required")
        @Digits(integer = 7, fraction = 2)
        @DecimalMin("0.01")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotNull(message = "Budget period is required")
        LocalDate budgetPeriod
) {
}
