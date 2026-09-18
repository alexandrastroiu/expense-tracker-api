package com.project.expensemanager.controller;

import com.project.expensemanager.dto.category.CategoryResponse;
import com.project.expensemanager.entity.Category;
import com.project.expensemanager.mapper.CategoryMapper;
import com.project.expensemanager.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories", description = "Expense Categories")
@RestController
@RequestMapping("/api/categories")   // Base URL
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    // Get all categories
    @Operation(
            summary = "Get all expense categories",
            description = "Get all the expense categories."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryResponse> response = categories.stream().map(categoryMapper::mapToResponse).toList();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}