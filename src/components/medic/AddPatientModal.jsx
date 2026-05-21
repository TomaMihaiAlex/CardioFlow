import React, { useState, useEffect } from 'react';
import { ref, push, set } from 'firebase/database';
import { db } from '../../firebase';
import { useAuth } from '../../hooks/useAuth';
import Modal from '../shared/Modal';

const DEFAULT_LIMITS = {
  pulsMin: 50,  pulsMax: 120,
  spo2Min: 95,  spo2Max: 100,   // SpO2 (saturație oxigen)
  tempMin: 36.0, tempMax: 38.5,
  ecgMin: -500, ecgMax: 500,    // unitati raw ESP32
  umidMin: 20,  umidMax: 80,
};

const INITIAL_STATE = {
  nume: '', prenume: '', varsta: '', cnp: '',
  adresa: '', telefon: '', email: '', profesie: '', locMunca: '',
  istoricMedical: '', alergii: '', consultatiiCardio: '',
  deviceId: '',                   // ID dispozitiv ESP32 (ex: esp32_dispozitiv_01)
  limiteSenzori: { ...DEFAULT_LIMITS },
};

export default function AddPatientModal({ open, onClose, initialData = null }) {
  const { user } = useAuth();
  const [form,   setForm]   = useState(INITIAL_STATE);
  const [step,   setStep]   = useState(0);
  const [saving, setSaving] = useState(false);
  const [error,  setError]  = useState('');

  // Când se deschide cu date pre-completate (din programare), le aplicăm
  useEffect(() => {
    if (open && initialData) {
      setForm(f => ({ ...f, ...initialData }));
      setStep(0);
    }
    if (!open) {
      setStep(0);
      setError('');
    }
  }, [open, initialData]);

  const STEPS = ['Date Demografice', 'Date Medicale', 'Dispozitiv & Limite'];

  const handleChange = (field, value) =>
    setForm(f => ({ ...f, [field]: value }));

  const handleLimitChange = (key, value) =>
    setForm(f => ({ ...f, limiteSenzori: { ...f.limiteSenzori, [key]: parseFloat(value) || 0 } }));

  const validate = () => {
    if (step === 0) {
      if (!form.nume.trim() || !form.prenume.trim()) return 'Numele și prenumele sunt obligatorii.';
      if (!form.varsta || isNaN(form.varsta))         return 'Vârsta trebuie să fie un număr valid.';
      if (!form.cnp.trim())                           return 'CNP-ul este obligatoriu.';
    }
    return '';
  };

  const nextStep = () => {
    const err = validate();
    if (err) { setError(err); return; }
    setError('');
    setStep(s => s + 1);
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      const newRef = push(ref(db, 'pacienti'));
      await set(newRef, {
        medicId:           user.uid,
        nume:              form.nume.trim(),
        prenume:           form.prenume.trim(),
        varsta:            parseInt(form.varsta),
        cnp:               form.cnp.trim(),
        adresa:            form.adresa.trim(),
        telefon:           form.telefon.trim(),
        email:             form.email.trim().toLowerCase(),
        profesie:          form.profesie.trim(),
        locMunca:          form.locMunca.trim(),
        istoricMedical:    form.istoricMedical.trim(),
        alergii:           form.alergii.trim(),
        consultatiiCardio: form.consultatiiCardio.trim(),
        deviceId:          form.deviceId.trim(),   // leagă pacientul de ESP32
        limiteSenzori:     form.limiteSenzori,
        creatLa:           Date.now(),
      });
      setForm(INITIAL_STATE);
      setStep(0);
      onClose();
    } catch (e) {
      setError('Eroare la salvare. Verificați conexiunea și regulile Realtime Database.');
      console.error(e);
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => { setForm(INITIAL_STATE); setStep(0); setError(''); onClose(); };

  return (
    <Modal open={open} onClose={handleClose} title="Adaugă Pacient Nou" size="lg">
      {/* Indicator pași */}
      <div className="flex items-center gap-2 mb-6">
        {STEPS.map((label, i) => (
          <React.Fragment key={i}>
            <div className={`flex items-center gap-2 ${i > step ? 'opacity-40' : ''}`}>
              <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold
                ${i < step ? 'bg-emerald-500 text-white' : i === step ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-500'}`}>
                {i < step ? '✓' : i + 1}
              </div>
              <span className={`text-sm font-medium hidden sm:inline ${i === step ? 'text-blue-700' : 'text-slate-500'}`}>{label}</span>
            </div>
            {i < STEPS.length - 1 && <div className="flex-1 h-px bg-slate-200" />}
          </React.Fragment>
        ))}
      </div>

      {step === 0 && <StepDemografice form={form} onChange={handleChange} />}
      {step === 1 && <StepMedicale    form={form} onChange={handleChange} />}
      {step === 2 && <StepDispozitivLimite form={form} onChange={handleChange} onLimitChange={handleLimitChange} />}

      {error && <div className="mt-4 text-red-600 text-sm bg-red-50 border border-red-200 rounded-lg px-4 py-2.5">{error}</div>}

      <div className="flex justify-between mt-6 pt-4 border-t border-slate-100">
        <button onClick={() => step === 0 ? handleClose() : setStep(s => s - 1)}
          className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg">
          {step === 0 ? 'Anulează' : '← Înapoi'}
        </button>
        {step < STEPS.length - 1
          ? <button onClick={nextStep} className="btn-primary">Continuă →</button>
          : <button onClick={handleSave} disabled={saving} className="btn-primary disabled:opacity-60">{saving ? 'Se salvează...' : 'Salvează Pacientul'}</button>
        }
      </div>

      <style>{`
        .input-field{width:100%;padding:.5rem .75rem;border:1px solid #e2e8f0;border-radius:.5rem;font-size:.875rem;color:#1e293b;background:#f8fafc;outline:none;transition:all .15s;}
        .input-field:focus{background:white;box-shadow:0 0 0 2px #3b82f6;border-color:transparent;}
        .btn-primary{padding:.5rem 1.25rem;background:#2563eb;color:white;border-radius:.5rem;font-size:.875rem;font-weight:600;transition:background .15s;cursor:pointer;}
        .btn-primary:hover{background:#1d4ed8;}
      `}</style>
    </Modal>
  );
}

function StepDemografice({ form, onChange }) {
  return (
    <div className="space-y-4">
      <STitle>Identitate</STitle>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <F label="Prenume *"><input className="input-field" value={form.prenume} onChange={e => onChange('prenume', e.target.value)} placeholder="Ion" /></F>
        <F label="Nume *">   <input className="input-field" value={form.nume}    onChange={e => onChange('nume',    e.target.value)} placeholder="Popescu" /></F>
        <F label="Vârstă *"> <input className="input-field" type="number" min="0" max="130" value={form.varsta} onChange={e => onChange('varsta', e.target.value)} placeholder="55" /></F>
        <F label="CNP *">    <input className="input-field" value={form.cnp}     onChange={e => onChange('cnp', e.target.value)} placeholder="1234567890123" maxLength={13} /></F>
      </div>
      <STitle>Contact</STitle>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <F label="Email">   <input className="input-field" type="email" value={form.email}   onChange={e => onChange('email',   e.target.value)} placeholder="pacient@email.ro" /></F>
        <F label="Telefon"> <input className="input-field"              value={form.telefon} onChange={e => onChange('telefon', e.target.value)} placeholder="07xx xxx xxx" /></F>
        <F label="Adresă" className="sm:col-span-2"><input className="input-field" value={form.adresa} onChange={e => onChange('adresa', e.target.value)} placeholder="Str. Exemplu nr. 1" /></F>
        <F label="Profesie">    <input className="input-field" value={form.profesie} onChange={e => onChange('profesie',  e.target.value)} placeholder="Inginer" /></F>
        <F label="Loc de muncă"><input className="input-field" value={form.locMunca} onChange={e => onChange('locMunca', e.target.value)} placeholder="SC Exemplu SRL" /></F>
      </div>
    </div>
  );
}

function StepMedicale({ form, onChange }) {
  return (
    <div className="space-y-4">
      <STitle>Antecedente & Istoric</STitle>
      <F label="Istoric Medical"><textarea className="input-field resize-none" rows={4} value={form.istoricMedical} onChange={e => onChange('istoricMedical', e.target.value)} placeholder="Hipertensiune diagnosticată în 2018..." /></F>
      <F label="Alergii">        <textarea className="input-field resize-none" rows={3} value={form.alergii}         onChange={e => onChange('alergii',         e.target.value)} placeholder="Penicilină..." /></F>
      <F label="Consultații Cardiologice Anterioare"><textarea className="input-field resize-none" rows={4} value={form.consultatiiCardio} onChange={e => onChange('consultatiiCardio', e.target.value)} placeholder="Ecocardiografie 01.2024 — FE 60%..." /></F>
    </div>
  );
}

function StepDispozitivLimite({ form, onChange, onLimitChange }) {
  const rows = [
    { label: 'Puls',        unit: 'bpm', minK: 'pulsMin', maxK: 'pulsMax' },
    { label: 'SpO₂',        unit: '%',   minK: 'spo2Min', maxK: 'spo2Max' },
    { label: 'Temperatură', unit: '°C',  minK: 'tempMin', maxK: 'tempMax' },
    { label: 'ECG',         unit: 'raw', minK: 'ecgMin',  maxK: 'ecgMax'  },
    { label: 'Umiditate',   unit: '%',   minK: 'umidMin', maxK: 'umidMax' },
  ];
  return (
    <div className="space-y-5">
      <div>
        <STitle>Dispozitiv Wearable</STitle>
        <p className="text-slate-500 text-sm mb-3">ID-ul dispozitivului ESP32 din calea <code className="bg-slate-100 px-1 rounded text-xs">device_data/{'{'}deviceId{'}'}</code></p>
        <F label="ID Dispozitiv ESP32">
          <input className="input-field font-mono" value={form.deviceId} onChange={e => onChange('deviceId', e.target.value)} placeholder="esp32_dispozitiv_01" />
        </F>
      </div>
      <div>
        <STitle>Limite Normale Personalizate</STitle>
        <p className="text-slate-500 text-sm mb-3">Valorile în afara acestor limite vor declanșa alerte automate.</p>
        <div className="space-y-3">
          {rows.map(r => (
            <div key={r.minK} className="grid grid-cols-3 items-center gap-3 p-3 bg-slate-50 rounded-xl">
              <span className="text-sm font-medium text-slate-700">{r.label} <span className="text-slate-400 font-normal">({r.unit})</span></span>
              <F label="Min"><input type="number" step="0.1" className="input-field" value={form.limiteSenzori[r.minK]} onChange={e => onLimitChange(r.minK, e.target.value)} /></F>
              <F label="Max"><input type="number" step="0.1" className="input-field" value={form.limiteSenzori[r.maxK]} onChange={e => onLimitChange(r.maxK, e.target.value)} /></F>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function STitle({ children }) { return <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">{children}</h3>; }
function F({ label, children, className = '' }) {
  return <div className={className}>{label && <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>}{children}</div>;
}
