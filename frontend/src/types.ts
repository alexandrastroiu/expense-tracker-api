export interface Category {
  id: number;
  categoryName: string;
}
export type Frequency = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY";
export interface Expense {
  id: number;
  title: string;
  description: string | null;
  amount: number;
  categoryId: number;
  expenseDate?: string;
  startDate?: string;
  endDate?: string | null;
  frequency?: Frequency;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export interface Budget {
  id: number;
  amount: number;
  budgetPeriod: string;
}
export interface Summary extends Budget {
  totalCurrentExpenses: number;
  totalMonthlyExpenses: number;
  remainingCurrentBudget: number;
  remainingMonthlyBudget: number;
  budgetPercentage: number;
}

export const ACCOUNT_CURRENCIES = [
  "RON", "EUR", "USD", "GBP", "CAD", "AUD", "CHF", "JPY", "PLN", "BGN", "HUF",
] as const;
export type AccountCurrency = (typeof ACCOUNT_CURRENCIES)[number];
export interface UserProfile {
  username: string;
  currency: AccountCurrency;
}
