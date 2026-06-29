import { useState } from 'react';
import { userApi, errMsg } from '../../api';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const roleLabel = (r) => ({ INSTRUCTOR: 'Instructor', LECTURER: 'Lecturer', HOD: 'HOD' }[r] || r);

export default function PendingInstructors() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const { data } = await userApi.getPendingInstructors();
      if (isCancelled?.()) return;
      setItems(data);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const approve = async (id, name) => {
    try {
      await userApi.approveInstructor(id);
      setMsg(`Approved ${name}. They have been notified.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const reject = async (id, name) => {
    if (!confirm(`Reject and delete ${name}'s application?`)) return;
    try {
      await userApi.rejectInstructor(id);
      setMsg(`Rejected ${name}.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Pending Staff</h1>
      <p className="page-sub">Review and approve staff account requests (Instructor / Lecturer / HOD).</p>

      {error && <div className="alert alert-error">{error}</div>}
      {msg && <div className="alert alert-success">{msg}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>No pending staff requests.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Name</th><th>Role</th><th>Email</th><th>Department</th><th>Phone</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.fullName}</td>
                    <td>{roleLabel(u.role)}</td>
                    <td>{u.email}</td>
                    <td>{u.department || '—'}</td>
                    <td>{u.phoneNumber || '—'}</td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-success btn-sm" onClick={() => approve(u.id, u.fullName)}>Approve</button>
                      <button className="btn btn-danger btn-sm" onClick={() => reject(u.id, u.fullName)}>Reject</button>
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
