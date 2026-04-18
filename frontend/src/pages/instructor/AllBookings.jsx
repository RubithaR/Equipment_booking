import { useEffect, useState } from 'react';
import { bookingApi, equipmentApi, userApi, errMsg } from '../../api';
import Badge from '../../components/Badge';

export default function AllBookings() {
  const [items, setItems] = useState([]);
  const [equipMap, setEquipMap] = useState({});
  const [studentMap, setStudentMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');

  const load = async () => {
    setLoading(true);
    try {
      const { data: bks } = await bookingApi.list();
      setItems(bks);

      const { data: equips } = await equipmentApi.list();
      const em = {}; equips.forEach((e) => { em[e.id] = e; });
      setEquipMap(em);

      const userIds = [...new Set(bks.map((b) => b.userId))];
      const sm = {};
      await Promise.all(userIds.map(async (id) => {
        try { const { data } = await userApi.getById(id); sm[id] = data; } catch {}
      }));
      setStudentMap(sm);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const visible = items.filter((b) => filter === 'ALL' || b.status === filter)
                       .sort((a, b) => b.id - a.id);

  return (
    <div className="container">
      <h1 className="page-title">All Bookings</h1>
      <p className="page-sub">Full booking history across all students.</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="filter-bar">
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="PENDING_APPROVAL">Pending</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="REJECTED">Rejected</option>
          <option value="CANCELLED">Cancelled</option>
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
                  <th>#</th><th>Student</th><th>Equipment</th><th>Start</th><th>End</th><th>Status</th><th>Purpose</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((b) => (
                  <tr key={b.id}>
                    <td>{b.id}</td>
                    <td>{studentMap[b.userId]?.fullName || `#${b.userId}`}</td>
                    <td>{equipMap[b.equipmentId]?.name || `#${b.equipmentId}`}</td>
                    <td>{fmt(b.startTime)}</td>
                    <td>{fmt(b.endTime)}</td>
                    <td><Badge value={b.status} /></td>
                    <td>{b.purpose || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  );
}

function fmt(dt) { return new Date(dt).toLocaleString(); }
