import axios from "axios";
import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../api/api";
import type { SchedulingPreferences } from "../../api/contracts";
import { getErrorMessage } from "../../api/errors";
import Dayline, { type DaylineEvent } from "../../components/dayline/Dayline";
import { vietnamDayRange } from "../../lib/vietnamTime";
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

export const DashboardPage = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [googleConnected, setGoogleConnected] = useState(false);
  const [calendarEvents, setCalendarEvents] = useState<DaylineEvent[]>([]);
  const [preferences, setPreferences] = useState<SchedulingPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    try {
      const range = vietnamDayRange();
      const [userResponse, googleStatusResponse, eventsResponse, preferencesResponse] = await Promise.all([
        api.get<UserInfo>("/api/auth/me"),
        api.get<GoogleConnectionStatus>("/api/v1/auth/google/status"),
        api.get<DaylineEvent[]>("/api/v1/calendar/events", { params: range }),
        api.get<SchedulingPreferences>("/api/v1/scheduling-preferences"),
      ]);
      setUser(userResponse.data);
      setGoogleConnected(googleStatusResponse.data.connected);
      setCalendarEvents(eventsResponse.data);
      setPreferences(preferencesResponse.data);
    } catch (requestError) {
      if (axios.isAxiosError(requestError) && requestError.response?.status === 401) {
        localStorage.removeItem("access_token");
        navigate("/", { replace: true });
        return;
      }
      setError(getErrorMessage(requestError, "Không thể tải không gian làm việc."));
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
      setError("Hãy kết nối Google Calendar trước khi đồng bộ.");
      return;
    }

    try {
      setSyncing(true);
      setError(null);
      setNotice(null);
      const response = await api.post<CalendarSyncResponse>("/api/v1/calendars/sync");
      setNotice(`Đã đồng bộ ${response.data.calendarsSynced} lịch: ${response.data.eventsCreated} sự kiện mới và ${response.data.eventsUpdated} sự kiện được cập nhật.`);
      await loadDashboard();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể đồng bộ Google Calendar."));
    } finally {
      setSyncing(false);
    }
  };

  if (loading) {
    return <p className={styles.loading}>Đang tải không gian làm việc…</p>;
  }

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Không gian làm việc hôm nay</p>
          <h1 className={styles.welcomeTitle}>Chào mừng trở lại{user ? `, ${user.name}` : ""}.</h1>
          <p className={styles.subtitle}>Đồng bộ lịch, chuẩn bị danh sách nhiệm vụ, rồi tạo kế hoạch để xem xét.</p>
        </div>
        <div className={`${styles.connection} ${googleConnected ? styles.connected : styles.disconnected}`}>
          <span className={styles.connectionDot} />
          {googleConnected ? "Đã kết nối Google Calendar" : "Chưa kết nối Google Calendar"}
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <div className={styles.grid}>
        <article className={`${styles.card} ${styles.timelineCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Lịch cục bộ</p>
              <h2>Lịch trình hôm nay</h2>
            </div>
            <button className={styles.syncButton} disabled={syncing || !googleConnected} onClick={() => void syncCalendar()} type="button">
              {syncing ? "Đang đồng bộ…" : "Đồng bộ lịch"}
            </button>
          </div>
          <Dayline
            events={calendarEvents}
            workdayEndTime={preferences?.workdayEndTime}
            workdayStartTime={preferences?.workdayStartTime}
          />
          <p className={styles.cardHint}></p>
        </article>

        <article className={styles.card}>
          <p className={styles.cardEyebrow}>Bắt đầu từ đây</p>
          <h2>Chuẩn bị một kế hoạch đáng tin cậy</h2>
          <ol className={styles.checklist}>
            <li><Link to="/tasks">Tạo hoặc cập nhật nhiệm vụ</Link></li>
            <li><Link to="/preferences">Thiết lập lịch tập trung</Link></li>
            <li><Link to="/planning">Tạo và xem xét bản nháp</Link></li>
          </ol>
          <p className={styles.cardHint}>FlowTime chỉ tạo sự kiện Google sau khi bạn phê duyệt và áp dụng kế hoạch.</p>
        </article>

        <article className={styles.card}>
          <p className={styles.cardEyebrow}>Tài khoản</p>
          <h2>{user?.email || "Đã đăng nhập"}</h2>
          <dl className={styles.accountDetails}>
            <div><dt>Múi giờ</dt><dd>{user?.timezone || "—"}</dd></div>
            <div><dt>Sự kiện hôm nay</dt><dd>{calendarEvents.length}</dd></div>
          </dl>
        </article>
      </div>
    </section>
  );
};

export default DashboardPage;
