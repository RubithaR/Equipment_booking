import { useMemo, useState } from 'react';
import { bookingApi, itemApi, labApi, userApi, departmentApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';
import { byId, fmt, trunc } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const isLabOnly = (it) => (it?.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';

export default function SupervisorQueue() {
  const me = getCurrentUser();
  const [department, setDepartment] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [itemMap, setItemMap] = useState({});
  const [labMap, setLabMap] = useState({});
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reviewing, setReviewing] = useState(null);     // { booking, line, action }

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [{ data: bks }, { data: items }, { data: labs }] = await Promise.all([
        bookingApi.awaitingMySupervision(),
        itemApi.list(),
        labApi.list(),
      ]);
      if (isCancelled?.()) return;
      setBookings(bks);
      setItemMap(byId(items));
      setLabMap(byId(labs));

      if (me?.departmentId) {
        try { const { data } = await departmentApi.get(me.departmentId); if (!isCancelled?.()) setDepartment(data); } catch {}
      }

      const studentIds = [...new Set(bks.map((b) => b.studentUserId))];
      const sm = {};
      await Promise.all(studentIds.map(async (id) => {
        try { const { data } = await userApi.getById(id); sm[id] = data; } catch {}
      }));
      if (!isCancelled?.()) setStudentMap(sm);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const submit = async (note) => {
    const { booking, line, action } = reviewing;
    try {
      if (action === 'approve') await bookingApi.hodApprove(booking.id, line.id, note);
      else                       await bookingApi.hodReject(booking.id, line.id, note);
      setReviewing(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  // Per-booking, only show lines awaiting THIS HoD's decision.
  const visible = useMemo(() => bookings
    .map((b) => ({
      booking: b,
      lines: (b.items || []).filter((line) =>
        line.assignedHodUserId === me?.id
        && line.state === 'AWAITING_HOD'),
    }))
    .filter((row) => row.lines.length > 0), [bookings, me?.id]);

  return (
    <div className="container">
      <h1 className="page-title">
        HoD Approval Queue
        {department && <span className="dept-chip">{department.name}{department.code ? ` · ${department.code}` : ''}</span>}
      </h1>
      <p className="page-sub">
        Equipment requests from students in your department, awaiting your stage-1 approval.
        Approve to forward to the lab instructor, or reject with a reason.
      </p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        visible.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>Nothing waiting on you.</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {visible.map(({ booking: b, lines }) => {
              const st = studentMap[b.studentUserId];
              return (
                <div key={b.id} className="card" style={{ padding: 16 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, color: 'var(--muted)' }}>Booking #{b.id}</div>
                      <h3 style={{ margin: '4px 0' }}>{b.projectName}</h3>
                      <div style={{ fontSize: 13, color: 'var(--muted)' }}>
                        {st ? <>{st.fullName}{st.indexNumber ? ` · ${st.indexNumber}` : ''}{st.uniEmail ? ` · ${st.uniEmail}` : ''}</> : `Student #${b.studentUserId}`}
                        {' · '}{fmt(b.startDate)} → {fmt(b.returnDate)}
                      </div>
                      {b.purpose && (
                        <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>{trunc(b.purpose, 200)}</div>
                      )}
                    </div>
                  </div>

                  <div className="table-wrap" style={{ marginTop: 12 }}>
                    <table>
                      <thead>
                        <tr>
                          <th>Item</th><th>Use</th><th>Lab</th><th>Requested time</th><th>State</th><th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {lines.map((line) => {
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
                              <td>{line.requestedUseTime ? fmt(line.requestedUseTime) : '—'}</td>
                              <td><Badge value={line.state} /></td>
                              <td style={{ display: 'flex', gap: 8 }}>
                                <button className="btn btn-success btn-sm"
                                        onClick={() => setReviewing({ booking: b, line, action: 'approve' })}>
                                  Approve
                                </button>
                                <button className="btn btn-danger btn-sm"
                                        onClick={() => setReviewing({ booking: b, line, action: 'decline' })}>
                                  Reject
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}

      {reviewing && (
        <DecisionModal action={reviewing.action}
                       lineId={reviewing.line.id}
                       bookingId={reviewing.booking.id}
                       onClose={() => setReviewing(null)}
                       onSubmit={submit} />
      )}
    </div>
  );
}

function DecisionModal({ action, bookingId, lineId, onClose, onSubmit }) {
  const isApprove = action === 'approve';
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!isApprove && !note.trim()) {
      alert('Please provide a reason for rejecting.'); return;
    }
    setBusy(true);
    try { await onSubmit(note); }
    finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isApprove ? 'Approve' : 'Reject'} line #{lineId} of booking #{bookingId}</h2>
        <p style={{ marginTop: 0, color: 'var(--muted)' }}>
          {isApprove
            ? 'Approving forwards this line to the lab instructor for final approval.'
            : 'Rejecting ends this line — the student is notified with your reason.'}
        </p>
        <form onSubmit={submit}>
          <div className="field">
            <label>{isApprove ? 'Note (optional)' : 'Reason (visible to the student)'}</label>
            <textarea rows="3" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
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
