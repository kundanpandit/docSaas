import { cn } from "@/lib/utils";

import PageCanvas from "./PageCanvas";
import { GROUP_COLORS } from "./effects";

let measureCtx;
/** Width of text in bold Helvetica per point of font size (like the server's PDFBox font metrics). */
const unitWidth = (text) => {
  measureCtx ??= document.createElement("canvas").getContext("2d");
  measureCtx.font = "bold 100px Helvetica, Arial, sans-serif";
  return measureCtx.measureText(text).width / 100;
};

/** Points -> container width units, so overlays scale with the page as it is drawn. */
const cq = (points, pageWidth) => `${(points / pageWidth) * 100}cqw`;

const Watermark = ({ watermark, width, height }) => {
  const span = watermark.diagonal ? Math.hypot(width, height) * 0.8 : width * 0.7;
  const size = Math.max(10, Math.min(span / unitWidth(watermark.text), Math.min(width, height) / 4));
  const angle = watermark.diagonal ? (Math.atan2(height, width) * 180) / Math.PI : 0;
  return (
    <div className="pointer-events-none absolute inset-0 flex items-center justify-center overflow-hidden">
      <span
        className="whitespace-nowrap font-bold"
        style={{
          fontFamily: "Helvetica, Arial, sans-serif",
          fontSize: cq(size, width),
          lineHeight: 1,
          color: `rgba(89, 89, 89, ${watermark.opacity})`,
          transform: `rotate(${-angle}deg)`,
        }}
      >
        {watermark.text}
      </span>
    </div>
  );
};

const PageLabel = ({ label, width }) => {
  const [vertical, horizontal] = label.position.split("-");
  const margin = cq(28, width);
  return (
    <span
      className="pointer-events-none absolute whitespace-nowrap"
      style={{
        fontFamily: "Helvetica, Arial, sans-serif",
        fontSize: cq(10, width),
        lineHeight: 1,
        color: "rgb(51, 51, 51)",
        [vertical === "top" ? "top" : "bottom"]: margin,
        ...(horizontal === "left" && { left: margin }),
        ...(horizontal === "right" && { right: margin }),
        ...(horizontal === "center" && { left: "50%", transform: "translateX(-50%)" }),
      }}
    >
      {label.text}
    </span>
  );
};

/**
 * One page as it will come out of the tool: the page (PDF canvas or image) inside a square cell,
 * with the tool's effect drawn on top. Rotation animates.
 */
const PageView = ({ page, effect = {}, className }) => {
  const width = effect.frame?.width ?? page.width;
  const height = effect.frame?.height ?? page.height;
  const color = effect.group != null ? GROUP_COLORS[effect.group % GROUP_COLORS.length] : null;

  return (
    <div className={cn("relative flex aspect-square w-full items-center justify-center", className)}>
      <div
        className={cn(
          "relative overflow-hidden bg-white shadow-md ring-1 ring-black/10 transition-[transform,filter,opacity] duration-500 ease-out",
          effect.dim && "opacity-35 grayscale"
        )}
        style={{
          aspectRatio: `${width} / ${height}`,
          [width >= height ? "width" : "height"]: "100%",
          transform: `rotate(${effect.rotate ?? 0}deg)`,
          containerType: "inline-size",
          outline: color ? `3px solid ${color}` : undefined,
          outlineOffset: 2,
        }}
      >
        {page.kind === "pdf" && <PageCanvas doc={page.doc} pageNumber={page.pageNumber} className="absolute inset-0 h-full w-full" />}
        {page.kind === "image" && (
          <div className="absolute inset-0" style={{ padding: effect.frame ? `${(effect.frame.margin / width) * 100}%` : 0 }}>
            <img src={page.url} alt="" className="h-full w-full object-contain" draggable={false} />
          </div>
        )}
        {effect.watermark && <Watermark watermark={effect.watermark} width={width} height={height} />}
        {effect.label && <PageLabel label={effect.label} width={width} />}
      </div>

      {effect.badge && (
        <span
          className="absolute left-1 top-1 rounded-full px-2 py-0.5 text-[11px] font-semibold text-white shadow"
          style={{ backgroundColor: color ?? "var(--primary)" }}
        >
          {effect.badge}
        </span>
      )}
    </div>
  );
};

export default PageView;
