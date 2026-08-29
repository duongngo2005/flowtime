import { Routes, Route, Navigate } from "react-router-dom";
import React from "react";
import HomePage from "./pages/home/HomePage";
import DashboardPage from "./pages/dashboard/DashboardPage";
import OAuth2CallbackPage from "./pages/auth/OAuth2CallbackPage";

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const token = localStorage.getItem("access_token");
  return token ? <>{children}</> : <Navigate to="/" replace />;
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
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;