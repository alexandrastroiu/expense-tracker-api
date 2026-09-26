/** Expense Manager Database Schema **/

-- USERS
CREATE TABLE users(
    id  SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- CATEGORIES
CREATE TABLE categories(
    id SERIAL PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE
);

-- EXPENSES
CREATE TABLE expenses(
    id  SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    amount NUMERIC(9, 2) NOT NULL CHECK (amount > 0),
    category_id INT NOT NULL REFERENCES categories(id),
    expense_date DATE NOT NULL
);

-- RECURRING EXPENSES
CREATE TABLE recurring_expenses(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    amount NUMERIC(9,2) NOT NULL CHECK (amount > 0),
    category_id INT NOT NULL REFERENCES categories(id),
    start_date DATE NOT NULL,
    end_date DATE CHECK (end_date IS NULL OR end_date >= start_date),
    frequency VARCHAR(50) NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'))
);

-- BUDGETS
CREATE TABLE budgets(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(9, 2) NOT NULL CHECK (amount > 0),
    budget_period DATE NOT NULL,

    CONSTRAINT unique_monthly_budget_per_user UNIQUE (user_id, budget_period)
);