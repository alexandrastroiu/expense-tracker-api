package com.project.expensemanager.web_layer_tests;

import com.project.expensemanager.controller.ExpenseController;
import com.project.expensemanager.controller.RecurringExpenseController;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.GlobalExceptionHandler;
import com.project.expensemanager.exception.ResourceNotFoundException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.provider.Arguments;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controller tests

@WebMvcTest({
        ExpenseController.class,
        RecurringExpenseController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class
})
@WithMockUser(username = "alice", authorities = "USER")
class ControllerTest {

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

    @ParameterizedTest
    @MethodSource("badRequests")
    void invalidRequestsReturn400BeforeCallingServices(
            MockHttpServletRequestBuilder request
    ) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoBusinessCalls();
    }

    static Stream<MockHttpServletRequestBuilder> badRequests() {
        return Stream.of(
                // The recurring filter requires both amount bounds.
                get("/api/recurring-expenses/filter")
                        .param("maxAmount", "5"),

                get("/api/expenses/filter")
                        .param("start", "not-a-date"),

                get("/api/expenses/filter")
                        .param("minAmount", "not-a-number"),

                get("/api/expenses/not-an-integer"),

                post("/api/expenses")
                        .contentType(APPLICATION_JSON)
                        .content("{"),

                post("/api/expenses")
                        .contentType(APPLICATION_JSON),

                post("/api/recurring-expenses")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Subscription",
                                  "amount": 10.00,
                                  "categoryId": 1,
                                  "startDate": "2026-09-01",
                                  "frequency": "BIWEEKLY"
                                }
                                """)
        );
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(patch("/api/expenses/1")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));

        verifyNoBusinessCalls();
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mvc.perform(post("/api/expenses")
                        .contentType(TEXT_PLAIN)
                        .content("not JSON"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        verifyNoBusinessCalls();
    }

    @Test
    void validationErrorsIdentifyInvalidFields() throws Exception {
        mvc.perform(post("/api/expenses")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "amount": 0,
                                  "categoryId": 1,
                                  "expenseDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("title:")))
                .andExpect(jsonPath("$.message", containsString("amount:")));

        verifyNoBusinessCalls();
    }

    @Test
    void missingExpenseReturns404() throws Exception {
        User alice = new User("alice");
        when(userService.getUserByUsername("alice")).thenReturn(alice);
        when(expenseService.getUserExpenseById(alice, 99))
                .thenThrow(new ResourceNotFoundException(
                        "Expense not found."
                ));

        mvc.perform(get("/api/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Expense not found."));
    }

    @Test
    void unexpectedFailureReturnsGeneric500() throws Exception {
        when(userService.getUserByUsername("alice"))
                .thenThrow(new IllegalStateException(
                        "Sensitive database connection details"
                ));

        mvc.perform(get("/api/expenses/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred."));
    }

    @Test
    void deleteUsesAuthenticatedUserAndReturns204() throws Exception {
        User alice = new User("alice");
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        mvc.perform(delete("/api/expenses/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(expenseService).deleteExpense(alice, 7);
    }

    private void verifyNoBusinessCalls() {
        verifyNoInteractions(
                userService,
                expenseService,
                recurringExpenseService,
                categoryService,
                expenseMapper,
                recurringExpenseMapper
        );
    }

    // Test sorting validation
    @ParameterizedTest(name = "{0}: rejects {1}")
    @MethodSource("invalidSortRequests")
    void unsupportedSortReturns400BeforeCallingServices(
            String endpoint,
            String invalidField,
            MockHttpServletRequestBuilder request
    ) throws Exception {
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Unsupported sort field: " + invalidField
                ));

        verifyNoBusinessCalls();
    }

    static Stream<Arguments> invalidSortRequests() {
        return paginatedEndpoints().flatMap(endpoint ->
                Stream.of(
                        Arguments.of(
                                endpoint,
                                "unknownField",
                                paginatedRequest(endpoint)
                                        .param("sort", "unknownField,asc")
                        ),
                        Arguments.of(
                                endpoint,
                                "unknownField",
                                paginatedRequest(endpoint)
                                        .param(
                                                "sort",
                                                "amount,asc",
                                                "unknownField,desc"
                                        )
                        ),
                        Arguments.of(
                                endpoint,
                                "user.passwordHash",
                                paginatedRequest(endpoint)
                                        .param("sort", "user.passwordHash,asc")
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}: accepts multiple sort fields")
    @MethodSource("paginatedEndpoints")
    void validSortingIsPassedToService(String endpoint) throws Exception {
        User alice = new User("alice");
        when(userService.getUserByUsername("alice")).thenReturn(alice);

        switch (endpoint) {
            case "/api/expenses" ->
                    when(expenseService.getExpensesForUser(eq(alice), any(Pageable.class))).thenReturn(Page.empty());

            case "/api/expenses/search" ->
                    when(expenseService.searchExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            any(Pageable.class)
                    )).thenReturn(Page.empty());

            case "/api/expenses/filter" ->
                    when(expenseService.filterExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            any(Pageable.class)
                    )).thenReturn(Page.empty());

            case "/api/recurring-expenses" ->
                    when(recurringExpenseService.getAllUserRecurringExpenses(
                            eq(alice), any(Pageable.class)
                    )).thenReturn(Page.empty());

            case "/api/recurring-expenses/search" ->
                    when(recurringExpenseService.searchRecurringExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            any(Pageable.class)
                    )).thenReturn(Page.empty());

            case "/api/recurring-expenses/filter" ->
                    when(recurringExpenseService.filterRecurringExpensesByAmount(
                            eq(alice),
                            any(java.math.BigDecimal.class),
                            any(java.math.BigDecimal.class),
                            any(Pageable.class)
                    )).thenReturn(Page.empty());

            default -> throw new IllegalArgumentException(
                    "Unexpected endpoint: " + endpoint
            );
        }

        mvc.perform(paginatedRequest(endpoint)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "amount,asc", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        switch (endpoint) {
            case "/api/expenses" ->
                    verify(expenseService).getExpensesForUser(eq(alice), captor.capture());

            case "/api/expenses/search" ->
                    verify(expenseService).searchExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            captor.capture()
                    );

            case "/api/expenses/filter" ->
                    verify(expenseService).filterExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            captor.capture()
                    );

            case "/api/recurring-expenses" ->
                    verify(recurringExpenseService).getAllUserRecurringExpenses(eq(alice), captor.capture());

            case "/api/recurring-expenses/search" ->
                    verify(recurringExpenseService).searchRecurringExpenses(
                            eq(alice),
                            isNull(),
                            isNull(),
                            isNull(),
                            captor.capture()
                    );

            case "/api/recurring-expenses/filter" ->
                    verify(recurringExpenseService).filterRecurringExpensesByAmount(
                            eq(alice),
                            any(java.math.BigDecimal.class),
                            any(java.math.BigDecimal.class),
                            captor.capture()
                    );

            default -> throw new IllegalArgumentException("Unexpected endpoint: " + endpoint);
        }

        Pageable pageable = captor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(
                List.of(
                        Sort.Order.asc("amount"),
                        Sort.Order.desc("id")
                ),
                pageable.getSort().toList()
        );
    }

    static Stream<String> paginatedEndpoints() {
        return Stream.of(
                "/api/expenses",
                "/api/expenses/search",
                "/api/expenses/filter",
                "/api/recurring-expenses",
                "/api/recurring-expenses/search",
                "/api/recurring-expenses/filter"
        );
    }

    private static MockHttpServletRequestBuilder paginatedRequest(
            String endpoint
    ) {
        MockHttpServletRequestBuilder request = get(endpoint);

        if (endpoint.equals("/api/recurring-expenses/filter")) {
            request.param("minAmount", "10");
            request.param("maxAmount", "100");
        }

        return request;
    }
}