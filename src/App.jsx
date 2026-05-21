import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import Login                from './pages/Login';
import Register             from './pages/Register';
import MedicDashboard       from './pages/MedicDashboard';
import PacientDashboard     from './pages/PacientDashboard';
import ReceptionistDashboard from './pages/ReceptionistDashboard';
import LoadingSpinner       from './components/shared/LoadingSpinner';

const ROLE_HOME = { medic: '/medic', pacient: '/pacient', receptionist: '/receptionist' };
const roleHome  = (role) => ROLE_HOME[role] || '/login';

function ProtectedRoute({ children, allowedRole }) {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (!user)   return <Navigate to="/login" replace />;
  if (role && role !== allowedRole) return <Navigate to={roleHome(role)} replace />;
  return children;
}

function PublicOnlyRoute({ children }) {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (user)    return <Navigate to={roleHome(role)} replace />;
  return children;
}

function RootRedirect() {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (!user)   return <Navigate to="/login" replace />;
  return <Navigate to={roleHome(role)} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login"        element={<PublicOnlyRoute><Login /></PublicOnlyRoute>} />
        <Route path="/register"     element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />
        <Route path="/medic"        element={<ProtectedRoute allowedRole="medic">       <MedicDashboard /></ProtectedRoute>} />
        <Route path="/pacient"      element={<ProtectedRoute allowedRole="pacient">     <PacientDashboard /></ProtectedRoute>} />
        <Route path="/receptionist" element={<ProtectedRoute allowedRole="receptionist"><ReceptionistDashboard /></ProtectedRoute>} />
        <Route path="*"             element={<RootRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}
