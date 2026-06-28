export function fmt(dt) {
  return dt ? new Date(dt).toLocaleString() : '—';
}

export function trunc(s, n) {
  if (!s) return '';
  return s.length > n ? s.slice(0, n) + '…' : s;
}

export function byId(arr) {
  return Object.fromEntries(arr.map((x) => [x.id, x]));
}
