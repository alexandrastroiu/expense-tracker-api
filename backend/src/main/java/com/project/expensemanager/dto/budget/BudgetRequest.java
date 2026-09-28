package com.project.expensemanager.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetRequest(
        @Schema(
                minimum = "0.01",
                maximum = "9999999.99",
                multipleOf = 0.01,
                description = "Positive amount with at most 7 integer digits and 2 decimal places."
        )
        @NotNull(message = "Amount is required")
        @Digits(integer = 7, fraction = 2)
        @DecimalMin("0.01")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotNull(message = "Budget period is required")
        LocalDate budgetPeriod
) {
}
