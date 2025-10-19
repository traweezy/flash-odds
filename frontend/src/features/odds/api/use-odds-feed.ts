import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";

import { fetchJson } from "@shared/lib/fetch";
import type { OddsFrame, OddsRow } from "@shared/types/odds";

export type FeedStatus = "connecting" | "live" | "sse" | "polling" | "offline";

const WS_PATH = "/ws/odds";
const SSE_PATH = "/api/odds/stream";
const REST_PATH = "/api/odds";

const queryKey = ["odds", "rows"] as const;

const parseFrame = (data: unknown): OddsFrame | null => {
  if (!data || typeof data !== "object") {
    return null;
  }
  const frame = data as Partial<OddsFrame>;
  if (frame.type !== "snapshot" && frame.type !== "delta") {
    return null;
  }
  return frame as OddsFrame;
};

const applyFrame = (
  state: Map<string, OddsRow>,
  frame: OddsFrame,
): Map<string, OddsRow> => {
  if (frame.type === "snapshot") {
    const snapshot = new Map<string, OddsRow>();
    for (const change of frame.rows) {
      if (change.row) {
        snapshot.set(change.row.id, change.row);
      }
    }
    return snapshot;
  }
  const next = new Map(state);
  for (const change of frame.rows) {
    if (change.op === "remove") {
      next.delete(change.id);
    } else if (change.row) {
      next.set(change.row.id, change.row);
    }
  }
  return next;
};

const mapToSortedArray = (source: Map<string, OddsRow>): OddsRow[] =>
  Array.from(source.values()).sort((a, b) => a.id.localeCompare(b.id));

const indexRows = (rows: OddsRow[]): Map<string, OddsRow> => {
  const map = new Map<string, OddsRow>();
  for (const row of rows) {
    map.set(row.id, row);
  }
  return map;
};

export const useOddsFeed = () => {
  const cacheRef = useRef<Map<string, OddsRow>>(new Map());
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<FeedStatus>("connecting");

  const { data: rows = [], refetch } = useQuery({
    queryKey,
    queryFn: async () => {
      const payload = await fetchJson<OddsRow[]>(REST_PATH);
      cacheRef.current = indexRows(payload);
      return mapToSortedArray(cacheRef.current);
    },
    staleTime: 15_000,
    refetchOnWindowFocus: false,
    refetchInterval: false,
    initialData: [],
  });

  const setQueryRows = useCallback(
    (nextRows: Map<string, OddsRow>) => {
      const sorted = mapToSortedArray(nextRows);
      queryClient.setQueryData(queryKey, sorted);
    },
    [queryClient],
  );

  const handleFrame = useCallback(
    (frame: OddsFrame) => {
      cacheRef.current = applyFrame(cacheRef.current, frame);
      setQueryRows(cacheRef.current);
    },
    [setQueryRows],
  );

  useEffect(() => {
    let isActive = true;
    let ws: WebSocket | null = null;
    let es: EventSource | null = null;
    let pollTimer: number | null = null;

    const connectWebSocket = () => {
      try {
        const protocol =
          globalThis.location?.protocol === "https:" ? "wss" : "ws";
        const wsUrl = `${protocol}://${globalThis.location?.host ?? "localhost:8080"}${WS_PATH}`;
        ws = new WebSocket(wsUrl);
        ws.addEventListener("open", () => {
          if (!isActive) {
            return;
          }
          setStatus("live");
        });
        ws.addEventListener("message", (event) => {
          try {
            const payload = parseFrame(JSON.parse(event.data));
            if (payload) {
              handleFrame(payload);
            }
          } catch (error) {
            console.error("Failed to parse websocket frame", error);
          }
        });
        ws.addEventListener("close", () => {
          if (!isActive) {
            return;
          }
          connectSse();
        });
        ws.addEventListener("error", () => {
          if (!isActive) {
            return;
          }
          ws?.close();
          connectSse();
        });
      } catch (error) {
        console.error("WebSocket connection failed", error);
        connectSse();
      }
    };

    const connectSse = () => {
      setStatus("sse");
      try {
        es = new EventSource(SSE_PATH);
        es.onmessage = (event) => {
          const payload = parseFrame(JSON.parse(event.data));
          if (payload) {
            handleFrame(payload);
          }
        };
        es.onerror = () => {
          es?.close();
          startPolling();
        };
      } catch (error) {
        console.error("SSE connection failed", error);
        startPolling();
      }
    };

    const startPolling = () => {
      setStatus("polling");
      const poll = async () => {
        try {
          const payload = await fetchJson<OddsRow[]>(REST_PATH);
          cacheRef.current = indexRows(payload);
          setQueryRows(cacheRef.current);
        } catch (error) {
          console.error("Polling failed", error);
          setStatus("offline");
        }
      };
      poll();
      pollTimer = window.setInterval(poll, 15_000);
    };

    connectWebSocket();

    return () => {
      isActive = false;
      ws?.close();
      es?.close();
      if (pollTimer) {
        clearInterval(pollTimer);
      }
    };
  }, [handleFrame, setQueryRows]);

  const refresh = useCallback(async () => {
    const result = await refetch({ throwOnError: false });
    if (result.data) {
      cacheRef.current = indexRows(result.data);
      setQueryRows(cacheRef.current);
    }
  }, [refetch, setQueryRows]);

  const metadata = useMemo(
    () => ({
      status,
      total: rows.length,
    }),
    [rows.length, status],
  );

  return { rows, status, metadata, refresh };
};
