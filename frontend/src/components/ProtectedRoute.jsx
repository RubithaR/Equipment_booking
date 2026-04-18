import { Navigate } from 'react-router-dom';
import { isAuthenticated, getCurrentUser, homePathFor } from '../auth';

// Wrap routes that require login. Optionally restrict to specific roles.
export default function ProtectedRoute({ children, roles }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  const user = getCurrentUser();
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }
  return children;
}
