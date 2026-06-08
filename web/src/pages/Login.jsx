import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { loginUser } from '../firebase';

export default function Login() {
  const [email,    setEmail]    = useState('');
  const [password, setPassword] = useState('');
  const [error,    setError]    = useState('');
  const [loading,  setLoading]  = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await loginUser(email, password);
      // Redirecționarea este gestionată de App.jsx prin schimbarea stării de auth
    } catch {
      setError('Email sau parolă incorectă. Verificați datele și încercați din nou.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-950 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* Header colorat */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-6">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 bg-white/20 rounded-xl flex items-center justify-center">
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </div>
              <div>
                <h1 className="text-white font-bold text-xl tracking-tight">CardioFlow</h1>
                <p className="text-blue-200 text-xs mt-0.5">Sistem de Supraveghere a Sănătății</p>
              </div>
            </div>
          </div>

          {/* Formular */}
          <div className="px-8 py-8">
            <h2 className="text-slate-800 font-semibold text-base mb-6">Autentificare</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <FormField label="Adresă Email">
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                  placeholder="adresa@spital.ro"
                  className="input-base"
                />
              </FormField>

              <FormField label="Parolă">
                <input
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="input-base"
                />
              </FormField>

              {error && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg">
                  <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold py-2.5 rounded-lg transition-colors duration-150 disabled:opacity-60 disabled:cursor-not-allowed mt-2 flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Se autentifică...
                  </>
                ) : 'Intră în cont'}
              </button>
            </form>

            <p className="text-center text-slate-500 text-sm mt-5">
              Nu ai cont?{' '}
              <Link to="/register" className="text-blue-600 hover:text-blue-700 font-medium">
                Înregistrează-te
              </Link>
            </p>
          </div>
        </div>

        <p className="text-center text-blue-200/50 text-xs mt-4">
          Acces restricționat — doar personalul medical autorizat
        </p>
      </div>

      {/* Stil inline pentru input (evită repetarea claselor lungi) */}
      <style>{`
        .input-base {
          width: 100%;
          padding: 0.625rem 1rem;
          border-radius: 0.5rem;
          border: 1px solid #e2e8f0;
          background: #f8fafc;
          color: #1e293b;
          font-size: 0.875rem;
          transition: all 0.15s;
          outline: none;
        }
        .input-base:focus {
          background: white;
          border-color: transparent;
          box-shadow: 0 0 0 2px #3b82f6;
        }
        .input-base::placeholder { color: #94a3b8; }
      `}</style>
    </div>
  );
}

function FormField({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>
      {children}
    </div>
  );
}
