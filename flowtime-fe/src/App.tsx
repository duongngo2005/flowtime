import { Routes, Route, Navigate, Outlet } from "react-router-dom";
import HomePage from "./pages/home/HomePage";
import DashboardPage from "./pages/dashboard/DashboardPage";
import OAuth2CallbackPage from "./pages/auth/OAuth2CallbackPage";
import AppShell from "./components/layout/AppShell";
import TasksPage from "./pages/tasks/TasksPage";
import PreferencesPage from "./pages/preferences/PreferencesPage";
import PlanningPage from "./pages/planning/PlanningPage";

const ProtectedRoute = () => {
  const token = localStorage.getItem("access_token");
  return token ? <Outlet /> : <Navigate to="/" replace />;
};

const PublicHomeRoute = () => {
  const token = localStorage.getItem("access_token");
  if (token) {
    return <Navigate to="/dashboard" replace />;
  }
  return <HomePage />;
};

const App = () => {
  return (
    <Routes>
      <Route path="/" element={<PublicHomeRoute />} />
      <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/preferences" element={<PreferencesPage />} />
          <Route path="/planning" element={<PlanningPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
