import { Suspense, memo, useMemo } from "react";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import { useOddsFeed } from "@features/odds/api/use-odds-feed";
import { OddsGrid } from "@features/odds/components/odds-grid";
import { OddsToolbar } from "@features/odds/components/odds-toolbar";
import { useOddsFilters } from "@features/odds/state/filters.store";
import { Skeleton } from "@shared/components/skeleton";

export const AppShell = memo(() => {
  const { rows, status, metadata, refresh } = useOddsFeed();
  const { sport, market } = useOddsFilters();

  const filteredRows = useMemo(
    () =>
      rows.filter((row) => {
        const sportMatch = sport === "all" || row.sport === sport;
        const marketMatch = market === "all" || row.market === market;
        return sportMatch && marketMatch;
      }),
    [rows, sport, market],
  );

  return (
    <div className="app-shell">
      <main className="mx-auto flex w-full max-w-[1200px] flex-1 flex-col gap-6 px-4 py-6">
        <OddsToolbar rows={rows} status={status} onRefresh={refresh} />
        <section className="flex flex-1 flex-col gap-4">
          <header className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-sm text-slate-400">
              <span className="font-semibold text-accent">
                {metadata.total}
              </span>{" "}
              live rows · status {status}
            </div>
          </header>
          <div className="flex-1 rounded-lg border border-slate-800 bg-slate-900/60 p-2">
            <Suspense
              fallback={
                <div className="flex flex-col gap-3">
                  <Skeleton className="h-10" />
                  <Skeleton className="h-48" />
                </div>
              }
            >
              {filteredRows.length === 0 ? (
                <div className="flex h-64 items-center justify-center rounded-lg border border-dashed border-slate-700 text-slate-400">
                  Waiting for live odds…
                </div>
              ) : (
                <OddsGrid rows={filteredRows} />
              )}
            </Suspense>
          </div>
        </section>
      </main>
      <ReactQueryDevtools buttonPosition="bottom-left" />
    </div>
  );
});

AppShell.displayName = "AppShell";
