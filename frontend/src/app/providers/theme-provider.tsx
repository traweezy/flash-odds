import { createContext, useContext, useEffect, useMemo, useState } from "react";

type Theme = "light" | "dark";

type ThemeContextValue = {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggle: () => void;
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

const prefersDarkMedia = globalThis.matchMedia?.(
  "(prefers-color-scheme: dark)",
);

const getInitialTheme = (): Theme => {
  if (typeof localStorage !== "undefined") {
    const stored = localStorage.getItem("flashodds-theme");
    if (stored === "light" || stored === "dark") {
      return stored;
    }
  }
  return prefersDarkMedia?.matches ? "dark" : "dark";
};

export const ThemeProvider = ({ children }: { children: React.ReactNode }) => {
  const [theme, setThemeState] = useState<Theme>(getInitialTheme);

  useEffect(() => {
    const classList = document.documentElement.classList;
    classList.remove("light", "dark");
    classList.add(theme);
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("flashodds-theme", theme);
  }, [theme]);

  useEffect(() => {
    if (!prefersDarkMedia) {
      return;
    }
    const handler = (event: MediaQueryListEvent) => {
      setThemeState(event.matches ? "dark" : "light");
    };
    prefersDarkMedia.addEventListener("change", handler);
    return () => prefersDarkMedia.removeEventListener("change", handler);
  }, []);

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      setTheme: setThemeState,
      toggle: () =>
        setThemeState((current) => (current === "dark" ? "light" : "dark")),
    }),
    [theme],
  );

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
};

export const useTheme = (): ThemeContextValue => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used within ThemeProvider");
  }
  return context;
};
