package com.project.expensemanager.web_layer_tests;

import com.project.expensemanager.controller.ExpenseController;
import com.project.expensemanager.controller.RecurringExpenseController;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.GlobalExceptionHandler;
import com.project.expensemanager.mapper.ExpenseMapper;
import com.project.expensemanager.mapper.RecurringExpenseMapper;
import com.project.expensemanager.security.CustomUserDetailsService;
import com.project.expensemanager.security.JWTService;
import com.project.expensemanager.security.JwtAuthenticationFilter;
import com.project.expensemanager.security.SecurityConfig;
import com.project.expensemanager.service.CategoryService;
import com.project.expensemanager.service.ExpenseService;
import com.project.expensemanager.service.RecurringExpenseService;
import com.project.expensemanager.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Security tests

@WebMvcTest({
        ExpenseController.class,
        RecurringExpenseController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class
})
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean ExpenseService expenseService;
    @MockitoBean RecurringExpenseService recurringExpenseService;
    @MockitoBean UserService userService;
    @MockitoBean CategoryService categoryService;
    @MockitoBean ExpenseMapper expenseMapper;
    @MockitoBean RecurringExpenseMapper recurringExpenseMapper;
    @MockitoBean JWTService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void protectedExpenseEndpointRejectsRequestWithoutAuthentication()
            throws Exception {
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, expenseService);
    }

    @Test
    void protectedRecurringExpenseEndpointRejectsRequestWithoutAuthentication()
            throws Exception {
        mvc.perform(get("/api/recurring-expenses"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, recurringExpenseService);
    }

    @Test
    @WithMockUser(username = "alice", authorities = "USER")
    void authenticatedUserCanAccessOwnExpenses() throws Exception {
        User alice = new User("alice");
        when(userService.getUserByUsername("alice")).thenReturn(alice);
        when(expenseService.getExpensesForUser(
                eq(alice), any(Pageable.class)
        )).thenReturn(Page.empty());

        mvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(userService).getUserByUsername("alice");
        verify(expenseService).getExpensesForUser(
                eq(alice), any(Pageable.class)
        );
    }
}