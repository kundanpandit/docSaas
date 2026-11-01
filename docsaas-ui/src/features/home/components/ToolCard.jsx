import { Link } from "react-router-dom";
import { useSelector } from "react-redux";
import { ArrowUpRight, Lock } from "lucide-react";

import TiltCard from "@/components/effects/TiltCard";
import { selectIsAuthenticated } from "@/features/auth/selectors/authSelectors";
import { cn } from "@/lib/utils";

const ToolCard = ({ id, title, description, icon: Icon, available, category }) => {
  const locked = useSelector((state) => !selectIsAuthenticated(state)) && category === "ai";
  return (
  <Link
    to={`/tools/${id}`}
    className="block h-full rounded-2xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
  >
    <TiltCard max={8} className="glass flex flex-col rounded-2xl p-4 transition-shadow sm:p-5 duration-300 hover:shadow-lift">
      {/* layers are pushed forward in 3D so they float above the card on tilt */}
      <div className="flex items-start justify-between" style={{ transform: "translateZ(30px)" }}>
        <span
          className={cn(
            "flex h-10 w-10 items-center justify-center rounded-xl transition-transform sm:h-11 sm:w-11 duration-300 group-hover:scale-110",
            available ? "bg-brand-gradient text-white shadow-[0_8px_20px_-8px_var(--primary)]" : "bg-muted text-muted-foreground"
          )}
        >
          {Icon && <Icon size={20} />}
        </span>

        {available && locked ? (
          <span className="flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-foreground">
            <Lock size={11} /> Log in
          </span>
        ) : available ? (
          <ArrowUpRight
            size={18}
            className="text-muted-foreground transition-all duration-300 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-primary"
          />
        ) : (
          <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-foreground">Soon</span>
        )}
      </div>

      <div className="mt-4" style={{ transform: "translateZ(20px)" }}>
        <h3 className="text-sm font-semibold sm:text-base">{title}</h3>
        <p className="mt-1 line-clamp-2 text-xs text-muted-foreground sm:text-sm">{description}</p>
      </div>
    </TiltCard>
  </Link>
  );
};

export default ToolCard;
