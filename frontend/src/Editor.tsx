import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { X } from "lucide-react";
import type { Category, Expense, Budget, AccountCurrency } from "./types";
export function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
export function Modal({
  title,
  close,
  children,
}: {
  title: string;
  close: () => void;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
      aria-labelledby="modal-title"
    >
      <div className="modal-heading">
        <h2 id="modal-title">{title}</h2>
        <button
          className="icon-button"
          aria-label="Close dialog"
          onClick={close}
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export default function Editor({
  currency,
  item,
  recurring,
  categories,
  budget,
  period,
  close,
  save,
}: {
  currency: AccountCurrency;
  item?: Expense;
  recurring: boolean;
  categories: Category[];
  budget?: Budget | null;
  period: string;
  close: () => void;
  save: (body: object) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const isBudget = budget !== undefined;
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = Object.fromEntries(
      new FormData(event.currentTarget),
    ) as Record<string, string>;
    setError("");
    if (recurring && values.endDate && values.endDate < values.startDate) {
      setError("End date must be on or after the start date.");
      return;
    }
    setBusy(true);
    try {
      await save(
        isBudget
          ? { amount: values.amount, budgetPeriod: `${period}-01` }
          : {
              ...values,
              categoryId: Number(values.categoryId),
              ...(recurring ? { endDate: values.endDate || null } : {}),
            },
      );
      close();
    } catch (error) {
      setError(error instanceof Error ? error.message : "Unable to save.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={
        isBudget
          ? budget
            ? "Edit monthly budget"
            : "Set a monthly budget"
          : `${item ? "Edit" : "Add"} ${recurring ? "recurring payment" : "expense"}`
      }
      close={() => {
        if (!busy) close();
      }}
    >
      <p className="muted">
        {isBudget
          ? `Set the budget amount for ${period}.`
          : "Enter the details below."}
      </p>
      <form onSubmit={submit}>
        {error && (
          <div className="error" role="alert">
            {error}
          </div>
        )}
        <fieldset disabled={busy}>
          {!isBudget && (
            <label>
              Title
              <input
                name="title"
                autoFocus
                required
                maxLength={100}
                defaultValue={item?.title}
                placeholder="e.g. Weekly groceries"
              />
            </label>
          )}
          <div className="form-grid">
            <label>
              Amount ({currency})
              <input
                name="amount"
                type="number"
                inputMode="decimal"
                required
                min="0.01"
                max="9999999.99"
                step="0.01"
                defaultValue={isBudget ? budget?.amount : item?.amount}
                placeholder="0.00"
                autoFocus={isBudget}
              />
            </label>
            {!isBudget && (
              <label>
                Category
                <select
                  name="categoryId"
                  required
                  defaultValue={item?.categoryId || ""}
                >
                  <option value="" disabled>
                    Choose a category
                  </option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.categoryName}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>
          {!isBudget && (
            <>
              <div className="form-grid">
                <label>
                  {recurring ? "Start date" : "Date"}
                  <input
                    name={recurring ? "startDate" : "expenseDate"}
                    type="date"
                    required
                    defaultValue={
                      item?.startDate || item?.expenseDate || today()
                    }
                  />
                </label>
                {recurring && (
                  <label>
                    End date <span className="muted">(optional)</span>
                    <input
                      name="endDate"
                      type="date"
                      defaultValue={item?.endDate || ""}
                    />
                  </label>
                )}
              </div>
              {recurring && (
                <label>
                  Frequency
                  <select
                    name="frequency"
                    defaultValue={item?.frequency || "MONTHLY"}
                  >
                    {["DAILY", "WEEKLY", "MONTHLY", "YEARLY"].map((value) => (
                      <option key={value} value={value}>
                        {value.charAt(0) + value.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              <label>
                Notes <span className="muted">(optional)</span>
                <textarea
                  name="description"
                  rows={3}
                  maxLength={500}
                  defaultValue={item?.description || ""}
                  placeholder="Add a description"
                />
              </label>
            </>
          )}
          <div className="modal-actions">
            <button type="button" className="secondary" onClick={close}>
              Cancel
            </button>
            <button className="primary" type="submit">
              {busy ? "Saving…" : "Save changes"}
            </button>
          </div>
        </fieldset>
      </form>
    </Modal>
  );
}
