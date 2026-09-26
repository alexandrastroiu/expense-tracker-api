package com.project.expensemanager.service;

import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.InvalidRequestException;
import com.project.expensemanager.exception.UserExistsException;
import com.project.expensemanager.repository.UserRepository;
import com.project.expensemanager.security.JWTService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JWTService jwtService) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.authenticationManager = authenticationManager;
            this.jwtService = jwtService;
    }

    public void register(User user, String rawPassword) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserExistsException("Username is already registered.");
        }

        if(userRepository.existsByEmail(user.getEmail())) {
            throw new UserExistsException("Email is already registered.");
        }

        // BCrypt passwords have a maximum length of 72 bytes
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must be at most 72 bytes in UTF-8.");
        }

        // Store hashed password
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        try {
            userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException exception){     // Edge case, check if a database rule was broken
            if (isUsernameOrEmailDuplicate(exception)) {
                throw new UserExistsException("Username or email is already registered..");       // The database username/email uniqueness constraint was broken
            }
            throw exception;        // Another database constraint was broken
        }
    }

    public String login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtService.generateToken(userDetails);
    }

    // Helper method
    // Checks if an exception is caused by the database username/email uniqueness constraint
    private boolean isUsernameOrEmailDuplicate(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintException && ("users_username_key".equals(constraintException.getConstraintName()) || "users_email_key".equals(constraintException.getConstraintName()))) {
                return true;
            }
        }
        return false;
    }
}