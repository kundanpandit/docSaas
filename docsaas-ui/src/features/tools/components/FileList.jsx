import { AnimatePresence, motion } from "framer-motion";
import { ArrowDown, ArrowUp, FileText, ImageIcon, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { formatBytes } from "../utils/files";

/** Stable identity per File object, so rows animate correctly when reordered. */
const ids = new WeakMap();
let nextId = 0;
const keyOf = (file) => {
  if (!ids.has(file)) ids.set(file, ++nextId);
  return ids.get(file);
};

const FileList = ({ files, reorder, disabled, onMove, onRemove }) => (
  <ol className="space-y-2" aria-label="Selected files">
    <AnimatePresence initial={false}>
      {files.map((file, index) => {
        const Icon = file.type.startsWith("image/") ? ImageIcon : FileText;

        return (
          <motion.li
            key={keyOf(file)}
            layout
            initial={{ opacity: 0, y: -8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, x: 24, transition: { duration: 0.15 } }}
            transition={{ type: "spring", stiffness: 500, damping: 38 }}
            className="glass flex items-center gap-3 rounded-2xl px-3 py-2.5"
          >
            {reorder && (
              <span className="w-5 shrink-0 text-center text-xs font-semibold text-muted-foreground">{index + 1}</span>
            )}

            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/12 text-primary">
              <Icon size={18} />
            </span>

            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium" title={file.name}>{file.name}</p>
              <p className="text-xs text-muted-foreground">{formatBytes(file.size)}</p>
            </div>

            {reorder && files.length > 1 && (
              <div className="flex shrink-0">
                <Button
                  type="button"
                  size="icon-sm"
                  variant="ghost"
                  disabled={disabled || index === 0}
                  onClick={() => onMove(index, index - 1)}
                  aria-label={`Move ${file.name} up`}
                >
                  <ArrowUp />
                </Button>
                <Button
                  type="button"
                  size="icon-sm"
                  variant="ghost"
                  disabled={disabled || index === files.length - 1}
                  onClick={() => onMove(index, index + 1)}
                  aria-label={`Move ${file.name} down`}
                >
                  <ArrowDown />
                </Button>
              </div>
            )}

            <Button
              type="button"
              size="icon-sm"
              variant="ghost"
              disabled={disabled}
              onClick={() => onRemove(index)}
              aria-label={`Remove ${file.name}`}
              className="shrink-0 hover:bg-destructive/10 hover:text-destructive"
            >
              <X />
            </Button>
          </motion.li>
        );
      })}
    </AnimatePresence>
  </ol>
);

export default FileList;
