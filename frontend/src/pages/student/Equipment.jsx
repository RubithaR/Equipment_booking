import { useEffect, useState } from 'react';
import { equipmentApi, bookingApi, errMsg } from '../../api';
import { getCurrentUser } from '../../auth';

export default function Equipment() {
  const me = getCurrentUser();
  const [items, setItems] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [bookingFor, setBookingFor] = useState(null);
  const [bookingMsg, setBookingMsg] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await equipmentApi.list();
      setItems(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = items.filter((e) => {
    if (filter !== 'ALL' && e.status !== filter) return false;
    if (search && !(`${e.name} ${e.category} ${e.location}`.toLowerCase().includes(search.toLowerCase()))) return false;
    return true;
  });

  return (
    <div className="container">
      <h1 className="page-title">Browse Equipment</h1>
      <p className="page-sub">Pick equipment, choose your time slot — your booking will be sent to an instructor for approval.</p>

      {bookingMsg && <div className="alert alert-success">{bookingMsg}</div>}

      <div className="filter-bar">
        <input placeholder="Search by name, category or location…" value={search}
               onChange={(e) => setSearch(e.target.value)} style={{ flex: 1, minWidth: 200 }} />
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="AVAILABLE">Available</option>
          <option value="IN_USE">In use</option>
          <option value="MAINTENANCE">Maintenance</option>
        </select>
      </div>

      {loading ? <div className="loading">Loading…</div> : (
        filtered.length === 0 ? (
          <div className="empty"><div className="empty-icon">🔍</div>No equipment matches your filter.</div>
        ) : (
          <div className="equip-grid">
            {filtered.map((e) => {
              const status = (e.status || 'AVAILABLE').toUpperCase();
              const statusClass = status === 'AVAILABLE' ? 'eq-status-ok'
                                : status === 'IN_USE' ? 'eq-status-busy'
                                : 'eq-status-warn';
              const statusLabel = status === 'IN_USE' ? 'In use'
                                : status === 'MAINTENANCE' ? 'Maintenance'
                                : 'Available';
              return (
                <article key={e.id} className="eq-card">
                  <div className="eq-card-row">
                    <span className="eq-cat">{e.category || 'General'}</span>
                    <span className={`eq-status ${statusClass}`}>{statusLabel}</span>
                  </div>
                  <h3 className="eq-title">{e.name}</h3>
                  <div className="eq-meta">
                    {(e.location || 'Lab')}{e.description ? ` · ${e.description}` : ''}
                  </div>
                  <div className="eq-foot">
                    <div>
                      <div className="eq-foot-label">Next slot</div>
                      <div className="eq-foot-value">
                        {status === 'AVAILABLE' ? 'Open now' : status === 'IN_USE' ? 'Busy today' : 'Unavailable'}
                      </div>
                    </div>
                    <button
                      className="btn-pill btn-pill-solid btn-pill-sm"
                      onClick={() => setBookingFor(e)}
                    >
                      Book now
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )
      )}

      {bookingFor && (
        <BookingModal
          equipment={bookingFor}
          userId={me.id}
          onClose={() => setBookingFor(null)}
          onSuccess={(msg) => {
            setBookingFor(null);
            setBookingMsg(msg);
            setTimeout(() => setBookingMsg(''), 5000);
            load();
          }}
        />
      )}
    </div>
  );
}

const pad = (n) => String(n).padStart(2, '0');
const toLocalInput = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
const fmtNice = (d) => d.toLocaleString(undefined, {
  weekday: 'short', month: 'short', day: 'numeric',
  hour: 'numeric', minute: '2-digit',
});

function BookingModal({ equipment, userId, onClose, onSuccess }) {
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [purpose, setPurpose] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [blocked, setBlocked] = useState([]);
  const [now, setNow] = useState(new Date());

  const nowMin = toLocalInput(now);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await bookingApi.list();
        const mine = data
          .filter((b) => b.equipmentId === equipment.id)
          .filter((b) => !['REJECTED', 'CANCELLED'].includes((b.status || '').toUpperCase()))
          .map((b) => ({
            id: b.id,
            start: new Date(b.startTime),
            end: new Date(b.endTime),
            status: (b.status || 'PENDING_APPROVAL').toUpperCase(),
          }))
          .filter((b) => b.end >= new Date())
          .sort((a, b) => a.start - b.start);
        setBlocked(mine);
      } catch {
        setBlocked([]);
      }
    })();
  }, [equipment.id]);

  const overlaps = (s, e) => blocked.some((b) => s < b.end && e > b.start);

  const submit = async (ev) => {
    ev.preventDefault();
    setError('');
    const s = new Date(startTime);
    const e = new Date(endTime);
    if (!(s instanceof Date) || isNaN(s) || !(e instanceof Date) || isNaN(e)) {
      setError('Please pick valid start and end times.'); return;
    }
    if (e <= s) { setError('End time must be after start time.'); return; }
    if (s < new Date(Date.now() - 60_000)) { setError('Start time cannot be in the past.'); return; }
    const clash = blocked.find((b) => s < b.end && e > b.start);
    if (clash) {
      setError(`This slot clashes with an existing booking (${fmtNice(clash.start)} → ${fmtNice(clash.end)}). Pick a free time.`);
      return;
    }
    setBusy(true);
    try {
      const { data } = await bookingApi.create({
        userId, equipmentId: equipment.id,
        startTime, endTime, purpose,
      });
      onSuccess(`Booking #${data.id} submitted — awaiting instructor approval.`);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  const liveClash = startTime && endTime
    ? overlaps(new Date(startTime), new Date(endTime))
    : false;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Book {equipment.name}</h2>

        <div className="booking-now">
          <span className="booking-now-dot" />
          <span className="booking-now-label">Now</span>
          <span className="booking-now-value">{fmtNice(now)}</span>
        </div>

        <div className="booked-list">
          <div className="booked-list-head">
            <span>Already booked</span>
            <span className="booked-list-count">{blocked.length}</span>
          </div>
          {blocked.length === 0 ? (
            <div className="booked-empty">No upcoming bookings — this equipment is wide open.</div>
          ) : (
            <ul className="booked-rows">
              {blocked.map((b) => (
                <li key={b.id} className="booked-row">
                  <span className={`booked-dot booked-dot-${b.status === 'CONFIRMED' ? 'busy' : 'pend'}`} />
                  <span className="booked-range">{fmtNice(b.start)} → {fmtNice(b.end)}</span>
                  <span className={`booked-pill ${b.status === 'CONFIRMED' ? 'booked-pill-busy' : 'booked-pill-pend'}`}>
                    {b.status === 'CONFIRMED' ? 'Confirmed' : 'Pending'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {!error && liveClash && (
          <div className="alert alert-error">That range overlaps a booked slot above. Pick a free time.</div>
        )}

        <form onSubmit={submit}>
          <div className="field-row">
            <div className="field">
              <label>Start Time</label>
              <input type="datetime-local" value={startTime} required min={nowMin}
                     onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div className="field">
              <label>End Time</label>
              <input type="datetime-local" value={endTime} required min={startTime || nowMin}
                     onChange={(e) => setEndTime(e.target.value)} />
            </div>
          </div>
          <div className="field">
            <label>Purpose</label>
            <textarea rows="3" value={purpose}
                      onChange={(e) => setPurpose(e.target.value)}
                      placeholder="What will you use it for?" />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn" disabled={busy || liveClash}>
              {busy ? 'Submitting…' : 'Submit Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
