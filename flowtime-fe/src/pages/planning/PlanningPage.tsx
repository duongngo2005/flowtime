import { useCallback, useEffect, useState, type FormEvent } from "react";
import api from "../../api/api";
import type { PlanningSession, PlannedSlot, PlanningStatus } from "../../api/contracts";
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
  new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));

const unscheduledMessage = (reason: string): string => {
  const messages: Record<string, string> = {
    DEADLINE_PASSED: "The deadline has already passed.",
    NO_AVAILABLE_SLOT: "No eligible continuous slot is available.",
    INSUFFICIENT_DURATION: "There is not enough eligible time in this planning window.",
  };
  return messages[reason] || reason;
};

const statusIsWarning = (status: PlanningStatus): boolean => status === "APPLY_FAILED" || status === "CANCELLED";

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
      setError(getErrorMessage(requestError, "Could not load the latest plan."));
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
      setNotice("Draft plan generated. Review every proposed slot before approval.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not generate a draft plan."));
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
      setNotice(`Removed “${slot.taskTitle}” from this draft.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not remove this slot."));
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
      setNotice("Plan approved. It has not been written to Google Calendar yet.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not approve this plan."));
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
        setNotice("Plan applied to Google Calendar.");
      } else {
        setError(response.data.lastApplyError || "Some slots could not be applied. You can retry this plan.");
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not apply this plan."));
    } finally {
      setSubmitting(false);
    }
  };

  const cancelPlan = async () => {
    if (!plan || !window.confirm("Cancel this draft or approved plan? It has not created any Google events.")) return;

    try {
      setSubmitting(true);
      setError(null);
      const response = await api.post<PlanningSession>(`/api/v1/planning/${plan.id}/cancel`);
      setPlan(response.data);
      setNotice("Plan cancelled. No Google Calendar event was changed.");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not cancel this plan."));
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
          <p className={styles.eyebrow}>Planning workspace</p>
          <h1 className={styles.title}>Review before FlowTime writes.</h1>
          <p className={styles.subtitle}>A draft is safe: it only stores suggestions. Google Calendar changes happen only after approval and apply.</p>
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <div className={styles.twoColumn}>
        <form className={styles.panel} onSubmit={generatePlan}>
          <h2 className={styles.panelTitle}>Generate a draft</h2>
          <p className={styles.panelHint}>The engine considers your local synced calendar, task backlog and saved preferences.</p>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              Start date
              <input className={styles.input} onChange={(event) => setStartDate(event.target.value)} required type="date" value={startDate} />
            </label>
            <label className={styles.field}>
              Days to plan
              <input className={styles.input} max="14" min="1" onChange={(event) => setDays(event.target.value)} required type="number" value={days} />
            </label>
          </div>
          <div className={styles.actions}>
            <button className={styles.primaryButton} disabled={submitting} type="submit">{submitting ? "Generating…" : "Generate draft"}</button>
          </div>
        </form>

        <div className={styles.panel}>
          <h2 className={styles.panelTitle}>How this works</h2>
          <p className={styles.panelHint}>Drafts never call Google Calendar. The apply step re-checks the target calendar and safely retries without duplicate events.</p>
          <div className={styles.detailGrid}>
            <div className={styles.detailItem}><span className={styles.detailLabel}>1. Draft</span><span className={styles.detailValue}>Review slots</span></div>
            <div className={styles.detailItem}><span className={styles.detailLabel}>2. Approve</span><span className={styles.detailValue}>Confirm plan</span></div>
            <div className={styles.detailItem}><span className={styles.detailLabel}>3. Apply</span><span className={styles.detailValue}>Create events</span></div>
          </div>
        </div>
      </div>

      <div className={styles.panel}>
        <h2 className={styles.panelTitle}>Current plan</h2>
        {loadingPlan ? (
          <p className={styles.empty}>Loading saved plan…</p>
        ) : !plan ? (
          <p className={styles.empty}>No draft in this browser session. Generate a plan when your tasks and preferences are ready.</p>
        ) : (
          <>
            <div className={styles.detailGrid}>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Status</span><span className={`${styles.statusPill} ${statusIsWarning(plan.status) ? styles.statusPillWarning : ""}`}>{plan.status}</span></div>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Window</span><span className={styles.detailValue}>{plan.startDate} → {plan.endDate}</span></div>
              <div className={styles.detailItem}><span className={styles.detailLabel}>Apply attempts</span><span className={styles.detailValue}>{plan.applyAttempts}</span></div>
            </div>

            {plan.lastApplyError && <p className={styles.alertError} style={{ marginTop: "18px" }}>{plan.lastApplyError}</p>}

            <h3 className={styles.sectionTitle}>Scheduled slots</h3>
            {plan.slots.length === 0 ? <p className={styles.empty}>No task could be scheduled in this window.</p> : (
              <div className={styles.slotList}>
                {plan.slots.map((slot) => (
                  <article className={`${styles.slotCard} ${slot.status === "REMOVED" ? styles.slotMuted : ""}`} key={slot.id}>
                    <div>
                      <h4>{slot.taskTitle}</h4>
                      <p className={styles.slotTime}>{formatSlotTime(slot.startAt)} → {formatSlotTime(slot.endAt)} · {slot.durationMinutes} min</p>
                      <p className={styles.taskMeta}>Review: {slot.status} · Apply: {slot.applyStatus}{slot.applyError ? ` · ${slot.applyError}` : ""}</p>
                    </div>
                    {canReview && slot.status === "PROPOSED" && <button className={styles.secondaryButton} disabled={submitting} onClick={() => void removeSlot(slot)} type="button">Remove</button>}
                  </article>
                ))}
              </div>
            )}

            <h3 className={styles.sectionTitle}>Unscheduled tasks</h3>
            {plan.unscheduledTasks.length === 0 ? <p className={styles.empty}>Every eligible task in this planning window received a suggestion.</p> : (
              <div className={styles.unscheduledList}>
                {plan.unscheduledTasks.map((task) => (
                  <article className={styles.unscheduledCard} key={task.id}>
                    <div><h4>{task.taskTitle}</h4><p className={styles.taskDescription}>{unscheduledMessage(task.reason)} · {task.unscheduledMinutes} min remaining</p></div>
                    <span className={`${styles.statusPill} ${styles.statusPillWarning}`}>{task.reason}</span>
                  </article>
                ))}
              </div>
            )}

            <div className={styles.actions}>
              {canReview && <button className={styles.primaryButton} disabled={submitting} onClick={() => void approvePlan()} type="button">Approve plan</button>}
              {canApply && <button className={styles.primaryButton} disabled={submitting} onClick={() => void applyPlan()} type="button">{plan.status === "APPLY_FAILED" ? "Retry apply" : "Apply to Google Calendar"}</button>}
              {canCancel && <button className={styles.dangerButton} disabled={submitting} onClick={() => void cancelPlan()} type="button">Cancel plan</button>}
            </div>
          </>
        )}
      </div>
    </section>
  );
};

export default PlanningPage;
