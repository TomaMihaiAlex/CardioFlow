import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ref, set } from 'firebase/database';
import { registerUser, db } from '../firebase';

const FIREBASE_ERRORS = {
  'auth/email-already-in-use': 'Există deja un cont cu această adresă de email.',
  'auth/invalid-email':        'Adresa de email nu este validă.',
  'auth/weak-password':        'Parola trebuie să aibă cel puțin 6 caractere.',
};

const ROLES = [
  { value: 'pacient',       label: 'Pacient',       icon: '🏃', desc: 'Urmărire date personale și programări' },
  { value: 'medic',         label: 'Medic',         icon: '🩺', desc: 'Gestionare pacienți și monitorizare' },
  { value: 'receptionist',  label: 'Recepționist',  icon: '📋', desc: 'Procesare cereri și asignare medici' },
];

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    prenume: '', nume: '', email: '', password: '', confirmPassword: '', role: 'pacient',
  });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const set_ = (field, value) => setForm(f => ({ ...f, [field]: value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.prenume.trim() || !form.nume.trim()) { setError('Introduceți numele și prenumele.'); return; }
    if (form.password !== form.confirmPassword)    { setError('Parolele nu coincid.'); return; }
    if (form.password.length < 6)                  { setError('Parola trebuie să aibă cel puțin 6 caractere.'); return; }

    setLoading(true);
    try {
      const credential = await registerUser(form.email.trim(), form.password);
      const uid = credential.user.uid;

      await set(ref(db, `users/${uid}`), {
        uid,
        email:   form.email.trim().toLowerCase(),
        role:    form.role,
        prenume: form.prenume.trim(),
        nume:    form.nume.trim(),
      });

      const dest = { medic: '/medic', pacient: '/pacient', receptionist: '/receptionist' };
      navigate(dest[form.role] || '/pacient', { replace: true });
    } catch (err) {
      setError(FIREBASE_ERRORS[err.code] || 'A apărut o eroare. Încercați din nou.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-950 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* Header */}
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

          <div className="px-8 py-8">
            <h2 className="text-slate-800 font-semibold text-base mb-6">Creare Cont Nou</h2>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Selecție rol */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Tip cont</label>
                <div className="grid grid-cols-3 gap-2">
                  {ROLES.map(r => (
                    <button key={r.value} type="button" onClick={() => set_('role', r.value)}
                      className={`p-3 rounded-xl border-2 text-left transition-all
                        ${form.role === r.value ? 'border-blue-600 bg-blue-50' : 'border-slate-200 hover:border-slate-300'}`}>
                      <span className="text-xl block mb-1">{r.icon}</span>
                      <p className={`text-xs font-semibold ${form.role === r.value ? 'text-blue-700' : 'text-slate-700'}`}>{r.label}</p>
                    </button>
                  ))}
                </div>
                <p className="text-xs text-slate-400 mt-1.5">
                  {ROLES.find(r => r.value === form.role)?.desc}
                </p>
              </div>

              {/* Nume */}
              <div className="grid grid-cols-2 gap-3">
                <F label="Prenume">
                  <input type="text" value={form.prenume} onChange={e => set_('prenume', e.target.value)}
                    required placeholder="Ion" className="input-base" />
                </F>
                <F label="Nume">
                  <input type="text" value={form.nume} onChange={e => set_('nume', e.target.value)}
                    required placeholder="Popescu" className="input-base" />
                </F>
              </div>

              <F label="Adresă Email">
                <input type="email" value={form.email} onChange={e => set_('email', e.target.value)}
                  required autoComplete="email" placeholder="adresa@email.ro" className="input-base" />
              </F>

              <F label="Parolă">
                <input type="password" value={form.password} onChange={e => set_('password', e.target.value)}
                  required autoComplete="new-password" placeholder="Minim 6 caractere" className="input-base" />
              </F>

              <F label="Confirmă Parola">
                <input type="password" value={form.confirmPassword} onChange={e => set_('confirmPassword', e.target.value)}
                  required autoComplete="new-password" placeholder="••••••••"
                  className={`input-base ${form.confirmPassword && form.password !== form.confirmPassword ? 'ring-2 ring-red-400' : ''}`} />
                {form.confirmPassword && form.password !== form.confirmPassword && (
                  <p className="text-xs text-red-500 mt-1">Parolele nu coincid</p>
                )}
              </F>

              {error && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg">
                  <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                  <span>{error}</span>
                </div>
              )}

              <button type="submit" disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-lg transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2">
                {loading ? (
                  <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>Se creează contul...</>
                ) : 'Creează Cont'}
              </button>
            </form>

            <p className="text-center text-slate-500 text-sm mt-5">
              Ai deja cont?{' '}
              <Link to="/login" className="text-blue-600 hover:text-blue-700 font-medium">Autentifică-te</Link>
            </p>
          </div>
        </div>
        <p className="text-center text-blue-200/50 text-xs mt-4">Acces restricționat personalului medical autorizat</p>
      </div>

      <style>{`.input-base{width:100%;padding:.625rem 1rem;border-radius:.5rem;border:1px solid #e2e8f0;background:#f8fafc;color:#1e293b;font-size:.875rem;outline:none;transition:all .15s;}.input-base:focus{background:white;border-color:transparent;box-shadow:0 0 0 2px #3b82f6;}.input-base::placeholder{color:#94a3b8;}`}</style>
    </div>
  );
}

function F({ label, children }) {
  return <div><label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>{children}</div>;
}
