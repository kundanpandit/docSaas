import { useRef } from "react";
import { motion, useMotionTemplate, useMotionValue, useReducedMotion, useSpring, useTransform } from "framer-motion";

import { cn } from "@/lib/utils";

const SPRING = { stiffness: 220, damping: 22, mass: 0.6 };

/**
 * Card that tilts toward the pointer in 3D, with a soft light glare.
 * Falls back to a flat card for touch devices and reduced-motion users.
 */
const TiltCard = ({ className, children, max = 10, glare = true, as = "div", ...props }) => {
  const ref = useRef(null);
  const reduceMotion = useReducedMotion();

  const px = useMotionValue(0.5);
  const py = useMotionValue(0.5);
  const rotateX = useSpring(useTransform(py, [0, 1], [max, -max]), SPRING);
  const rotateY = useSpring(useTransform(px, [0, 1], [-max, max]), SPRING);
  const glareX = useTransform(px, (v) => `${v * 100}%`);
  const glareY = useTransform(py, (v) => `${v * 100}%`);
  const glareBg = useMotionTemplate`radial-gradient(circle at ${glareX} ${glareY}, oklch(1 0 0 / 0.35), transparent 55%)`;

  const handleMove = (event) => {
    if (reduceMotion || event.pointerType !== "mouse" || !ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    px.set((event.clientX - rect.left) / rect.width);
    py.set((event.clientY - rect.top) / rect.height);
  };

  const reset = () => {
    px.set(0.5);
    py.set(0.5);
  };

  const Component = motion[as] ?? motion.div;

  return (
    <div style={{ perspective: 900 }} className="h-full">
      <Component
        ref={ref}
        onPointerMove={handleMove}
        onPointerLeave={reset}
        style={reduceMotion ? undefined : { rotateX, rotateY, transformStyle: "preserve-3d" }}
        whileHover={reduceMotion ? undefined : { y: -4 }}
        transition={{ type: "spring", ...SPRING }}
        className={cn("group relative h-full will-change-transform", className)}
        {...props}
      >
        {children}
        {glare && !reduceMotion && (
          <motion.span
            aria-hidden="true"
            style={{ background: glareBg }}
            className="pointer-events-none absolute inset-0 rounded-[inherit] opacity-0 mix-blend-soft-light transition-opacity duration-300 group-hover:opacity-100"
          />
        )}
      </Component>
    </div>
  );
};

export default TiltCard;
