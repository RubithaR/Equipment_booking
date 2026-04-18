import { useEffect, useState } from 'react';
import { userApi, errMsg } from '../../api';
import Badge from '../../components/Badge';

export default function Users() {
  const [items, setItems] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const { data } = await userApi.getAll();
        setItems(data);
      } catch (err) {
        setError(errMsg(err));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const visible = items
    .filter((u) => filter === 'ALL' || u.role === filter)
    .filter((u) => !search ||
      `${u.fullName} ${u.email} ${u.indexNumber || ''} ${u.enNumber || ''}`
        .toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => a.id - b.id);

  return (
    <div className="container">
      <h1 className="page-title">All Users</h1>
      <p className="page-sub">Students, instructors and admin in one place.</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="filter-bar">
        <input placeholder="Search name, email or EN number…" value={search}
               onChange={(e) => setSearch(e.target.value)} style={{ flex: 1, minWidth: 200 }} />
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All roles</option>
          <option value="STUDENT">Students</option>
          <option value="INSTRUCTOR">Instructors</option>
          <option value="ADMIN">Admin</option>
        </select>
      </div>

      {loading ? <div className="loading">Loading…</div> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th>
                <th>Department</th><th>Phone</th><th>EN / Index</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.fullName}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{u.nameWithInitial}</div></td>
                  <td>{u.email}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{u.uniEmail}</div></td>
                  <td><Badge value={u.role} /></td>
                  <td><Badge value={u.status} /></td>
                  <td>{u.department || '—'}</td>
                  <td>{u.phoneNumber || '—'}</td>
                  <td>{u.enNumber || '—'}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{u.indexNumber}</div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
