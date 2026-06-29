import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { bookingApi, itemApi, labApi, errMsg } from '../../api';
import Badge from '../../components/Badge';
import { byId, fmt } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

const ACTIVE_UMBRELLA = new Set([
  'SUBMITTED', 'AWAITING_HANDLER',
  'READY_FOR_COLLECTION', 'COLLECTED', 'OVERDUE',
]);

const TERMINAL_LINE = new Set([
  'REJECTED', 'RETURNED', 'CANCELLED',
]);

export default function MyBookings() {
  const location = useLocation();
  const [flash, setFlash] = useState(location.state?.flash || '');
  const [bookings, setBookings] = useState([]);
  const [itemMap, setItemMap] = useState({});
  const [labMap, setLabMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [{ data: bks }, { data: items }, { data: labs }] = await Promise.all([
        bookingApi.mine(), itemApi.list(), labApi.list(),
      ]);
      if (isCancelled?.()) return;
      setBookings(bks);
      setItemMap(byId(items));
      setLabMap(byId(labs));
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  useEffect(() => {
    if (!flash) return;
    const t = setTimeout(() => setFlash(''), 6000);
    return () => clearTimeout(t);
  }, [flash]);

  const cancel = async (id) => {
    if (!confirm("Cancel this booking? Items already collected can't be cancelled.")) return;
    try { await bookingApi.cancel(id); load(); }
    catch (err) { alert(errMsg(err)); }
  };

  return (
    <div className="container">
      <h1 className="page-title">My Bookings</h1>
      <p className="page-sub">Each booking can include multiple items — every line tracks its own approval and pickup state.</p>

      {flash && <div className="alert alert-success">{flash}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        bookings.length === 0 ? (
          <div className="empty"><div className="empty-icon">📅</div>No bookings yet. Browse equipment to make your first booking.</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {bookings.map((b) => (
              <BookingCard key={b.id} booking={b}
                           itemMap={itemMap} labMap={labMap}
                           onCancel={() => cancel(b.id)} />
            ))}
          </div>
        )
      )}
    </div>
  );
}

function BookingCard({ booking, itemMap, labMap, onCancel }) {
  const canCancel = ACTIVE_UMBRELLA.has(booking.state)
                    && booking.items.some((it) => !TERMINAL_LINE.has(it.state)
                                                  && it.state !== 'COLLECTED'
                                                  && it.state !== 'OVERDUE');
  return (
    <div className="card" style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Booking #{booking.id}</div>
          <h3 style={{ margin: '4px 0' }}>{booking.projectName}</h3>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>
            {fmt(booking.startDate)} → {fmt(booking.returnDate)}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Badge value={booking.state} />
          {canCancel && (
            <button className="btn btn-danger btn-sm" onClick={onCancel}>Cancel</button>
          )}
        </div>
      </div>

      <div className="table-wrap" style={{ marginTop: 12 }}>
        <table>
          <thead>
            <tr><th>Item</th><th>Lab</th><th>State</th><th>Pickup</th></tr>
          </thead>
          <tbody>
            {booking.items.map((line) => {
              const it = itemMap[line.itemId];
              const lb = labMap[line.labId];
              return (
                <tr key={line.id}>
                  <td>
                    {it ? it.name : `Item #${line.itemId}`}
                    <div style={{ fontSize: 12, color: 'var(--muted)' }}>{it?.model}</div>
                  </td>
                  <td>{lb ? lb.name : `Lab #${line.labId}`}</td>
                  <td><Badge value={line.state} /></td>
                  <td>
                    {line.pickupAt ? fmt(line.pickupAt) : '—'}
                    {line.pickupNote && (
                      <div style={{ fontSize: 12, color: 'var(--muted)' }}>{line.pickupNote}</div>
                    )}
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

