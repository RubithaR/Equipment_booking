import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi, errMsg } from '../api';
import { setSession, homePathFor } from '../auth';
import AuthBrand from '../components/AuthBrand';
import PasswordInput from '../components/PasswordInput';

export default function Login() {
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const { data } = await userApi.login(email, password);
      setSession(data);
      nav(homePathFor(data.role), { replace: true });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <AuthBrand
        eyebrow="Welcome back"
        headline={<>Smart <em>Lab</em> · Equipment Booking</>}
        lede="One sign-in for students, instructors and admin. Browse lab inventory, request approvals, and track every booking from a single dashboard."
      />
      <div className="auth-form-side">
        <div className="auth-card">
          <Link to="/" className="auth-back">← Back to home</Link>

          <h1 className="auth-title">Sign in</h1>
          <p className="auth-sub">Access your Smart Lab account in one click.</p>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={submit}>
            <div className="field">
              <label>Email</label>
              <input type="email" value={email} required
                     onChange={(e) => setEmail(e.target.value)}
                     placeholder="you@example.com" />
            </div>
            <div className="field">
              <label>Password</label>
              <PasswordInput value={password} required
                     onChange={(e) => setPassword(e.target.value)}
                     placeholder="••••••••" />
            </div>
            <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg btn-pill-block" disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in →'}
            </button>
          </form>

          <div className="auth-divider"><span>or continue with</span></div>

          <button
            type="button"
            className="btn-social"
            onClick={() => alert('Google sign-in is coming soon. Please use your email and password for now.')}
          >
            <svg className="g-icon" viewBox="0 0 48 48" aria-hidden="true">
              <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3c-1.6 4.7-6 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34 6.5 29.3 4.5 24 4.5 13.2 4.5 4.5 13.2 4.5 24S13.2 43.5 24 43.5 43.5 34.8 43.5 24c0-1.2-.1-2.4-.4-3.5z"/>
              <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34 6.5 29.3 4.5 24 4.5 16.4 4.5 9.8 8.6 6.3 14.7z"/>
              <path fill="#4CAF50" d="M24 43.5c5.2 0 9.9-2 13.4-5.2l-6.2-5.1c-2 1.5-4.5 2.3-7.2 2.3-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.6 39.4 16.3 43.5 24 43.5z"/>
              <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.1 5.7l6.2 5.1c-.4.4 6.6-4.8 6.6-14.8 0-1.2-.1-2.4-.4-3.5z"/>
            </svg>
            Continue with Google
          </button>

          <p className="auth-switch">
            New here? <Link to="/register">Create an account</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
