import { motion } from "framer-motion";
import { AlertCircle, Check, Clock, Download, Loader2, Pencil, RotateCcw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { RETENTION_MINUTES } from "@/features/home/constants/tools";
import { formatBytes } from "../utils/files";
import TextResult from "./TextResult";
import InsightsResult from "./InsightsResult";
import ResultPreview from "../preview/ResultPreview";
import { canPreviewResult } from "../preview/canPreview";

const Panel = ({ children }) => (
  <motion.div
    role="status"
    aria-live="polite"
    initial={{ opacity: 0, y: 12, scale: 0.98 }}
    animate={{ opacity: 1, y: 0, scale: 1 }}
    transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
    className="glass relative overflow-hidden rounded-3xl px-6 py-12 text-center md:py-14"
  >
    {children}
  </motion.div>
);

const ProgressRing = ({ value }) => {
  const r = 34;
  const c = 2 * Math.PI * r;
  return (
    <svg width="88" height="88" viewBox="0 0 88 88" className="mx-auto -rotate-90" aria-hidden="true">
      <circle cx="44" cy="44" r={r} fill="none" strokeWidth="6" className="stroke-foreground/10" />
      <motion.circle
        cx="44"
        cy="44"
        r={r}
        fill="none"
        strokeWidth="6"
        strokeLinecap="round"
        stroke="var(--primary)"
        strokeDasharray={c}
        animate={{ strokeDashoffset: c - (c * value) / 100 }}
        transition={{ duration: 0.3 }}
      />
    </svg>
  );
};

/**
 * phase: "uploading" | "processing" | "succeeded" | "failed" | "expired"
 * Success is only shown after the backend reports SUCCEEDED (FR-006).
 */
/** onBack (optional): return to the previous step with the work kept (e.g. "Keep editing"). */
const JobStatusPanel = ({ tool, phase, progress, job, error, downloading, onDownload, onRetry, onReset, onBack, backLabel = "Back" }) => {
  if (phase === "uploading") {
    return (
      <Panel>
        <div className="relative mx-auto w-fit">
          <ProgressRing value={progress} />
          <span className="absolute inset-0 flex items-center justify-center text-sm font-semibold">{progress}%</span>
        </div>
        <h2 className="mt-5 text-xl font-semibold">Uploading…</h2>
        <p className="mt-1 text-sm text-muted-foreground">Keep this tab open.</p>
      </Panel>
    );
  }

  if (phase === "processing") {
    return (
      <Panel>
        <div className="relative mx-auto flex h-20 w-20 items-center justify-center">
          <span className="bg-brand-gradient absolute inset-0 animate-ping rounded-full opacity-20" />
          <span className="bg-brand-gradient relative flex h-16 w-16 items-center justify-center rounded-full text-white shadow-[0_12px_30px_-10px_var(--primary)]">
            <Loader2 size={28} className="animate-spin" />
          </span>
        </div>
        <h2 className="mt-5 text-xl font-semibold">Working on it…</h2>
        <p className="mt-1 text-sm text-muted-foreground">This usually takes a few seconds.</p>
      </Panel>
    );
  }

  if (phase === "succeeded") {
    return (
      <Panel>
        <div aria-hidden="true" className="absolute -top-24 left-1/2 h-48 w-2/3 -translate-x-1/2 rounded-full bg-success/25 blur-3xl" />
        <motion.span
          initial={{ scale: 0, rotate: -30 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ type: "spring", stiffness: 260, damping: 16, delay: 0.1 }}
          className="relative mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-success text-white shadow-[0_12px_30px_-10px_var(--success)]"
        >
          <Check size={32} strokeWidth={3} />
        </motion.span>

        <h2 className="relative mt-5 text-2xl font-semibold">Your file is ready</h2>
        {job?.resultSummary && (
          <p className="relative mx-auto mt-2 max-w-md text-pretty text-muted-foreground">{job.resultSummary}</p>
        )}

        {tool?.resultView === "text" && job && <TextResult job={job} />}
        {tool?.resultView === "insights" && job && <InsightsResult job={job} />}

        <Button
          size="lg"
          onClick={onDownload}
          disabled={downloading}
          className="relative mt-8 h-13 rounded-xl px-8 text-base shadow-[0_12px_30px_-10px_var(--primary)] transition-transform hover:-translate-y-0.5"
        >
          {downloading ? <Loader2 className="animate-spin" /> : <Download />}
          Download {job?.output?.fileName?.endsWith(".zip") ? "ZIP" : "file"}
        </Button>

        {job?.output && (
          <p className="relative mt-3 text-sm text-muted-foreground">
            {job.output.fileName} · {formatBytes(job.output.sizeBytes)}
          </p>
        )}

        {!tool?.resultView && canPreviewResult(job) && <ResultPreview job={job} />}

        <div className="relative mt-8 flex flex-col items-center gap-1">
          <div className="flex flex-wrap justify-center gap-2">
            {onBack && (
              <Button variant="outline" onClick={onBack} className="rounded-xl">
                <Pencil />
                {backLabel}
              </Button>
            )}
            <Button variant="ghost" onClick={onReset} className="rounded-xl">
              <RotateCcw />
              Start over
            </Button>
          </div>
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Clock size={13} />
            Deleted automatically after {RETENTION_MINUTES} minutes
          </p>
        </div>
      </Panel>
    );
  }

  return (
    <Panel>
      <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-destructive/12 text-destructive">
        <AlertCircle size={30} />
      </span>
      <h2 className="mt-5 text-xl font-semibold">
        {phase === "expired" ? "This file has expired" : "Something went wrong"}
      </h2>
      <p className="mx-auto mt-2 max-w-md text-pretty text-muted-foreground">{error}</p>

      <div className="mt-8 flex flex-wrap justify-center gap-2">
        {phase === "failed" && onRetry && (
          <Button size="lg" onClick={onRetry} className="h-11 rounded-xl px-5">
            <RotateCcw />
            Try again
          </Button>
        )}
        {onBack && phase === "failed" && (
          <Button size="lg" variant="outline" onClick={onBack} className="h-11 rounded-xl px-5">
            <Pencil />
            {backLabel}
          </Button>
        )}
        <Button size="lg" variant="outline" onClick={onReset} className="h-11 rounded-xl px-5">
          Start over
        </Button>
      </div>
    </Panel>
  );
};

export default JobStatusPanel;
