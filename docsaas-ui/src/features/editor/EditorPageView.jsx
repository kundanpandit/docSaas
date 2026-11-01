import { useEffect, useRef, useState } from "react";

import PageCanvas from "@/features/tools/preview/PageCanvas";
import { loadPdfjs } from "@/lib/pdf";
import { cn } from "@/lib/utils";

import { FONTS, LINE_HEIGHT, bounds, clamp, measureText, moved, sampleColors, simplify, toPageDelta, toPagePoint } from "./geometry";
import { newId } from "./model";

const DRAW_TOOLS = ["whiteout", "highlight", "rect", "line", "arrow", "pen"];

/** Text runs of a PDF page in displayed coordinates, for "edit existing text". */
const useTextRuns = (doc, page, enabled) => {
  const [runs, setRuns] = useState(null);
  useEffect(() => {
    if (!enabled || !doc || !page.source || runs) return undefined;
    let cancelled = false;
    (async () => {
      const pdfjs = await loadPdfjs();
      const p = await doc.getPage(page.source);
      const viewport = p.getViewport({ scale: 1 });
      const content = await p.getTextContent();
      // font names (bold/italic) are known once the page's fonts have loaded
      await p.getOperatorList().catch(() => {});
      const out = [];
      for (const item of content.items) {
        if (!item.str?.trim()) continue;
        const tx = pdfjs.Util.transform(viewport.transform, item.transform);
        const size = Math.hypot(tx[2], tx[3]);
        // only horizontal, upright text can be replaced in place
        if (size < 2 || Math.abs(tx[1]) > size * 0.02 || tx[0] <= 0) continue;
        const style = content.styles[item.fontName] ?? {};
        const ascent = style.ascent || 0.8;
        const descent = style.descent || -0.2;
        const fontName = (() => {
          try {
            return p.commonObjs.has(item.fontName) ? p.commonObjs.get(item.fontName)?.name ?? "" : "";
          } catch {
            return "";
          }
        })();
        out.push({
          text: item.str,
          x: tx[4],
          baseline: tx[5],
          top: tx[5] - size * ascent,
          w: item.width,
          h: size * (ascent - descent),
          size,
          font: style.fontFamily === "monospace" || /courier|mono/i.test(fontName) ? "courier"
            : style.fontFamily === "serif" || /times|serif|roman|georgia|garamond|cambria/i.test(fontName) && !/sans/i.test(fontName) ? "times"
              : "helvetica",
          bold: /bold|black|heavy|semibold/i.test(fontName),
          italic: /italic|oblique/i.test(fontName),
        });
      }
      if (!cancelled) setRuns(out);
    })().catch(() => !cancelled && setRuns([]));
    return () => {
      cancelled = true;
    };
  }, [doc, page.source, enabled, runs]);
  return runs;
};

// ===== object drawing (SVG, in page points) =====

const ArrowHead = ({ o }) => {
  const angle = Math.atan2(o.y2 - o.y1, o.x2 - o.x1);
  const head = Math.max(8, (o.width ?? 2) * 4);
  const spread = (25 * Math.PI) / 180;
  const pts = [
    [o.x2, o.y2],
    [o.x2 - head * Math.cos(angle - spread), o.y2 - head * Math.sin(angle - spread)],
    [o.x2 - head * Math.cos(angle + spread), o.y2 - head * Math.sin(angle + spread)],
  ];
  return <polygon points={pts.map((p) => p.join(",")).join(" ")} fill={o.color} />;
};

const Shape = ({ o }) => {
  switch (o.type) {
    case "whiteout":
      return <rect x={o.x} y={o.y} width={o.w} height={o.h} fill={o.color ?? "#ffffff"} />;
    case "highlight":
      return <rect x={o.x} y={o.y} width={o.w} height={o.h} fill={o.color} opacity={o.opacity ?? 0.4} style={{ mixBlendMode: "multiply" }} />;
    case "rect": {
      const sw = o.strokeWidth ?? 2;
      return (
        <g opacity={o.opacity ?? 1}>
          {o.color && o.color !== "none" && <rect x={o.x} y={o.y} width={o.w} height={o.h} fill={o.color} />}
          {sw > 0 && (
            <rect x={o.x + sw / 2} y={o.y + sw / 2} width={Math.max(0, o.w - sw)} height={Math.max(0, o.h - sw)} fill="none" stroke={o.stroke} strokeWidth={sw} />
          )}
        </g>
      );
    }
    case "line":
    case "arrow": {
      const angle = Math.atan2(o.y2 - o.y1, o.x2 - o.x1);
      const head = Math.max(8, (o.width ?? 2) * 4);
      const end = o.type === "arrow" ? [o.x2 - Math.cos(angle) * head * 0.8, o.y2 - Math.sin(angle) * head * 0.8] : [o.x2, o.y2];
      return (
        <g>
          <line x1={o.x1} y1={o.y1} x2={end[0]} y2={end[1]} stroke={o.color} strokeWidth={o.width} strokeLinecap="round" />
          {o.type === "arrow" && <ArrowHead o={o} />}
        </g>
      );
    }
    case "ink":
      return (
        <polyline
          points={o.points.map((p) => p.join(",")).join(" ")}
          fill="none"
          stroke={o.color}
          strokeWidth={o.width}
          strokeLinecap="round"
          strokeLinejoin="round"
          opacity={o.opacity ?? 1}
        />
      );
    default:
      return null;
  }
};

/** An invisible, wider copy of a shape that is easy to click. */
const HitArea = ({ o, zoom }) => {
  const pad = 6 / zoom;
  if (o.type === "line" || o.type === "arrow") return <line x1={o.x1} y1={o.y1} x2={o.x2} y2={o.y2} stroke="transparent" strokeWidth={(o.width ?? 2) + pad * 2} />;
  if (o.type === "ink") return <polyline points={o.points.map((p) => p.join(",")).join(" ")} fill="none" stroke="transparent" strokeWidth={(o.width ?? 2) + pad * 2} />;
  return <rect x={o.x} y={o.y} width={o.w} height={o.h} fill="transparent" />;
};

/**
 * One editable page. Objects are stored in points on the page as displayed; the whole page,
 * objects included, is turned by the user's rotation like a sheet of paper.
 */
const EditorPageView = ({ page, index, doc, objects, zoom, tool, style, selectedId, editingId, images, pendingImage, dispatch, onSelect, onEdit, onPlaced, onActive }) => {
  const frameRef = useRef(null);
  const innerRef = useRef(null);
  // the shape being drawn: a ref for event handlers (events can outrun re-renders), state for display
  const [draft, setDraftState] = useState(null);
  const draftRef = useRef(null);
  const setDraft = (next) => {
    draftRef.current = next;
    setDraftState(next);
  };
  const drag = useRef(null);
  const runs = useTextRuns(doc, page, tool === "edittext");

  const w = page.width * zoom;
  const h = page.height * zoom;
  const sideways = page.rotate % 180 !== 0;
  const point = (e) => toPagePoint(e, frameRef.current, page, zoom);
  const inside = (p) => ({ x: clamp(p.x, 0, page.width), y: clamp(p.y, 0, page.height) });

  // ===== creating objects =====

  const startDraw = (e) => {
    e.stopPropagation();
    onActive(page.id);
    if (pendingImage) {
      const p = point(e);
      const width = Math.min(220, page.width * 0.5);
      const height = (width * pendingImage.height) / pendingImage.width;
      dispatch({
        type: "addObject",
        object: { id: newId("o"), pageId: page.id, type: "image", imageId: pendingImage.id, x: p.x - width / 2, y: p.y - height / 2, w: width, h: height, angle: -page.rotate },
      });
      onPlaced();
      return;
    }
    if (tool === "text") {
      const p = point(e);
      const id = newId("o");
      dispatch({
        type: "addObject",
        object: { id, pageId: page.id, type: "text", x: p.x, y: p.y - style.size * 0.6, text: "", size: style.size, font: style.font, bold: style.bold, italic: style.italic, color: style.color, angle: -page.rotate },
      });
      onSelect(id);
      onEdit(id);
      e.preventDefault();
      return;
    }
    if (!DRAW_TOOLS.includes(tool)) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    const p = inside(point(e));
    setDraft({ start: p, end: p, points: [[p.x, p.y]] });
  };

  const moveDraw = (e) => {
    const d = draftRef.current;
    if (!d) return;
    const p = inside(point(e));
    setDraft({ ...d, end: p, points: tool === "pen" ? [...d.points, [p.x, p.y]] : d.points });
  };

  const endDraw = (e) => {
    const d = draftRef.current;
    if (!d) return;
    // the release point is the true end, even if the last move event was not processed yet
    const end = inside(point(e));
    const { start } = d;
    const draftPoints = tool === "pen" ? [...d.points, [end.x, end.y]] : d.points;
    const box = { x: Math.min(start.x, end.x), y: Math.min(start.y, end.y), w: Math.abs(end.x - start.x), h: Math.abs(end.y - start.y) };
    const base = { id: newId("o"), pageId: page.id };
    let object = null;
    if (tool === "pen") object = { ...base, type: "ink", points: simplify(draftPoints), color: style.color, width: style.strokeWidth };
    else if ((tool === "line" || tool === "arrow") && Math.hypot(end.x - start.x, end.y - start.y) > 3)
      object = { ...base, type: tool, x1: start.x, y1: start.y, x2: end.x, y2: end.y, color: style.stroke, width: style.strokeWidth };
    else if (box.w > 3 && box.h > 3) {
      if (tool === "whiteout") object = { ...base, type: "whiteout", ...box, color: "#ffffff" };
      if (tool === "highlight") object = { ...base, type: "highlight", ...box, color: style.highlight, opacity: 0.4 };
      if (tool === "rect") object = { ...base, type: "rect", ...box, stroke: style.stroke, strokeWidth: style.strokeWidth, color: style.fill };
    }
    setDraft(null);
    if (object) {
      dispatch({ type: "addObject", object });
      if (tool !== "pen") onSelect(object.id);
    }
  };

  /** Replaces a run of existing text: cover it with its own background, then type over it. */
  const editRun = (run) => {
    const canvas = innerRef.current?.querySelector("canvas");
    const colors = canvas ? sampleColors(canvas, { x: run.x - 1, y: run.top - 1, w: run.w + 2, h: run.h + 2 }, canvas.width / page.width) : null;
    const cover = { id: newId("o"), pageId: page.id, type: "whiteout", x: run.x - 1, y: run.top - 1, w: run.w + 2, h: run.h + 2, color: colors?.background ?? "#ffffff" };
    const textId = newId("o");
    const text = {
      id: textId,
      pageId: page.id,
      type: "text",
      x: run.x,
      y: run.baseline - FONTS[run.font].baseline * run.size,
      text: run.text,
      size: Math.round(run.size * 10) / 10,
      font: run.font,
      bold: run.bold,
      italic: run.italic,
      color: colors?.text ?? "#111111",
      angle: 0,
    };
    dispatch({ type: "addObject", object: cover });
    dispatch({ type: "addObject", object: text });
    onSelect(textId);
    onEdit(textId);
  };

  // ===== moving and resizing (select tool) =====

  const startDrag = (e, o, mode) => {
    if (tool !== "select" || editingId === o.id) return;
    e.stopPropagation();
    onSelect(o.id);
    onActive(page.id);
    e.currentTarget.setPointerCapture(e.pointerId);
    drag.current = { mode, o, x: e.clientX, y: e.clientY, moved: false };
  };

  const moveDrag = (e) => {
    const d = drag.current;
    if (!d) return;
    const { dx, dy } = toPageDelta(e.clientX - d.x, e.clientY - d.y, page, zoom);
    if (!d.moved) {
      if (Math.hypot(dx, dy) * zoom < 2) return;
      d.moved = true;
      dispatch({ type: "checkpoint" });
    }
    const o = d.o;
    let patch;
    if (d.mode === "move") patch = moved(o, dx, dy);
    else if (d.mode === "p1") patch = { x1: o.x1 + dx, y1: o.y1 + dy };
    else if (d.mode === "p2") patch = { x2: o.x2 + dx, y2: o.y2 + dy };
    else if (o.type === "text") {
      // dragging the corner scales the font; the turn of the text decides which way is "wider"
      const a = ((o.angle ?? 0) * Math.PI) / 180;
      const along = dx * Math.cos(a) + dy * Math.sin(a);
      const { width } = measureText(o);
      patch = { size: clamp(Math.round(o.size * (1 + along / width) * 10) / 10, 4, 300) };
    } else if (o.type === "image") {
      const a = ((o.angle ?? 0) * Math.PI) / 180;
      const along = dx * Math.cos(a) + dy * Math.sin(a);
      const nw = Math.max(8, o.w + along);
      patch = { w: nw, h: (nw * o.h) / o.w };
    } else {
      // box corner "se": grows right/down; keep a minimum size
      patch = { w: Math.max(4, o.w + dx), h: Math.max(4, o.h + dy) };
    }
    dispatch({ type: "updateObject", id: o.id, patch, record: false });
  };

  // line/arrow end handles say which end they move in data-mode ("p1" | "p2")
  const startEndpoint = (e) => startDrag(e, selected, e.currentTarget.dataset.mode);

  const endDrag = () => {
    drag.current = null;
  };

  // ===== rendering =====

  const shapes = objects.filter((o) => o.type !== "text" && o.type !== "image");
  const htmlObjects = objects.filter((o) => o.type === "text" || o.type === "image");
  const selected = objects.find((o) => o.id === selectedId);
  const drawing = DRAW_TOOLS.includes(tool) || tool === "text" || pendingImage;
  const handle = 8 / zoom;
  const selectionBox = selected && selected.type !== "text" && selected.type !== "image" ? bounds(selected) : null;

  let draftShape = null;
  if (draft) {
    const { start, end } = draft;
    const box = { x: Math.min(start.x, end.x), y: Math.min(start.y, end.y), w: Math.abs(end.x - start.x), h: Math.abs(end.y - start.y) };
    if (tool === "pen") draftShape = { type: "ink", points: draft.points, color: style.color, width: style.strokeWidth };
    else if (tool === "line" || tool === "arrow") draftShape = { type: tool, x1: start.x, y1: start.y, x2: end.x, y2: end.y, color: style.stroke, width: style.strokeWidth };
    else if (tool === "whiteout") draftShape = { type: "rect", ...box, stroke: "#94a3b8", strokeWidth: 1 / zoom, color: "#ffffff" };
    else if (tool === "highlight") draftShape = { type: "highlight", ...box, color: style.highlight, opacity: 0.4 };
    else if (tool === "rect") draftShape = { type: "rect", ...box, stroke: style.stroke, strokeWidth: style.strokeWidth, color: style.fill };
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <div
        ref={frameRef}
        className="relative"
        style={{ width: sideways ? h : w, height: sideways ? w : h }}
        data-page-id={page.id}
        onPointerEnter={() => onActive(page.id)}
      >
        <div
          ref={innerRef}
          className="absolute left-1/2 top-1/2 overflow-hidden bg-white shadow-lg ring-1 ring-black/10"
          style={{ width: w, height: h, transform: `translate(-50%, -50%) rotate(${page.rotate}deg)` }}
          // clicking empty paper clears the selection (objects stop the event when grabbed)
          onPointerDown={() => {
            onActive(page.id);
            if (tool === "select") onSelect(null);
          }}
        >
          {page.source > 0 && doc && <PageCanvas doc={doc} pageNumber={page.source} className="absolute inset-0 h-full w-full" />}

          {/* shapes */}
          <svg className="absolute inset-0" width={w} height={h} viewBox={`0 0 ${page.width} ${page.height}`} style={{ pointerEvents: "none" }}>
            {shapes.map((o) => (
              <g
                key={o.id}
                style={{ pointerEvents: tool === "select" ? "all" : "none", cursor: tool === "select" ? "move" : undefined }}
                onPointerDown={(e) => startDrag(e, o, "move")}
                onPointerMove={moveDrag}
                onPointerUp={endDrag}
              >
                <Shape o={o} />
                <HitArea o={o} zoom={zoom} />
              </g>
            ))}
            {draftShape && <Shape o={draftShape} />}
            {selectionBox && (
              <g style={{ pointerEvents: tool === "select" ? "all" : "none" }}>
                <rect x={selectionBox.x - 2 / zoom} y={selectionBox.y - 2 / zoom} width={selectionBox.w + 4 / zoom} height={selectionBox.h + 4 / zoom}
                  fill="none" stroke="#6366f1" strokeWidth={1.5 / zoom} strokeDasharray={`${4 / zoom} ${3 / zoom}`} />
                {(selected.type === "line" || selected.type === "arrow") &&
                  [["p1", selected.x1, selected.y1], ["p2", selected.x2, selected.y2]].map(([mode, x, y]) => (
                    <circle key={mode} data-mode={mode} cx={x} cy={y} r={handle / 1.5} fill="#fff" stroke="#6366f1" strokeWidth={1.5 / zoom} style={{ cursor: "crosshair" }}
                      onPointerDown={startEndpoint} onPointerMove={moveDrag} onPointerUp={endDrag} />
                  ))}
                {["whiteout", "highlight", "rect"].includes(selected.type) && (
                  <rect x={selected.x + selected.w - handle / 2} y={selected.y + selected.h - handle / 2} width={handle} height={handle} fill="#fff" stroke="#6366f1" strokeWidth={1.5 / zoom}
                    style={{ cursor: "nwse-resize" }} onPointerDown={(e) => startDrag(e, selected, "resize")} onPointerMove={moveDrag} onPointerUp={endDrag} />
                )}
              </g>
            )}
          </svg>

          {/* text and images */}
          {htmlObjects.map((o) => {
            const isSelected = o.id === selectedId;
            const editing = o.id === editingId;
            const common = {
              position: "absolute",
              left: o.x * zoom,
              top: o.y * zoom,
              transform: `rotate(${o.angle ?? 0}deg)`,
              transformOrigin: "0 0",
              pointerEvents: tool === "select" || editing ? "auto" : "none",
            };
            if (o.type === "image") {
              const img = images.get(o.imageId);
              return (
                <div key={o.id} style={{ ...common, width: o.w * zoom, height: o.h * zoom, cursor: tool === "select" ? "move" : undefined }}
                  className={cn(isSelected && "outline-2 outline-dashed outline-indigo-500")}
                  onPointerDown={(e) => startDrag(e, o, "move")} onPointerMove={moveDrag} onPointerUp={endDrag}>
                  {img && <img src={img.url} alt="" className="h-full w-full select-none" draggable={false} style={{ opacity: o.opacity ?? 1 }} />}
                  {isSelected && tool === "select" && (
                    <span className="absolute -bottom-1.5 -right-1.5 h-3 w-3 cursor-nwse-resize rounded-sm border-2 border-indigo-500 bg-white"
                      onPointerDown={(e) => startDrag(e, o, "resize")} onPointerMove={moveDrag} onPointerUp={endDrag} />
                  )}
                </div>
              );
            }
            const font = FONTS[o.font] ?? FONTS.helvetica;
            const textStyle = {
              fontFamily: font.css,
              fontSize: o.size * zoom,
              lineHeight: LINE_HEIGHT,
              fontWeight: o.bold ? 700 : 400,
              fontStyle: o.italic ? "italic" : "normal",
              color: o.color,
              whiteSpace: "pre",
            };
            if (editing) {
              const { width, height } = measureText({ ...o, text: o.text || " " });
              return (
                <textarea
                  key={o.id}
                  autoFocus
                  value={o.text}
                  spellCheck={false}
                  aria-label="Text"
                  onFocus={() => dispatch({ type: "checkpoint" })}
                  onChange={(e) => dispatch({ type: "updateObject", id: o.id, patch: { text: e.target.value }, record: false })}
                  onBlur={() => onEdit(null)}
                  onKeyDown={(e) => {
                    if (e.key === "Escape") e.currentTarget.blur();
                    e.stopPropagation();
                  }}
                  onPointerDown={(e) => e.stopPropagation()}
                  className="resize-none overflow-hidden border-0 bg-transparent p-0 outline-2 outline-dashed outline-indigo-500"
                  style={{ ...common, ...textStyle, width: width * zoom + 16, height: height * zoom + 2, caretColor: o.color }}
                />
              );
            }
            return (
              <div key={o.id} style={{ ...common, ...textStyle, cursor: tool === "select" ? "move" : undefined }}
                className={cn("select-none", isSelected && "outline-2 outline-dashed outline-indigo-500")}
                onPointerDown={(e) => startDrag(e, o, "move")} onPointerMove={moveDrag} onPointerUp={endDrag}
                onDoubleClick={() => onEdit(o.id)}>
                {o.text || " "}
                {isSelected && tool === "select" && (
                  <span className="absolute -bottom-1.5 -right-1.5 h-3 w-3 cursor-nwse-resize rounded-sm border-2 border-indigo-500 bg-white"
                    onPointerDown={(e) => startDrag(e, o, "resize")} onPointerMove={moveDrag} onPointerUp={endDrag} />
                )}
              </div>
            );
          })}

          {/* existing text that can be replaced */}
          {tool === "edittext" &&
            runs?.map((run, i) => (
              <button
                key={i}
                type="button"
                title="Click to edit this text"
                aria-label={`Edit "${run.text}"`}
                className="absolute rounded-[2px] outline-1 outline-dashed outline-indigo-400/60 hover:bg-indigo-500/15 hover:outline-indigo-600"
                style={{ left: run.x * zoom - 1, top: run.top * zoom - 1, width: run.w * zoom + 2, height: run.h * zoom + 2 }}
                onClick={() => editRun(run)}
              />
            ))}

          {/* capture layer for drawing tools */}
          {drawing && (
            <div
              className="absolute inset-0 z-10"
              style={{ cursor: pendingImage ? "copy" : tool === "text" ? "text" : drawing ? "crosshair" : "default", touchAction: "none" }}
              onPointerDown={startDraw}
              onPointerMove={moveDraw}
              onPointerUp={endDraw}
            />
          )}
        </div>
      </div>
      <span className="text-xs text-muted-foreground">Page {index + 1}</span>
    </div>
  );
};

export default EditorPageView;
