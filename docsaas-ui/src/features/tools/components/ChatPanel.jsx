import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Bot, FileText, RotateCcw, SendHorizontal, Sparkles, User } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { RETENTION_MINUTES } from "@/features/home/constants/tools";
import { cn } from "@/lib/utils";
import { useAskDocumentMutation } from "../api/toolsApi";

const STARTERS = ["Summarize this document", "What are the key points?", "List any dates or deadlines", "Are there any action items?"];
const MAX_HISTORY = 6;

const Bubble = ({ message }) => {
  const isUser = message.role === "user";
  const Icon = isUser ? User : Bot;

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn("flex gap-2.5", isUser && "flex-row-reverse")}
    >
      <span
        className={cn(
          "flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
          isUser ? "bg-muted text-muted-foreground" : "bg-brand-gradient text-white"
        )}
      >
        <Icon size={15} />
      </span>
      <div className={cn("max-w-[85%] rounded-2xl px-4 py-2.5 text-sm", isUser ? "bg-primary text-primary-foreground" : "border bg-background/70")}>
        <p className="whitespace-pre-wrap break-words">{message.content}</p>
        {message.pages?.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {message.pages.map((p) => (
              <span key={p} className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-medium text-primary">
                <FileText size={11} /> Page {p}
              </span>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  );
};

/** Chat about a document prepared by the chat-with-pdf tool. History lives only in this tab. */
const ChatPanel = ({ job, fileName, onReset }) => {
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [ask, { isLoading }] = useAskDocumentMutation();
  const scrollRef = useRef(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, isLoading]);

  const send = async (text) => {
    const question = text.trim();
    if (!question || isLoading) return;

    const history = messages.slice(-MAX_HISTORY).map(({ role, content }) => ({ role, content }));
    setMessages((m) => [...m, { role: "user", content: question }]);
    setDraft("");

    try {
      const answer = await ask({ jobId: job.id, question, history }).unwrap();
      setMessages((m) => [...m, { role: "assistant", content: answer.answer, pages: answer.pages }]);
    } catch (err) {
      // Put the question back so nothing typed is lost.
      setMessages((m) => m.slice(0, -1));
      setDraft(question);
      toast.error(err?.data?.message || "Couldn't get an answer. Please try again.");
    }
  };

  return (
    <div className="glass flex h-[min(70vh,640px)] flex-col overflow-hidden rounded-3xl">
      <div className="flex items-center justify-between gap-3 border-b px-5 py-3">
        <div className="flex min-w-0 items-center gap-2.5">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/12 text-primary">
            <FileText size={17} />
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">{fileName}</p>
            <p className="text-xs text-muted-foreground">Chat ends after {RETENTION_MINUTES} minutes</p>
          </div>
        </div>
        <Button variant="ghost" size="sm" onClick={onReset} className="shrink-0 rounded-lg">
          <RotateCcw />
          New file
        </Button>
      </div>

      <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto px-5 py-5" aria-live="polite">
        {messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center text-center">
            <span className="bg-brand-gradient flex h-12 w-12 items-center justify-center rounded-2xl text-white">
              <Sparkles size={22} />
            </span>
            <p className="mt-3 font-medium">Ask anything about your document</p>
            <div className="mt-5 flex max-w-md flex-wrap justify-center gap-2">
              {STARTERS.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => send(s)}
                  className="rounded-full border bg-background/50 px-3 py-1.5 text-sm transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:text-primary"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <Bubble key={i} message={m} />
        ))}

        <AnimatePresence>
          {isLoading && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center gap-2.5">
              <span className="bg-brand-gradient flex h-8 w-8 items-center justify-center rounded-full text-white">
                <Bot size={15} />
              </span>
              <span className="flex gap-1 rounded-2xl border bg-background/70 px-4 py-3">
                {[0, 1, 2].map((d) => (
                  <span key={d} className="h-1.5 w-1.5 animate-bounce rounded-full bg-muted-foreground" style={{ animationDelay: `${d * 0.15}s` }} />
                ))}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          send(draft);
        }}
        className="border-t p-3"
      >
        <div className="flex items-end gap-2 rounded-2xl border bg-background/60 p-1.5 focus-within:ring-3 focus-within:ring-ring/40">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                send(draft);
              }
            }}
            rows={1}
            maxLength={1000}
            placeholder="Ask a question about this document…"
            aria-label="Your question"
            className="max-h-32 min-h-9 flex-1 resize-none bg-transparent px-2.5 py-2 text-sm outline-none placeholder:text-muted-foreground"
          />
          <Button type="submit" size="icon-lg" disabled={!draft.trim() || isLoading} aria-label="Send" className="rounded-xl">
            <SendHorizontal />
          </Button>
        </div>
        <p className="mt-2 text-center text-[11px] text-muted-foreground">
          Answers are AI-generated from your document and can be wrong. Check important details.
        </p>
      </form>
    </div>
  );
};

export default ChatPanel;
