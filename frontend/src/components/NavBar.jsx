import { NavLink, useNavigate } from 'react-router-dom';
import { getCurrentUser, logout } from '../auth';
import uniLogo from '../assets/Uni_logo.png';

export default function NavBar() {
  const nav = useNavigate();
  const user = getCurrentUser();
  if (!user) return null;

  const doLogout = () => {
    logout();
    nav('/login', { replace: true });
  };

  const role = user.role;
  const roleLabel = role === 'STUDENT' ? 'Student'
    : role === 'INSTRUCTOR' ? 'Instructor'
      : 'Administrator';
  const initials = (user.fullName || user.email || '?')
    .split(/\s+/).filter(Boolean).slice(0, 2)
    .map(s => s[0].toUpperCase()).join('') || '?';

  return (
    <nav className="navbar">
      <div className="nav-inner">
        <div className="nav-brand">
          <div className="nav-logo">
            <img src={uniLogo} alt="University of Sri Jayewardenepura" />
          </div>
          <div className="nav-brand-text">
            <span className="nav-uni">University of Sri Jayewardenepura</span>
            <span className="nav-platform">Smart<em>Lab</em> · Equipment Booking</span>
          </div>
        </div>

        <div className="nav-links">
          {role === 'STUDENT' && (
            <>
              <NavLink to="/student/equipment">Equipment</NavLink>
              <NavLink to="/student/bookings">My Bookings</NavLink>
              <NavLink to="/student/notifications">Notifications</NavLink>
            </>
          )}
          {role === 'INSTRUCTOR' && (
            <>
              <NavLink to="/instructor/equipment/new">Add Equipment</NavLink>
              <NavLink to="/instructor/students-pending">Pending Students</NavLink>
              <NavLink to="/instructor/pending">Pending Bookings</NavLink>
              <NavLink to="/instructor/all">All Bookings</NavLink>
              <NavLink to="/instructor/notifications">Notifications</NavLink>
            </>
          )}
          {role === 'ADMIN' && (
            <>
              <NavLink to="/admin">Overview</NavLink>
              <NavLink to="/admin/instructors-pending">Pending Instructors</NavLink>
              <NavLink to="/admin/users">Users</NavLink>
              <NavLink to="/admin/labs">Labs</NavLink>
              <NavLink to="/admin/bookings">Bookings</NavLink>
              <NavLink to="/admin/equipment">Equipment</NavLink>
            </>
          )}
        </div>

        <div className="nav-user">
          <div className="user-card">
            <div className="user-avatar" aria-hidden="true">{initials}</div>
            <div className="user-meta">
              <span className="user-name">{user.fullName}</span>
              <span className="user-role">{roleLabel}</span>
            </div>
          </div>
          <button className="btn-logout" onClick={doLogout}>Logout</button>
        </div>
      </div>
    </nav>
  );
}
