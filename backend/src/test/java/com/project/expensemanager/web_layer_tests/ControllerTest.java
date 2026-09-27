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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Autowired MockMvc mvc;

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
                get("/api/recurring-expenses/filter")
                        .param("maxAmount", "5"),
                get("/api/expenses")
                        .param("start", "not-a-date"),
                get("/api/expenses")
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
                .andExpect(jsonPath("$.message",
                        containsString("title:")))
                .andExpect(jsonPath("$.message",
                        containsString("amount:")));

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
}