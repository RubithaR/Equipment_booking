import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { bookingApi, itemApi, labApi } from '../../api';
import { addToCart, isInCart, removeFromCart, subscribeCart } from '../../cart';
import { byId, fmt } from '../../utils/format';

export default function Equipment() {
  const [items, setItems] = useState([]);
  const [labs, setLabs] = useState([]);
  const [availMap, setAvailMap] = useState({});   // itemId -> { status, bookedUntil }
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [, setCartTick] = useState(0);   // re-render when cart changes

  useEffect(() => subscribeCart(() => setCartTick((n) => n + 1)), []);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [itemsRes, labsRes] = await Promise.all([itemApi.list(), labApi.list()]);
        setItems(itemsRes.data);
        setLabs(labsRes.data);

        // Overlay booking-derived availability (in process / in use + booked-until date).
        const ids = itemsRes.data.map((i) => i.id);
        if (ids.length) {
          try {
            const { data } = await bookingApi.availability(ids);
            setAvailMap(byId(data.map((a) => ({ ...a, id: a.itemId }))));
          } catch { /* availability is best-effort; catalogue still works without it */ }
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const labLookup = byId(labs);
  const labText = (labId) => {
    const l = labLookup[labId];
    if (!l) return 'Lab';
    return l.location ? `${l.name} · ${l.location}` : l.name;
  };

  // Effective status shown to students: admin block (maintenance/out of service)
  // wins; otherwise the booking-derived bucket (available / in process / in use).
  const bucketOf = (e) => {
    const adminStatus = (e.status || 'AVAILABLE').toUpperCase();
    if (adminStatus === 'MAINTENANCE' || adminStatus === 'OUT_OF_SERVICE') return adminStatus;
    return availMap[e.id]?.status || 'AVAILABLE';
  };

  const filtered = items.filter((e) => {
    if (filter !== 'ALL' && bucketOf(e) !== filter) return false;
    if (search) {
      const lab = labLookup[e.labId];
      const haystack = `${e.name} ${e.model || ''} ${e.category} ${lab?.name || ''} ${lab?.location || ''}`.toLowerCase();
      if (!haystack.includes(search.toLowerCase())) return false;
    }
    return true;
  });

  return (
    <div className="container">
      <h1 className="page-title">Browse Equipment</h1>
      <p className="page-sub">Add multiple items to your cart, then submit one booking — each lab's instructor reviews their own items.</p>

      <div className="filter-bar">
        <input placeholder="Search by name, model, category or lab…" value={search}
               onChange={(e) => setSearch(e.target.value)} style={{ flex: 1, minWidth: 200 }} />
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="AVAILABLE">Available</option>
          <option value="IN_PROCESS">In process</option>
          <option value="IN_USE">In use</option>
          <option value="MAINTENANCE">Maintenance</option>
          <option value="OUT_OF_SERVICE">Out of service</option>
        </select>
      </div>

      {loading ? <div className="loading">Loading…</div> : (
        filtered.length === 0 ? (
          <div className="empty"><div className="empty-icon">🔍</div>No equipment matches your filter.</div>
        ) : (
          <div className="equip-grid">
            {filtered.map((e) => {
              const adminStatus = (e.status || 'AVAILABLE').toUpperCase();
              const blocked = adminStatus === 'MAINTENANCE' || adminStatus === 'OUT_OF_SERVICE';
              // Booking-derived bucket (in process / in use) takes over once the item is bookable.
              const bucket = bucketOf(e);
              const bookedUntil = availMap[e.id]?.bookedUntil;

              const statusClass = bucket === 'AVAILABLE' ? 'eq-status-ok'
                                : (bucket === 'IN_USE' || bucket === 'IN_PROCESS') ? 'eq-status-busy'
                                : 'eq-status-warn';
              const statusLabel = bucket === 'IN_USE' ? 'In use'
                                : bucket === 'IN_PROCESS' ? 'In process'
                                : bucket === 'MAINTENANCE' ? 'Maintenance'
                                : bucket === 'OUT_OF_SERVICE' ? 'Out of service'
                                : 'Available';
              const lab = labLookup[e.labId];
              // Booked items stay bookable — students just pick a date after bookedUntil.
              const bookable = !blocked && lab?.instructorUserId;
              const inCart = isInCart(e.id);
              return (
                <article key={e.id} className="eq-card">
                  <div className="eq-card-row">
                    <span className="eq-cat">{e.category || 'General'}</span>
                    <span className={`eq-status ${statusClass}`}>{statusLabel}</span>
                  </div>
                  <h3 className="eq-title">{e.name}</h3>
                  <div className="eq-meta">
                    {e.model ? <strong>{e.model}</strong> : null}
                    {e.model ? ' · ' : ''}{labText(e.labId)}
                    {e.description ? ` · ${e.description}` : ''}
                  </div>
                  {e.instructorNames && e.instructorNames.length > 0 && (
                    <div className="eq-instructors">
                      <span className="eq-instructors-label">Instructor{e.instructorNames.length > 1 ? 's' : ''}:</span>{' '}
                      {e.instructorNames.join(', ')}
                    </div>
                  )}
                  <div className="eq-foot">
                    <div>
                      <div className="eq-foot-label">Status</div>
                      <div className="eq-foot-value">{statusLabel}</div>
                      {bookedUntil && (bucket === 'IN_USE' || bucket === 'IN_PROCESS') && (
                        <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
                          Booked until {fmt(bookedUntil)} — pick a later start.
                        </div>
                      )}
                    </div>
                    {!bookable ? (
                      <button className="btn-pill btn-pill-ghost btn-pill-sm" disabled>
                        {lab && !lab.instructorUserId ? 'No instructor' : 'Unavailable'}
                      </button>
                    ) : inCart ? (
                      <button className="btn-pill btn-pill-ghost btn-pill-sm"
                              onClick={() => removeFromCart(e.id)}>
                        Remove from cart
                      </button>
                    ) : (
                      <button className="btn-pill btn-pill-solid btn-pill-sm"
                              onClick={() => addToCart({
                                itemId: e.id, labId: e.labId,
                                name: e.name, model: e.model,
                                labName: lab?.name,
                              })}>
                        Add to cart
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )
      )}

      <div style={{ position: 'sticky', bottom: 16, marginTop: 24, textAlign: 'right' }}>
        <Link to="/student/cart" className="btn-pill btn-pill-solid btn-pill-lg">
          Review cart →
        </Link>
      </div>
    </div>
  );
}
