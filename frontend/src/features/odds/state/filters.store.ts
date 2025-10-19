import { create } from "zustand";
import { devtools } from "zustand/middleware";

export type OddsFiltersState = {
  sport: string;
  market: string;
  region: string;
  quickFilter: string;
  setSport: (value: string) => void;
  setMarket: (value: string) => void;
  setRegion: (value: string) => void;
  setQuickFilter: (value: string) => void;
  reset: () => void;
};

const DEFAULT_STATE = {
  sport: "all",
  market: "all",
  region: "us",
  quickFilter: "",
} as const;

export const useOddsFilters = create<OddsFiltersState>()(
  devtools(
    (set) => ({
      ...DEFAULT_STATE,
      setSport: (sport) => set({ sport }),
      setMarket: (market) => set({ market }),
      setRegion: (region) => set({ region }),
      setQuickFilter: (quickFilter) => set({ quickFilter }),
      reset: () => set({ ...DEFAULT_STATE }),
    }),
    { name: "odds-filters" },
  ),
);
