// Small status badge with colors driven by CSS classes.
export default function Badge({ value }) {
  if (!value) return null;
  const cls = 'badge badge-' + String(value).toLowerCase();
  return <span className={cls}>{String(value).replace(/_/g, ' ')}</span>;
}
