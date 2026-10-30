package com.project.expensemanager.controller;

import com.project.expensemanager.dto.user.UserProfileResponse;
import com.project.expensemanager.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get user profile information (username and currency)
    @GetMapping("/profile")
    public UserProfileResponse getCurrentUserProfile(Authentication authentication) {
        return userService.getUserProfile(authentication.getName());
    }
}