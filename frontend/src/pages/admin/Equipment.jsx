import { useEffect, useState } from 'react';
import { equipmentApi, errMsg } from '../../api';
import Badge from '../../components/Badge';

const EMPTY = { name: '', category: '', location: '', description: '', status: 'AVAILABLE' };

export default function AdminEquipment() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // null | 'new' | equipment object

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await equipmentApi.list();
      setItems(data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const save = async (form) => {
    try {
      if (editing === 'new') {
        await equipmentApi.create(form);
      } else {
        await equipmentApi.update(editing.id, form);
      }
      setEditing(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const remove = async (id, name) => {
    if (!confirm(`Delete ${name}?`)) return;
    try {
      await equipmentApi.remove(id);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">Manage Equipment</h1>
          <p className="page-sub">Add, edit or remove lab equipment.</p>
        </div>
        <button className="btn" onClick={() => setEditing('new')}>+ Add Equipment</button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">🧪</div>No equipment yet. Click "Add Equipment".</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Name</th><th>Category</th><th>Lab</th><th>Location</th><th>Instructors</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {items.map((e) => (
                  <tr key={e.id}>
                    <td>{e.id}</td>
                    <td>{e.name}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{e.description}</div></td>
                    <td>{e.category}</td>
                    <td>{e.labName || '—'}</td>
                    <td>{e.location}</td>
                    <td style={{ fontSize: 12 }}>
                      {e.instructorNames && e.instructorNames.length > 0
                        ? e.instructorNames.join(', ')
                        : '—'}
                    </td>
                    <td><Badge value={e.status} /></td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => setEditing(e)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => remove(e.id, e.name)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {editing && (
        <EquipmentModal
          initial={editing === 'new' ? EMPTY : editing}
          isNew={editing === 'new'}
          onClose={() => setEditing(null)}
          onSave={save}
        />
      )}
    </div>
  );
}

function EquipmentModal({ initial, isNew, onClose, onSave }) {
  const [form, setForm] = useState(initial);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    await onSave(form);
    setBusy(false);
  };
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Add Equipment' : 'Edit Equipment'}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Name</label>
            <input value={form.name} required onChange={set('name')} />
          </div>
          <div className="field-row">
            <div className="field">
              <label>Category</label>
              <input value={form.category} onChange={set('category')} placeholder="Electronics" />
            </div>
            <div className="field">
              <label>Location</label>
              <input value={form.location} onChange={set('location')} placeholder="Lab A-101" />
            </div>
          </div>
          <div className="field">
            <label>Status</label>
            <select value={form.status} onChange={set('status')}>
              <option value="AVAILABLE">Available</option>
              <option value="IN_USE">In use</option>
              <option value="MAINTENANCE">Maintenance</option>
            </select>
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
