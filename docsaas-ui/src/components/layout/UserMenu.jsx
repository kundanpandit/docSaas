import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Briefcase, Coins, LayoutGrid, LogOut, Settings } from "lucide-react";

import { logout } from "@/features/auth/slice/authSlice";
import { selectUser } from "@/features/auth/selectors/authSelectors";
import { storage } from "@/lib/storage";

import {
  Avatar,
  AvatarFallback,
} from "@/components/ui/avatar";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const UserMenu = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const user = useSelector(selectUser);

  const initials = user?.fullName
    ?.split(" ")
    ?.map((name) => name[0])
    ?.join("")
    ?.substring(0, 2)
    ?.toUpperCase();

  const handleLogout = () => {
    storage.removeToken();
    storage.removeUser();

    dispatch(logout());

    navigate("/", { replace: true });
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button aria-label="Account menu" className="rounded-full focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
          <Avatar>
            <AvatarFallback className="bg-brand-gradient text-xs font-semibold text-white">
              {initials || "U"}
            </AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        className="surface-solid w-64 rounded-2xl p-1.5"
      >
        <div className="px-3 py-2">
          <p className="font-medium">
            {user?.fullName}
          </p>

          <p className="text-sm text-muted-foreground">
            {user?.email}
          </p>
        </div>

        <DropdownMenuSeparator />

        <DropdownMenuItem onClick={() => navigate("/")} className="rounded-lg">
          <LayoutGrid className="mr-2 h-4 w-4" />
          All tools
        </DropdownMenuItem>

        <DropdownMenuItem onClick={() => navigate("/jobs")} className="rounded-lg">
          <Briefcase className="mr-2 h-4 w-4" />
          My jobs
        </DropdownMenuItem>

        <DropdownMenuItem onClick={() => navigate("/credits")} className="rounded-lg">
          <Coins className="mr-2 h-4 w-4" />
          Credits
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={() =>
            navigate("/settings")
          }
        >
          <Settings className="mr-2 h-4 w-4" />
          Settings
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={handleLogout}
        >
          <LogOut className="mr-2 h-4 w-4" />
          Logout
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default UserMenu;