import { useId } from "react";
import { Link } from "react-router-dom";

import { cn } from "@/lib/utils";

export const TAGLINE = "Smart document tools";

/**
 * The DocSaaS mark: two stacked pages with a folded corner on the brand gradient.
 * Same artwork as public/favicon.svg — keep them in sync.
 */
export const LogoMark = ({ className }) => {
  const id = useId();
  return (
    <svg viewBox="0 0 40 40" className={className} aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id={`${id}-g`} x1="4" y1="2" x2="36" y2="38" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#7B4DFF" />
          <stop offset="1" stopColor="#1FB5D6" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" fill={`url(#${id}-g)`} />
      {/* back page */}
      <rect x="9" y="11" width="15" height="20" rx="2.5" fill="#fff" fillOpacity=".38" transform="rotate(-8 16.5 21)" />
      {/* front page with folded corner */}
      <path d="M16 9h8.6L31 15.4V29a2.5 2.5 0 0 1-2.5 2.5H16A2.5 2.5 0 0 1 13.5 29V11.5A2.5 2.5 0 0 1 16 9Z" fill="#fff" />
      <path d="M24.6 9v4.4a2 2 0 0 0 2 2H31" fill="none" stroke={`url(#${id}-g)`} strokeWidth="1.6" strokeLinejoin="round" />
      {/* text lines */}
      <rect x="17" y="19" width="10.5" height="1.9" rx=".95" fill={`url(#${id}-g)`} />
      <rect x="17" y="23" width="7.5" height="1.9" rx=".95" fill={`url(#${id}-g)`} fillOpacity=".7" />
      <rect x="17" y="27" width="9" height="1.9" rx=".95" fill={`url(#${id}-g)`} fillOpacity=".45" />
    </svg>
  );
};

/**
 * @param compact  mark only
 * @param tagline  show the tagline under the name (hidden on very small screens)
 */
const Logo = ({ className, to = "/", compact = false, tagline = true }) => (
  <Link
    to={to}
    aria-label="DocSaaS home"
    className={cn("group flex items-center gap-2.5 rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50", className)}
  >
    <LogoMark className="h-9 w-9 shrink-0 drop-shadow-[0_6px_14px_rgba(123,77,255,0.35)] transition-transform duration-300 group-hover:-rotate-3 group-hover:scale-105" />
    {!compact && (
      <span className="flex flex-col leading-none">
        <span className="text-[1.15rem] font-bold tracking-tight">
          Doc<span className="text-gradient">SaaS</span>
        </span>
        {tagline && (
          <span className="mt-1 hidden whitespace-nowrap text-[10.5px] font-medium tracking-wide text-muted-foreground min-[400px]:block">
            {TAGLINE}
          </span>
        )}
      </span>
    )}
  </Link>
);

export default Logo;
