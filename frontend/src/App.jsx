import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import { getCurrentUser, homePathFor } from './auth';

import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Terms from './pages/Terms';
import Notifications from './pages/Notifications';

import StudentEquipment from './pages/student/Equipment';
import MyBookings from './pages/student/MyBookings';

import PendingBookings from './pages/instructor/PendingBookings';
import AllBookings from './pages/instructor/AllBookings';
import InstructorAddEquipment from './pages/instructor/AddEquipment';

import AdminOverview from './pages/admin/AdminOverview';
import PendingInstructors from './pages/admin/PendingInstructors';
import Users from './pages/admin/Users';
import AdminEquipment from './pages/admin/Equipment';

function Shell() {
  return (
    <>
      <NavBar />
      <Outlet />
    </>
  );
}

function RootRedirect() {
  const user = getCurrentUser();
  if (user) return <Navigate to={homePathFor(user.role)} replace />;
  return <Home />;
}

function NotFound() {
  const user = getCurrentUser();
  const location = useLocation();
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace state={{ from: location }} />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/terms" element={<Terms />} />

        <Route element={<ProtectedRoute roles={['STUDENT']}><Shell /></ProtectedRoute>}>
          <Route path="/student" element={<Navigate to="/student/equipment" replace />} />
          <Route path="/student/equipment" element={<StudentEquipment />} />
          <Route path="/student/bookings" element={<MyBookings />} />
          <Route path="/student/notifications" element={<Notifications />} />
        </Route>

        <Route element={<ProtectedRoute roles={['INSTRUCTOR']}><Shell /></ProtectedRoute>}>
          <Route path="/instructor" element={<Navigate to="/instructor/pending" replace />} />
          <Route path="/instructor/pending" element={<PendingBookings />} />
          <Route path="/instructor/all" element={<AllBookings />} />
          <Route path="/instructor/equipment/new" element={<InstructorAddEquipment />} />
          <Route path="/instructor/notifications" element={<Notifications />} />
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN']}><Shell /></ProtectedRoute>}>
          <Route path="/admin" element={<AdminOverview />} />
          <Route path="/admin/instructors-pending" element={<PendingInstructors />} />
          <Route path="/admin/users" element={<Users />} />
          <Route path="/admin/bookings" element={<AllBookings />} />
          <Route path="/admin/equipment" element={<AdminEquipment />} />
          <Route path="/admin/notifications" element={<Notifications />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
