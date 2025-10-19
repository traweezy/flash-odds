export type OddsRow = {
  id: string;
  sport: string;
  event: string;
  market: string;
  line: number | null;
  price: number | null;
  book: string;
  startsAt: string;
  updatedAt: string;
  extra: Record<string, unknown>;
};

export type OddsRowChange = {
  op: "upsert" | "remove";
  id: string;
  row: OddsRow | null;
};

export type OddsFrame = {
  type: "snapshot" | "delta";
  ts: string;
  rows: OddsRowChange[];
};
