import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpRight,
  Bold,
  BringToFront,
  Copy,
  Eraser,
  FilePlus2,
  Highlighter,
  ImagePlus,
  Italic,
  Loader2,
  Minus,
  MousePointer2,
  PenLine,
  Plus,
  Redo2,
  RotateCcw,
  RotateCw,
  Save,
  SendToBack,
  Signature,
  Slash,
  Square,
  TextCursorInput,
  Trash2,
  Type,
  Undo2,
} from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import PageCanvas from "@/features/tools/preview/PageCanvas";
import { openPdf } from "@/lib/pdf";
import { cn } from "@/lib/utils";

import EditorPageView from "./EditorPageView";
import SignaturePad from "./SignaturePad";
import { FONTS, clamp, moved } from "./geometry";
import { DEFAULT_STYLE, buildSave, editorReducer, initialState, newId } from "./model";

const TOOLS = [
  { id: "select", label: "Select & move", icon: MousePointer2, key: "v" },
  { id: "edittext", label: "Edit existing text", icon: TextCursorInput, key: "e" },
  { id: "text", label: "Add text", icon: Type, key: "t" },
  { id: "whiteout", label: "Whiteout (cover content)", icon: Eraser, key: "w" },
  { id: "highlight", label: "Highlight", icon: Highlighter, key: "h" },
  { id: "rect", label: "Rectangle", icon: Square, key: "r" },
  { id: "line", label: "Line", icon: Slash, key: "l" },
  { id: "arrow", label: "Arrow", icon: ArrowUpRight, key: "a" },
  { id: "pen", label: "Draw", icon: PenLine, key: "p" },
];

const SWATCHES = ["#111111", "#ffffff", "#e11d48", "#f59e0b", "#16a34a", "#2563eb", "#7c3aed"];
const HIGHLIGHTS = ["#ffe600", "#86efac", "#f9a8d4", "#93c5fd"];

const ToolButton = ({ active, label, onClick, disabled, children }) => (
  <button
    type="button"
    title={label}
    aria-label={label}
    aria-pressed={active}
    disabled={disabled}
    onClick={onClick}
    className={cn(
      "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition-colors disabled:opacity-40",
      active ? "bg-primary text-primary-foreground" : "hover:bg-accent"
    )}
  >
    {children}
  </button>
);

const ColorRow = ({ label, value, colors, onChange, allowNone }) => (
  <div>
    <p className="mb-1.5 text-xs font-medium text-muted-foreground">{label}</p>
    <div className="flex flex-wrap items-center gap-1.5">
      {allowNone && (
        <button type="button" onClick={() => onChange("none")} aria-label="No fill" title="No fill"
          className={cn("h-6 w-6 rounded-full border bg-[linear-gradient(135deg,transparent_45%,#e11d48_45%,#e11d48_55%,transparent_55%)]", value === "none" && "ring-2 ring-primary ring-offset-1")} />
      )}
      {colors.map((c) => (
        <button key={c} type="button" onClick={() => onChange(c)} aria-label={`Color ${c}`}
          className={cn("h-6 w-6 rounded-full border", value === c && "ring-2 ring-primary ring-offset-1")} style={{ backgroundColor: c }} />
      ))}
      <input type="color" value={value === "none" ? "#ffffff" : value} onChange={(e) => onChange(e.target.value)} aria-label={`Custom ${label.toLowerCase()}`}
        className="h-6 w-8 cursor-pointer rounded border bg-transparent" />
    </div>
  </div>
);

const NumberField = ({ label, value, min, max, step = 1, onChange }) => (
  <label className="block text-xs font-medium text-muted-foreground">
    {label}
    <input type="number" value={value} min={min} max={max} step={step}
      onChange={(e) => e.target.value !== "" && onChange(clamp(Number(e.target.value), min, max))}
      className="mt-1 h-8 w-full rounded-lg border bg-background px-2 text-sm text-foreground" />
  </label>
);

/** Properties of the selected object, or of the next object the current tool will create. */
const PropertiesPanel = ({ tool, style, setStyle, selected, update, dispatch }) => {
  const kind = selected?.type ?? { text: "text", edittext: "text", pen: "ink", rect: "rect", line: "line", arrow: "arrow", highlight: "highlight" }[tool];
  // reads/writes the selected object, else the default style
  const get = (objKey, styleKey = objKey) => (selected ? selected[objKey] : style[styleKey]);
  const set = (objKey, styleKey, value) => (selected ? update({ [objKey]: value }) : setStyle((s) => ({ ...s, [styleKey]: value })));

  return (
    <div className="space-y-4">
      <p className="text-sm font-semibold">{selected ? "Selected" : TOOLS.find((t) => t.id === tool)?.label}</p>
      {kind === "text" && (
        <>
          <div className="grid grid-cols-3 gap-1">
            {Object.entries(FONTS).map(([id, f]) => (
              <button key={id} type="button" onClick={() => set("font", "font", id)} style={{ fontFamily: f.css }}
                className={cn("rounded-lg border px-2 py-1.5 text-sm", get("font") === id && "border-primary bg-primary/10 text-primary")}>
                {f.label}
              </button>
            ))}
          </div>
          <div className="flex items-end gap-2">
            <div className="flex-1"><NumberField label="Size" value={get("size")} min={4} max={300} onChange={(v) => set("size", "size", v)} /></div>
            <ToolButton label="Bold" active={get("bold")} onClick={() => set("bold", "bold", !get("bold"))}><Bold size={16} /></ToolButton>
            <ToolButton label="Italic" active={get("italic")} onClick={() => set("italic", "italic", !get("italic"))}><Italic size={16} /></ToolButton>
          </div>
          <ColorRow label="Color" value={get("color")} colors={SWATCHES} onChange={(v) => set("color", "color", v)} />
        </>
      )}
      {kind === "ink" && (
        <>
          <ColorRow label="Color" value={get("color")} colors={SWATCHES} onChange={(v) => set("color", "color", v)} />
          <NumberField label="Thickness" value={get("width", "strokeWidth")} min={0.5} max={30} step={0.5} onChange={(v) => set("width", "strokeWidth", v)} />
        </>
      )}
      {(kind === "line" || kind === "arrow") && (
        <>
          <ColorRow label="Color" value={get("color", "stroke")} colors={SWATCHES} onChange={(v) => set("color", "stroke", v)} />
          <NumberField label="Thickness" value={get("width", "strokeWidth")} min={0.5} max={30} step={0.5} onChange={(v) => set("width", "strokeWidth", v)} />
        </>
      )}
      {kind === "rect" && (
        <>
          <ColorRow label="Border" value={get("stroke")} colors={SWATCHES} onChange={(v) => set("stroke", "stroke", v)} />
          <ColorRow label="Fill" value={get("color", "fill")} colors={SWATCHES} allowNone onChange={(v) => set("color", "fill", v)} />
          <NumberField label="Border thickness" value={get("strokeWidth")} min={0} max={30} step={0.5} onChange={(v) => set("strokeWidth", "strokeWidth", v)} />
        </>
      )}
      {kind === "highlight" && <ColorRow label="Color" value={get("color", "highlight")} colors={HIGHLIGHTS} onChange={(v) => set("color", "highlight", v)} />}
      {kind === "whiteout" && (
        <>
          <ColorRow label="Cover color" value={selected?.color ?? "#ffffff"} colors={["#ffffff", "#f5f5f4", "#111111"]} onChange={(v) => selected && update({ color: v })} />
          <p className="text-xs text-muted-foreground">Text under the whiteout is removed from the file when you save. Images and drawings underneath are only covered.</p>
        </>
      )}
      {kind === "image" && (
        <label className="block text-xs font-medium text-muted-foreground">
          Opacity
          <input type="range" min={0.1} max={1} step={0.05} value={selected.opacity ?? 1} onChange={(e) => update({ opacity: Number(e.target.value) })} className="mt-2 w-full" />
        </label>
      )}
      {tool === "edittext" && !selected && <p className="text-xs text-muted-foreground">Click any text on the page to change it. The original is covered and your new text is typed in the same place, font and size.</p>}
      {tool === "select" && !selected && <p className="text-xs text-muted-foreground">Click an object to move or change it. Double-click text to edit it. Drag the corner handle to resize.</p>}

      {selected && (
        <div className="flex flex-wrap gap-1 border-t pt-3">
          <ToolButton label="Bring to front" onClick={() => dispatch({ type: "reorderObject", id: selected.id, toFront: true })}><BringToFront size={16} /></ToolButton>
          <ToolButton label="Send to back" onClick={() => dispatch({ type: "reorderObject", id: selected.id, toFront: false })}><SendToBack size={16} /></ToolButton>
          <ToolButton label="Duplicate" onClick={() => dispatch({ type: "addObject", object: { ...selected, ...moved(selected, 12, 12), id: newId("o") } })}><Copy size={16} /></ToolButton>
          <ToolButton label="Delete (Del)" onClick={() => dispatch({ type: "removeObject", id: selected.id })}><Trash2 size={16} className="text-destructive" /></ToolButton>
        </div>
      )}
    </div>
  );
};

/**
 * Full PDF editor. Pages render with pdf.js; edits stay in the browser until saved, then the
 * server applies them (EditPdfProcessor). The work is handed to `onClose` when the editor closes
 * and comes back as `initial`, so users can keep editing after saving.
 */
const PdfEditor = ({ file, initial, onClose, onSave, saving }) => {
  const [doc, setDoc] = useState(null);
  const [error, setError] = useState(null);
  const [state, dispatch] = useReducer(editorReducer, initial?.state ?? initialState([]));
  const [images, setImages] = useState(() => initial?.images ?? new Map());
  const [style, setStyle] = useState(() => initial?.style ?? DEFAULT_STYLE);
  const resumed = useRef(Boolean(initial?.state?.present.pages.length));
  const latest = useRef(null);
  const [tool, setTool] = useState("select");
  const [selectedId, setSelectedId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [zoom, setZoom] = useState(null);
  const [pendingImage, setPendingImage] = useState(null);
  const [signing, setSigning] = useState(false);
  const [activePageId, setActivePageId] = useState(null);
  const scrollRef = useRef(null);
  const imageInput = useRef(null);

  const { pages, objects } = state.present;
  const dirty = state.past.length > 0;
  const selected = objects.find((o) => o.id === selectedId) ?? null;

  // keep work across unmounts (save -> result -> keep editing)
  useEffect(() => {
    latest.current = { state, images, style };
  }, [state, images, style]);
  useEffect(() => () => onClose?.(latest.current), [onClose]);

  // open the PDF and, the first time, build the page list
  useEffect(() => {
    let cancelled = false;
    openPdf(file)
      .then(async (d) => {
        if (cancelled) return;
        if (!resumed.current) {
          const list = [];
          for (let n = 1; n <= d.numPages; n++) {
            const v = (await d.getPage(n)).getViewport({ scale: 1 });
            list.push({ id: newId("p"), source: n, width: v.width, height: v.height, rotate: 0 });
          }
          if (cancelled) return;
          dispatch({ type: "load", pages: list });
        }
        setDoc(d);
      })
      .catch((err) => !cancelled && setError(err?.name === "PasswordException"
        ? "This PDF is password protected. Unlock it first with Unlock PDF, then edit it."
        : "This PDF could not be opened for editing."));
    return () => {
      cancelled = true;
    };
  }, [file]);

  // fit the widest page to the available width on first load
  useEffect(() => {
    if (zoom != null || !pages.length || !scrollRef.current) return;
    const widest = Math.max(...pages.map((p) => (p.rotate % 180 ? p.height : p.width)));
    setZoom(clamp((scrollRef.current.clientWidth - 64) / widest, 0.4, 1.6));
  }, [pages, zoom]);

  // warn before leaving with unsaved changes
  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (e) => e.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const update = useCallback((patch) => selectedId && dispatch({ type: "updateObject", id: selectedId, patch }), [selectedId]);

  // keyboard shortcuts
  useEffect(() => {
    const onKey = (e) => {
      const typing = /^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName) || e.target.isContentEditable;
      if (typing) return;
      const mod = e.ctrlKey || e.metaKey;
      if (mod && e.key.toLowerCase() === "z") {
        e.preventDefault();
        dispatch({ type: e.shiftKey ? "redo" : "undo" });
      } else if (mod && e.key.toLowerCase() === "y") {
        e.preventDefault();
        dispatch({ type: "redo" });
      } else if ((e.key === "Delete" || e.key === "Backspace") && selectedId) {
        e.preventDefault();
        dispatch({ type: "removeObject", id: selectedId });
        setSelectedId(null);
      } else if (e.key === "Escape") {
        setSelectedId(null);
        setPendingImage(null);
        setTool("select");
      } else if (selected && e.key.startsWith("Arrow")) {
        e.preventDefault();
        const step = e.shiftKey ? 10 : 1;
        const d = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[e.key];
        dispatch({ type: "updateObject", id: selected.id, patch: moved(selected, ...d) });
      } else if (!mod && !e.altKey) {
        const t = TOOLS.find((x) => x.key === e.key.toLowerCase());
        if (t) setTool(t.id);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [selected, selectedId]);

  const pickTool = (id) => {
    setTool(id);
    setPendingImage(null);
    if (id !== "select") setSelectedId(null);
  };

  const addImage = (blob, width, height) => {
    const id = newId("img");
    const url = URL.createObjectURL(blob);
    const name = blob.name ?? "signature.png";
    const image = { id, file: blob instanceof File ? blob : new File([blob], name, { type: blob.type || "image/png" }), url, width, height };
    setImages((m) => new Map(m).set(id, image));
    setPendingImage(image);
    setSelectedId(null);
    toast("Click on a page to place it", { icon: "👆" });
  };

  const onImageFile = (e) => {
    const f = e.target.files?.[0];
    e.target.value = "";
    if (!f) return;
    if (!/^image\/(png|jpeg|webp)$/.test(f.type)) {
      toast.error("Please choose a JPG, PNG or WEBP image.");
      return;
    }
    if (f.size > 15 * 1024 * 1024) {
      toast.error("That image is too large (max 15 MB).");
      return;
    }
    const img = new Image();
    const url = URL.createObjectURL(f);
    img.onload = () => {
      URL.revokeObjectURL(url);
      addImage(f, img.naturalWidth, img.naturalHeight);
    };
    img.onerror = () => toast.error("That image could not be read.");
    img.src = url;
  };

  const save = () => {
    setEditingId(null);
    const save = buildSave(file, state.present, new Map([...images].map(([id, v]) => [id, v.file])));
    onSave(save);
  };

  const scrollToPage = (id) => {
    scrollRef.current?.querySelector(`[data-page-id="${id}"]`)?.scrollIntoView({ behavior: "smooth", block: "start" });
    setActivePageId(id);
  };

  const objectsByPage = useMemo(() => {
    const map = new Map(pages.map((p) => [p.id, []]));
    objects.forEach((o) => map.get(o.pageId)?.push(o));
    return map;
  }, [pages, objects]);

  if (error) {
    return <div className="glass rounded-3xl p-10 text-center text-muted-foreground">{error}</div>;
  }

  const activePage = pages.find((p) => p.id === activePageId) ?? pages[0];
  const z = zoom ?? 1;

  return (
    <div className="glass flex h-[calc(100vh-7rem)] min-h-[560px] flex-col overflow-hidden rounded-3xl">
      {/* toolbar */}
      <div className="flex items-center gap-1 overflow-x-auto border-b px-2 py-1.5">
        {TOOLS.map((t) => (
          <ToolButton key={t.id} label={`${t.label} (${t.key.toUpperCase()})`} active={tool === t.id && !pendingImage} onClick={() => pickTool(t.id)}>
            <t.icon size={18} />
          </ToolButton>
        ))}
        <ToolButton label="Add image" active={!!pendingImage && !pendingImage.file.name.startsWith("signature")} onClick={() => imageInput.current?.click()}>
          <ImagePlus size={18} />
        </ToolButton>
        <ToolButton label="Add signature" onClick={() => setSigning(true)}>
          <Signature size={18} />
        </ToolButton>
        <input ref={imageInput} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={onImageFile} />

        <span className="mx-1 h-6 w-px shrink-0 bg-border" />
        <ToolButton label="Undo (Ctrl+Z)" disabled={!state.past.length} onClick={() => dispatch({ type: "undo" })}><Undo2 size={18} /></ToolButton>
        <ToolButton label="Redo (Ctrl+Y)" disabled={!state.future.length} onClick={() => dispatch({ type: "redo" })}><Redo2 size={18} /></ToolButton>
        <span className="mx-1 h-6 w-px shrink-0 bg-border" />
        <ToolButton label="Zoom out" onClick={() => setZoom((v) => clamp((v ?? 1) - 0.1, 0.3, 3))}><Minus size={18} /></ToolButton>
        <span className="w-12 shrink-0 text-center text-xs tabular-nums text-muted-foreground">{Math.round(z * 100)}%</span>
        <ToolButton label="Zoom in" onClick={() => setZoom((v) => clamp((v ?? 1) + 0.1, 0.3, 3))}><Plus size={18} /></ToolButton>

        <div className="ml-auto flex shrink-0 items-center gap-2 pl-2">
          <span className="hidden text-xs text-muted-foreground sm:inline">{dirty ? "Unsaved changes" : "No changes yet"}</span>
          <Button onClick={save} disabled={!dirty || saving || !doc} className="rounded-xl">
            {saving ? <Loader2 className="animate-spin" /> : <Save />}
            Save PDF
          </Button>
        </div>
      </div>

      <div className="flex min-h-0 flex-1">
        {/* pages */}
        <aside className="hidden w-40 shrink-0 flex-col gap-3 overflow-y-auto border-r p-3 md:flex" aria-label="Pages">
          {pages.map((p, i) => (
            <div key={p.id} className={cn("group rounded-xl p-1.5", activePage?.id === p.id && "bg-primary/10")}>
              <button type="button" onClick={() => scrollToPage(p.id)} className="flex w-full items-center justify-center" aria-label={`Go to page ${i + 1}`}>
                <div className="relative bg-white shadow ring-1 ring-black/10 transition-transform"
                  style={{ width: 96, aspectRatio: `${p.width} / ${p.height}`, transform: `rotate(${p.rotate}deg) scale(${p.rotate % 180 ? Math.min(1, 96 / ((96 * p.height) / p.width)) : 1})` }}>
                  {p.source > 0 && doc && <PageCanvas doc={doc} pageNumber={p.source} className="absolute inset-0 h-full w-full" />}
                </div>
              </button>
              <div className="mt-1.5 flex items-center justify-between">
                <span className="text-xs text-muted-foreground">{i + 1}</span>
                <div className="flex opacity-70 transition-opacity group-hover:opacity-100">
                  <button type="button" title="Move up" aria-label="Move page up" disabled={i === 0} onClick={() => dispatch({ type: "movePage", from: i, to: i - 1 })} className="rounded p-0.5 hover:bg-accent disabled:opacity-30"><ArrowUp size={13} /></button>
                  <button type="button" title="Move down" aria-label="Move page down" disabled={i === pages.length - 1} onClick={() => dispatch({ type: "movePage", from: i, to: i + 1 })} className="rounded p-0.5 hover:bg-accent disabled:opacity-30"><ArrowDown size={13} /></button>
                  <button type="button" title="Rotate left" aria-label="Rotate page left" onClick={() => dispatch({ type: "rotatePage", id: p.id, delta: -90 })} className="rounded p-0.5 hover:bg-accent"><RotateCcw size={13} /></button>
                  <button type="button" title="Rotate right" aria-label="Rotate page right" onClick={() => dispatch({ type: "rotatePage", id: p.id, delta: 90 })} className="rounded p-0.5 hover:bg-accent"><RotateCw size={13} /></button>
                </div>
              </div>
              <div className="mt-0.5 flex justify-end opacity-70 transition-opacity group-hover:opacity-100">
                <button type="button" title="Duplicate page" aria-label="Duplicate page" onClick={() => dispatch({ type: "duplicatePage", id: p.id })} className="rounded p-0.5 hover:bg-accent"><Copy size={13} /></button>
                <button type="button" title="Insert blank page after" aria-label="Insert blank page after" onClick={() => dispatch({ type: "insertBlank", afterId: p.id })} className="rounded p-0.5 hover:bg-accent"><FilePlus2 size={13} /></button>
                <button type="button" title="Delete page" aria-label="Delete page" disabled={pages.length <= 1} onClick={() => dispatch({ type: "removePage", id: p.id })} className="rounded p-0.5 text-destructive hover:bg-accent disabled:opacity-30"><Trash2 size={13} /></button>
              </div>
            </div>
          ))}
        </aside>

        {/* canvas */}
        <div ref={scrollRef} className="min-w-0 flex-1 overflow-auto bg-muted/50 p-6">
          {!doc || zoom == null ? (
            <div className="flex h-full items-center justify-center text-muted-foreground"><Loader2 className="animate-spin" /></div>
          ) : (
            <div className="mx-auto flex w-fit flex-col items-center gap-6">
              {pages.map((p, i) => (
                <EditorPageView
                  key={p.id}
                  page={p}
                  index={i}
                  doc={doc}
                  objects={objectsByPage.get(p.id) ?? []}
                  zoom={z}
                  tool={tool}
                  style={style}
                  selectedId={selectedId}
                  editingId={editingId}
                  images={images}
                  pendingImage={pendingImage}
                  dispatch={dispatch}
                  onSelect={setSelectedId}
                  onEdit={(id) => {
                    setEditingId(id);
                    // an empty text box that loses focus is removed
                    if (id == null && editingId) {
                      const o = objects.find((x) => x.id === editingId);
                      if (o && o.type === "text" && !o.text.trim()) dispatch({ type: "removeObject", id: o.id });
                    }
                  }}
                  onPlaced={() => {
                    setPendingImage(null);
                    setTool("select");
                  }}
                  onActive={setActivePageId}
                />
              ))}
            </div>
          )}
        </div>

        {/* properties */}
        <aside className="hidden w-60 shrink-0 overflow-y-auto border-l p-4 lg:block" aria-label="Properties">
          <PropertiesPanel tool={tool} style={style} setStyle={setStyle} selected={selected} update={update} dispatch={dispatch} />
        </aside>
      </div>

      <SignaturePad open={signing} onOpenChange={setSigning}
        onDone={({ blob, width, height }) => addImage(new File([blob], "signature.png", { type: "image/png" }), width / 2, height / 2)} />
    </div>
  );
};

export default PdfEditor;
