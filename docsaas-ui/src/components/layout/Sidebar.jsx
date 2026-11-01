import { Link } from "react-router-dom";
import { Sparkles } from "lucide-react";

import Logo from "@/components/common/Logo";
import NavItem from "./NavItem";
import { navigationItems } from "@/lib/navigation";

const Sidebar = () => {
  return (
    <aside className="glass sticky top-4 hidden h-[calc(100vh-2rem)] w-60 shrink-0 flex-col rounded-3xl p-4 md:flex">
      <Logo className="px-2 py-1" />

      <nav aria-label="Workspace" className="mt-8 flex-1 space-y-1">
        {navigationItems.map((item) => (
          <NavItem key={item.path} item={item} />
        ))}
      </nav>

      <Link
        to="/#tools"
        className="group rounded-2xl bg-primary/10 p-4 text-sm transition-colors hover:bg-primary/15"
      >
        <span className="flex items-center gap-2 font-medium text-primary">
          <Sparkles size={15} />
          All tools
        </span>
        <span className="mt-1 block text-xs text-muted-foreground">Browse every PDF and image tool.</span>
      </Link>
    </aside>
  );
};

export default Sidebar;
