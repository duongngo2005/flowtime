import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { logout } from "../../api/api";
import Dayline, { type DaylineEvent } from "../../components/dayline/Dayline";
import styles from "./DashboardPage.module.css";

interface UserInfo {
  id: number;
  email: string;
  name: string;
  timezone: string;
}

interface GoogleConnectionStatus {
  connected: boolean;
}

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [googleConnected, setGoogleConnected] = useState(false);
  const [calendarEvents, setCalendarEvents] = useState<DaylineEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        const [userResponse, googleStatusResponse, eventsResponse] = await Promise.all([
          api.get<UserInfo>("/api/auth/me"),
          api.get<GoogleConnectionStatus>("/api/v1/auth/google/status"),
          api.get<DaylineEvent[]>("/api/v1/calendar/events", {
            params: { from: today.toISOString(), to: tomorrow.toISOString() },
          }),
        ]);
        setUser(userResponse.data);
        setGoogleConnected(googleStatusResponse.data.connected);
        setCalendarEvents(eventsResponse.data);
      } catch {
        localStorage.removeItem("access_token");
        navigate("/", { replace: true });
      } finally {
        setLoading(false);
      }
    };
    fetchUser();
  }, [navigate]);

  const handleLogout = async () => {
    await logout();
  };

  if (loading) {
    return (
      <div className={styles.page}>
        <p style={{ fontFamily: "var(--font-mono)", color: "var(--color-secondary)" }}>
          Loading FlowTime Dashboard…
        </p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <header className={styles.header}>
          <span className={styles.brand}>FLOWTIME // DASHBOARD</span>
          <span className={styles.statusBadge}>
            {googleConnected ? "GOOGLE CALENDAR CONNECTED" : "GOOGLE CALENDAR NOT CONNECTED"}
          </span>
        </header>

        {user && (
          <>
            <h1 className={styles.welcomeTitle}>
              Welcome back, {user.name}
            </h1>

            <Dayline events={calendarEvents} />

            <div className={styles.userInfo}>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>Account Email</span>
                <span className={styles.infoValue}>{user.email}</span>
              </div>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>System Timezone</span>
                <span className={styles.infoValue}>{user.timezone}</span>
              </div>
            </div>

            <button onClick={handleLogout} className={styles.logoutBtn}>
              Sign out
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
