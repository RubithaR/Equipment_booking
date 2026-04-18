import { Link } from 'react-router-dom';
import uniLogo from '../assets/Uni_logo.png';

export default function Terms() {
  return (
    <div className="terms-wrap">
      <header className="terms-head">
        <Link to="/" className="terms-brand">
          <div className="home-logo home-logo-sm">
            <img src={uniLogo} alt="" />
          </div>
          <div>
            <div className="home-uni">University of Sri Jayewardenepura</div>
            <div className="home-platform">Smart<em>Lab</em> · Equipment Booking</div>
          </div>
        </Link>
        <Link to="/" className="auth-back" style={{ margin: 0 }}>← Back to home</Link>
      </header>

      <main className="terms-main">
        <div className="auth-step-badge" style={{ marginBottom: 18 }}>
          <span className="auth-step-dot" /> Last updated · April 2026
        </div>
        <h1 className="terms-title">Terms &amp; Conditions</h1>
        <p className="terms-lede">
          By creating an account on Smart Lab · Equipment Booking, you agree to the following
          terms. Please read them carefully — they protect you, your peers, and university property.
        </p>

        <section className="terms-section">
          <h2>1. Eligibility</h2>
          <p>Accounts are issued to current students, instructors and authorised staff of the
          Faculty of Engineering, University of Sri Jayewardenepura. Instructor accounts require
          administrator approval before use.</p>
        </section>

        <section className="terms-section">
          <h2>2. Use of equipment</h2>
          <p>Booked equipment must be used solely for academic and research purposes within
          designated lab spaces. You are responsible for the equipment from the start of your
          approved slot until you return it in working condition.</p>
        </section>

        <section className="terms-section">
          <h2>3. Booking conduct</h2>
          <p>Reservations should reflect genuine intent to use the equipment. Repeated no-shows,
          late returns, or abandonment of bookings may lead to suspension of booking privileges
          at the discretion of the lab administrator.</p>
        </section>

        <section className="terms-section">
          <h2>4. Damage and loss</h2>
          <p>Any damage, loss, or malfunction must be reported to the lab instructor immediately.
          Costs arising from negligence may be recovered in line with university policy.</p>
        </section>

        <section className="terms-section">
          <h2>5. Account security</h2>
          <p>You are responsible for keeping your login credentials confidential. Do not share
          your account or password with another user. Notify the administrator immediately if you
          suspect unauthorised access.</p>
        </section>

        <section className="terms-section">
          <h2>6. Data &amp; privacy</h2>
          <p>We store your name, university email, department, and booking history to operate
          the service. Data is held within the university's systems and is not shared with third
          parties for marketing.</p>
        </section>

        <section className="terms-section">
          <h2>7. Changes</h2>
          <p>These terms may be updated from time to time. Material changes will be communicated
          via in-app notification. Continued use of the platform after such changes constitutes
          acceptance.</p>
        </section>

        <p className="terms-foot">
          Questions? Contact the Faculty of Engineering lab administrator. By clicking
          <em> "I agree to the Terms &amp; Conditions"</em> on the registration form, you confirm
          you have read and accept these terms.
        </p>

        <Link to="/register" className="btn-pill btn-pill-solid btn-pill-lg" style={{ marginTop: 24 }}>
          ← Back to registration
        </Link>
      </main>
    </div>
  );
}
