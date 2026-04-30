import { useEffect, useMemo, useState } from 'react';
import { bookingApi, itemApi, labApi, userApi, departmentApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';
import { byId, fmt, trunc } from '../../utils/format';

const ACTIONABLE = new Set(['SUBMITTED', 'INSTRUCTOR_REVIEWING']);
const FINALISABLE = new Set(['SUPERVISOR_APPROVED']);
const READY = new Set(['READY_FOR_COLLECTION']);
const ACTIVE_LOAN = new Set(['COLLECTED', 'OVERDUE']);
const WAITING_SUPERVISOR = new Set(['AWAITING_SUPERVISOR']);

export default function PendingBookings() {
  const me = getCurrentUser();
  const [bookings, setBookings] = useState([]);
  const [itemMap, setItemMap] = useState({});
  const [labMap, setLabMap] = useState({});
  const [studentMap, setStudentMap] = useState({});
  const [supervisorMap, setSupervisorMap] = useState({});
  const [departmentMap, setDepartmentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data: bks } = await bookingApi.assignedToMe();
      setBookings(bks);

      const [{ data: items }, { data: labs }, { data: depts }] = await Promise.all([
        itemApi.list(), labApi.list(), departmentApi.list(),
      ]);
      setItemMap(byId(items));
      setLabMap(byId(labs));
      setDepartmentMap(byId(depts));

      const studentIds = [...new Set(bks.map((b) => b.studentUserId))];
      const supervisorIds = [...new Set(bks.flatMap((b) =>
        (b.items || []).map((i) => i.assignedSupervisorUserId).filter(Boolean)))];
      const sm = {}, vm = {};
      await Promise.all([
        ...studentIds.map(async (id) => {
          try { const { data } = await userApi.getById(id); sm[id] = data; } catch {}
        }),
        ...supervisorIds.map(async (id) => {
          try { const { data } = await userApi.getById(id); vm[id] = data; } catch {}
        }),
      ]);
      setStudentMap(sm);
      setSupervisorMap(vm);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const onAction = async (action, booking, line, body) => {
    try {
      switch (action) {
        case 'approve':   await bookingApi.approve(booking.id, line.id, body); break;
        case 'reject':    await bookingApi.reject(booking.id, line.id, body.reason); break;
        case 'delegate':  await bookingApi.delegate(booking.id, line.id, body); break;
        case 'finalise':  await bookingApi.finalise(booking.id, line.id, body); break;
        case 'collect':   await bookingApi.collect(booking.id, line.id); break;
        case 'return':    await bookingApi.markReturned(booking.id, line.id); break;
        default: throw new Error('unknown action: ' + action);
      }
      setModal(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  const visibleBookings = useMemo(() => bookings.filter((b) =>
    (b.items || []).some((line) => line.instructorUserId === me?.id)
  ), [bookings, me?.id]);

  return (
    <div className="container">
      <h1 className="page-title">My Review Queue</h1>
      <p className="page-sub">Booking requests for items in the labs assigned to you. Each line is reviewed independently.</p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        visibleBookings.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>Nothing waiting on you.</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {visibleBookings.map((b) => (
              <BookingCard
                key={b.id}
                booking={b}
                me={me}
                itemMap={itemMap}
                labMap={labMap}
                studentMap={studentMap}
                supervisorMap={supervisorMap}
                onOpen={(kind, line, action) =>
                  setModal({ kind, booking: b, line, action })
                }
                onPlainAction={(action, line) => onAction(action, b, line, {})}
              />
            ))}
          </div>
        )
      )}

      {modal?.kind === 'review' && (
        <ReviewModal action={modal.action} bookingId={modal.booking.id} lineId={modal.line.id}
                     onClose={() => setModal(null)}
                     onSubmit={(body) => onAction(modal.action, modal.booking, modal.line, body)} />
      )}
      {modal?.kind === 'delegate' && (
        <DelegateModal booking={modal.booking} line={modal.line}
                       suggestedDeptHodUserId={departmentMap[modal.booking.studentDepartmentId]?.hodUserId}
                       onClose={() => setModal(null)}
                       onSubmit={(body) => onAction('delegate', modal.booking, modal.line, body)} />
      )}
      {modal?.kind === 'finalise' && (
        <FinaliseModal bookingId={modal.booking.id} lineId={modal.line.id}
                       onClose={() => setModal(null)}
                       onSubmit={(body) => onAction('finalise', modal.booking, modal.line, body)} />
      )}
    </div>
  );
}

function BookingCard({ booking, me, itemMap, labMap, studentMap, supervisorMap, onOpen, onPlainAction }) {
  const myLines = (booking.items || []).filter((line) => line.instructorUserId === me?.id);
  const student = studentMap[booking.studentUserId];

  return (
    <div className="card" style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Booking #{booking.id}</div>
          <h3 style={{ margin: '4px 0' }}>{booking.projectName}</h3>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>
            {student ? <>{student.fullName}{student.indexNumber ? ` · ${student.indexNumber}` : ''}</> : `Student #${booking.studentUserId}`}
            {' · '}{fmt(booking.startDate)} → {fmt(booking.returnDate)}
          </div>
          {booking.purpose && (
            <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>{trunc(booking.purpose, 200)}</div>
          )}
        </div>
        <Badge value={booking.state} />
      </div>

      <div className="table-wrap" style={{ marginTop: 12 }}>
        <table>
          <thead>
            <tr>
              <th>Item</th><th>Lab</th><th>State</th><th>Pickup</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {myLines.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              const sv = line.assignedSupervisorUserId ? supervisorMap[line.assignedSupervisorUserId] : null;
              return (
                <tr key={line.id}>
                  <td>{it?.name || `#${line.itemId}`}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{it?.model}</div></td>
                  <td>{lb?.name || `#${line.labId}`}</td>
                  <td>
                    <Badge value={line.state} />
                    {WAITING_SUPERVISOR.has(line.state) && sv && (
                      <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>
                        with {sv.fullName}
                      </div>
                    )}
                  </td>
                  <td>{line.pickupAt ? fmt(line.pickupAt) : '—'}</td>
                  <td>
                    <Actions line={line}
                             onOpen={onOpen}
                             onPlainAction={onPlainAction} />
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

function Actions({ line, onOpen, onPlainAction }) {
  if (ACTIONABLE.has(line.state)) {
    return (
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button className="btn btn-success btn-sm"
                onClick={() => onOpen('review', line, 'approve')}>Approve</button>
        <button className="btn btn-secondary btn-sm"
                onClick={() => onOpen('delegate', line)}>Delegate</button>
        <button className="btn btn-danger btn-sm"
                onClick={() => onOpen('review', line, 'reject')}>Reject</button>
      </div>
    );
  }
  if (FINALISABLE.has(line.state)) {
    return (
      <button className="btn btn-success btn-sm"
              onClick={() => onOpen('finalise', line)}>
        Finalise
      </button>
    );
  }
  if (READY.has(line.state)) {
    return (
      <button className="btn btn-success btn-sm"
              onClick={() => onPlainAction('collect', line)}>
        Mark collected
      </button>
    );
  }
  if (ACTIVE_LOAN.has(line.state)) {
    return (
      <button className="btn btn-secondary btn-sm"
              onClick={() => onPlainAction('return', line)}>
        Mark returned
      </button>
    );
  }
  return <span style={{ fontSize: 12, color: 'var(--muted)' }}>—</span>;
}

function ReviewModal({ action, bookingId, lineId, onClose, onSubmit }) {
  const isApprove = action === 'approve';
  const [pickupAt, setPickupAt] = useState('');
  const [pickupNote, setPickupNote] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (isApprove) await onSubmit({ pickupAt, pickupNote });
      else            await onSubmit({ reason });
    } finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isApprove ? 'Approve' : 'Reject'} line #{lineId} of booking #{bookingId}</h2>
        <form onSubmit={submit}>
          {isApprove ? (
            <>
              <div className="field">
                <label>Pickup date & time</label>
                <input type="datetime-local" required value={pickupAt}
                       onChange={(e) => setPickupAt(e.target.value)} />
              </div>
              <div className="field">
                <label>Pickup note (optional)</label>
                <textarea rows="3" value={pickupNote}
                          onChange={(e) => setPickupNote(e.target.value)} />
              </div>
            </>
          ) : (
            <div className="field">
              <label>Reason (visible to student)</label>
              <textarea rows="3" value={reason}
                        onChange={(e) => setReason(e.target.value)} />
            </div>
          )}
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className={`btn ${isApprove ? 'btn-success' : 'btn-danger'}`}
                    disabled={busy}>
              {busy ? 'Working…' : (isApprove ? 'Approve' : 'Reject')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function DelegateModal({ booking, line, suggestedDeptHodUserId, onClose, onSubmit }) {
  const [q, setQ] = useState('');
  const [results, setResults] = useState([]);
  const [picked, setPicked] = useState(null);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    if (!suggestedDeptHodUserId) return;
    (async () => {
      try {
        const { data } = await userApi.getById(suggestedDeptHodUserId);
        if (data?.status === 'ACTIVE') setPicked(data);
      } catch {}
    })();
  }, [suggestedDeptHodUserId]);

  useEffect(() => {
    const term = q.trim();
    if (term.length < 2) { setResults([]); return; }
    const t = setTimeout(async () => {
      setSearching(true);
      try {
        const { data } = await userApi.search({ q: term, roles: 'HOD,LECTURER', limit: 15 });
        setResults(data);
      } catch { setResults([]); }
      finally { setSearching(false); }
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  const submit = async (e) => {
    e.preventDefault();
    if (!picked) { alert('Pick a supervisor first.'); return; }
    setBusy(true);
    try { await onSubmit({ supervisorUserId: picked.id, note }); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
        <h2>Delegate line #{line.id} of booking #{booking.id}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Supervisor</label>
            {picked ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px',
                            border: '1px solid var(--border)', borderRadius: 8, background: 'var(--bg-soft)' }}>
                <div style={{ flex: 1 }}>
                  <strong>{picked.fullName}</strong> <span style={{ color: 'var(--muted)' }}>· {picked.role} · {picked.email}</span>
                </div>
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => setPicked(null)}>
                  Change
                </button>
              </div>
            ) : (
              <>
                <input value={q} onChange={(e) => setQ(e.target.value)}
                       placeholder="Type a name or email (HoDs and Lecturers only)…" autoFocus />
                {searching && <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 6 }}>Searching…</div>}
                {!searching && results.length > 0 && (
                  <ul style={{ listStyle: 'none', padding: 0, margin: '8px 0 0',
                               maxHeight: 240, overflow: 'auto',
                               border: '1px solid var(--border)', borderRadius: 8 }}>
                    {results.map((u) => (
                      <li key={u.id}>
                        <button type="button"
                                onClick={() => setPicked(u)}
                                style={{ width: '100%', textAlign: 'left', padding: '8px 12px',
                                         background: 'transparent', border: 'none', cursor: 'pointer',
                                         borderBottom: '1px solid var(--border)' }}>
                          <strong>{u.fullName}</strong>
                          <span style={{ color: 'var(--muted)' }}> · {u.role} · {u.email}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                {!searching && q.trim().length >= 2 && results.length === 0 && (
                  <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 6 }}>No matches.</div>
                )}
              </>
            )}
          </div>
          <div className="field">
            <label>Note for the supervisor (optional)</label>
            <textarea rows="3" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn" disabled={busy || !picked}>
              {busy ? 'Sending…' : 'Send for approval'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function FinaliseModal({ bookingId, lineId, onClose, onSubmit }) {
  const [pickupAt, setPickupAt] = useState('');
  const [pickupNote, setPickupNote] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try { await onSubmit({ pickupAt, pickupNote }); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Finalise line #{lineId} of booking #{bookingId}</h2>
        <p style={{ marginTop: 0, color: 'var(--muted)' }}>
          Supervisor approved this line. Set the pickup datetime to release the item.
        </p>
        <form onSubmit={submit}>
          <div className="field">
            <label>Pickup date & time</label>
            <input type="datetime-local" required value={pickupAt}
                   onChange={(e) => setPickupAt(e.target.value)} />
          </div>
          <div className="field">
            <label>Pickup note (optional)</label>
            <textarea rows="3" value={pickupNote}
                      onChange={(e) => setPickupNote(e.target.value)} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-success" disabled={busy}>
              {busy ? 'Working…' : 'Release to student'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

