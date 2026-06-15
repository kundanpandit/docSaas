import { Moon, Sun } from "lucide-react";
import { useDispatch, useSelector } from "react-redux";

import { setTheme } from "@/features/theme/slice/themeSlice";
import { selectTheme } from "@/features/theme/selectors/themeSelectors";
import { storage } from "@/lib/storage";

import { Button } from "@/components/ui/button";

const ThemeToggle = () => {
  const dispatch = useDispatch();

  const theme = useSelector(selectTheme);

  const isDark = theme === "DARK";

  const handleToggle = () => {
    const newTheme = isDark
      ? "LIGHT"
      : "DARK";

    storage.setTheme(newTheme);

    dispatch(setTheme(newTheme));
  };

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={handleToggle}
    >
      {isDark ? (
        <Sun size={18} />
      ) : (
        <Moon size={18} />
      )}
    </Button>
  );
};

export default ThemeToggle;