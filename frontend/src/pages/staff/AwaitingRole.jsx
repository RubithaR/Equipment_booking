import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi, errMsg } from '../../api';
import { getCurrentUser, logout } from '../../auth';
import uniLogo from '../../assets/Uni_logo.png';

const roleLabel = (r) =>
  ({ INSTRUCTOR: 'Instructor', LECTURER: 'Lecturer', HOD: 'Head of Department' }[r] || r);

// Welcome / holding page for a registered staff member whose role hasn't been
// assigned yet. Polls our own user record; the moment an admin assigns a real
// role we prompt a fresh sign-in so a new token (with the role) is issued.
export default function AwaitingRole() {
  const nav = useNavigate();
  const me = getCurrentUser();
  const [assignedRole, setAssignedRole] = useState(null);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState('');

  const check = async () => {
    if (!me?.id) return;
    setChecking(true);
    setError('');
    try {
      const { data } = await userApi.getById(me.id);
      if (data.role && data.role !== 'STAFF') setAssignedRole(data.role);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setChecking(false);
    }
  };

  // Auto-check on mount, then every 15s until a role is assigned.
  useEffect(() => {
    check();
    const t = setInterval(check, 15000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const signInAgain = () => {
    logout();
    nav('/login', { replace: true });
  };

  return (
    <div className="container">
      <section className="welcome-hero">
        <div className="welcome-logo">
          <img src={uniLogo} alt="University of Sri Jayewardenepura" />
        </div>
        <h1 className="welcome-title">Welcome to University of <em>Sri Jayewardenepura</em></h1>
        <p className="welcome-lede">
          Hi {me?.fullName || 'there'}, your staff account has been created successfully.
        </p>
      </section>

      {assignedRole ? (
        <div className="welcome-card welcome-card-ok">
          <div className="welcome-badge welcome-badge-ok">🎉</div>
          <h2>You're all set!</h2>
          <p>
            An administrator assigned you the role of <strong>{roleLabel(assignedRole)}</strong>.
            Sign in again to open your dashboard.
          </p>
          <button className="btn-pill btn-pill-solid btn-pill-lg" onClick={signInAgain}>
            Sign in again →
          </button>
        </div>
      ) : (
        <div className="welcome-card">
          <div className="welcome-badge">⏳</div>
          <h2>Waiting for the administrator to assign your role</h2>
          <p>
            Once an admin assigns you a role — <strong>Head of Department</strong>,
            {' '}<strong>Lecturer</strong> or <strong>Instructor</strong> — you'll be able to sign in
            and use your dashboard.
          </p>

          <ol className="welcome-steps">
            <li className="is-done"><span>1</span> Account created</li>
            <li className="is-active"><span>2</span> Awaiting role assignment</li>
            <li><span>3</span> Sign in to your dashboard</li>
          </ol>

          <p className="welcome-hint">This page checks automatically every few seconds.</p>

          {error && <div className="alert alert-error">{error}</div>}

          <button className="btn-pill btn-pill-ghost btn-pill-lg" onClick={check} disabled={checking}>
            {checking ? 'Checking…' : '↻ Check now'}
          </button>
        </div>
      )}
    </div>
  );
}
