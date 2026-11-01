import { Link, useLocation } from "react-router-dom";
import { Clock, Coins, Lock, Sparkles, Zap } from "lucide-react";

import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import useUsage, { timeUntil } from "../useUsage";

/** Small ring showing the share of today's credits still available. */
const Ring = ({ value, max, className }) => {
  const r = 7;
  const c = 2 * Math.PI * r;
  const share = max > 0 ? Math.min(Math.max(value / max, 0), 1) : 0;
  return (
    <svg viewBox="0 0 18 18" className={cn("h-[18px] w-[18px] -rotate-90", className)} aria-hidden="true">
      <circle cx="9" cy="9" r={r} fill="none" stroke="currentColor" strokeOpacity=".18" strokeWidth="2.4" />
      <circle
        cx="9"
        cy="9"
        r={r}
        fill="none"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeDasharray={c}
        strokeDashoffset={c * (1 - share)}
        className="transition-[stroke-dashoffset] duration-500"
      />
    </svg>
  );
};

const Row = ({ icon: Icon, children }) => (
  <li className="flex items-center gap-2.5 text-sm">
    <Icon size={15} className="shrink-0 text-muted-foreground" />
    <span className="min-w-0">{children}</span>
  </li>
);

/**
 * Header pill with today's remaining credits; opens a small panel with usage details.
 * Works for guests and logged-in users (the server decides the numbers).
 */
const UsageWidget = ({ className }) => {
  const { usage, isAuthenticated, isError } = useUsage();
  const location = useLocation();

  if (!usage) {
    return isError ? null : <span className={cn("h-9 w-[4.5rem] animate-pulse rounded-xl bg-muted", className)} aria-hidden="true" />;
  }

  const { remaining, dailyCredits, used, jobsToday, resetsAt, userDaily, guestDaily, standardCost, aiCost } = usage;
  const low = remaining < Math.max(standardCost, 1);
  const pct = dailyCredits > 0 ? Math.round((used / dailyCredits) * 100) : 0;

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label={`${remaining} of ${dailyCredits} credits left today. Show usage`}
          className={cn(
            "flex h-9 items-center gap-1.5 rounded-xl border bg-background/60 px-2.5 text-sm font-semibold tabular-nums transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
            low ? "border-destructive/40 text-destructive" : "border-border text-primary",
            className
          )}
        >
          <Ring value={remaining} max={dailyCredits} />
          <span className="text-foreground">{remaining}</span>
          <span className="hidden font-normal text-muted-foreground sm:inline">/ {dailyCredits}</span>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" sideOffset={8} className="surface-solid w-[min(20rem,calc(100vw-2rem))] rounded-2xl p-4">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-semibold">Today's credits</p>
          <span
            className={cn(
              "rounded-full px-2 py-0.5 text-[11px] font-medium",
              isAuthenticated ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
            )}
          >
            {isAuthenticated ? "Free account" : "Guest"}
          </span>
        </div>

        <p className="mt-3 flex items-baseline gap-1.5">
          <span className="text-3xl font-bold tabular-nums">{remaining}</span>
          <span className="text-sm text-muted-foreground">of {dailyCredits} left</span>
        </p>
        <div
          className="mt-2 h-2 overflow-hidden rounded-full bg-muted"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={dailyCredits}
          aria-valuenow={used}
          aria-label="Credits used today"
        >
          <div
            className={cn("h-full rounded-full transition-[width] duration-500", low ? "bg-destructive" : "bg-brand-gradient")}
            style={{ width: `${Math.min(pct, 100)}%` }}
          />
        </div>

        <ul className="mt-4 space-y-2">
          <Row icon={Coins}>
            {used} used · {jobsToday} {jobsToday === 1 ? "job" : "jobs"} today
          </Row>
          <Row icon={Clock}>Resets in {timeUntil(resetsAt)}</Row>
          <Row icon={Zap}>
            Tools cost {standardCost} credit{standardCost === 1 ? "" : "s"} · AI tools {aiCost}
          </Row>
          {!isAuthenticated && (
            <Row icon={Lock}>
              <span className="text-muted-foreground">AI tools need a free account</span>
            </Row>
          )}
        </ul>

        {isAuthenticated ? (
          <Button asChild variant="outline" className="mt-4 h-9 w-full rounded-xl">
            <Link to="/credits">View credit history</Link>
          </Button>
        ) : (
          <div className="mt-4 rounded-xl bg-primary/8 p-3">
            <p className="flex items-start gap-2 text-sm">
              <Sparkles size={16} className="mt-0.5 shrink-0 text-primary" />
              <span>
                Log in free to get <strong>{userDaily} credits a day</strong> ({Math.round(userDaily / Math.max(guestDaily, 1))}× more) and unlock AI tools.
              </span>
            </p>
            <div className="mt-3 grid grid-cols-2 gap-2">
              <Button asChild variant="outline" className="h-9 rounded-xl">
                <Link to="/login" state={{ from: location.pathname }}>Log in</Link>
              </Button>
              <Button asChild className="h-9 rounded-xl">
                <Link to="/register">Sign up free</Link>
              </Button>
            </div>
          </div>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default UsageWidget;
