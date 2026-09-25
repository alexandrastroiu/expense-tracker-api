package com.project.expensemanager.unit_tests;

import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.UserExistsException;
import com.project.expensemanager.repository.UserRepository;
import com.project.expensemanager.security.JWTService;
import com.project.expensemanager.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// Unit tests for Authentication service methods

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    // Simulate external dependencies using the Mockito framework
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTService jwtService;

    @Mock
    Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthService authService;

    User user;

    // Setup
    @BeforeEach
    void setUp() {
        user = new User("test_user", "Test", "Test", "test@gmail.com");
    }

    // Test register method
    @Test
    public void register_NewUser_RegistersUserSuccessfully() {
        String rawPassword = "password123";

        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encoded-password");

        authService.register(user, rawPassword);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    public void register_WithDuplicateUsername_ThrowsExceptionAndDoesNotSave() {
        String rawPassword = "password123";

        when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);
        Exception exception = assertThrows(UserExistsException.class,
                () -> authService.register(user, rawPassword));
        Assertions.assertEquals("Username is already registered.", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    public void register_WithDuplicateEmail_ThrowsExceptionAndDoesNotSave() {
        String rawPassword = "password123";

        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        Exception exception = assertThrows(UserExistsException.class,
                () -> authService.register(user, rawPassword));
        Assertions.assertEquals("Email is already registered.", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    // Test login method
    @Test
    public void login_WithValidCredentials_ReturnsToken() {
        String rawPassword = "password123";
        String expectedToken = "jwt-token";

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(expectedToken);

        String result = authService.login(user.getUsername(), rawPassword);
        assertEquals(expectedToken, result);
        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(authentication).getPrincipal();
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    public void login_WithInvalidCredentials_DoesNotGenerateToken() {
        String rawPassword = "wrong-password";
        String expectedToken = "jwt-token";

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Bad Credentials."));
        assertThrows(BadCredentialsException.class,
                () -> authService.login(user.getUsername(), rawPassword)
        );
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }
}