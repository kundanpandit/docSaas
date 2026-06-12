import { Bell } from "lucide-react";

const Header = () => {
  return (
    <header className="h-16 border-b px-6 flex items-center justify-between">
      <h2 className="font-semibold">
        Dashboard
      </h2>

      <Bell size={20} />
    </header>
  );
};

export default Header;