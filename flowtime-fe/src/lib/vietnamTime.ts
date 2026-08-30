export const VIETNAM_TIMEZONE = "Asia/Ho_Chi_Minh";

export interface VietnamDateTime {
  date: string;
  hour: number;
  minute: number;
}

const partFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: VIETNAM_TIMEZONE,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

const dateFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: VIETNAM_TIMEZONE,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

const partsFor = (formatter: Intl.DateTimeFormat, value: Date) =>
  Object.fromEntries(
    formatter
      .formatToParts(value)
      .filter((part) => part.type !== "literal")
      .map((part) => [part.type, part.value]),
  );

export const vietnamDate = (value = new Date()): string => {
  const parts = partsFor(dateFormatter, value);
  return `${parts.year}-${parts.month}-${parts.day}`;
};

export const vietnamDateTime = (value = new Date()): VietnamDateTime => {
  const parts = partsFor(partFormatter, value);
  return {
    date: `${parts.year}-${parts.month}-${parts.day}`,
    hour: Number(parts.hour),
    minute: Number(parts.minute),
  };
};

export const vietnamDateAtMinutes = (date: string, minutes: number): Date =>
  new Date(Date.parse(`${date}T00:00:00+07:00`) + minutes * 60_000);

export const vietnamDayRange = (value = new Date()): { from: string; to: string } => {
  const date = vietnamDate(value);
  const start = vietnamDateAtMinutes(date, 0);
  const nextDay = new Date(start.getTime() + 24 * 60 * 60 * 1000);

  return { from: start.toISOString(), to: nextDay.toISOString() };
};

export const addDaysToVietnamDate = (date: string, days: number): string => {
  const [year, month, day] = date.split("-").map(Number);
  const result = new Date(Date.UTC(year, month - 1, day + days));
  return `${result.getUTCFullYear()}-${String(result.getUTCMonth() + 1).padStart(2, "0")}-${String(result.getUTCDate()).padStart(2, "0")}`;
};

export const formatVietnamDate = (date: string): string =>
  new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeZone: VIETNAM_TIMEZONE,
  }).format(vietnamDateAtMinutes(date, 0));

export const minutesFromTime = (time: string): number | null => {
  const match = /^(\d{2}):(\d{2})/.exec(time);
  if (!match) return null;

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;

  return hours * 60 + minutes;
};

export const formatMinutes = (minutes: number): string => {
  const normalized = Math.max(0, Math.min(24 * 60 - 1, Math.round(minutes)));
  return `${String(Math.floor(normalized / 60)).padStart(2, "0")}:${String(normalized % 60).padStart(2, "0")}`;
};
