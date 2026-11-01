import { useEffect, useState } from "react";
import { Check, Copy, Loader2 } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { getErrorMessage } from "@/services/api/apiClient";
import { fetchJobText } from "../api/toolsApi";

/** Inline **bold** only; everything else is plain text (no HTML is ever injected). */
const renderInline = (text) =>
  text.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith("**") && part.endsWith("**") ? <strong key={i}>{part.slice(2, -2)}</strong> : part
  );

/** Tiny, safe Markdown subset: headings, bullet lists, rules, italics line, paragraphs. */
const Markdown = ({ source }) => {
  const blocks = [];
  let list = null;

  source.split("\n").forEach((raw, i) => {
    const line = raw.trimEnd();
    const bullet = line.match(/^\s*[-*]\s+(.*)$/);
    if (bullet) {
      list = list ?? [];
      list.push(<li key={i}>{renderInline(bullet[1])}</li>);
      return;
    }
    if (list) {
      blocks.push(<ul key={`ul-${i}`} className="my-2 list-disc space-y-1 pl-5">{list}</ul>);
      list = null;
    }
    if (!line.trim()) return;
    if (line.startsWith("### ")) blocks.push(<h4 key={i} className="mt-4 font-semibold">{renderInline(line.slice(4))}</h4>);
    else if (line.startsWith("## ")) blocks.push(<h3 key={i} className="mt-5 text-lg font-semibold">{renderInline(line.slice(3))}</h3>);
    else if (line.startsWith("# ")) blocks.push(<h2 key={i} className="mt-5 text-xl font-semibold">{renderInline(line.slice(2))}</h2>);
    else if (/^-{3,}$/.test(line)) blocks.push(<hr key={i} className="my-4 border-border" />);
    else if (/^\*[^*].*\*$/.test(line)) blocks.push(<p key={i} className="text-xs italic text-muted-foreground">{line.slice(1, -1)}</p>);
    else blocks.push(<p key={i} className="my-2">{renderInline(line)}</p>);
  });
  if (list) blocks.push(<ul key="ul-end" className="my-2 list-disc space-y-1 pl-5">{list}</ul>);

  return <div className="text-sm leading-relaxed">{blocks}</div>;
};

/** Shows a text result (OCR text or AI summary) with a copy button. */
const TextResult = ({ job }) => {
  const [text, setText] = useState(null);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);
  const isMarkdown = job.output?.mimeType?.startsWith("text/markdown");

  useEffect(() => {
    let active = true;
    fetchJobText(job.id)
      .then((t) => active && setText(t))
      .catch(async (err) => active && setError(await getErrorMessage(err, "Could not load the result.")));
    return () => {
      active = false;
    };
  }, [job.id]);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      toast.error("Copy failed. Select the text and copy it manually.");
    }
  };

  return (
    <div className="relative mt-6 text-left">
      <div className="mb-2 flex items-center justify-between">
        <p className="text-sm font-medium">{isMarkdown ? "Summary" : "Extracted text"}</p>
        <Button size="sm" variant="ghost" onClick={copy} disabled={!text} className="rounded-lg">
          {copied ? <Check /> : <Copy />}
          {copied ? "Copied" : "Copy"}
        </Button>
      </div>
      <div className="max-h-96 overflow-y-auto rounded-2xl border bg-background/60 p-4">
        {error && <p className="text-sm text-destructive">{error}</p>}
        {!error && text === null && <Loader2 className="mx-auto animate-spin text-muted-foreground" />}
        {text !== null && (isMarkdown ? <Markdown source={text} /> : <pre className="whitespace-pre-wrap break-words font-sans text-sm">{text}</pre>)}
      </div>
    </div>
  );
};

export default TextResult;
