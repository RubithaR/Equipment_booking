import { useMemo, useState } from 'react';
import { bookingApi, itemApi, labApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';
import { byId, fmt, trunc } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

// Lines reach the instructor only after the HoD approves (state SUBMITTED).
const ACTIONABLE = new Set(['SUBMITTED', 'INSTRUCTOR_REVIEWING']);
const READY = new Set(['READY_FOR_COLLECTION']);
const ACTIVE_LOAN = new Set(['COLLECTED', 'OVERDUE']);

const isLabOnly = (it) => (it?.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';

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
        case 'approve':    await bookingApi.approve(booking.id, line.id, body); break;
        case 'confirmLab': await bookingApi.confirmLab(booking.id, line.id, body); break;
        case 'reject':     await bookingApi.reject(booking.id, line.id, body.reason); break;
        case 'collect':    await bookingApi.collect(booking.id, line.id); break;
        case 'return':     await bookingApi.markReturned(booking.id, line.id); break;
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
      <p className="page-sub">
        Requests that your Head of Department has already approved, now awaiting your final sign-off.
        Borrowable items get a pickup time; lab-only items get a confirmed in-lab time.
      </p>

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
                onOpen={(kind, line, action) =>
                  setModal({ kind, booking: b, line, action })
                }
                onPlainAction={(action, line) => onAction(action, b, line, {})}
              />
            ))}
          </div>
        )
      )}

      {modal?.kind === 'approve' && (
        <ApproveModal booking={modal.booking} line={modal.line}
                      labOnly={isLabOnly(itemMap[modal.line.itemId])}
                      onClose={() => setModal(null)}
                      onSubmit={(action, body) => onAction(action, modal.booking, modal.line, body)} />
      )}
      {modal?.kind === 'reject' && (
        <RejectModal bookingId={modal.booking.id} lineId={modal.line.id}
                     onClose={() => setModal(null)}
                     onSubmit={(body) => onAction('reject', modal.booking, modal.line, body)} />
      )}
    </div>
  );
}

function BookingCard({ booking, me, itemMap, labMap, studentMap, onOpen, onPlainAction }) {
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
              <th>Item</th><th>Use</th><th>Lab</th><th>State</th><th>Time</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {myLines.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              const labOnly = isLabOnly(it);
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
                  <td>
                    <Actions line={line} labOnly={labOnly}
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

function Actions({ line, labOnly, onOpen, onPlainAction }) {
  if (ACTIONABLE.has(line.state)) {
    return (
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button className="btn btn-success btn-sm"
                onClick={() => onOpen('approve', line)}>
          {labOnly ? 'Confirm time' : 'Approve'}
        </button>
        <button className="btn btn-danger btn-sm"
                onClick={() => onOpen('reject', line)}>Reject</button>
      </div>
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
  if (line.state === 'LAB_CONFIRMED') {
    return <span style={{ fontSize: 12, color: 'var(--muted)' }}>Lab use confirmed</span>;
  }
  return <span style={{ fontSize: 12, color: 'var(--muted)' }}>—</span>;
}

function ApproveModal({ booking, line, labOnly, onClose, onSubmit }) {
  const [time, setTime] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (labOnly) await onSubmit('confirmLab', { availableTime: time, note });
      else         await onSubmit('approve', { pickupAt: time, pickupNote: note });
    } finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{labOnly ? 'Confirm lab time for' : 'Approve'} line #{line.id} of booking #{booking.id}</h2>
        {labOnly && line.requestedUseTime && (
          <p style={{ marginTop: 0, color: 'var(--muted)' }}>
            Student requested: <strong>{fmt(line.requestedUseTime)}</strong>
          </p>
        )}
        <form onSubmit={submit}>
          <div className="field">
            <label>{labOnly ? 'Available lab time (when the student can use it)' : 'Pickup date & time'}</label>
            <input type="datetime-local" required value={time}
                   onChange={(e) => setTime(e.target.value)} />
          </div>
          <div className="field">
            <label>{labOnly ? 'Note (optional)' : 'Pickup note (optional)'}</label>
            <textarea rows="3" value={note}
                      onChange={(e) => setNote(e.target.value)} />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-success" disabled={busy}>
              {busy ? 'Working…' : (labOnly ? 'Confirm time' : 'Approve')}
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
    setBusy(true);
    try { await onSubmit({ reason }); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Reject line #{lineId} of booking #{bookingId}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Reason (visible to student)</label>
            <textarea rows="3" value={reason}
                      onChange={(e) => setReason(e.target.value)} />
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
