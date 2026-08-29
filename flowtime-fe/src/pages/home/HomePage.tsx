import React, { useState } from "react";
import LoginModal from "../../components/auth/LoginModal/LoginModal";
import Dayline from "../../components/dayline/Dayline";
import styles from "./HomePage.module.css";

export const HomePage: React.FC = () => {
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  const openLogin = () => setIsLoginModalOpen(true);
  const closeLogin = () => setIsLoginModalOpen(false);

  const scrollToHowItWorks = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    const target = document.getElementById("how-it-works");
    if (target) {
      target.scrollIntoView({ behavior: "smooth" });
    }
  };

  return (
    <div className={styles.page}>
      <LoginModal isOpen={isLoginModalOpen} onClose={closeLogin} />

      <header className={styles.headerNav}>
        <a href="/" className={styles.logo}>
          FLOWTIME
        </a>
        <div className={styles.navRight}>
          <a href="#how-it-works" onClick={scrollToHowItWorks} className={styles.navLink}>
            How it works
          </a>
          <button type="button" onClick={openLogin} className={styles.navSignInBtn}>
            Sign in
          </button>
        </div>
      </header>

      <div className={styles.container}>
        <section className={styles.hero} aria-labelledby="hero-title">
          <h1 id="hero-title" className={styles.heroThesis}>
            Your calendar is full.
            <br />
            Your time isn’t.
          </h1>

          <p className={styles.heroSub}>
            FlowTime finds the right time for the things you need to do — around your calendar, priorities, and working habits.
          </p>

          <div className={styles.heroCtas}>
            <button type="button" onClick={openLogin} className={styles.primaryBtn}>
              <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
                  fill="#4285F4"
                />
                <path
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  fill="#34A853"
                />
                <path
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  fill="#FBBC05"
                />
                <path
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  fill="#EA4335"
                />
              </svg>
              <span>Continue with Google</span>
            </button>

            <a href="#how-it-works" onClick={scrollToHowItWorks} className={styles.secondaryLink}>
              See how it works ↓
            </a>
          </div>

          <div className={styles.heroDaylineWrapper}>
            <Dayline />
          </div>
        </section>

        <section id="how-it-works" className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>01 // CONCEPT</span>
            <h2 className={styles.sectionTitle}>
              A calendar shows when you’re busy. FlowTime helps you decide when to work.
            </h2>
          </div>

          <div className={styles.flowEquation}>
            <span className={styles.equationTerm}>YOUR CALENDAR</span>
            <span className={styles.equationOperator}>+</span>
            <span className={styles.equationTerm}>YOUR TASKS</span>
            <span className={styles.equationOperator}>+</span>
            <span className={styles.equationTerm}>YOUR PREFERENCES</span>
            <span className={styles.equationOperator}>→</span>
            <span className={styles.equationResult}>FLOWTIME SCHEDULING ENGINE</span>
            <span className={styles.equationOperator}>→</span>
            <span className={styles.equationResult}>BEST TIME TO WORK</span>
          </div>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>02 // SCHEDULING ENGINE</span>
            <h2 className={styles.sectionTitle}>
              Need two hours? FlowTime finds them.
            </h2>
          </div>

          <div className={styles.candidateCard}>
            <div className={styles.candidateHeader}>
              <span className={styles.candidateQuery}>findBestTime: "2 hours for Backend API"</span>
              <span className={styles.candidateBadge}>RANKED CANDIDATES</span>
            </div>

            <div className={styles.slotList}>
              <div className={`${styles.slotRow} ${styles.slotRowBest}`}>
                <div className={styles.slotLeft}>
                  <span>Wed 19:00 — 21:00</span>
                  <span className={styles.slotTag}>Best match</span>
                </div>
                <span className={styles.slotScore}>94 pts</span>
              </div>

              <div className={`${styles.slotRow} ${styles.slotRowNormal}`}>
                <div className={styles.slotLeft}>
                  <span>Thu 20:00 — 22:00</span>
                </div>
                <span className={styles.slotScore}>87 pts</span>
              </div>

              <div className={`${styles.slotRow} ${styles.slotRowNormal}`}>
                <div className={styles.slotLeft}>
                  <span>Sat 09:00 — 11:00</span>
                </div>
                <span className={styles.slotScore}>81 pts</span>
              </div>
            </div>
          </div>

          <p className={styles.sectionSub}>
            FlowTime checks your calendar, working hours, deadlines, and preferences before recommending a time.
          </p>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>03 // NATURAL LANGUAGE INTERFACE</span>
            <h2 className={styles.sectionTitle}>
              Just tell FlowTime what you need.
            </h2>
          </div>

          <div className={styles.dialogueCard}>
            <div className={styles.dialogueExchange}>
              <div className={styles.userBubble}>
                “Move my English class to Friday.”
              </div>
              <div className={styles.assistantBubble}>
                <div className={styles.assistantText}>
                  I checked Friday: 14:00–16:00 is completely open and matches your focus preferences.
                </div>
                <div className={styles.confirmCard}>
                  <span>Pending change: English Class → Friday 14:00–16:00</span>
                  <button type="button" onClick={openLogin} className={styles.confirmActionBtn}>
                    Confirm change
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>04 // INTEGRATION</span>
            <h2 className={styles.sectionTitle}>
              Everything that shapes your time.
            </h2>
          </div>

          <div className={styles.pillarsGrid}>
            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>CALENDAR</span>
              <h3 className={styles.pillarHeading}>What’s already happening.</h3>
              <p className={styles.pillarDesc}>
                Direct two-way Google Calendar synchronization. Free and busy blocks as your ground truth.
              </p>
            </div>

            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>TASKS</span>
              <h3 className={styles.pillarHeading}>What needs to happen.</h3>
              <p className={styles.pillarDesc}>
                Tasks with durations, priorities, deadlines, and split preferences waiting for the right slot.
              </p>
            </div>

            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>PREFERENCES</span>
              <h3 className={styles.pillarHeading}>How you prefer to work.</h3>
              <p className={styles.pillarDesc}>
                Working hours, break buffers, no-weekend rules, and peak energy windows defined by you.
              </p>
            </div>
          </div>
        </section>

        <section className={styles.privacySection}>
          <h2 className={styles.privacyTitle}>Your calendar stays yours.</h2>
          <p className={styles.privacyText}>
            FlowTime plans around your calendar. Changes to your schedule happen only when you explicitly confirm them.
          </p>
        </section>

        <section className={styles.finalCta}>
          <h2 className={styles.finalHeadline}>
            Your time is already there.
            <br />
            FlowTime helps you find it.
          </h2>

          <button type="button" onClick={openLogin} className={styles.primaryBtn}>
            Continue with Google
          </button>
        </section>
      </div>

      <footer className={styles.footer}>
        <span className={styles.footerCode}>FLOWTIME // TIME, ORGANIZED.</span>
        <span className={styles.footerCode}>PRECISION TIME ENGINE · SPRING AI · GOOGLE OAUTH</span>
      </footer>
    </div>
  );
};

export default HomePage;
