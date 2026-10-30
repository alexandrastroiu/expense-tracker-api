package com.project.expensemanager.dto.user;

import com.project.expensemanager.entity.AccountCurrency;

public record UserProfileResponse(
        String username,
        AccountCurrency currency
) {}