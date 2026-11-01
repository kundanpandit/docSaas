import NavItem from "./NavItem";
import { navigationItems } from "@/lib/navigation";

/** Bottom tab bar for the workspace on small screens. */
const MobileSidebar = () => (
  <nav
    aria-label="Workspace"
    className="glass-strong fixed inset-x-3 bottom-3 z-40 grid grid-cols-5 gap-1 rounded-2xl p-1.5 md:hidden"
  >
    {navigationItems.map((item) => (
      <NavItem key={item.path} item={item} layoutId="mobile-nav-active" compact />
    ))}
  </nav>
);

export default MobileSidebar;
