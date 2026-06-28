import { useState } from 'react';
import { labApi, departmentApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const EMPTY = {
  departmentId: '',
  name: '',
  location: '',
  description: '',
  instructorUserId: '',
};

export default function AdminLabs() {
  const me = getCurrentUser();
  const isMainAdmin = me?.role === 'MAIN_ADMIN';

  const [labs, setLabs] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [instructors, setInstructors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // null | 'new' | lab object

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const params = isMainAdmin ? {} : { departmentId: me.departmentId };
      const [labsRes, deptsRes, instructorsRes] = await Promise.all([
        labApi.list(params),
        departmentApi.list().catch(() => ({ data: [] })),
        userApi.getByRole('INSTRUCTOR').catch(() => ({ data: [] })),
      ]);
      if (isCancelled?.()) return;
      setLabs(labsRes.data);
      setDepartments(deptsRes.data);
      setInstructors(instructorsRes.data || []);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const save = async (form) => {
    try {
      const payload = {
        departmentId: Number(form.departmentId),
        name: form.name,
        location: form.location || null,
        description: form.description || null,
        instructorUserId: form.instructorUserId ? Number(form.instructorUserId) : null,
      };
      if (editing === 'new') {
        await labApi.create(payload);
      } else {
        await labApi.update(editing.id, payload);
      }
      setEditing(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const remove = async (lab) => {
    if (!confirm(`Delete lab "${lab.name}"? Items in this lab must be moved or deleted first.`)) return;
    try {
      await labApi.remove(lab.id);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const deptName = (id) => departments.find((d) => d.id === id)?.name || `Dept #${id}`;
  const instructorName = (id) => {
    if (!id) return '— unassigned —';
    const u = instructors.find((u) => u.id === id);
    return u ? u.fullName : `User #${id}`;
  };

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">Manage Labs</h1>
          <p className="page-sub">
            {isMainAdmin
              ? 'Labs across all departments.'
              : 'Labs in your department. Create labs and assign an instructor to each.'}
          </p>
        </div>
        <button className="btn" onClick={() => setEditing('new')}>+ Add Lab</button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        labs.length === 0 ? (
          <div className="empty"><div className="empty-icon">🏛️</div>No labs yet. Click "Add Lab".</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Name</th><th>Department</th><th>Location</th><th>Instructor</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {labs.map((l) => (
                  <tr key={l.id}>
                    <td>{l.id}</td>
                    <td>{l.name}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{l.description}</div></td>
                    <td>{deptName(l.departmentId)}</td>
                    <td>{l.location || '—'}</td>
                    <td>{instructorName(l.instructorUserId)}</td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => setEditing(l)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => remove(l)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {editing && (
        <LabModal
          initial={editing === 'new'
            ? { ...EMPTY, departmentId: isMainAdmin ? '' : me.departmentId || '' }
            : {
                departmentId: editing.departmentId,
                name: editing.name,
                location: editing.location || '',
                description: editing.description || '',
                instructorUserId: editing.instructorUserId || '',
              }}
          isNew={editing === 'new'}
          departments={isMainAdmin ? departments : departments.filter((d) => d.id === me.departmentId)}
          instructors={instructors}
          onClose={() => setEditing(null)}
          onSave={save}
        />
      )}
    </div>
  );
}

function LabModal({ initial, isNew, departments, instructors, onClose, onSave }) {
  const [form, setForm] = useState(initial);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  // Filter instructors to those in the chosen department (when known).
  const eligibleInstructors = form.departmentId
    ? instructors.filter((u) => u.departmentId == form.departmentId && u.status === 'ACTIVE')
    : instructors.filter((u) => u.status === 'ACTIVE');

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    await onSave(form);
    setBusy(false);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Add Lab' : 'Edit Lab'}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Department</label>
            <select value={form.departmentId} required onChange={set('departmentId')}>
              <option value="">— Select department —</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Lab name</label>
            <input value={form.name} required onChange={set('name')} placeholder="Embedded Systems Lab" />
          </div>
          <div className="field-row">
            <div className="field">
              <label>Location</label>
              <input value={form.location} onChange={set('location')} placeholder="Building A, Room 101" />
            </div>
            <div className="field">
              <label>Instructor</label>
              <select value={form.instructorUserId} onChange={set('instructorUserId')}>
                <option value="">— Unassigned —</option>
                {eligibleInstructors.map((u) => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>
                ))}
              </select>
            </div>
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows="3" value={form.description} onChange={set('description')} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
