/**
 * pdf.js, loaded on first use so pages without previews don't download it.
 * Documents are cached per file (and password) and the oldest are destroyed to bound memory.
 */

let pdfjsPromise;

export const loadPdfjs = () =>
  (pdfjsPromise ??= Promise.all([import("pdfjs-dist"), import("pdfjs-dist/build/pdf.worker.min.mjs?url")]).then(
    ([pdfjs, worker]) => {
      pdfjs.GlobalWorkerOptions.workerSrc = worker.default;
      return pdfjs;
    }
  ));

const MAX_OPEN = 6;
const open = new Map(); // key -> { blob, password, promise }

/** Opens a PDF Blob/File. Rejects with error.name === "PasswordException" for protected files. */
export const openPdf = async (blob, password = "") => {
  for (const [key, entry] of open) {
    if (entry.blob === blob && entry.password === password) {
      // refresh LRU position
      open.delete(key);
      open.set(key, entry);
      return entry.promise;
    }
  }

  const pdfjs = await loadPdfjs();
  const promise = blob.arrayBuffer().then((data) =>
    pdfjs.getDocument({ data, password: password || undefined, isEvalSupported: false }).promise
  );
  const key = Symbol("pdf");
  open.set(key, { blob, password, promise });
  promise.catch(() => open.delete(key));

  while (open.size > MAX_OPEN) {
    const [oldest, entry] = open.entries().next().value;
    open.delete(oldest);
    entry.promise.then((doc) => doc.destroy()).catch(() => {});
  }
  return promise;
};

export const isPdf = (file) => file?.type === "application/pdf" || /\.pdf$/i.test(file?.name ?? "");

export const isImage = (file) =>
  /^image\/(png|jpeg|webp)$/.test(file?.type ?? "") || /\.(png|jpe?g|webp)$/i.test(file?.name ?? "");
