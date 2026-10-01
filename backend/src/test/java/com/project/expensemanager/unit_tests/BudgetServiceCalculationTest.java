package com.project.expensemanager.unit_tests;

import com.project.expensemanager.entity.*;
import com.project.expensemanager.model.BudgetSummary;
import com.project.expensemanager.repository.BudgetRepository;
import com.project.expensemanager.repository.ExpenseRepository;
import com.project.expensemanager.repository.RecurringExpenseRepository;
import com.project.expensemanager.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceCalculationTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RecurringExpenseRepository recurringExpenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private BudgetService budgetService;

    private final User user = new User("test_user");
    private final Category category = new Category("Other");

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            nullValues = "NULL",
            value = {
                    "Monthly Jan 31 clamps to February, MONTHLY, 2026-01-31, NULL,       2026-02-01, 1.00",
                    "Monthly ends before payment,      MONTHLY, 2026-01-31, 2026-02-15, 2026-02-01, 0.00",
                    "Monthly continues in March,       MONTHLY, 2026-01-31, NULL,       2026-03-01, 1.00",
                    "Leap anniversary in non-leap year, YEARLY, 2024-02-29, NULL,       2025-02-01, 1.00",
                    "Leap anniversary in leap year,     YEARLY, 2024-02-29, NULL,       2028-02-01, 1.00",
                    "Weekly crosses month boundary,     WEEKLY, 2026-01-29, NULL,       2026-02-01, 4.00",
                    "Daily includes February 29,        DAILY,  2024-02-10, NULL,       2024-02-01, 20.00"
            }
    )
    void getTotalRecurringExpenses_matchesDocumentCases(
            String description,
            Frequency frequency,
            LocalDate start,
            LocalDate end,
            LocalDate period,
            String expected
    ) {
        when(recurringExpenseRepository.findByUser(user))
                .thenReturn(List.of(
                        recurring(frequency, start, end, "1.00")
                ));

        assertMoney(
                expected,
                budgetService.getTotalRecurringExpenses(user, period)
        );

        verifyNoInteractions(expenseRepository, budgetRepository);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            nullValues = "NULL",
            value = {
                    "Daily includes both endpoints,       DAILY,   2026-02-10, 2026-02-12, 2026-02-01, 3.00",
                    "Daily same-day series,               DAILY,   2026-02-28, 2026-02-28, 2026-02-01, 1.00",
                    "Daily clamps to requested month,     DAILY,   2026-01-10, 2026-03-10, 2026-02-01, 28.00",
                    "Daily ends on month first day,       DAILY,   2026-01-10, 2026-02-01, 2026-02-01, 1.00",
                    "Future series contributes nothing,   DAILY,   2026-03-01, NULL,       2026-02-01, 0.00",
                    "Finished series contributes nothing, DAILY,   2026-01-01, 2026-01-31, 2026-02-01, 0.00",

                    "Weekly ends before next payment,     WEEKLY,  2026-01-29, 2026-02-04, 2026-02-01, 0.00",
                    "Weekly includes payment on end date, WEEKLY,  2026-01-29, 2026-02-05, 2026-02-01, 1.00",
                    "Weekly counts five occurrences,      WEEKLY,  2026-01-01, NULL,       2026-01-01, 5.00",

                    "Monthly includes clamped end date,   MONTHLY, 2026-01-31, 2026-02-28, 2026-02-01, 1.00",
                    "Monthly preserves original day 31,   MONTHLY, 2026-01-31, 2026-03-30, 2026-03-01, 0.00",
                    "Monthly clamps to leap February,     MONTHLY, 2024-01-31, NULL,       2024-02-01, 1.00",
                    "Monthly includes initial payment,    MONTHLY, 2026-02-28, 2026-02-28, 2026-02-01, 1.00",

                    "Yearly skips other months,           YEARLY,  2024-02-29, NULL,       2025-03-01, 0.00",
                    "Yearly ends before clamped payment,  YEARLY,  2024-02-29, 2025-02-27, 2025-02-01, 0.00",
                    "Yearly includes clamped end date,    YEARLY,  2024-02-29, 2025-02-28, 2025-02-01, 1.00",
                    "Yearly preserves leap anniversary,   YEARLY,  2024-02-29, 2028-02-28, 2028-02-01, 0.00"
            }
    )
    void getTotalRecurringExpenses_handlesRecurrenceBoundaries(
            String description,
            Frequency frequency,
            LocalDate start,
            LocalDate end,
            LocalDate period,
            String expected
    ) {
        when(recurringExpenseRepository.findByUser(user))
                .thenReturn(List.of(
                        recurring(frequency, start, end, "1.00")
                ));

        assertMoney(
                expected,
                budgetService.getTotalRecurringExpenses(user, period)
        );

        verifyNoInteractions(expenseRepository, budgetRepository);
    }

    @Test
    void getTotalRecurringExpenses_noExpenses_returnsZero() {
        when(recurringExpenseRepository.findByUser(user))
                .thenReturn(List.of());

        assertMoney(
                "0.00",
                budgetService.getTotalRecurringExpenses(
                        user, LocalDate.of(2026, 2, 1)
                )
        );

        verifyNoInteractions(expenseRepository, budgetRepository);
    }

    @Test
    void getTotalRecurringExpenses_combinesAllFrequencies() {
        when(recurringExpenseRepository.findByUser(user))
                .thenReturn(mixedRecurringExpenses());

        assertMoney(
                "30.29",
                budgetService.getTotalRecurringExpenses(
                        user, LocalDate.of(2026, 2, 17)
                )
        );

        verify(recurringExpenseRepository).findByUser(user);
        verifyNoInteractions(expenseRepository, budgetRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "15.35, 30.29, 45.64",
            "0.00,  30.29, 30.29",
            "15.35, 0.00,  15.35",
            "0.00,  0.00,  0.00"
    })
    void getTotalMonthlyExpenses_combinesSuppliedTotals(
            BigDecimal currentExpenses,
            BigDecimal recurringExpenses,
            String expected
    ) {
        assertMoney(
                expected,
                budgetService.getTotalMonthlyExpenses(
                        currentExpenses, recurringExpenses
                )
        );

        verifyNoInteractions(
                budgetRepository,
                expenseRepository,
                recurringExpenseRepository
        );
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 45.64, 54.36",
            "100.00, 0.00,  100.00",
            "100.00, 100.00, 0.00",
            "20.00,  45.64, -25.64"
    })
    void getRemainingBudget_subtractsExpenses(
            BigDecimal budget,
            BigDecimal expenses,
            String expected
    ) {
        assertMoney(
                expected,
                budgetService.getRemainingBudget(budget, expenses)
        );

        verifyNoInteractions(
                budgetRepository,
                expenseRepository,
                recurringExpenseRepository
        );
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 45.64, 45.64",
            "100.00, 0.00,  0.00",
            "100.00, 100.00, 100.00",
            "20.00,  45.64, 228.20",
            "3.00,   1.00,  33.33",
            "6.00,   1.00,  16.67"
    })
    void getBudgetPercentage_calculatesAndRounds(
            BigDecimal budget,
            BigDecimal expenses,
            String expected
    ) {
        assertMoney(
                expected,
                budgetService.getBudgetPercentage(budget, expenses)
        );

        verifyNoInteractions(
                budgetRepository,
                expenseRepository,
                recurringExpenseRepository
        );
    }

    @Test
    void getBudgetSummary_calculatesAllFields() {
        LocalDate period = LocalDate.of(2026, 2, 1);
        stubBudget(period, "100.00");
        stubMixedExpenses(period);

        BudgetSummary summary =
                budgetService.getBudgetSummary(user, period);

        assertAll(
                () -> assertEquals(Integer.valueOf(7), summary.id()),
                () -> assertEquals(period, summary.budgetPeriod()),
                () -> assertMoney("100.00", summary.amount()),
                () -> assertMoney(
                        "15.35", summary.totalCurrentExpenses()
                ),
                () -> assertMoney(
                        "45.64", summary.totalMonthlyExpenses()
                ),
                () -> assertMoney(
                        "84.65", summary.remainingCurrentBudget()
                ),
                () -> assertMoney(
                        "54.36", summary.remainingMonthlyBudget()
                ),
                () -> assertMoney(
                        "45.64", summary.budgetPercentage()
                )
        );

        verifySummaryQueriesOnce(period);
    }

    @Test
    void getBudgetSummary_overBudget_preservesNegativeRemainingAmount() {
        LocalDate period = LocalDate.of(2026, 2, 1);
        stubBudget(period, "20.00");
        stubMixedExpenses(period);

        BudgetSummary summary =
                budgetService.getBudgetSummary(user, period);

        assertAll(
                () -> assertMoney(
                        "4.65", summary.remainingCurrentBudget()
                ),
                () -> assertMoney(
                        "-25.64", summary.remainingMonthlyBudget()
                ),
                () -> assertMoney(
                        "228.20", summary.budgetPercentage()
                )
        );

        verifySummaryQueriesOnce(period);
    }

    @Test
    void getBudgetSummary_noExpenses_returnsFullBudget() {
        LocalDate period = LocalDate.of(2026, 2, 1);
        stubBudget(period, "100.00");
        stubExpenses(period, List.of(), List.of());

        BudgetSummary summary =
                budgetService.getBudgetSummary(user, period);

        assertAll(
                () -> assertMoney(
                        "0.00", summary.totalCurrentExpenses()
                ),
                () -> assertMoney(
                        "0.00", summary.totalMonthlyExpenses()
                ),
                () -> assertMoney(
                        "100.00", summary.remainingCurrentBudget()
                ),
                () -> assertMoney(
                        "100.00", summary.remainingMonthlyBudget()
                ),
                () -> assertMoney(
                        "0.00", summary.budgetPercentage()
                )
        );

        verifySummaryQueriesOnce(period);
    }

    private void stubMixedExpenses(LocalDate period) {
        stubExpenses(
                period,
                List.of(expense("10.10"), expense("5.25")),
                mixedRecurringExpenses()
        );
    }

    private List<RecurringExpense> mixedRecurringExpenses() {
        return List.of(
                recurring(
                        Frequency.DAILY,
                        LocalDate.of(2026, 2, 10),
                        LocalDate.of(2026, 2, 12),
                        "0.10"
                ),
                recurring(
                        Frequency.WEEKLY,
                        LocalDate.of(2026, 1, 29),
                        null,
                        "2.50"
                ),
                recurring(
                        Frequency.MONTHLY,
                        LocalDate.of(2026, 1, 31),
                        null,
                        "7.99"
                ),
                recurring(
                        Frequency.YEARLY,
                        LocalDate.of(2024, 2, 29),
                        null,
                        "12.00"
                )
        );
    }

    private void stubExpenses(
            LocalDate period,
            List<Expense> current,
            List<RecurringExpense> recurring
    ) {
        when(expenseRepository.findByUserAndExpenseDateBetween(
                user,
                period.withDayOfMonth(1),
                period.withDayOfMonth(period.lengthOfMonth())
        )).thenReturn(current);

        when(recurringExpenseRepository.findByUser(user))
                .thenReturn(recurring);
    }

    private void stubBudget(LocalDate period, String amount) {
        LocalDate monthStart = period.withDayOfMonth(1);
        Budget budget = new Budget(
                user, new BigDecimal(amount), monthStart
        );
        budget.setId(7);

        when(budgetRepository.findByUserAndBudgetPeriod(
                user, monthStart
        )).thenReturn(Optional.of(budget));
    }

    private void verifySummaryQueriesOnce(LocalDate period) {
        LocalDate monthStart = period.withDayOfMonth(1);
        LocalDate monthEnd =
                period.withDayOfMonth(period.lengthOfMonth());

        verify(budgetRepository).findByUserAndBudgetPeriod(
                user, monthStart
        );
        verify(expenseRepository).findByUserAndExpenseDateBetween(
                user, monthStart, monthEnd
        );
        verify(recurringExpenseRepository).findByUser(user);

        verifyNoMoreInteractions(
                budgetRepository,
                expenseRepository,
                recurringExpenseRepository
        );
    }

    private RecurringExpense recurring(
            Frequency frequency,
            LocalDate start,
            LocalDate end,
            String amount
    ) {
        return new RecurringExpense(
                user,
                "Recurring expense",
                "",
                new BigDecimal(amount),
                category,
                start,
                end,
                frequency
        );
    }

    private Expense expense(String amount) {
        Expense expense = mock(Expense.class);
        when(expense.getAmount()).thenReturn(new BigDecimal(amount));
        return expense;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual
        );
    }
}