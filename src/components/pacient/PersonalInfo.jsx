import React from 'react';

export default function PersonalInfo({ patient }) {
  if (!patient) return null;

  return (
    <div className="space-y-4">
      <Card title="Date Personale">
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <Field label="Prenume"      value={patient.prenume} />
          <Field label="Nume"         value={patient.nume} />
          <Field label="Vârstă"       value={patient.varsta ? `${patient.varsta} ani` : '—'} />
          <Field label="Telefon"      value={patient.telefon} />
          <Field label="Email"        value={patient.email} />
          <Field label="Profesie"     value={patient.profesie} />
          <Field label="Adresă"       value={patient.adresa}  className="col-span-2 sm:col-span-3" />
          <Field label="Loc de Muncă" value={patient.locMunca} className="col-span-2 sm:col-span-2" />
        </div>
      </Card>

      <Card title="Dosar Medical">
        <TextBlock label="Istoric Medical"            value={patient.istoricMedical} />
        <TextBlock label="Alergii"                    value={patient.alergii} />
        <TextBlock label="Consultații Cardiologice"   value={patient.consultatiiCardio} />
      </Card>

      {patient.limiteSenzori && (
        <Card title="Limite Normale Configurate de Medic">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { l: 'Puls Min',      v: `${patient.limiteSenzori.pulsMin} bpm`,  c: 'blue' },
              { l: 'Puls Max',      v: `${patient.limiteSenzori.pulsMax} bpm`,  c: 'blue' },
              { l: 'Temp Min',      v: `${patient.limiteSenzori.tempMin} °C`,   c: 'orange' },
              { l: 'Temp Max',      v: `${patient.limiteSenzori.tempMax} °C`,   c: 'orange' },
              { l: 'ECG Min',       v: `${patient.limiteSenzori.ecgMin} mV`,    c: 'indigo' },
              { l: 'ECG Max',       v: `${patient.limiteSenzori.ecgMax} mV`,    c: 'indigo' },
              { l: 'Umiditate Min', v: `${patient.limiteSenzori.umidMin} %`,    c: 'sky' },
              { l: 'Umiditate Max', v: `${patient.limiteSenzori.umidMax} %`,    c: 'sky' },
            ].map(item => (
              <LimitBadge key={item.l} label={item.l} value={item.v} colorKey={item.c} />
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}

const colorMap = {
  blue:   { bg: 'bg-blue-50',   text: 'text-blue-800',   sub: 'text-blue-500'   },
  orange: { bg: 'bg-orange-50', text: 'text-orange-800', sub: 'text-orange-500' },
  indigo: { bg: 'bg-indigo-50', text: 'text-indigo-800', sub: 'text-indigo-500' },
  sky:    { bg: 'bg-sky-50',    text: 'text-sky-800',    sub: 'text-sky-500'    },
};

function LimitBadge({ label, value, colorKey }) {
  const c = colorMap[colorKey] || colorMap.blue;
  return (
    <div className={`${c.bg} rounded-lg p-3`}>
      <p className={`text-xs font-medium ${c.sub}`}>{label}</p>
      <p className={`text-base font-bold ${c.text} mt-0.5`}>{value}</p>
    </div>
  );
}

function Card({ title, children }) {
  return (
    <div className="bg-white rounded-xl border border-slate-100 p-5">
      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">{title}</h3>
      {children}
    </div>
  );
}

function Field({ label, value, className = '' }) {
  return (
    <div className={className}>
      <p className="text-xs text-slate-400">{label}</p>
      <p className="text-sm font-medium text-slate-700 mt-0.5">{value || '—'}</p>
    </div>
  );
}

function TextBlock({ label, value }) {
  if (!value) return null;
  return (
    <div className="mb-4 last:mb-0">
      <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">{label}</p>
      <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{value}</p>
    </div>
  );
}
