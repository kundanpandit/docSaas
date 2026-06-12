import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";

import { storage } from "@/lib/storage";
import { setTheme } from "@/features/theme/slice/themeSlice";
import { THEME } from "@/constants/theme";
import { selectTheme } from "@/features/theme/selectors/themeSelectors";

const ThemeInitializer = ({ children }) => {
  const dispatch = useDispatch();
  const theme = useSelector(selectTheme);

 useEffect(() => {
  const savedTheme =
    storage.getTheme() || THEME.SYSTEM;

  dispatch(setTheme(savedTheme));
}, [dispatch]);

useEffect(() => {

  if (!theme) return;

  applyTheme(theme);

  if (theme === THEME.SYSTEM) {

    const mediaQuery = window.matchMedia(
      "(prefers-color-scheme: dark)"
    );

    const handleChange = () => {
      applyTheme(THEME.SYSTEM);
    };

    mediaQuery.addEventListener(
      "change",
      handleChange
    );

    return () => {
      mediaQuery.removeEventListener(
        "change",
        handleChange
      );
    };
  }

}, [theme]);
  return children;
};

function applyTheme(theme) {
  const html = document.documentElement;

  html.classList.remove("dark");

  if (theme === THEME.DARK) {
    html.classList.add("dark");
    return;
  }

  if (theme === THEME.SYSTEM) {
    const prefersDark =
      window.matchMedia(
        "(prefers-color-scheme: dark)"
      ).matches;

    if (prefersDark) {
      html.classList.add("dark");
    }
  }
}

export default ThemeInitializer;