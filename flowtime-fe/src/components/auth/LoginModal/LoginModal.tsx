import React, { useEffect, useState, useRef } from "react";
import { GOOGLE_LOGIN_URL } from "../../../api/api";
import styles from "./LoginModal.module.css";

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const START_HOUR = 9;
const END_HOUR = 21;
const TOTAL_DAYLINE_MINUTES = (END_HOUR - START_HOUR) * 60;

const MODAL_DEMO_BLOCKS = [
  { id: "mb1", startMinutes: 60, durationMinutes: 90 },
  { id: "mb2", startMinutes: 270, durationMinutes: 90 },
  { id: "mb3", startMinutes: 600, durationMinutes: 90 },
];

export const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [currentTimeStr, setCurrentTimeStr] = useState("18:42");
  const [markerPercent, setMarkerPercent] = useState<number>(80.8);
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    const updateTime = () => {
      const now = new Date();
      const h = now.getHours();
      const m = now.getMinutes();
      setCurrentTimeStr(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);

      const currentMinutesFrom09 = (h - START_HOUR) * 60 + m;
      const clamped = Math.max(0, Math.min(TOTAL_DAYLINE_MINUTES, currentMinutesFrom09));
      const pct = (clamped / TOTAL_DAYLINE_MINUTES) * 100;
      setMarkerPercent(pct > 0 && pct < 100 ? pct : 80.8);
    };

    updateTime();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "unset";
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleGoogleLogin = () => {
    setIsLoading(true);
    window.location.href = GOOGLE_LOGIN_URL;
  };

  return (
    <div
      className={styles.overlay}
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-headline"
    >
      <div className={styles.modal} ref={modalRef}>
        <button
          type="button"
          className={styles.closeButton}
          onClick={onClose}
          aria-label="Close modal"
        >
          ×
        </button>

        <div className={styles.modalHeader}>
          <span className={styles.logo}>FLOWTIME</span>
          <span className={styles.clock}>{currentTimeStr}</span>
        </div>

        <div className={styles.headlineBlock}>
          <h2 id="modal-headline" className={styles.headline}>
            Make time
            <br />
            for what
            <br />
            matters.
          </h2>
          <p className={styles.description}>
            Your calendar, tasks, and time organized around your priorities.
          </p>
        </div>

        <div className={styles.daylineWrapper} aria-hidden="true">
          <div className={styles.ticksRow}>
            {["09", "12", "15", "18", "21"].map((t) => (
              <div key={t} className={styles.tickItem}>
                <span className={styles.tickLabel}>{t}</span>
                <div className={styles.tickMark} />
              </div>
            ))}
          </div>

          <div className={styles.track}>
            {MODAL_DEMO_BLOCKS.map((block) => {
              const left = (block.startMinutes / TOTAL_DAYLINE_MINUTES) * 100;
              const width = (block.durationMinutes / TOTAL_DAYLINE_MINUTES) * 100;
              return (
                <div
                  key={block.id}
                  className={styles.eventBlock}
                  style={{
                    left: `${left}%`,
                    width: `${width}%`,
                  }}
                />
              );
            })}

            <div
              className={styles.copperMarker}
              style={{
                left: `${markerPercent}%`,
              }}
            >
              <div className={styles.copperLine} />
              <div className={styles.copperTag}>
                <span className={styles.copperPip}>▲</span>
                <span className={styles.copperText}>NOW</span>
              </div>
            </div>
          </div>
        </div>

        <div className={styles.actionBlock}>
          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={isLoading}
            className={styles.googleButton}
            aria-label="Continue with Google"
          >
            {isLoading ? (
              <span className={styles.loadingText}>Connecting your calendar…</span>
            ) : (
              <>
                <svg className={styles.googleIcon} viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
                    fill="#4285F4"
                  />
                  <path
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    fill="#34A853"
                  />
                  <path
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                    fill="#FBBC05"
                  />
                  <path
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                    fill="#EA4335"
                  />
                </svg>
                <span>Continue with Google</span>
              </>
            )}
          </button>

          <p className={styles.privacyNote}>
            Your calendar stays yours. FlowTime plans around it.
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginModal;
