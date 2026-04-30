import { useEffect, useState } from 'react';
import { labApi, userApi, errMsg } from '../../api';

export default function Labs() {
  const [labs, setLabs] = useState([]);
  const [instructors, setInstructors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [newLabName, setNewLabName] = useState('');
  const [creating, setCreating] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [labsRes, instRes] = await Promise.all([
        labApi.list(),
        userApi.getByRole('INSTRUCTOR'),
      ]);
      setLabs(labsRes.data);
      // Only show ACTIVE instructors (admin has already approved them)
      setInstructors((instRes.data || []).filter(u => u.status === 'ACTIVE'));
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const createLab = async (e) => {
    e.preventDefault();
    if (!newLabName.trim()) return;
    setCreating(true);
    try {
      await labApi.create({ name: newLabName.trim() });
      setMsg(`Created lab "${newLabName}".`);
      setNewLabName('');
      load();
      setTimeout(() => setMsg(''), 4000);
    } catch (err) {
      alert(errMsg(err));
    } finally {
      setCreating(false);
    }
  };

  const deleteLab = async (id, name) => {
    if (!confirm(`Delete lab "${name}"? All instructor assignments will be removed.`)) return;
    try {
      await labApi.remove(id);
      setMsg(`Deleted lab "${name}".`);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const assign = async (labId, instructorId) => {
    if (!instructorId) return;
    try {
      await labApi.assignInstructor(labId, Number(instructorId));
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const unassign = async (labId, instructorId, name) => {
    if (!confirm(`Remove ${name} from this lab?`)) return;
    try {
      await labApi.unassignInstructor(labId, instructorId);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Manage Labs</h1>
      <p className="page-sub">Create labs and assign instructors. Each instructor can manage equipment for the labs they're assigned to.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {msg && <div className="alert alert-success">{msg}</div>}

      <form onSubmit={createLab} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginBottom: 24 }}>
        <div className="field" style={{ flex: 1, marginBottom: 0 }}>
          <label>New Lab Name</label>
          <input value={newLabName}
                 onChange={(e) => setNewLabName(e.target.value)}
                 placeholder="Lab 234" required />
        </div>
        <button type="submit" className="btn-pill btn-pill-solid" disabled={creating}>
          {creating ? 'Creating…' : '+ Create Lab'}
        </button>
      </form>

      {loading ? <div className="loading">Loading…</div> : (
        labs.length === 0 ? (
          <div className="empty"><div className="empty-icon">🏗</div>No labs yet. Create one above.</div>
        ) : (
          <div style={{ display: 'grid', gap: 16 }}>
            {labs.map((lab) => {
              const assignedIds = new Set((lab.instructors || []).map(i => i.id));
              const unassigned = instructors.filter(i => !assignedIds.has(i.id));
              return (
                <div key={lab.id} className="lab-card">
                  <div className="lab-card-head">
                    <div>
                      <div className="lab-card-eyebrow">Lab #{lab.id}</div>
                      <h3 className="lab-card-name">{lab.name}</h3>
                    </div>
                    <button className="btn btn-danger btn-sm" onClick={() => deleteLab(lab.id, lab.name)}>
                      Delete Lab
                    </button>
                  </div>

                  <div className="lab-card-section">
                    <div className="lab-card-section-title">Assigned Instructors ({lab.instructors?.length || 0})</div>
                    {(!lab.instructors || lab.instructors.length === 0) ? (
                      <div className="lab-card-empty">No instructors assigned yet — they can't add equipment for this lab.</div>
                    ) : (
                      <div className="lab-instructor-list">
                        {lab.instructors.map((i) => (
                          <span key={i.id} className="lab-instructor-pill">
                            {i.fullName}
                            {i.department ? <span className="lab-instructor-dept"> · {i.department}</span> : null}
                            <button className="lab-instructor-x"
                                    title={`Unassign ${i.fullName}`}
                                    onClick={() => unassign(lab.id, i.id, i.fullName)}>×</button>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="lab-card-section">
                    <div className="lab-card-section-title">Assign more</div>
                    {unassigned.length === 0 ? (
                      <div className="lab-card-empty">All active instructors are already assigned to this lab.</div>
                    ) : (
                      <select defaultValue=""
                              onChange={(e) => { assign(lab.id, e.target.value); e.target.value = ''; }}>
                        <option value="">— Pick an instructor to assign —</option>
                        {unassigned.map((i) => (
                          <option key={i.id} value={i.id}>
                            {i.fullName}{i.department ? ` (${i.department})` : ''}
                          </option>
                        ))}
                      </select>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}
    </div>
  );
}
