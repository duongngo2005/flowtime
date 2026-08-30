export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type TaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface TaskPayload {
  title: string;
  description: string | null;
  estimatedDuration: number;
  priority: TaskPriority;
  deadline: string | null;
  preferredStartTime: string | null;
  preferredEndTime: string | null;
  minSessionDuration: number | null;
  maxDailyMinutes: number | null;
  splitAllowed: boolean;
  category: string | null;
}

export interface Task extends TaskPayload {
  id: number;
  status: TaskStatus;
  createdAt: string;
  updatedAt: string;
}

export type WorkingDay =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface SchedulingPreferences {
  timezone: string;
  workdayStartTime: string;
  workdayEndTime: string;
  workingDays: WorkingDay[];
  focusDurationMinutes: number;
  breakDurationMinutes: number;
  dailyFocusLimit: number;
  configured: boolean;
}

export type SchedulingPreferencesPayload = Omit<SchedulingPreferences, "configured">;

export type PlanningStatus = "DRAFT" | "APPROVED" | "APPLYING" | "APPLY_FAILED" | "CANCELLED" | "APPLIED";
export type PlannedSlotStatus = "PROPOSED" | "ACCEPTED" | "REMOVED";
export type PlannedSlotApplyStatus = "NOT_REQUESTED" | "PENDING" | "APPLYING" | "APPLIED" | "FAILED";

export interface PlannedSlot {
  id: number;
  taskId: number;
  taskTitle: string;
  startAt: string;
  endAt: string;
  durationMinutes: number;
  status: PlannedSlotStatus;
  googleCalendarId: string;
  googleEventId: string;
  applyStatus: PlannedSlotApplyStatus;
  applyError: string | null;
  appliedAt: string | null;
}

export interface UnscheduledTask {
  id: number;
  taskId: number;
  taskTitle: string;
  unscheduledMinutes: number;
  reason: string;
}

export interface PlanningSession {
  id: number;
  startDate: string;
  endDate: string;
  timezone: string;
  status: PlanningStatus;
  createdAt: string;
  updatedAt: string;
  applyAttempts: number;
  applyStartedAt: string | null;
  appliedAt: string | null;
  lastApplyError: string | null;
  slots: PlannedSlot[];
  unscheduledTasks: UnscheduledTask[];
}
