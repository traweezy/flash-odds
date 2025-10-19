export const formatAmericanOdds = (value: number | null): string => {
  if (value == null || Number.isNaN(value)) {
    return "—";
  }
  return value > 0 ? `+${value}` : `${value}`;
};

export const formatLine = (line: number | null): string => {
  if (line == null || Number.isNaN(line)) {
    return "—";
  }
  const rounded = Math.round(line * 10) / 10;
  return rounded % 1 === 0 ? `${Math.trunc(rounded)}` : rounded.toFixed(1);
};
