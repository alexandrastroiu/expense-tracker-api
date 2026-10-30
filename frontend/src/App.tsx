import { useCallback, useEffect, useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  LayoutDashboard,
  LogOut,
  Moon,
  Pencil,
  Plus,
  RefreshCw,
  Repeat2,
  Search,
  Sun,
  Trash2,
  Wallet,
  X,
} from "lucide-react";
import Auth, { Brand } from "./Auth";
import Editor, { Modal, today } from "./Editor";
import {
  ApiError,
  listPath,
  readSession,
  request,
  saveSession,
  type Session,
} from "./api";
import type { Budget, Category, Expense, Page, Summary } from "./types";
const money = (value: number) =>
  new Intl.NumberFormat(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
const dateLabel = (value?: string | null) =>
  value
    ? new Date(`${value}T12:00:00`).toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "Ongoing";
type View = "overview" | "expenses" | "recurring";
function useTheme() {
  const [theme, setTheme] = useState(() => {
    try {
      return (
        localStorage.getItem("ledger.theme") ||
        (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
      );
    } catch {
      return "light";
    }
  });
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem("ledger.theme", theme);
    } catch {
      /* Theme still works without storage. */
    }
  }, [theme]);
  return { theme, toggle: () => setTheme(theme === "dark" ? "light" : "dark") };
}
export default function App() {
  const [session, setSession] = useState<Session | null>(readSession);
  const [notice, setNotice] = useState("");
  const { theme, toggle } = useTheme();
  const logout = useCallback((message = "") => {
    saveSession(null);
    setSession(null);
    setNotice(message);
  }, []);
  useEffect(() => {
    if (!session) return;
    const timeout = setTimeout(
      () => logout("Your session expired. Sign in to continue."),
      Math.max(0, session.expiresAt - Date.now()),
    );
    return () => clearTimeout(timeout);
  }, [session, logout]);
  return (
    <>
      <button
        className="theme-toggle"
        onClick={toggle}
        aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
        title={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
      >
        {theme === "light" ? <Moon size={18} /> : <Sun size={18} />}
      </button>
      {session ? (
        <Workspace key={session.token} session={session} logout={logout} />
      ) : (
        <Auth
          onLogin={(value) => {
            setNotice("");
            setSession(value);
          }}
          notice={notice}
        />
      )}
    </>
  );
}
function Workspace({
  session,
  logout,
}: {
  session: Session;
  logout: (message?: string) => void;
}) {
  const [view, setView] = useState<View>("overview");
  const [period, setPeriod] = useState(today().slice(0, 7));
  const [categories, setCategories] = useState<Category[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [summaryError, setSummaryError] = useState("");
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [data, setData] = useState<Page<Expense> | null>(null);
  const [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [sort, setSort] = useState("");
  const [filters, setFilters] = useState<Record<string, string>>({
    mode: "search",
  });
  const [refresh, setRefresh] = useState(0),
    [toast, setToast] = useState("");
  const [editor, setEditor] = useState<{
    item?: Expense;
    budget?: Budget | null;
  } | null>(null);
  const [deletion, setDeletion] = useState<{
    id: number;
    title: string;
    budget?: boolean;
  } | null>(null);
  const [deleting, setDeleting] = useState(false),
    [deleteError, setDeleteError] = useState("");
  const recurring = view === "recurring";
  const api = useCallback(
    async <T,>(path: string, options?: RequestInit) => {
      try {
        return await request<T>(path, session.token, options);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401)
          logout("Your session ended. Please sign in again.");
        throw error;
      }
    },
    [session.token, logout],
  );
  useEffect(() => {
    const controller = new AbortController();
    api<Category[]>("/categories", { signal: controller.signal })
      .then(setCategories)
      .catch((error) => {
        if (error.name !== "AbortError") setError(error.message);
      });
    return () => controller.abort();
  }, [api, refresh]);
  useEffect(() => {
    const controller = new AbortController();
    setSummaryLoading(true);
    setSummary(null);
    setSummaryError("");
    api<Summary>(`/budgets/summary?period=${period}-01`, {
      signal: controller.signal,
    })
      .then(setSummary)
      .catch((error) => {
        if (
          error.name !== "AbortError" &&
          !(error instanceof ApiError && error.status === 404)
        )
          setSummaryError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setSummaryLoading(false);
      });
    return () => controller.abort();
  }, [api, period, refresh]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setData(null);
    api<Page<Expense>>(
      listPath(
        recurring,
        view === "overview" ? {} : filters,
        page,
        view === "overview" ? 5 : size,
        sort || `${recurring ? "startDate" : "expenseDate"},desc`,
      ),
      { signal: controller.signal },
    )
      .then((result) => {
        if (result.totalPages > 0 && page >= result.totalPages)
          setPage(result.totalPages - 1);
        else setData(result);
      })
      .catch((error) => {
        if (error.name !== "AbortError") setError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [api, view, recurring, filters, page, size, sort, refresh]);
  useEffect(() => {
    if (!toast) return;
    const timeout = setTimeout(() => setToast(""), 4000);
    return () => clearTimeout(timeout);
  }, [toast]);
  function navigate(next: View) {
    setView(next);
    setPage(0);
    setSort("");
    setFilters({ mode: "search" });
  }
  function changed(message: string) {
    setRefresh((value) => value + 1);
    setToast(message);
  }
  async function save(body: object) {
    const base =
      editor?.budget !== undefined
        ? "/budgets"
        : recurring
          ? "/recurring-expenses"
          : "/expenses";
    const id = editor?.budget?.id ?? editor?.item?.id;
    await api(`${base}${id ? `/${id}` : ""}`, {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(body),
    });
    changed("Changes saved.");
  }
  async function remove() {
    if (!deletion) return;
    setDeleting(true);
    setDeleteError("");
    try {
      await api(
        `/${deletion.budget ? "budgets" : recurring ? "recurring-expenses" : "expenses"}/${deletion.id}`,
        { method: "DELETE" },
      );
      setDeletion(null);
      changed("Deleted successfully.");
    } catch (error) {
      setDeleteError(
        error instanceof Error ? error.message : "Unable to delete.",
      );
    } finally {
      setDeleting(false);
    }
  }
  const nav = [
    { id: "overview" as View, name: "Overview", icon: LayoutDashboard },
    { id: "expenses" as View, name: "Expenses", icon: Wallet },
    { id: "recurring" as View, name: "Recurring payments", icon: Repeat2 },
  ];
  return (
    <div className="app-layout">
      <aside className="sidebar">
        <Brand />
        <span className="nav-label">YOUR WORKSPACE</span>
        <nav>
          {nav.map(({ id, name, icon: Icon }) => (
            <button
              key={id}
              className={view === id ? "selected" : ""}
              onClick={() => navigate(id)}
            >
              <Icon size={19} />
              {name}
              {view === id && <span className="nav-indicator" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-note">
          <div className="note-icon">↗</div>
          <strong>Manage your spending</strong>
          <p>Track expenses and review your monthly budget.</p>
        </div>
        <div className="profile">
          <span className="avatar">
            {session.username.charAt(0).toUpperCase()}
          </span>
          <div>
            <strong>{session.username}</strong>
            <small>Personal workspace</small>
          </div>
          <button
            className="icon-button"
            onClick={() => logout()}
            title="Log out"
            aria-label="Log out"
          >
            <LogOut size={18} />
          </button>
        </div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <span>
            Personal finance{" "}
            <span className="breadcrumb">
              / {nav.find((item) => item.id === view)?.name}
            </span>
          </span>
          <span className="topbar-label">
            <span className="status-dot" /> Expense tracking dashboard
          </span>
        </header>
        <div className="page-content">
          <div className="page-heading">
            <div>
              <span className="eyebrow">
                {view === "overview" ? "DASHBOARD" : "EXPENSE MANAGEMENT"}
              </span>
              <h1>
                {view === "overview"
                  ? "Spending overview"
                  : recurring
                    ? "Recurring payments"
                    : "Expenses"}
              </h1>
              <p>
                {view === "overview"
                  ? "Review your monthly spending and budget balance."
                  : recurring
                    ? "Manage scheduled payments and subscriptions."
                    : "View, search, and manage your recorded expenses."}
              </p>
            </div>
            <button
              className="primary"
              onClick={() => setEditor({})}
              disabled={!categories.length}
            >
              <Plus size={18} />
              {recurring ? "Add payment" : "Add expense"}
            </button>
          </div>
          {view === "overview" && (
            <>
              <div className="section-heading">
                <h2>Monthly snapshot</h2>
                <label className="month-picker">
                  <CalendarDays size={17} />
                  <span className="sr-only">Budget month</span>
                  <input
                    type="month"
                    value={period}
                    required
                    onChange={(event) => {
                      if (event.target.value) setPeriod(event.target.value);
                    }}
                  />
                </label>
              </div>
              {summaryError ? (
                <div className="error" role="alert">
                  {summaryError}
                  <button onClick={() => setRefresh((value) => value + 1)}>
                    Try again
                  </button>
                </div>
              ) : summaryLoading ? (
                <div className="panel state">
                  Loading your monthly snapshot…
                </div>
              ) : summary ? (
                <>
                  <div className="stats-grid">
                    <Stat
                      label="Monthly budget"
                      value={summary.amount}
                      hint="Your plan for the month"
                      icon={<Wallet size={18} />}
                    />
                    <Stat
                      label="Recorded spending"
                      value={summary.totalCurrentExpenses}
                      hint="Expenses dated this month"
                      icon={<ArrowUpRight size={18} />}
                    />
                    <Stat
                      label="Estimated spending"
                      value={summary.totalMonthlyExpenses}
                      hint="Including scheduled payments"
                      icon={<Repeat2 size={18} />}
                    />
                    <Stat
                      label="Estimated remaining"
                      value={summary.remainingMonthlyBudget}
                      hint="After scheduled payments"
                      icon={<ArrowDownLeft size={18} />}
                      accent
                    />
                  </div>
                  <div className="budget-panel panel">
                    <div className="budget-copy">
                      <span className="budget-icon">
                        <Wallet size={24} />
                      </span>
                      <div>
                        <h3>Monthly budget usage</h3>
                        <p>
                          {Number(summary.budgetPercentage) > 100
                            ? "Estimated spending exceeds your budget."
                            : "Estimated spending is within your budget."}
                        </p>
                      </div>
                    </div>
                    <div className="budget-progress">
                      <div>
                        <strong>
                          {money(summary.budgetPercentage)}%{" "}
                          <span>of budget estimated</span>
                        </strong>
                        <span>
                          {money(summary.remainingCurrentBudget)} left before
                          recurring
                        </span>
                      </div>
                      <progress
                        max={100}
                        value={Math.max(
                          0,
                          Math.min(100, summary.budgetPercentage),
                        )}
                        aria-label="Estimated budget usage"
                      />
                    </div>
                    <div className="budget-actions">
                      <button
                        className="text-button"
                        onClick={() => setEditor({ budget: summary })}
                      >
                        <Pencil size={14} /> Edit budget
                      </button>
                      <button
                        className="icon-button danger"
                        title="Delete budget"
                        aria-label="Delete budget"
                        onClick={() => {
                          setDeleteError("");
                          setDeletion({
                            id: summary.id,
                            title: "this monthly budget",
                            budget: true,
                          });
                        }}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="panel budget-empty">
                  <span className="budget-icon">
                    <Wallet size={26} />
                  </span>
                  <div>
                    <h3>No budget set for this month</h3>
                    <p>
                      Set a budget to see spending, scheduled payments, and
                      what’s left.
                    </p>
                  </div>
                  <button
                    className="secondary"
                    onClick={() => setEditor({ budget: null })}
                  >
                    <Plus size={16} /> Set budget
                  </button>
                </div>
              )}
              <div className="section-heading">
                <h2>
                  Recent expenses <span className="subtle-badge">Latest 5</span>
                </h2>
                <button
                  className="text-button"
                  onClick={() => navigate("expenses")}
                >
                  View all expenses <ArrowUpRight size={16} />
                </button>
              </div>
            </>
          )}
          {view !== "overview" && (
            <FilterBar
              key={view}
              recurring={recurring}
              categories={categories}
              apply={(values) => {
                setFilters(values);
                setPage(0);
              }}
            />
          )}
          <section
            className="panel expense-panel"
            aria-label={recurring ? "Recurring payments" : "Expenses"}
          >
            <div className="table-heading">
              <div>
                <h2>
                  {view === "overview"
                    ? "Latest expenses"
                    : recurring
                      ? "Your recurring payments"
                      : "Your expenses"}
                </h2>
                <span>
                  {loading
                    ? "Loading…"
                    : `${data?.totalElements ?? 0} ${recurring ? "payments" : "expenses"}`}{" "}
                  · Amounts in your chosen currency
                </span>
              </div>
              {view !== "overview" && (
                <label className="sort-control">
                  <span className="sr-only">Sort records</span>
                  <select
                    value={
                      sort || `${recurring ? "startDate" : "expenseDate"},desc`
                    }
                    onChange={(event) => {
                      setSort(event.target.value);
                      setPage(0);
                    }}
                  >
                    <option
                      value={`${recurring ? "startDate" : "expenseDate"},desc`}
                    >
                      Newest first
                    </option>
                    <option
                      value={`${recurring ? "startDate" : "expenseDate"},asc`}
                    >
                      Oldest first
                    </option>
                    <option value="amount,desc">Highest amount</option>
                    <option value="amount,asc">Lowest amount</option>
                    <option value="title,asc">Title A–Z</option>
                  </select>
                </label>
              )}
            </div>
            {error ? (
              <div className="state">
                <p role="alert">{error}</p>
                <button
                  className="secondary"
                  onClick={() => setRefresh((value) => value + 1)}
                >
                  <RefreshCw size={16} />
                  Try again
                </button>
              </div>
            ) : loading ? (
              <div className="state" role="status">
                Loading your {recurring ? "payments" : "expenses"}…
              </div>
            ) : !data?.content.length ? (
              <div className="state">
                <span className="empty-icon">
                  <Wallet size={28} />
                </span>
                <h3>
                  {data?.totalElements === 0 &&
                  Object.values(filters).filter(Boolean).length > 1
                    ? "No matching records"
                    : "No records yet"}
                </h3>
                <p>Add a record or adjust your search to get started.</p>
              </div>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>{recurring ? "Payment" : "Expense"}</th>
                      <th>Category</th>
                      <th>{recurring ? "Schedule" : "Date"}</th>
                      <th className="amount-cell">Amount</th>
                      <th>
                        <span className="sr-only">Actions</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <div className="expense-name">
                            <span
                              className={`category-icon tone-${item.categoryId % 4}`}
                            >
                              {recurring ? (
                                <Repeat2 size={18} />
                              ) : (
                                <Wallet size={18} />
                              )}
                            </span>
                            <div>
                              <strong>{item.title}</strong>
                              {item.description && (
                                <small>{item.description}</small>
                              )}
                            </div>
                          </div>
                        </td>
                        <td>
                          <span
                            className={`category-badge tone-${item.categoryId % 4}`}
                          >
                            {categories.find(
                              (category) => category.id === item.categoryId,
                            )?.categoryName || "Category"}
                          </span>
                        </td>
                        <td>
                          {recurring ? (
                            <>
                              <span className="frequency">
                                {item.frequency?.toLowerCase()}
                              </span>
                              <small className="date-range">
                                {dateLabel(item.startDate)} →{" "}
                                {dateLabel(item.endDate)}
                              </small>
                            </>
                          ) : (
                            dateLabel(item.expenseDate)
                          )}
                        </td>
                        <td className="amount-cell">
                          <strong>{money(item.amount)}</strong>
                          {recurring && (
                            <small className="date-range">per payment</small>
                          )}
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              className="icon-button"
                              title={`Edit ${item.title}`}
                              aria-label={`Edit ${item.title}`}
                              onClick={() => setEditor({ item })}
                            >
                              <Pencil size={16} />
                            </button>
                            <button
                              className="icon-button danger"
                              title={`Delete ${item.title}`}
                              aria-label={`Delete ${item.title}`}
                              onClick={() => {
                                setDeleteError("");
                                setDeletion({ id: item.id, title: item.title });
                              }}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {view !== "overview" && (
              <footer className="pagination">
                <label>
                  Rows per page
                  <select
                    value={size}
                    onChange={(event) => {
                      setSize(Number(event.target.value));
                      setPage(0);
                    }}
                  >
                    {[10, 20, 50].map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </label>
                <span>
                  Page {data?.totalPages ? page + 1 : 0} of{" "}
                  {data?.totalPages || 0}
                </span>
                <button
                  className="icon-button"
                  aria-label="Previous page"
                  disabled={loading || page === 0}
                  onClick={() => setPage((value) => value - 1)}
                >
                  <ChevronLeft size={18} />
                </button>
                <button
                  className="icon-button"
                  aria-label="Next page"
                  disabled={loading || !data || page + 1 >= data.totalPages}
                  onClick={() => setPage((value) => value + 1)}
                >
                  <ChevronRight size={18} />
                </button>
              </footer>
            )}
          </section>
          <footer className="page-footer">
            <span>Spendly · Personal expense tracker</span>
            <span>EXPENSES · PAYMENTS · BUDGETS</span>
          </footer>
        </div>
      </main>
      {toast && (
        <div className="toast" role="status">
          <Check size={18} />
          {toast}
          <button
            className="icon-button"
            aria-label="Dismiss notification"
            onClick={() => setToast("")}
          >
            <X size={15} />
          </button>
        </div>
      )}
      {editor && (
        <Editor
          item={editor.item}
          recurring={recurring}
          categories={categories}
          budget={editor.budget}
          period={period}
          close={() => setEditor(null)}
          save={save}
        />
      )}
      {deletion && (
        <Modal
          title="Delete this record?"
          close={() => {
            if (!deleting) setDeletion(null);
          }}
        >
          <p className="muted">
            “{deletion.title}” will be permanently removed.
          </p>
          {deleteError && (
            <div className="error" role="alert">
              {deleteError}
            </div>
          )}
          <div className="modal-actions">
            <button
              className="secondary"
              disabled={deleting}
              onClick={() => setDeletion(null)}
            >
              Keep record
            </button>
            <button
              className="delete-button"
              disabled={deleting}
              onClick={remove}
            >
              {deleting ? "Deleting…" : "Delete record"}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
function Stat({
  label,
  value,
  hint,
  icon,
  accent = false,
}: {
  label: string;
  value: number;
  hint: string;
  icon: React.ReactNode;
  accent?: boolean;
}) {
  return (
    <article className={`stat-card ${accent ? "accent" : ""}`}>
      <div>
        <span>{label}</span>
        {icon}
      </div>
      <strong className={value < 0 ? "negative" : ""}>{money(value)}</strong>
      <small>{hint}</small>
    </article>
  );
}
function FilterBar({
  recurring,
  categories,
  apply,
}: {
  recurring: boolean;
  categories: Category[];
  apply: (values: Record<string, string>) => void;
}) {
  const [mode, setMode] = useState("search");
  return (
    <form
      key={String(recurring)}
      className="panel filter-bar"
      onSubmit={(event) => {
        event.preventDefault();
        apply({
          ...(Object.fromEntries(new FormData(event.currentTarget)) as Record<
            string,
            string
          >),
          mode,
        });
      }}
    >
      <div className="filter-mode">
        <button
          type="button"
          className={mode === "search" ? "active" : ""}
          onClick={() => {
            setMode("search");
            apply({ mode: "search" });
          }}
        >
          Search
        </button>
        <button
          type="button"
          className={mode === "filter" ? "active" : ""}
          onClick={() => {
            setMode("filter");
            apply({ mode: "filter" });
          }}
        >
          Range filters
        </button>
      </div>
      <div className="filter-fields" key={mode}>
        {mode === "search" ? (
          <>
            <label className="search-field">
              <Search size={17} />
              <span className="sr-only">Search by title</span>
              <input
                name="title"
                placeholder="Search by title…"
                maxLength={100}
              />
            </label>
            {recurring ? (
              <label>
                <span className="sr-only">Frequency</span>
                <select name="frequency">
                  <option value="">All frequencies</option>
                  {["DAILY", "WEEKLY", "MONTHLY", "YEARLY"].map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </label>
            ) : (
              <label>
                <span className="sr-only">Exact date</span>
                <input name="expenseDate" type="date" title="Exact date" />
              </label>
            )}
          </>
        ) : (
          <>
            <label>
              Min amount
              <input
                name="minAmount"
                type="number"
                min="0"
                step="0.01"
                required={recurring}
                placeholder="0.00"
              />
            </label>
            <label>
              Max amount
              <input
                name="maxAmount"
                type="number"
                min="0"
                step="0.01"
                required={recurring}
                placeholder="No maximum"
              />
            </label>
            {!recurring && (
              <>
                <label>
                  From
                  <input name="start" type="date" />
                </label>
                <label>
                  To
                  <input name="end" type="date" />
                </label>
              </>
            )}
          </>
        )}
        {!(recurring && mode === "filter") && (
          <label>
            <span className="sr-only">Category</span>
            <select name="categoryId">
              <option value="">All categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.categoryName}
                </option>
              ))}
            </select>
          </label>
        )}
        <button className="primary" type="submit">
          Apply
        </button>
        <button
          className="text-button"
          type="reset"
          onClick={() => apply({ mode })}
        >
          Reset
        </button>
      </div>
    </form>
  );
}
