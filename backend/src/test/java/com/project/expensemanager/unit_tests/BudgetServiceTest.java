package com.project.expensemanager.unit_tests;

import com.project.expensemanager.entity.*;
import com.project.expensemanager.exception.BudgetExistsException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.BudgetRepository;
import com.project.expensemanager.repository.ExpenseRepository;
import com.project.expensemanager.repository.RecurringExpenseRepository;
import com.project.expensemanager.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// Unit tests for Budget service methods

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    // Simulate external dependencies using the Mockito framework
    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RecurringExpenseRepository recurringExpenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private BudgetService budgetService;

    User user;

    // Setup
    @BeforeEach
    void setUp() {
        user = new User("test_user");
    }

    // Test create method
    @Test
    public void createBudget_ValidBudget_Saved() {
        Budget savedBudget = new Budget(user,
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 9, 1)
        );
        savedBudget.setId(1);

        when(budgetRepository.existsByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1))).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);
        Budget result = budgetService.createBudget(user, savedBudget);
        assertEquals(savedBudget, result);
        assertEquals(1, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getAmount());
        assertEquals(LocalDate.of(2026, 9, 1), result.getBudgetPeriod());
        verify(budgetRepository).existsByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1));
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    public void createBudget_DuplicateBudget_ThrowsBudgetExistsException() {
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));

        when(budgetRepository.existsByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1))).thenReturn(true);
        BudgetExistsException exception = assertThrows(BudgetExistsException.class, () -> budgetService.createBudget(user, budget));
        assertEquals("Budget already exists for this month.", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Test read methods
    @Test
    public void getUserBudgetById_ReturnsCorrectData() {
        Integer budgetId = 1;
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));
        budget.setId(budgetId);

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.of(budget));
        Budget result = budgetService.getUserBudgetById(user, budgetId);
        assertEquals(budgetId, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getAmount());
        assertEquals(LocalDate.of(2026, 9, 1), result.getBudgetPeriod());
        verify(budgetRepository).findByUserAndId(user, budgetId);
    }

    @Test
    public void getUserBudgetById_ThrowsResourceNotFoundException() {
        Integer budgetId = 1;

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> budgetService.getUserBudgetById(user, budgetId));
        assertEquals("Budget not found", exception.getMessage());
    }

    @Test
    public void getUserBudgetByPeriod_ReturnsCorrectData() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        Budget budget = new Budget(user, new BigDecimal("1000.00"), period);
        budget.setId(1);

        when(budgetRepository.findByUserAndBudgetPeriod(user, period)).thenReturn(Optional.of(budget));
        Budget result = budgetService.getUserBudgetByPeriod(user, period);
        assertEquals(1, result.getId());
        assertEquals(new BigDecimal("1000.00"), result.getAmount());
        assertEquals(period, result.getBudgetPeriod());
        verify(budgetRepository).findByUserAndBudgetPeriod(user, period);
    }

    @Test
    public void getUserBudgetByPeriod_ThrowsResourceNotFoundException() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        when(budgetRepository.findByUserAndBudgetPeriod(user, period)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.getUserBudgetByPeriod(user, period)
        );

        assertEquals("Budget not found", exception.getMessage());
    }

    @Test
    public void getTotalCurrentExpenses_CalculatesTotal() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        Expense firstExpense = mock(Expense.class);
        Expense secondExpense = mock(Expense.class);
        Expense thirdExpense = mock(Expense.class);

        when(firstExpense.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(secondExpense.getAmount()).thenReturn(new BigDecimal("50.50"));
        when(thirdExpense.getAmount()).thenReturn(new BigDecimal("25.25"));

        when(expenseRepository.findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(
                firstExpense,
                secondExpense,
                thirdExpense
        ));

        BigDecimal result = budgetService.getTotalCurrentExpenses(user, period);
        assertEquals(new BigDecimal("175.75"), result);
        verify(expenseRepository).findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    @Test
    public void getTotalCurrentExpenses_NoExpenses_ReturnsZero() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        when(expenseRepository.findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(List.of());
        BigDecimal result = budgetService.getTotalCurrentExpenses(user, period);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    public void getRemainingCurrentBudget_CalculatesTotal() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));
        Expense firstExpense = mock(Expense.class);
        Expense secondExpense = mock(Expense.class);

        when(firstExpense.getAmount()).thenReturn(new BigDecimal("150.00"));
        when(secondExpense.getAmount()).thenReturn(new BigDecimal("50.00"));
        when(budgetRepository.findByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1))).thenReturn(Optional.of(budget));
        when(expenseRepository.findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(List.of(firstExpense, secondExpense));
        BigDecimal result = budgetService.getRemainingCurrentBudget(user, period);
        assertEquals(new BigDecimal("800.00"), result);
    }

    @Test
    public void getRemainingMonthlyBudget_CalculatesTotal() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));

        Expense currentExpense = mock(Expense.class);
        when(currentExpense.getAmount()).thenReturn(new BigDecimal("250.00"));

        RecurringExpense recurringExpense = mock(RecurringExpense.class);
        when(recurringExpense.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(recurringExpense.getFrequency()).thenReturn(Frequency.MONTHLY);
        when(recurringExpense.getStartDate()).thenReturn(LocalDate.of(2026, 1, 15));
        when(recurringExpense.getEndDate()).thenReturn(null);

        when(budgetRepository.findByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1))).thenReturn(Optional.of(budget));
        when(expenseRepository.findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(List.of(currentExpense));
        when(recurringExpenseRepository.findByUser(user)).thenReturn(List.of(recurringExpense));
        BigDecimal result = budgetService.getRemainingMonthlyBudget(user, period);
        assertEquals(new BigDecimal("650.00"), result);
        verify(budgetRepository).findByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1));
        verify(expenseRepository).findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verify(recurringExpenseRepository).findByUser(user);
    }

    @Test
    public void getBudgetPercentage_CalculatesTotal() {
        LocalDate period = LocalDate.of(2026, 9, 1);
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));

        Expense expense = mock(Expense.class);
        when(expense.getAmount()).thenReturn(new BigDecimal("250.00"));
        when(budgetRepository.findByUserAndBudgetPeriod(user, LocalDate.of(2026, 9, 1))).thenReturn(Optional.of(budget));
        when(expenseRepository.findByUserAndExpenseDateBetween(user, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(List.of(expense));
        when(recurringExpenseRepository.findByUser(user)).thenReturn(List.of());
        BigDecimal result = budgetService.getBudgetPercentage(user, period);
        assertEquals(0, new BigDecimal("25").compareTo(result));
    }

    // Test update method
    @Test
    public void updateBudget_UpdatesBudget() {
        Integer budgetId = 1;
        LocalDate period = LocalDate.of(2026, 9, 1);
        Budget existingBudget = new Budget(user, new BigDecimal("1000.00"), period);
        existingBudget.setId(budgetId);
        Budget updatedBudget = new Budget(user, new BigDecimal("1200.00"), LocalDate.of(2026, 9, 20));

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.existsByUserAndBudgetPeriod(user, period)).thenReturn(true);
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);
        Budget result = budgetService.updateBudget(user, budgetId, updatedBudget);
        assertEquals(new BigDecimal("1200.00"), result.getAmount());
        assertEquals(period, result.getBudgetPeriod());
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    public void updateBudget_WithDuplicateBudget_ThrowsBudgetExistsException() {
        Integer budgetId = 1;
        Budget existingBudget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));
        existingBudget.setId(budgetId);
        Budget updatedBudget = new Budget(user, new BigDecimal("1500.00"), LocalDate.of(2026, 10, 15));

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.existsByUserAndBudgetPeriod(user, LocalDate.of(2026, 10, 1))).thenReturn(true);
        BudgetExistsException exception = assertThrows(BudgetExistsException.class, () -> budgetService.updateBudget(user, budgetId, updatedBudget));
        assertEquals("A different budget already exists for this month.", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    public void updateBudget_BudgetNotFound_ThrowsResourceNotFoundException() {
        Integer budgetId = 1;
        Budget updatedBudget = new Budget(user, new BigDecimal("1500.00"), LocalDate.of(2026, 10, 15));

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.updateBudget(user, budgetId, updatedBudget)
        );
        assertEquals("Budget not found", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // Test delete method
    @Test
    public void deleteBudget_DeletesBudget() {
        Integer budgetId = 1;
        Budget budget = new Budget(user, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 1));
        budget.setId(budgetId);

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.of(budget));
        budgetService.deleteBudget(user, budgetId);
        verify(budgetRepository).delete(budget);
    }

    @Test
    public void deleteBudget_ThrowsResourceNotFoundException() {
        Integer budgetId = 1;

        when(budgetRepository.findByUserAndId(user, budgetId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.deleteBudget(user, budgetId)
        );
        assertEquals("Budget not found", exception.getMessage());
        verify(budgetRepository, never()).delete(any(Budget.class));
    }
}