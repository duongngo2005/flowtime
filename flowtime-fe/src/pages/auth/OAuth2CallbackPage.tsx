import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import styles from "./OAuth2CallbackPage.module.css";

const OAuth2CallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  useEffect(() => {
    if (!token) return;

    localStorage.setItem("access_token", token);
    window.history.replaceState({}, "", "/oauth2/callback");
    navigate("/dashboard", { replace: true });
  }, [navigate, token]);

  if (!token) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h2 className={styles.errorTitle}>Đăng nhập không thành công</h2>
          <p className={styles.errorMessage}>
            Không thể hoàn tất đăng nhập bằng Google. Vui lòng thử lại.
          </p>
          <button
            type="button"
            onClick={() => navigate("/", { replace: true })}
            className={styles.button}
          >
            Về trang chủ
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <p className={styles.loadingText}>
        Đang kết nối lịch và thiết lập phiên làm việc…
      </p>
    </div>
  );
};

export default OAuth2CallbackPage;
