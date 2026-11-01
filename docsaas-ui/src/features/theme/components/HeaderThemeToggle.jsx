import { Moon, Sun } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { useDispatch, useSelector } from "react-redux";

import { setTheme } from "@/features/theme/slice/themeSlice";
import { selectTheme } from "@/features/theme/selectors/themeSelectors";
import { storage } from "@/lib/storage";
import { THEME } from "@/constants/theme";
import { cn } from "@/lib/utils";

const isDarkNow = (theme) =>
  theme === THEME.DARK ||
  (theme === THEME.SYSTEM && window.matchMedia("(prefers-color-scheme: dark)").matches);

const HeaderThemeToggle = ({ className }) => {
  const dispatch = useDispatch();
  const theme = useSelector(selectTheme);
  // Resolve SYSTEM to what is actually on screen, so one click always flips it.
  const isDark = isDarkNow(theme);

  const toggleTheme = () => {
    const next = isDark ? THEME.LIGHT : THEME.DARK;
    storage.setTheme(next);
    dispatch(setTheme(next));
  };

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      className={cn(
        "relative flex h-9 w-9 items-center justify-center overflow-hidden rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
        className
      )}
    >
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={isDark ? "sun" : "moon"}
          initial={{ y: 12, opacity: 0, rotate: -45 }}
          animate={{ y: 0, opacity: 1, rotate: 0 }}
          exit={{ y: -12, opacity: 0, rotate: 45 }}
          transition={{ duration: 0.2 }}
        >
          {isDark ? <Sun size={17} /> : <Moon size={17} />}
        </motion.span>
      </AnimatePresence>
    </button>
  );
};

export default HeaderThemeToggle;
