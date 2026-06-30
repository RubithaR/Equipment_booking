import { useState } from 'react';
import { bookingApi, userApi, errMsg } from '../../api';
import { isAdmin } from '../../auth';
import Badge from '../../components/Badge';
import { fmt } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

// Lifecycle categories — each tab groups the booking umbrella states that belong together.
// `states: null` means "match everything" (the All tab). `color`/`tint` drive the box accent.
const TAB_GROUPS = [
  { key: 'ALL',         label: 'All',                  states: null,                                            color: '#1e4d8b', tint: '#dbe7f5' },
  { key: 'IN_PROGRESS', label: 'In progress',          states: ['SUBMITTED', 'AWAITING_HANDLER'],               color: '#b8822a', tint: '#f5e4c1' },
  { key: 'IN_USE',      label: 'In use',               states: ['READY_FOR_COLLECTION', 'COLLECTED', 'OVERDUE'], color: '#0891b2', tint: '#d6eef3' },
  { key: 'COMPLETED',   label: 'Completed',            states: ['RETURNED', 'COMPLETED'],                       color: '#1b6e4a', tint: '#cce5d8' },
  { key: 'CLOSED',      label: 'Rejected / Cancelled', states: ['REJECTED', 'CANCELLED'],                       color: '#9b1c1c', tint: '#f5d0d0' },
];

const inTab = (group, state) => group.states === null || group.states.includes(state);

export default function AllBookings() {
  const admin = isAdmin();

  const [bookings, setBookings] = useState([]);
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('ALL');

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

  const activeGroup = TAB_GROUPS.find((g) => g.key === tab) || TAB_GROUPS[0];
  const visible = bookings
    .filter((b) => inTab(activeGroup, b.state))
    .sort((a, b) => b.id - a.id);
  const countFor = (group) => bookings.filter((b) => inTab(group, b.state)).length;

  return (
    <div className="container">
      <h1 className="page-title">All Bookings</h1>
      <p className="page-sub">{admin ? 'Booking history scoped to your view.' : 'Bookings touching your labs.'}</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="cat-tabs">
        {TAB_GROUPS.map((g) => (
          <button key={g.key}
                  type="button"
                  style={{ '--cat': g.color, '--cat-bg': g.tint }}
                  className={`cat-box ${tab === g.key ? 'active' : ''}`}
                  onClick={() => setTab(g.key)}>
            <span className="cat-count">{countFor(g)}</span>
            <span className="cat-label">{g.label}</span>
          </button>
        ))}
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

