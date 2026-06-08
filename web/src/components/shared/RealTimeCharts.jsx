import React, { useState, useEffect } from 'react';
import { ref, query, orderByChild, limitToLast, onValue } from 'firebase/database';
import {
  ResponsiveContainer, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine,
} from 'recharts';
import { db } from '../../firebase';
import LoadingSpinner from './LoadingSpinner';
import EcgWidget from './EcgWidget';

const MAX_POINTS = 30; // citiri pentru graficele de tendință

export default function RealTimeCharts({ deviceId, limiteSenzori }) {
  const [data,    setData]    = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!deviceId) { setLoading(false); return; }

    const q = query(
      ref(db, `device_data/${deviceId}/readings`),
      orderByChild('timestamp'),
      limitToLast(MAX_POINTS)
    );

    const unsub = onValue(q, (snap) => {
      const rows = [];
      snap.forEach(child => {
        const raw = child.val();

        // ── Timestamp: Unix seconds (simulator/ESP32) SAU ISO string (Android app) ──
        const tsRaw = raw.timestamp;
        const ts = typeof tsRaw === 'number'
          ? new Date(tsRaw * 1000)               // Unix seconds → ms
          : tsRaw ? new Date(tsRaw) : new Date(); // ISO string sau fallback

        // ── ECG: CSV string (simulator) SAU array (dispozitiv real) ──
        const ecgRaw = raw.sensors?.ecg_data ?? raw.ecg ?? raw.ecgData ?? null;
        let ecgArr = null;
        if (ecgRaw) {
          if (Array.isArray(ecgRaw)) {
            ecgArr = ecgRaw.map(Number).filter(n => !isNaN(n));
          } else {
            ecgArr = String(ecgRaw).split(',').map(Number).filter(n => !isNaN(n));
          }
          // Normalizăm valorile brute AD8232 (0-4095) la intervalul [-0.4, 1.5]
          if (ecgArr.length > 0 && ecgArr[0] > 10) {
            const mid = 2048;
            ecgArr = ecgArr.map(v => (v - mid) / mid);
          }
        }

        rows.push({
          id:          child.key,
          time:        ts.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
          timestamp:   ts,
          // BPM: suportă simulator (sensors.heart_rate_bpm) ȘI Android app real (heartRate / bpm)
          puls:        raw.sensors?.heart_rate_bpm ?? raw.heartRate ?? raw.bpm ?? null,
          // SpO2: suportă ambele formate
          spo2:        raw.sensors?.spo2_percent ?? raw.spo2 ?? null,
          // Temperatură: suportă ambele formate
          temperatura: raw.sensors?.temperature_c ?? raw.temperature ?? null,
          // Umiditate: suportă ambele formate
          umiditate:   raw.sensors?.humidity_percent ?? raw.humidity ?? null,
          ecgWaveform: ecgArr,
          leadsOff:    raw.hardware_status?.ecg_leads_off ?? raw.leadsOff ?? false,
          uptime:      raw.uptime_ms ?? null,
        });
      });
      setData(rows);
      setLoading(false);
    }, err => {
      console.error('Eroare onValue device_data:', err);
      setLoading(false);
    });

    return () => unsub();
  }, [deviceId]);

  // ecgBuffer este gestionat direct în EcgMonitor cu loop de animație

  if (!deviceId) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-slate-400">
        <span className="text-4xl mb-3">📡</span>
        <p className="font-medium">ID dispozitiv neconfigurat</p>
        <p className="text-sm mt-1">Medicul trebuie să seteze ID-ul dispozitivului ESP32 în fișa pacientului.</p>
      </div>
    );
  }

  if (loading) return <LoadingSpinner fullScreen={false} />;

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-slate-400">
        <span className="text-4xl mb-3">📶</span>
        <p className="font-medium">Nicio citire primită de la dispozitiv</p>
        <p className="text-sm mt-1">
          Porniți simulatorul: <code className="bg-slate-100 px-1 rounded">cd simulator && npm start</code>
        </p>
      </div>
    );
  }

  const lim    = limiteSenzori || {};
  const latest = data[data.length - 1];

  return (
    <div className="space-y-5">
      {/* Avertisment electrozi */}
      {latest?.leadsOff && (
        <div className="flex items-center gap-2 bg-amber-50 border border-amber-300 text-amber-800 text-sm px-4 py-3 rounded-xl">
          <span className="text-lg">⚠️</span>
          <span className="font-medium">Electrozii ECG sunt deconectați</span>
        </div>
      )}

      {/* Valori live */}
      <LiveValues latest={latest} lim={lim} />

      {/* ── Monitor ECG Canvas — stil spital ─────────────────────────────── */}
      <EcgWidget deviceId={deviceId} />

      {/* Puls */}
      <ChartCard title="Puls" subtitle="bpm" color="#ef4444"
        badge={latest?.puls != null ? `${latest.puls} bpm` : null}
        badgeColor={getBadgeColor(latest?.puls, lim.pulsMin, lim.pulsMax)}>
        <ResponsiveContainer width="100%" height={180}>
          <AreaChart data={data} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="gPuls" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="#ef4444" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <YAxis domain={['auto', 'auto']} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip content={<ChartTooltip unit="bpm" />} />
            {lim.pulsMin && <ReferenceLine y={lim.pulsMin} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Min', fontSize: 10, fill: '#fbbf24' }} />}
            {lim.pulsMax && <ReferenceLine y={lim.pulsMax} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Max', fontSize: 10, fill: '#fbbf24' }} />}
            <Area type="monotone" dataKey="puls" stroke="#ef4444" strokeWidth={2} fill="url(#gPuls)" dot={false} connectNulls />
          </AreaChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* SpO2 */}
      <ChartCard title="SpO₂ — Saturație Oxigen" subtitle="%" color="#8b5cf6"
        badge={latest?.spo2 != null ? `${latest.spo2} %` : null}
        badgeColor={getBadgeColor(latest?.spo2, lim.spo2Min, lim.spo2Max)}>
        <ResponsiveContainer width="100%" height={180}>
          <AreaChart data={data} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="gSpo2" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="#8b5cf6" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <YAxis domain={[85, 100]} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip content={<ChartTooltip unit="%" />} />
            {lim.spo2Min && <ReferenceLine y={lim.spo2Min} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Min', fontSize: 10, fill: '#fbbf24' }} />}
            <Area type="monotone" dataKey="spo2" stroke="#8b5cf6" strokeWidth={2} fill="url(#gSpo2)" dot={false} connectNulls />
          </AreaChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Temperatură */}
      <ChartCard title="Temperatură" subtitle="°C" color="#f97316"
        badge={latest?.temperatura != null ? `${latest.temperatura.toFixed(1)} °C` : null}
        badgeColor={getBadgeColor(latest?.temperatura, lim.tempMin, lim.tempMax)}>
        <ResponsiveContainer width="100%" height={180}>
          <AreaChart data={data} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="gTemp" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="#f97316" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#f97316" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <YAxis domain={['auto', 'auto']} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip content={<ChartTooltip unit="°C" />} />
            {lim.tempMin && <ReferenceLine y={lim.tempMin} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Min', fontSize: 10, fill: '#fbbf24' }} />}
            {lim.tempMax && <ReferenceLine y={lim.tempMax} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Max', fontSize: 10, fill: '#fbbf24' }} />}
            <Area type="monotone" dataKey="temperatura" stroke="#f97316" strokeWidth={2} fill="url(#gTemp)" dot={false} connectNulls />
          </AreaChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Umiditate */}
      <ChartCard title="Umiditate" subtitle="%" color="#0ea5e9"
        badge={latest?.umiditate != null ? `${latest.umiditate} %` : null}
        badgeColor={getBadgeColor(latest?.umiditate, lim.umidMin, lim.umidMax)}>
        <ResponsiveContainer width="100%" height={160}>
          <AreaChart data={data} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id="gUmid" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="#0ea5e9" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <YAxis domain={[0, 100]} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip content={<ChartTooltip unit="%" />} />
            <Area type="monotone" dataKey="umiditate" stroke="#0ea5e9" strokeWidth={2} fill="url(#gUmid)" dot={false} connectNulls />
          </AreaChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Info dispozitiv */}
      <div className="flex items-center gap-3 text-xs text-slate-400 bg-slate-50 rounded-xl px-4 py-2.5">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse flex-shrink-0" />
        <span>Dispozitiv: <strong className="text-slate-600">{deviceId}</strong></span>
        {latest?.uptime != null && (
          <span>· Uptime: <strong className="text-slate-600">{(latest.uptime / 1000).toFixed(0)}s</strong></span>
        )}
        <span className="ml-auto">{data.length} citiri afișate</span>
      </div>
    </div>
  );
}

/* EcgMonitor înlocuit cu EcgWidget (Canvas + Firebase propriu) — vezi EcgWidget.jsx */
function EcgMonitor_UNUSED({ data, heartRate }) {
  const DISPLAY_WIN   = 800;
  const TICK_MS       = 50;
  const SAMPLES_CYCLE = 100;

  // Template = un ciclu cardiac din ultima citire Firebase → se ciclăm la nesfârșit
  const templateRef   = useRef([]);
  const loopPhaseRef  = useRef(0);   // poziție curentă în template [0, SAMPLES_CYCLE)
  const heartRateRef  = useRef(heartRate || 72);
  const displayBufRef = useRef([]);
  const lastIdRef     = useRef(null);
  const [display, setDisplay] = useState([]);

  // Sincronizăm BPM-ul în ref ca să fie accesibil în closure-ul setInterval
  useEffect(() => {
    heartRateRef.current = heartRate || 72;
  }, [heartRate]);

  // Când sosește o citire nouă, extragem un ciclu cardiac ca template
  useEffect(() => {
    if (!data?.length) return;
    const latest = data[data.length - 1];
    if (!latest?.ecgWaveform?.length || latest.id === lastIdRef.current) return;
    lastIdRef.current = latest.id;
    // Primul ciclu (100 sample-uri) = template pentru loop infinit
    templateRef.current = latest.ecgWaveform.slice(0, SAMPLES_CYCLE);
  }, [data]);

  // Loop de animație — rulează continuu, NICIODATĂ nu se oprește
  useEffect(() => {
    const timer = setInterval(() => {
      if (templateRef.current.length === 0) return;

      const bpm = heartRateRef.current;
      // Câte sample-uri avansăm per tick, calculat din BPM:
      // (sample-uri/ciclu) × (cicluri/minut) / (60s/min) × (tick în secunde)
      const advance = Math.max(1, Math.round(SAMPLES_CYCLE * bpm / 60 * TICK_MS / 1000));

      // Punem `advance` sample-uri noi în buffer prin ciclarea template-ului
      for (let i = 0; i < advance; i++) {
        const idx = Math.floor(loopPhaseRef.current) % SAMPLES_CYCLE;
        displayBufRef.current.push(templateRef.current[idx]);
        loopPhaseRef.current = (loopPhaseRef.current + 1) % SAMPLES_CYCLE;
      }

      // Tăiem la fereastra de afișare
      if (displayBufRef.current.length > DISPLAY_WIN) {
        displayBufRef.current.splice(0, displayBufRef.current.length - DISPLAY_WIN);
      }

      setDisplay(displayBufRef.current.map((v, i) => ({ i, v })));
    }, TICK_MS);

    return () => clearInterval(timer);
  }, []); // rulează O SINGURĂ DATĂ — loop-ul este perpetuu

  const hasData = display.length > 0;

  return (
    <div className="rounded-2xl overflow-hidden border border-slate-800 shadow-xl">
      {/* Header */}
      <div className="bg-slate-900 px-4 py-2.5 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <span className={`w-2 h-2 rounded-full ${hasData ? 'bg-green-400 animate-pulse' : 'bg-slate-600'}`} />
          <span className="text-green-400 text-sm font-bold tracking-widest font-mono">ECG</span>
          <span className="text-slate-600 text-xs font-mono">· 25mm/s · 10mm/mV</span>
        </div>
        {heartRate != null && (
          <div className="flex items-center gap-2">
            <svg className="w-4 h-4 text-red-500 animate-pulse" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 21.593c-.525-.444-10.465-9.358-10.465-13.093C1.535 5.036 4.19 3 7.053 3c1.905 0 3.627.97 4.947 2.637C13.32 3.97 15.043 3 16.947 3 19.81 3 22.465 5.036 22.465 8.5c0 3.735-9.94 12.649-10.465 13.093z"/>
            </svg>
            <span className="text-red-400 text-2xl font-bold font-mono leading-none">{heartRate}</span>
            <span className="text-slate-500 text-xs font-mono">BPM</span>
          </div>
        )}
      </div>

      {/* Canvas ECG pe fundal întunecat cu grid ECG paper */}
      <div
        className="px-2 pt-3 pb-2"
        style={{
          background: '#050f05',
          backgroundImage: [
            // Linii majore (la fiecare 50px) — mai vizibile
            'repeating-linear-gradient(#0f2a0f 0px, #0f2a0f 1px, transparent 1px, transparent 50px)',
            'repeating-linear-gradient(90deg, #0f2a0f 0px, #0f2a0f 1px, transparent 1px, transparent 50px)',
            // Linii minore (la fiecare 10px) — foarte subtile
            'repeating-linear-gradient(#071407 0px, #071407 1px, transparent 1px, transparent 10px)',
            'repeating-linear-gradient(90deg, #071407 0px, #071407 1px, transparent 1px, transparent 10px)',
          ].join(', '),
        }}
      >
        {!hasData ? (
          <div className="h-[220px] flex items-center justify-center text-green-900 text-sm font-mono">
            Așteptare date ECG...
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={display} margin={{ top: 15, right: 5, left: -35, bottom: 5 }}>
              <XAxis dataKey="i" hide />
              <YAxis domain={[-0.4, 1.5]} hide />
              <Line
                type="linear"
                dataKey="v"
                stroke="#00e676"
                strokeWidth={1.5}
                dot={false}
                isAnimationActive={false}  // CRITIC: fără animație Recharts (o facem noi)
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}

/* ── Valori live ────────────────────────────────────────────────────────────── */
function LiveValues({ latest, lim }) {
  if (!latest) return null;
  const metrics = [
    { label: 'Puls',        value: latest.puls,        unit: 'bpm', min: lim.pulsMin, max: lim.pulsMax, color: '#ef4444' },
    { label: 'SpO₂',        value: latest.spo2,        unit: '%',   min: lim.spo2Min, max: lim.spo2Max, color: '#8b5cf6' },
    { label: 'Temperatură', value: latest.temperatura, unit: '°C',  min: lim.tempMin, max: lim.tempMax, color: '#f97316', dec: 1 },
    { label: 'Umiditate',   value: latest.umiditate,   unit: '%',   min: lim.umidMin, max: lim.umidMax, color: '#0ea5e9' },
  ];
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {metrics.map(m => {
        const bc      = getBadgeColor(m.value, m.min, m.max);
        const display = m.value != null ? (m.dec ? m.value.toFixed(m.dec) : m.value) : '—';
        return (
          <div key={m.label} className={`rounded-xl p-3 border
            ${bc === 'red' ? 'bg-red-50 border-red-200' : bc === 'amber' ? 'bg-amber-50 border-amber-200' : 'bg-white border-slate-100'}`}>
            <p className="text-xs font-medium text-slate-500 mb-1">{m.label}</p>
            <p className="text-2xl font-bold" style={{ color: m.color }}>{display}</p>
            <p className="text-xs text-slate-400">{m.unit}</p>
            {bc === 'red'   && <span className="mt-1 inline-block text-xs text-red-600 font-semibold">⚠ Alertă</span>}
            {bc === 'amber' && <span className="mt-1 inline-block text-xs text-amber-600 font-semibold">! Aproape de limită</span>}
          </div>
        );
      })}
    </div>
  );
}

/* ── Utilitare ──────────────────────────────────────────────────────────────── */
function ChartCard({ title, subtitle, color, badge, badgeColor, children }) {
  return (
    <div className="bg-white rounded-xl border border-slate-100 p-4">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full" style={{ background: color }} />
          <span className="text-sm font-semibold text-slate-700">{title}</span>
          <span className="text-xs text-slate-400">({subtitle})</span>
        </div>
        {badge && (
          <span className={`text-sm font-bold px-2.5 py-1 rounded-lg
            ${badgeColor === 'red'   ? 'bg-red-100 text-red-700'
            : badgeColor === 'amber' ? 'bg-amber-100 text-amber-700'
            : 'bg-emerald-100 text-emerald-700'}`}>
            {badge}
          </span>
        )}
      </div>
      {children}
    </div>
  );
}

function ChartTooltip({ active, payload, label, unit }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 text-sm shadow-lg">
      <p className="text-slate-400 text-xs mb-1">{label}</p>
      <p className="font-semibold text-slate-800">{payload[0]?.value} {unit}</p>
    </div>
  );
}

function getBadgeColor(value, min, max) {
  if (value == null) return 'neutral';
  if ((min != null && value < min) || (max != null && value > max)) return 'red';
  const m = 0.05;
  if (min != null && value < min * (1 + m)) return 'amber';
  if (max != null && value > max * (1 - m)) return 'amber';
  return 'green';
}
