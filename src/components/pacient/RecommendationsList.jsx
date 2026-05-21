import React from 'react';

const ACTIVITY_LABELS = {
  bicicleta: 'Bicicletă', alergat: 'Alergat',    plimbare: 'Plimbare',
  inot: 'Înot',           yoga: 'Yoga',           fitness: 'Fitness / Sală',
  stretching: 'Stretching', altele: 'Altele',
};

const ACTIVITY_ICONS  = {
  bicicleta: '🚴', alergat: '🏃',  plimbare: '🚶',
  inot: '🏊',      yoga: '🧘',     fitness: '🏋️',
  stretching: '🤸', altele: '🏅',
};

const ACTIVITY_COLORS = {
  bicicleta: 'bg-sky-50 border-sky-200',
  alergat:   'bg-red-50 border-red-200',
  plimbare:  'bg-emerald-50 border-emerald-200',
  inot:      'bg-blue-50 border-blue-200',
  yoga:      'bg-purple-50 border-purple-200',
  fitness:   'bg-orange-50 border-orange-200',
  stretching:'bg-teal-50 border-teal-200',
  altele:    'bg-slate-50 border-slate-200',
};

export default function RecommendationsList({ patient }) {
  const recs = patient?.recomandari || [];

  if (recs.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-100 p-8 text-center text-slate-400">
        <span className="text-4xl block mb-3">🩺</span>
        <p className="font-medium">Nicio recomandare înregistrată.</p>
        <p className="text-sm mt-1">Medicul dumneavoastră va adăuga recomandări în curând.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {recs.map((r, i) => (
        <div
          key={i}
          className={`rounded-xl border p-4 ${ACTIVITY_COLORS[r.tip] || ACTIVITY_COLORS.altele}`}
        >
          <div className="flex items-center gap-3">
            <span className="text-2xl">{ACTIVITY_ICONS[r.tip] || '🏅'}</span>
            <div className="flex-1">
              <div className="flex items-start justify-between gap-2">
                <p className="font-semibold text-slate-800">{ACTIVITY_LABELS[r.tip] || r.tip}</p>
                <span className="flex-shrink-0 text-sm font-bold text-emerald-700 bg-emerald-100 px-3 py-0.5 rounded-full">
                  {r.durataZilnica}
                </span>
              </div>
              {r.alteIndicatii && (
                <p className="text-sm text-slate-600 mt-1.5 leading-relaxed">{r.alteIndicatii}</p>
              )}
              {r.adaugatLa && (
                <p className="text-xs text-slate-400 mt-2">
                  Recomandat pe {new Date(r.adaugatLa).toLocaleDateString('ro-RO', { day: '2-digit', month: 'long', year: 'numeric' })}
                </p>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
