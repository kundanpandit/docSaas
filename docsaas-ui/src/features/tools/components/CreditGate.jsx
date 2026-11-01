import { Link, useLocation } from "react-router-dom";
import { Clock, Coins, Lock, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import useUsage, { timeUntil } from "@/features/usage/useUsage";

const Shell = ({ icon: Icon, tone = "primary", children }) => (
  <div className="glass rounded-3xl px-6 py-12 text-center md:px-12 md:py-14">
    <span
      className={
        tone === "primary"
          ? "bg-brand-gradient mx-auto flex h-14 w-14 items-center justify-center rounded-2xl text-white shadow-[0_12px_30px_-10px_var(--primary)]"
          : "mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground"
      }
    >
      <Icon size={26} />
    </span>
    {children}
  </div>
);

const LoginButtons = () => {
  const location = useLocation();
  return (
    <div className="mx-auto mt-7 grid max-w-sm grid-cols-2 gap-2">
      <Button asChild variant="outline" size="lg" className="h-11 rounded-xl">
        <Link to="/login" state={{ from: location.pathname }}>Log in</Link>
      </Button>
      <Button asChild size="lg" className="h-11 rounded-xl">
        <Link to="/register">Sign up free</Link>
      </Button>
    </div>
  );
};

/** AI tools are for logged-in users only. */
export const LoginRequired = ({ tool }) => {
  const { usage } = useUsage();
  const daily = usage?.userDaily ?? 30;
  return (
    <Shell icon={Lock}>
      <h2 className="mt-5 text-2xl font-semibold">Log in to use {tool.title}</h2>
      <p className="mx-auto mt-2 max-w-md text-muted-foreground">
        AI tools are included free with an account. You also get <strong className="text-foreground">{daily} credits a day</strong>{" "}
        instead of {usage?.guestDaily ?? 3}, and your job history.
      </p>
      <LoginButtons />
    </Shell>
  );
};

/** Shown when the server rejects a job because today's credits are used up. */
export const OutOfCredits = ({ message, onReset }) => {
  const { usage, isAuthenticated } = useUsage();
  return (
    <Shell icon={Coins} tone="muted">
      <h2 className="mt-5 text-2xl font-semibold">{isAuthenticated ? "You've used today's credits" : "Guest credits used up"}</h2>
      <p className="mx-auto mt-2 max-w-md text-muted-foreground">{message}</p>
      {usage?.resetsAt && (
        <p className="mt-3 flex items-center justify-center gap-1.5 text-sm text-muted-foreground">
          <Clock size={14} /> Credits reset in {timeUntil(usage.resetsAt)}
        </p>
      )}
      {isAuthenticated ? (
        <div className="mt-7">
          <Button variant="outline" size="lg" className="h-11 rounded-xl" onClick={onReset}>
            Back to the tool
          </Button>
        </div>
      ) : (
        <>
          <p className="mx-auto mt-4 inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1.5 text-sm font-medium text-primary">
            <Sparkles size={14} /> Free account: {usage?.userDaily ?? 30} credits a day + AI tools
          </p>
          <LoginButtons />
        </>
      )}
    </Shell>
  );
};

/** One line under the action button: what this run costs and what's left. */
export const CreditHint = ({ cost }) => {
  const { usage } = useUsage();
  if (!usage || !usage.enforced) return null;
  const enough = usage.remaining >= cost;
  return (
    <p className={enough ? "text-center text-xs text-muted-foreground" : "text-center text-xs font-medium text-destructive"}>
      Uses {cost} {cost === 1 ? "credit" : "credits"} · {usage.remaining} of {usage.dailyCredits} left today
    </p>
  );
};
