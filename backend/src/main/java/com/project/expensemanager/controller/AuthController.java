package com.project.expensemanager.controller;

import com.project.expensemanager.dto.auth.LoginRequest;
import com.project.expensemanager.dto.auth.LoginResponse;
import com.project.expensemanager.dto.auth.RegisterRequest;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.mapper.UserMapper;
import com.project.expensemanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final AuthService authService;

    private AuthController(AuthService service, UserMapper userMapper) {
        this.userMapper = userMapper;
        this.authService = service;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account using the provided username, email, and password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "409", description = "Username or Email already used")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = userMapper.mapToEntity(request);
        authService.register(user, request.password());

        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @Operation(
            summary = "Log in a user",
            description = "Authenticates a user using their username and password and returns a JWT token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request.username(), request.password());
        LoginResponse response = new LoginResponse(token);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}