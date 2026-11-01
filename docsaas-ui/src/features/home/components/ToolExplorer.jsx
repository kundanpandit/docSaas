import { useMemo, useState } from "react";
import { AnimatePresence, LayoutGroup, motion } from "framer-motion";
import { Search, X } from "lucide-react";

import Container from "@/components/common/Container";
import SectionHeading from "@/components/common/SectionHeading";
import { ALL_TOOLS, TOOL_CATEGORIES } from "@/features/home/constants/tools";
import { cn } from "@/lib/utils";
import ToolCard from "./ToolCard";

const TABS = [{ id: "all", label: "All tools" }, ...TOOL_CATEGORIES.map(({ id, label }) => ({ id, label }))];

const ToolExplorer = () => {
  const [tab, setTab] = useState("all");
  const [query, setQuery] = useState("");

  const tools = useMemo(() => {
    const q = query.trim().toLowerCase();
    return ALL_TOOLS
      .filter((t) => tab === "all" || t.category === tab)
      .filter((t) => !q || `${t.title} ${t.description}`.toLowerCase().includes(q))
      // working tools first
      .sort((a, b) => Number(b.available) - Number(a.available));
  }, [tab, query]);

  return (
    <section id="tools" className="py-16 md:py-24">
      <Container>
        <SectionHeading
          eyebrow="Tools"
          title="Pick a tool and get it done"
          description="Everything runs on our servers, so it works on any device. No installs, no watermarks."
        />

        <div className="mt-10 flex flex-col items-center gap-4 md:flex-row md:justify-between">
          <LayoutGroup>
            <div role="tablist" aria-label="Tool categories" className="glass flex max-w-full gap-1 overflow-x-auto rounded-2xl p-1">
              {TABS.map((t) => (
                <button
                  key={t.id}
                  role="tab"
                  aria-selected={tab === t.id}
                  onClick={() => setTab(t.id)}
                  className={cn(
                    "relative whitespace-nowrap rounded-xl px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                    tab === t.id ? "text-primary-foreground" : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {tab === t.id && (
                    <motion.span
                      layoutId="tool-tab"
                      className="bg-brand-gradient absolute inset-0 rounded-xl shadow-[0_6px_16px_-6px_var(--primary)]"
                      transition={{ type: "spring", stiffness: 400, damping: 32 }}
                    />
                  )}
                  <span className="relative">{t.label}</span>
                </button>
              ))}
            </div>
          </LayoutGroup>

          <label className="glass flex h-11 w-full items-center gap-2 rounded-2xl px-4 focus-within:ring-3 focus-within:ring-ring/40 md:w-72">
            <Search size={16} className="shrink-0 text-muted-foreground" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search tools…"
              aria-label="Search tools"
              className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
            {query && (
              <button type="button" onClick={() => setQuery("")} aria-label="Clear search" className="text-muted-foreground hover:text-foreground">
                <X size={15} />
              </button>
            )}
          </label>
        </div>

        <motion.div layout className="mt-8 grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-4">
          <AnimatePresence mode="popLayout">
            {tools.map((tool) => (
              <motion.div
                key={tool.id}
                layout
                initial={{ opacity: 0, scale: 0.94 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.94 }}
                transition={{ duration: 0.22 }}
              >
                <ToolCard {...tool} />
              </motion.div>
            ))}
          </AnimatePresence>
        </motion.div>

        {tools.length === 0 && (
          <p className="mt-10 text-center text-muted-foreground">
            No tools match “{query}”. Try “PDF” or “image”.
          </p>
        )}
      </Container>
    </section>
  );
};

export default ToolExplorer;
