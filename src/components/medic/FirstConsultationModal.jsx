import React, { useState, useEffect } from 'react';
import { ref, get } from 'firebase/database';
import { db } from '../../firebase';
import { useAuth } from '../../hooks/useAuth';
import { activatePatient } from '../../utils/activatePatient';
import Modal from '../shared/Modal';

const DEFAULT_THRESHOLDS = {
  pulsMin: 50,   pulsMax: 120,
  spo2Min: 92,   spo2Max: 100,
  tempMin: 36.0, tempMax: 38.5,
  ecgMin: -500,  ecgMax: 500,
  umidMin: 20,   umidMax: 80,
};

const STEPS = ['Date Pacient', 'Dosar Medical', 'Dispozitiv ESP32', 'Praguri Alarmă'];

export default function FirstConsultationModal({ open, onClose, appointmentRequest }) {
  const { user } = useAuth();
  const [step,       setStep]       = useState(0);
  const [saving,     setSaving]     = useState(false);
  const [error,      setError]      = useState('');
  const [patientData, setPatientData] = useState({ prenume: '', nume: '', varsta: '', telefon: '', email: '' });
  const [medicalData, setMedicalData] = useState({ anamneza: '', alergii: '', diagnosticCurent: '' });
  const [deviceId,   setDeviceId]   = useState('');
  const [devStatus,  setDevStatus]  = useState('idle'); // idle | checking | found | paired_other | not_found
  const [thresholds, setThresholds] = useState({ ...DEFAULT_THRESHOLDS });

  // Pre-completăm cu datele din cererea de programare
  useEffect(() => {
    if (open && appointmentRequest) {
      setPatientData({
        prenume: appointmentRequest.prenume || '',
        nume:    appointmentRequest.nume    || '',
        varsta:  appointmentRequest.varsta  ? String(appointmentRequest.varsta) : '',
        telefon: appointmentRequest.telefon || '',
        email:   appointmentRequest.email   || '',
      });
      setMedicalData(m => ({
        ...m,
        anamneza: appointmentRequest.symptoms || appointmentRequest.descriereProblema || '',
      }));
    }
    if (!open) { setStep(0); setError(''); setDevStatus('idle'); setDeviceId(''); }
  }, [open, appointmentRequest?.id]);

  const setPD = (k, v) => setPatientData(p => ({ ...p, [k]: v }));
  const setMD = (k, v) => setMedicalData(m => ({ ...m, [k]: v }));
  const setTh = (k, v) => setThresholds(t => ({ ...t, [k]: parseFloat(v) || 0 }));

  const validateStep = () => {
    if (step === 0) {
      if (!patientData.prenume.trim() || !patientData.nume.trim()) return 'Numele este obligatoriu.';
      if (!patientData.email.trim())  return 'Email-ul este obligatoriu.';
    }
    if (step === 2 && devStatus !== 'found') return 'Verificați și confirmați dispozitivul ESP32.';
    return '';
  };

  const nextStep = () => {
    const err = validateStep();
    if (err) { setError(err); return; }
    setError('');
    setStep(s => s + 1);
  };

  const checkDevice = async () => {
    if (!deviceId.trim()) return;
    setDevStatus('checking');
    try {
      const [dataSnap, pairingSnap] = await Promise.all([
        get(ref(db, `device_data/${deviceId.trim()}`)),
        get(ref(db, `devicePairings/${deviceId.trim()}`)),
      ]);
      if (pairingSnap.exists()) {
        const p = pairingSnap.val();
        const ownUid = appointmentRequest?.pacientUid;
        // Dispozitiv deja asociat altui pacient
        setDevStatus(p.patientUid && p.patientUid !== ownUid ? 'paired_other' : 'found');
      } else if (dataSnap.exists()) {
        setDevStatus('found');
      } else {
        setDevStatus('not_found');
      }
    } catch { setDevStatus('not_found'); }
  };

  const handleActivate = async () => {
    setSaving(true);
    setError('');
    try {
      await activatePatient({
        patientUid:  appointmentRequest?.pacientUid,
        medicId:     user.uid,
        requestId:   appointmentRequest?.id || null,
        patientData,
        medicalData,
        deviceId:    deviceId.trim(),
        thresholds,
      });
      onClose();
    } catch (e) {
      setError('Eroare la activare. Verificați conexiunea și regulile Realtime Database.');
      console.error(e);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Prima Consultație — Activare Pacient" size="lg">
      {/* Stepper */}
      <div className="flex items-center gap-2 mb-7">
        {STEPS.map((label, i) => (
          <React.Fragment key={i}>
            <div className={`flex items-center gap-2 ${i > step ? 'opacity-40' : ''}`}>
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0
                ${i < step  ? 'bg-emerald-500 text-white'
                : i === step ? 'bg-blue-600 text-white'
                :              'bg-slate-200 text-slate-500'}`}>
                {i < step ? '✓' : i + 1}
              </div>
              <span className={`text-sm font-medium hidden md:inline
                ${i === step ? 'text-blue-700' : 'text-slate-400'}`}>
                {label}
              </span>
            </div>
            {i < STEPS.length - 1 && <div className="flex-1 h-px bg-slate-200" />}
          </React.Fragment>
        ))}
      </div>

      {/* Step 1 — Date pacient */}
      {step === 0 && (
        <div className="space-y-4">
          <SectionTitle>Date Personale</SectionTitle>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <F label="Prenume *"><input className="ifield" value={patientData.prenume} onChange={e => setPD('prenume', e.target.value)} placeholder="Ion" /></F>
            <F label="Nume *">   <input className="ifield" value={patientData.nume}    onChange={e => setPD('nume',    e.target.value)} placeholder="Popescu" /></F>
            <F label="Vârstă">   <input className="ifield" type="number" value={patientData.varsta}  onChange={e => setPD('varsta',  e.target.value)} placeholder="72" /></F>
            <F label="Telefon">  <input className="ifield" value={patientData.telefon} onChange={e => setPD('telefon', e.target.value)} placeholder="07xx xxx xxx" /></F>
            <F label="Email *" className="sm:col-span-2">
              <input className="ifield" type="email" value={patientData.email} onChange={e => setPD('email', e.target.value)} placeholder="pacient@email.ro" />
            </F>
          </div>
        </div>
      )}

      {/* Step 2 — Dosar medical */}
      {step === 1 && (
        <div className="space-y-4">
          <SectionTitle>Antecedente & Dosar</SectionTitle>
          <F label="Anamneză / Simptome (pre-completat din cerere)">
            <textarea className="ifield resize-none" rows={5}
              value={medicalData.anamneza}
              onChange={e => setMD('anamneza', e.target.value)}
              placeholder="Simptome descrise de pacient, istoricul bolii actuale..."
            />
          </F>
          <F label="Alergii">
            <textarea className="ifield resize-none" rows={3}
              value={medicalData.alergii}
              onChange={e => setMD('alergii', e.target.value)}
              placeholder="Penicilină, aspirină..."
            />
          </F>
          <F label="Diagnostic Curent">
            <input className="ifield" value={medicalData.diagnosticCurent}
              onChange={e => setMD('diagnosticCurent', e.target.value)}
              placeholder="Fibrilație atrială paroxistică, HTA grad II..."
            />
          </F>
        </div>
      )}

      {/* Step 3 — Asociere dispozitiv */}
      {step === 2 && (
        <div className="space-y-5">
          <SectionTitle>Asociere Hardware ESP32</SectionTitle>
          <p className="text-slate-500 text-sm">
            Introduceți ID-ul dispozitivului din calea Firebase{' '}
            <code className="bg-slate-100 px-1.5 py-0.5 rounded text-xs">device_data/{'{'}deviceId{'}'}</code>
          </p>

          <div className="flex gap-2">
            <input
              className="ifield flex-1 font-mono"
              value={deviceId}
              onChange={e => { setDeviceId(e.target.value); setDevStatus('idle'); }}
              placeholder="esp32_dispozitiv_01"
              onKeyDown={e => e.key === 'Enter' && checkDevice()}
            />
            <button
              onClick={checkDevice}
              disabled={!deviceId.trim() || devStatus === 'checking'}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white text-sm font-semibold rounded-lg disabled:opacity-50 whitespace-nowrap"
            >
              {devStatus === 'checking' ? '...' : 'Verifică'}
            </button>
          </div>

          {/* Status verificare */}
          {devStatus === 'found' && (
            <div className="flex items-center gap-3 bg-emerald-50 border border-emerald-200 rounded-xl p-4">
              <span className="text-2xl">✅</span>
              <div>
                <p className="font-semibold text-emerald-800">Dispozitiv găsit și disponibil</p>
                <p className="text-xs text-emerald-600 mt-0.5 font-mono">{deviceId}</p>
              </div>
            </div>
          )}
          {devStatus === 'not_found' && (
            <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl p-4">
              <span className="text-2xl">❌</span>
              <div>
                <p className="font-semibold text-red-800">Dispozitiv negăsit în Firebase</p>
                <p className="text-xs text-red-600 mt-0.5">Verificați că ESP32-ul a trimis cel puțin o citire.</p>
              </div>
            </div>
          )}
          {devStatus === 'paired_other' && (
            <div className="flex items-center gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4">
              <span className="text-2xl">⚠️</span>
              <div>
                <p className="font-semibold text-amber-800">Dispozitiv asociat altui pacient</p>
                <p className="text-xs text-amber-600 mt-0.5">Dezasociați dispozitivul din fișa anterioară înainte de reasignare.</p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Step 4 — Praguri alarmă */}
      {step === 3 && (
        <div className="space-y-4">
          <SectionTitle>Praguri Personalizate de Alarmă</SectionTitle>
          <p className="text-slate-500 text-sm">
            Valorile în afara intervalelor vor genera alerte instant (asincron) din aplicația Android.
          </p>
          <div className="space-y-3">
            {[
              { label: 'Puls',        unit: 'bpm', mk: 'pulsMin', xk: 'pulsMax' },
              { label: 'SpO₂',        unit: '%',   mk: 'spo2Min', xk: 'spo2Max' },
              { label: 'Temperatură', unit: '°C',  mk: 'tempMin', xk: 'tempMax' },
              { label: 'ECG',         unit: 'raw', mk: 'ecgMin',  xk: 'ecgMax'  },
              { label: 'Umiditate',   unit: '%',   mk: 'umidMin', xk: 'umidMax' },
            ].map(r => (
              <div key={r.mk} className="grid grid-cols-3 items-center gap-3 p-3 bg-slate-50 rounded-xl">
                <span className="text-sm font-medium text-slate-700">
                  {r.label} <span className="text-slate-400 font-normal text-xs">({r.unit})</span>
                </span>
                <F label="Min">
                  <input type="number" step="0.1" className="ifield" value={thresholds[r.mk]} onChange={e => setTh(r.mk, e.target.value)} />
                </F>
                <F label="Max">
                  <input type="number" step="0.1" className="ifield" value={thresholds[r.xk]} onChange={e => setTh(r.xk, e.target.value)} />
                </F>
              </div>
            ))}
          </div>

          {/* Sumar activare */}
          <div className="mt-4 bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm">
            <p className="font-semibold text-blue-800 mb-2">La confirmare se vor executa atomic:</p>
            <ul className="text-blue-700 space-y-1 text-xs list-disc list-inside">
              <li>Creare fișă medicală (medicalRecords)</li>
              <li>Asociere dispozitiv <span className="font-mono">{deviceId}</span> (devicePairings)</li>
              <li>Salvare praguri alarmă (sensorThresholds)</li>
              <li>Actualizare profil pacient (users)</li>
              {appointmentRequest?.id && <li>Marcare cerere ca activă (appointmentRequests)</li>}
            </ul>
          </div>
        </div>
      )}

      {error && (
        <div className="mt-4 text-red-600 text-sm bg-red-50 border border-red-200 rounded-lg px-4 py-2.5">{error}</div>
      )}

      {/* Navigare */}
      <div className="flex justify-between mt-6 pt-4 border-t border-slate-100">
        <button
          onClick={() => step === 0 ? onClose() : setStep(s => s - 1)}
          className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg"
        >
          {step === 0 ? 'Anulează' : '← Înapoi'}
        </button>

        {step < STEPS.length - 1 ? (
          <button onClick={nextStep} className="btn-activate">Continuă →</button>
        ) : (
          <button onClick={handleActivate} disabled={saving} className="btn-activate disabled:opacity-60">
            {saving ? 'Se activează...' : '✅ Activează Pacientul'}
          </button>
        )}
      </div>

      <style>{`
        .ifield{width:100%;padding:.5rem .75rem;border:1px solid #e2e8f0;border-radius:.5rem;font-size:.875rem;color:#1e293b;background:#f8fafc;outline:none;transition:all .15s;}
        .ifield:focus{background:white;box-shadow:0 0 0 2px #3b82f6;border-color:transparent;}
        .btn-activate{padding:.5rem 1.25rem;background:#2563eb;color:white;border-radius:.5rem;font-size:.875rem;font-weight:600;cursor:pointer;transition:background .15s;}
        .btn-activate:hover{background:#1d4ed8;}
      `}</style>
    </Modal>
  );
}

function SectionTitle({ children }) {
  return <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">{children}</h3>;
}
function F({ label, children, className = '' }) {
  return (
    <div className={className}>
      {label && <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>}
      {children}
    </div>
  );
}
