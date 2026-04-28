import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi, errMsg } from '../api';
import AuthBrand from '../components/AuthBrand';

const UNI_DOMAIN = '@foe.sjp.ac.lk';
const ALLOWED_LOGIN_DOMAINS = ['@gmail.com', UNI_DOMAIN];

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
  const [uniEmailPrefix, setUniEmailPrefix] = useState('');

  const [agreed, setAgreed] = useState(false);

  // Inline duplicate-check errors
  const [emailErr, setEmailErr] = useState('');
  const [enErr, setEnErr] = useState('');
  const [indexErr, setIndexErr] = useState('');

  // Inline format errors
  const [emailFmtErr, setEmailFmtErr] = useState('');
  const [phoneErr, setPhoneErr] = useState('');
  const [enFmtErr, setEnFmtErr] = useState('');
  const [uniEmailFmtErr, setUniEmailFmtErr] = useState('');

  const validateEmailFormat = () => {
    if (!email) { setEmailFmtErr(''); return; }
    const ok = ALLOWED_LOGIN_DOMAINS.some((d) => email.toLowerCase().endsWith(d));
    setEmailFmtErr(ok ? '' : 'Email must end with @gmail.com or @foe.sjp.ac.lk');
  };

  const validatePhone = () => {
    if (!phoneNumber) { setPhoneErr(''); return; }
    const ok = phoneNumber.length === 9
      || (phoneNumber.length === 10 && phoneNumber.startsWith('0'));
    setPhoneErr(ok ? '' : 'Phone must be 9 digits, or 10 digits starting with 0');
  };

  const validateEnFormat = () => {
    if (!enNumber) { setEnFmtErr(''); return; }
    setEnFmtErr(/^EN\d{6}$/.test(enNumber) ? '' : 'Format: EN followed by 6 digits (e.g., EN102752)');
  };

  const validateUniEmailFormat = () => {
    if (!uniEmailPrefix) { setUniEmailFmtErr(''); return; }
    setUniEmailFmtErr(/^en\d{6}$/.test(uniEmailPrefix) ? '' : 'Format: en + 6 digits (e.g., en102752)');
  };

  const checkEmail = async () => {
    validateEmailFormat();
    if (!email) { setEmailErr(''); return; }
    try {
      const { data } = await userApi.checkAvailability({ email });
      setEmailErr(data.emailTaken ? 'Email already registered' : '');
    } catch { /* ignore network blips while typing */ }
  };

  const checkEn = async () => {
    validateEnFormat();
    if (!enNumber) { setEnErr(''); return; }
    try {
      const { data } = await userApi.checkAvailability({ enNumber });
      setEnErr(data.enTaken ? 'EN number already registered' : '');
    } catch { /* ignore */ }
  };

  const checkIndex = async () => {
    if (!indexNumber) { setIndexErr(''); return; }
    try {
      const { data } = await userApi.checkAvailability({ indexNumber });
      setIndexErr(data.indexTaken ? 'Index number already registered' : '');
    } catch { /* ignore */ }
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setBusy(true);
    try {
      const uniEmail = uniEmailPrefix ? uniEmailPrefix + UNI_DOMAIN : '';
      const payload = {
        email, password, fullName, role, department, phoneNumber,
        ...(role === 'STUDENT' && { enNumber, indexNumber, nameWithInitial, uniEmail }),
      };
      await userApi.register(payload);
      if (role === 'INSTRUCTOR') {
        setSuccess('Account created. Awaiting admin approval — you will be notified. Redirecting to sign in…');
        setTimeout(() => nav('/login'), 3000);
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

  const submitDisabled = busy || !agreed
    || !!emailErr || !!enErr || !!indexErr
    || !!emailFmtErr || !!phoneErr || !!enFmtErr || !!uniEmailFmtErr;

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
                     onChange={(e) => {
                       setEmail(e.target.value);
                       setEmailErr(''); setEmailFmtErr('');
                     }}
                     onBlur={checkEmail}
                     placeholder="you@gmail.com" />
              {emailFmtErr && <span className="field-error">{emailFmtErr}</span>}
              {!emailFmtErr && emailErr && <span className="field-error">{emailErr}</span>}
            </div>
            <div className="field">
              <label>Phone Number</label>
              <input value={phoneNumber} required inputMode="numeric"
                     onChange={(e) => {
                       const v = e.target.value.replace(/\D/g, '').slice(0, 10);
                       setPhoneNumber(v);
                       setPhoneErr('');
                     }}
                     onBlur={validatePhone}
                     placeholder="0763341476 or 763341476" />
              {phoneErr && <span className="field-error">{phoneErr}</span>}
            </div>
          </div>

          {role === 'STUDENT' && (
            <>
              <div className="field-row">
                <div className="field">
                  <label>EN Number</label>
                  <input value={enNumber} required
                         onChange={(e) => {
                           const v = e.target.value.toUpperCase().slice(0, 8);
                           setEnNumber(v);
                           setEnErr(''); setEnFmtErr('');
                         }}
                         onBlur={checkEn}
                         placeholder="EN102752" />
                  {enFmtErr && <span className="field-error">{enFmtErr}</span>}
                  {!enFmtErr && enErr && <span className="field-error">{enErr}</span>}
                </div>
                <div className="field">
                  <label>Index Number</label>
                  <input value={indexNumber} required
                         onChange={(e) => {
                           setIndexNumber(e.target.value.toUpperCase());
                           setIndexErr('');
                         }}
                         onBlur={checkIndex}
                         placeholder="21ENG009" />
                  {indexErr && <span className="field-error">{indexErr}</span>}
                </div>
              </div>
              <div className="field-row">
                <div className="field">
                  <label>Name with Initial</label>
                  <input value={nameWithInitial} required
                         onChange={(e) => setNameWithInitial(e.target.value.toUpperCase())}
                         placeholder="A. HARISHAN" />
                </div>
                <div className="field">
                  <label>University Email</label>
                  <div className="email-split">
                    <input value={uniEmailPrefix} required
                           onChange={(e) => {
                             const v = e.target.value.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 8);
                             setUniEmailPrefix(v);
                             setUniEmailFmtErr('');
                           }}
                           onBlur={validateUniEmailFormat}
                           placeholder="en102752" />
                    <span className="email-suffix">{UNI_DOMAIN}</span>
                  </div>
                  {uniEmailFmtErr && <span className="field-error">{uniEmailFmtErr}</span>}
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

          <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg btn-pill-block"
                  disabled={submitDisabled}>
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
