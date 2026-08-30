import { NavLink, Outlet } from "react-router-dom";
import { logout } from "../../api/api";
import styles from "./AppShell.module.css";

const navigation = [
  { to: "/dashboard", label: "Overview" },
  { to: "/tasks", label: "Tasks" },
  { to: "/preferences", label: "Preferences" },
  { to: "/planning", label: "Plan" },
];

const AppShell = () => (
  <div className={styles.shell}>
    <header className={styles.header}>
      <NavLink className={styles.brand} to="/dashboard">
        FLOWTIME
      </NavLink>
      <nav aria-label="Primary navigation" className={styles.navigation}>
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
        Sign out
      </button>
    </header>
    <main className={styles.main}>
      <Outlet />
    </main>
  </div>
);

export default AppShell;
