export const formatShortDateTime = (iso: string): string => {
  const date = new Date(iso);
  return Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};
