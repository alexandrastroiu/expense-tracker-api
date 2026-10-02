package com.project.expensemanager.integration_tests;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests
// Tests controllers, services, repositories, and JWT authentication

@SpringBootTest(properties = {
        // Test-only JWT secret value
        "jwt.secret=integration-test-secret-at-least-32-bytes-long",
        "jwt.expiration=3600000"
})
@AutoConfigureMockMvc
@Testcontainers
class ExpenseManagerIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "database/database_schema.sql"
                            ),
                            "/docker-entrypoint-initdb.d/01-schema.sql"
                    );

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer categoryId;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("""
                TRUNCATE TABLE expenses, recurring_expenses,
                               budgets, users, categories
                RESTART IDENTITY CASCADE
                """);

        categoryId = jdbc.queryForObject(
                """
                INSERT INTO categories (category_name)
                VALUES ('Other')
                RETURNING id
                """,
                Integer.class
        );
    }

    @Test
    void registrationAndLoginAllowAuthenticatedRequests() throws Exception {
        String token = registerAndLogin("alice");

        mvc.perform(get("/api/expenses")
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody("alice", "WrongPassword123!")))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/expenses")
                        .header(AUTHORIZATION, bearer("invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expenseCreateUpdateAndDeleteArePersisted() throws Exception {
        String token = registerAndLogin("alice");

        int id = create(
                "/api/expenses",
                token,
                expenseBody("Groceries", "20.00", "2026-02-10")
        );

        mvc.perform(get("/api/expenses/{id}", id)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.amount").value(20.0))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.expenseDate").value("2026-02-10"));

        mvc.perform(put("/api/expenses/{id}", id)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseBody(
                                "Updated groceries", "35.00", "2026-02-11"
                        )))
                .andExpect(status().isOk());

        mvc.perform(get("/api/expenses/{id}", id)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated groceries"))
                .andExpect(jsonPath("$.amount").value(35.0))
                .andExpect(jsonPath("$.expenseDate").value("2026-02-11"));

        mvc.perform(delete("/api/expenses/{id}", id)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/expenses/{id}", id)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/expenses",
            "/api/recurring-expenses",
            "/api/budgets"
    })
    void anotherUserCannotReadUpdateOrDeleteRecords(
            String endpoint
    ) throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        String originalBody = resourceBody(endpoint, "20.00");
        String updatedBody = resourceBody(endpoint, "99.00");

        int id = create(endpoint, aliceToken, originalBody);

        mvc.perform(get(endpoint + "/{id}", id)
                        .header(AUTHORIZATION, bearer(bobToken)))
                .andExpect(status().isNotFound());

        mvc.perform(put(endpoint + "/{id}", id)
                        .header(AUTHORIZATION, bearer(bobToken))
                        .contentType(APPLICATION_JSON)
                        .content(updatedBody))
                .andExpect(status().isNotFound());

        mvc.perform(delete(endpoint + "/{id}", id)
                        .header(AUTHORIZATION, bearer(bobToken)))
                .andExpect(status().isNotFound());

        mvc.perform(get(endpoint + "/{id}", id)
                        .header(AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(20.0));
    }

    @Test
    void searchAndFilterReturnOnlyMatchingExpensesForCurrentUser()
            throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        int matchingId = create(
                "/api/expenses", aliceToken,
                expenseBody("Groceries", "20.00", "2026-02-10")
        );

        create(
                "/api/expenses", aliceToken,
                expenseBody("Transport", "80.00", "2026-02-12")
        );

        create(
                "/api/expenses", bobToken,
                expenseBody("Groceries", "20.00", "2026-02-10")
        );

        mvc.perform(get("/api/expenses/search")
                        .header(AUTHORIZATION, bearer(aliceToken))
                        .param("title", "grocer")
                        .param("categoryId", categoryId.toString())
                        .param("amount", "20.00")
                        .param("expenseDate", "2026-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(matchingId));

        mvc.perform(get("/api/expenses/filter")
                        .header(AUTHORIZATION, bearer(aliceToken))
                        .param("categoryId", categoryId.toString())
                        .param("minAmount", "20.00")
                        .param("maxAmount", "20.00")
                        .param("start", "2026-02-10")
                        .param("end", "2026-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(matchingId));

        mvc.perform(get("/api/expenses")
                        .header(AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void recurringListsAndQueriesExcludeAnotherUsersRecords()
            throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        int aliceId = create(
                "/api/recurring-expenses",
                aliceToken,
                recurringBody("10.00")
        );

        create(
                "/api/recurring-expenses",
                bobToken,
                recurringBody("10.00")
        );

        mvc.perform(get("/api/recurring-expenses")
                        .header(AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceId));

        mvc.perform(get("/api/recurring-expenses/search")
                        .header(AUTHORIZATION, bearer(aliceToken))
                        .param("title", "subscription")
                        .param("frequency", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceId));

        mvc.perform(get("/api/recurring-expenses/filter")
                        .header(AUTHORIZATION, bearer(aliceToken))
                        .param("minAmount", "10.00")
                        .param("maxAmount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceId));
    }

    @Test
    void budgetSummaryUsesOnlyCurrentUsersExpensesForRequestedMonth()
            throws Exception {
        String aliceToken = registerAndLogin("alice");
        String bobToken = registerAndLogin("bob");

        int budgetId = create(
                "/api/budgets", aliceToken,
                budgetBody("100.00", "2026-02-17")
        );

        create(
                "/api/expenses", aliceToken,
                expenseBody("Groceries", "20.00", "2026-02-20")
        );

        create(
                "/api/recurring-expenses", aliceToken,
                recurringBody("10.00")
        );

        create(
                "/api/expenses", aliceToken,
                expenseBody("March expense", "50.00", "2026-03-01")
        );

        create(
                "/api/expenses", bobToken,
                expenseBody("Bob expense", "70.00", "2026-02-10")
        );

        create(
                "/api/recurring-expenses", bobToken,
                recurringBody("40.00")
        );

        mvc.perform(get("/api/budgets/summary")
                        .header(AUTHORIZATION, bearer(aliceToken))
                        .param("period", "2026-02-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.budgetPeriod").value("2026-02-01"))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.totalCurrentExpenses").value(20.0))
                .andExpect(jsonPath("$.totalMonthlyExpenses").value(30.0))
                .andExpect(jsonPath("$.remainingCurrentBudget").value(80.0))
                .andExpect(jsonPath("$.remainingMonthlyBudget").value(70.0))
                .andExpect(jsonPath("$.budgetPercentage").value(30.0));

        mvc.perform(get("/api/budgets")
                        .header(AUTHORIZATION, bearer(bobToken))
                        .param("period", "2026-02-17"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/budgets/summary")
                        .header(AUTHORIZATION, bearer(bobToken))
                        .param("period", "2026-02-17"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateMonthlyBudgetReturnsConflictWithoutChangingOriginal()
            throws Exception {
        String token = registerAndLogin("alice");

        int id = create(
                "/api/budgets", token,
                budgetBody("100.00", "2026-02-01")
        );

        postJson(
                "/api/budgets", token,
                budgetBody("200.00", "2026-02-20")
        ).andExpect(status().isConflict());

        mvc.perform(get("/api/budgets/{id}", id)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.budgetPeriod").value("2026-02-01"));
    }

    private String registerAndLogin(String username) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "password": "%s",
                                  "email": "%s@example.com"
                                }
                                """.formatted(username, PASSWORD, username)))
                .andExpect(status().isCreated());

        String response = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(isEmptyOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private int create(String endpoint, String token, String body)
            throws Exception {
        String response = postJson(endpoint, token, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.id");
        return id.intValue();
    }

    private ResultActions postJson(
            String endpoint, String token, String body
    ) throws Exception {
        return mvc.perform(post(endpoint)
                .header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON)
                .content(body));
    }

    private String resourceBody(String endpoint, String amount) {
        return switch (endpoint) {
            case "/api/expenses" ->
                    expenseBody("Groceries", amount, "2026-02-10");
            case "/api/recurring-expenses" ->
                    recurringBody(amount);
            case "/api/budgets" ->
                    budgetBody(amount, "2026-02-01");
            default -> throw new IllegalArgumentException(
                    "Unexpected endpoint: " + endpoint
            );
        };
    }

    private String expenseBody(String title, String amount, String date) {
        return """
                {
                  "title": "%s",
                  "description": "Integration test expense",
                  "amount": %s,
                  "categoryId": %d,
                  "expenseDate": "%s"
                }
                """.formatted(title, amount, categoryId, date);
    }

    private String recurringBody(String amount) {
        return """
                {
                  "title": "Subscription",
                  "description": "Monthly subscription",
                  "amount": %s,
                  "categoryId": %d,
                  "startDate": "2026-01-15",
                  "frequency": "MONTHLY"
                }
                """.formatted(amount, categoryId);
    }

    private String budgetBody(String amount, String period) {
        return """
                {
                  "amount": %s,
                  "budgetPeriod": "%s"
                }
                """.formatted(amount, period);
    }

    private String loginBody(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}