import React, { useState, useEffect } from 'react';
import { ref, push, set } from 'firebase/database';
import { db } from '../../firebase';
import { useAuth } from '../../hooks/useAuth';
import { PS } from '../../hooks/usePatientState';

const STATUS_CFG = {
  pending:   { icon: '⏳', label: 'În așteptare',      color: 'amber',  msg: 'Cererea ta a fost primită. Echipa de recepție o va procesa în cel mai scurt timp.' },
  scheduled: { icon: '📅', label: 'Programare confirmată', color: 'blue', msg: 'Recepția a confirmat programarea. Medicul te va activa în sistem după prima consultație.' },
  active:    { icon: '✅', label: 'Activ',              color: 'emerald', msg: 'Ești activat în sistem. Redirecționare...' },
};

const COLOR = {
  amber:   'bg-amber-50 border-amber-200 text-amber-800',
  blue:    'bg-blue-50 border-blue-200 text-blue-800',
  emerald: 'bg-emerald-50 border-emerald-200 text-emerald-800',
};

export default function BookAppointment({ appointmentRequest, state }) {
  const { user, userData } = useAuth();
  const [form, setForm] = useState({ prenume: '', nume: '', varsta: '', telefon: '', symptoms: '' });
  const [saving,  setSaving]  = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error,   setError]   = useState('');

  // Pre-completăm din profil
  useEffect(() => {
    if (userData) {
      setForm(f => ({
        ...f,
        prenume: userData.prenume || '',
        nume:    userData.nume    || '',
        telefon: userData.telefon || '',
        varsta:  userData.varsta  ? String(userData.varsta) : '',
      }));
    }
  }, [userData]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.symptoms.trim()) { setError('Descrieți simptomele.'); return; }
    setSaving(true);
    setError('');
    try {
      const newRef = push(ref(db, 'appointmentRequests'));
      await set(newRef, {
        patientUid:    user.uid,
        email:         user.email.toLowerCase(),
        prenume:       form.prenume.trim(),
        nume:          form.nume.trim(),
        varsta:        form.varsta ? parseInt(form.varsta) : null,
        telefon:       form.telefon.trim(),
        symptoms:      form.symptoms.trim(),
        status:        'pending',
        medicId:       null,
        medicNume:     null,
        scheduledSlot: null,
        creatLa:       Date.now(),
      });
      setSubmitted(true);
    } catch (e) {
      setError('Eroare la trimitere. Reîncercați.');
      console.error(e);
    } finally {
      setSaving(false);
    }
  };

  // Dacă există deja o cerere, arătăm statusul ei
  const req = appointmentRequest;
  const showStatus = req || submitted;

  if (showStatus) {
    const cfg  = STATUS_CFG[req?.status || 'pending'];
    const colC = COLOR[cfg.color];
    return (
      <div className="max-w-lg mx-auto space-y-5">
        <div className={`rounded-2xl border p-6 ${colC}`}>
          <div className="flex items-center gap-3 mb-3">
            <span className="text-3xl">{cfg.icon}</span>
            <div>
              <p className="font-bold text-lg">{cfg.label}</p>
              <p className="text-sm opacity-80 mt-0.5">{cfg.msg}</p>
            </div>
          </div>

          {req && (
            <div className="bg-white/60 rounded-xl p-4 space-y-1.5 text-sm mt-3">
              <InfoRow label="Pacient"       value={`${req.prenume} ${req.nume}`} />
              <InfoRow label="Simptome"      value={req.symptoms} />
              {req.medicNume && <InfoRow label="Medic asignat" value={req.medicNume} strong />}
              {req.scheduledSlot && (
                <InfoRow label="Programare"
                  value={`${req.scheduledSlot.date} la ${req.scheduledSlot.time}`}
                  strong
                />
              )}
              <InfoRow label="Trimis"
                value={req.creatLa ? new Date(req.creatLa).toLocaleString('ro-RO') : '—'}
              />
            </div>
          )}
        </div>
      </div>
    );
  }

  // Form inițial — pacient fără nicio cerere
  return (
    <div className="max-w-lg mx-auto">
      {/* Hero */}
      <div className="bg-gradient-to-br from-blue-600 to-blue-700 rounded-2xl p-6 text-white mb-5 text-center">
        <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-3">
          <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
        <h2 className="text-xl font-bold">Rezervă o Programare</h2>
        <p className="text-blue-200 text-sm mt-1 max-w-xs mx-auto">
          Nu aveți un medic asignat. Trimiteți o cerere — echipa noastră vă va aloca un specialist disponibil.
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-100 p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <F label="Prenume"><input className="inp" value={form.prenume} onChange={e => setForm(f=>({...f,prenume:e.target.value}))} placeholder="Ion" required /></F>
            <F label="Nume">   <input className="inp" value={form.nume}    onChange={e => setForm(f=>({...f,nume:   e.target.value}))} placeholder="Popescu" required /></F>
            <F label="Vârstă"><input className="inp" type="number" value={form.varsta} onChange={e => setForm(f=>({...f,varsta:e.target.value}))} placeholder="72" /></F>
            <F label="Telefon"><input className="inp" value={form.telefon} onChange={e => setForm(f=>({...f,telefon:e.target.value}))} placeholder="07xx" /></F>
          </div>

          <F label="Descrieți simptomele și motivul programării *">
            <textarea
              className="inp resize-none" rows={5}
              value={form.symptoms}
              onChange={e => setForm(f => ({...f, symptoms: e.target.value}))}
              placeholder="Descrieți simptomele, de cât timp le aveți, ce tratamente urmați..."
              required
            />
          </F>

          {error && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

          <button type="submit" disabled={saving}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-xl transition-colors disabled:opacity-60 flex items-center justify-center gap-2 text-sm">
            {saving
              ? <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>Se trimite...</>
              : '📅 Trimite Cererea de Programare'
            }
          </button>
        </form>
      </div>

      <style>{`.inp{width:100%;padding:.5rem .75rem;border:1px solid #e2e8f0;border-radius:.5rem;font-size:.875rem;color:#1e293b;background:#f8fafc;outline:none;transition:all .15s;}.inp:focus{background:white;box-shadow:0 0 0 2px #3b82f6;border-color:transparent;}`}</style>
    </div>
  );
}

function F({ label, children }) {
  return <div><label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>{children}</div>;
}
function InfoRow({ label, value, strong }) {
  return (
    <div className="flex gap-2">
      <span className="text-slate-500 flex-shrink-0 w-28 text-xs">{label}:</span>
      <span className={`flex-1 text-xs ${strong ? 'font-bold' : ''}`}>{value || '—'}</span>
    </div>
  );
}
