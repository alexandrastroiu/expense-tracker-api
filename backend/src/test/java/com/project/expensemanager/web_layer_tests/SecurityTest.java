package com.project.expensemanager.web_layer_tests;

import com.project.expensemanager.controller.AuthController;
import com.project.expensemanager.controller.ExpenseController;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.GlobalExceptionHandler;
import com.project.expensemanager.mapper.ExpenseMapper;
import com.project.expensemanager.mapper.UserMapper;
import com.project.expensemanager.repository.UserRepository;
import com.project.expensemanager.security.CustomUserDetailsService;
import com.project.expensemanager.security.JWTService;
import com.project.expensemanager.security.JwtAuthenticationFilter;
import com.project.expensemanager.security.SecurityConfig;
import com.project.expensemanager.service.AuthService;
import com.project.expensemanager.service.CategoryService;
import com.project.expensemanager.service.ExpenseService;
import com.project.expensemanager.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {AuthController.class, ExpenseController.class},
        properties = {
                "jwt.secret=01234567890123456789012345678901",
                "jwt.expiration=3600000"
        }
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class,
        CustomUserDetailsService.class,
        JWTService.class,
        AuthService.class,
        UserMapper.class
})
class SecurityTest {

    private static final String SECRET =
            "01234567890123456789012345678901";
    private static final String OTHER_SECRET =
            "abcdefghijklmnopqrstuvwxyz012345";

    private static final String LOGIN = """
            {
              "username": "alice",
              "password": "password123"
            }
            """;

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean UserRepository userRepository;
    @MockitoBean ExpenseService expenseService;
    @MockitoBean UserService userService;
    @MockitoBean CategoryService categoryService;
    @MockitoBean ExpenseMapper expenseMapper;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User(
                "alice", "Alice", "Example",
                passwordEncoder.encode("password123"),
                "alice@example.com"
        );

        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(alice));
        when(userService.getUserByUsername("alice")).thenReturn(alice);
    }

    @Test
    void loginWithoutTokenReturnsJwt() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token",
                        not(isEmptyOrNullString())));
    }

    @Test
    void unknownUsernameReturns401InsteadOf500() throws Exception {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "missing",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error")
                        .value("Invalid username or password"));

        verify(userRepository).findByUsername("missing");
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error")
                        .value("Invalid username or password"));
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void invalidTokenDoesNotBlockPublicLogin(String header)
            throws Exception {
        mvc.perform(post("/api/auth/login")
                        .header("Authorization", header)
                        .contentType(APPLICATION_JSON)
                        .content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token",
                        not(isEmptyOrNullString())));
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void invalidTokenCannotAccessProtectedEndpoint(String header)
            throws Exception {
        mvc.perform(get("/api/expenses")
                        .header("Authorization", header))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                userService, expenseService, categoryService, expenseMapper
        );
    }

    @Test
    void missingTokenCannotAccessProtectedEndpoint() throws Exception {
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, expenseService);
    }

    @Test
    void validTokenAuthenticatesCorrectUserWithoutCreatingSession()
            throws Exception {
        when(expenseService.filterExpenses(
                alice, null, null, null, null, null
        )).thenReturn(List.of());

        mvc.perform(get("/api/expenses")
                        .header("Authorization", "Bearer " + token(
                                "alice", SECRET,
                                Instant.now().plusSeconds(3600)
                        )))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNull(
                                result.getRequest().getSession(false)
                        ));

        verify(userService).getUserByUsername("alice");
        verify(expenseService).filterExpenses(
                alice, null, null, null, null, null
        );
    }

    @Test
    void registrationIsPublicAndDoesNotRequireCsrfToken()
            throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bob",
                                  "firstName": "Bob",
                                  "lastName": "Example",
                                  "password": "password123",
                                  "email": "bob@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getUsername().equals("bob")
                        && passwordEncoder.matches(
                        "password123", user.getPasswordHash()
                )
        ));
    }

    static Stream<String> invalidAuthorizationHeaders() {
        return Stream.of(
                "Bearer abc",
                "Bearer",
                "Bearer ",
                "Bearer " + token(
                        "alice", SECRET, Instant.now().minusSeconds(3600)
                ),
                "Bearer " + token(
                        "alice", OTHER_SECRET, Instant.now().plusSeconds(3600)
                ),
                "Bearer " + token(
                        "deleted-user", SECRET, Instant.now().plusSeconds(3600)
                ),
                "BearerX" + token(
                        "alice", SECRET, Instant.now().plusSeconds(3600)
                )
        );
    }

    private static String token(
            String username, String secret, Instant expiration
    ) {
        return Jwts.builder()
                .subject(username)
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                ))
                .compact();
    }
}