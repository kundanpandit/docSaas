import { NavLink } from "react-router-dom";
import { motion } from "framer-motion";

import { cn } from "@/lib/utils";

const NavItem = ({ item, layoutId = "nav-active", compact = false }) => {
  const Icon = item.icon;

  return (
    <NavLink
      to={item.path}
      end={item.end}
      className={({ isActive }) =>
        cn(
          "relative flex items-center gap-3 rounded-xl text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
          compact ? "flex-col gap-1 px-2 py-1.5 text-[11px]" : "px-3 py-2.5",
          isActive ? "text-primary-foreground" : "text-muted-foreground hover:bg-accent hover:text-foreground"
        )
      }
    >
      {({ isActive }) => (
        <>
          {isActive && (
            <motion.span
              layoutId={layoutId}
              transition={{ type: "spring", stiffness: 450, damping: 34 }}
              className="bg-brand-gradient absolute inset-0 rounded-xl shadow-[0_6px_16px_-6px_var(--primary)]"
            />
          )}
          <Icon size={compact ? 18 : 17} className="relative" />
          <span className="relative">{item.title}</span>
        </>
      )}
    </NavLink>
  );
};

export default NavItem;
