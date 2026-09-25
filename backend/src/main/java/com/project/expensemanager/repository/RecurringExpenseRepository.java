package com.project.expensemanager.repository;

import com.project.expensemanager.entity.Category;
import com.project.expensemanager.entity.Frequency;
import com.project.expensemanager.entity.RecurringExpense;
import com.project.expensemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Integer> {

    // Query methods
    List<RecurringExpense> findByUser(User user);

    Optional<RecurringExpense> findByUserAndId(User user, Integer id);

    List<RecurringExpense> findByUserAndTitle(User user, String title);

    List<RecurringExpense> findByUserAndFrequency(User user, Frequency frequency);

    List<RecurringExpense> findByUserAndCategory(User user, Category category);

    List<RecurringExpense> findByUserAndAmountBetween(User user, BigDecimal startAmount, BigDecimal endAmount);
}
