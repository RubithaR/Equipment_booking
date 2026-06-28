import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { itemApi, labApi, errMsg } from '../../api';
import Badge from '../../components/Badge';
import { byId } from '../../utils/format';

export default function AdminEquipment() {
  const [items, setItems] = useState([]);
  const [labs, setLabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [itemsRes, labsRes] = await Promise.all([itemApi.list(), labApi.list()]);
      setItems(itemsRes.data);
      setLabs(labsRes.data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

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
          <p className="page-sub">All items across labs. Add/edit items from the instructor catalogue page.</p>
        </div>
        <Link to="/admin/labs" className="btn">Manage Labs</Link>
      </div>

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
                  <th>Lab</th><th>Serial</th><th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((e) => {
                  const lab = labLookup[e.labId];
                  return (
                    <tr key={e.id}>
                      <td>{e.id}</td>
                      <td>{e.name}<div style={{ fontSize: 12, color: 'var(--muted)' }}>{e.description}</div></td>
                      <td>{e.model}</td>
                      <td>{e.category}</td>
                      <td>{lab ? `${lab.name}${lab.location ? ` · ${lab.location}` : ''}` : `Lab #${e.labId}`}</td>
                      <td>{e.serialNumber || '—'}</td>
                      <td><Badge value={e.status} /></td>
                      <td>
                        <button className="btn btn-danger btn-sm" onClick={() => remove(e)}>Delete</button>
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
