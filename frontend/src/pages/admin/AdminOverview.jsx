import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { userApi, equipmentApi, bookingApi, errMsg } from '../../api';

export default function AdminOverview() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [students, instructors, pending, equipment, bookings] = await Promise.all([
          userApi.getByRole('STUDENT'),
          userApi.getByRole('INSTRUCTOR'),
          userApi.getPendingInstructors(),
          equipmentApi.list(),
          bookingApi.list(),
        ]);
        setStats({
          students: students.data.length,
          instructors: instructors.data.filter((i) => i.status === 'ACTIVE').length,
          pendingInstructors: pending.data.length,
          equipment: equipment.data.length,
          bookings: bookings.data.length,
          pendingBookings: bookings.data.filter((b) => b.status === 'PENDING_APPROVAL').length,
          confirmedBookings: bookings.data.filter((b) => b.status === 'CONFIRMED').length,
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
            <Stat label="Equipment" val={stats.equipment} />
            <Stat label="Total bookings" val={stats.bookings} />
            <Stat label="Pending bookings" val={stats.pendingBookings} />
            <Stat label="Confirmed bookings" val={stats.confirmedBookings} />
          </div>

          <div className="card">
            <h3 style={{ marginTop: 0 }}>Quick links</h3>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <Link to="/admin/instructors-pending" className="btn">Review pending instructors</Link>
              <Link to="/admin/users" className="btn btn-secondary">Manage users</Link>
              <Link to="/admin/equipment" className="btn btn-secondary">Manage equipment</Link>
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
