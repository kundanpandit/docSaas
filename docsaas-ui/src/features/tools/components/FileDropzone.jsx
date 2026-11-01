import { useRef, useState } from "react";
import { motion, useReducedMotion } from "framer-motion";
import { Plus, UploadCloud } from "lucide-react";

import { Button } from "@/components/ui/button";
import { MAX_FILE_SIZE_MB } from "@/features/home/constants/tools";
import { cn } from "@/lib/utils";

const FileDropzone = ({ tool, onFiles, compact = false }) => {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const reduceMotion = useReducedMotion();

  const multiple = tool.maxFiles > 1;
  const open = () => inputRef.current?.click();

  const handleDrop = (event) => {
    event.preventDefault();
    setDragging(false);
    onFiles(Array.from(event.dataTransfer.files));
  };

  const handleChange = (event) => {
    onFiles(Array.from(event.target.files));
    // allow selecting the same file again after removing it
    event.target.value = "";
  };

  const input = (
    <input
      ref={inputRef}
      type="file"
      accept={tool.input.accept}
      multiple={multiple}
      onChange={handleChange}
      className="hidden"
      aria-label={`Select ${tool.input.label} files`}
    />
  );

  const dragProps = {
    onDragOver: (event) => {
      event.preventDefault();
      setDragging(true);
    },
    onDragLeave: () => setDragging(false),
    onDrop: handleDrop,
  };

  if (compact) {
    return (
      <button
        type="button"
        onClick={open}
        {...dragProps}
        className={cn(
          "flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed py-4 text-sm font-medium text-muted-foreground transition-colors hover:border-primary/50 hover:text-primary focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
          dragging ? "border-primary bg-primary/10 text-primary" : "border-border"
        )}
      >
        <Plus size={16} />
        Add more files
        {input}
      </button>
    );
  }

  return (
    <div style={{ perspective: 1000 }}>
      <motion.div
        {...dragProps}
        onClick={open}
        animate={reduceMotion ? undefined : { rotateX: dragging ? 8 : 0, scale: dragging ? 1.02 : 1, y: dragging ? -6 : 0 }}
        transition={{ type: "spring", stiffness: 260, damping: 22 }}
        className={cn(
          "glass group relative flex cursor-pointer flex-col items-center justify-center gap-5 overflow-hidden rounded-3xl px-6 py-16 text-center md:py-20",
          dragging && "shadow-lift ring-2 ring-primary"
        )}
      >
        <div aria-hidden="true" className="bg-brand-gradient absolute -top-20 left-1/2 h-40 w-2/3 -translate-x-1/2 rounded-full opacity-0 blur-3xl transition-opacity duration-500 group-hover:opacity-20" />

        <motion.span
          animate={reduceMotion ? undefined : { y: dragging ? -8 : 0 }}
          className="bg-brand-gradient relative flex h-16 w-16 items-center justify-center rounded-2xl text-white shadow-[0_14px_30px_-10px_var(--primary)] transition-transform duration-300 group-hover:-translate-y-1 group-hover:rotate-3"
        >
          <UploadCloud size={30} />
        </motion.span>

        <div className="relative">
          <p className="text-lg font-semibold">
            {dragging ? "Drop to upload" : `Drop your ${tool.input.label} ${multiple ? "files" : "file"} here`}
          </p>
          <p className="mt-1 text-sm text-muted-foreground">or</p>
        </div>

        <Button
          type="button"
          size="lg"
          onClick={(event) => {
            event.stopPropagation();
            open();
          }}
          className="relative h-12 rounded-xl px-7 text-base shadow-[0_10px_24px_-10px_var(--primary)]"
        >
          Choose {multiple ? "files" : "file"}
        </Button>

        <p className="relative text-xs text-muted-foreground">
          {tool.input.label}
          {multiple ? ` · up to ${tool.maxFiles} files` : ""} · max {MAX_FILE_SIZE_MB} MB each
        </p>
        {input}
      </motion.div>
    </div>
  );
};

export default FileDropzone;
