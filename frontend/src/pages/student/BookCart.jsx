import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { bookingApi, errMsg, labApi, userApi } from '../../api';
import { getCurrentUser } from '../../auth';
import { clearCart, getCart, removeFromCart, subscribeCart } from '../../cart';
import { useAsyncEffect } from '../../hooks/useAsyncEffect';

export default function BookCart() {
  const me = getCurrentUser();
  const nav = useNavigate();

  const [cart, setCart] = useState(getCart());
  const [labMap, setLabMap] = useState({});                  // labId -> { name, location, instructorUserId }
  const [instructorMap, setInstructorMap] = useState({});    // userId -> UserResponse
  const [useTimes, setUseTimes] = useState({});              // itemId -> requested lab-use time (LAB_ONLY only)

  // Form fields shared across the whole booking.
  const [projectName, setProjectName] = useState('');
  const [purpose, setPurpose] = useState('');
  const [startDate, setStartDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const [attachments, setAttachments] = useState([]);

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => subscribeCart(setCart), []);

  useAsyncEffect(async (isCancelled) => {
    const labIds = [...new Set(cart.map((c) => c.labId))];
    if (labIds.length === 0) {
      setLabMap({});
      setInstructorMap({});
      return;
    }
    const lm = {};
    await Promise.all(labIds.map(async (id) => {
      try { const { data } = await labApi.get(id); lm[id] = data; } catch {}
    }));
    if (isCancelled()) return;
    setLabMap(lm);
    const insIds = [...new Set(Object.values(lm).map((l) => l?.instructorUserId).filter(Boolean))];
    const im = {};
    await Promise.all(insIds.map(async (id) => {
      try { const { data } = await userApi.getById(id); im[id] = data; } catch {}
    }));
    if (!isCancelled()) setInstructorMap(im);
  }, [cart]);

  const isLabOnly = (line) => (line.usageType || 'BORROWABLE').toUpperCase() === 'LAB_ONLY';

  // Group lines by lab so the form makes the per-instructor approval explicit.
  const groupedByLab = cart.reduce((acc, line) => {
    (acc[line.labId] = acc[line.labId] || []).push(line);
    return acc;
  }, {});
  const labGroups = Object.entries(groupedByLab);

  const addAttachment = () =>
    setAttachments([...attachments, { fileUrl: '', fileName: '', kind: 'OTHER' }]);
  const updateAttachment = (i, k, v) => {
    const copy = [...attachments];
    copy[i] = { ...copy[i], [k]: v };
    setAttachments(copy);
  };
  const removeAttachment = (i) =>
    setAttachments(attachments.filter((_, idx) => idx !== i));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!me?.departmentId) {
      setError('Your account is missing a department. Please log out and back in.');
      return;
    }
    if (cart.length === 0) {
      setError('Your cart is empty. Add items from the equipment catalogue first.');
      return;
    }
    const start = new Date(startDate);
    const ret = new Date(returnDate);
    if (!(ret > start)) {
      setError('Return date must be after start date.');
      return;
    }
    // Refuse if any lab has no instructor assigned (server would reject anyway, but fail fast).
    const blockingLab = labGroups.find(([labId]) => !labMap[labId]?.instructorUserId);
    if (blockingLab) {
      const [labId] = blockingLab;
      setError(`Lab ${labMap[labId]?.name || `#${labId}`} has no instructor assigned — remove its items.`);
      return;
    }
    // Lab-only items need the student to propose a lab-use time.
    const missingTime = cart.find((c) => isLabOnly(c) && !useTimes[c.itemId]);
    if (missingTime) {
      setError(`Pick a preferred lab-use time for "${missingTime.name}" (it's lab-use only).`);
      return;
    }

    const cleanAttachments = attachments
      .filter((a) => a.fileUrl?.trim() && a.fileName?.trim())
      .map((a) => ({ fileUrl: a.fileUrl.trim(), fileName: a.fileName.trim(), kind: a.kind || 'OTHER' }));

    setBusy(true);
    try {
      const { data } = await bookingApi.create({
        items: cart.map((c) => ({
          itemId: c.itemId,
          labId: c.labId,
          requestedUseTime: isLabOnly(c) ? (useTimes[c.itemId] || null) : null,
        })),
        projectName,
        purpose,
        startDate,
        returnDate,
        studentDepartmentId: me.departmentId,
        attachments: cleanAttachments.length ? cleanAttachments : undefined,
      });
      clearCart();
      const uniqueInstructors = new Set(Object.values(labMap).map((l) => l?.instructorUserId).filter(Boolean));
      nav('/student/bookings', {
        state: {
          flash: `Booking #${data.id} submitted with ${cart.length} item${cart.length === 1 ? '' : 's'}`
                 + ` across ${uniqueInstructors.size} instructor${uniqueInstructors.size === 1 ? '' : 's'}.`,
        },
      });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container add-eq-wrap">
      <Link to="/student/equipment" className="auth-back">← Continue browsing</Link>
      <div className="add-eq-head">
        <div>
          <h1 className="page-title">Submit booking</h1>
          <p className="page-sub">
            All items below share the same start and return window. Each lab's instructor reviews
            their own items independently — they can approve directly or forward to a supervisor.
          </p>
        </div>
      </div>

      {cart.length === 0 ? (
        <div className="empty">
          <div className="empty-icon">🛒</div>
          Your cart is empty. <Link to="/student/equipment">Browse equipment</Link> to add items.
        </div>
      ) : (
        <div className="add-eq-grid">
          <form className="add-eq-form" onSubmit={submit}>
            {error && <div className="alert alert-error">{error}</div>}

            <Section num="01" title="Project" sub="What you'll use these items for.">
              <div className="field">
                <label>Project name <span className="req">*</span></label>
                <input value={projectName} required onChange={(e) => setProjectName(e.target.value)}
                       placeholder="e.g., Final-year IoT power-monitor" />
              </div>
              <div className="field">
                <label>Purpose / justification <span className="req">*</span></label>
                <textarea rows="4" value={purpose} required onChange={(e) => setPurpose(e.target.value)}
                          placeholder="Briefly describe why you need these items." />
              </div>
            </Section>

            <Section num="02" title="Duration" sub="Shared by every item in the booking.">
              <div className="field-row">
                <div className="field">
                  <label>Start date <span className="req">*</span></label>
                  <input type="datetime-local" value={startDate} required
                         onChange={(e) => setStartDate(e.target.value)} />
                </div>
                <div className="field">
                  <label>Return date <span className="req">*</span></label>
                  <input type="datetime-local" value={returnDate} required
                         onChange={(e) => setReturnDate(e.target.value)} />
                </div>
              </div>
            </Section>

            <Section num="03" title="Attachments" sub="Request letter, supervisor letter, related docs (optional).">
              {attachments.length === 0 && (
                <div className="add-eq-empty" style={{ marginBottom: 8 }}>
                  Paste a link to your file (Drive, Dropbox, etc.). Direct upload coming soon.
                </div>
              )}
              {attachments.map((a, i) => (
                <div key={i} className="field-row" style={{ alignItems: 'flex-end' }}>
                  <div className="field">
                    <label>Kind</label>
                    <select value={a.kind} onChange={(e) => updateAttachment(i, 'kind', e.target.value)}>
                      <option value="REQUEST_LETTER">Request letter</option>
                      <option value="SUPERVISOR_LETTER">Supervisor letter</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="field">
                    <label>File name</label>
                    <input value={a.fileName} onChange={(e) => updateAttachment(i, 'fileName', e.target.value)}
                           placeholder="request-letter.pdf" />
                  </div>
                  <div className="field">
                    <label>URL</label>
                    <input value={a.fileUrl} onChange={(e) => updateAttachment(i, 'fileUrl', e.target.value)}
                           placeholder="https://drive.google.com/..." />
                  </div>
                  <button type="button" className="btn btn-secondary btn-sm"
                          onClick={() => removeAttachment(i)}>Remove</button>
                </div>
              ))}
              <button type="button" className="btn-pill btn-pill-ghost"
                      onClick={addAttachment} style={{ marginTop: 8 }}>
                + Add attachment
              </button>
            </Section>

            <div className="add-eq-actions">
              <Link to="/student/equipment" className="btn-pill btn-pill-ghost">Cancel</Link>
              <button type="submit" className="btn-pill btn-pill-solid btn-pill-lg" disabled={busy}>
                {busy ? 'Submitting…' : `Submit ${cart.length} item${cart.length === 1 ? '' : 's'} →`}
              </button>
            </div>
          </form>

          <aside className="add-eq-aside">
            <div className="add-eq-card">
              <div className="add-eq-card-head">
                <span className="add-eq-card-eyebrow">Cart</span>
                <span className="add-eq-card-count">{cart.length}</span>
              </div>
              {labGroups.map(([labId, lines]) => {
                const lab = labMap[labId];
                const instr = lab?.instructorUserId ? instructorMap[lab.instructorUserId] : null;
                return (
                  <div key={labId} style={{ paddingTop: 12, borderTop: '1px solid var(--border)' }}>
                    <div style={{ fontWeight: 600 }}>
                      {lab ? lab.name : `Lab #${labId}`}
                      {lab?.location ? ` · ${lab.location}` : ''}
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 6 }}>
                      Instructor: {instr ? `${instr.fullName} (${instr.email})` : '— unassigned —'}
                    </div>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                      {lines.map((l) => (
                        <li key={l.itemId} style={{ padding: '4px 0' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{ flex: 1 }}>
                              <div>{l.name}
                                {' '}<span className={`eq-usage ${isLabOnly(l) ? 'eq-usage-lab' : 'eq-usage-borrow'}`}>
                                  {isLabOnly(l) ? 'Lab only' : 'Borrowable'}
                                </span>
                              </div>
                              <div style={{ fontSize: 12, color: 'var(--muted)' }}>{l.model}</div>
                            </div>
                            <button type="button" className="btn btn-secondary btn-sm"
                                    onClick={() => removeFromCart(l.itemId)}>
                              Remove
                            </button>
                          </div>
                          {isLabOnly(l) && (
                            <div className="field" style={{ marginTop: 6 }}>
                              <label style={{ fontSize: 12 }}>Preferred lab-use time <span className="req">*</span></label>
                              <input type="datetime-local" value={useTimes[l.itemId] || ''}
                                     onChange={(e) => setUseTimes((m) => ({ ...m, [l.itemId]: e.target.value }))} />
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                );
              })}
            </div>

            <div className="add-eq-tips">
              <div className="add-eq-tips-title">How approvals work</div>
              <ul>
                <li>Your request goes to your <strong>Head of Department</strong> first for approval.</li>
                <li>Once the HoD approves, the lab <strong>instructor</strong> gives the final approval.</li>
                <li><strong>Borrowable</strong> items get a pickup time; <strong>lab-only</strong> items get a confirmed in-lab time.</li>
                <li>You'll see each line's progress in <em>My Bookings</em>.</li>
              </ul>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}

function Section({ num, title, sub, children }) {
  return (
    <div className="add-eq-section">
      <div className="add-eq-section-head">
        <span className="add-eq-section-num">{num}</span>
        <div>
          <h2>{title}</h2>
          <p>{sub}</p>
        </div>
      </div>
      {children}
    </div>
  );
}
