import { useEffect, useState } from 'react';
import { userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';

export default function PendingStudents() {
  const me = getCurrentUser();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const load = async () => {
    if (!me?.id) {
      setError('Could not read your instructor id from session.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const { data } = await userApi.getPendingStudents(me.id);
      setItems(data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const approve = async (id, name) => {
    try {
      await userApi.approveStudent(id, me.id);
      setMsg(`Approved ${name}. They have been notified and can now log in.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const reject = async (id, name) => {
    if (!confirm(`Reject and delete ${name}'s application?`)) return;
    try {
      await userApi.rejectStudent(id, me.id);
      setMsg(`Rejected ${name}.`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Pending Students</h1>
      <p className="page-sub">Students from your department waiting for approval.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {msg && <div className="alert alert-success">{msg}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>No pending student requests in your department.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Full Name</th>
                  <th>EN Number</th>
                  <th>Index</th>
                  <th>University Email</th>
                  <th>Department</th>
                  <th>Phone</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.fullName}</td>
                    <td>{u.enNumber || '—'}</td>
                    <td>{u.indexNumber || '—'}</td>
                    <td>{u.uniEmail || '—'}</td>
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
