import React, { useState } from 'react';
import { usePatientState, PS } from '../hooks/usePatientState';
import Navbar              from '../components/shared/Navbar';
import LoadingSpinner      from '../components/shared/LoadingSpinner';
import PersonalInfo        from '../components/pacient/PersonalInfo';
import RecommendationsList from '../components/pacient/RecommendationsList';
import RealTimeCharts      from '../components/shared/RealTimeCharts';
import AlertHistory        from '../components/pacient/AlertHistory';
import BookAppointment     from '../components/pacient/BookAppointment';

const TABS = ['Fișă Personală', 'Recomandări', 'Grafice Real-time', 'Alerte'];

export default function PacientDashboard() {
  const { state, activeRecord, appointmentRequest, userData } = usePatientState();
  const [tab, setTab] = useState(0);

  if (state === PS.LOADING) return <LoadingSpinner />;

  // ── Stări pre-monitorizare: fără fișă / cerere în curs ────────────────────
  if (state === PS.NO_RECORD || state === PS.REQUEST_PENDING || state === PS.REQUEST_SCHEDULED) {
    return (
      <div className="flex flex-col h-screen bg-slate-50">
        <Navbar title="Panou Pacient" />

        {/* Banner stare curentă */}
        {state !== PS.NO_RECORD && (
          <StatusBanner state={state} request={appointmentRequest} />
        )}

        <div className="flex-1 overflow-y-auto p-5">
          <BookAppointment
            appointmentRequest={appointmentRequest}
            state={state}
          />
        </div>
      </div>
    );
  }

  // ── Stare monitorizare activă ─────────────────────────────────────────────
  const patient  = activeRecord;
  const deviceId = patient?.deviceId || userData?.patientId || null;

  return (
    <div className="flex flex-col h-screen bg-slate-50">
      <Navbar title="Panou Pacient" />

      <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4">
        <p className="text-blue-200 text-sm">Bun venit,</p>
        <h1 className="text-white text-xl font-bold">{patient?.prenume} {patient?.nume}</h1>
        <div className="flex items-center gap-3 mt-1">
          <span className="flex items-center gap-1.5 text-blue-200 text-xs">
            <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse" />
            Monitorizare activă
          </span>
          {deviceId && (
            <span className="text-xs bg-white/10 text-blue-100 px-2 py-0.5 rounded font-mono">
              📡 {deviceId}
            </span>
          )}
        </div>
      </div>

      <div className="flex border-b border-slate-200 bg-white px-4 overflow-x-auto">
        {TABS.map((t, i) => (
          <button key={i} onClick={() => setTab(i)}
            className={`px-5 py-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors flex-shrink-0
              ${tab === i ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
            {t}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-5">
        <div className="max-w-4xl mx-auto">
          {tab === 0 && <PersonalInfo        patient={patient} />}
          {tab === 1 && <RecommendationsList patient={patient} />}
          {tab === 2 && <RealTimeCharts deviceId={deviceId} limiteSenzori={patient?.limiteSenzori} />}
          {tab === 3 && patient && <AlertHistory pacientId={patient.id} />}
        </div>
      </div>
    </div>
  );
}

function StatusBanner({ state, request }) {
  const cfg = {
    [PS.REQUEST_PENDING]:   { bg: 'bg-amber-500', text: '⏳ Cererea ta este în procesare la recepție.' },
    [PS.REQUEST_SCHEDULED]: {
      bg: 'bg-blue-500',
      text: `📅 Programare confirmată${request?.scheduledSlot ? ` — ${request.scheduledSlot.date} la ${request.scheduledSlot.time}` : ''}${request?.medicNume ? ` cu ${request.medicNume}` : ''}.`,
    },
  }[state];

  if (!cfg) return null;
  return (
    <div className={`${cfg.bg} text-white text-sm px-5 py-2.5 font-medium`}>
      {cfg.text}
    </div>
  );
}
