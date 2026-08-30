import { NavLink, Outlet } from "react-router-dom";
import { logout } from "../../api/api";
import styles from "./AppShell.module.css";

const navigation = [
  { to: "/dashboard", label: "Tổng quan" },
  { to: "/tasks", label: "Nhiệm vụ" },
  { to: "/preferences", label: "Sở thích" },
  { to: "/planning", label: "Kế hoạch" },
];

const AppShell = () => (
  <div className={styles.shell}>
    <header className={styles.header}>
      <NavLink className={styles.brand} to="/dashboard">
        FLOWTIME
      </NavLink>
      <nav aria-label="Điều hướng chính" className={styles.navigation}>
        {navigation.map((item) => (
          <NavLink
            className={({ isActive }) => `${styles.navLink} ${isActive ? styles.activeNavLink : ""}`}
            key={item.to}
            to={item.to}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <button className={styles.signOutButton} onClick={() => void logout()} type="button">
        Đăng xuất
      </button>
    </header>
    <main className={styles.main}>
      <Outlet />
    </main>
  </div>
);

export default AppShell;
