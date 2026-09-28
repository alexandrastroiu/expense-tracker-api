package com.project.expensemanager.controller;

import com.project.expensemanager.dto.error.ErrorResponse;
import com.project.expensemanager.dto.recurringexpense.RecurringExpenseRequest;
import com.project.expensemanager.dto.recurringexpense.RecurringExpenseResponse;
import com.project.expensemanager.entity.Category;
import com.project.expensemanager.entity.Frequency;
import com.project.expensemanager.entity.RecurringExpense;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.mapper.RecurringExpenseMapper;
import com.project.expensemanager.service.CategoryService;
import com.project.expensemanager.service.RecurringExpenseService;
import com.project.expensemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recurring Expenses", description = "Manage user recurring expenses")
@ApiResponse(
        responseCode = "401",
        description = "Missing, invalid, or expired bearer token",
        content = @Content
)
@RestController
@RequestMapping("/api/recurring-expenses")  // Base URL
public class RecurringExpenseController {

    private final UserService userService;
    private final RecurringExpenseService recurringExpenseService;
    private final CategoryService categoryService;
    private final RecurringExpenseMapper recurringExpenseMapper;

    public RecurringExpenseController(UserService userService, RecurringExpenseService recurringExpenseService, CategoryService categoryService, RecurringExpenseMapper recurringExpenseMapper) {
        this.userService = userService;
        this.recurringExpenseService = recurringExpenseService;
        this.categoryService = categoryService;
        this.recurringExpenseMapper = recurringExpenseMapper;
    }

    // Create
    @Operation(
            summary = "Create a recurring expense",
            description = "Creates a new recurring expense for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurring expense created successfully", content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid recurring expense data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            Authentication authentication,
            @Valid @RequestBody RecurringExpenseRequest request
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Category category = categoryService.getCategoryById(request.categoryId());
        RecurringExpense recurringExpense = recurringExpenseMapper.mapToEntity(request, user, category);
        RecurringExpense savedRecurringExpense = recurringExpenseService.createRecurringExpense(recurringExpense);
        RecurringExpenseResponse response = recurringExpenseMapper.mapToResponse(savedRecurringExpense);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Update
    @Operation(
            summary = "Update a recurring expense",
            description = "Updates the specified recurring expense belonging to the authenticated user with the provided data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurring expense updated successfully", content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid recurring expense data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recurring expense or category not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{recurringExpenseId}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
            Authentication authentication,
            @PathVariable Integer recurringExpenseId,
            @Valid @RequestBody RecurringExpenseRequest request
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Category category = categoryService.getCategoryById(request.categoryId());
        RecurringExpense recurringExpense = recurringExpenseMapper.mapToEntity(request, user, category);
        RecurringExpense updatedExpense = recurringExpenseService.updateRecurringExpense(recurringExpenseId, user, recurringExpense);
        RecurringExpenseResponse response = recurringExpenseMapper.mapToResponse(updatedExpense);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get all user recurring expenses
    // Pagination
    @Operation(
            summary = "Get all recurring expenses",
            description = "Returns all recurring expenses belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurring expenses retrieved successfully", useReturnTypeSchema = true)
    })
    @GetMapping
    public ResponseEntity<Page<RecurringExpenseResponse>> getAllRecurringExpenses(
            Authentication authentication,
            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = {"startDate", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Page<RecurringExpense> expenses = recurringExpenseService.getAllUserRecurringExpenses(user, pageable);
        Page<RecurringExpenseResponse> response = expenses.map(recurringExpenseMapper::mapToResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get recurring expense by ID
    @Operation(
            summary = "Get recurring expense by ID",
            description = "Returns the specified recurring expense belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurring expense retrieved successfully", content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recurring expense not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{recurringExpenseId}")
    public ResponseEntity<RecurringExpenseResponse> getExpenseById(
            Authentication authentication,
            @PathVariable Integer recurringExpenseId
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        RecurringExpense expense = recurringExpenseService.getUserRecurringExpenseById(user, recurringExpenseId);
        RecurringExpenseResponse response = recurringExpenseMapper.mapToResponse(expense);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Search recurring expenses
    // Pagination
    @Operation(
            summary = "Search recurring expenses",
            description = "Returns the authenticated user's recurring expenses that match the specified search criteria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurring expenses retrieved successfully",  useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/search")
    public ResponseEntity<Page<RecurringExpenseResponse>> searchRecurringExpenses (
            Authentication authentication,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Frequency frequency,
            @RequestParam(required = false) Integer categoryId,
            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = {"startDate", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Page<RecurringExpense> expenses = recurringExpenseService.searchRecurringExpenses(
                user,
                title,
                categoryId,
                frequency,
                pageable
        );
        Page<RecurringExpenseResponse> response = expenses.map(recurringExpenseMapper::mapToResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Filter recurring expenses by amount
    // Pagination
    @Operation(
            summary = "Filter recurring expenses",
            description = "Returns the authenticated user's recurring expenses that match the specified filter criteria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurring expense retrieved successfully", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid filtering criteria", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/filter")
    public ResponseEntity<Page<RecurringExpenseResponse>> filterRecurringExpenses (
            Authentication authentication,
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount,
            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = {"startDate", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        Page<RecurringExpense> expenses = recurringExpenseService.filterRecurringExpensesByAmount(user, minAmount, maxAmount, pageable);
        Page<RecurringExpenseResponse> response = expenses.map(recurringExpenseMapper::mapToResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Delete
    @Operation(
            summary = "Delete a recurring expense",
            description = "Deletes the specified recurring expense belonging to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recurring expense deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Recurring expense not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @DeleteMapping("/{recurringExpenseId}")
    public ResponseEntity<Void> deleteRecurringExpense(
            Authentication authentication,
            @PathVariable Integer recurringExpenseId
    ) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        recurringExpenseService.deleteRecurringExpense(user, recurringExpenseId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}