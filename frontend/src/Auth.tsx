import { useState, type FormEvent } from "react";
import {
  ArrowRight,
  ChartNoAxesCombined,
  ShieldCheck,
  Wallet,
} from "lucide-react";
import { request, saveSession, tokenExpiry, type Session } from "./api";
export function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark">
        S<span>↗</span>
      </span>
      spendly<span className="brand-dot">.</span>
    </div>
  );
}
export default function Auth({
  onLogin,
  notice,
}: {
  onLogin: (session: Session) => void;
  notice: string;
}) {
  const [register, setRegister] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setBusy(true);
    const fields = Object.fromEntries(
      new FormData(event.currentTarget),
    ) as Record<string, string>;
    try {
      if (register) {
        if (new TextEncoder().encode(fields.password).length > 72)
          throw new Error("Password must be at most 72 bytes.");
        await request("/auth/register", undefined, {
          method: "POST",
          body: JSON.stringify(fields),
        });
        setRegister(false);
        setSuccess("Account created. Sign in to get started.");
      } else {
        const { token } = await request<{ token: string }>(
          "/auth/login",
          undefined,
          { method: "POST", body: JSON.stringify(fields) },
        );
        const expiresAt = tokenExpiry(token);
        if (expiresAt <= Date.now())
          throw new Error("The server returned an expired or invalid session.");
        const session = { token, username: fields.username, expiresAt };
        saveSession(session);
        onLogin(session);
      }
    } catch (error) {
      setError(
        error instanceof Error ? error.message : "Something went wrong.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-layout">
      <aside className="auth-story">
        <Brand />
        <div>
          <span className="eyebrow light">PERSONAL EXPENSE TRACKER</span>
          <h1>
            A clearer view
            <br />
            of your spending.
          </h1>
          <p>
            Manage expenses, recurring payments, and monthly budgets in one
            dashboard.
          </p>
          <div className="story-features">
            <span>
              <Wallet size={18} /> Track your spending
            </span>
            <span>
              <ChartNoAxesCombined size={18} /> Monitor your budget
            </span>
          </div>
        </div>
        <small>Expenses · Recurring payments · Budgets</small>
      </aside>
      <main className="auth-main">
        <div className="auth-card">
          <span className="eyebrow">YOUR SPENDLY ACCOUNT</span>
          <h2>{register ? "Create an account" : "Sign in to Spendly"}</h2>
          <p>
            {register
              ? "Enter your details to start tracking expenses."
              : "Access your expenses and monthly budgets."}
          </p>
          <div className="auth-tabs">
            <button
              className={!register ? "active" : ""}
              onClick={() => {
                setRegister(false);
                setError("");
              }}
            >
              Sign in
            </button>
            <button
              className={register ? "active" : ""}
              onClick={() => {
                setRegister(true);
                setError("");
              }}
            >
              Create account
            </button>
          </div>
          {(notice || success) && (
            <div className="notice" role="status">
              {success || notice}
            </div>
          )}
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              {register && (
                <div className="form-grid">
                  <label>
                    First name
                    <input
                      name="firstName"
                      required
                      maxLength={50}
                      autoComplete="given-name"
                    />
                  </label>
                  <label>
                    Last name
                    <input
                      name="lastName"
                      required
                      maxLength={50}
                      autoComplete="family-name"
                    />
                  </label>
                </div>
              )}
              <label>
                Username
                <input
                  name="username"
                  placeholder="Your username"
                  required
                  maxLength={50}
                  autoComplete="username"
                />
              </label>
              {register && (
                <label>
                  Email address
                  <input
                    name="email"
                    type="email"
                    placeholder="you@example.com"
                    required
                    maxLength={100}
                    autoComplete="email"
                  />
                </label>
              )}
              <label>
                Password
                <input
                  name="password"
                  type="password"
                  placeholder="At least 8 characters"
                  required
                  minLength={8}
                  maxLength={255}
                  autoComplete={register ? "new-password" : "current-password"}
                />
              </label>
              <button className="primary full" type="submit">
                {busy ? "One moment…" : register ? "Create account" : "Sign in"}
                <ArrowRight size={18} />
              </button>
            </fieldset>
          </form>
          <div className="auth-foot">
            <ShieldCheck size={16} /> Your expense data is linked to your
            account.
          </div>
        </div>
        <small className="auth-copyright">
          SPENDLY · PERSONAL EXPENSE TRACKER
        </small>
      </main>
    </div>
  );
}
