import uniLogo from '../assets/Uni_logo.png';

export default function AuthBrand({ eyebrow = 'Equipment Booking Platform', headline, lede }) {
  return (
    <aside className="auth-brand">
      <div className="brand-header">
        <div className="brand-logo">
          <img src={uniLogo} alt="University of Sri Jayewardenepura" />
        </div>
        <div className="brand-mark">
          <div className="brand-uni">University of <em>Sri Jayewardenepura</em></div>
          <div className="brand-name">Smart Lab · Equipment Booking</div>
        </div>
      </div>

      <div className="brand-body">
        <div className="brand-eyebrow brand-eyebrow-pill">
          <span className="brand-eyebrow-dot" />
          {eyebrow}
        </div>
        <h2 className="brand-headline">{headline}</h2>
        <p className="brand-lede">{lede}</p>

        <div className="brand-trust">
          <div className="brand-trust-item">
            <div className="brand-trust-num">120<span>+</span></div>
            <div className="brand-trust-label">Instruments</div>
          </div>
          <div className="brand-trust-divider" />
          <div className="brand-trust-item">
            <div className="brand-trust-num">5</div>
            <div className="brand-trust-label">Departments</div>
          </div>
          <div className="brand-trust-divider" />
          <div className="brand-trust-item">
            <div className="brand-trust-num">24<span>h</span></div>
            <div className="brand-trust-label">Avg. approval</div>
          </div>
        </div>
      </div>

      <div className="brand-footer">
        <div className="brand-meta-row">
          <div>
            <span>Faculty</span>
            <span>Engineering</span>
          </div>
          <div>
            <span>Platform</span>
            <span>Smart Lab v1.0</span>
          </div>
          <div>
            <span>Since</span>
            <span>2026</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
