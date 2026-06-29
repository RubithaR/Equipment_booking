import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { userApi, itemApi, labApi, bookingApi, errMsg } from '../../api';

export default function AdminOverview() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [students, instructors, pending, items, labs, bookings] = await Promise.all([
          userApi.getByRole('STUDENT'),
          userApi.getByRole('INSTRUCTOR'),
          userApi.getPendingInstructors(),
          itemApi.list(),
          labApi.list(),
          bookingApi.list(),
        ]);
        const ACTIVE = new Set([
          'SUBMITTED', 'AWAITING_HANDLER', 'READY_FOR_COLLECTION', 'COLLECTED',
        ]);
        setStats({
          students: students.data.length,
          instructors: instructors.data.filter((i) => i.status === 'ACTIVE').length,
          pendingInstructors: pending.data.length,
          labs: labs.data.length,
          items: items.data.length,
          bookings: bookings.data.length,
          pendingBookings: bookings.data.filter((b) =>
            b.state === 'SUBMITTED' || b.state === 'AWAITING_HANDLER').length,
          activeBookings: bookings.data.filter((b) => ACTIVE.has(b.state)).length,
        });
      } catch (err) {
        setError(errMsg(err));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div className="container">
      <h1 className="page-title">Admin Overview</h1>
      <p className="page-sub">System-wide stats and quick links.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <div className="loading">Loading…</div> : stats && (
        <>
          <div className="stat-grid">
            <Stat label="Students" val={stats.students} />
            <Stat label="Active instructors" val={stats.instructors} />
            <Stat label="Pending instructors" val={stats.pendingInstructors} accent={stats.pendingInstructors > 0} />
            <Stat label="Labs" val={stats.labs} />
            <Stat label="Items" val={stats.items} />
            <Stat label="Total bookings" val={stats.bookings} />
            <Stat label="Awaiting review" val={stats.pendingBookings} accent={stats.pendingBookings > 0} />
            <Stat label="Active bookings" val={stats.activeBookings} />
          </div>

          <div className="card">
            <h3 style={{ marginTop: 0 }}>Quick links</h3>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <Link to="/admin/instructors-pending" className="btn">Review pending instructors</Link>
              <Link to="/admin/labs" className="btn btn-secondary">Manage labs</Link>
              <Link to="/admin/equipment" className="btn btn-secondary">View items</Link>
              <Link to="/admin/users" className="btn btn-secondary">Manage users</Link>
              <Link to="/admin/bookings" className="btn btn-secondary">View all bookings</Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, val, accent }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className="stat-val" style={accent ? { color: 'var(--warning)' } : {}}>{val}</div>
    </div>
  );
}
