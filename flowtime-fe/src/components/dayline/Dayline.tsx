import React, { useEffect, useMemo, useState } from "react";
import styles from "./Dayline.module.css";

const START_HOUR = 9;
const END_HOUR = 21;
const TOTAL_MINUTES = (END_HOUR - START_HOUR) * 60;

export interface DaylineEvent {
  id: number;
  title: string;
  startAt: string;
  endAt: string;
  allDay: boolean;
}

interface DaylineProps {
  events?: DaylineEvent[];
}

const DEMO_BLOCKS = [
  { id: "1", start: 60, dur: 100, label: "CLASS", type: "busy" },
  { id: "2", start: 160, dur: 110, label: "AVAILABLE 1h50m", type: "open" },
  { id: "3", start: 270, dur: 90, label: "MEETING", type: "busy" },
  { id: "4", start: 360, dur: 240, label: "AVAILABLE 4h00m", type: "open" },
  { id: "5", start: 600, dur: 90, label: "DEEP WORK", type: "busy" },
];

export const Dayline: React.FC<DaylineProps> = ({ events }) => {
  const [currentTimeStr, setCurrentTimeStr] = useState("18:42");
  const [markerPercent, setMarkerPercent] = useState<number>(80.8);

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      const h = now.getHours();
      const m = now.getMinutes();
      setCurrentTimeStr(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);

      const mins = (h - START_HOUR) * 60 + m;
      const clamped = Math.max(0, Math.min(TOTAL_MINUTES, mins));
      const pct = (clamped / TOTAL_MINUTES) * 100;
      setMarkerPercent(pct > 0 && pct < 100 ? pct : 80.8);
    };

    updateTime();
    const interval = setInterval(updateTime, 1000 * 60);
    return () => clearInterval(interval);
  }, []);

  const blocks = useMemo(() => {
    if (events === undefined) return DEMO_BLOCKS;

    const dayStart = new Date();
    dayStart.setHours(0, 0, 0, 0);
    const workingStart = new Date(dayStart);
    workingStart.setHours(START_HOUR, 0, 0, 0);
    const workingEnd = new Date(dayStart);
    workingEnd.setHours(END_HOUR, 0, 0, 0);

    return events.flatMap((event) => {
      const eventStart = new Date(event.startAt);
      const eventEnd = new Date(event.endAt);
      const visibleStart = new Date(Math.max(eventStart.getTime(), workingStart.getTime()));
      const visibleEnd = new Date(Math.min(eventEnd.getTime(), workingEnd.getTime()));

      if (visibleStart >= visibleEnd) return [];

      return [{
        id: String(event.id),
        start: (visibleStart.getTime() - workingStart.getTime()) / 60000,
        dur: (visibleEnd.getTime() - visibleStart.getTime()) / 60000,
        label: event.allDay ? `${event.title} · ALL DAY` : event.title,
        type: "busy",
      }];
    });
  }, [events]);

  return (
    <div className={styles.daylineWrapper} aria-label="Dayline schedule timeline">
      <div className={styles.ticksRow} aria-hidden="true">
        {["09:00", "12:00", "15:00", "18:00", "21:00"].map((t) => (
          <div key={t} className={styles.tickItem}>
            <span className={styles.tickLabel}>{t}</span>
            <div className={styles.tickMark} />
          </div>
        ))}
      </div>

      <div className={styles.track}>
        {blocks.map((b) => {
          const left = (b.start / TOTAL_MINUTES) * 100;
          const width = (b.dur / TOTAL_MINUTES) * 100;
          const isBusy = b.type === "busy";

          return (
            <div
              key={b.id}
              className={`${styles.eventBlock} ${isBusy ? styles.busyBlock : styles.openBlock}`}
              style={{ left: `${left}%`, width: `${width}%` }}
            >
              <span className={styles.blockText}>{b.label}</span>
            </div>
          );
        })}

        {events && blocks.length === 0 && (
          <span className={styles.emptyState}>NO LOCAL EVENTS TODAY</span>
        )}

        <div
          className={styles.copperMarker}
          style={{ left: `${markerPercent}%` }}
          role="status"
          aria-label={`Current time indicator: ${currentTimeStr} Now`}
        >
          <div className={styles.copperLine} />
          <div className={styles.copperTag}>
            <span className={styles.copperPip}>▲</span>
            <span className={styles.copperText}>NOW {currentTimeStr}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dayline;
