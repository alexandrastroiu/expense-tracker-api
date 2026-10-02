package com.project.expensemanager.service;

import com.project.expensemanager.entity.Category;
import com.project.expensemanager.entity.Frequency;
import com.project.expensemanager.entity.RecurringExpense;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.InvalidRequestException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.CategoryRepository;
import com.project.expensemanager.repository.RecurringExpenseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class RecurringExpenseService {
    // Inject repositories
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CategoryRepository categoryRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringExpenseRepository, CategoryRepository categoryRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.categoryRepository = categoryRepository;
    }

    // Business logic
    // Create
    @Transactional
    public RecurringExpense createRecurringExpense(RecurringExpense recurringExpense) {
        Category category = categoryRepository.findById(recurringExpense.getCategory().getId()).orElseThrow( () -> new ResourceNotFoundException("Category not found"));

        validateDate(recurringExpense.getEndDate(), recurringExpense.getStartDate());

        return recurringExpenseRepository.save(recurringExpense);
    }

    // Update
    @Transactional
    public RecurringExpense updateRecurringExpense(Integer recurringExpenseId,User user, RecurringExpense updatedRecurringExpense) {
        RecurringExpense recurringExpense = getUserRecurringExpenseById(user, recurringExpenseId);

        Category category = categoryRepository.findById(updatedRecurringExpense.getCategory().getId()).orElseThrow( () -> new ResourceNotFoundException("Category not found."));

        validateDate(updatedRecurringExpense.getEndDate(), updatedRecurringExpense.getStartDate());

        recurringExpense.setTitle(updatedRecurringExpense.getTitle());
        recurringExpense.setDescription(updatedRecurringExpense.getDescription());
        recurringExpense.setAmount(updatedRecurringExpense.getAmount());
        recurringExpense.setCategory(category);
        recurringExpense.setStartDate(updatedRecurringExpense.getStartDate());
        recurringExpense.setEndDate(updatedRecurringExpense.getEndDate());
        recurringExpense.setFrequency(updatedRecurringExpense.getFrequency());

        return recurringExpenseRepository.save(recurringExpense);
    }

    // Read
    public RecurringExpense getUserRecurringExpenseById(User user, Integer recurringExpenseId) {
        return recurringExpenseRepository.findByUserAndId(user, recurringExpenseId).orElseThrow(() -> new ResourceNotFoundException("Recurring Expense not found."));
    }

    public List<RecurringExpense> getUserRecurringExpenseByTitle(User user, String title) {
        return recurringExpenseRepository.findByUserAndTitle(user, title);
    }

    public List<RecurringExpense> getUserRecurringExpenseByCategory(User user, Integer categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow( () -> new ResourceNotFoundException("Category not found"));

        return recurringExpenseRepository.findByUserAndCategory(user, category);
    }

    public List<RecurringExpense> getUserRecurringExpenseByFrequency(User user, Frequency frequency) {
        return recurringExpenseRepository.findByUserAndFrequency(user, frequency);
    }

    // Pagination
    public Page<RecurringExpense> getAllUserRecurringExpenses(User user, Pageable pageable) {
        return recurringExpenseRepository.findByUser(user, pageable);
    }

    // Search
    // Pagination
    public Page<RecurringExpense> searchRecurringExpenses(
            User user,
            String title,
            Integer categoryId,
            Frequency frequency,
            Pageable pageable
    ) {
        Specification<RecurringExpense> criteria = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            // Always restrict the search to the authenticated user.
            conditions.add(cb.equal(root.get("user"), user));

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

            if (frequency != null) {
                conditions.add(cb.equal(root.get("frequency"), frequency));
            }

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return recurringExpenseRepository.findAll(criteria, pageable);
    }

    // Filter recurring expenses by amount
    // Pagination
    public Page<RecurringExpense> filterRecurringExpensesByAmount(
            User user,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable
    ) {
        if (isInvalidAmount(minAmount, maxAmount)) {
            throw new InvalidRequestException("Minimum amount cannot be greater than maximum amount.");
        }

        Specification<RecurringExpense> criteria = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            conditions.add(cb.equal(root.get("user"), user));

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

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return recurringExpenseRepository.findAll(criteria, pageable);
    }

    // Delete
    @Transactional
    public void deleteRecurringExpense(User user, Integer recurringExpenseId) {
    RecurringExpense recurringExpense = getUserRecurringExpenseById(user, recurringExpenseId);
    recurringExpenseRepository.delete(recurringExpense);
    }

    // Helper methods
    private void validateDate(LocalDate endDate, LocalDate startDate) {
        if (startDate!= null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidRequestException("End date cannot be before start date.");
        }
    }

    private boolean isInvalidAmount(BigDecimal minAmount, BigDecimal maxAmount) {
        return (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0);
    }
}