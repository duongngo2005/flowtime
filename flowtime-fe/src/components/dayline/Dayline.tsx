import React, { useEffect, useMemo, useState } from "react";
import {
  formatMinutes,
  minutesFromTime,
  vietnamDateAtMinutes,
  vietnamDateTime,
} from "../../lib/vietnamTime";
import styles from "./Dayline.module.css";

const DEFAULT_START_TIME = "09:00";
const DEFAULT_END_TIME = "17:00";

export interface DaylineEvent {
  id: number;
  title: string;
  startAt: string;
  endAt: string;
  allDay: boolean;
}

interface DaylineProps {
  events?: DaylineEvent[];
  workdayStartTime?: string;
  workdayEndTime?: string;
}

const DEMO_BLOCKS = [
  { id: "1", start: 60, dur: 100, label: "LỚP HỌC", type: "busy" },
  { id: "2", start: 160, dur: 110, label: "TRỐNG 1 GIỜ 50 PHÚT", type: "open" },
  { id: "3", start: 270, dur: 90, label: "HỌP", type: "busy" },
  { id: "4", start: 360, dur: 240, label: "TRỐNG 4 GIỜ", type: "open" },
  { id: "5", start: 600, dur: 90, label: "LÀM VIỆC SÂU", type: "busy" },
];

export const Dayline: React.FC<DaylineProps> = ({
  events,
  workdayStartTime = DEFAULT_START_TIME,
  workdayEndTime = DEFAULT_END_TIME,
}) => {
  const [currentTimeStr, setCurrentTimeStr] = useState("");
  const [markerPercent, setMarkerPercent] = useState<number | null>(null);
  const [outsideTimeline, setOutsideTimeline] = useState<"before" | "after" | null>(null);

  const { startMinutes, endMinutes, totalMinutes } = useMemo(() => {
    const defaultStart = minutesFromTime(DEFAULT_START_TIME)!;
    const defaultEnd = minutesFromTime(DEFAULT_END_TIME)!;
    const parsedStart = minutesFromTime(workdayStartTime) ?? defaultStart;
    const parsedEnd = minutesFromTime(workdayEndTime) ?? defaultEnd;

    if (parsedEnd <= parsedStart) {
      return { startMinutes: defaultStart, endMinutes: defaultEnd, totalMinutes: defaultEnd - defaultStart };
    }

    return { startMinutes: parsedStart, endMinutes: parsedEnd, totalMinutes: parsedEnd - parsedStart };
  }, [workdayEndTime, workdayStartTime]);

  const ticks = useMemo(
    () => Array.from({ length: 5 }, (_, index) => formatMinutes(startMinutes + ((endMinutes - startMinutes) * index) / 4)),
    [endMinutes, startMinutes],
  );

  useEffect(() => {
    const updateTime = () => {
      const now = vietnamDateTime();
      const nowMinutes = now.hour * 60 + now.minute;
      setCurrentTimeStr(formatMinutes(nowMinutes));

      const elapsedMinutes = nowMinutes - startMinutes;
      if (elapsedMinutes < 0) {
        setMarkerPercent(null);
        setOutsideTimeline("before");
        return;
      }
      if (elapsedMinutes >= totalMinutes) {
        setMarkerPercent(null);
        setOutsideTimeline("after");
        return;
      }

      setMarkerPercent((elapsedMinutes / totalMinutes) * 100);
      setOutsideTimeline(null);
    };

    updateTime();
    const interval = setInterval(updateTime, 1000 * 60);
    return () => clearInterval(interval);
  }, [startMinutes, totalMinutes]);

  const blocks = useMemo(() => {
    if (events === undefined) return DEMO_BLOCKS;

    const { date } = vietnamDateTime();
    const workingStart = vietnamDateAtMinutes(date, startMinutes);
    const workingEnd = vietnamDateAtMinutes(date, endMinutes);

    return events.flatMap((event) => {
      const eventStart = new Date(event.startAt);
      const eventEnd = new Date(event.endAt);
      const visibleStart = new Date(Math.max(eventStart.getTime(), workingStart.getTime()));
      const visibleEnd = new Date(Math.min(eventEnd.getTime(), workingEnd.getTime()));

      if (visibleStart >= visibleEnd) return [];

      return [{
        id: String(event.id),
        start: (visibleStart.getTime() - workingStart.getTime()) / 60_000,
        dur: (visibleEnd.getTime() - visibleStart.getTime()) / 60_000,
        label: event.allDay ? `${event.title} · CẢ NGÀY` : event.title,
        type: "busy",
      }];
    });
  }, [endMinutes, events, startMinutes]);

  return (
    <div className={styles.daylineWrapper} aria-label="Dòng thời gian trong ngày">
      <div className={styles.ticksRow} aria-hidden="true">
        {ticks.map((tick) => (
          <div key={tick} className={styles.tickItem}>
            <span className={styles.tickLabel}>{tick}</span>
            <div className={styles.tickMark} />
          </div>
        ))}
      </div>

      {markerPercent === null && currentTimeStr && outsideTimeline && (
        <p className={styles.outsideNow} role="status">
          BÂY GIỜ {currentTimeStr} · {outsideTimeline === "before" ? "trước khung giờ hiển thị" : "sau khung giờ hiển thị"}
        </p>
      )}

      <div className={styles.track}>
        {blocks.map((block) => {
          const left = (block.start / totalMinutes) * 100;
          const width = (block.dur / totalMinutes) * 100;
          const isBusy = block.type === "busy";

          return (
            <div
              key={block.id}
              className={`${styles.eventBlock} ${isBusy ? styles.busyBlock : styles.openBlock}`}
              style={{ left: `${left}%`, width: `${width}%` }}
            >
              <span className={styles.blockText}>{block.label}</span>
            </div>
          );
        })}

        {events && blocks.length === 0 && (
          <span className={styles.emptyState}>HÔM NAY KHÔNG CÓ SỰ KIỆN ĐỒNG BỘ</span>
        )}

        {markerPercent !== null && (
          <div
            className={styles.copperMarker}
            style={{ left: `${markerPercent}%` }}
            role="status"
            aria-label={`Mốc thời gian hiện tại: ${currentTimeStr}`}
          >
            <div className={styles.copperLine} />
            <div className={styles.copperTag}>
              <span className={styles.copperPip}>▲</span>
              <span className={styles.copperText}>BÂY GIỜ {currentTimeStr}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dayline;
