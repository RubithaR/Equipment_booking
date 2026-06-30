import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import { getCurrentUser, homePathFor } from './auth';

import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Terms from './pages/Terms';
import Notifications from './pages/Notifications';
import Chat from './pages/Chat';

import StudentEquipment from './pages/student/Equipment';
import MyBookings from './pages/student/MyBookings';
import BookCart from './pages/student/BookCart';

import PendingBookings from './pages/instructor/PendingBookings';
import AllBookings from './pages/instructor/AllBookings';
import PendingStudents from './pages/instructor/PendingStudents';

import AdminOverview from './pages/admin/AdminOverview';
import PendingInstructors from './pages/admin/PendingInstructors';
import Users from './pages/admin/Users';
import AdminEquipment from './pages/admin/Equipment';
import AdminAddEquipment from './pages/admin/AddEquipment';
import AdminLabs from './pages/admin/Labs';
import AdminDepartments from './pages/admin/Departments';

import HodDashboard from './pages/hod/HodDashboard';
import AwaitingRole from './pages/staff/AwaitingRole';

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

        {/* Registered staff awaiting an admin-assigned role — welcome / holding page with nav */}
        <Route element={<ProtectedRoute roles={['STAFF']}><Shell /></ProtectedRoute>}>
          <Route path="/staff" element={<AwaitingRole />} />
        </Route>

        <Route element={<ProtectedRoute roles={['STUDENT']}><Shell /></ProtectedRoute>}>
          <Route path="/student" element={<Navigate to="/student/equipment" replace />} />
          <Route path="/student/equipment" element={<StudentEquipment />} />
          <Route path="/student/cart" element={<BookCart />} />
          <Route path="/student/bookings" element={<MyBookings />} />
          <Route path="/student/chat" element={<Chat />} />
          <Route path="/student/notifications" element={<Notifications />} />
        </Route>

        {/* Handler queue — instructor / lecturer / HOD can be assigned a request to handle */}
        <Route element={<ProtectedRoute roles={['INSTRUCTOR', 'LECTURER', 'HOD']}><Shell /></ProtectedRoute>}>
          <Route path="/instructor" element={<Navigate to="/instructor/pending" replace />} />
          <Route path="/instructor/pending" element={<PendingBookings />} />
          <Route path="/instructor/students-pending" element={<PendingStudents />} />
          <Route path="/instructor/all" element={<AllBookings />} />
          <Route path="/instructor/chat" element={<Chat />} />
          <Route path="/instructor/notifications" element={<Notifications />} />
        </Route>

        {/* HOD dashboard — review student requests and assign a handler */}
        <Route element={<ProtectedRoute roles={['HOD']}><Shell /></ProtectedRoute>}>
          <Route path="/hod" element={<HodDashboard />} />
          <Route path="/hod/chat" element={<Chat />} />
          <Route path="/hod/notifications" element={<Notifications />} />
        </Route>

        <Route element={<ProtectedRoute roles={['MAIN_ADMIN', 'DEPT_ADMIN']}><Shell /></ProtectedRoute>}>
          <Route path="/admin" element={<AdminOverview />} />
          <Route path="/admin/instructors-pending" element={<PendingInstructors />} />
          <Route path="/admin/users" element={<Users />} />
          <Route path="/admin/bookings" element={<AllBookings />} />
          <Route path="/admin/equipment" element={<AdminEquipment />} />
          <Route path="/admin/equipment/new" element={<AdminAddEquipment />} />
          <Route path="/admin/equipment/:id/edit" element={<AdminAddEquipment />} />
          <Route path="/admin/labs" element={<AdminLabs />} />
          <Route path="/admin/departments" element={<AdminDepartments />} />
          <Route path="/admin/notifications" element={<Notifications />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
