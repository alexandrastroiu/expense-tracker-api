package com.project.expensemanager.entity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false, precision = 9, scale = 2)
    private BigDecimal amount;

    @Column(name = "budget_period", nullable = false)
    private LocalDate budgetPeriod;

    // Default constructor
    protected Budget() {}

    // Parameterized constructor
    public Budget(User user, BigDecimal amount, LocalDate budgetPeriod) {
        this.user = user;
        this.amount = amount;
        this.budgetPeriod = budgetPeriod;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getBudgetPeriod() {
        return budgetPeriod;
    }

    public void setBudgetPeriod(LocalDate budgetPeriod) {
        this.budgetPeriod = budgetPeriod;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}