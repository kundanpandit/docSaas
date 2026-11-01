import { useEffect } from "react";
import { motion, useMotionValue, useReducedMotion, useSpring, useTransform } from "framer-motion";
import { FileText, Files, Minimize2, Scissors, Sparkles } from "lucide-react";

import { cn } from "@/lib/utils";

const SPRING = { stiffness: 80, damping: 18, mass: 0.8 };

const PageSkeleton = ({ title, accent, children }) => (
  // near-opaque so the pages behind do not show through the text
  <div className="glass-strong absolute inset-0 rounded-2xl bg-card/90 p-5">
    <div className="flex items-center gap-2.5">
      <span className={cn("flex h-8 w-8 items-center justify-center rounded-lg text-white", accent)}>
        <FileText size={16} />
      </span>
      <div className="min-w-0">
        <p className="truncate text-sm font-medium">{title}</p>
        <p className="text-[11px] text-muted-foreground">PDF document</p>
      </div>
    </div>
    <div className="mt-5 space-y-2.5">
      <div className="h-2 w-11/12 rounded-full bg-foreground/10" />
      <div className="h-2 w-full rounded-full bg-foreground/10" />
      <div className="h-2 w-4/5 rounded-full bg-foreground/10" />
    </div>
    <div className="mt-5 h-24 rounded-xl bg-gradient-to-br from-primary/15 via-brand-2/10 to-transparent" />
    <div className="mt-5 space-y-2.5">
      <div className="h-2 w-full rounded-full bg-foreground/10" />
      <div className="h-2 w-2/3 rounded-full bg-foreground/10" />
    </div>
    {children}
  </div>
);

/** Back pages: plain frosted sheets, no text, so nothing competes with the front page. */
const Sheet = ({ tint }) => (
  <div className="glass absolute inset-0 rounded-2xl" style={{ backgroundImage: `linear-gradient(160deg, ${tint}, transparent 60%)` }} />
);

const Chip = ({ icon: Icon, label, className, delay }) => (
  <div
    className={cn("animate-float glass absolute flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium", className)}
    style={{ animationDelay: delay }}
  >
    <span className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/15 text-primary">
      <Icon size={14} />
    </span>
    {label}
  </div>
);

/** Decorative 3D stack of documents that turns gently toward the pointer. */
const DocStack3D = ({ className }) => {
  const reduceMotion = useReducedMotion();
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const rotateY = useSpring(useTransform(mx, [-1, 1], [-30, -12]), SPRING);
  const rotateX = useSpring(useTransform(my, [-1, 1], [20, 8]), SPRING);

  useEffect(() => {
    if (reduceMotion) return undefined;
    const onMove = (event) => {
      mx.set((event.clientX / window.innerWidth) * 2 - 1);
      my.set((event.clientY / window.innerHeight) * 2 - 1);
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    return () => window.removeEventListener("pointermove", onMove);
  }, [mx, my, reduceMotion]);

  return (
    <div aria-hidden="true" className={cn("relative mx-auto h-[400px] w-full max-w-[420px] select-none", className)} style={{ perspective: 1200 }}>
      <motion.div
        className="preserve-3d absolute left-1/2 top-1/2 h-[360px] w-[280px] -translate-x-1/2 -translate-y-1/2"
        style={{ rotateX: reduceMotion ? 14 : rotateX, rotateY: reduceMotion ? -21 : rotateY }}
      >
        {/* back pages */}
        <div className="absolute inset-0" style={{ transform: "translate3d(-46px, 30px, -90px)" }}>
          <Sheet tint="color-mix(in oklch, var(--chart-4) 25%, transparent)" />
        </div>
        <div className="absolute inset-0" style={{ transform: "translate3d(-22px, 14px, -45px)" }}>
          <Sheet tint="color-mix(in oklch, var(--brand-2) 25%, transparent)" />
        </div>

        {/* front page: a compression in progress */}
        <div className="absolute inset-0 shadow-lift rounded-2xl" style={{ transform: "translateZ(0)" }}>
          <PageSkeleton title="annual-report.pdf" accent="bg-brand-gradient">
            <div className="absolute inset-x-5 bottom-5">
              <div className="mb-1.5 flex items-center justify-between text-[11px] text-muted-foreground">
                <span className="flex items-center gap-1"><Minimize2 size={12} /> Compressing</span>
                <span className="font-medium text-success">−77%</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-foreground/10">
                <div
                  className="h-full w-3/4 rounded-full"
                  style={{
                    backgroundImage: "linear-gradient(90deg, var(--primary), var(--brand-2), var(--primary))",
                    backgroundSize: "200% 100%",
                    animation: "shimmer 2.4s linear infinite",
                  }}
                />
              </div>
            </div>
          </PageSkeleton>
        </div>

        {/* floating chips sit in front of the stack */}
        <div className="preserve-3d absolute inset-0" style={{ transform: "translateZ(80px)" }}>
          <Chip icon={Files} label="Merge" className="-left-16 top-8" delay="0s" />
          <Chip icon={Scissors} label="Split" className="-right-14 top-28" delay="-2s" />
          <Chip icon={Sparkles} label="AI chat" className="-left-10 bottom-16" delay="-4s" />
        </div>
      </motion.div>
    </div>
  );
};

export default DocStack3D;
