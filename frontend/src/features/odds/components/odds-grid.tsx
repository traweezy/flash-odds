import { memo, useCallback, useEffect, useMemo, useState } from "react";
import type { ColDef, GridApi, GridReadyEvent } from "ag-grid-community";
import { AgGridReact } from "ag-grid-react";

import type { OddsRow } from "@shared/types/odds";
import { formatAmericanOdds, formatLine } from "@shared/lib/odds";
import { formatShortDateTime } from "@shared/lib/time";
import { useOddsFilters } from "../state/filters.store";

import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";

export type OddsGridProps = {
  rows: OddsRow[];
};

export const OddsGrid = memo<OddsGridProps>(({ rows }) => {
  const { quickFilter } = useOddsFilters();
  const [gridApi, setGridApi] = useState<GridApi | null>(null);

  const columnDefs = useMemo<ColDef[]>(
    () => [
      {
        field: "event",
        headerName: "Matchup",
        pinned: "left",
        minWidth: 260,
        cellRenderer: ({ value }: { value: string }) => (
          <span className="font-medium text-slate-100">{value}</span>
        ),
      },
      {
        field: "sport",
        headerName: "Sport",
        width: 110,
        cellClass: "uppercase text-xs text-slate-400 tracking-wide",
      },
      {
        field: "market",
        headerName: "Market",
        width: 120,
        cellClass: "uppercase text-xs text-slate-400 tracking-wide",
      },
      {
        field: "extra.participant",
        headerName: "Runner",
        width: 160,
        valueGetter: ({ data }) => data?.extra?.participant ?? "—",
      },
      {
        field: "line",
        headerName: "Line",
        width: 100,
        valueFormatter: ({ value }) =>
          formatLine(typeof value === "number" ? value : null),
      },
      {
        field: "price",
        headerName: "Price",
        width: 100,
        valueFormatter: ({ value }) =>
          formatAmericanOdds(typeof value === "number" ? value : null),
      },
      {
        field: "book",
        headerName: "Book",
        width: 140,
      },
      {
        field: "startsAt",
        headerName: "Start",
        width: 160,
        valueFormatter: ({ value }) =>
          typeof value === "string" ? formatShortDateTime(value) : "—",
      },
      {
        field: "updatedAt",
        headerName: "Updated",
        width: 160,
        valueFormatter: ({ value }) =>
          typeof value === "string" ? formatShortDateTime(value) : "—",
      },
    ],
    [],
  );

  const onGridReady = useCallback(
    (event: GridReadyEvent) => {
      setGridApi(event.api);
      event.api.setQuickFilter(quickFilter);
    },
    [quickFilter],
  );

  useEffect(() => {
    gridApi?.setQuickFilter(quickFilter);
  }, [gridApi, quickFilter]);

  return (
    <div className="ag-theme-quartz h-full w-full">
      <AgGridReact
        rowData={rows}
        columnDefs={columnDefs}
        animateRows
        deltaRowDataMode
        getRowId={({ data }) => data.id}
        domLayout="normal"
        suppressAggFuncInHeader
        onGridReady={onGridReady}
      />
    </div>
  );
});

OddsGrid.displayName = "OddsGrid";
