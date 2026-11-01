import { lazy, Suspense, useDeferredValue, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { ArrowRight, Info, ShieldCheck, Sparkles, Wrench } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import ToolLayout from "@/components/layout/ToolLayout";
import NotFoundPage from "@/components/common/NotFoundPage";
import usePageTitle from "@/hooks/usePageTitle";
import { ALL_TOOLS, RETENTION_MINUTES, getToolById } from "@/features/home/constants/tools";
import { getErrorMessage } from "@/services/api/apiClient";

import FileDropzone from "../components/FileDropzone";
import FileList from "../components/FileList";
import ToolOptions from "../components/ToolOptions";
import JobStatusPanel from "../components/JobStatusPanel";
import ChatPanel from "../components/ChatPanel";
import DocumentPreview from "../preview/DocumentPreview";
import { CreditHint, LoginRequired, OutOfCredits } from "../components/CreditGate";
import { selectIsAuthenticated } from "@/features/auth/selectors/authSelectors";
import { refreshUsage } from "@/features/usage/api/usageApi";
import useUsage, { CREDIT_ERRORS } from "@/features/usage/useUsage";

// The editor is large and only needed on its own page
const PdfEditor = lazy(() => import("@/features/editor/PdfEditor"));
import { isImage, isPdf } from "@/lib/pdf";
import { downloadJobResult, startToolJob, toolsApi, useGetJobQuery, useGetToolStatusQuery } from "../api/toolsApi";
import { validateFiles } from "../utils/files";

const TERMINAL = ["SUCCEEDED", "FAILED", "EXPIRED"];

const defaultOptions = (tool) =>
  Object.fromEntries(tool.options.map((option) => [option.name, option.default]));

const visibleOptions = (tool, values) =>
  tool.options.filter((option) => !option.showIf || option.showIf(values));

const ToolWorkspace = ({ tool }) => {
  const [files, setFiles] = useState([]);
  const [options, setOptions] = useState(() => defaultOptions(tool));
  const [optionErrors, setOptionErrors] = useState({});
  // stage: select -> uploading -> job (status comes from the server) | error
  const [stage, setStage] = useState("select");
  const [progress, setProgress] = useState(0);
  const [jobId, setJobId] = useState(null);
  const [error, setError] = useState(null);
  const [errorCode, setErrorCode] = useState(null);
  const [downloading, setDownloading] = useState(false);
  const dispatch = useDispatch();
  const { usage } = useUsage();
  // Same pricing as the server (CreditService); AI tools cost more
  const cost = tool.category === "ai" ? usage?.aiCost ?? 3 : usage?.standardCost ?? 1;
  // Typing in option fields must stay snappy; the preview follows a moment later
  const previewOptions = useDeferredValue(options);
  // PDF editor: work in progress survives saving (keep editing after seeing the result)
  const [editorWork, setEditorWork] = useState(null);
  const [lastSave, setLastSave] = useState(null);

  // Poll the job until the backend reports a terminal status.
  const cached = toolsApi.endpoints.getJob.useQueryState(jobId, { skip: !jobId });
  const finished = TERMINAL.includes(cached.data?.status);
  const { data: job, isError: pollFailed } = useGetJobQuery(jobId, {
    skip: !jobId || stage !== "job",
    pollingInterval: finished ? 0 : 1000,
  });

  // A finished job changes today's usage (failed jobs are refunded), so refresh the credit displays.
  useEffect(() => {
    if (finished) refreshUsage(dispatch);
  }, [finished, dispatch]);

  /** Starts a job; on rejection keeps the server's message and code (credits, login). */
  const start = async (args) => {
    setStage("uploading");
    setProgress(0);
    setError(null);
    setErrorCode(null);
    try {
      const created = await startToolJob({ ...args, toolId: tool.id, onProgress: setProgress });
      setJobId(created.id);
      setStage("job");
      refreshUsage(dispatch);
    } catch (err) {
      setError(await getErrorMessage(err));
      setErrorCode(err?.response?.data?.code ?? null);
      setStage("error");
    }
  };

  let phase = stage;
  let message = error;
  if (stage === "job") {
    if (job?.status === "SUCCEEDED") phase = "succeeded";
    else if (job?.status === "FAILED") {
      phase = "failed";
      message = job.errorMessage;
    } else if (job?.status === "EXPIRED") {
      phase = "expired";
      message = "Files are deleted after processing for your privacy. Please run the tool again.";
    } else if (pollFailed) {
      phase = "failed";
      message = "Lost connection while checking the job status.";
    } else phase = "processing";
  } else if (stage === "error") {
    phase = "failed";
  }

  const addFiles = (incoming) => {
    const { accepted, errors } = validateFiles(tool, incoming, files.length);
    errors.forEach((msg) => toast.error(msg));
    if (accepted.length) setFiles((current) => [...current, ...accepted]);
  };

  const moveFile = (from, to) =>
    setFiles((current) => {
      const next = [...current];
      const [item] = next.splice(from, 1);
      next.splice(to, 0, item);
      return next;
    });

  const removeFile = (index) => setFiles((current) => current.filter((_, i) => i !== index));

  const changeOption = (name, value) => {
    setOptions((current) => ({ ...current, [name]: value }));
    setOptionErrors((current) => ({ ...current, [name]: null }));
  };

  const submit = async () => {
    const shown = visibleOptions(tool, options);
    const errors = {};
    shown.forEach((option) => {
      const msg = option.validate?.(options[option.name]);
      if (msg) errors[option.name] = msg;
    });
    setOptionErrors(errors);
    if (Object.keys(errors).length) return;

    if (files.length < tool.minFiles) {
      toast.error(`Please add at least ${tool.minFiles} files.`);
      return;
    }

    // Only send options that apply to the current selection.
    const payload = Object.fromEntries(shown.map((option) => [option.name, options[option.name]]));

    await start({ files, options: payload });
  };

  /** Sends the editor's changes: the PDF + images as files, the change list as JSON data. */
  const saveEdits = async (save) => {
    setLastSave(save);
    await start({ files: save.files, options: {}, data: save.data });
  };

  const download = async () => {
    setDownloading(true);
    try {
      await downloadJobResult(job);
    } catch (err) {
      if (err?.response?.status === 410) {
        setError("Files are deleted after processing for your privacy. Please run the tool again.");
        setStage("expired");
      } else {
        toast.error(await getErrorMessage(err, "Download failed. Please try again."));
      }
    } finally {
      setDownloading(false);
    }
  };

  const reset = () => {
    setEditorWork(null);
    setLastSave(null);
    setFiles([]);
    setOptions(defaultOptions(tool));
    setOptionErrors({});
    setJobId(null);
    setError(null);
    setStage("select");
  };

  if (phase === "succeeded" && tool.resultView === "chat") {
    return <ChatPanel job={job} fileName={files[0]?.name ?? "Your document"} onReset={reset} />;
  }

  if (phase === "failed" && CREDIT_ERRORS.includes(errorCode)) {
    return (
      <OutOfCredits
        message={message}
        onReset={() => {
          setError(null);
          setErrorCode(null);
          setStage("select");
        }}
      />
    );
  }

  if (phase !== "select") {
    return (
      <JobStatusPanel
        tool={tool}
        phase={phase}
        progress={progress}
        job={job}
        error={message}
        downloading={downloading}
        onDownload={download}
        onRetry={tool.editor ? (lastSave ? () => saveEdits(lastSave) : null) : files.length ? submit : null}
        onReset={reset}
        onBack={tool.editor ? () => {
          setJobId(null);
          setStage("select");
        } : null}
        backLabel="Keep editing"
      />
    );
  }

  if (files.length === 0) {
    return <FileDropzone tool={tool} onFiles={addFiles} />;
  }

  if (tool.editor) {
    return (
      <Suspense fallback={<div className="glass flex h-96 items-center justify-center rounded-3xl text-muted-foreground">Loading editor…</div>}>
        <PdfEditor file={files[0]} initial={editorWork} onClose={setEditorWork} onSave={saveEdits} saving={stage === "uploading"} />
      </Suspense>
    );
  }

  const ready = files.length >= tool.minFiles;

  return (
    <div className="grid gap-6 md:grid-cols-[1fr_360px]">
      <div className="space-y-3">
        <FileList files={files} reorder={tool.reorder} onMove={moveFile} onRemove={removeFile} />
        {files.length < tool.maxFiles && <FileDropzone tool={tool} onFiles={addFiles} compact />}
        {files.some((f) => isPdf(f) || isImage(f)) && (
          <DocumentPreview
            toolId={tool.id}
            files={files}
            options={previewOptions}
            title="Live preview"
            password={tool.id === "unlock-pdf" ? previewOptions.password ?? "" : ""}
          />
        )}
      </div>

      <div className="glass flex flex-col gap-6 self-start rounded-3xl p-6 md:sticky md:top-24">
        <ToolOptions options={tool.options} values={options} errors={optionErrors} onChange={changeOption} />

        {tool.options.length === 0 && (
          <div>
            <p className="text-sm font-semibold">
              {files.length} {files.length === 1 ? "file" : "files"} selected
            </p>
            <p className="mt-1 text-sm text-muted-foreground">
              {tool.reorder && files.length > 1 ? "Files are processed in the order shown." : "No settings needed."}
            </p>
          </div>
        )}

        {!ready && (
          <p className="text-sm text-muted-foreground">
            Add at least {tool.minFiles} files to continue.
          </p>
        )}

        <Button size="lg" onClick={submit} disabled={!ready} className="h-12 rounded-xl text-base shadow-[0_10px_24px_-10px_var(--primary)] transition-transform enabled:hover:-translate-y-0.5">
          {tool.actionLabel}
          <ArrowRight />
        </Button>
        <CreditHint cost={cost} />
      </div>
    </div>
  );
};

const ComingSoon = ({ tool }) => {
  const available = ALL_TOOLS.filter((t) => t.available).slice(0, 6);

  return (
    <div className="glass rounded-3xl p-8 text-center md:p-12">
      <div className="mx-auto mb-4 inline-flex items-center gap-2 rounded-full bg-primary/10 px-3.5 py-1.5 text-sm font-medium text-primary">
        <Sparkles className="h-4 w-4" />
        Coming soon
      </div>
      <h2 className="text-2xl font-semibold">{tool.title} is on the way</h2>
      <p className="mx-auto mt-2 max-w-lg text-muted-foreground">
        We're building this tool right now. In the meantime, try one of these:
      </p>

      <div className="mt-8 flex flex-wrap justify-center gap-2">
        {available.map((t) => (
          <Link
            key={t.id}
            to={`/tools/${t.id}`}
            className="rounded-full border bg-background/50 px-4 py-2 text-sm transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:text-primary"
          >
            {t.title}
          </Link>
        ))}
      </div>
    </div>
  );
};

const ToolInfo = ({ tool }) => (
  <div className="grid gap-4 md:grid-cols-2">
    {tool.about && (
      <div className="glass flex gap-3 rounded-2xl p-5">
        <Info className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
        <p className="text-sm text-muted-foreground">{tool.about}</p>
      </div>
    )}
    <div className="glass flex gap-3 rounded-2xl p-5">
      <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
      <p className="text-sm text-muted-foreground">
        Your uploads are deleted as soon as processing finishes, and results are deleted after{" "}
        {RETENTION_MINUTES} minutes. No account needed.
      </p>
    </div>
  </div>
);

/** The server says this tool's engine isn't installed/configured right now. */
const Unavailable = ({ reason }) => {
  const others = ALL_TOOLS.filter((t) => t.available).slice(0, 6);

  return (
    <div className="glass rounded-3xl p-8 text-center md:p-12">
      <div className="mx-auto mb-4 inline-flex items-center gap-2 rounded-full bg-muted px-3.5 py-1.5 text-sm font-medium text-muted-foreground">
        <Wrench className="h-4 w-4" />
        Temporarily unavailable
      </div>
      <h2 className="text-2xl font-semibold">This tool isn't available right now</h2>
      <p className="mx-auto mt-2 max-w-lg text-muted-foreground">{reason} Please check back soon, or try another tool:</p>
      <div className="mt-8 flex flex-wrap justify-center gap-2">
        {others.map((t) => (
          <Link
            key={t.id}
            to={`/tools/${t.id}`}
            className="rounded-full border bg-background/50 px-4 py-2 text-sm transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:text-primary"
          >
            {t.title}
          </Link>
        ))}
      </div>
    </div>
  );
};

const ToolPage = () => {
  const { toolId } = useParams();
  const tool = getToolById(toolId);
  // If this request fails we still show the tool; the server rejects unavailable tools anyway.
  const { data: status } = useGetToolStatusQuery();
  const serverStatus = status?.tools?.find((t) => t.id === toolId);
  const isAuthenticated = useSelector(selectIsAuthenticated);

  usePageTitle(tool?.title ?? "Tool not found", tool ? `${tool.description} Free online, no sign-up required.` : undefined);

  if (!tool) {
    return <NotFoundPage />;
  }

  let body;
  if (!tool.available) body = <ComingSoon tool={tool} />;
  else if (tool.category === "ai" && !isAuthenticated) body = <LoginRequired tool={tool} />;
  else if (serverStatus && !serverStatus.available) body = <Unavailable reason={serverStatus.reason} />;
  else body = <ToolWorkspace key={tool.id} tool={tool} />;

  return (
    <ToolLayout tool={tool} wide={tool.editor} aside={tool.available ? <ToolInfo tool={tool} /> : null}>
      {body}
    </ToolLayout>
  );
};

export default ToolPage;
