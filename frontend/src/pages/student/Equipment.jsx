import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { itemApi, labApi } from '../../api';
import { addToCart, isInCart, removeFromCart, subscribeCart } from '../../cart';
import { byId } from '../../utils/format';

export default function Equipment() {
  const [items, setItems] = useState([]);
  const [labs, setLabs] = useState([]);
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

  const filtered = items.filter((e) => {
    if (filter !== 'ALL' && e.status !== filter) return false;
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
              const status = (e.status || 'AVAILABLE').toUpperCase();
              const statusClass = status === 'AVAILABLE' ? 'eq-status-ok'
                                : status === 'IN_USE' ? 'eq-status-busy'
                                : 'eq-status-warn';
              const statusLabel = status === 'IN_USE' ? 'In use'
                                : status === 'MAINTENANCE' ? 'Maintenance'
                                : status === 'OUT_OF_SERVICE' ? 'Out of service'
                                : 'Available';
              const lab = labLookup[e.labId];
              const bookable = (status === 'AVAILABLE' || status === 'IN_USE')
                               && lab?.instructorUserId;
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
                  <div className="eq-foot">
                    <div>
                      <div className="eq-foot-label">Status</div>
                      <div className="eq-foot-value">{statusLabel}</div>
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
