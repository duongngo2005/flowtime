import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import styles from "./OAuth2CallbackPage.module.css";

const OAuth2CallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get("token");

    if (token) {
      localStorage.setItem("access_token", token);
      window.history.replaceState({}, "", "/oauth2/callback");
      navigate("/dashboard", { replace: true });
    } else {
      setError("Google sign-in could not be completed. Please try again.");
    }
  }, [searchParams, navigate]);

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h2 className={styles.errorTitle}>Authentication Failed</h2>
          <p className={styles.errorMessage}>{error}</p>
          <button
            type="button"
            onClick={() => navigate("/", { replace: true })}
            className={styles.button}
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <p className={styles.loadingText}>
        Connecting your calendar and setting up session…
      </p>
    </div>
  );
};

export default OAuth2CallbackPage;
