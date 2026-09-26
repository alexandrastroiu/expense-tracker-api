package com.project.expensemanager.service;

import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Inject repositories
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Business logic
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow( () -> new ResourceNotFoundException("User with username " + username + " not found."));
    }

}
