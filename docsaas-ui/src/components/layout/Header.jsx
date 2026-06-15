import { Bell, Info } from "lucide-react";
import toast from "react-hot-toast";

import ThemeToggle from "./ThemeToggle";
import UserMenu from "./UserMenu";

import { Button } from "@/components/ui/button";

const Header = () => {
  return (
    // <header className="w-full h-16 border-b px-6 flex items-center justify-between">
     <header className="h-16 w-full border-b px-6 flex items-center justify-between"> 
      <h2 className="font-semibold text-lg">
        Dashboard
      </h2>

      <div className="flex items-center gap-2">

        <Button
          variant="ghost"
          size="icon"
          onClick={() =>
            toast(
              "Free Plan • Credits: 950 • Storage: 12 MB / 100 MB"
            )
          }
        >
          <Info size={18} />
        </Button>

        <Button
          variant="ghost"
          size="icon"
          onClick={() =>
            toast(
              "Notifications are under development"
            )
          }
        >
          <Bell size={18} />
        </Button>

        <ThemeToggle />

        <UserMenu />

      </div>
      
    </header>
  );
};

export default Header;