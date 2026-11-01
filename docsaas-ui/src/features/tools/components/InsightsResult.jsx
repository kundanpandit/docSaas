import { useEffect, useState } from "react";
import { AlertTriangle, BadgeCheck, Loader2 } from "lucide-react";

import { getErrorMessage } from "@/services/api/apiClient";
import { fetchJobText } from "../api/toolsApi";

const SECTIONS = [
  { key: "keyPoints", title: "Key points", main: (it) => it.text },
  { key: "dates", title: "Dates", main: (it) => it.date, sub: (it) => it.description },
  {
    key: "entities",
    title: "People & organizations",
    main: (it) => it.name,
    sub: (it) => [it.type !== "other" ? it.type : null, it.role].filter(Boolean).join(" · "),
  },
  { key: "figures", title: "Figures", main: (it) => it.value, sub: (it) => it.description },
  {
    key: "actionItems",
    title: "Action items",
    main: (it) => it.text,
    sub: (it) => [it.owner && `Owner: ${it.owner}`, it.due && `Due: ${it.due}`].filter(Boolean).join(" · "),
  },
];

/** Verified = the quoted text was found on the cited page by the server, not just claimed by the AI. */
const Evidence = ({ item }) => (
  <span className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
    {item.verified ? (
      <span className="inline-flex items-center gap-1 text-success">
        <BadgeCheck className="h-3.5 w-3.5" /> Verified
      </span>
    ) : (
      <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400">
        <AlertTriangle className="h-3.5 w-3.5" /> Unverified
      </span>
    )}
    {item.page && <span>p. {item.page}</span>}
    {item.quote && <q className="italic">{item.quote}</q>}
  </span>
);

const Section = ({ section, items }) => {
  if (!items?.length) return null;
  return (
    <section className="mt-5">
      <h3 className="text-sm font-semibold">
        {section.title} <span className="font-normal text-muted-foreground">({items.length})</span>
      </h3>
      <ul className="mt-2 space-y-2.5">
        {items.map((item, i) => {
          const sub = section.sub?.(item);
          return (
            <li key={i} className="rounded-xl border bg-background/50 px-3 py-2">
              <span className="block text-sm font-medium">{section.main(item)}</span>
              {sub && <span className="block text-sm text-muted-foreground">{sub}</span>}
              <Evidence item={item} />
            </li>
          );
        })}
      </ul>
    </section>
  );
};

/** Structured AI insights (document-insights tool), rendered from the result JSON. */
const InsightsResult = ({ job }) => {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    fetchJobText(job.id)
      .then((text) => active && setData(typeof text === "string" ? JSON.parse(text) : text))
      .catch(async (err) => active && setError(await getErrorMessage(err, "Could not load the insights.")));
    return () => {
      active = false;
    };
  }, [job.id]);

  const empty = data && SECTIONS.every((s) => !data[s.key]?.length);

  return (
    <div className="relative mt-6 text-left">
      <div className="max-h-[32rem] overflow-y-auto rounded-2xl border bg-background/60 p-4">
        {error && <p className="text-sm text-destructive">{error}</p>}
        {!error && !data && <Loader2 className="mx-auto animate-spin text-muted-foreground" />}
        {data && (
          <>
            {data.documentType && (
              <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium capitalize text-primary">
                {data.documentType}
              </span>
            )}
            {data.overview && <p className="mt-2 text-sm leading-relaxed">{data.overview}</p>}
            {data.topics?.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-1.5">
                {data.topics.map((topic) => (
                  <span key={topic} className="rounded-full border px-2.5 py-0.5 text-xs">
                    {topic}
                  </span>
                ))}
              </div>
            )}
            {SECTIONS.map((section) => (
              <Section key={section.key} section={section} items={data[section.key]} />
            ))}
            {empty && <p className="mt-4 text-sm text-muted-foreground">No specific insights were found in this document.</p>}
            {data.notice && <p className="mt-5 text-xs italic text-muted-foreground">{data.notice}</p>}
          </>
        )}
      </div>
    </div>
  );
};

export default InsightsResult;
