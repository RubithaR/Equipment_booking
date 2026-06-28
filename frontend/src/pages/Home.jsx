import { Link, useNavigate } from 'react-router-dom';
import uniLogo from '../assets/Uni_logo.png';
import { getCurrentUser, homePathFor } from '../auth';

export default function Home() {
  const nav = useNavigate();
  const user = getCurrentUser();

  const goDashboard = () => {
    if (user) nav(homePathFor(user.role));
    else nav('/login');
  };

  return (
    <div className="home-wrap">
      {/* === Top Nav === */}
      <header className="home-nav">
        <div className="home-nav-inner">
          <div className="home-brand">
            <div className="home-logo">
              <img src={uniLogo} alt="University of Sri Jayewardenepura" />
            </div>
            <div className="home-brand-text">
              <span className="home-uni">University of Sri Jayewardenepura</span>
              <span className="home-platform">Smart<em>Lab</em> · Equipment Booking</span>
            </div>
          </div>

          <nav className="home-nav-links">
            <a href="#how">How it works</a>
            <a href="#categories">Categories</a>
            <a href="#contact">Contact</a>
          </nav>

          <div className="home-nav-cta">
            {user ? (
              <button className="btn-pill btn-pill-solid" onClick={goDashboard}>
                Go to Dashboard →
              </button>
            ) : (
              <>
                <Link to="/login" className="btn-pill btn-pill-ghost">Log in</Link>
                <Link to="/register" className="btn-pill btn-pill-solid">Sign up</Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* === Hero === */}
      <section className="home-hero">
        <div className="home-hero-grid">
          <div className="home-hero-copy">
            <div className="home-eyebrow">
              <span className="home-eyebrow-dot" />
              Faculty of Engineering · Equipment Booking Platform
            </div>
            <h1 className="home-headline">
              Reserve <em>equipment</em><br />
              in minutes — not days.
            </h1>
            <p className="home-lede">
              Browse the university's full inventory, request the instrument you need, and let your
              instructor approve in one click. Built for students, instructors and admin in one place.
            </p>

            <div className="home-cta-row">
              <Link to="/register" className="btn-pill btn-pill-solid btn-pill-lg">
                Get started — it's free
              </Link>
              <Link to="/login" className="btn-pill btn-pill-ghost btn-pill-lg">
                I have an account
              </Link>
            </div>

            <div className="home-stats">
              <div className="home-stat">
                <div className="home-stat-num">120<span>+</span></div>
                <div className="home-stat-label">Instruments listed</div>
              </div>
              <div className="home-stat">
                <div className="home-stat-num">5</div>
                <div className="home-stat-label">Engineering departments</div>
              </div>
              <div className="home-stat">
                <div className="home-stat-num">24<span>h</span></div>
                <div className="home-stat-label">Average approval time</div>
              </div>
            </div>
          </div>

          <div className="home-hero-art">
            <div className="hero-card hero-card-main">
              <div className="hero-card-row">
                <span className="hero-tag">Electronics</span>
                <span className="hero-status hero-status-ok">Available</span>
              </div>
              <div className="hero-card-title">Tektronix Oscilloscope</div>
              <div className="hero-card-meta">Lab A-101 · TBS1052B 50 MHz</div>
              <div className="hero-card-foot">
                <div>
                  <div className="hero-card-foot-label">Slot</div>
                  <div className="hero-card-foot-value">Wed · 10:00–12:00</div>
                </div>
                <button className="btn-pill btn-pill-solid btn-pill-sm">Book now</button>
              </div>
            </div>

            


            <div className="hero-bg-blob" aria-hidden="true" />
          </div>
        </div>
      </section>

      {/* === How it works === */}
      <section className="home-how" id="how">
        <div className="home-section-head">
          
          <h2 className="home-section-title">Three steps from request to lab bench.</h2>
        </div>

        <div className="how-grid">
          <div className="how-step">
            <div className="how-step-num">01</div>
            <h3>Register with your university email</h3>
            <p>Students get instant access. Instructors are reviewed by the admin to keep approvals trusted.</p>
          </div>
          <div className="how-step">
            <div className="how-step-num">02</div>
            <h3>Browse the live inventory</h3>
            <p>Filter by department, status, or location. See exactly which instruments are free to book right now.</p>
          </div>
          <div className="how-step">
            <div className="how-step-num">03</div>
            <h3>Reserve and get approved</h3>
            <p>Pick a time slot, send the request, and get notified the moment your instructor approves it.</p>
          </div>
        </div>
      </section>

      {/* === Categories === */}
      <section className="home-cats" id="categories">
        <div className="home-section-head">

          <h2 className="home-section-title">Equipment from every engineering discipline.</h2>
        </div>

        <div className="cat-grid">
          {[
            { name: 'Computer Engineering', count: '32 items', icon: '⌬' },
            { name: 'Electrical & Electronic', count: '28 items', icon: '⚡' },
            { name: 'Mechanical Engineering', count: '24 items', icon: '⚙' },
            { name: 'Civil Engineering', count: '18 items', icon: '◮' },
            { name: 'Manufacturing & Industrial', count: '21 items', icon: '✦' },
          ].map((c) => (
            <div className="cat-card" key={c.name}>
              <div className="cat-icon">{c.icon}</div>
              <div className="cat-name">{c.name}</div>
              <div className="cat-count">{c.count}</div>
            </div>
          ))}
        </div>
      </section>

      {/* === CTA Strip === */}
      <section className="home-cta-strip">
        <div className="home-cta-strip-inner">
          <div>
            <div className="home-section-eyebrow">— Ready to book?</div>
            <h2 className="home-section-title light">Your next equipment is one click away.</h2>
          </div>
          <div className="home-cta-strip-actions">
            <Link to="/register" className="btn-pill btn-pill-accent btn-pill-lg">Create your account</Link>
            <Link to="/login" className="btn-pill btn-pill-ghost-light btn-pill-lg">Sign in</Link>
          </div>
        </div>
      </section>

      {/* === Footer === */}
      <footer className="home-foot" id="contact">
        <div className="home-foot-inner">
          <div className="home-foot-brand">
            <div className="home-logo home-logo-sm">
              <img src={uniLogo} alt="" />
            </div>
            <div>
              <div className="home-uni">University of Sri Jayewardenepura</div>
              <div className="home-platform">Smart<em>Lab</em> · Equipment Booking</div>
            </div>
          </div>
          <div className="home-foot-meta">
            <div>Faculty of Engineering</div>
            <div>·</div>
            <div>© 2026 SmartLab</div>
          </div>
        </div>
      </footer>
    </div>
  );
}
