import { useState } from 'react';
import { itemApi, labApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import { byId } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

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
  labId: '',
  model: '',
  name: '',
  category: '',
  serialNumber: '',
  status: 'AVAILABLE',
  description: '',
};

export default function AddEquipment() {
  const me = getCurrentUser();
  const [labs, setLabs] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [recent, setRecent] = useState([]);

  const loadRecent = async (isCancelled) => {
    try {
      const { data } = await itemApi.list();
      if (isCancelled?.()) return;
      const mine = data.filter((i) => labs.some((l) => l.id === i.labId));
      const sorted = [...mine].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 5);
      setRecent(sorted);
    } catch {
      if (!isCancelled?.()) setRecent([]);
    }
  };

  useAsyncEffect(async (isCancelled) => {
    try {
      const { data } = await labApi.list({ instructorUserId: me?.id });
      if (isCancelled()) return;
      setLabs(data);
      if (data.length === 1) setForm((f) => ({ ...f, labId: data[0].id }));
    } catch {
      if (!isCancelled()) setLabs([]);
    }
  }, []);

  useAsyncEffect(loadRecent, [labs]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setBusy(true);
    try {
      const payload = { ...form, labId: Number(form.labId) };
      const { data } = await itemApi.create(payload);
      setSuccess(`"${data.name}" added. Students can now book it.`);
      setForm({ ...EMPTY, labId: form.labId }); // keep lab selection
      loadRecent();
      setTimeout(() => setSuccess(''), 5000);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  const labLookup = byId(labs);

  return (
    <div className="container add-eq-wrap">
      <div className="add-eq-head">
        <div>
          <h1 className="page-title">Add Lab Item</h1>
          <p className="page-sub">
            Register a new item in one of your labs. Students can book it once its status
            is <em>Available</em>.
          </p>
        </div>
        <div className="add-eq-badge">
          <span className="add-eq-badge-dot" />
          Instructor · Catalogue
        </div>
      </div>

      {labs.length === 0 && (
        <div className="alert alert-error">
          You don't have any labs assigned yet. Ask your department admin to assign you to a lab before adding items.
        </div>
      )}

      <div className="add-eq-grid">
        <form className="add-eq-form" onSubmit={submit}>
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">01</span>
              <div>
                <h2>Lab</h2>
                <p>Which lab does this item live in?</p>
              </div>
            </div>
            <div className="field">
              <label>Lab <span className="req">*</span></label>
              <select value={form.labId} required onChange={set('labId')} disabled={labs.length === 0}>
                <option value="">— Select a lab —</option>
                {labs.map((l) => (
                  <option key={l.id} value={l.id}>{l.name}{l.location ? ` — ${l.location}` : ''}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">02</span>
              <div>
                <h2>Identity</h2>
                <p>What is this physical unit?</p>
              </div>
            </div>
            <div className="field-row">
              <div className="field">
                <label>Model <span className="req">*</span></label>
                <input value={form.model} required onChange={set('model')}
                       placeholder="Tektronix TBS1052B" />
                <span className="field-hint">Make + model — students search by this.</span>
              </div>
              <div className="field">
                <label>Display name <span className="req">*</span></label>
                <input value={form.name} required onChange={set('name')}
                       placeholder="Oscilloscope #1" />
              </div>
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
                <label>Serial number</label>
                <input value={form.serialNumber} onChange={set('serialNumber')}
                       placeholder="C012345" />
                <span className="field-hint">Optional — useful for tracking the physical unit.</span>
              </div>
            </div>
          </div>

          <div className="add-eq-section">
            <div className="add-eq-section-head">
              <span className="add-eq-section-num">03</span>
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
              <span className="add-eq-section-num">04</span>
              <div>
                <h2>Description</h2>
                <p>Specs, accessories, or safety notes (optional).</p>
              </div>
            </div>
            <div className="field">
              <label>Details</label>
              <textarea rows="4" value={form.description} onChange={set('description')}
                        placeholder="50 MHz, 2-channel. Comes with BNC probes." />
            </div>
          </div>

          <div className="add-eq-actions">
            <button type="button" className="btn-pill btn-pill-ghost"
                    onClick={() => { setForm({ ...EMPTY, labId: form.labId }); setError(''); setSuccess(''); }}>
              Reset form
            </button>
            <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg"
                    disabled={busy || !form.labId}>
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
                  const lab = labLookup[e.labId];
                  return (
                    <li key={e.id} className="add-eq-recent-row">
                      <div>
                        <div className="add-eq-recent-name">{e.name}</div>
                        <div className="add-eq-recent-meta">
                          {e.model} · {e.category}{lab ? ` · ${lab.name}` : ''}
                        </div>
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
        </aside>
      </div>
    </div>
  );
}
