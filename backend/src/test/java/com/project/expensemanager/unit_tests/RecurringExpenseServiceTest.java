package com.project.expensemanager.unit_tests;

import com.project.expensemanager.entity.*;
import com.project.expensemanager.exception.InvalidRequestException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.repository.CategoryRepository;
import com.project.expensemanager.repository.RecurringExpenseRepository;
import com.project.expensemanager.service.RecurringExpenseService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for Recurring Expense service methods

@ExtendWith(MockitoExtension.class)
public class RecurringExpenseServiceTest {

    // Simulate external dependencies using the Mockito framework
    @Mock
    private RecurringExpenseRepository recurringExpenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

    User user;

    // Setup
    @BeforeEach
    void setUp() {
        user = new User("test_user");
    }

    // Test create method
    @Test
    public void createRecurringExpense_ValidRecurringExpense_Saved() {
        Category category = new Category("Entertainment");
        category.setId(1);
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);

        when(categoryRepository.findById(recurringExpense.getCategory().getId())).thenReturn(Optional.of(category));
        when(recurringExpenseRepository.save(recurringExpense)).thenReturn(recurringExpense);
        RecurringExpense result = recurringExpenseService.createRecurringExpense(recurringExpense);

        assertSame(recurringExpense, result);
        verify(recurringExpenseRepository).save(recurringExpense);
    }

    @Test
    public void createRecurringExpense_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Category category = new Category("Entertainment");
        category.setId(1);
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);

        when(categoryRepository.findById(recurringExpense.getCategory().getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> recurringExpenseService.createRecurringExpense(recurringExpense)
        );
        assertEquals("Category not found", exception.getMessage());
        verify(recurringExpenseRepository, never()).delete(any(RecurringExpense.class));
    }

    @Test
    public void createRecurringExpense_WithInvalidDate_ThrowsInvalidRequestException() {
        Category category = new Category("Entertainment");
        category.setId(1);
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(1), Frequency.MONTHLY);

        when(categoryRepository.findById(recurringExpense.getCategory().getId())).thenReturn(Optional.of(category));
        Exception exception = assertThrows(InvalidRequestException.class,
                () -> recurringExpenseService.createRecurringExpense(recurringExpense)
        );
        assertEquals("End date cannot be before start date.", exception.getMessage());
        verify(recurringExpenseRepository, never()).delete(any(RecurringExpense.class));
    }

    // Test read methods
    @Test
    public void getUserRecurringExpenseById_ReturnsCorrectData() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(id);

        when(recurringExpenseRepository.findByUserAndId(user, id)).thenReturn(Optional.of(recurringExpense));
        RecurringExpense result = recurringExpenseService.getUserRecurringExpenseById(user, id);
        assertSame(recurringExpense, result);
        verify(recurringExpenseRepository).findByUserAndId(user, id);
    }

    @Test
    public void getUserRecurringExpenseById_ThrowsResourceNotFoundException() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(id);

        when(recurringExpenseRepository.findByUserAndId(user, id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> recurringExpenseService.getUserRecurringExpenseById(user, id)
        );
        assertEquals("Recurring Expense not found.", exception.getMessage());
        verify(recurringExpenseRepository).findByUserAndId(user, id);
    }

    @Test
    public void getUserRecurringExpensesByTitle_ReturnsCorrectData() {
        Integer id = 1;
        String title = "Netflix subscription";
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> expected = List.of(new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY));
        expected.getFirst().setId(id);

        when(recurringExpenseRepository.findByUserAndTitle(user, title)).thenReturn(expected);
        List<RecurringExpense> result = recurringExpenseService.getUserRecurringExpenseByTitle(user, title);
        assertSame(expected, result);
        verify(recurringExpenseRepository).findByUserAndTitle(user, title);
    }

    @Test
    public void getUserRecurringExpensesByCategory_ReturnsCorrectData() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> expected = List.of(new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY));
        expected.getFirst().setId(id);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(recurringExpenseRepository.findByUserAndCategory(user, category)).thenReturn(expected);
        List<RecurringExpense> result = recurringExpenseService.getUserRecurringExpenseByCategory(user, category.getId());
        assertSame(expected, result);
        verify(recurringExpenseRepository).findByUserAndCategory(user, category);
    }

    @Test
    public void getUserRecurringExpensesByCategory_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> expected = List.of(new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY));
        expected.getFirst().setId(id);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> recurringExpenseService.getUserRecurringExpenseByCategory(user, category.getId())
        );
        assertEquals("Category not found", exception.getMessage());
    }

    @Test
    public void getUserRecurringExpensesByFrequency_ReturnsCorrectData() {
        Integer id = 1;
        Frequency frequency = Frequency.MONTHLY;
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> expected = List.of(new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY));
        expected.getFirst().setId(id);

        when(recurringExpenseRepository.findByUserAndFrequency(user, frequency)).thenReturn(expected);
        List<RecurringExpense> result = recurringExpenseService.getUserRecurringExpenseByFrequency(user, frequency);
        assertSame(expected, result);
        verify(recurringExpenseRepository).findByUserAndFrequency(user, frequency);
    }

    @Test
    public void getAllUserRecurringExpenses_ReturnsCorrectData() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> expected = List.of(new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY));
        expected.getFirst().setId(id);

        when(recurringExpenseRepository.findByUser(user)).thenReturn(expected);
        List<RecurringExpense> result = recurringExpenseService.getAllUserRecurringExpenses(user);
        assertSame(expected, result);
        verify(recurringExpenseRepository).findByUser(user);
    }

    @Test
    public void searchRecurringExpenses_WithCriteria_ReturnsCorrectData() {
        Integer id = 1;
        String title = "Netflix subscription";
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> recurringExpenses = List.of(
                new RecurringExpense(user, "Youtube", "", new BigDecimal("4.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY),
                new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY)
        );
        List<RecurringExpense> expected = List.of(recurringExpenses.get(1));

        when(recurringExpenseRepository.findByUser(user)).thenReturn(List.of(recurringExpenses.get(1)));
        List<RecurringExpense> result = recurringExpenseService.searchRecurringExpenses(user, title, null, null);
        assertEquals(expected, result);
        verify(recurringExpenseRepository).findByUser(user);
    }

    @Test
    public void searchRecurringExpenses_WithNoCriteria_ReturnsCorrectData() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        List<RecurringExpense> recurringExpenses = List.of(
                new RecurringExpense(user, "Youtube", "", new BigDecimal("4.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY),
                new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY)
        );

        when(recurringExpenseRepository.findByUser(user)).thenReturn(recurringExpenses);
        List<RecurringExpense> result = recurringExpenseService.searchRecurringExpenses(user, null, null, null);
        assertEquals(recurringExpenses, result);
        verify(recurringExpenseRepository).findByUser(user);
    }

    @Test
    public void filterRecurringExpensesByAmount_ReturnsCorrectData() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        BigDecimal minAmount = new BigDecimal("2.00"), maxAmount = new BigDecimal("5.00");
        List<RecurringExpense> recurringExpenses = List.of(
                new RecurringExpense(user, "Youtube", "", new BigDecimal("4.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY),
                new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY)
        );
        List<RecurringExpense> expected = List.of(recurringExpenses.getFirst());

        when(recurringExpenseRepository.findByUserAndAmountBetween(user, minAmount, maxAmount)).thenReturn(expected);
        List<RecurringExpense> result = recurringExpenseService.filterRecurringExpensesByAmount(user, minAmount, maxAmount);
        assertEquals(expected, result);
        verify(recurringExpenseRepository).findByUserAndAmountBetween(user, minAmount, maxAmount);
    }

    @Test
    public void filterRecurringExpensesByAmount_WithInvalidFilter_ThrowsInvalidRequestException() {
        Integer id = 1;
        Category category = new Category("Entertainment");
        category.setId(id);
        BigDecimal minAmount = new BigDecimal("5.00"), maxAmount = new BigDecimal("2.00");
        List<RecurringExpense> recurringExpenses = List.of(
                new RecurringExpense(user, "Youtube", "", new BigDecimal("4.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY),
                new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY)
        );
        List<RecurringExpense> expected = List.of(recurringExpenses.getFirst());

        Exception exception = assertThrows(InvalidRequestException.class,
                () -> recurringExpenseService.filterRecurringExpensesByAmount(user, minAmount, maxAmount));
        assertEquals("Minimum amount cannot be greater than maximum amount", exception.getMessage());
    }

    // Test update method
    @Test
    public void updateExpense_UpdatesExpense() {
        Integer categoryId = 1, recurringExpenseId = 1;
        Category category = new Category("Entertainment");
        category.setId(categoryId);
        RecurringExpense recurringExpense =  new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(recurringExpenseId);
        RecurringExpense updatedRecurringExpense =   new RecurringExpense(user, "New title", "New description", new BigDecimal("6.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.WEEKLY);

        when(recurringExpenseRepository.findByUserAndId(user, recurringExpenseId)).thenReturn(Optional.of(recurringExpense));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(recurringExpenseRepository.save(recurringExpense)).thenReturn(recurringExpense);
        RecurringExpense result = recurringExpenseService.updateRecurringExpense(recurringExpenseId, user, updatedRecurringExpense);
        assertEquals(recurringExpenseId, result.getId());
        assertEquals("New title", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(new BigDecimal("6.99"), result.getAmount());
        assertEquals(category, result.getCategory());
        assertEquals(LocalDate.now(), result.getStartDate());
        assertEquals(LocalDate.now().plusMonths(1), result.getEndDate());
        assertEquals(Frequency.WEEKLY, result.getFrequency());
        verify(recurringExpenseRepository).findByUserAndId(user, recurringExpenseId);
        verify(categoryRepository).findById(category.getId());
        verify(recurringExpenseRepository).save(recurringExpense);
    }

    @Test
    public void updateExpense_WithInvalidCategory_ThrowsResourceNotFoundException() {
        Integer categoryId = 1, recurringExpenseId = 1;
        Category category1 = new Category("Entertainment"), category2 = new Category("Others");
        category1.setId(categoryId);
        category2.setId(2);
        RecurringExpense recurringExpense =  new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category1, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(recurringExpenseId);
        RecurringExpense updatedRecurringExpense =   new RecurringExpense(user, "New title", "New description", new BigDecimal("6.99"), category2, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.WEEKLY);

        when(recurringExpenseRepository.findByUserAndId(user, recurringExpenseId)).thenReturn(Optional.of(recurringExpense));
        when(categoryRepository.findById(updatedRecurringExpense.getCategory().getId())).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                ResourceNotFoundException.class,
                () -> recurringExpenseService.updateRecurringExpense(recurringExpenseId, user, updatedRecurringExpense));
        assertEquals("Category not found.", exception.getMessage());
        verify(recurringExpenseRepository).findByUserAndId(user, recurringExpenseId);
        verify(categoryRepository).findById(category2.getId());
        verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
    }

    @Test
    public void updateExpense_WithInvalidDate_ThrowsInvalidRequestException() {
        Integer categoryId = 1, recurringExpenseId = 1;
        Category category = new Category("Entertainment");
        category.setId(categoryId);
        RecurringExpense recurringExpense =  new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), category, LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(recurringExpenseId);
        RecurringExpense updatedRecurringExpense =   new RecurringExpense(user, "New title", "New description", new BigDecimal("6.99"), category, LocalDate.now().plusMonths(1), LocalDate.now(), Frequency.WEEKLY);

        when(recurringExpenseRepository.findByUserAndId(user, recurringExpenseId)).thenReturn(Optional.of(recurringExpense));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        Exception exception = assertThrows(InvalidRequestException.class,
                () -> recurringExpenseService.updateRecurringExpense(recurringExpenseId, user, updatedRecurringExpense)
        );
        assertEquals("End date cannot be before start date.", exception.getMessage());
        verify(recurringExpenseRepository).findByUserAndId(user, recurringExpenseId);
        verify(categoryRepository).findById(category.getId());
        verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
    }

    // Test delete method
    @Test
    public void deleteExpense_DeletesExpense() {
        Integer id = 1;
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), new Category("Entertainment"), LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(id);

        when(recurringExpenseRepository.findByUserAndId(user, id)).thenReturn(Optional.of(recurringExpense));
        recurringExpenseService.deleteRecurringExpense(user, recurringExpense.getId());
        verify(recurringExpenseRepository).findByUserAndId(user, id);
        verify(recurringExpenseRepository).delete(recurringExpense);
    }

    @Test
    public void deleteExpense_ThrowsResourceNotFoundException() {
        Integer id = 1;
        RecurringExpense recurringExpense = new RecurringExpense(user, "Netflix subscription", "", new BigDecimal("5.99"), new Category("Entertainment"), LocalDate.now(), LocalDate.now().plusMonths(1), Frequency.MONTHLY);
        recurringExpense.setId(id);

        when(recurringExpenseRepository.findByUserAndId(user, id)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                ResourceNotFoundException.class,
                () -> recurringExpenseService.deleteRecurringExpense(user, recurringExpense.getId()));
        assertEquals("Recurring Expense not found.", exception.getMessage());
        verify(recurringExpenseRepository).findByUserAndId(user, id);
        verify(recurringExpenseRepository, never()).delete(any(RecurringExpense.class));
    }
}