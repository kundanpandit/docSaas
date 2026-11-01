import { MAX_FILE_SIZE_MB } from "@/features/home/constants/tools";

export const formatBytes = (bytes) => {
  if (bytes == null) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const kindOf = (file) => {
  const name = file.name.toLowerCase();

  if (file.type === "application/pdf" || name.endsWith(".pdf")) return "pdf";
  if (file.type === "image/png" || name.endsWith(".png")) return "png";
  if (file.type === "image/jpeg" || /\.jpe?g$/.test(name)) return "jpg";
  if (file.type === "image/webp" || name.endsWith(".webp")) return "webp";
  if (name.endsWith(".docx")) return "docx";
  if (name.endsWith(".doc")) return "doc";

  return null;
};

/**
 * Quick client-side checks for immediate feedback. The server re-validates
 * everything (including the real file content), so this is not a security check.
 * Returns { accepted, errors }.
 */
export const validateFiles = (tool, incoming, existingCount) => {
  const accepted = [];
  const errors = [];
  let count = existingCount;

  for (const file of incoming) {
    if (count >= tool.maxFiles) {
      errors.push(
        tool.maxFiles === 1
          ? "This tool works with one file at a time."
          : `You can add up to ${tool.maxFiles} files.`
      );
      break;
    }

    if (!tool.input.kinds.includes(kindOf(file))) {
      errors.push(`"${file.name}" is not a ${tool.input.label} file.`);
      continue;
    }

    if (file.size === 0) {
      errors.push(`"${file.name}" is empty.`);
      continue;
    }

    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      errors.push(`"${file.name}" is larger than ${MAX_FILE_SIZE_MB} MB.`);
      continue;
    }

    accepted.push(file);
    count++;
  }

  return { accepted, errors };
};
