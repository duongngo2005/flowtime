import { useCallback, useEffect, useState, type FormEvent } from "react";
import api from "../../api/api";
import type { PlannedSlotApplyStatus, PlannedSlotStatus, PlanningSession, PlannedSlot, PlanningStatus } from "../../api/contracts";
import { getErrorMessage } from "../../api/errors";
import styles from "../workspace/WorkspacePage.module.css";

const activePlanStorageKey = "active_planning_id";

const localDate = (): string => {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

const formatSlotTime = (value: string): string =>
  new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));

const unscheduledMessage = (reason: string): string => {
  const messages: Record<string, string> = {
    DEADLINE_PASSED: "Hạn chót đã qua.",
    NO_AVAILABLE_SLOT: "Không có khung thời gian liên tục phù hợp.",
    INSUFFICIENT_DURATION: "Không đủ thời gian phù hợp trong khoảng lập kế hoạch này.",
  };
  return messages[reason] || reason;
};

const statusIsWarning = (status: PlanningStatus): boolean => status === "APPLY_FAILED" || status === "CANCELLED";

const planningStatusLabel: Record<PlanningStatus, string> = {
  DRAFT: "Bản nháp",
  APPROVED: "Đã phê duyệt",
  APPLYING: "Đang áp dụng",
  APPLY_FAILED: "Áp dụng thất bại",
  CANCELLED: "Đã hủy",
  APPLIED: "Đã áp dụng",
};

const slotStatusLabel: Record<PlannedSlotStatus, string> = {
  PROPOSED: "Đề xuất",
  ACCEPTED: "Đã chấp nhận",
  REMOVED: "Đã bỏ",
};

const applyStatusLabel: Record<PlannedSlotApplyStatus, string> = {
  NOT_REQUESTED: "Chưa yêu cầu",
  PENDING: "Đang chờ",
  APPLYING: "Đang áp dụng",
  APPLIED: "Đã áp dụng",
  FAILED: "Thất bại",
};

const PlanningPage = () => {
  const [startDate, setStartDate] = useState(localDate);
  const [days, setDays] = useState("7");
  const [plan, setPlan] = useState<PlanningSession | null>(null);
  const [loadingPlan, setLoadingPlan] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadPlan = useCallback(async (planningId: string) => {
    try {
      setLoadingPlan(true);
      setError(null);
      const response = await api.get<PlanningSession>(`/api/v1/planning/${planningId}`);
      setPlan(response.data);
    } catch (requestError) {
      window.sessionStorage.removeItem(activePlanStorageKey);
      setError(getErrorMessage(requestError, "Không thể tải kế hoạch mới nhất."));
    } finally {
      setLoadingPlan(false);
    }
  }, []);

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      const savedPlanId = window.sessionStorage.getItem(activePlanStorageKey);
      if (savedPlanId) void loadPlan(savedPlanId);
    }, 0);

    return () => window.clearTimeout(requestId);
  }, [loadPlan]);

  const generatePlan = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setNotice(null);

    try {
      const response = await api.post<PlanningSession>("/api/v1/planning", {
        startDate,
        days: Number(days),
      });
      window.sessionStorage.setItem(activePlanStorageKey, String(response.data.id));
      setPlan(response.data);
      setNotice("Đã tạo bản nháp. Hãy xem xét từng khung giờ được đề xuất trước khi phê duyệt.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể tạo bản nháp kế hoạch."));
    } finally {
      setSubmitting(false);
    }
  };

  const removeSlot = async (slot: PlannedSlot) => {
    if (!plan) return;

    try {
      setSubmitting(true);
      setError(null);
      const response = await api.delete<PlanningSession>(`/api/v1/planning/${plan.id}/slots/${slot.id}`);
      setPlan(response.data);
      setNotice(`Đã bỏ “${slot.taskTitle}” khỏi bản nháp.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể bỏ khung giờ này."));
    } finally {
      setSubmitting(false);
    }
  };

  const approvePlan = async () => {
    if (!plan) return;

    try {
      setSubmitting(true);
      setError(null);
      const response = await api.post<PlanningSession>(`/api/v1/planning/${plan.id}/approve`);
      setPlan(response.data);
      setNotice("Đã phê duyệt kế hoạch. Kế hoạch chưa được ghi vào Google Calendar.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể phê duyệt kế hoạch này."));
    } finally {
      setSubmitting(false);
    }
  };

  const applyPlan = async () => {
    if (!plan) return;

    try {
      setSubmitting(true);
      setError(null);
      const response = await api.post<PlanningSession>(`/api/v1/planning/${plan.id}/apply`);
      setPlan(response.data);
      if (response.data.status === "APPLIED") {
        setNotice("Đã áp dụng kế hoạch vào Google Calendar.");
      } else {
        setError(response.data.lastApplyError || "Một số khung giờ chưa thể áp dụng. Bạn có thể thử lại kế hoạch này.");
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể áp dụng kế hoạch này."));
    } finally {
      setSubmitting(false);
    }
  };

  const cancelPlan = async () => {
    if (!plan || !window.confirm("Hủy bản nháp hoặc kế hoạch đã phê duyệt này? Thao tác này chưa tạo sự kiện Google nào.")) return;

    try {
      setSubmitting(true);
      setError(null);
      const response = await api.post<PlanningSession>(`/api/v1/planning/${plan.id}/cancel`);
      setPlan(response.data);
      setNotice("Đã hủy kế hoạch. Không có sự kiện Google Calendar nào bị thay đổi.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Không thể hủy kế hoạch này."));
    } finally {
      setSubmitting(false);
    }
  };

  const canReview = plan?.status === "DRAFT";
  const canApply = plan?.status === "APPROVED" || plan?.status === "APPLY_FAILED";
  const canCancel = plan?.status === "DRAFT" || plan?.status === "APPROVED";

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Không gian lập kế hoạch</p>
          <h1 className={styles.title}>Xem xét trước khi FlowTime ghi lịch.</h1>
          <p className={styles.subtitle}>Bản nháp an toàn: nó chỉ lưu đề xuất. Google Calendar chỉ thay đổi sau khi bạn phê duyệt và áp dụng.</p>
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <div className={styles.twoColumn}>
        <form className={styles.panel} onSubmit={generatePlan}>
          <h2 className={styles.panelTitle}>Tạo bản nháp</h2>
          <p className={styles.panelHint}>Công cụ xem xét lịch đồng bộ cục bộ, danh sách nhiệm vụ và các tùy chọn đã lưu của bạn.</p>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              Ngày bắt đầu
              <input className={styles.input} onChange={(event) => setStartDate(event.target.value)} required type="date" value={startDate} />
            </label>
            <label className={styles.field}>
              Số ngày để lên kế hoạch
              <input className={styles.input} max="14" min="1" onChange={(event) => setDays(event.target.value)} required type="number" value={days} />
            </label>
          </div>
          <div className={styles.actions}>
            <button className={styles.primaryButton} disabled={submitting} type="submit">{submitting ? "Đang tạo…" : "Tạo bản nháp"}</button>
          </div>
        </form>

        <div className={styles.panel}>
          <h2 className={styles.panelTitle}>Cách thức hoạt động</h2>
          <p className={styles.panelHint}>Bản nháp không gọi Google Calendar. Bước áp dụng kiểm tra lại lịch đích và có thể thử lại an toàn mà không tạo sự kiện trùng.</p>
          <div className={styles.detailGrid}>
            <div className={styles.detailItem}><span className={styles.detailLabel}>1. Bản nháp</span><span className={styles.detailValue}>Xem xét khung giờ</span></div>
            <div className={styles.detailItem}><span className={styles.detailLabel}>2. Phê duyệt</span><span className={styles.detailValue}>Xác nhận kế hoạch</span></div>
            <div className={styles.detailItem}><span className={styles.detailLabel}>3. Áp dụng</span><span className={styles.detailValue}>Tạo sự kiện</span></div>
          </div>
        </div>
      </div>

      <div className={styles.panel}>
        <h2 className={styles.panelTitle}>Kế hoạch hiện tại</h2>
        {loadingPlan ? (
          <p className={styles.empty}>Đang tải kế hoạch đã lưu…</p>
        ) : !plan ? (
          <p className={styles.empty}>Chưa có bản nháp trong phiên trình duyệt này. Hãy tạo kế hoạch khi nhiệm vụ và thiết lập đã sẵn sàng.</p>
        ) : (
          <>
            <div className={styles.detailGrid}>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Tình trạng</span><span className={`${styles.statusPill} ${statusIsWarning(plan.status) ? styles.statusPillWarning : ""}`}>{planningStatusLabel[plan.status]}</span></div>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Cửa sổ</span><span className={styles.detailValue}>{plan.startDate} → {plan.endDate}</span></div>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Lần thử áp dụng</span><span className={styles.detailValue}>{plan.applyAttempts}</span></div>
            </div>

            {plan.lastApplyError && <p className={styles.alertError} style={{ marginTop: "18px" }}>{plan.lastApplyError}</p>}

            <h3 className={styles.sectionTitle}>Các khung giờ đã lên lịch</h3>
            {plan.slots.length === 0 ? <p className={styles.empty}>Không thể lên lịch nhiệm vụ nào trong cửa sổ này.</p> : (
              <div className={styles.slotList}>
                {plan.slots.map((slot) => (
                  <article className={`${styles.slotCard} ${slot.status === "REMOVED" ? styles.slotMuted : ""}`} key={slot.id}>
                    <div>
                      <h4>{slot.taskTitle}</h4>
                      <p className={styles.slotTime}>{formatSlotTime(slot.startAt)} → {formatSlotTime(slot.endAt)} · {slot.durationMinutes} phút</p>
                      <p className={styles.taskMeta}>Xem xét: {slotStatusLabel[slot.status]} · Áp dụng: {applyStatusLabel[slot.applyStatus]}{slot.applyError ? ` · ${slot.applyError}` : ""}</p>
                    </div>
                    {canReview && slot.status === "PROPOSED" && <button className={styles.secondaryButton} disabled={submitting} onClick={() => void removeSlot(slot)} type="button">Bỏ</button>}
                  </article>
                ))}
              </div>
            )}

            <h3 className={styles.sectionTitle}>Tác vụ không theo lịch trình</h3>
            {plan.unscheduledTasks.length === 0 ? <p className={styles.empty}>Mọi nhiệm vụ phù hợp trong cửa sổ này đều đã nhận được đề xuất.</p> : (
              <div className={styles.unscheduledList}>
                {plan.unscheduledTasks.map((task) => (
                  <article className={styles.unscheduledCard} key={task.id}>
                    <div><h4>{task.taskTitle}</h4><p className={styles.taskDescription}>{unscheduledMessage(task.reason)} · Còn {task.unscheduledMinutes} phút</p></div>
                    <span className={`${styles.statusPill} ${styles.statusPillWarning}`}>{unscheduledMessage(task.reason)}</span>
                  </article>
                ))}
              </div>
            )}

            <div className={styles.actions}>
              {canReview && <button className={styles.primaryButton} disabled={submitting} onClick={() => void approvePlan()} type="button">Phê duyệt kế hoạch</button>}
              {canApply && <button className={styles.primaryButton} disabled={submitting} onClick={() => void applyPlan()} type="button">{plan.status === "APPLY_FAILED" ? "Thử áp dụng lại" : "Áp dụng vào Google Calendar"}</button>}
              {canCancel && <button className={styles.dangerButton} disabled={submitting} onClick={() => void cancelPlan()} type="button">Hủy kế hoạch</button>}
            </div>
          </>
        )}
      </div>
    </section>
  );
};

export default PlanningPage;
