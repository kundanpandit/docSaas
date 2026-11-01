import { useLocation } from "react-router-dom";

import UserMenu from "./UserMenu";
import Logo from "@/components/common/Logo";
import HeaderThemeToggle from "@/features/theme/components/HeaderThemeToggle";
import UsageWidget from "@/features/usage/components/UsageWidget";
import { navigationItems } from "@/lib/navigation";

const Header = () => {
  const { pathname } = useLocation();
  const current = navigationItems.find((item) => !item.end && pathname.startsWith(item.path));

  return (
    <header className="glass sticky top-3 z-30 flex h-14 items-center justify-between rounded-2xl pl-4 pr-2 sm:top-4">
      <div className="flex items-center gap-3">
        <Logo compact className="md:hidden" />
        <h2 className="font-semibold">{current?.title ?? "Workspace"}</h2>
      </div>

      <div className="flex items-center gap-1.5">
        <UsageWidget />
        <HeaderThemeToggle />
        <UserMenu />
      </div>
    </header>
  );
};

export default Header;
