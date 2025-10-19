import { memo, useMemo } from "react";
import { Moon, RefreshCw, Sun } from "lucide-react";

import { useTheme } from "@app/providers/theme-provider";
import { InputField } from "@shared/components/input-field";
import { SelectField } from "@shared/components/select-field";
import type { OddsRow } from "@shared/types/odds";

import { useOddsFilters } from "../state/filters.store";

export type OddsToolbarProps = {
  rows: OddsRow[];
  status: string;
  onRefresh: () => void;
};

export const OddsToolbar = memo(
  ({ rows, status, onRefresh }: OddsToolbarProps) => {
    const { theme, toggle } = useTheme();
    const {
      sport,
      market,
      region,
      quickFilter,
      setMarket,
      setRegion,
      setSport,
      setQuickFilter,
      reset,
    } = useOddsFilters();

    const sports = useMemo(() => {
      const values = new Set(rows.map((row) => row.sport));
      return ["all", ...Array.from(values)].map((value) => ({
        value,
        label: value === "all" ? "All sports" : value.toUpperCase(),
      }));
    }, [rows]);

    const markets = useMemo(() => {
      const values = new Set(rows.map((row) => row.market));
      return ["all", ...Array.from(values)].map((value) => ({
        value,
        label: value === "all" ? "All markets" : value.toUpperCase(),
      }));
    }, [rows]);

    return (
      <div className="flex flex-col gap-4 rounded-lg border border-slate-800 bg-slate-900/60 p-4 shadow-lg shadow-slate-950/30">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-100">
              FlashOdds 25
            </h1>
            <p className="text-sm text-slate-400">
              Live price flashes with smart fallbacks
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-slate-800 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-slate-300">
              {status}
            </span>
            <button
              type="button"
              onClick={reset}
              className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-accent"
            >
              Reset
            </button>
            <button
              type="button"
              onClick={onRefresh}
              className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-accent"
            >
              <RefreshCw className="h-4 w-4" aria-hidden />
              Refresh
            </button>
            <button
              type="button"
              onClick={toggle}
              aria-label="Toggle theme"
              className="inline-flex items-center justify-center rounded-full border border-slate-700 bg-slate-900 p-2 text-slate-100 transition hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-accent"
            >
              {theme === "dark" ? (
                <Sun className="h-5 w-5" aria-hidden />
              ) : (
                <Moon className="h-5 w-5" aria-hidden />
              )}
            </button>
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-4">
          <SelectField
            id="sport"
            label="Sport"
            value={sport}
            onChange={setSport}
            options={sports}
          />
          <SelectField
            id="market"
            label="Market"
            value={market}
            onChange={setMarket}
            options={markets}
          />
          <SelectField
            id="region"
            label="Region"
            value={region}
            onChange={setRegion}
            options={[
              { value: "us", label: "United States" },
              { value: "uk", label: "United Kingdom" },
              { value: "eu", label: "Europe" },
            ]}
          />
          <InputField
            id="quick-filter"
            label="Quick filter"
            value={quickFilter}
            placeholder="Find a team, book, or line"
            onChange={setQuickFilter}
          />
        </div>
      </div>
    );
  },
);

OddsToolbar.displayName = "OddsToolbar";
