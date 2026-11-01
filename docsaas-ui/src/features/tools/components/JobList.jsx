import { useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Download, FileQuestion, Inbox, Loader2 } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { getToolById } from "@/features/home/constants/tools";
import { getErrorMessage } from "@/services/api/apiClient";
import { cn } from "@/lib/utils";
import { downloadJobResult } from "../api/toolsApi";
import { formatBytes } from "../utils/files";

const STATUS = {
  SUCCEEDED: { label: "Ready", className: "bg-success/12 text-success" },
  FAILED: { label: "Failed", className: "bg-destructive/12 text-destructive" },
  EXPIRED: { label: "Expired", className: "bg-muted text-muted-foreground" },
  PROCESSING: { label: "Processing", className: "bg-primary/12 text-primary" },
  QUEUED: { label: "Queued", className: "bg-primary/12 text-primary" },
};

const formatWhen = (iso) =>
  new Date(iso).toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });

const JobRow = ({ job, index }) => {
  const [downloading, setDownloading] = useState(false);
  const tool = getToolById(job.tool);
  const Icon = tool?.icon ?? FileQuestion;
  const status = STATUS[job.status] ?? STATUS.QUEUED;

  const download = async () => {
    setDownloading(true);
    try {
      await downloadJobResult(job);
    } catch (err) {
      toast.error(await getErrorMessage(err, "Download failed"));
    } finally {
      setDownloading(false);
    }
  };

  return (
    <motion.li
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index * 0.04, 0.4) }}
      className="flex items-center gap-3 rounded-2xl px-3 py-3 transition-colors hover:bg-accent/50"
    >
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/12 text-primary">
        <Icon size={18} />
      </span>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">
          {tool?.title ?? job.tool}
          {job.output?.fileName && <span className="font-normal text-muted-foreground"> · {job.output.fileName}</span>}
        </p>
        <p className="truncate text-xs text-muted-foreground">
          {formatWhen(job.createdAt)}
          {job.status === "FAILED" && job.errorMessage ? ` · ${job.errorMessage}` : ""}
          {job.status === "SUCCEEDED" && job.output ? ` · ${formatBytes(job.output.sizeBytes)}` : ""}
        </p>
      </div>

      <span className={cn("hidden shrink-0 rounded-full px-2.5 py-1 text-xs font-medium sm:inline", status.className)}>
        {status.label}
      </span>

      {job.status === "SUCCEEDED" && (
        <Button size="icon" variant="ghost" onClick={download} disabled={downloading} aria-label={`Download ${job.output?.fileName ?? "result"}`} className="shrink-0 rounded-xl">
          {downloading ? <Loader2 className="animate-spin" /> : <Download />}
        </Button>
      )}
    </motion.li>
  );
};

const JobList = ({ jobs, isLoading, isError, emptyAction = true }) => {
  if (isLoading) {
    return (
      <div className="space-y-2 p-1">
        {[0, 1, 2].map((i) => (
          <div key={i} className="h-14 animate-pulse rounded-2xl bg-foreground/5" />
        ))}
      </div>
    );
  }

  if (isError) {
    return <p className="p-6 text-center text-sm text-muted-foreground">Couldn't load your jobs. Please refresh.</p>;
  }

  if (!jobs?.length) {
    return (
      <div className="flex flex-col items-center px-6 py-12 text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
          <Inbox size={22} />
        </span>
        <p className="mt-4 font-medium">No jobs yet</p>
        <p className="mt-1 text-sm text-muted-foreground">Files you process while logged in show up here.</p>
        {emptyAction && (
          <Button asChild className="mt-5 rounded-xl">
            <Link to="/#tools">Try a tool</Link>
          </Button>
        )}
      </div>
    );
  }

  return (
    <ul className="divide-y divide-border/60">
      {jobs.map((job, index) => (
        <JobRow key={job.id} job={job} index={index} />
      ))}
    </ul>
  );
};

export default JobList;
