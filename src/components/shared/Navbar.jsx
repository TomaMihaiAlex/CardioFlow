import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { logoutUser } from '../../firebase';

export default function Navbar({ title }) {
  const { userData } = useAuth();

  const handleLogout = async () => {
    try { await logoutUser(); } catch (e) { console.error(e); }
  };

  return (
    <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-5 flex-shrink-0 z-10">
      <div className="flex items-center gap-2.5">
        <div className="w-7 h-7 bg-blue-600 rounded-lg flex items-center justify-center">
          <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
        </div>
        <span className="font-bold text-slate-800 tracking-tight">CardioFlow</span>
        {title && <span className="text-slate-400 text-sm hidden sm:inline">/ {title}</span>}
      </div>

      <div className="flex items-center gap-3">
        {userData && (
          <div className="text-right hidden sm:block">
            <p className="text-sm font-medium text-slate-700 leading-none">
              {userData.prenume} {userData.nume}
            </p>
            <p className="text-xs text-slate-400 mt-0.5 capitalize">{userData.role}</p>
          </div>
        )}
        <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 font-semibold text-sm">
          {userData ? (userData.prenume?.[0] ?? userData.email?.[0] ?? '?').toUpperCase() : '?'}
        </div>
        <button
          onClick={handleLogout}
          className="text-sm text-slate-500 hover:text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-lg transition-colors font-medium"
        >
          Ieșire
        </button>
      </div>
    </header>
  );
}
