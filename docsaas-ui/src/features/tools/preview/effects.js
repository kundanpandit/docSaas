/**
 * What each tool will do to a page, for the live preview. Mirrors the server processors
 * (same sizes, margins and opacities) so the preview matches the downloaded result.
 *
 * effect = {
 *   rotate:    extra clockwise degrees
 *   dim:       page is not part of the result
 *   badge:     short label ("Part 2")
 *   group:     color index for pages that end up in the same output file
 *   watermark: { text, diagonal, opacity }
 *   label:     { text, position }   page number
 *   frame:     { width, height, margin }   images-to-pdf page in points
 * }
 */

/** "1-3, 5" -> [[1,3],[5,5]], or null when the text is not a valid list yet. */
export const parseRanges = (text) => {
  if (!text || !text.trim()) return null;
  const ranges = [];
  for (const part of text.split(",")) {
    const p = part.trim();
    if (!p) continue;
    const m = p.match(/^(\d+)\s*(?:-\s*(\d+))?$/);
    if (!m) return null;
    const from = Number(m[1]);
    const to = m[2] ? Number(m[2]) : from;
    if (from < 1 || to < from) return null;
    ranges.push([from, to]);
  }
  return ranges.length ? ranges : null;
};

const rangeIndex = (ranges, page) => ranges?.findIndex(([from, to]) => page >= from && page <= to) ?? -1;

const A4 = [595.28, 841.89];
const LETTER = [612, 792];
const IMAGE_MARGIN = { none: 0, small: 20, large: 56 };

/**
 * @param page { fileIndex, pageNumber (1-based within its file), index (0-based overall), total, width, height }
 *             width/height: page (or image) size in points/pixels as displayed
 */
export const pageEffect = (toolId, options, page) => {
  switch (toolId) {
    case "merge-pdf":
      return { group: page.fileIndex, badge: page.pageNumber === 1 ? `File ${page.fileIndex + 1}` : null };

    case "split-pdf": {
      if (options.mode === "every") return { group: page.pageNumber - 1, badge: `File ${page.pageNumber}` };
      const ranges = parseRanges(options.ranges);
      if (!ranges) return {};
      const i = rangeIndex(ranges, page.pageNumber);
      if (i < 0) return { dim: true };
      if (options.mode === "extract") return { group: 0, badge: "Kept" };
      return { group: i, badge: ranges.length > 1 ? `Part ${i + 1}` : "Kept" };
    }

    case "rotate-pdf": {
      const selected = options.pages !== "ranges" || rangeIndex(parseRanges(options.ranges), page.pageNumber) >= 0;
      // unselected pages stay in the document unchanged, so they are not dimmed
      return selected ? { rotate: Number(options.angle) || 90 } : {};
    }

    case "watermark-pdf":
      return options.text?.trim()
        ? { watermark: { text: options.text.trim(), diagonal: options.layout !== "center", opacity: { light: 0.15, strong: 0.45 }[options.opacity] ?? 0.28 } }
        : {};

    case "page-numbers": {
      const start = Number.parseInt(options.start, 10);
      const first = Number.isFinite(start) && start >= 1 ? start : 1;
      const n = first + page.index;
      const last = first + page.total - 1;
      const text = options.format === "of-total" ? `${n} of ${last}` : options.format === "page" ? `Page ${n}` : String(n);
      return { label: { text, position: options.position || "bottom-center" } };
    }

    case "images-to-pdf": {
      if (options.pageSize === "fit") return { frame: { width: page.width, height: page.height, margin: 0 } };
      const [w, h] = options.pageSize === "letter" ? LETTER : A4;
      const landscape = options.orientation === "landscape" || (options.orientation !== "portrait" && page.width > page.height);
      return { frame: { width: landscape ? h : w, height: landscape ? w : h, margin: IMAGE_MARGIN[options.margin] ?? 20 } };
    }

    default:
      return {};
  }
};

/** Short description of the result under the preview. */
export const previewSummary = (toolId, options, pages) => {
  const total = pages.length;
  switch (toolId) {
    case "split-pdf": {
      if (options.mode === "every") return `${total} separate PDF files`;
      const ranges = parseRanges(options.ranges);
      if (!ranges) return "Enter pages to see what will be split";
      const kept = pages.filter((p) => rangeIndex(ranges, p.pageNumber) >= 0).length;
      const outside = ranges.some(([, to]) => to > total);
      if (outside) return `Some pages are beyond the end of the document (it has ${total})`;
      return options.mode === "extract" ? `${kept} pages in one new PDF` : `${ranges.length} ${ranges.length === 1 ? "PDF" : "PDFs"}, ${kept} pages`;
    }
    case "rotate-pdf": {
      if (options.pages !== "ranges") return `All ${total} pages rotated ${options.angle || 90}° clockwise`;
      const ranges = parseRanges(options.ranges);
      const n = ranges ? pages.filter((p) => rangeIndex(ranges, p.pageNumber) >= 0).length : 0;
      return `${n} of ${total} pages rotated`;
    }
    case "merge-pdf":
      return `${total} pages in one PDF, in this order`;
    default:
      return `${total} ${total === 1 ? "page" : "pages"}`;
  }
};

/** Distinct, theme-friendly colors for page groups (split parts, merged files). */
export const GROUP_COLORS = ["#6366f1", "#10b981", "#f59e0b", "#ec4899", "#06b6d4", "#8b5cf6", "#ef4444", "#84cc16"];
