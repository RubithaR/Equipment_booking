import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { itemApi, labApi, errMsg } from '../../api';
import Badge from '../../components/Badge';
import { byId } from '../../utils/format';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

export default function AdminEquipment() {
  const location = useLocation();
  const [items, setItems] = useState([]);
  const [labs, setLabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [flash, setFlash] = useState(location.state?.flash || '');

  const load = async (isCancelled) => {
    setLoading(true);
    try {
      const [itemsRes, labsRes] = await Promise.all([itemApi.list(), labApi.list()]);
      if (isCancelled?.()) return;
      setItems(itemsRes.data);
      setLabs(labsRes.data);
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
    const t = setTimeout(() => setFlash(''), 5000);
    return () => clearTimeout(t);
  }, [flash]);

  const labLookup = byId(labs);

  const remove = async (item) => {
    if (!confirm(`Delete ${item.name}?`)) return;
    try {
      await itemApi.remove(item.id);
      load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">Items</h1>
          <p className="page-sub">All items across labs. Add new items to the catalogue here.</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Link to="/admin/labs" className="btn btn-secondary">Manage Labs</Link>
          <Link to="/admin/equipment/new" className="btn">+ Add Item</Link>
        </div>
      </div>

      {flash && <div className="alert alert-success">{flash}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">🧪</div>No items yet.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th><th>Name</th><th>Model</th><th>Category</th>
                  <th>Lab</th><th>Serial</th><th>Usage</th><th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {[...items].sort((a, b) => a.id - b.id).map((e, i) => {
                  const lab = labLookup[e.labId];
                  return (
                    <tr key={e.id}>
                      <td>{i + 1}</td>
                      <td>{e.name}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{e.description}</div></td>
                      <td>{e.model}</td>
                      <td>{e.category}</td>
                      <td>{lab ? `${lab.name}${lab.location ? ` · ${lab.location}` : ''}` : `Lab #${e.labId}`}</td>
                      <td>{e.serialNumber || '—'}</td>
                      <td>
                        <span className={`eq-usage ${(e.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY' ? 'eq-usage-lab' : 'eq-usage-borrow'}`}>
                          {(e.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY' ? 'Lab only' : 'Borrowable'}
                        </span>
                      </td>
                      <td><Badge value={e.status} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <Link to={`/admin/equipment/${e.id}/edit`} className="btn btn-secondary btn-sm">Edit</Link>
                          <button className="btn btn-danger btn-sm" onClick={() => remove(e)}>Delete</button>
                        </div>
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
