import { useState } from 'react';
import { bookingApi, itemApi, labApi, userApi, errMsg } from '../../api';
import Badge from '../../components/Badge';
import { byId, fmt, trunc } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

export default function HodDashboard() {
  const [tab, setTab] = useState('requests');           // 'requests' | 'approved'
  const [requests, setRequests] = useState([]);          // awaiting my review (SUBMITTED)
  const [processed, setProcessed] = useState([]);        // already approved/rejected
  const [itemMap, setItemMap] = useState({});
  const [labMap, setLabMap] = useState({});
  const [userMap, setUserMap] = useState({});            // userId -> { fullName, role, email, indexNumber }
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [modal, setModal] = useState(null);              // { kind:'assign'|'reject', booking, line }

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [{ data: reqs }, { data: done }, { data: items }, { data: labs }] = await Promise.all([
        bookingApi.awaitingHod(),
        bookingApi.hodProcessed(),
        itemApi.list(),
        labApi.list(),
      ]);
      if (isCancelled?.()) return;
      setRequests(reqs);
      setProcessed(done);
      const labsById = byId(labs);
      setItemMap(byId(items));
      setLabMap(labsById);

      // Resolve the users we display: students, each line's handler, and each lab's default instructor.
      const ids = new Set();
      [...reqs, ...done].forEach((b) => {
        ids.add(b.studentUserId);
        (b.items || []).forEach((l) => {
          if (l.instructorUserId) ids.add(l.instructorUserId);
          const labInstr = labsById[l.labId]?.instructorUserId;
          if (labInstr) ids.add(labInstr);
        });
      });
      const um = {};
      await Promise.all([...ids].map(async (id) => {
        try { const { data } = await userApi.getById(id); um[id] = data; } catch {}
      }));
      if (!isCancelled?.()) setUserMap(um);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const onApprove = async ({ handlerUserId, note }) => {
    try {
      await bookingApi.hodApprove(modal.booking.id, modal.line.id, { handlerUserId, note });
      setMsg('Approved and sent to the assigned handler.');
      setModal(null);
      load();
    } catch (err) { alert(errMsg(err)); }
  };

  const onReject = async (reason) => {
    try {
      await bookingApi.hodReject(modal.booking.id, modal.line.id, reason);
      setMsg('Request rejected. The student has been notified.');
      setModal(null);
      load();
    } catch (err) { alert(errMsg(err)); }
  };

  const userName = (id) => userMap[id]?.fullName || (id ? `User #${id}` : '—');

  return (
    <div className="container">
      <h1 className="page-title">HOD Dashboard</h1>
      <p className="page-sub">Review booking requests from your department and assign each to an instructor, lecturer, or HOD.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {msg && <div className="alert alert-success">{msg}</div>}

      <div className="filter-bar" style={{ gap: 8 }}>
        <button className={`btn ${tab === 'requests' ? '' : 'btn-secondary'}`} onClick={() => setTab('requests')}>
          Student Requests{requests.length ? ` (${requests.length})` : ''}
        </button>
        <button className={`btn ${tab === 'approved' ? '' : 'btn-secondary'}`} onClick={() => setTab('approved')}>
          Approved / Sent{processed.length ? ` (${processed.length})` : ''}
        </button>
      </div>

      {loading ? <div className="loading">Loading…</div> : (
        tab === 'requests' ? (
          requests.length === 0 ? (
            <div className="empty"><div className="empty-icon">✅</div>No requests waiting on you.</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {requests.map((b) => (
                <RequestCard key={b.id} booking={b} itemMap={itemMap} labMap={labMap}
                             userMap={userMap} userName={userName}
                             onAssign={(line) => setModal({ kind: 'assign', booking: b, line })}
                             onReject={(line) => setModal({ kind: 'reject', booking: b, line })} />
              ))}
            </div>
          )
        ) : (
          processed.length === 0 ? (
            <div className="empty"><div className="empty-icon">🗂️</div>You haven't approved any requests yet.</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {processed.map((b) => (
                <ProcessedCard key={b.id} booking={b} itemMap={itemMap} labMap={labMap}
                               userName={userName} />
              ))}
            </div>
          )
        )
      )}

      {modal?.kind === 'assign' && (
        <AssignModal booking={modal.booking} line={modal.line}
                     itemMap={itemMap} labMap={labMap}
                     onClose={() => setModal(null)} onSubmit={onApprove} />
      )}
      {modal?.kind === 'reject' && (
        <RejectModal bookingId={modal.booking.id} lineId={modal.line.id}
                     onClose={() => setModal(null)} onSubmit={onReject} />
      )}
    </div>
  );
}

function Header({ booking, userName }) {
  const student = booking.studentUserId;
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 13, color: 'var(--muted)' }}>Booking #{booking.id}</div>
        <h3 style={{ margin: '4px 0' }}>{booking.projectName}</h3>
        <div style={{ fontSize: 13, color: 'var(--muted)' }}>
          {userName(student)}{' · '}{fmt(booking.startDate)} → {fmt(booking.returnDate)}
        </div>
        {booking.purpose && (
          <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>{trunc(booking.purpose, 240)}</div>
        )}
      </div>
      <Badge value={booking.state} />
    </div>
  );
}

function RequestCard({ booking, itemMap, labMap, userName, onAssign, onReject }) {
  const lines = (booking.items || []).filter((l) => l.state === 'SUBMITTED');
  if (lines.length === 0) return null;
  return (
    <div className="card" style={{ padding: 16 }}>
      <Header booking={booking} userName={userName} />
      <div className="table-wrap" style={{ marginTop: 12 }}>
        <table>
          <thead>
            <tr><th>Item</th><th>Lab</th><th>Lab instructor</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {lines.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              const labOnly = (line.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';
              const times = (line.useSlots || []).map((s) => `${fmt(s.at)}${s.to ? ` – ${fmt(s.to)}` : ''}`);
              return (
                <tr key={line.id}>
                  <td>
                    {it?.name || `#${line.itemId}`}
                    <div style={{ fontSize: 12, color: 'var(--muted)' }}>{it?.model}</div>
                    {labOnly && (
                      <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>
                        🏫 Requested lab times:{' '}
                        {times.length ? times.join(' · ') : '—'}
                      </div>
                    )}
                  </td>
                  <td>{lb?.name || `#${line.labId}`}</td>
                  <td>{userName(lb?.instructorUserId)}</td>
                  <td style={{ display: 'flex', gap: 8 }}>
                    <button className="btn btn-success btn-sm" onClick={() => onAssign(line)}>Approve &amp; assign</button>
                    <button className="btn btn-danger btn-sm" onClick={() => onReject(line)}>Reject</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ProcessedCard({ booking, itemMap, labMap, userName }) {
  const lines = (booking.items || []);
  return (
    <div className="card" style={{ padding: 16 }}>
      <Header booking={booking} userName={userName} />
      <div className="table-wrap" style={{ marginTop: 12 }}>
        <table>
          <thead>
            <tr><th>Item</th><th>Lab</th><th>Sent to</th><th>State</th></tr>
          </thead>
          <tbody>
            {lines.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              return (
                <tr key={line.id}>
                  <td>{it?.name || `#${line.itemId}`}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{it?.model}</div></td>
                  <td>{lb?.name || `#${line.labId}`}</td>
                  <td>{line.state === 'SUBMITTED' ? '—' : userName(line.instructorUserId)}</td>
                  <td><Badge value={line.state} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AssignModal({ booking, line, itemMap, labMap, onClose, onSubmit }) {
  const [staff, setStaff] = useState([]);
  const [loading, setLoading] = useState(true);
  const [handlerId, setHandlerId] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  // Load every active instructor / lecturer / HOD so the HOD can pick from the full list.
  useAsyncEffect(async (isCancelled) => {
    setLoading(true);
    try {
      const { data } = await userApi.search({ q: '', roles: 'INSTRUCTOR,LECTURER,HOD', limit: 100 });
      if (!isCancelled()) setStaff(data);
    } catch {
      if (!isCancelled()) setStaff([]);
    } finally {
      if (!isCancelled()) setLoading(false);
    }
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    if (!handlerId) { alert('Pick a handler first.'); return; }
    setBusy(true);
    try { await onSubmit({ handlerUserId: Number(handlerId), note }); }
    finally { setBusy(false); }
  };

  const it = itemMap[line.itemId];
  const lb = labMap[line.labId];
  const groups = [['INSTRUCTOR', 'Instructors'], ['LECTURER', 'Lecturers'], ['HOD', 'HODs']];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
        <h2>Assign handler — {it?.name || `item #${line.itemId}`}</h2>
        <p style={{ marginTop: 0, color: 'var(--muted)' }}>
          {lb?.name || `Lab #${line.labId}`} · booking #{booking.id}. Send to any instructor, lecturer, or HOD.
        </p>
        <form onSubmit={submit}>
          <div className="field">
            <label>Handler</label>
            {loading ? (
              <div style={{ fontSize: 13, color: 'var(--muted)' }}>Loading staff…</div>
            ) : staff.length === 0 ? (
              <div style={{ fontSize: 13, color: 'var(--muted)' }}>No active staff found.</div>
            ) : (
              <select value={handlerId} required autoFocus
                      onChange={(e) => setHandlerId(e.target.value)}>
                <option value="">— Select a handler —</option>
                {groups.map(([role, label]) => {
                  const list = staff.filter((u) => u.role === role);
                  if (!list.length) return null;
                  return (
                    <optgroup key={role} label={label}>
                      {list.map((u) => (
                        <option key={u.id} value={u.id}>{u.fullName} · {u.email}</option>
                      ))}
                    </optgroup>
                  );
                })}
              </select>
            )}
          </div>
          <div className="field">
            <label>Note for the handler (optional)</label>
            <textarea rows="3" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-success" disabled={busy || !handlerId}>
              {busy ? 'Sending…' : 'Approve & send'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RejectModal({ bookingId, lineId, onClose, onSubmit }) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) { alert('Please give a reason.'); return; }
    setBusy(true);
    try { await onSubmit(reason); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Reject line #{lineId} of booking #{bookingId}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Reason (visible to the student)</label>
            <textarea rows="3" value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-danger" disabled={busy}>
              {busy ? 'Working…' : 'Reject'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
