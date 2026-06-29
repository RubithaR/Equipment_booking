import { useState } from 'react';
import { userApi, errMsg } from '../../api';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const ROLE_OPTIONS = [
  { value: 'INSTRUCTOR', label: 'Instructor' },
  { value: 'LECTURER', label: 'Lecturer' },
  { value: 'HOD', label: 'Head of Department (HOD)' },
];

export default function PendingInstructors() {
  const [items, setItems] = useState([]);
  const [picked, setPicked] = useState({});   // userId -> chosen role
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const { data } = await userApi.getPendingInstructors();
      if (isCancelled?.()) return;
      setItems(data);
      // Default every row's picker to Instructor.
      setPicked(Object.fromEntries(data.map((u) => [u.id, 'INSTRUCTOR'])));
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const assign = async (id, name) => {
    const role = picked[id] || 'INSTRUCTOR';
    const label = ROLE_OPTIONS.find((r) => r.value === role)?.label || role;
    try {
      await userApi.assignRole(id, role);
      setMsg(`Assigned ${name} as ${label}. They can sign in to reach their dashboard.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const reject = async (id, name) => {
    if (!confirm(`Remove ${name}'s staff account? This deletes the account.`)) return;
    try {
      await userApi.rejectInstructor(id);
      setMsg(`Removed ${name}.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Pending Staff</h1>
      <p className="page-sub">
        Staff who signed up and are waiting for a role. Choose a role and click <strong>Assign</strong> —
        the account then becomes an Instructor, Lecturer or HOD and they can sign in to their dashboard.
      </p>

      {error && <div className="alert alert-error">{error}</div>}
      {msg && <div className="alert alert-success">{msg}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>No staff are waiting for a role.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Name</th><th>Email</th><th>Department</th><th>Phone</th><th>Assign role</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>{u.department || '—'}</td>
                    <td>{u.phoneNumber || '—'}</td>
                    <td>
                      <select
                        value={picked[u.id] || 'INSTRUCTOR'}
                        onChange={(e) => setPicked((p) => ({ ...p, [u.id]: e.target.value }))}>
                        {ROLE_OPTIONS.map((r) => (
                          <option key={r.value} value={r.value}>{r.label}</option>
                        ))}
                      </select>
                    </td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-success btn-sm" onClick={() => assign(u.id, u.fullName)}>Assign</button>
                      <button className="btn btn-danger btn-sm" onClick={() => reject(u.id, u.fullName)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  );
}
