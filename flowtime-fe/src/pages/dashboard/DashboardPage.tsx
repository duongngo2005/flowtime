import axios from "axios";
import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../api/api";
import { getErrorMessage } from "../../api/errors";
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

interface CalendarSyncResponse {
  calendarsSynced: number;
  eventsCreated: number;
  eventsUpdated: number;
  syncedFrom: string;
  syncedTo: string;
}

const dayRange = () => {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  return { from: start.toISOString(), to: end.toISOString() };
};

export const DashboardPage = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [googleConnected, setGoogleConnected] = useState(false);
  const [calendarEvents, setCalendarEvents] = useState<DaylineEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    try {
      const range = dayRange();
      const [userResponse, googleStatusResponse, eventsResponse] = await Promise.all([
        api.get<UserInfo>("/api/auth/me"),
        api.get<GoogleConnectionStatus>("/api/v1/auth/google/status"),
        api.get<DaylineEvent[]>("/api/v1/calendar/events", { params: range }),
      ]);
      setUser(userResponse.data);
      setGoogleConnected(googleStatusResponse.data.connected);
      setCalendarEvents(eventsResponse.data);
    } catch (requestError) {
      if (axios.isAxiosError(requestError) && requestError.response?.status === 401) {
        localStorage.removeItem("access_token");
        navigate("/", { replace: true });
        return;
      }
      setError(getErrorMessage(requestError, "Could not load your dashboard."));
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      void loadDashboard();
    }, 0);

    return () => window.clearTimeout(requestId);
  }, [loadDashboard]);

  const syncCalendar = async () => {
    if (!googleConnected) {
      setError("Connect Google Calendar before syncing.");
      return;
    }

    try {
      setSyncing(true);
      setError(null);
      setNotice(null);
      const response = await api.post<CalendarSyncResponse>("/api/v1/calendars/sync");
      setNotice(`Synced ${response.data.calendarsSynced} calendars: ${response.data.eventsCreated} new and ${response.data.eventsUpdated} updated events.`);
      await loadDashboard();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not sync Google Calendar."));
    } finally {
      setSyncing(false);
    }
  };

  if (loading) {
    return <p className={styles.loading}>Loading your workspace…</p>;
  }

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Today’s workspace</p>
          <h1 className={styles.welcomeTitle}>Welcome back{user ? `, ${user.name}` : ""}.</h1>
          <p className={styles.subtitle}>Sync your calendar, shape your task backlog, then generate a plan you can review.</p>
        </div>
        <div className={`${styles.connection} ${googleConnected ? styles.connected : styles.disconnected}`}>
          <span className={styles.connectionDot} />
          {googleConnected ? "Google Calendar connected" : "Google Calendar not connected"}
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <div className={styles.grid}>
        <article className={`${styles.card} ${styles.timelineCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Local calendar</p>
              <h2>Today’s schedule</h2>
            </div>
            <button className={styles.syncButton} disabled={syncing || !googleConnected} onClick={() => void syncCalendar()} type="button">
              {syncing ? "Syncing…" : "Sync calendar"}
            </button>
          </div>
          <Dayline events={calendarEvents} />
          <p className={styles.cardHint}>Scheduling reads these synced events as busy time. Sync again before generating a new plan.</p>
        </article>

        <article className={styles.card}>
          <p className={styles.cardEyebrow}>Start here</p>
          <h2>Prepare a reliable plan</h2>
          <ol className={styles.checklist}>
            <li><Link to="/tasks">Create or update tasks</Link></li>
            <li><Link to="/preferences">Set your focus schedule</Link></li>
            <li><Link to="/planning">Generate and review a draft</Link></li>
          </ol>
          <p className={styles.cardHint}>FlowTime will not create a Google event until you approve and apply a plan.</p>
        </article>

        <article className={styles.card}>
          <p className={styles.cardEyebrow}>Account</p>
          <h2>{user?.email || "Signed in"}</h2>
          <dl className={styles.accountDetails}>
            <div><dt>Timezone</dt><dd>{user?.timezone || "—"}</dd></div>
            <div><dt>Events today</dt><dd>{calendarEvents.length}</dd></div>
          </dl>
        </article>
      </div>
    </section>
  );
};

export default DashboardPage;
