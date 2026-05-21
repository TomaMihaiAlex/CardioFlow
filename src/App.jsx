import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import LandingPage          from './pages/LandingPage';
import Login                from './pages/Login';
import Register             from './pages/Register';
import MedicDashboard       from './pages/MedicDashboard';
import PacientDashboard     from './pages/PacientDashboard';
import ReceptionistDashboard from './pages/ReceptionistDashboard';
import LoadingSpinner       from './components/shared/LoadingSpinner';

const ROLE_HOME = { medic: '/medic', pacient: '/pacient', receptionist: '/receptionist' };
const roleHome  = (role) => ROLE_HOME[role] || '/medic';

/** Rută protejată — necesită autentificare + rol specific */
function ProtectedRoute({ children, allowedRole }) {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (!user)   return <Navigate to="/" replace />;
  if (role && role !== allowedRole) return <Navigate to={roleHome(role)} replace />;
  return children;
}

/** Rute publice (/, /login, /register) — dacă ești logat, redirecționezi la dashboard */
function PublicRoute({ children }) {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (user)    return <Navigate to={roleHome(role)} replace />;
  return children;
}

/** Redirect inteligent din rădăcină */
function RootRoute() {
  const { user, role, loading } = useAuth();
  if (loading) return <LoadingSpinner />;
  if (user)    return <Navigate to={roleHome(role)} replace />;
  return <LandingPage />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Pagina principală — landing dacă nu ești logat, dashboard dacă ești */}
        <Route path="/"         element={<RootRoute />} />

        {/* Pagini publice */}
        <Route path="/login"    element={<PublicRoute><Login /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />

        {/* Dashboard-uri protejate pe rol */}
        <Route path="/medic"        element={<ProtectedRoute allowedRole="medic">       <MedicDashboard /></ProtectedRoute>} />
        <Route path="/pacient"      element={<ProtectedRoute allowedRole="pacient">     <PacientDashboard /></ProtectedRoute>} />
        <Route path="/receptionist" element={<ProtectedRoute allowedRole="receptionist"><ReceptionistDashboard /></ProtectedRoute>} />

        {/* Orice altă rută → rădăcină */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
