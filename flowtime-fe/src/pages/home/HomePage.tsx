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
            Cách hoạt động
          </a>
          <button type="button" onClick={openLogin} className={styles.navSignInBtn}>
            Đăng nhập
          </button>
        </div>
      </header>

      <div className={styles.container}>
        <section className={styles.hero} aria-labelledby="hero-title">
          <h1 id="hero-title" className={styles.heroThesis}>
            Lịch của bạn có thể bận.
            <br />
            Thời gian vẫn có thể được sắp xếp.
          </h1>

          <p className={styles.heroSub}>
            FlowTime lập kế hoạch cho các nhiệm vụ còn lại dựa trên lịch đã đồng bộ, ưu tiên và thói quen làm việc của bạn.
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
              <span>Tiếp tục với Google</span>
            </button>

            <a href="#how-it-works" onClick={scrollToHowItWorks} className={styles.secondaryLink}>
              Xem cách hoạt động ↓
            </a>
          </div>

          <div className={styles.heroDaylineWrapper}>
            <Dayline />
          </div>
        </section>

        <section id="how-it-works" className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>01 // QUY TRÌNH</span>
            <h2 className={styles.sectionTitle}>
              Lịch cho biết khi nào bạn bận. FlowTime giúp bạn tạo một bản kế hoạch để xem xét.
            </h2>
          </div>

          <div className={styles.flowEquation}>
            <span className={styles.equationTerm}>LỊCH CỦA BẠN</span>
            <span className={styles.equationOperator}>+</span>
            <span className={styles.equationTerm}>NHIỆM VỤ CỦA BẠN</span>
            <span className={styles.equationOperator}>+</span>
            <span className={styles.equationTerm}>SỞ THÍCH CỦA BẠN</span>
            <span className={styles.equationOperator}>→</span>
            <span className={styles.equationResult}>BỘ LẬP LỊCH FLOWTIME</span>
            <span className={styles.equationOperator}>→</span>
            <span className={styles.equationResult}>BẢN NHÁP KẾ HOẠCH</span>
          </div>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>02 // LẬP KẾ HOẠCH</span>
            <h2 className={styles.sectionTitle}>
              Cần thời gian tập trung? Hãy để FlowTime đề xuất các khung giờ phù hợp.
            </h2>
          </div>

          <div className={styles.candidateCard}>
            <div className={styles.candidateHeader}>
              <span className={styles.candidateQuery}>Tạo bản nháp cho: “2 giờ làm Backend API”</span>
              <span className={styles.candidateBadge}>KHUNG GIỜ ĐỀ XUẤT</span>
            </div>

            <div className={styles.slotList}>
              <div className={`${styles.slotRow} ${styles.slotRowBest}`}>
                <div className={styles.slotLeft}>
                  <span>Thứ Tư 19:00 — 21:00</span>
                  <span className={styles.slotTag}>Đề xuất</span>
                </div>
                <span className={styles.slotScore}>xem xét</span>
              </div>

              <div className={`${styles.slotRow} ${styles.slotRowNormal}`}>
                <div className={styles.slotLeft}>
                  <span>Thứ Năm 20:00 — 22:00</span>
                </div>
                <span className={styles.slotScore}>xem xét</span>
              </div>

              <div className={`${styles.slotRow} ${styles.slotRowNormal}`}>
                <div className={styles.slotLeft}>
                  <span>Thứ Bảy 09:00 — 11:00</span>
                </div>
                <span className={styles.slotScore}>xem xét</span>
              </div>
            </div>
          </div>

          <p className={styles.sectionSub}>
            FlowTime kiểm tra lịch đã đồng bộ, giờ làm việc, hạn chót và sở thích trước khi tạo đề xuất.
          </p>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>03 // BẠN QUYẾT ĐỊNH</span>
            <h2 className={styles.sectionTitle}>
              Xem xét từng đề xuất trước khi thay đổi lịch.
            </h2>
          </div>

          <div className={styles.dialogueCard}>
            <div className={styles.dialogueExchange}>
              <div className={styles.userBubble}>
                “Tôi đồng ý áp dụng các khung giờ này.”
              </div>
              <div className={styles.assistantBubble}>
                <div className={styles.assistantText}>
                  Bản nháp chỉ lưu đề xuất. Google Calendar chỉ được thay đổi sau khi bạn phê duyệt và áp dụng kế hoạch.
                </div>
                <div className={styles.confirmCard}>
                  <span>Chờ xác nhận: tạo các sự kiện đã chọn</span>
                  <button type="button" onClick={openLogin} className={styles.confirmActionBtn}>
                    Bắt đầu với Google
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionPre}>04 // DỮ LIỆU ĐẦU VÀO</span>
            <h2 className={styles.sectionTitle}>
              Mọi dữ kiện tạo nên kế hoạch của bạn.
            </h2>
          </div>

          <div className={styles.pillarsGrid}>
            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>LỊCH</span>
              <h3 className={styles.pillarHeading}>Những việc đã diễn ra.</h3>
              <p className={styles.pillarDesc}>
                Đồng bộ các lịch Google vào dữ liệu cục bộ để xác định thời gian bận trước khi lập kế hoạch.
              </p>
            </div>

            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>NHIỆM VỤ</span>
              <h3 className={styles.pillarHeading}>Những việc cần hoàn thành.</h3>
              <p className={styles.pillarDesc}>
                Nhiệm vụ có thời lượng, ưu tiên, hạn chót và tùy chọn chia nhỏ để đưa vào các khung giờ phù hợp.
              </p>
            </div>

            <div className={styles.pillarCol}>
              <span className={styles.pillarTag}>SỞ THÍCH</span>
              <h3 className={styles.pillarHeading}>Cách bạn muốn làm việc.</h3>
              <p className={styles.pillarDesc}>
                Giờ làm việc, thời gian nghỉ, ngày làm việc và giới hạn tập trung mỗi ngày do bạn thiết lập.
              </p>
            </div>
          </div>
        </section>

        <section className={styles.privacySection}>
          <h2 className={styles.privacyTitle}>Lịch của bạn vẫn thuộc về bạn.</h2>
          <p className={styles.privacyText}>
            FlowTime lập kế hoạch dựa trên lịch của bạn. Lịch chỉ thay đổi khi bạn chủ động phê duyệt và áp dụng kế hoạch.
          </p>
        </section>

        <section className={styles.finalCta}>
          <h2 className={styles.finalHeadline}>
            Thời gian của bạn vẫn ở đó.
            <br />
            FlowTime giúp bạn sắp xếp thời gian đó.
          </h2>

          <button type="button" onClick={openLogin} className={styles.primaryBtn}>
            Tiếp tục với Google
          </button>
        </section>
      </div>

      <footer className={styles.footer}>
        <span className={styles.footerCode}>FLOWTIME // SẮP XẾP THỜI GIAN.</span>
        <span className={styles.footerCode}>BỘ LẬP LỊCH · GOOGLE OAUTH</span>
      </footer>
    </div>
  );
};

export default HomePage;
