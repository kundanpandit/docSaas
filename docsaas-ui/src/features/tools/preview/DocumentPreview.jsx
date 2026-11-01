import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, FileWarning, Loader2, Lock } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { isImage, isPdf, openPdf } from "@/lib/pdf";

import PageView from "./PageView";
import { pageEffect, previewSummary } from "./effects";

const MAX_PAGES = 60;

const loadImage = (file) =>
  new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => resolve({ kind: "image", url, width: img.naturalWidth, height: img.naturalHeight });
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("unreadable"));
    };
    img.src = url;
  });

/**
 * Turns the selected files into a flat list of pages with their displayed size.
 * Protected or unreadable PDFs become a single placeholder entry.
 */
const usePages = (files, password) => {
  const [state, setState] = useState({ loading: true, pages: [], hidden: 0 });

  useEffect(() => {
    let cancelled = false;
    const urls = [];

    (async () => {
      setState((s) => ({ ...s, loading: true }));
      const pages = [];
      let hidden = 0;
      for (const [fileIndex, file] of files.entries()) {
        if (isPdf(file)) {
          try {
            const doc = await openPdf(file, password);
            for (let n = 1; n <= doc.numPages; n++) {
              if (pages.length >= MAX_PAGES) {
                hidden += doc.numPages - n + 1;
                break;
              }
              const viewport = (await doc.getPage(n)).getViewport({ scale: 1 });
              pages.push({ kind: "pdf", doc, pageNumber: n, fileIndex, fileName: file.name, width: viewport.width, height: viewport.height });
            }
          } catch (err) {
            pages.push({ kind: err?.name === "PasswordException" ? "locked" : "broken", fileIndex, fileName: file.name, pageNumber: 1, width: 1, height: 1.414 });
          }
        } else if (isImage(file)) {
          if (pages.length >= MAX_PAGES) {
            hidden++;
            continue;
          }
          try {
            const img = await loadImage(file);
            urls.push(img.url);
            pages.push({ ...img, pageNumber: 1, fileIndex, fileName: file.name });
          } catch {
            pages.push({ kind: "broken", fileIndex, fileName: file.name, pageNumber: 1, width: 1, height: 1 });
          }
        }
        if (cancelled) return;
      }
      if (!cancelled) setState({ loading: false, pages, hidden });
    })();

    return () => {
      cancelled = true;
      urls.forEach((u) => URL.revokeObjectURL(u));
    };
  }, [files, password]);

  return state;
};

const Placeholder = ({ kind }) => (
  <div className="flex aspect-square w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed bg-muted/40 p-3 text-center text-xs text-muted-foreground">
    {kind === "locked" ? <Lock size={20} /> : <FileWarning size={20} />}
    {kind === "locked" ? "Password protected: no preview" : "Preview not available"}
  </div>
);

const PageCard = ({ page, effect, caption, onOpen }) => (
  <figure className="min-w-0">
    {page.kind === "pdf" || page.kind === "image" ? (
      <button
        type="button"
        onClick={onOpen}
        className="block w-full rounded-xl p-1 transition-colors hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        aria-label={`Enlarge ${caption}`}
      >
        <PageView page={page} effect={effect} />
      </button>
    ) : (
      <Placeholder kind={page.kind} />
    )}
    <figcaption className="mt-1 truncate text-center text-xs text-muted-foreground">{caption}</figcaption>
  </figure>
);

/**
 * Live preview of the selected files with the tool's current options applied.
 * toolId null = plain preview (e.g. a downloaded result).
 */
const DocumentPreview = ({ toolId = null, files, options = {}, title = "Preview", password = "" }) => {
  const { loading, pages, hidden } = usePages(files, password);
  const [open, setOpen] = useState(null);

  const withEffects = useMemo(() => {
    const viewable = pages.filter((p) => p.kind === "pdf" || p.kind === "image");
    return pages.map((page, index) => ({
      page,
      effect: toolId ? pageEffect(toolId, options, { ...page, index, total: viewable.length }) : {},
    }));
  }, [pages, toolId, options]);

  const multiFile = files.length > 1;
  const caption = (page) => (multiFile ? `${page.fileName}${page.kind === "pdf" ? ` · p. ${page.pageNumber}` : ""}` : page.kind === "pdf" ? `Page ${page.pageNumber}` : page.fileName);
  const viewable = withEffects.filter((p) => p.page.kind === "pdf" || p.page.kind === "image");

  const step = (delta) => setOpen((i) => (i + delta + viewable.length) % viewable.length);
  const current = open != null ? viewable[open] : null;

  if (!files.length) return null;

  return (
    <section className="glass rounded-3xl p-4" aria-label={title}>
      <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2 px-1">
        <h2 className="font-semibold">{title}</h2>
        {!loading && toolId && viewable.length > 0 && (
          <p className="text-sm text-muted-foreground">{previewSummary(toolId, options, viewable.map((v) => v.page))}</p>
        )}
      </div>

      {loading && pages.length === 0 ? (
        <div className="flex h-40 items-center justify-center text-muted-foreground">
          <Loader2 className="animate-spin" />
        </div>
      ) : (
        <div className="grid max-h-[40rem] grid-cols-2 gap-3 overflow-y-auto p-1 sm:grid-cols-3">
          {withEffects.map(({ page, effect }, i) => (
            <PageCard
              key={`${page.fileIndex}-${page.pageNumber}-${i}`}
              page={page}
              effect={effect}
              caption={caption(page)}
              onOpen={() => setOpen(viewable.findIndex((v) => v.page === page))}
            />
          ))}
        </div>
      )}
      {hidden > 0 && <p className="mt-2 px-1 text-xs text-muted-foreground">+ {hidden} more pages not shown in the preview.</p>}

      <Dialog open={current != null} onOpenChange={(o) => !o && setOpen(null)}>
        <DialogContent
          className="max-h-[95vh] sm:max-w-3xl"
          onKeyDown={(e) => {
            if (e.key === "ArrowRight") step(1);
            if (e.key === "ArrowLeft") step(-1);
          }}
        >
          <DialogTitle>{current ? caption(current.page) : ""}</DialogTitle>
          <DialogDescription className="sr-only">Enlarged preview. Use the arrow keys to move between pages.</DialogDescription>
          {current && <PageView page={current.page} effect={current.effect} className="mx-auto max-h-[75vh] max-w-[75vh]" />}
          {viewable.length > 1 && (
            <div className="flex items-center justify-center gap-3">
              <Button variant="outline" size="icon" onClick={() => step(-1)} aria-label="Previous page">
                <ChevronLeft />
              </Button>
              <span className="text-sm tabular-nums text-muted-foreground">
                {open + 1} / {viewable.length}
              </span>
              <Button variant="outline" size="icon" onClick={() => step(1)} aria-label="Next page">
                <ChevronRight />
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </section>
  );
};

export default DocumentPreview;
