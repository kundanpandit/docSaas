/** Browser fonts the editor shows; metric-compatible with the PDF standard fonts the server uses. */
export const FONTS = {
  helvetica: { label: "Sans", css: "Arial, Helvetica, sans-serif", baseline: 0.9465 },
  times: { label: "Serif", css: "'Times New Roman', Times, serif", baseline: 0.9375 },
  courier: { label: "Mono", css: "'Courier New', Courier, monospace", baseline: 0.8665 },
};

/** CSS line height of text objects; the server spaces lines the same way. */
export const LINE_HEIGHT = 1.2;

let ctx;
/** Size of a text object in points: widest line x number of lines. */
export const measureText = ({ text, font, size, bold, italic }) => {
  ctx ??= document.createElement("canvas").getContext("2d");
  ctx.font = `${italic ? "italic " : ""}${bold ? "bold " : ""}${size}px ${FONTS[font]?.css ?? FONTS.helvetica.css}`;
  const lines = text.split("\n");
  const width = Math.max(...lines.map((l) => ctx.measureText(l).width), size * 0.3);
  return { width, height: lines.length * size * LINE_HEIGHT };
};

/**
 * Pointer position -> page points (as displayed before the user's rotation), for a page drawn
 * at `zoom` inside `frame` and turned `rotate` degrees clockwise around its center.
 */
export const toPagePoint = (event, frame, page, zoom) => {
  const rect = frame.getBoundingClientRect();
  const dx = event.clientX - (rect.left + rect.width / 2);
  const dy = event.clientY - (rect.top + rect.height / 2);
  const a = (-page.rotate * Math.PI) / 180;
  const x = dx * Math.cos(a) - dy * Math.sin(a);
  const y = dx * Math.sin(a) + dy * Math.cos(a);
  return { x: x / zoom + page.width / 2, y: y / zoom + page.height / 2 };
};

/** Turns a screen-space drag delta into page points for a page turned `rotate` degrees. */
export const toPageDelta = (dx, dy, page, zoom) => {
  const a = (-page.rotate * Math.PI) / 180;
  return { dx: (dx * Math.cos(a) - dy * Math.sin(a)) / zoom, dy: (dx * Math.sin(a) + dy * Math.cos(a)) / zoom };
};

export const clamp = (v, min, max) => Math.min(max, Math.max(min, v));

/** Bounding box of an object in page points. */
export const bounds = (o) => {
  switch (o.type) {
    case "line":
    case "arrow":
      return { x: Math.min(o.x1, o.x2), y: Math.min(o.y1, o.y2), w: Math.abs(o.x2 - o.x1), h: Math.abs(o.y2 - o.y1) };
    case "ink": {
      const xs = o.points.map((p) => p[0]);
      const ys = o.points.map((p) => p[1]);
      const x = Math.min(...xs);
      const y = Math.min(...ys);
      return { x, y, w: Math.max(...xs) - x, h: Math.max(...ys) - y };
    }
    case "text": {
      const { width, height } = measureText(o);
      const turned = Math.abs(o.angle ?? 0) % 180 === 90;
      return { x: o.x, y: o.y, w: turned ? height : width, h: turned ? width : height };
    }
    default:
      return { x: o.x, y: o.y, w: o.w, h: o.h };
  }
};

/** Moves any object by (dx, dy) points. */
export const moved = (o, dx, dy) => {
  switch (o.type) {
    case "line":
    case "arrow":
      return { x1: o.x1 + dx, y1: o.y1 + dy, x2: o.x2 + dx, y2: o.y2 + dy };
    case "ink":
      return { points: o.points.map(([x, y]) => [x + dx, y + dy]) };
    default:
      return { x: o.x + dx, y: o.y + dy };
  }
};

/** Drops points closer than `min` to the previous one: smaller saves, same look. */
export const simplify = (points, min = 0.75) =>
  points.filter((p, i) => i === 0 || i === points.length - 1 || Math.hypot(p[0] - points[i - 1][0], p[1] - points[i - 1][1]) >= min);

const hex = (r, g, b) => `#${[r, g, b].map((v) => v.toString(16).padStart(2, "0")).join("")}`;

/**
 * Background and text colors of a region of a rendered page canvas (for "edit existing text"):
 * the most common color along the region's border, and the pixel inside that differs most from it.
 */
export const sampleColors = (canvas, box, scale) => {
  try {
    const x = Math.max(0, Math.floor(box.x * scale));
    const y = Math.max(0, Math.floor(box.y * scale));
    const w = Math.min(canvas.width - x, Math.ceil(box.w * scale));
    const h = Math.min(canvas.height - y, Math.ceil(box.h * scale));
    if (w < 2 || h < 2) return null;
    const { data } = canvas.getContext("2d").getImageData(x, y, w, h);
    const at = (i, j) => {
      const k = (j * w + i) * 4;
      return [data[k], data[k + 1], data[k + 2]];
    };
    const counts = new Map();
    const add = (c) => {
      const key = c.map((v) => Math.round(v / 8) * 8).join(",");
      counts.set(key, (counts.get(key) ?? 0) + 1);
    };
    for (let i = 0; i < w; i++) {
      add(at(i, 0));
      add(at(i, h - 1));
    }
    for (let j = 0; j < h; j++) {
      add(at(0, j));
      add(at(w - 1, j));
    }
    const bg = [...counts.entries()].sort((a, b) => b[1] - a[1])[0][0].split(",").map((v) => Math.min(255, Number(v)));
    let best = bg;
    let bestDist = -1;
    for (let j = 0; j < h; j += 2) {
      for (let i = 0; i < w; i += 2) {
        const c = at(i, j);
        const d = Math.abs(c[0] - bg[0]) + Math.abs(c[1] - bg[1]) + Math.abs(c[2] - bg[2]);
        if (d > bestDist) {
          bestDist = d;
          best = c;
        }
      }
    }
    return { background: hex(...bg), text: bestDist > 60 ? hex(...best) : "#111111" };
  } catch {
    return null; // e.g. canvas not rendered yet
  }
};
