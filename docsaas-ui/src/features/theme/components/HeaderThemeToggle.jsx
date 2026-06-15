import { Moon, Sun } from "lucide-react";
import { useDispatch, useSelector } from "react-redux";

import { setTheme } from "@/features/theme/slice/themeSlice";
import { selectTheme } from "@/features/theme/selectors/themeSelectors";
import { storage } from "@/lib/storage";

const HeaderThemeToggle = () => {
  const dispatch = useDispatch();

  const theme = useSelector(selectTheme);

  const isDark = theme === "DARK";

  const toggleTheme = () => {
    const newTheme = isDark
      ? "LIGHT"
      : "DARK";

    storage.setTheme(newTheme);

    dispatch(
      setTheme(newTheme)
    );
  };

  return (
    <button
      onClick={toggleTheme}
      className="
        flex
        h-10
        w-10
        items-center
        justify-center
        rounded-full
        border
        transition-colors
        hover:bg-accent
      "
    >
      {isDark ? (
        <Sun size={18} />
      ) : (
        <Moon size={18} />
      )}
    </button>
  );
};

export default HeaderThemeToggle;