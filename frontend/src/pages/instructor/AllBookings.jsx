import { useState } from 'react';
import { bookingApi, userApi, errMsg } from '../../api';
import { isAdmin } from '../../auth';
import Badge from '../../components/Badge';
import { fmt } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const STATES = [
  'ALL',
  'SUBMITTED', 'AWAITING_HANDLER',
  'READY_FOR_COLLECTION', 'COLLECTED', 'RETURNED', 'OVERDUE',
  'REJECTED', 'CANCELLED', 'COMPLETED',
];

export default function AllBookings() {
  const admin = isAdmin();

  const [bookings, setBookings] = useState([]);
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');

  useAsyncEffect(async (isCancelled) => {
    try {
      const { data: bks } = admin
        ? await bookingApi.list()
        : await bookingApi.assignedToMe();
      if (isCancelled()) return;
      setBookings(bks);

      const studentIds = [...new Set(bks.map((b) => b.studentUserId))];
      const sm = {};
      await Promise.all(studentIds.map(async (id) => {
        try { const { data } = await userApi.getById(id); sm[id] = data; } catch {}
      }));
      if (!isCancelled()) setStudentMap(sm);
    } catch (err) {
      if (!isCancelled()) setError(errMsg(err));
    } finally {
      if (!isCancelled()) setLoading(false);
    }
  }, []);

  const visible = bookings
    .filter((b) => filter === 'ALL' || b.state === filter)
    .sort((a, b) => b.id - a.id);

  return (
    <div className="container">
      <h1 className="page-title">All Bookings</h1>
      <p className="page-sub">{admin ? 'Booking history scoped to your view.' : 'Bookings touching your labs.'}</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="filter-bar">
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          {STATES.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All states' : s}</option>)}
        </select>
      </div>

      {loading ? <div className="loading">Loading…</div> : (
        visible.length === 0 ? (
          <div className="empty"><div className="empty-icon">📋</div>No bookings.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th><th>Student</th><th>Project</th>
                  <th>Items</th><th>Window</th><th>State</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((b) => {
                  const itemSummary = (b.items || []).map((it) => it.state).reduce((acc, s) => {
                    acc[s] = (acc[s] || 0) + 1; return acc;
                  }, {});
                  return (
                    <tr key={b.id}>
                      <td>{b.id}</td>
                      <td>{studentMap[b.studentUserId]?.fullName || `#${b.studentUserId}`}</td>
                      <td>{b.projectName}</td>
                      <td>
                        <div>{(b.items || []).length} item{(b.items || []).length === 1 ? '' : 's'}</div>
                        <div style={{ fontSize: 12, color: 'var(--muted)' }}>
                          {Object.entries(itemSummary).map(([s, n]) => `${n}× ${s}`).join(', ')}
                        </div>
                      </td>
                      <td>{fmt(b.startDate)}<br /><small>→ {fmt(b.returnDate)}</small></td>
                      <td><Badge value={b.state} /></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  );
}

