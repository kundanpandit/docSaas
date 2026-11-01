/**
 * Editor state: pages (output order) and objects drawn on them, with undo/redo.
 *
 * Coordinates are PDF points on the page as displayed before the user's extra rotation, origin
 * top-left, y down: exactly what the server's EditCanvas draws (see EditPdfProcessor).
 *
 * page   = { id, source (1-based page of the original, 0 for blank), width, height, rotate }
 * object = { id, pageId, type, ...fields }   (fields per type: see EditCanvas.java)
 */

let nextId = 1;
export const newId = (prefix) => `${prefix}${nextId++}`;

export const BLANK_A4 = { width: 595.28, height: 841.89 };

export const DEFAULT_STYLE = {
  color: "#111111",
  fill: "none",
  stroke: "#e11d48",
  highlight: "#ffe600",
  font: "helvetica",
  size: 16,
  bold: false,
  italic: false,
  strokeWidth: 2,
};

export const initialState = (pages) => ({
  present: { pages, objects: [] },
  past: [],
  future: [],
});

const MAX_HISTORY = 100;

const commit = (state, present) => ({
  present,
  past: [...state.past.slice(-MAX_HISTORY + 1), state.present],
  future: [],
});

/**
 * Actions:
 *  load {pages}         start over with these pages (no history)
 *  checkpoint           remember the current state for undo (start of a drag)
 *  addObject {object}   updateObject {id, patch, record}   removeObject {id}
 *  movePage {from,to}   rotatePage {id, delta}   removePage {id}   duplicatePage {id}   insertBlank {afterId}
 *  reorderObject {id, toFront}   undo   redo
 */
export const editorReducer = (state, action) => {
  const { pages, objects } = state.present;
  switch (action.type) {
    case "load":
      return initialState(action.pages);
    case "checkpoint":
      return commit(state, state.present);
    case "addObject":
      return commit(state, { pages, objects: [...objects, action.object] });
    case "updateObject": {
      const next = { pages, objects: objects.map((o) => (o.id === action.id ? { ...o, ...action.patch } : o)) };
      return action.record === false ? { ...state, present: next } : commit(state, next);
    }
    case "removeObject":
      return commit(state, { pages, objects: objects.filter((o) => o.id !== action.id) });
    case "reorderObject": {
      const obj = objects.find((o) => o.id === action.id);
      const rest = objects.filter((o) => o.id !== action.id);
      return commit(state, { pages, objects: action.toFront ? [...rest, obj] : [obj, ...rest] });
    }
    case "movePage": {
      if (action.to < 0 || action.to >= pages.length) return state;
      const next = [...pages];
      const [page] = next.splice(action.from, 1);
      next.splice(action.to, 0, page);
      return commit(state, { pages: next, objects });
    }
    case "rotatePage":
      return commit(state, {
        pages: pages.map((p) => (p.id === action.id ? { ...p, rotate: (((p.rotate + action.delta) % 360) + 360) % 360 } : p)),
        objects,
      });
    case "removePage":
      if (pages.length <= 1) return state;
      return commit(state, { pages: pages.filter((p) => p.id !== action.id), objects: objects.filter((o) => o.pageId !== action.id) });
    case "duplicatePage": {
      const index = pages.findIndex((p) => p.id === action.id);
      const copy = { ...pages[index], id: newId("p") };
      const copies = objects.filter((o) => o.pageId === action.id).map((o) => ({ ...o, id: newId("o"), pageId: copy.id }));
      const next = [...pages];
      next.splice(index + 1, 0, copy);
      return commit(state, { pages: next, objects: [...objects, ...copies] });
    }
    case "insertBlank": {
      const index = pages.findIndex((p) => p.id === action.afterId);
      const ref = pages[index];
      // same displayed size as the page it follows, so a blank page matches the document
      const blank = { id: newId("p"), source: 0, width: ref?.width ?? BLANK_A4.width, height: ref?.height ?? BLANK_A4.height, rotate: 0 };
      const next = [...pages];
      next.splice(index + 1, 0, blank);
      return commit(state, { pages: next, objects });
    }
    case "undo":
      if (!state.past.length) return state;
      return { present: state.past[state.past.length - 1], past: state.past.slice(0, -1), future: [state.present, ...state.future] };
    case "redo":
      if (!state.future.length) return state;
      return { present: state.future[0], past: [...state.past, state.present], future: state.future.slice(1) };
    default:
      return state;
  }
};

/**
 * The save request: the plan for EditPdfProcessor plus the files to upload
 * (the PDF first, then each distinct image once, referenced by index).
 */
export const buildSave = (pdfFile, { pages, objects }, images) => {
  const pageIndex = new Map(pages.map((p, i) => [p.id, i]));
  const imageFiles = [];
  const imageIndex = new Map();

  const planObjects = objects
    .filter((o) => pageIndex.has(o.pageId))
    .filter((o) => o.type !== "text" || o.text.trim())
    .map((o) => {
      const { pageId, imageId, ...fields } = o;
      delete fields.id;
      const out = { ...fields, page: pageIndex.get(pageId) };
      if (fields.type === "image") {
        if (!imageIndex.has(imageId)) {
          imageFiles.push(images.get(imageId));
          imageIndex.set(imageId, imageFiles.length);
        }
        out.image = imageIndex.get(imageId);
      }
      return round(out);
    });

  const plan = {
    pages: pages.map((p) =>
      p.source ? { source: p.source, rotate: p.rotate } : { blank: true, width: p.width, height: p.height, rotate: p.rotate }
    ),
    objects: planObjects,
  };
  return { files: [pdfFile, ...imageFiles], data: plan, changes: planObjects.length };
};

/** Numbers to 2 decimals: keeps the upload small without visible change. */
const round = (o) =>
  Object.fromEntries(
    Object.entries(o).map(([k, v]) => [
      k,
      typeof v === "number" ? Math.round(v * 100) / 100 : k === "points" ? v.map(([x, y]) => [Math.round(x * 100) / 100, Math.round(y * 100) / 100]) : v,
    ])
  );
