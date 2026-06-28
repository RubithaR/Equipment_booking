import { useState } from 'react';
import { notificationApi, errMsg } from '../api';
import { getCurrentUser } from '../auth';
import { useAsyncEffect } from '../hooks/useAsyncEffect';

export default function Notifications() {
  const me = getCurrentUser();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (isCancelled) => {
    try {
      const { data } = await notificationApi.byUser(me.id);
      if (isCancelled?.()) return;
      setItems(data);
    } catch (err) {
      if (isCancelled?.()) return;
      setError(errMsg(err));
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  };

  useAsyncEffect(load, []);

  const markRead = async (id) => {
    try {
      await notificationApi.markRead(id);
      await load();
    } catch (err) {
      alert(errMsg(err));
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Notifications</h1>
      <p className="page-sub">Updates on your bookings, approvals, and system messages.</p>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? <div className="loading">Loading…</div> : (
        items.length === 0 ? (
          <div className="empty"><div className="empty-icon">🔔</div>No notifications yet.</div>
        ) : (
          <div>
            {items.map((n) => (
              <div key={n.id} className={`notif-item t-${n.type} ${!n.read ? 'unread' : ''}`}>
                <div className="notif-title">
                  <span>{n.title}</span>
                  <span className="notif-time">{new Date(n.createdAt).toLocaleString()}</span>
                </div>
                <div className="notif-msg">{n.message}</div>
                {!n.read && (
                  <button className="btn btn-secondary btn-sm" style={{ marginTop: 8 }}
                          onClick={() => markRead(n.id)}>Mark read</button>
                )}
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
}
