import { useEffect, useMemo, useState } from 'react';
import { departmentApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';

export default function AdminDepartments() {
  const me = getCurrentUser();
  const isMainAdmin = me?.role === 'MAIN_ADMIN';

  const [departments, setDepartments] = useState([]);
  const [hodsByDept, setHodsByDept] = useState({}); // deptId -> active HoDs in that dept
  const [hodNameById, setHodNameById] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // department row currently being edited

  const load = async () => {
    setLoading(true);
    try {
      const { data: depts } = await departmentApi.list();
      const visible = isMainAdmin ? depts : depts.filter((d) => d.id === me.departmentId);
      setDepartments(visible);

      // All HoDs across the system, grouped by department.
      const { data: hods } = await userApi.getByRole('HOD');
      const grouped = {};
      const nameMap = {};
      hods.forEach((h) => {
        nameMap[h.id] = h;
        if (h.status !== 'ACTIVE') return;
        if (!grouped[h.departmentId]) grouped[h.departmentId] = [];
        grouped[h.departmentId].push(h);
      });
      setHodsByDept(grouped);
      setHodNameById(nameMap);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, []);

  const setHod = async (deptId, hodUserId) => {
    try {
      await departmentApi.setHod(deptId, hodUserId || null);
      setEditing(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Departments</h1>
      <p className="page-sub">
        Pin a Head of Department for each department. Instructors see this person as the
        default supervisor when delegating a booking.
      </p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        departments.length === 0 ? (
          <div className="empty"><div className="empty-icon">🏛️</div>No departments visible.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th><th>Department</th><th>Code</th>
                  <th>Current HoD</th><th>Eligible HoDs</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {departments.map((d) => {
                  const eligible = hodsByDept[d.id] || [];
                  const currentHod = d.hodUserId ? hodNameById[d.hodUserId] : null;
                  return (
                    <tr key={d.id}>
                      <td>{d.id}</td>
                      <td>{d.name}</td>
                      <td>{d.code}</td>
                      <td>
                        {currentHod
                          ? <>{currentHod.fullName}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{currentHod.email}</div></>
                          : <span style={{ color: 'var(--muted)' }}>— unassigned —</span>}
                      </td>
                      <td>{eligible.length}</td>
                      <td>
                        <button className="btn btn-secondary btn-sm" onClick={() => setEditing(d)}>
                          {currentHod ? 'Change' : 'Assign'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )
      )}

      {editing && (
        <HodModal department={editing}
                  eligible={hodsByDept[editing.id] || []}
                  currentHodId={editing.hodUserId}
                  onClose={() => setEditing(null)}
                  onSubmit={setHod} />
      )}
    </div>
  );
}

function HodModal({ department, eligible, currentHodId, onClose, onSubmit }) {
  const [pickedId, setPickedId] = useState(currentHodId || '');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try { await onSubmit(department.id, pickedId ? Number(pickedId) : null); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>HoD for {department.name}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Head of Department</label>
            <select value={pickedId} onChange={(e) => setPickedId(e.target.value)}>
              <option value="">— Unassigned —</option>
              {eligible.map((u) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>
              ))}
            </select>
            {eligible.length === 0 && (
              <span className="field-hint">
                No active HoD users in this department yet — create one first via the Users page.
              </span>
            )}
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
