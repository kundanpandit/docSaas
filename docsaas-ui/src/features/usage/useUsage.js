import { useSelector } from "react-redux";

import { selectIsAuthenticated, selectUser } from "@/features/auth/selectors/authSelectors";
import { useGetUsageQuery } from "./api/usageApi";

/** Today's usage for whoever is browsing. Cached per identity, refreshed every minute and on focus. */
const useUsage = () => {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const user = useSelector(selectUser);
  const key = isAuthenticated ? `user:${user?.id ?? "me"}` : "guest";
  const query = useGetUsageQuery(key, { pollingInterval: 60_000, refetchOnMountOrArgChange: 30 });
  return { ...query, usage: query.data, isAuthenticated };
};

export default useUsage;

/** "5h 12m" / "18m" until the given ISO time. */
export const timeUntil = (iso, now = Date.now()) => {
  const ms = new Date(iso).getTime() - now;
  if (!Number.isFinite(ms) || ms <= 0) return "a moment";
  const minutes = Math.ceil(ms / 60_000);
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h}h ${m}m` : `${m}m`;
};

/** API error codes that mean "out of credits" or "log in first" rather than a processing failure. */
export const CREDIT_ERRORS = ["INSUFFICIENT_CREDITS", "LOGIN_REQUIRED"];
