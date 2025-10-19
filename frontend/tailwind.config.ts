import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: "#0f172a",
          elevated: "#111c33",
        },
        accent: {
          DEFAULT: "#f97316",
          muted: "#fb923c",
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
