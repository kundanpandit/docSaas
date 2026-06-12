import NavItem from "./NavItem";
import { navigationItems } from "@/lib/navigation";

const Sidebar = () => {
  return (
    <aside className="hidden md:flex w-64 border-r bg-background flex-col">
      
      <div className="border-b p-6">
        <h1 className="text-xl font-bold">
          DocSaaS
        </h1>
      </div>

      <nav className="flex-1 p-4 space-y-2">
        {navigationItems.map((item) => (
          <NavItem
            key={item.path}
            item={item}
          />
        ))}
      </nav>
    </aside>
  );
};

export default Sidebar;