import { Moon, Sun } from "lucide-react";
import { useDispatch, useSelector } from "react-redux";

import { setTheme } from "@/features/theme/slice/themeSlice";
import { selectTheme } from "@/features/theme/selectors/themeSelectors";
import { storage } from "@/lib/storage";

const ThemeSwitcher = () => {
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
      fixed
      bottom-10
      right-2
      sm:bottom-6
      sm:right-6

      z-50

      flex
      items-center

      h-10
      w-20

      sm:h-12
      sm:w-24

      rounded-full
      border
      border-border

      bg-card

      p-1

      shadow-lg
      hover:shadow-xl

      transition-all
      duration-300
    "
  >
    <div
      className={`
        absolute
        top-1
        left-1

        h-8
        w-8

        sm:h-10
        sm:w-10

        rounded-full

        bg-primary

        flex
        items-center
        justify-center

        transition-all
        duration-300

        ${
          isDark
            ? "translate-x-10 sm:translate-x-12"
            : "translate-x-0"
        }
      `}
    >
      {isDark ? (
        <Moon
          size={16}
          className="text-primary-foreground"
        />
      ) : (
        <Sun
          size={16}
          className="text-primary-foreground"
        />
      )}
    </div>

    <div className="flex w-full items-center justify-between px-2 sm:px-3">
      <Sun
        size={14}
        className="text-muted-foreground"
      />

      <Moon
        size={14}
        className="text-muted-foreground"
      />
    </div>
  </button>
);
};

export default ThemeSwitcher;