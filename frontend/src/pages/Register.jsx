import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi, errMsg } from '../api';
import AuthBrand from '../components/AuthBrand';

export default function Register() {
  const nav = useNavigate();
  const [role, setRole] = useState('STUDENT');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Common
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [department, setDepartment] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');

  // Student-only
  const [enNumber, setEnNumber] = useState('');
  const [indexNumber, setIndexNumber] = useState('');
  const [nameWithInitial, setNameWithInitial] = useState('');
  const [uniEmail, setUniEmail] = useState('');

  const [agreed, setAgreed] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setBusy(true);
    try {
      const payload = {
        email, password, fullName, role, department, phoneNumber,
        ...(role === 'STUDENT' && { enNumber, indexNumber, nameWithInitial, uniEmail }),
      };
      await userApi.register(payload);
      if (role === 'INSTRUCTOR') {
        setSuccess('Account created. Awaiting admin approval — you will be notified.');
      } else {
        setSuccess('Account created. Redirecting to sign in…');
        setTimeout(() => nav('/login'), 1500);
      }
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <AuthBrand
        eyebrow="Join the platform"
        headline={<>Smart <em>Lab</em> · Equipment Booking</>}
        lede="Register with your university credentials to reserve lab equipment, manage approvals, and stay in sync with your faculty. Instructor accounts go live after administrator review."
      />
      <div className="auth-form-side">
      <div className="auth-card wide">
        <Link to="/" className="auth-back">← Back to home</Link>

        <h1 className="auth-title">Create account</h1>
        <p className="auth-sub">Fill the form below to get started — it takes under a minute.</p>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div className="field">
          <label>I am registering as</label>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="STUDENT">Student</option>
            <option value="INSTRUCTOR">Instructor (requires admin approval)</option>
          </select>
        </div>

        <form onSubmit={submit}>
          <div className="field-row">
            <div className="field">
              <label>Full Name</label>
              <input value={fullName} required
                     onChange={(e) => setFullName(e.target.value)}
                     placeholder="e.g., A. Harishan" />
            </div>
            <div className="field">
              <label>Department</label>
              <select value={department} required
                      onChange={(e) => setDepartment(e.target.value)}>
                <option value="">-- Select department --</option>
                <option value="Computer Engineering">Computer Engineering</option>
                <option value="Mechanical Engineering">Mechanical Engineering</option>
                <option value="Civil Engineering">Civil Engineering</option>
                <option value="Electrical and Electronic Engineering">Electrical and Electronic Engineering</option>
                <option value="Manufacturing and Industrial Engineering">Manufacturing and Industrial Engineering</option>
              </select>
            </div>
          </div>

          <div className="field-row">
            <div className="field">
              <label>Email (used for login)</label>
              <input type="email" value={email} required
                     onChange={(e) => setEmail(e.target.value)}
                     placeholder="you@gmail.com" />
            </div>
            <div className="field">
              <label>Phone Number</label>
              <input value={phoneNumber}
                     onChange={(e) => setPhoneNumber(e.target.value)}
                     placeholder="07XXXXXXXX" />
            </div>
          </div>

          {role === 'STUDENT' && (
            <>
              <div className="field-row">
                <div className="field">
                  <label>EN Number</label>
                  <input value={enNumber} required
                         onChange={(e) => setEnNumber(e.target.value)}
                         placeholder="EN102752" />
                </div>
                <div className="field">
                  <label>Index Number</label>
                  <input value={indexNumber} required
                         onChange={(e) => setIndexNumber(e.target.value)}
                         placeholder="21ENG009" />
                </div>
              </div>
              <div className="field-row">
                <div className="field">
                  <label>Name with Initial</label>
                  <input value={nameWithInitial} required
                         onChange={(e) => setNameWithInitial(e.target.value)}
                         placeholder="A. Harishan" />
                </div>
                <div className="field">
                  <label>University Email</label>
                  <input type="email" value={uniEmail} required
                         onChange={(e) => setUniEmail(e.target.value)}
                         placeholder="en102752@feo.sjp.ac.lk" />
                </div>
              </div>
            </>
          )}

          <div className="field">
            <label>Password</label>
            <input type="password" value={password} required minLength={6}
                   onChange={(e) => setPassword(e.target.value)}
                   placeholder="At least 6 characters" />
          </div>

          <label className="auth-check">
            <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
            <span className="auth-check-box" aria-hidden="true" />
            <span>
              I agree to the <Link to="/terms" target="_blank" rel="noopener noreferrer">Terms &amp; Conditions</Link>
            </span>
          </label>

          <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg btn-pill-block" disabled={busy || !agreed}>
            {busy ? 'Creating…' : 'Create account →'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
      </div>
    </div>
  );
}
