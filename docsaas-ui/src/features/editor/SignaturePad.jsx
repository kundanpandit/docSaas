import { useEffect, useRef, useState } from "react";
import { Eraser } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

const INKS = [
  { value: "#111111", label: "Black" },
  { value: "#1d4ed8", label: "Blue" },
];

/** Crops a canvas to its drawn pixels (plus a small margin) and returns a transparent PNG. */
const trimmedPng = (canvas) =>
  new Promise((resolve) => {
    const ctx = canvas.getContext("2d");
    const { width, height } = canvas;
    const { data } = ctx.getImageData(0, 0, width, height);
    let minX = width, minY = height, maxX = -1, maxY = -1;
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        if (data[(y * width + x) * 4 + 3] > 0) {
          minX = Math.min(minX, x);
          maxX = Math.max(maxX, x);
          minY = Math.min(minY, y);
          maxY = Math.max(maxY, y);
        }
      }
    }
    if (maxX < 0) return resolve(null);
    const pad = 6;
    const x = Math.max(0, minX - pad);
    const y = Math.max(0, minY - pad);
    const w = Math.min(width, maxX + pad) - x;
    const h = Math.min(height, maxY + pad) - y;
    const out = document.createElement("canvas");
    out.width = w;
    out.height = h;
    out.getContext("2d").drawImage(canvas, x, y, w, h, 0, 0, w, h);
    out.toBlob((blob) => resolve(blob && { blob, width: w, height: h }), "image/png");
  });

/** Draw a signature with mouse, pen or finger; returns it as a transparent PNG. */
const SignaturePad = ({ open, onOpenChange, onDone }) => {
  const canvasRef = useRef(null);
  const last = useRef(null);
  const [ink, setInk] = useState(INKS[0].value);
  const [empty, setEmpty] = useState(true);

  useEffect(() => {
    if (!open) return;
    // the canvas mounts with the dialog: size it for sharp lines on high-DPI screens
    requestAnimationFrame(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = canvas.clientWidth * ratio;
      canvas.height = canvas.clientHeight * ratio;
      setEmpty(true);
    });
  }, [open]);

  const pos = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const ratio = canvasRef.current.width / rect.width;
    return [(e.clientX - rect.left) * ratio, (e.clientY - rect.top) * ratio];
  };

  const down = (e) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    last.current = pos(e);
  };

  const move = (e) => {
    if (!last.current) return;
    const ctx = canvasRef.current.getContext("2d");
    const p = pos(e);
    const ratio = canvasRef.current.width / canvasRef.current.clientWidth;
    ctx.strokeStyle = ink;
    ctx.lineWidth = 2.5 * ratio * (e.pressure > 0 && e.pointerType === "pen" ? 0.5 + e.pressure : 1);
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    ctx.moveTo(...last.current);
    ctx.lineTo(...p);
    ctx.stroke();
    last.current = p;
    setEmpty(false);
  };

  const clear = () => {
    const canvas = canvasRef.current;
    canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    setEmpty(true);
  };

  const use = async () => {
    // read the pixels before the dialog (and its canvas) goes away, then close at once
    const pending = trimmedPng(canvasRef.current);
    onOpenChange(false);
    const result = await pending;
    if (result) onDone(result);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogTitle>Draw your signature</DialogTitle>
        <DialogDescription>Sign with your mouse, a pen or your finger. You can place and resize it on the page next.</DialogDescription>
        <canvas
          ref={canvasRef}
          className="h-44 w-full touch-none rounded-xl border-2 border-dashed bg-white"
          onPointerDown={down}
          onPointerMove={move}
          onPointerUp={() => (last.current = null)}
          onPointerLeave={() => (last.current = null)}
          aria-label="Signature drawing area"
        />
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            {INKS.map((c) => (
              <button key={c.value} type="button" onClick={() => setInk(c.value)} aria-label={`${c.label} ink`}
                className={cn("h-7 w-7 rounded-full ring-offset-2 ring-offset-background", ink === c.value && "ring-2 ring-primary")}
                style={{ backgroundColor: c.value }} />
            ))}
            <Button type="button" variant="ghost" size="sm" onClick={clear}>
              <Eraser /> Clear
            </Button>
          </div>
          <Button type="button" onClick={use} disabled={empty}>Use signature</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default SignaturePad;
