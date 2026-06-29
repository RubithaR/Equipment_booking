import { useState } from 'react';
import { departmentApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const roleLabel = (r) =>
  ({ INSTRUCTOR: 'Instructor', LECTURER: 'Lecturer', HOD: 'HOD', STAFF: 'awaiting role' }[r] || r);

export default function AdminDepartments() {
  const me = getCurrentUser();
  const isMainAdmin = me?.role === 'MAIN_ADMIN';

  const [departments, setDepartments] = useState([]);
  const [staffByDept, setStaffByDept] = useState({}); // deptId -> active staff in that dept
  const [allStaff, setAllStaff] = useState([]);       // every active staff member (any dept)
  const [staffById, setStaffById] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // department row currently being edited

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [{ data: depts }, { data: staff }] = await Promise.all([
        departmentApi.list(),
        // All active staff — any of them can be made the department's HoD.
        userApi.search({ q: '', roles: 'STAFF,INSTRUCTOR,LECTURER,HOD', limit: 100 }),
      ]);
      if (isCancelled?.()) return;

      const visible = isMainAdmin ? depts : depts.filter((d) => d.id === me.departmentId);
      const grouped = {};
      const nameMap = {};
      staff.forEach((h) => {
        nameMap[h.id] = h;
        if (h.status !== 'ACTIVE') return;
        if (!grouped[h.departmentId]) grouped[h.departmentId] = [];
        grouped[h.departmentId].push(h);
      });
      setDepartments(visible);
      setStaffByDept(grouped);
      setAllStaff(staff.filter((h) => h.status === 'ACTIVE'));
      setStaffById(nameMap);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

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
        Pin a Head of Department for each department. You can pick any active staff member —
        if they aren't an HoD yet, assigning them here promotes them to Head of Department.
        Student booking requests are reviewed by their department's HoD first.
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
                  <th>Current HoD</th><th>Staff</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {departments.map((d) => {
                  const eligible = staffByDept[d.id] || [];
                  const currentHod = d.hodUserId ? staffById[d.hodUserId] : null;
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
                  eligible={allStaff}
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
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.email}){u.role !== 'HOD' ? ` — currently ${roleLabel(u.role)}` : ''}
                </option>
              ))}
            </select>
            {eligible.length === 0 ? (
              <span className="field-hint">
                No active staff yet — staff appear here once they register.
              </span>
            ) : (
              <span className="field-hint">
                Any staff member can be picked. If they aren't an HoD yet, they'll be promoted to
                Head of Department of this department.
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
