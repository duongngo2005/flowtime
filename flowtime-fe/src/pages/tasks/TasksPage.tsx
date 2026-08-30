import { useCallback, useEffect, useState, type FormEvent } from "react";
import api from "../../api/api";
import type { Task, TaskPayload, TaskPriority } from "../../api/contracts";
import { getErrorMessage } from "../../api/errors";
import styles from "../workspace/WorkspacePage.module.css";

interface TaskForm {
  title: string;
  description: string;
  estimatedDuration: string;
  priority: TaskPriority;
  deadline: string;
  preferredStartTime: string;
  preferredEndTime: string;
  minSessionDuration: string;
  splitAllowed: boolean;
  category: string;
}

const emptyForm = (): TaskForm => ({
  title: "",
  description: "",
  estimatedDuration: "60",
  priority: "MEDIUM",
  deadline: "",
  preferredStartTime: "",
  preferredEndTime: "",
  minSessionDuration: "",
  splitAllowed: false,
  category: "",
});

const dateTimeForInput = (value: string | null): string => {
  if (!value) return "";
  const date = new Date(value);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
};

const formForTask = (task: Task): TaskForm => ({
  title: task.title,
  description: task.description || "",
  estimatedDuration: String(task.estimatedDuration),
  priority: task.priority,
  deadline: dateTimeForInput(task.deadline),
  preferredStartTime: task.preferredStartTime?.slice(0, 5) || "",
  preferredEndTime: task.preferredEndTime?.slice(0, 5) || "",
  minSessionDuration: task.minSessionDuration ? String(task.minSessionDuration) : "",
  splitAllowed: task.splitAllowed,
  category: task.category || "",
});

const formatDateTime = (value: string | null): string =>
  value ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "No deadline";

const TasksPage = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [form, setForm] = useState<TaskForm>(emptyForm);
  const [editingTaskId, setEditingTaskId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadTasks = useCallback(async () => {
    try {
      setError(null);
      const response = await api.get<Task[]>("/api/v1/tasks");
      setTasks(response.data);
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not load your tasks."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      void loadTasks();
    }, 0);

    return () => window.clearTimeout(requestId);
  }, [loadTasks]);

  const resetForm = () => {
    setEditingTaskId(null);
    setForm(emptyForm());
  };

  const submitTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);

    const payload: TaskPayload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      estimatedDuration: Number(form.estimatedDuration),
      priority: form.priority,
      deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      preferredStartTime: form.preferredStartTime || null,
      preferredEndTime: form.preferredEndTime || null,
      minSessionDuration: form.minSessionDuration ? Number(form.minSessionDuration) : null,
      splitAllowed: form.splitAllowed,
      category: form.category.trim() || null,
    };

    try {
      if (editingTaskId) {
        await api.put(`/api/v1/tasks/${editingTaskId}`, payload);
        setNotice("Task updated.");
      } else {
        await api.post("/api/v1/tasks", payload);
        setNotice("Task created.");
      }
      resetForm();
      await loadTasks();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not save this task."));
    } finally {
      setSaving(false);
    }
  };

  const completeTask = async (taskId: number) => {
    try {
      setError(null);
      await api.patch(`/api/v1/tasks/${taskId}/complete`);
      setNotice("Task marked as complete.");
      await loadTasks();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not complete this task."));
    }
  };

  const deleteTask = async (taskId: number) => {
    if (!window.confirm("Delete this task? This cannot be undone.")) return;

    try {
      setError(null);
      await api.delete(`/api/v1/tasks/${taskId}`);
      setNotice("Task deleted.");
      if (editingTaskId === taskId) resetForm();
      await loadTasks();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Could not delete this task."));
    }
  };

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Task backlog</p>
          <h1 className={styles.title}>Plan the work that matters.</h1>
          <p className={styles.subtitle}>Create tasks first; FlowTime will schedule only incomplete, uncommitted work.</p>
        </div>
      </div>

      {error && <p className={styles.alertError} role="alert">{error}</p>}
      {notice && <p className={styles.alertSuccess} role="status">{notice}</p>}

      <div className={styles.twoColumn}>
        <form className={styles.panel} onSubmit={submitTask}>
          <h2 className={styles.panelTitle}>{editingTaskId ? "Edit task" : "Create task"}</h2>
          <p className={styles.panelHint}>Keep duration realistic. Deadline and preferred hours guide the scheduler.</p>

          <div className={styles.formGrid}>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              Title
              <input className={styles.input} maxLength={255} onChange={(event) => setForm({ ...form, title: event.target.value })} required value={form.title} />
            </label>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              Description <span aria-hidden="true">(optional)</span>
              <textarea className={styles.textarea} maxLength={10_000} onChange={(event) => setForm({ ...form, description: event.target.value })} value={form.description} />
            </label>
            <label className={styles.field}>
              Duration (minutes)
              <input className={styles.input} min="1" onChange={(event) => setForm({ ...form, estimatedDuration: event.target.value })} required type="number" value={form.estimatedDuration} />
            </label>
            <label className={styles.field}>
              Priority
              <select className={styles.select} onChange={(event) => setForm({ ...form, priority: event.target.value as TaskPriority })} value={form.priority}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </label>
            <label className={`${styles.field} ${styles.fullWidth}`}>
              Deadline <span aria-hidden="true">(optional)</span>
              <input className={styles.input} onChange={(event) => setForm({ ...form, deadline: event.target.value })} type="datetime-local" value={form.deadline} />
            </label>
            <label className={styles.field}>
              Prefer from <span aria-hidden="true">(optional)</span>
              <input className={styles.input} onChange={(event) => setForm({ ...form, preferredStartTime: event.target.value })} type="time" value={form.preferredStartTime} />
            </label>
            <label className={styles.field}>
              Prefer until <span aria-hidden="true">(optional)</span>
              <input className={styles.input} onChange={(event) => setForm({ ...form, preferredEndTime: event.target.value })} type="time" value={form.preferredEndTime} />
            </label>
            <label className={styles.field}>
              Minimum session <span aria-hidden="true">(optional)</span>
              <input className={styles.input} min="1" onChange={(event) => setForm({ ...form, minSessionDuration: event.target.value })} type="number" value={form.minSessionDuration} />
            </label>
            <label className={styles.field}>
              Category <span aria-hidden="true">(optional)</span>
              <input className={styles.input} maxLength={100} onChange={(event) => setForm({ ...form, category: event.target.value })} value={form.category} />
            </label>
            <label className={`${styles.checkboxLabel} ${styles.fullWidth}`}>
              <input checked={form.splitAllowed} onChange={(event) => setForm({ ...form, splitAllowed: event.target.checked })} type="checkbox" />
              Allow FlowTime to split this task across sessions
            </label>
          </div>

          <div className={styles.actions}>
            <button className={styles.primaryButton} disabled={saving} type="submit">
              {saving ? "Saving…" : editingTaskId ? "Save changes" : "Create task"}
            </button>
            {editingTaskId && <button className={styles.secondaryButton} onClick={resetForm} type="button">Cancel edit</button>}
          </div>
        </form>

        <div className={styles.panel}>
          <h2 className={styles.panelTitle}>Your tasks</h2>
          <p className={styles.panelHint}>Completed tasks are retained for history and excluded from new plans.</p>
          {loading ? (
            <p className={styles.empty}>Loading tasks…</p>
          ) : tasks.length === 0 ? (
            <p className={styles.empty}>No tasks yet. Create one to start planning.</p>
          ) : (
            <div className={styles.taskList}>
              {tasks.map((task) => (
                <article className={styles.taskCard} key={task.id}>
                  <div>
                    <h3 className={task.status === "COMPLETED" ? styles.completed : undefined}>{task.title}</h3>
                    {task.description && <p className={styles.taskDescription}>{task.description}</p>}
                    <div className={styles.taskMeta}>
                      <span>{task.estimatedDuration} min</span>
                      <span>{task.priority}</span>
                      <span>{formatDateTime(task.deadline)}</span>
                      {task.category && <span>{task.category}</span>}
                    </div>
                  </div>
                  <div className={styles.taskActions}>
                    {task.status !== "COMPLETED" && <button className={styles.textButton} onClick={() => void completeTask(task.id)} type="button">Complete</button>}
                    <button className={styles.textButton} onClick={() => { setEditingTaskId(task.id); setForm(formForTask(task)); setNotice(null); }} type="button">Edit</button>
                    <button className={`${styles.textButton} ${styles.textButtonDanger}`} onClick={() => void deleteTask(task.id)} type="button">Delete</button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
};

export default TasksPage;
