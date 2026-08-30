import { useCallback, useEffect, useState, type FormEvent } from "react";
import api from "../../api/api";
import type { SchedulingPreferences, SchedulingPreferencesPayload, WorkingDay } from "../../api/contracts";
import { getErrorMessage } from "../../api/errors";
import styles from "../workspace/WorkspacePage.module.css";

const week: Array<{ value: WorkingDay; label: string }> = [
  { value: "MONDAY", label: "Thứ Hai" },
  { value: "TUESDAY", label: "Thứ Ba" },
  { value: "WEDNESDAY", label: "Thứ Tư" },
  { value: "THURSDAY", label: "Thứ Năm" },
  { value: "FRIDAY", label: "Thứ Sáu" },
  { value: "SATURDAY", label: "Thứ Bảy" },
  { value: "SUNDAY", label: "Chủ Nhật" },
];

const browserTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";

const defaults = (): SchedulingPreferencesPayload => ({
  timezone: browserTimezone,
  workdayStartTime: "09:00",
  workdayEndTime: "17:00",
  workingDays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  focusDurationMinutes: 50,
  breakDurationMinutes: 10,
  dailyFocusLimit: 480,
});

const formForPreferences = (preference: SchedulingPreferences): SchedulingPreferencesPayload => ({
  timezone: preference.timezone,
  workdayStartTime: preference.workdayStartTime.slice(0, 5),
  workdayEndTime: preference.workdayEndTime.slice(0, 5),
  workingDays: preference.workingDays,
  focusDurationMinutes: preference.focusDurationMinutes,
  breakDurationMinutes: preference.breakDurationMinutes,
  dailyFocusLimit: preference.dailyFocusLimit,
});

const PreferencesPage = () => {
  const [form, setForm] = useState<SchedulingPreferencesPayload>(defaults);
  const [configured, setConfigured] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadPreferences = useCallback(async () => {
    try {
      setError(null);
      const response = await api.get<SchedulingPreferences>("/api/v1/scheduling-preferences");
      setForm(formForPreferences(response.data));
      setConfigured(response.data.configured);
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể tải thiết lập lịch làm việc."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      void loadPreferences();
    }, 0);

    return () => window.clearTimeout(requestId);
  }, [loadPreferences]);

  const toggleWorkingDay = (day: WorkingDay) => {
    setForm((current) => ({
      ...current,
      workingDays: current.workingDays.includes(day)
        ? current.workingDays.filter((item) => item !== day)
        : [...current.workingDays, day],
    }));
  };

  const savePreferences = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);

    try {
      const response = await api.put<SchedulingPreferences>("/api/v1/scheduling-preferences", form);
      setForm(formForPreferences(response.data));
      setConfigured(true);
      setNotice("Đã lưu thiết lập lịch làm việc.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể lưu thiết lập lịch làm việc."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Quy tắc lập lịch</p>
          <h1 className={styles.title}>Cho FlowTime biết khi nào bạn có thể tập trung.</h1>
          <p className={styles.subtitle}>Các giới hạn này là điều kiện bắt buộc cho mọi bản kế hoạch mới.</p>
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <form className={styles.panel} onSubmit={savePreferences}>
        <h2 className={styles.panelTitle}>Lịch làm việc {configured ? "" : "(chưa được thiết lập)"}</h2>
        <p className={styles.panelHint}>Dùng múi giờ IANA, ví dụ <code>Asia/Ho_Chi_Minh</code>. Cần chọn ít nhất một ngày làm việc.</p>

        {loading ? (
          <p className={styles.empty}>Đang tải thiết lập…</p>
        ) : (
          <>
            <div className={styles.formGrid}>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                Múi giờ
                <input className={styles.input} onChange={(event) => setForm({ ...form, timezone: event.target.value })} required value={form.timezone} />
              </label>
              <label className={styles.field}>
                Bắt đầu ngày làm việc
                <input className={styles.input} onChange={(event) => setForm({ ...form, workdayStartTime: event.target.value })} required type="time" value={form.workdayStartTime} />
              </label>
              <label className={styles.field}>
                Kết thúc ngày làm việc
                <input className={styles.input} onChange={(event) => setForm({ ...form, workdayEndTime: event.target.value })} required type="time" value={form.workdayEndTime} />
              </label>
              <fieldset className={`${styles.field} ${styles.fullWidth}`}>
                <legend>Ngày làm việc</legend>
                <div className={styles.weekdayGrid}>
                  {week.map((day) => (
                    <label className={styles.checkboxLabel} key={day.value}>
                      <input checked={form.workingDays.includes(day.value)} onChange={() => toggleWorkingDay(day.value)} type="checkbox" />
                      {day.label}
                    </label>
                  ))}
                </div>
              </fieldset>
              <label className={styles.field}>
                Một phiên tập trung (phút)
                <input className={styles.input} max="240" min="5" onChange={(event) => setForm({ ...form, focusDurationMinutes: Number(event.target.value) })} required type="number" value={form.focusDurationMinutes} />
              </label>
              <label className={styles.field}>
                Nghỉ giữa phiên (phút)
                <input className={styles.input} max="240" min="0" onChange={(event) => setForm({ ...form, breakDurationMinutes: Number(event.target.value) })} required type="number" value={form.breakDurationMinutes} />
              </label>
              <label className={styles.field}>
                Giới hạn tập trung mỗi ngày (phút)
                <input className={styles.input} max="1440" min="15" onChange={(event) => setForm({ ...form, dailyFocusLimit: Number(event.target.value) })} required type="number" value={form.dailyFocusLimit} />
              </label>
            </div>
            <div className={styles.actions}>
              <button className={styles.primaryButton} disabled={saving} type="submit">{saving ? "Đang lưu…" : "Lưu thiết lập"}</button>
            </div>
          </>
        )}
      </form>
    </section>
  );
};

export default PreferencesPage;
