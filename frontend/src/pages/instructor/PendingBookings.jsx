import { useEffect, useState } from 'react';
import { bookingApi, equipmentApi, userApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';

export default function PendingBookings() {
  const me = getCurrentUser();
  const [items, setItems] = useState([]);
  const [equipMap, setEquipMap] = useState({});
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reviewing, setReviewing] = useState(null);
  const [action, setAction] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data: bks } = await bookingApi.byStatus('PENDING_APPROVAL');
      setItems(bks);

      const { data: equips } = await equipmentApi.list();
      const m = {}; equips.forEach((e) => { m[e.id] = e; });
      setEquipMap(m);

      const userIds = [...new Set(bks.map((b) => b.userId))];
      const sm = {};
      await Promise.all(userIds.map(async (id) => {
        try {
          const { data } = await userApi.getById(id);
          sm[id] = data;
        } catch {}
      }));
      setStudentMap(sm);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const submitReview = async (note) => {
    try {
      if (action === 'approve') {
        await bookingApi.approve(reviewing.id, me.id, note);
      } else {
        await bookingApi.reject(reviewing.id, me.id, note);
      }
      setReviewing(null); setAction(null);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Pending Bookings</h1>
      <p className="page-sub">Review student equipment booking requests and approve or reject them.</p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">✅</div>No pending bookings — all caught up!</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Student</th>
                  <th>Equipment</th>
                  <th>Time</th>
                  <th>Purpose</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((b) => (
                  <tr key={b.id}>
                    <td>{b.id}</td>
                    <td>
                      {studentMap[b.userId]?.fullName || `User #${b.userId}`}
                      <div style={{ fontSize: 12, color: 'var(--muted)' }}>
                        {studentMap[b.userId]?.indexNumber}
                      </div>
                    </td>
                    <td>{equipMap[b.equipmentId]?.name || `#${b.equipmentId}`}</td>
                    <td>{fmt(b.startTime)}<br /><small>to {fmt(b.endTime)}</small></td>
                    <td>{b.purpose || '—'}</td>
                    <td style={{ display: 'flex', gap: 8 }}>
                      <button className="btn btn-success btn-sm"
                              onClick={() => { setReviewing(b); setAction('approve'); }}>
                        Approve
                      </button>
                      <button className="btn btn-danger btn-sm"
                              onClick={() => { setReviewing(b); setAction('reject'); }}>
                        Reject
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {reviewing && (
        <ReviewModal
          booking={reviewing}
          action={action}
          onClose={() => { setReviewing(null); setAction(null); }}
          onSubmit={submitReview}
        />
      )}
    </div>
  );
}

function ReviewModal({ booking, action, onClose, onSubmit }) {
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const isApprove = action === 'approve';

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    await onSubmit(note);
    setBusy(false);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isApprove ? 'Approve' : 'Reject'} Booking #{booking.id}</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label>Note {isApprove ? '(optional)' : '(reason — visible to student)'}</label>
            <textarea rows="3" value={note} onChange={(e) => setNote(e.target.value)}
                      placeholder={isApprove ? 'e.g., Approved for lab session' : 'e.g., Equipment reserved for staff'} />
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

function fmt(dt) { return new Date(dt).toLocaleString(); }
