import { useEffect, useState } from 'react';
import { bookingApi, equipmentApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';
import Badge from '../../components/Badge';

export default function MyBookings() {
  const me = getCurrentUser();
  const [bookings, setBookings] = useState([]);
  const [equipMap, setEquipMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [{ data: bks }, { data: equips }] = await Promise.all([
        bookingApi.byUser(me.id),
        equipmentApi.list(),
      ]);
      setBookings(bks);
      const m = {};
      equips.forEach((e) => { m[e.id] = e; });
      setEquipMap(m);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const cancel = async (id) => {
    if (!confirm('Cancel this booking?')) return;
    try {
      await bookingApi.cancel(id);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">My Bookings</h1>
      <p className="page-sub">Track your equipment booking requests and their status.</p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        bookings.length === 0 ? (
          <div className="empty"><div className="empty-icon">📅</div>No bookings yet. Browse equipment to make your first booking.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Equipment</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Purpose</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {bookings.sort((a, b) => b.id - a.id).map((b) => {
                  const eq = equipMap[b.equipmentId];
                  const canCancel = b.status === 'PENDING_APPROVAL' || b.status === 'CONFIRMED';
                  return (
                    <tr key={b.id}>
                      <td>{b.id}</td>
                      <td>{eq ? eq.name : `Equipment #${b.equipmentId}`}</td>
                      <td>{fmt(b.startTime)}</td>
                      <td>{fmt(b.endTime)}</td>
                      <td>{b.purpose || '—'}</td>
                      <td><Badge value={b.status} /></td>
                      <td>
                        {canCancel && (
                          <button className="btn btn-danger btn-sm" onClick={() => cancel(b.id)}>Cancel</button>
                        )}
                        {b.reviewNote && (
                          <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>
                            Note: {b.reviewNote}
                          </div>
                        )}
                      </td>
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

function fmt(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleString();
}
