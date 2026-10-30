import { ACCOUNT_CURRENCIES, type UserProfile } from "./types";
const SESSION_KEY = "spendly.session";
export interface Session {
  token: string;
  username: string;
  expiresAt: number;
}
export function tokenExpiry(token: string): number {
  try {
    const segment = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(segment));
    return typeof exp === "number" ? exp * 1000 : 0;
  } catch {
    return 0;
  }
}
export function readSession(): Session | null {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_KEY) || "null");
    if (
      session?.token &&
      typeof session.username === "string" &&
      tokenExpiry(session.token) > Date.now()
    )
      return { ...session, expiresAt: tokenExpiry(session.token) };
    sessionStorage.removeItem(SESSION_KEY);
  } catch {
    /* An unavailable browser store starts a fresh session. */
  }
  return null;
}
export function saveSession(session: Session | null) {
  if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  else sessionStorage.removeItem(SESSION_KEY);
}
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}
export async function request<T>(
  path: string,
  token?: string,
  options: RequestInit = {},
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError")
      throw error;
    throw new ApiError(0, "Unable to reach the server. Please try again.");
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.message ||
        (response.status === 401
          ? "Please sign in again."
          : `Request failed (${response.status}). Please try again.`),
    );
  }
  if (response.status === 204 || response.headers.get("content-length") === "0")
    return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) : (undefined as T);
}
export function listPath(
  recurring: boolean,
  filters: Record<string, string>,
  page: number,
  size: number,
  sort: string,
) {
  const base = recurring ? "/recurring-expenses" : "/expenses";
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  params.append("sort", sort);
  params.append("sort", "id,desc");
  const mode = filters.mode || "search";
  const allowed =
    mode === "filter"
      ? recurring
        ? ["minAmount", "maxAmount"]
        : ["categoryId", "minAmount", "maxAmount", "start", "end"]
      : recurring
        ? ["title", "categoryId", "frequency"]
        : ["title", "categoryId", "expenseDate", "amount"];
  for (const key of allowed) if (filters[key]) params.set(key, filters[key]);
  return `${base}${allowed.some((key) => filters[key]) ? `/${mode}` : ""}?${params}`;
}

export async function getUserProfile(token: string, signal?: AbortSignal): Promise<UserProfile> {
  const profile = await request<UserProfile>("/users/profile", token, { signal });
  if (!profile || typeof profile.username !== "string" || !ACCOUNT_CURRENCIES.includes(profile.currency)) {
    throw new Error("Your account currency is missing or unsupported. Please contact support.");
  }
  return profile;
}
