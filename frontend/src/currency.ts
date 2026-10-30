import type { AccountCurrency } from "./types";

export function currencyFormatter(currency: AccountCurrency, locale?: string) {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    currencyDisplay: "code",
    // Preserve the backend's two-decimal amounts, including for JPY.
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
