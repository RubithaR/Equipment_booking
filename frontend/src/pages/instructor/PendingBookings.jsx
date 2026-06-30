import { useMemo, useState } from 'react';
import { bookingApi, itemApi, labApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';
import { byId, fmt, trunc } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const ACTIONABLE = new Set(['AWAITING_HANDLER']);   // approve / reject
const READY = new Set(['READY_FOR_COLLECTION']);    // mark collected
const ACTIVE_LOAN = new Set(['COLLECTED', 'OVERDUE']); // mark returned

// Sort priority for the queue — items needing my decision float to the top.
const ACTION_RANK = { AWAITING_HANDLER: 0, READY_FOR_COLLECTION: 1, COLLECTED: 2, OVERDUE: 2 };

export default function PendingBookings() {
  const me = getCurrentUser();
  const [bookings, setBookings] = useState([]);
  const [itemMap, setItemMap] = useState({});
  const [labMap, setLabMap] = useState({});
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [{ data: bks }, { data: items }, { data: labs }] = await Promise.all([
        bookingApi.assignedToMe(),
        itemApi.list(),
        labApi.list(),
      ]);
      if (isCancelled?.()) return;
      setBookings(bks);
      setItemMap(byId(items));
      setLabMap(byId(labs));

      const studentIds = [...new Set(bks.map((b) => b.studentUserId))];
      const sm = {};
      await Promise.all(studentIds.map(async (id) => {
        try { const { data } = await userApi.getById(id); sm[id] = data; } catch {}
      }));
      if (isCancelled?.()) return;
      setStudentMap(sm);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const onAction = async (action, booking, line, body) => {
    try {
      switch (action) {
        case 'approve': await bookingApi.handlerApprove(booking.id, line.id, body); break;
        case 'reject':  await bookingApi.handlerReject(booking.id, line.id, body.reason); break;
        case 'collect': await bookingApi.collect(booking.id, line.id); break;
        case 'return':  await bookingApi.markReturned(booking.id, line.id); break;
        default: throw new Error('unknown action: ' + action);
      }
      setModal(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  // Only lines an HOD has assigned to me (excludes SUBMITTED, which is awaiting HOD review),
  // sorted so requests needing my decision (AWAITING_HANDLER) come first.
  const visibleBookings = useMemo(() => {
    const rankOf = (b) => {
      let best = 9;
      for (const l of (b.items || [])) {
        if (l.instructorUserId !== me?.id || l.state === 'SUBMITTED') continue;
        const r = ACTION_RANK[l.state];
        if (r !== undefined && r < best) best = r;
      }
      return best;
    };
    return bookings
      .filter((b) => (b.items || []).some((line) => line.instructorUserId === me?.id && line.state !== 'SUBMITTED'))
      .slice()
      .sort((a, b) => rankOf(a) - rankOf(b));
  }, [bookings, me?.id]);

  return (
    <div className="container">
      <h1 className="page-title">My Tasks</h1>
      <p className="page-sub">Requests an HOD has assigned to you. Approve to release the item to the student, or reject with a reason.</p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        visibleBookings.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>Nothing assigned to you right now.</div>
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
                onOpen={(line, action) => setModal({ booking: b, line, action })}
                onPlainAction={(action, line) => onAction(action, b, line, {})}
              />
            ))}
          </div>
        )
      )}

      {modal && (
        <ReviewModal action={modal.action} bookingId={modal.booking.id} line={modal.line}
                     onClose={() => setModal(null)}
                     onSubmit={(body) => onAction(modal.action, modal.booking, modal.line, body)} />
      )}
    </div>
  );
}

function BookingCard({ booking, me, itemMap, labMap, studentMap, onOpen, onPlainAction }) {
  const myLines = (booking.items || []).filter((line) => line.instructorUserId === me?.id && line.state !== 'SUBMITTED');
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
              <th>Item</th><th>Use</th><th>Lab</th><th>State</th><th>Pickup / Lab time</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {myLines.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              const labOnly = (line.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';
              return (
                <tr key={line.id}>
                  <td>{it?.name || `#${line.itemId}`}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{it?.model}</div></td>
                  <td>
                    <span className={`eq-usage ${labOnly ? 'eq-usage-lab' : 'eq-usage-borrow'}`}>
                      {labOnly ? 'Lab only' : 'Borrowable'}
                    </span>
                  </td>
                  <td>{lb?.name || `#${line.labId}`}</td>
                  <td><Badge value={line.state} /></td>
                  <td>
                    {line.pickupAt ? fmt(line.pickupAt)
                      : line.requestedUseTime ? <span style={{ color: 'var(--muted)' }}>asked: {fmt(line.requestedUseTime)}</span>
                      : '—'}
                  </td>
                  <td><Actions line={line} onOpen={onOpen} onPlainAction={onPlainAction} /></td>
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
        <button className="btn btn-success btn-sm" onClick={() => onOpen(line, 'approve')}>Approve</button>
        <button className="btn btn-danger btn-sm" onClick={() => onOpen(line, 'reject')}>Reject</button>
      </div>
    );
  }
  if (READY.has(line.state)) {
    return (
      <button className="btn btn-success btn-sm" onClick={() => onPlainAction('collect', line)}>Mark collected</button>
    );
  }
  if (ACTIVE_LOAN.has(line.state)) {
    return (
      <button className="btn btn-secondary btn-sm" onClick={() => onPlainAction('return', line)}>Mark returned</button>
    );
  }
  return <span style={{ fontSize: 12, color: 'var(--muted)' }}>—</span>;
}

function ReviewModal({ action, bookingId, line, onClose, onSubmit }) {
  const isApprove = action === 'approve';
  const labOnly = (line?.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';
  const proposed = line?.useSlots || [];
  const [pickupAt, setPickupAt] = useState('');
  const [pickupNote, setPickupNote] = useState('');
  const [ticked, setTicked] = useState(() => new Set());   // set of slot.at strings the instructor is available for
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState('');

  const toggle = (at) => setTicked((prev) => {
    const next = new Set(prev);
    next.has(at) ? next.delete(at) : next.add(at);
    return next;
  });

  const submit = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!isApprove) {
      if (!reason.trim()) { setFormError('Please give a reason for rejecting.'); return; }
      setBusy(true);
      try { await onSubmit({ reason }); } finally { setBusy(false); }
      return;
    }
    if (labOnly) {
      const confirmedSlots = [...ticked];
      if (confirmedSlots.length === 0) { setFormError('Tick at least one time you are available for.'); return; }
      setBusy(true);
      try { await onSubmit({ confirmedSlots, pickupNote }); } finally { setBusy(false); }
    } else {
      if (!pickupAt) { setFormError('Set a pickup date & time.'); return; }
      setBusy(true);
      try { await onSubmit({ pickupAt, pickupNote }); } finally { setBusy(false); }
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isApprove ? 'Approve' : 'Reject'} line #{line?.id} of booking #{bookingId}</h2>
        <form onSubmit={submit}>
          {isApprove ? (
            labOnly ? (
              <>
                <div className="add-eq-empty" style={{ marginBottom: 8 }}>
                  🏫 Lab-use only. Tick the times you're available — only the ticked times are sent to the student.
                </div>
                <div className="field">
                  <label>Student's proposed times</label>
                  {proposed.length === 0 ? (
                    <div style={{ fontSize: 13, color: 'var(--muted)' }}>The student didn't propose any times.</div>
                  ) : (
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {proposed.map((s) => {
                        const on = ticked.has(s.at);
                        return (
                          <li key={s.at}>
                            <label style={{
                              display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer',
                              padding: '10px 12px', borderRadius: 8,
                              border: `1px solid ${on ? 'var(--success, #1b6e4a)' : 'var(--border)'}`,
                              background: on ? 'rgba(27,110,74,0.07)' : 'transparent',
                              transition: 'background .12s, border-color .12s',
                            }}>
                              <input type="checkbox" checked={on} onChange={() => toggle(s.at)}
                                     style={{ width: 16, height: 16, flex: 'none', margin: 0, accentColor: 'var(--success, #1b6e4a)' }} />
                              <span style={{ fontSize: 14, fontWeight: on ? 600 : 400 }}>
                                {fmt(s.at)}{s.to ? ` – ${fmt(s.to)}` : ''}
                              </span>
                            </label>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>
                <div className="field">
                  <label>Note for the student (optional)</label>
                  <textarea rows="3" value={pickupNote}
                            onChange={(e) => setPickupNote(e.target.value)} />
                </div>
              </>
            ) : (
              <>
                <div className="field">
                  <label>Pickup date &amp; time</label>
                  <input type="datetime-local" required value={pickupAt}
                         onChange={(e) => setPickupAt(e.target.value)} />
                </div>
                <div className="field">
                  <label>Pickup note (optional)</label>
                  <textarea rows="3" value={pickupNote}
                            onChange={(e) => setPickupNote(e.target.value)} />
                </div>
              </>
            )
          ) : (
            <div className="field">
              <label>Reason (visible to student)</label>
              <textarea rows="3" value={reason}
                        onChange={(e) => setReason(e.target.value)} />
            </div>
          )}
          {formError && (
            <div style={{
              marginTop: 12, padding: '14px 18px', borderRadius: 6, fontSize: 14,
              background: 'var(--danger-bg)', color: 'var(--danger)',
              border: '1px solid var(--danger)', borderLeftWidth: 3,
            }}>{formError}</div>
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
