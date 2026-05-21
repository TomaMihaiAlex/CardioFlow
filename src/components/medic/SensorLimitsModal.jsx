import React, { useState, useEffect } from 'react';
import { ref, update } from 'firebase/database';
import { db } from '../../firebase';
import Modal from '../shared/Modal';

const SENSOR_ROWS = [
  { label: 'Puls',        unit: 'bpm', minK: 'pulsMin', maxK: 'pulsMax' },
  { label: 'SpO₂',        unit: '%',   minK: 'spo2Min', maxK: 'spo2Max' },
  { label: 'Temperatură', unit: '°C',  minK: 'tempMin', maxK: 'tempMax' },
  { label: 'ECG',         unit: 'raw', minK: 'ecgMin',  maxK: 'ecgMax'  },
  { label: 'Umiditate',   unit: '%',   minK: 'umidMin', maxK: 'umidMax' },
];

export default function SensorLimitsModal({ open, onClose, patient }) {
  const [limits,  setLimits]  = useState({});
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (patient?.limiteSenzori) setLimits({ ...patient.limiteSenzori });
  }, [patient]);

  const handleChange = (key, value) =>
    setLimits(l => ({ ...l, [key]: parseFloat(value) || 0 }));

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setSuccess(false);
    try {
      await update(ref(db, `pacienti/${patient.id}`), { limiteSenzori: limits });
      setSuccess(true);
      setTimeout(() => { setSuccess(false); onClose(); }, 1000);
    } catch (e) {
      setError('Eroare la salvare. Verificați regulile Realtime Database.');
      console.error(e);
    } finally {
      setSaving(false);
    }
  };

  if (!patient) return null;

  return (
    <Modal open={open} onClose={onClose} title={`Limite Senzori — ${patient.prenume} ${patient.nume}`} size="md">
      <p className="text-slate-500 text-sm mb-5">
        Câmpurile corespund valorilor din <code className="bg-slate-100 px-1 rounded text-xs">sensors.*</code> trimise de ESP32.
        Valorile în afara intervalului generează alerte.
      </p>
      <div className="space-y-3">
        {SENSOR_ROWS.map(r => (
          <div key={r.minK} className="grid grid-cols-3 items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
            <span className="text-sm font-medium text-slate-700">
              {r.label}<span className="text-slate-400 font-normal text-xs ml-1">({r.unit})</span>
            </span>
            {[r.minK, r.maxK].map((k, i) => (
              <label key={k} className="flex flex-col gap-1">
                <span className="text-xs text-slate-500">{i === 0 ? 'Min' : 'Max'}</span>
                <input type="number" step="0.1" value={limits[k] ?? ''}
                  onChange={e => handleChange(k, e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                />
              </label>
            ))}
          </div>
        ))}
      </div>

      {error   && <p className="mt-3 text-red-600 text-sm">{error}</p>}
      {success && <p className="mt-3 text-emerald-600 text-sm font-medium">Salvat cu succes!</p>}

      <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-slate-100">
        <button onClick={onClose} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg">Anulează</button>
        <button onClick={handleSave} disabled={saving}
          className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg disabled:opacity-60">
          {saving ? 'Se salvează...' : 'Salvează Limitele'}
        </button>
      </div>
    </Modal>
  );
}
