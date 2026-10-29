export interface Category { id: number; categoryName: string }
export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
export interface Expense { id: number; title: string; description: string | null; amount: number; categoryId: number; expenseDate?: string; startDate?: string; endDate?: string | null; frequency?: Frequency }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface Budget { id: number; amount: number; budgetPeriod: string }
export interface Summary extends Budget { totalCurrentExpenses: number; totalMonthlyExpenses: number; remainingCurrentBudget: number; remainingMonthlyBudget: number; budgetPercentage: number }
