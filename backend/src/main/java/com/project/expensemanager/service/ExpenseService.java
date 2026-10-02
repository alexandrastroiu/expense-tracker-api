package com.project.expensemanager.service;

import com.project.expensemanager.entity.Category;
import com.project.expensemanager.entity.Expense;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.InvalidRequestException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.CategoryRepository;
import com.project.expensemanager.repository.ExpenseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ExpenseService {

    // Inject repositories
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    // Business logic
    // Create
    @Transactional
    public Expense createExpense(Expense expense) {
        Category category = categoryRepository.findById(expense.getCategory().getId()).orElseThrow( () -> new ResourceNotFoundException("Category not found"));

        return expenseRepository.save(expense);
    }

    // Read
    public Page<Expense> getExpensesForUser(User user, Pageable pageable) {
        return expenseRepository.findByUser(user, pageable);
    }

    public Expense getUserExpenseById(User user, Integer expenseId) {
        return expenseRepository.findByUserAndId(user, expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found."));
    }

    public List<Expense> getUserExpensesByDate(User user, LocalDate expenseDate) {
        return expenseRepository.findByUserAndExpenseDate(user, expenseDate);
    }

    public List<Expense> getUserExpensesByTitle(User user, String title) {
        return expenseRepository.findByUserAndTitle(user, title);
    }

    public List<Expense> getUserExpensesByCategory(User user, Integer categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        return expenseRepository.findByUserAndCategory(user, category);
    }

    public List<Expense> getUserExpensesByAmount(User user, BigDecimal amount) {
        return expenseRepository.findByUserAndAmount(user, amount);
    }

    // Search expense by a criteria
    // Pagination
    public Page<Expense> searchExpenses(
            User user,
            LocalDate expenseDate,
            String title,
            Integer categoryId,
            BigDecimal amount,
            Pageable pageable
    ) {
        Specification<Expense> criteria = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            conditions.add(cb.equal(root.get("user"), user));

            if (expenseDate != null) {
                conditions.add(cb.equal(root.get("expenseDate"), expenseDate));
            }

            if (title != null && !title.isBlank()) {
                String escapedTitle = title.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");

                conditions.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + escapedTitle + "%",
                        '\\'
                ));
            }

            if (categoryId != null) {
                conditions.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (amount != null) {
                conditions.add(cb.equal(root.get("amount"), amount));
            }

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return expenseRepository.findAll(criteria, pageable);
    }


    // Filter user expenses
    // Pagination
    public Page<Expense> filterExpenses(
            User user,
            Integer categoryId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate start,
            LocalDate end,
            Pageable pageable
    ) {
        if (isInvalidDate(start, end)) {
            throw new InvalidRequestException("End date cannot be before start date.");
        }

        if (isInvalidAmount(minAmount, maxAmount)) {
            throw new InvalidRequestException("Minimum amount cannot be greater than maximum amount.");
        }

        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found.");
        }

        Specification<Expense> criteria = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            conditions.add(cb.equal(root.get("user"), user));

            if (categoryId != null) {
                conditions.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minAmount != null) {
                conditions.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("amount"), minAmount
                ));
            }
            if (maxAmount != null) {
                conditions.add(cb.lessThanOrEqualTo(
                        root.<BigDecimal>get("amount"), maxAmount
                ));
            }
            if (start != null) {
                conditions.add(cb.greaterThanOrEqualTo(
                        root.<LocalDate>get("expenseDate"), start
                ));
            }
            if (end != null) {
                conditions.add(cb.lessThanOrEqualTo(
                        root.<LocalDate>get("expenseDate"), end
                ));
            }

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return expenseRepository.findAll(criteria, pageable);
    }


    // Update
    @Transactional
    public Expense updateExpense(User user, Integer expenseId, Expense updatedExpense) {
        Expense expense = getUserExpenseById(user, expenseId);

        Category category = categoryRepository.findById(updatedExpense.getCategory().getId()).orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        expense.setTitle(updatedExpense.getTitle());
        expense.setDescription(updatedExpense.getDescription());
        expense.setExpenseDate(updatedExpense.getExpenseDate());
        expense.setAmount(updatedExpense.getAmount());
        expense.setCategory(category);

        return expenseRepository.save(expense);
    }

    // Delete
    @Transactional
    public void deleteExpense(User user, Integer expenseId) {
        Expense expense = getUserExpenseById(user, expenseId);
        expenseRepository.delete(expense);
    }

    // Helper methods
    private boolean isInvalidDate(LocalDate startDate, LocalDate endDate) {
        return (startDate != null && endDate != null && endDate.isBefore(startDate));
    }

    private boolean isInvalidAmount(BigDecimal minAmount, BigDecimal maxAmount) {
        return (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0);
    }
}