import { useEffect, useRef, useState } from 'react';
import { chatApi, errMsg } from '../api';
import { useAsyncEffect } from '../hooks/useAsyncEffect';

/**
 * Student ↔ instructor chat. The left pane lists my threads (one opens
 * automatically once an instructor approves a request); the right pane is the
 * live chatbox. Messages are polled every few seconds so a reply shows up
 * without a manual refresh — matching the rest of the app's polling style.
 */
export default function Chat() {
  const [conversations, setConversations] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [loadingConvs, setLoadingConvs] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editDraft, setEditDraft] = useState('');
  const [pendingDelete, setPendingDelete] = useState(null);
  const scrollRef = useRef(null);

  const active = conversations.find((c) => c.id === activeId) || null;

  const loadConversations = async () => {
    try {
      const { data } = await chatApi.listMine();
      setConversations(data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setLoadingConvs(false);
    }
  };

  const loadMessages = async (id) => {
    if (!id) return;
    try {
      const { data } = await chatApi.messages(id);
      setMessages(data);
    } catch (err) {
      setError(errMsg(err));
    }
  };

  // Initial thread-list load (cancellation-aware, matching the app's data-loading style).
  useAsyncEffect(async (cancelled) => {
    try {
      const { data } = await chatApi.listMine();
      if (cancelled()) return;
      setConversations(data);
    } catch (err) {
      if (!cancelled()) setError(errMsg(err));
    } finally {
      if (!cancelled()) setLoadingConvs(false);
    }
  }, []);

  // Refresh the thread list periodically for new-message previews + unread badges.
  useEffect(() => {
    const t = setInterval(loadConversations, 10000);
    return () => clearInterval(t);
  }, []);

  // While a thread is open, poll its messages every 3s (the first load happens on click).
  useEffect(() => {
    if (!activeId) return;
    const t = setInterval(() => loadMessages(activeId), 3000);
    return () => clearInterval(t);
  }, [activeId]);

  // Keep the view pinned to the newest message.
  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, activeId]);

  const openThread = (id) => {
    setActiveId(id);
    setMessages([]);          // drop the previous thread's messages while the new one loads
    loadMessages(id);         // fetch immediately so there's no 3s wait on open
    // Clear its unread badge optimistically; the server marks it read on fetch.
    setConversations((prev) => prev.map((c) => (c.id === id ? { ...c, unreadCount: 0 } : c)));
  };

  const send = async (e) => {
    e.preventDefault();
    const body = draft.trim();
    if (!body || sending || !activeId) return;
    setSending(true);
    setError('');
    try {
      const { data } = await chatApi.send(activeId, body);
      setMessages((prev) => [...prev, data]);
      setDraft('');
      loadConversations();
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setSending(false);
    }
  };

  const startEdit = (m) => { setEditingId(m.id); setEditDraft(m.body); };
  const cancelEdit = () => { setEditingId(null); setEditDraft(''); };

  const saveEdit = async (e) => {
    e.preventDefault();
    const body = editDraft.trim();
    if (!body) return;
    setError('');
    try {
      const { data } = await chatApi.edit(activeId, editingId, body);
      setMessages((prev) => prev.map((x) => (x.id === data.id ? data : x)));
      cancelEdit();
      loadConversations();
    } catch (err) {
      setError(errMsg(err));
    }
  };

  // Delete is confirmed via an in-page modal (no browser popups).
  const confirmDelete = async () => {
    const m = pendingDelete;
    if (!m) return;
    setError('');
    try {
      await chatApi.remove(activeId, m.id);
      setMessages((prev) => prev.filter((x) => x.id !== m.id));
      if (editingId === m.id) cancelEdit();
      loadConversations();
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setPendingDelete(null);
    }
  };

  return (
    <div className="container">
      <h1 className="page-title">Messages</h1>
      <p className="page-sub">Chat with the instructor handling your approved request.</p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="chat-layout card">
        <aside className="chat-list">
          {loadingConvs ? (
            <div className="loading">Loading…</div>
          ) : conversations.length === 0 ? (
            <div className="chat-empty" style={{ padding: 24 }}>
              <div className="empty-icon">💬</div>
              No conversations yet. A chat opens once an instructor approves your request.
            </div>
          ) : (
            conversations.map((c) => (
              <button
                key={c.id}
                className={`chat-list-item ${c.id === activeId ? 'active' : ''}`}
                onClick={() => openThread(c.id)}
              >
                <div className="chat-list-top">
                  <span className="chat-list-name">{c.otherUserName}</span>
                  {c.unreadCount > 0 && <span className="chat-badge">{c.unreadCount}</span>}
                </div>
                <div className="chat-list-sub">{c.projectName}</div>
                {c.lastMessage && <div className="chat-list-preview">{c.lastMessage}</div>}
              </button>
            ))
          )}
        </aside>

        <section className="chat-main">
          {!active ? (
            <div className="chat-empty">Select a conversation to start chatting.</div>
          ) : (
            <>
              <header className="chat-header">
                <div className="chat-header-name">{active.otherUserName}</div>
                <div className="chat-header-sub">
                  {roleLabel(active.otherUserRole)} · {active.projectName}
                </div>
              </header>

              <div className="chat-messages" ref={scrollRef}>
                {messages.length === 0 ? (
                  <div className="chat-empty">No messages yet. Say hello 👋</div>
                ) : (
                  messages.map((m) => (
                    <div key={m.id} className={`chat-row ${m.mine ? 'mine' : 'theirs'}`}>
                      <div className="chat-bubble">
                        {editingId === m.id ? (
                          <form className="chat-edit" onSubmit={saveEdit}>
                            <input
                              type="text"
                              value={editDraft}
                              maxLength={2000}
                              autoFocus
                              onChange={(e) => setEditDraft(e.target.value)}
                              onKeyDown={(e) => { if (e.key === 'Escape') cancelEdit(); }}
                            />
                            <div className="chat-edit-actions">
                              <button type="button" className="chat-action-btn" onClick={cancelEdit}>Cancel</button>
                              <button type="submit" className="chat-action-btn strong" disabled={!editDraft.trim()}>Save</button>
                            </div>
                          </form>
                        ) : (
                          <>
                            <div className="chat-bubble-body">{m.body}</div>
                            <div className="chat-bubble-time">
                              {new Date(m.createdAt).toLocaleString()}{m.editedAt ? ' · edited' : ''}
                            </div>
                            {m.mine && (
                              <div className="chat-actions">
                                <button className="chat-action-btn" onClick={() => startEdit(m)}>Edit</button>
                                <button className="chat-action-btn" onClick={() => setPendingDelete(m)}>Delete</button>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>

              <form className="chat-input" onSubmit={send}>
                <input
                  type="text"
                  placeholder="Type a message…"
                  value={draft}
                  maxLength={2000}
                  onChange={(e) => setDraft(e.target.value)}
                />
                <button className="btn" type="submit" disabled={sending || !draft.trim()}>
                  {sending ? 'Sending…' : 'Send'}
                </button>
              </form>
            </>
          )}
        </section>
      </div>

      {pendingDelete && (
        <div className="modal-overlay" onClick={() => setPendingDelete(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Delete message?</h2>
            <p style={{ color: 'var(--muted)', margin: '0 0 14px' }}>
              This message will be removed for both of you and can&apos;t be undone.
            </p>
            <blockquote className="chat-delete-preview">{pendingDelete.body}</blockquote>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setPendingDelete(null)}>
                Cancel
              </button>
              <button type="button" className="btn btn-danger" onClick={confirmDelete}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function roleLabel(role) {
  switch (role) {
    case 'STUDENT': return 'Student';
    case 'INSTRUCTOR': return 'Instructor';
    case 'LECTURER': return 'Lecturer';
    case 'HOD': return 'Head of Department';
    default: return 'User';
  }
}
