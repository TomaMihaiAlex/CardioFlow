import React, { useState, useEffect } from 'react';
import { ref, remove, query, orderByChild, limitToLast, onValue, update } from 'firebase/database';
import { db } from '../../firebase';
import RealTimeCharts        from '../shared/RealTimeCharts';
import EcgStreamChart        from './EcgStreamChart';
import RecommendationsModal  from './RecommendationsModal';
import SensorLimitsModal     from './SensorLimitsModal';

const TABS = ['Fișă', 'Recomandări', 'Grafice 30s', 'ECG Live', 'Alerte'];

const ACTIVITY_LABELS = {
  bicicleta:'Bicicletă', alergat:'Alergat', plimbare:'Plimbare',
  inot:'Înot', yoga:'Yoga', fitness:'Fitness', stretching:'Stretching', altele:'Altele',
};

export default function PatientDetail({ patient, onDeleted }) {
  const [tab,          setTab]          = useState(0);
  const [alerts,       setAlerts]       = useState([]);
  const [showRecModal, setShowRecModal] = useState(false);
  const [showLimModal, setShowLimModal] = useState(false);
  const [deleting,     setDeleting]     = useState(false);

  useEffect(() => {
    if (!patient?.id) return;
    const q = query(ref(db, `alerme/${patient.id}`), orderByChild('timestamp'), limitToLast(50));
    const unsub = onValue(q, (snap) => {
      const list = [];
      snap.forEach(child => list.push({ id: child.key, ...child.val() }));
      list.reverse();
      setAlerts(list);
    });
    return () => unsub();
  }, [patient?.id]);

  const handleDelete = async () => {
    if (!window.confirm(`Ștergeți pacientul ${patient.prenume} ${patient.nume}?`)) return;
    setDeleting(true);
    try {
      await remove(ref(db, `pacienti/${patient.id}`));
      onDeleted();
    } catch (e) { alert('Eroare la ștergere.'); console.error(e); }
    finally    { setDeleting(false); }
  };

  const handleResolveAlert = async (alertId) => {
    try { await update(ref(db, `alerme/${patient.id}/${alertId}`), { status: 'rezolvata' }); }
    catch (e) { console.error(e); }
  };

  if (!patient) return null;
  const deviceId    = patient.deviceId || null;
  const activeAlerts = alerts.filter(a => a.status === 'activa').length;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-start justify-between px-5 py-4 border-b border-slate-100 bg-white flex-shrink-0">
        <div>
          <h2 className="text-lg font-bold text-slate-800">{patient.prenume} {patient.nume}</h2>
          <div className="flex items-center gap-2 mt-1 flex-wrap text-sm text-slate-500">
            {patient.varsta && <span>{patient.varsta} ani</span>}
            {patient.cnp    && <><Dot /><span>CNP: {patient.cnp}</span></>}
            {deviceId && (
              <><Dot />
              <span className="font-mono text-xs bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                📡 {deviceId}
              </span></>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => setShowLimModal(true)} className="btn-sm blue">Limite</button>
          <button onClick={() => setShowRecModal(true)} className="btn-sm green">+ Rec.</button>
          <button onClick={handleDelete} disabled={deleting} className="btn-sm red">Șterge</button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-100 bg-white px-5 flex-shrink-0">
        {TABS.map((t, i) => (
          <button key={i} onClick={() => setTab(i)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors whitespace-nowrap
              ${tab === i ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
            {t}
            {i === 4 && activeAlerts > 0 && (
              <span className="ml-1 inline-flex items-center justify-center w-4 h-4 bg-red-500 text-white text-xs rounded-full">
                {activeAlerts}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-5 bg-slate-50">
        {tab === 0 && <TabFisa patient={patient} />}
        {tab === 1 && <TabRecs patient={patient} onAdd={() => setShowRecModal(true)} />}
        {tab === 2 && (
          <RealTimeCharts deviceId={deviceId} limiteSenzori={patient.limiteSenzori} />
        )}
        {tab === 3 && (
          deviceId
            ? <EcgStreamChart deviceId={deviceId} />
            : <NoDevice />
        )}
        {tab === 4 && <TabAlerte alerts={alerts} onResolve={handleResolveAlert} />}
      </div>

      <RecommendationsModal open={showRecModal} onClose={() => setShowRecModal(false)} patient={patient} />
      <SensorLimitsModal    open={showLimModal} onClose={() => setShowLimModal(false)} patient={patient} />

      <style>{`
        .btn-sm{padding:.25rem .75rem;border-radius:.5rem;font-size:.75rem;font-weight:600;transition:colors .15s;}
        .btn-sm.blue{color:#2563eb;background:#eff6ff;}.btn-sm.blue:hover{background:#dbeafe;}
        .btn-sm.green{color:#059669;background:#ecfdf5;}.btn-sm.green:hover{background:#d1fae5;}
        .btn-sm.red{color:#dc2626;}.btn-sm.red:hover{background:#fee2e2;}
      `}</style>
    </div>
  );
}

/* ── Tabs content ─────────────────────────────────────────────────────────── */
function TabFisa({ patient: p }) {
  return (
    <div className="space-y-4">
      <Card title="Date Personale">
        <Grid2>
          <Row l="Prenume"       v={p.prenume} />
          <Row l="Nume"          v={p.nume} />
          <Row l="Vârstă"        v={p.varsta ? `${p.varsta} ani` : null} />
          <Row l="CNP"           v={p.cnp} />
          <Row l="Telefon"       v={p.telefon} />
          <Row l="Email"         v={p.email} />
          <Row l="Adresă"        v={p.adresa}   cls="col-span-2" />
          <Row l="Profesie"      v={p.profesie} />
          <Row l="Loc de muncă"  v={p.locMunca} />
          <Row l="ID Dispozitiv" v={p.deviceId} mono />
        </Grid2>
      </Card>
      <Card title="Date Medicale">
        <Block l="Anamneză / Istoric Medical"   v={p.istoricMedical || p.anamneza} />
        <Block l="Alergii"                       v={p.alergii} />
        <Block l="Diagnostic Curent"             v={p.diagnosticCurent || p.consultatiiCardio} />
      </Card>
      {p.limiteSenzori && (
        <Card title="Limite Senzori Configurate">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              ['Puls Min',  `${p.limiteSenzori.pulsMin} bpm`],
              ['Puls Max',  `${p.limiteSenzori.pulsMax} bpm`],
              ['SpO₂ Min',  `${p.limiteSenzori.spo2Min} %`],
              ['SpO₂ Max',  `${p.limiteSenzori.spo2Max} %`],
              ['Temp Min',  `${p.limiteSenzori.tempMin} °C`],
              ['Temp Max',  `${p.limiteSenzori.tempMax} °C`],
              ['Umid. Min', `${p.limiteSenzori.umidMin} %`],
              ['Umid. Max', `${p.limiteSenzori.umidMax} %`],
            ].map(([l, v]) => (
              <div key={l} className="bg-blue-50 rounded-lg p-3">
                <p className="text-xs text-blue-500 font-medium">{l}</p>
                <p className="text-base font-bold text-blue-800 mt-0.5">{v}</p>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}

function TabRecs({ patient, onAdd }) {
  const recs = patient.recomandari || [];
  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-slate-700">Recomandări ({recs.length})</h3>
        <button onClick={onAdd} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg">+ Adaugă</button>
      </div>
      {recs.length === 0
        ? <Empty text="Nicio recomandare." />
        : <div className="space-y-3">{recs.map((r, i) => (
            <div key={i} className="bg-white rounded-xl border border-slate-100 p-4">
              <div className="flex items-center gap-3 mb-1">
                <span className="text-xl">{activityIcon(r.tip)}</span>
                <span className="font-semibold text-slate-700">{ACTIVITY_LABELS[r.tip] || r.tip}</span>
                <span className="ml-auto text-sm text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">{r.durataZilnica}</span>
              </div>
              {r.alteIndicatii && <p className="text-sm text-slate-500 mt-2">{r.alteIndicatii}</p>}
            </div>
          ))}</div>
      }
    </div>
  );
}

function TabAlerte({ alerts, onResolve }) {
  return (
    <div>
      <h3 className="font-semibold text-slate-700 mb-4">Alerte ({alerts.length})</h3>
      {alerts.length === 0 ? <Empty text="Nicio alertă." /> : (
        <div className="space-y-2">
          {alerts.map(a => (
            <div key={a.id} className={`flex items-start justify-between gap-4 p-3.5 rounded-xl border text-sm
              ${a.status === 'activa' ? 'bg-red-50 border-red-200' : 'bg-slate-50 border-slate-100'}`}>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className={`font-semibold ${a.status === 'activa' ? 'text-red-700' : 'text-slate-600'}`}>{a.tipAlerta}</span>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${a.status === 'activa' ? 'bg-red-100 text-red-700' : 'bg-slate-200 text-slate-500'}`}>{a.status}</span>
                </div>
                <p className="text-slate-500 mt-1">{a.textAsociat}</p>
                <p className="text-slate-300 text-xs mt-1">{a.timestamp ? new Date(a.timestamp).toLocaleString('ro-RO') : '—'}</p>
              </div>
              {a.status === 'activa' && (
                <button onClick={() => onResolve(a.id)} className="flex-shrink-0 text-xs px-3 py-1.5 bg-white border border-slate-200 text-slate-600 hover:bg-slate-100 rounded-lg">Rezolvă</button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function NoDevice() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-slate-400">
      <span className="text-5xl mb-4">📡</span>
      <p className="font-medium">Niciun dispozitiv asociat</p>
      <p className="text-sm mt-1">Setați un ID dispozitiv ESP32 în fișa pacientului.</p>
    </div>
  );
}

/* ── Micro-componente ─────────────────────────────────────────────────────── */
const Dot  = () => <span className="w-1 h-1 bg-slate-300 rounded-full" />;
const Card = ({ title, children }) => (
  <div className="bg-white rounded-xl border border-slate-100 p-4">
    <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">{title}</h4>
    {children}
  </div>
);
const Grid2 = ({ children }) => <div className="grid grid-cols-2 gap-x-8 gap-y-2">{children}</div>;
const Row   = ({ l, v, cls = '', mono }) => (
  <div className={cls}>
    <span className="text-xs text-slate-400">{l}</span>
    <p className={`text-sm font-medium text-slate-700 mt-0.5 ${mono ? 'font-mono text-xs' : ''}`}>{v || '—'}</p>
  </div>
);
const Block = ({ l, v }) => !v ? null : (
  <div className="mb-3 last:mb-0">
    <span className="text-xs text-slate-400 font-medium uppercase tracking-wide">{l}</span>
    <p className="text-sm text-slate-600 mt-1 leading-relaxed whitespace-pre-wrap">{v}</p>
  </div>
);
const Empty = ({ text }) => (
  <div className="text-center py-10 text-slate-400">
    <svg className="w-10 h-10 mx-auto mb-2 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
    <p className="text-sm">{text}</p>
  </div>
);
const activityIcon = t => ({bicicleta:'🚴',alergat:'🏃',plimbare:'🚶',inot:'🏊',yoga:'🧘',fitness:'🏋️',stretching:'🤸'}[t]||'🏅');
