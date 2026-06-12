import { THEME } from "@/constants/theme";

export const applyTheme = (theme) => {
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
};