import { describe, expect, it, vi, afterEach } from "vitest";
import { currencyFormatter } from "./currency";
import { getUserProfile } from "./api";
import { ACCOUNT_CURRENCIES } from "./types";

afterEach(() => vi.unstubAllGlobals());

describe("account currency", () => {
  it.each(ACCOUNT_CURRENCIES)("displays %s without losing stored decimal precision", (currency) => {
    const parts = currencyFormatter(currency, "en-US").formatToParts(1234.56);
    expect(parts.find((part) => part.type === "currency")?.value).toBe(currency);
    expect(parts.find((part) => part.type === "fraction")?.value).toBe("56");
  });

  it("loads the authenticated profile from the backend's profile route", async () => {
    const profile = { username: "alice", currency: "CHF" };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(profile)));
    vi.stubGlobal("fetch", fetchMock);
    const signal = new AbortController().signal;
    await expect(getUserProfile("jwt", signal)).resolves.toEqual(profile);
    expect(fetchMock).toHaveBeenCalledWith("/api/users/profile", expect.objectContaining({
      signal,
      headers: { Authorization: "Bearer jwt" },
    }));
  });

  it.each([null, { username: "alice" }, { username: "alice", currency: "XXX" }])(
    "rejects invalid profiles instead of assuming a currency",
    async (profile) => {
      vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(profile))));
      await expect(getUserProfile("jwt")).rejects.toThrow("currency is missing or unsupported");
    },
  );

  it("preserves unauthorized profile errors for session logout", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    await expect(getUserProfile("expired")).rejects.toMatchObject({ status: 401 });
  });
});
