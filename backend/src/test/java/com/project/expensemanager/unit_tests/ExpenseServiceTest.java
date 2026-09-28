package com.project.expensemanager.unit_tests;

import com.project.expensemanager.entity.Category;
import com.project.expensemanager.entity.Expense;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.InvalidRequestException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.CategoryRepository;
import com.project.expensemanager.repository.ExpenseRepository;
import com.project.expensemanager.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests for Expense service methods

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    // Simulate external dependencies using the Mockito framework
    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User user;

    // Setup
    @BeforeEach
    void setUp() {
        user = new User("test_user");
    }

    // Test create method
    @Test
    public void createExpense_ValidExpense_Saved() {
        Category category = new Category("Groceries");
        category.setId(1);
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category, LocalDate.now());

        when(categoryRepository.findById(expense.getCategory().getId())).thenReturn(Optional.of(category));
        when(expenseRepository.save(expense)).thenReturn(expense);
        Expense result = expenseService.createExpense(expense);

        assertSame(expense, result);
        verify(expenseRepository).save(expense);
    }

    @Test
    public void createExpense_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Category category = new Category("Groceries");
        category.setId(1);
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category, LocalDate.now());

        when(categoryRepository.findById(expense.getCategory().getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> expenseService.createExpense(expense)
        );
        assertEquals("Category not found", exception.getMessage());
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    // Test read methods
    @Test
    public void getExpensesForUser_ReturnsCorrectData() {
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries",
                new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now()),
                new Expense(user, "Movie tickets", "tickets",
                new BigDecimal("50.00"), new Category("Entertainment"), LocalDate.now())
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> expected = new PageImpl<>(expenses, pageable, expenses.size());

        when(expenseRepository.findByUser(user, pageable)).thenReturn(expected);
        Page<Expense> result = expenseService.getExpensesForUser(user, pageable);
        assertSame(expected, result);
        verify(expenseRepository).findByUser(user, pageable);
    }

    @Test
    public void getExpenseById_ReturnsCorrectData() {
        Integer id = 1;
        Expense expected = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now());
        expected.setId(1);

        when(expenseRepository.findByUserAndId(user, id)).thenReturn(Optional.of(expected));
        Expense result = expenseService.getUserExpenseById(user, id);
        assertSame(expected, result);
        verify(expenseRepository).findByUserAndId(user, id);
    }

    @Test
    public void getExpenseById_ThrowsResourceNotFoundException() {
        Integer id = 1;
        Expense expected = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now());
        expected.setId(2);

        when(expenseRepository.findByUserAndId(user, id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> expenseService.getUserExpenseById(user, id)
        );
        assertEquals("Expense not found.", exception.getMessage());
    }

    @Test
    public void getUserExpensesByDate_ReturnsCorrectData() {
        LocalDate date = LocalDate.now();
        List<Expense> expected = List.of(
                new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), date),
                new Expense(user, "Movie tickets", "tickets", new BigDecimal("50.00"), new Category("Entertainment"), date)
        );

        when(expenseRepository.findByUserAndExpenseDate(user, date)).thenReturn(expected);
        List<Expense> result = expenseService.getUserExpensesByDate(user, date);
        assertSame(expected, result);
        verify(expenseRepository).findByUserAndExpenseDate(user, date);
    }

    @Test
    public void getUserExpensesByTitle_ReturnsCorrectData() {
        String title = "Groceries";
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now()),
                new Expense(user, "Movie tickets", "tickets", new BigDecimal("50.00"), new Category("Entertainment"), LocalDate.now())
        );
        List<Expense> expected = List.of(expenses.getFirst());

        when(expenseRepository.findByUserAndTitle(user, title)).thenReturn(expected);
        List<Expense> result = expenseService.getUserExpensesByTitle(user, title);
        assertSame(expected, result);
        verify(expenseRepository).findByUserAndTitle(user, title);
    }

    @Test
    public void getUserExpensesByCategory_ReturnsCorrectData() {
        Category category = new Category("Groceries");
        category.setId(1);
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category, LocalDate.now()),
                new Expense(user, "Movie tickets", "tickets", new BigDecimal("50.00"), new Category("Entertainment"), LocalDate.now())
        );
        List<Expense> expected = List.of(expenses.getFirst());

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(expenseRepository.findByUserAndCategory(user, category)).thenReturn(expected);
        List<Expense> result = expenseService.getUserExpensesByCategory(user, category.getId());
        assertSame(expected, result);
        verify(expenseRepository).findByUserAndCategory(user, category);
    }

    @Test
    public void getUserExpensesByCategory_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Category category = new Category("Groceries");
        category.setId(1);
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category, LocalDate.now());

        when(categoryRepository.findById(expense.getCategory().getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> expenseService.getUserExpensesByCategory(user, category.getId())
        );
        assertEquals("Category not found.", exception.getMessage());
    }

    @Test
    public void getUserExpensesByAmount_ReturnsCorrectData() {
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now()),
                new Expense(user, "Movie tickets", "tickets", new BigDecimal("50.00"), new Category("Entertainment"), LocalDate.now())
        );
        List<Expense> expected = List.of(expenses.getFirst());

        when(expenseRepository.findByUserAndAmount(user, amount)).thenReturn(expected);
        List<Expense> result = expenseService.getUserExpensesByAmount(user, amount);
        assertSame(expected, result);
        verify(expenseRepository).findByUserAndAmount(user, amount);
    }

    @Test
    public void searchExpenses_WithCriteria_ReturnsCorrectData() {
        LocalDate date = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 20);
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), date),
                new Expense(user, "Movie tickets", "tickets", new BigDecimal("50.00"), new Category("Entertainment"), date.plusWeeks(1))
        );
        Page<Expense> expected = new PageImpl<>(expenses, pageable, 1);

        when(expenseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);
        Page<Expense> result = expenseService.searchExpenses(user, date, "Groceries", null, null, pageable);
        assertEquals(expected, result);
        verify(expenseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void filterExpenses_WithNoFilter_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Expense> expenses = List.of(
                new Expense(user, "Groceries", "Weekly groceries",
                new BigDecimal("100.00"), new Category("Groceries"),
                LocalDate.of(2026, 9, 28)),
                new Expense(user, "Movie tickets", "Tickets",
                new BigDecimal("50.00"), new Category("Entertainment"),
                LocalDate.of(2026, 9, 29))
        );
        Page<Expense> expected = new PageImpl<>(expenses, pageable, expenses.size());

        when(expenseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expected);
        Page<Expense> result = expenseService.filterExpenses(
                user, null, null, null, null, null, pageable
        );
        assertSame(expected, result);
        verify(expenseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void filterExpenses_WithCategoryAndStartDate_ReturnsPage() {
        Integer categoryId = 1;
        LocalDate start = LocalDate.of(2026, 9, 28);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> expected = new PageImpl<>(List.of(), pageable, 0);

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(expenseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expected);
        Page<Expense> result = expenseService.filterExpenses(
                user, categoryId, null, null, start, null, pageable
        );
        assertSame(expected, result);
        verify(categoryRepository).existsById(categoryId);
        verify(expenseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void filterExpenses_WithAllFilters_ReturnsPage() {
        Integer categoryId = 1;
        LocalDate start = LocalDate.of(2026, 9, 28);
        LocalDate end = start.plusWeeks(2);
        BigDecimal min = new BigDecimal("70.00");
        BigDecimal max = new BigDecimal("120.00");
        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> expected = new PageImpl<>(List.of(), pageable, 0);

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(expenseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);
        Page<Expense> result = expenseService.filterExpenses(user, categoryId, min, max, start, end, pageable);
        assertSame(expected, result);
        verify(expenseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void filterExpenses_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Integer categoryId = 1;
        Pageable pageable = PageRequest.of(0, 20);

        when(categoryRepository.existsById(categoryId)).thenReturn(false);
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> expenseService.filterExpenses(user, categoryId, null, null, null, null, pageable)
        );
        assertEquals("Category not found.", exception.getMessage());
        verify(expenseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void filterExpenses_WithReversedAmountRange_ThrowsInvalidRequestException() {
        Pageable pageable = PageRequest.of(0, 20);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> expenseService.filterExpenses(
                        user, null,
                        new BigDecimal("170.00"),
                        new BigDecimal("20.00"),
                        null, null, pageable
                )
        );
        assertEquals("Minimum amount cannot be greater than maximum amount.", exception.getMessage());
        verifyNoInteractions(expenseRepository);
    }

    @Test
    public void filterExpenses_WithReversedDateRange_ThrowsInvalidRequestException() {
        LocalDate start = LocalDate.of(2026, 9, 29);
        LocalDate end = LocalDate.of(2026, 9, 28);
        Pageable pageable = PageRequest.of(0, 20);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> expenseService.filterExpenses(
                        user, null, null, null, start, end, pageable
                )
        );
        assertEquals("End date cannot be before start date.", exception.getMessage());
        verifyNoInteractions(expenseRepository);
    }

    @Test
    public void filterExpenses_WithOnlyMinimumAmount_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> expected = new PageImpl<>(List.of(), pageable, 0);

        when(expenseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);
        Page<Expense> result = expenseService.filterExpenses(
                user, null, new BigDecimal("20.00"), null, null, null, pageable
        );
        assertSame(expected, result);
        verify(expenseRepository).findAll(any(Specification.class), eq(pageable));
    }

    // Test update method
    @Test
    public void updateExpense_UpdatesExpense() {
        Integer categoryId = 1, expenseId = 1;
        Category category = new Category("Others");
        category.setId(categoryId);
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category, LocalDate.now());
        expense.setId(expenseId);
        Expense updatedExpense = new Expense(user, "New title", "New description", new BigDecimal("150.00"), category, LocalDate.now());

        when(expenseRepository.findByUserAndId(user, expenseId)).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(updatedExpense.getCategory().getId())).thenReturn(Optional.of(category));
        when(expenseRepository.save(expense)).thenReturn(expense);
        Expense result = expenseService.updateExpense(user, expenseId, updatedExpense);
        assertEquals(expenseId, result.getId());
        assertEquals("New title", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertSame(category, result.getCategory());
        verify(expenseRepository).findByUserAndId(user, expenseId);
        verify(categoryRepository).findById(category.getId());
        verify(expenseRepository).save(expense);
    }

    @Test
    public void updateExpense_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Integer categoryId = 1, expenseId = 1;
        Category category1 = new Category("Groceries"), category2 = new Category("Otheras");
        category1.setId(1);
        category2.setId(categoryId);
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), category1, LocalDate.now());
        expense.setId(expenseId);
        Expense updatedExpense = new Expense(user, "New title", "New description", new BigDecimal("150.00"), category2, LocalDate.now());

        when(expenseRepository.findByUserAndId(user, expenseId)).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(updatedExpense.getCategory().getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(
               ResourceNotFoundException.class,
               () -> expenseService.updateExpense(user, expenseId, updatedExpense));
        assertEquals("Category not found.", exception.getMessage());
        verify(expenseRepository).findByUserAndId(user, expenseId);
        verify(categoryRepository).findById(category2.getId());
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    // Test delete method
    @Test
    public void deleteExpense_DeletesExpense() {
        Integer id = 1;
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now());
        expense.setId(1);

        when(expenseRepository.findByUserAndId(user, id)).thenReturn(Optional.of(expense));
        expenseService.deleteExpense(user, expense.getId());
        verify(expenseRepository).findByUserAndId(user, id);
        verify(expenseRepository).delete(expense);
    }

    @Test
    public void deleteExpense_ThrowsResourceNotFoundException() {
        Integer id = 1;
        Expense expense = new Expense(user, "Groceries", "Weekly groceries", new BigDecimal("100.00"), new Category("Groceries"), LocalDate.now());
        expense.setId(1);

        when(expenseRepository.findByUserAndId(user, id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                ResourceNotFoundException.class,
                () -> expenseService.deleteExpense(user, expense.getId()));
        assertEquals("Expense not found.", exception.getMessage());
        verify(expenseRepository).findByUserAndId(user, id);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }
}