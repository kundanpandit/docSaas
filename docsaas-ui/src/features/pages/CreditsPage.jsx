import { CalendarDays, Clock, Coins, Loader2, TrendingDown } from "lucide-react";

import usePageTitle from "@/hooks/usePageTitle";
import { getToolById } from "@/features/home/constants/tools";
import { useGetCreditsQuery } from "@/features/account/api/accountApi";
import { cn } from "@/lib/utils";
import { timeUntil } from "@/features/usage/useUsage";

const TYPE_LABEL = {
  JOB_CHARGE: "Tool used",
  JOB_REFUND: "Refund (job failed)",
};

const formatWhen = (iso) =>
  new Date(iso).toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });

const Stat = ({ icon: Icon, label, value }) => (
  <div className="glass rounded-2xl p-4">
    <div className="flex items-center gap-2 text-sm text-muted-foreground">
      <Icon size={15} />
      {label}
    </div>
    <p className="mt-2 text-2xl font-semibold tabular-nums">{value}</p>
  </div>
);

/** Credit balance and ledger (FR-023). The server is the authority; this page only displays it. */
const CreditsPage = () => {
  usePageTitle("Credits");
  const { data, isLoading, isError } = useGetCreditsQuery(undefined, { refetchOnMountOrArgChange: true });

  // Group tools by cost for a compact price list
  const costGroups = data
    ? Object.entries(data.costs).reduce((groups, [toolId, cost]) => {
        const tool = getToolById(toolId);
        if (tool?.available) (groups[cost] ??= []).push(tool.title);
        return groups;
      }, {})
    : {};

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Credits</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {data?.enforced
            ? `You get ${data.dailyCredits} credits every day, reset at midnight. Failed jobs don't count.`
            : "Credits are tracked for your account. During launch, tools stay free even if your balance runs out."}
        </p>
      </div>

      {isError && <p className="glass rounded-2xl p-4 text-sm text-destructive">Could not load your credits. Please refresh.</p>}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat icon={Coins} label="Left today" value={data ? `${data.balance} / ${data.dailyCredits}` : "–"} />
        <Stat icon={TrendingDown} label="Used today" value={data?.usedToday ?? "–"} />
        <Stat icon={CalendarDays} label="Used this month" value={data?.usedThisMonth ?? "–"} />
        <Stat icon={Clock} label="Resets in" value={data ? timeUntil(data.resetsAt) : "–"} />
      </div>

      {data && (
        <section className="glass rounded-3xl p-6">
          <h2 className="font-semibold">What tools cost</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {Object.entries(costGroups)
              .sort(([a], [b]) => a - b)
              .map(([cost, titles]) => (
                <li key={cost}>
                  <span className="font-medium">
                    {cost} {cost === "1" ? "credit" : "credits"}:
                  </span>{" "}
                  <span className="text-muted-foreground">{titles.join(", ")}</span>
                </li>
              ))}
          </ul>
        </section>
      )}

      <section className="glass rounded-3xl p-3 sm:p-4">
        <h2 className="px-2 pb-2 font-semibold">History</h2>
        {isLoading && <Loader2 className="mx-auto my-6 animate-spin text-muted-foreground" />}
        {data?.recent?.length === 0 && <p className="px-2 py-4 text-sm text-muted-foreground">No activity yet.</p>}
        <ul className="divide-y">
          {data?.recent?.map((entry, i) => (
            <li key={i} className="flex items-center justify-between gap-3 px-2 py-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">
                  {TYPE_LABEL[entry.type] ?? entry.type}
                  {entry.tool && <span className="font-normal text-muted-foreground"> · {getToolById(entry.tool)?.title ?? entry.tool}</span>}
                </p>
                <p className="text-xs text-muted-foreground">{formatWhen(entry.createdAt)}</p>
              </div>
              <span className={cn("shrink-0 font-semibold tabular-nums", entry.delta > 0 ? "text-success" : "text-foreground")}>
                {entry.delta > 0 ? `+${entry.delta}` : entry.delta}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
};

export default CreditsPage;
