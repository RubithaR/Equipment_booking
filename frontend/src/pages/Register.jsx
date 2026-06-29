import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi, departmentApi, errMsg } from '../api';
import AuthBrand from '../components/AuthBrand';
import PasswordInput from '../components/PasswordInput';

const STUDENT_EMAIL_RE = /^en\d{6}@foe\.sjp\.ac\.lk$/;
const EN_NUMBER_RE = /^EN\d{6}$/;

export default function Register() {
  const nav = useNavigate();
  const [kind, setKind] = useState('STUDENT');            // STUDENT | STAFF
  // Staff sign up without a role; an admin assigns the real role (HOD/Lecturer/Instructor) later.
  const role = kind === 'STUDENT' ? 'STUDENT' : 'STAFF';  // actual role sent to the API
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Reference data
  const [departments, setDepartments] = useState([]);

  // Common
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');

  // Student-only
  const [enNumber, setEnNumber] = useState('');
  const [indexNumber, setIndexNumber] = useState('');
  const [nameWithInitial, setNameWithInitial] = useState('');

  const [agreed, setAgreed] = useState(false);

  // Inline duplicate-check errors
  const [emailErr, setEmailErr] = useState('');
  const [enErr, setEnErr] = useState('');
  const [indexErr, setIndexErr] = useState('');

  // Inline format errors
  const [emailFmtErr, setEmailFmtErr] = useState('');
  const [phoneErr, setPhoneErr] = useState('');
  const [enFmtErr, setEnFmtErr] = useState('');

  useEffect(() => {
    departmentApi.list()
      .then((res) => setDepartments(res.data))
      .catch(() => setDepartments([]));
  }, []);

  const validateEmailFormat = () => {
    if (!email) { setEmailFmtErr(''); return; }
    if (role === 'STUDENT') {
      const ok = STUDENT_EMAIL_RE.test(email.toLowerCase());
      setEmailFmtErr(ok ? '' : 'Student email must be en<6 digits>@foe.sjp.ac.lk (e.g., en102020@foe.sjp.ac.lk)');
    } else {
      // Instructor: any well-formed email is fine for now.
      const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
      setEmailFmtErr(ok ? '' : 'Enter a valid email address');
    }
  };

  const validatePhone = () => {
    if (!phoneNumber) { setPhoneErr(''); return; }
    const ok = phoneNumber.length === 9
      || (phoneNumber.length === 10 && phoneNumber.startsWith('0'));
    setPhoneErr(ok ? '' : 'Phone must be 9 digits, or 10 digits starting with 0');
  };

  const validateEnFormat = () => {
    if (!enNumber) { setEnFmtErr(''); return; }
    setEnFmtErr(EN_NUMBER_RE.test(enNumber) ? '' : 'Format: EN followed by 6 digits (e.g., EN102020)');
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
      const payload = {
        email: email.toLowerCase(),
        password,
        fullName,
        role,
        departmentId: Number(departmentId),
        phoneNumber,
        ...(role === 'STUDENT' && {
          enNumber,
          indexNumber,
          nameWithInitial,
          uniEmail: email.toLowerCase(),
        }),
      };
      await userApi.register(payload);
      if (kind === 'STAFF') {
        setSuccess('Account created. Sign in to continue — an administrator will assign your role. Redirecting to sign in…');
      } else {
        setSuccess('Account created. You can sign in now. Redirecting to sign in…');
      }
      setTimeout(() => nav('/login'), 3000);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  const submitDisabled = busy || !agreed || !departmentId
    || !!emailErr || !!enErr || !!indexErr
    || !!emailFmtErr || !!phoneErr || !!enFmtErr;

  return (
    <div className="auth-wrap">
      <AuthBrand
        eyebrow="Join the platform"
        headline={<>Smart <em>Lab</em> · Equipment Booking</>}
        lede="Register with your university credentials to reserve lab equipment, manage approvals, and stay in sync with your faculty. Instructor accounts go live after department admin review."
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
          <select value={kind} onChange={(e) => {
            setKind(e.target.value);
            setEmailFmtErr('');
          }}>
            <option value="STUDENT">Student</option>
            <option value="STAFF">Staff (role assigned by admin after sign-up)</option>
          </select>
        </div>

        {kind === 'STAFF' && (
          <p className="auth-sub" style={{ marginTop: -4 }}>
            Sign up now — an administrator will assign your role (Head of Department, Lecturer or
            Instructor) once your account is reviewed. You can sign in straight away to check your status.
          </p>
        )}

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
              <select value={departmentId} required
                      onChange={(e) => setDepartmentId(e.target.value)}>
                <option value="">-- Select department --</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="field-row">
            <div className="field">
              <label>{role === 'STUDENT' ? 'University Email (used for login)' : 'Email (used for login)'}</label>
              <input type="email" value={email} required
                     onChange={(e) => {
                       setEmail(e.target.value);
                       setEmailErr(''); setEmailFmtErr('');
                     }}
                     onBlur={checkEmail}
                     placeholder={role === 'STUDENT' ? 'en102020@foe.sjp.ac.lk' : 'you@foe.sjp.ac.lk'} />
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
                         placeholder="EN102020" />
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
              <div className="field">
                <label>Name with Initial</label>
                <input value={nameWithInitial} required
                       onChange={(e) => setNameWithInitial(e.target.value.toUpperCase())}
                       placeholder="A. HARISHAN" />
              </div>
            </>
          )}

          <div className="field">
            <label>Password</label>
            <PasswordInput value={password} required minLength={6}
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
