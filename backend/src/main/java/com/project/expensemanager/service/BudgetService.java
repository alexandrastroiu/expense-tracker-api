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
import org.springframework.stereotype.Service;

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
    public Budget createBudget(User user, Budget budget) {
        LocalDate date = budget.getBudgetPeriod().withDayOfMonth(1);        // Allow only one monthly budget
        Budget savedBudget = new Budget(user, budget.getAmount(), date);

        if (budgetRepository.existsByUserAndBudgetPeriod(user, date)) {
            throw new BudgetExistsException("Budget already exists for this month.");
        }

        return budgetRepository.save(savedBudget);
    }

    // Read
    public Budget getUserBudgetById(User user, Integer budgetId) {
        return budgetRepository.findByUserAndId(user, budgetId).orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    public Budget getUserBudgetByPeriod(User user, LocalDate period) {
        return budgetRepository.findByUserAndBudgetPeriod(user, period.withDayOfMonth(1)).orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    // Update
    public Budget updateBudget(User user, Integer budgetId, Budget updatedBudget) {
        Budget budget = getUserBudgetById(user, budgetId);
        LocalDate budgetPeriod = updatedBudget.getBudgetPeriod().withDayOfMonth(1);

        if (budgetRepository.existsByUserAndBudgetPeriod(user, budgetPeriod) && !budgetPeriod.isEqual(budget.getBudgetPeriod())) {
                throw new BudgetExistsException("A different budget already exists for this month.");
        }

        budget.setAmount(updatedBudget.getAmount());
        budget.setBudgetPeriod(budgetPeriod);
        return budgetRepository.save(budget);
    }

    // Delete
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

    // Get total of monthly expenses
    public BigDecimal getTotalMonthlyExpenses(User user, LocalDate period) {
       LocalDate monthStart = period.withDayOfMonth(1);
       LocalDate monthEnd = period.withDayOfMonth(period.lengthOfMonth());
       BigDecimal total = new BigDecimal("0");
       BigDecimal currentExpenses = getTotalCurrentExpenses(user, period);
       total = total.add(currentExpenses);
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

    // Get current remaining budget after expenses
    public BigDecimal getRemainingCurrentBudget(User user, LocalDate period) {
        return (getUserBudgetByPeriod(user, period).getAmount()).subtract(getTotalCurrentExpenses(user, period));
    }

    // Get monthly remaining budget after expenses
    public BigDecimal getRemainingMonthlyBudget(User user, LocalDate period) {
        return ((getUserBudgetByPeriod(user, period).getAmount()).subtract(getTotalMonthlyExpenses(user, period)));
    }

    // Get percentage of budget usage per month
    public BigDecimal getBudgetPercentage(User user, LocalDate period) {
        BigDecimal budget = getUserBudgetByPeriod(user, period).getAmount();
        BigDecimal expenses = getTotalMonthlyExpenses(user, period);

        return expenses.multiply(BigDecimal.valueOf(100)).divide(budget, RoundingMode.HALF_UP);
    }

    // Get a budget summary
    public BudgetSummary getBudgetSummary(User user, LocalDate period) {
            Integer id = getUserBudgetByPeriod(user, period).getId();
            BigDecimal amount = getUserBudgetByPeriod(user, period).getAmount();

            return new BudgetSummary(
                    id,
                    amount,
                    period,
                    getTotalCurrentExpenses(user, period),
                    getTotalMonthlyExpenses(user, period),
                    getRemainingCurrentBudget(user, period),
                    getRemainingMonthlyBudget(user, period),
                    getBudgetPercentage(user, period)
            );
    }

}