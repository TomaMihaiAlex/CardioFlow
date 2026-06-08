import React, { useState } from 'react';
import { ref, push, set } from 'firebase/database';
import { db } from '../../firebase';
import Modal from '../shared/Modal';

const ACTIVITY_TYPES = [
  { value: 'bicicleta',  label: 'Bicicletă' },
  { value: 'alergat',    label: 'Alergat' },
  { value: 'plimbare',   label: 'Plimbare' },
  { value: 'inot',       label: 'Înot' },
  { value: 'yoga',       label: 'Yoga' },
  { value: 'fitness',    label: 'Fitness / Sală' },
  { value: 'stretching', label: 'Stretching' },
  { value: 'altele',     label: 'Altele' },
];

const INITIAL = { tip: 'plimbare', durataZilnica: '', alteIndicatii: '' };

export default function RecommendationsModal({ open, onClose, patient }) {
  const [form,   setForm]   = useState(INITIAL);
  const [saving, setSaving] = useState(false);
  const [error,  setError]  = useState('');

  const handleSave = async () => {
    if (!form.durataZilnica) { setError('Introduceți durata zilnică.'); return; }
    setSaving(true);
    setError('');
    try {
      // push() adaugă un nod nou sub /pacienti/{id}/recomandari cu key auto-generat
      const newRef = push(ref(db, `pacienti/${patient.id}/recomandari`));
      await set(newRef, {
        tip:           form.tip,
        durataZilnica: form.durataZilnica.trim(),
        alteIndicatii: form.alteIndicatii.trim(),
        adaugatLa:     new Date().toISOString(),
      });
      setForm(INITIAL);
      onClose();
    } catch (e) {
      setError('Eroare la salvare. Verificați regulile Realtime Database.');
      console.error(e);
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => { setForm(INITIAL); setError(''); onClose(); };

  if (!patient) return null;

  return (
    <Modal open={open} onClose={handleClose} title={`Adaugă Recomandare — ${patient.prenume} ${patient.nume}`} size="sm">
      <div className="space-y-4">
        <FormField label="Tip Activitate">
          <select
            value={form.tip}
            onChange={e => setForm(f => ({ ...f, tip: e.target.value }))}
            className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {ACTIVITY_TYPES.map(a => <option key={a.value} value={a.value}>{a.label}</option>)}
          </select>
        </FormField>
        <FormField label="Durată Zilnică">
          <input
            type="text" value={form.durataZilnica}
            onChange={e => setForm(f => ({ ...f, durataZilnica: e.target.value }))}
            placeholder="ex: 30 minute, 5 km, 1 oră"
            className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </FormField>
        <FormField label="Alte Indicații">
          <textarea
            rows={4} value={form.alteIndicatii}
            onChange={e => setForm(f => ({ ...f, alteIndicatii: e.target.value }))}
            placeholder="Ritm moderat, evitați pantele abrupte..."
            className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
          />
        </FormField>
      </div>
      {error && <p className="mt-3 text-red-600 text-sm">{error}</p>}
      <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-slate-100">
        <button onClick={handleClose} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg">Anulează</button>
        <button onClick={handleSave} disabled={saving} className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg disabled:opacity-60">
          {saving ? 'Se salvează...' : 'Adaugă Recomandare'}
        </button>
      </div>
    </Modal>
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
