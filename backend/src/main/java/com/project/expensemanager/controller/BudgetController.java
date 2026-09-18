package com.project.expensemanager.controller;

import com.project.expensemanager.dto.budget.BudgetRequest;
import com.project.expensemanager.dto.budget.BudgetResponse;
import com.project.expensemanager.dto.budget.BudgetSummaryResponse;
import com.project.expensemanager.entity.Budget;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.mapper.BudgetMapper;
import com.project.expensemanager.model.BudgetSummary;
import com.project.expensemanager.service.BudgetService;
import com.project.expensemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Budgets", description = "Manage user budgets")
@RestController
@RequestMapping("/api/budgets")    // Base URL
public class BudgetController {
    private final BudgetService budgetService;
    private final UserService userService;
    private final BudgetMapper budgetMapper;

    public BudgetController(BudgetService budgetService, UserService userService, BudgetMapper budgetMapper) {
        this.budgetService = budgetService;
        this.userService = userService;
        this.budgetMapper = budgetMapper;
    }

    // Create
    @Operation(
            summary = "Create a budget",
            description = "Creates a new budget for the authenticated user for the specified budget period."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Budget created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid budget data"),
            @ApiResponse(responseCode = "409", description = "A budget already exists for the specified period")
    })
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request
            ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Budget budget = budgetMapper.mapToEntity(request, user);
        Budget savedBudget = budgetService.createBudget(user, budget);
        BudgetResponse response = budgetMapper.mapToResponse(savedBudget);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get budget by ID
    @Operation(
            summary = "Get budget by ID",
            description = "Returns the specified budget belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            Authentication authentication,
            @PathVariable Integer budgetId
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Budget budget = budgetService.getUserBudgetById(user, budgetId);
        BudgetResponse response = budgetMapper.mapToResponse(budget);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get budget by period
    @Operation(
            summary = "Get budget by period",
            description = "Returns the authenticated user's budget for the specified budget period."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @GetMapping
    public ResponseEntity<BudgetResponse> getBudgetByPeriod(
            Authentication authentication,
            @RequestParam @DateTimeFormat LocalDate period
            ) {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            Budget budget = budgetService.getUserBudgetByPeriod(user, period);
            BudgetResponse response = budgetMapper.mapToResponse(budget);

            return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Budget summary
    @Operation(
            summary = "Get budget summary",
            description = "Returns a summary of the authenticated user's budget and expenses for the specified budget period."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found for the specified period")
    })
   @GetMapping("/summary")
   public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(
           Authentication authentication,
           @RequestParam @DateTimeFormat LocalDate period
   ) {
       String username = authentication.getName();
       User user = userService.getUserByUsername(username);
       BudgetSummary summary = budgetService.getBudgetSummary(user, period);
       BudgetSummaryResponse  response = budgetMapper.mapSummaryToResponse(summary);

       return ResponseEntity.status(HttpStatus.OK).body(response);
   }

    // Update
    @Operation(
            summary = "Update a budget",
            description = "Updates the specified budget belonging to the authenticated user with the provided data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid budget data"),
            @ApiResponse(responseCode = "404", description = "Budget not found"),
            @ApiResponse(responseCode = "409", description = "A different budget already exists for this month")
    })
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            Authentication authentication,
            @PathVariable Integer budgetId,
            @Valid @RequestBody BudgetRequest request
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Budget budget = budgetMapper.mapToEntity(request, user);
        Budget updatedBudget = budgetService.updateBudget(user, budgetId, budget);
        BudgetResponse response = budgetMapper.mapToResponse(updatedBudget);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Delete
    @Operation(
            summary = "Delete a budget",
            description = "Deletes the specified budget belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deletes the specified budget belonging to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            Authentication authentication,
            @PathVariable Integer budgetId
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        budgetService.deleteBudget(user, budgetId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}