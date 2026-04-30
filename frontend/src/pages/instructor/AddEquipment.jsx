import { useEffect, useState } from 'react';
import { equipmentApi, labApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';

const CATEGORIES = [
  'Electronics',
  'Mechanical',
  'Civil',
  'Electrical',
  'Manufacturing',
  'Computing',
  'General',
];

const EMPTY = {
  name: '',
  category: '',
  location: '',
  status: 'AVAILABLE',
  description: '',
  labId: '',
};

export default function AddEquipment() {
  const me = getCurrentUser();
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [recent, setRecent] = useState([]);
  const [myLabs, setMyLabs] = useState([]);

  const loadRecent = async () => {
    try {
      const { data } = await equipmentApi.list();
      const sorted = [...data].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 5);
      setRecent(sorted);
    } catch {
      setRecent([]);
    }
  };

  const loadMyLabs = async () => {
    if (!me?.id) return;
    try {
      const { data } = await labApi.byInstructor(me.id);
      setMyLabs(data || []);
    } catch {
      setMyLabs([]);
    }
  };

  useEffect(() => { loadRecent(); loadMyLabs(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setBusy(true);
    try {
      if (!form.labId) {
        setError('Please select a lab. You can only add equipment to labs you are assigned to.');
        setBusy(false);
        return;
      }
      const payload = {
        ...form,
        labId: Number(form.labId),
        instructorId: me.id,
      };
      const { data } = await equipmentApi.create(payload);
      setSuccess(`"${data.name}" added to ${data.labName || 'the catalogue'}. Students can now book it.`);
      setForm(EMPTY);
      loadRecent();
      setTimeout(() => setSuccess(''), 5000);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container add-eq-wrap">
      <div className="add-eq-head">
        <div>
          <h1 className="page-title">Add Lab Equipment</h1>
          <p className="page-sub">
            Register a new instrument in the SmartLab catalogue. Students can book it once its status
            is <em>Available</em>.
          </p>
        </div>
        <div className="add-eq-badge">
          <span className="add-eq-badge-dot" />
          Instructor · Catalogue
        </div>
      </div>

      <div className="add-eq-grid">
        <form className="add-eq-form" onSubmit={submit}>
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">01</span>
              <div>
                <h2>Identity</h2>
                <p>What is this piece of equipment?</p>
              </div>
            </div>
            <div className="field">
              <label>Equipment Name <span className="req">*</span></label>
              <input value={form.name} required onChange={set('name')}
                     placeholder="e.g., Tektronix TBS1052B Oscilloscope" />
              <span className="field-hint">Include make and model so students can identify it.</span>
            </div>
            <div className="field-row">
              <div className="field">
                <label>Category <span className="req">*</span></label>
                <select value={form.category} required onChange={set('category')}>
                  <option value="">— Select a category —</option>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Lab <span className="req">*</span></label>
                <select value={form.labId} required onChange={set('labId')}>
                  <option value="">— Select one of your labs —</option>
                  {myLabs.map((l) => (
                    <option key={l.id} value={l.id}>{l.name}</option>
                  ))}
                </select>
                <span className="field-hint">
                  {myLabs.length === 0
                    ? 'You are not assigned to any labs yet. Ask the admin.'
                    : 'Only labs you are assigned to are listed.'}
                </span>
              </div>
            </div>
            <div className="field">
              <label>Location <span className="req">*</span></label>
              <input value={form.location} required onChange={set('location')}
                     placeholder="Lab A-101" />
              <span className="field-hint">Building + room number (free text — different from the Lab record above).</span>
            </div>
          </div>

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">02</span>
              <div>
                <h2>Availability</h2>
                <p>Is this ready for students to book right now?</p>
              </div>
            </div>
            <div className="status-choice">
              {[
                { v: 'AVAILABLE', t: 'Available', d: 'Ready for booking', cls: 'ok' },
                { v: 'IN_USE', t: 'In use', d: 'Currently occupied', cls: 'busy' },
                { v: 'MAINTENANCE', t: 'Maintenance', d: 'Out of service', cls: 'warn' },
              ].map((opt) => (
                <label key={opt.v} className={`status-card ${form.status === opt.v ? 'status-card-on' : ''}`}>
                  <input type="radio" name="status" value={opt.v}
                         checked={form.status === opt.v}
                         onChange={set('status')} />
                  <span className={`status-dot status-dot-${opt.cls}`} />
                  <span className="status-title">{opt.t}</span>
                  <span className="status-desc">{opt.d}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">03</span>
              <div>
                <h2>Description</h2>
                <p>Specs, accessories, or safety notes (optional).</p>
              </div>
            </div>
            <div className="field">
              <label>Details</label>
              <textarea rows="4" value={form.description} onChange={set('description')}
                        placeholder="e.g., 50 MHz, 2-channel. Comes with BNC probes. Handle with care — fragile CRT replacement." />
              <span className="field-hint">Helps students understand what they are reserving.</span>
            </div>
          </div>

          <div className="add-eq-actions">
            <button type="button" className="btn-pill btn-pill-ghost"
                    onClick={() => { setForm(EMPTY); setError(''); setSuccess(''); }}>
              Reset form
            </button>
            <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg" disabled={busy}>
              {busy ? 'Saving…' : 'Add to Catalogue →'}
            </button>
          </div>
        </form>

        <aside className="add-eq-aside">
          <div className="add-eq-card">
            <div className="add-eq-card-head">
              <span className="add-eq-card-eyebrow">Recently added</span>
              <span className="add-eq-card-count">{recent.length}</span>
            </div>
            {recent.length === 0 ? (
              <div className="add-eq-empty">
                Nothing here yet. Your next entry will appear at the top.
              </div>
            ) : (
              <ul className="add-eq-recent">
                {recent.map((e) => {
                  const st = (e.status || 'AVAILABLE').toUpperCase();
                  const cls = st === 'AVAILABLE' ? 'ok' : st === 'IN_USE' ? 'busy' : 'warn';
                  return (
                    <li key={e.id} className="add-eq-recent-row">
                      <div>
                        <div className="add-eq-recent-name">{e.name}</div>
                        <div className="add-eq-recent-meta">{e.category} · {e.location}</div>
                      </div>
                      <span className={`eq-status eq-status-${cls}`}>
                        {st === 'IN_USE' ? 'In use' : st === 'MAINTENANCE' ? 'Maint.' : 'Available'}
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          <div className="add-eq-tips">
            <div className="add-eq-tips-title">Tips for a clean catalogue</div>
            <ul>
              <li>Use the make + model in the <strong>name</strong> — "Tektronix TBS1052B", not just "Oscilloscope".</li>
              <li>Keep <strong>location</strong> specific: "Lab A-101" beats "Electronics lab".</li>
              <li>Set status to <strong>Maintenance</strong> if it's broken — students won't be able to book it.</li>
              <li>Description is public — no private notes or passwords.</li>
            </ul>
          </div>
        </aside>
      </div>
    </div>
  );
}
