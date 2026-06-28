// Tiny localStorage-backed cart for the multi-item booking flow.
// Subscribers are notified via a `storage` event on this tab and a custom
// `sl-cart` event in-tab so the navbar badge can react instantly.

const KEY = 'sl_cart';
const EVENT = 'sl-cart';

function read() {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function write(items) {
  localStorage.setItem(KEY, JSON.stringify(items));
  window.dispatchEvent(new CustomEvent(EVENT));
}

export function getCart() { return read(); }
export function clearCart() { write([]); }
export function cartSize() { return read().length; }

/**
 * Add an item to the cart. Each entry is `{ itemId, labId, name, model, labName }`.
 * Duplicates (same itemId) are ignored.
 */
export function addToCart(entry) {
  const cart = read();
  if (cart.some((c) => c.itemId === entry.itemId)) return cart;
  const next = [...cart, entry];
  write(next);
  return next;
}

export function removeFromCart(itemId) {
  const next = read().filter((c) => c.itemId !== itemId);
  write(next);
  return next;
}

export function isInCart(itemId) {
  return read().some((c) => c.itemId === itemId);
}

export function subscribeCart(handler) {
  const onChange = () => handler(read());
  window.addEventListener(EVENT, onChange);
  window.addEventListener('storage', onChange);
  return () => {
    window.removeEventListener(EVENT, onChange);
    window.removeEventListener('storage', onChange);
  };
}
