import { Briefcase, Coins, LayoutGrid, Settings } from "lucide-react";

/** Workspace sidebar. "All tools" leaves the workspace for the public tool catalogue. */
export const navigationItems = [
  {
    title: "All tools",
    path: "/",
    icon: LayoutGrid,
    end: true,
  },
  {
    title: "My jobs",
    path: "/jobs",
    icon: Briefcase,
  },
  {
    title: "Credits",
    path: "/credits",
    icon: Coins,
  },
  {
    title: "Settings",
    path: "/settings",
    icon: Settings,
  },
];
