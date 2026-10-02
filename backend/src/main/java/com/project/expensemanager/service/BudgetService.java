package com.project.expensemanager.service;

import com.project.expensemanager.entity.Budget;
import com.project.expensemanager.entity.Expense;
import com.project.expensemanager.entity.RecurringExpense;
import com.project.expensemanager.entity.User;
import com.project.expensemanager.exception.BudgetExistsException;
import com.project.expensemanager.exception.ResourceNotFoundException;
import com.project.expensemanager.model.BudgetSummary;
import com.project.expensemanager.repository.BudgetRepository;
import com.project.expensemanager.repository.ExpenseRepository;
import com.project.expensemanager.repository.RecurringExpenseRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BudgetService {
    // Inject repositories
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository, RecurringExpenseRepository recurringExpenseRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
    }

    // Business logic
    // Create
    @Transactional
    public Budget createBudget(User user, Budget budget) {
        LocalDate date = budget.getBudgetPeriod().withDayOfMonth(1);        // Allow only one monthly budget
        Budget savedBudget = new Budget(user, budget.getAmount(), date);

        if (budgetRepository.existsByUserAndBudgetPeriod(user, date)) {
            throw new BudgetExistsException("Budget already exists for this month.");
        }

        try {
            return budgetRepository.saveAndFlush(savedBudget);
        }
        catch (DataIntegrityViolationException exception) {     // Edge case, check if a database rule was broken
            if (isMonthlyBudgetDuplicate(exception)) {
                throw new BudgetExistsException("Budget already exists for this month.");       // The database budget uniqueness constraint was broken
            }
            throw exception;        // Another database constraint was broken
        }
    }

    // Read
    public Budget getUserBudgetById(User user, Integer budgetId) {
        return budgetRepository.findByUserAndId(user, budgetId).orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    public Budget getUserBudgetByPeriod(User user, LocalDate period) {
        return budgetRepository.findByUserAndBudgetPeriod(user, period.withDayOfMonth(1)).orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    // Update
    @Transactional
    public Budget updateBudget(User user, Integer budgetId, Budget updatedBudget) {
        Budget budget = getUserBudgetById(user, budgetId);
        LocalDate budgetPeriod = updatedBudget.getBudgetPeriod().withDayOfMonth(1);

        if (budgetRepository.existsByUserAndBudgetPeriod(user, budgetPeriod) && !budgetPeriod.isEqual(budget.getBudgetPeriod())) {
                throw new BudgetExistsException("A different budget already exists for this month.");
        }

        budget.setAmount(updatedBudget.getAmount());
        budget.setBudgetPeriod(budgetPeriod);

        try {
            return budgetRepository.saveAndFlush(budget);
        }
        catch (DataIntegrityViolationException exception) {     // Edge case, check if a database rule was broken
            if (isMonthlyBudgetDuplicate(exception)) {
                throw new BudgetExistsException("Budget already exists for this month.");       // The database budget uniqueness constraint was broken
            }
            throw exception;        // Another database constraint was broken
        }
    }

    // Delete
    @Transactional
    public void deleteBudget(User user, Integer budgetId) {
        Budget budget = getUserBudgetById(user, budgetId);
        budgetRepository.delete(budget);
    }

    // Get total of current expenses
    public BigDecimal getTotalCurrentExpenses(User user, LocalDate period) {
        LocalDate startDate = period.withDayOfMonth(1);
        LocalDate endDate = period.withDayOfMonth(period.lengthOfMonth());
        List<Expense> currentExpenses = expenseRepository.findByUserAndExpenseDateBetween(user, startDate, endDate);
        BigDecimal total = new BigDecimal("0");

        for (Expense e : currentExpenses) {
            total = total.add(e.getAmount());
        }

        return total;
    }

    // Get total of recurring expenses
    public BigDecimal getTotalRecurringExpenses(User user, LocalDate period) {
       LocalDate monthStart = period.withDayOfMonth(1);
       LocalDate monthEnd = period.withDayOfMonth(period.lengthOfMonth());
       BigDecimal total = new BigDecimal("0");
       List<RecurringExpense> recurringExpenses = recurringExpenseRepository.findByUser(user);

       for (RecurringExpense r : recurringExpenses) {
           LocalDate rStart = r.getStartDate();
           LocalDate rEnd = r.getEndDate();
           long activeDays;
           LocalDate  paymentDate;
           boolean startsBeforeMonthEnd = rStart.isBefore(monthEnd) || rStart.isEqual(monthEnd);
           boolean endsAfterMonthStart = rEnd == null || rEnd.isAfter(monthStart) || rEnd.isEqual(monthStart);
           LocalDate recurringStart = rStart.isAfter(monthStart) ? rStart : monthStart;
           LocalDate recurringEnd = rEnd == null || rEnd.isAfter(monthEnd) ? monthEnd : rEnd;

           if (startsBeforeMonthEnd && endsAfterMonthStart) {
               switch (r.getFrequency()) {
                   case DAILY:
                       activeDays = ChronoUnit.DAYS.between(recurringStart, recurringEnd) + 1;

                       total = total.add(r.getAmount().multiply(BigDecimal.valueOf(activeDays)));
                       break;
                   case WEEKLY:
                       LocalDate rStartCopy = rStart;

                       while(rStartCopy.isBefore(recurringStart)) {
                            rStartCopy = rStartCopy.plusWeeks(1);
                       }

                       int weeks = 0;

                       while(!rStartCopy.isAfter(recurringEnd)) {
                           weeks++;
                           rStartCopy = rStartCopy.plusWeeks(1);
                       }

                       total = total.add(r.getAmount().multiply(BigDecimal.valueOf(weeks)));
                       break;
                   case MONTHLY:
                       paymentDate = LocalDate.of(period.getYear(), period.getMonth(), Math.min(rStart.getDayOfMonth(), monthEnd.getDayOfMonth()));

                       if (rEnd == null || !paymentDate.isAfter(rEnd)) {
                           total = total.add(r.getAmount());
                       }
                       break;
                   case YEARLY:
                       if (period.getMonth() == rStart.getMonth()) {
                           paymentDate = LocalDate.of(period.getYear(), rStart.getMonth(), Math.min(rStart.getDayOfMonth(), monthEnd.getDayOfMonth()));

                           if (rEnd == null || !paymentDate.isAfter(rEnd)) {
                               total = total.add(r.getAmount());
                           }
                       }
                       break;
               }
           }
       }

       return total;
    }

    // Get total of monthly expenses
    public BigDecimal getTotalMonthlyExpenses(BigDecimal expenses, BigDecimal recurringExpenses) {
        return expenses.add(recurringExpenses);
    }

    // Get current remaining budget
    public BigDecimal getRemainingBudget(BigDecimal budget, BigDecimal expenses) {
        return budget.subtract(expenses);
    }

    // Get percentage of budget usage per month
    public BigDecimal getBudgetPercentage(BigDecimal budget, BigDecimal expenses) {
        return expenses.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);
    }

    // Get a budget summary
    @Transactional(readOnly = true)
    public BudgetSummary getBudgetSummary(User user, LocalDate period) {
            Budget budget = getUserBudgetByPeriod(user, period);
            Integer id = budget.getId();
            LocalDate summaryPeriod = budget.getBudgetPeriod();
            BigDecimal amount = budget.getAmount();
            BigDecimal currentExpenses =  getTotalCurrentExpenses(user, period);
            BigDecimal recurringExpenses = getTotalRecurringExpenses(user, period);
            BigDecimal monthlyExpenses = getTotalMonthlyExpenses(currentExpenses, recurringExpenses);
            BigDecimal remainingCurrentBudget = getRemainingBudget(amount, currentExpenses);
            BigDecimal remainingMonthlyBudget = getRemainingBudget(amount, monthlyExpenses);
            BigDecimal budgetPercentage = getBudgetPercentage(amount, monthlyExpenses);

            return new BudgetSummary(
                    id,
                    amount,
                    summaryPeriod,
                    currentExpenses,
                    monthlyExpenses,
                    remainingCurrentBudget,
                    remainingMonthlyBudget,
                    budgetPercentage
            );
    }

    // Helper method
    // Checks if an exception is caused by the database "unique_monthly_budget_per_user" constraint
    private boolean isMonthlyBudgetDuplicate(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintException && "unique_monthly_budget_per_user".equals(constraintException.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

}