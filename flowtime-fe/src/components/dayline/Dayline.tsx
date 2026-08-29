import React, { useEffect, useState } from "react";
import styles from "./Dayline.module.css";

const START_HOUR = 9;
const END_HOUR = 21;
const TOTAL_MINUTES = (END_HOUR - START_HOUR) * 60;

interface DaylineProps {
  interactive?: boolean;
}

export const Dayline: React.FC<DaylineProps> = () => {
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

  const blocks = [
    { id: "1", start: 60, dur: 100, label: "CLASS", type: "busy" },
    { id: "2", start: 160, dur: 110, label: "AVAILABLE 1h50m", type: "open" },
    { id: "3", start: 270, dur: 90, label: "MEETING", type: "busy" },
    { id: "4", start: 360, dur: 240, label: "AVAILABLE 4h00m", type: "open" },
    { id: "5", start: 600, dur: 90, label: "DEEP WORK", type: "busy" },
  ];

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
