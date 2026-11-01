import { useEffect, useRef, useState } from "react";

/**
 * Renders one PDF page into a canvas filling its parent, only once it scrolls into view.
 * Re-renders at a higher resolution when the displayed size grows (e.g. the enlarged view).
 */
const PageCanvas = ({ doc, pageNumber, className }) => {
  const canvasRef = useRef(null);
  const [visible, setVisible] = useState(false);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;
    const io = new IntersectionObserver(([entry]) => entry.isIntersecting && setVisible(true), { rootMargin: "200px" });
    const ro = new ResizeObserver(([entry]) => setWidth((w) => Math.max(w, Math.round(entry.contentRect.width))));
    io.observe(canvas);
    ro.observe(canvas);
    return () => {
      io.disconnect();
      ro.disconnect();
    };
  }, []);

  useEffect(() => {
    if (!visible || !doc || !width) return undefined;
    let task;
    let cancelled = false;

    doc.getPage(pageNumber).then((page) => {
      if (cancelled) return;
      const base = page.getViewport({ scale: 1 });
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      const viewport = page.getViewport({ scale: (width * ratio) / base.width });
      const canvas = canvasRef.current;
      if (!canvas) return;
      canvas.width = Math.floor(viewport.width);
      canvas.height = Math.floor(viewport.height);
      task = page.render({ canvasContext: canvas.getContext("2d"), viewport });
      task.promise.catch(() => {});
    });

    return () => {
      cancelled = true;
      task?.cancel();
    };
  }, [doc, pageNumber, visible, width]);

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />;
};

export default PageCanvas;
