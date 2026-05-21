import React, { useState, useEffect } from 'react';
import { ref, query, orderByChild, limitToLast, onValue } from 'firebase/database';
import {
  ResponsiveContainer, LineChart, Line, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine,
} from 'recharts';
import { db } from '../../firebase';
import LoadingSpinner from './LoadingSpinner';

const MAX_POINTS = 30;

/**
 * Citește date din calea Realtime Database:
 *   device_data/{deviceId}/readings/{pushId}
 *
 * Schema unui nod de citire (trimis de ESP32):
 *   { timestamp, uptime_ms, hardware_status: { ecg_leads_off },
 *     sensors: { heart_rate_bpm, spo2_percent, temperature_c,
 *                humidity_percent, ecg_data: "v1,v2,v3,..." } }
 *
 * Listener onValue menține conexiunea live — orice nou nod apare instantaneu.
 */
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
        const ts  = new Date(raw.timestamp * 1000); // ESP32 trimite Unix seconds

        // ECG vine ca string CSV "0,1,2,3,4" — îl parsăm în array numeric
        const ecgRaw = raw.sensors?.ecg_data;
        const ecgArr = ecgRaw
          ? String(ecgRaw).split(',').map(Number).filter(n => !isNaN(n))
          : null;
        const ecgAvg = ecgArr?.length
          ? +(ecgArr.reduce((s, v) => s + v, 0) / ecgArr.length).toFixed(4)
          : null;

        rows.push({
          id:          child.key,
          time:        ts.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
          timestamp:   ts,
          puls:        raw.sensors?.heart_rate_bpm   ?? null,
          spo2:        raw.sensors?.spo2_percent      ?? null,
          temperatura: raw.sensors?.temperature_c    ?? null,
          umiditate:   raw.sensors?.humidity_percent ?? null,
          ecg:         ecgAvg,
          ecgWaveform: ecgArr,
          leadsOff:    raw.hardware_status?.ecg_leads_off ?? false,
          uptime:      raw.uptime_ms,
        });
      });
      // onValue cu orderByChild returnează în ordine crescătoare → e deja cronologic
      setData(rows);
      setLoading(false);
    }, err => {
      console.error('Eroare onValue device_data:', err);
      setLoading(false);
    });

    return () => unsub();
  }, [deviceId]);

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
        <p className="text-sm mt-1">Datele apar automat când ESP32-ul trimite citiri la <code className="bg-slate-100 px-1 rounded">device_data/{deviceId}/readings</code></p>
      </div>
    );
  }

  const lim    = limiteSenzori || {};
  const latest = data[data.length - 1];

  return (
    <div className="space-y-5">
      {/* Avertisment electrozi ECG deconectați */}
      {latest?.leadsOff && (
        <div className="flex items-center gap-2 bg-amber-50 border border-amber-300 text-amber-800 text-sm px-4 py-3 rounded-xl">
          <span className="text-lg">⚠️</span>
          <span className="font-medium">Electrozii ECG sunt deconectați (<code>ecg_leads_off: true</code>)</span>
        </div>
      )}

      {/* Valori live */}
      <LiveValues latest={latest} lim={lim} />

      {/* Puls */}
      <ChartCard title="Puls" subtitle="bpm" color="#ef4444"
        badge={latest?.puls != null ? `${latest.puls} bpm` : null}
        badgeColor={getBadgeColor(latest?.puls, lim.pulsMin, lim.pulsMax)}>
        <ResponsiveContainer width="100%" height={200}>
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
        <ResponsiveContainer width="100%" height={200}>
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
        <ResponsiveContainer width="100%" height={200}>
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
        <ResponsiveContainer width="100%" height={180}>
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
            {lim.umidMin && <ReferenceLine y={lim.umidMin} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Min', fontSize: 10, fill: '#fbbf24' }} />}
            {lim.umidMax && <ReferenceLine y={lim.umidMax} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Max', fontSize: 10, fill: '#fbbf24' }} />}
            <Area type="monotone" dataKey="umiditate" stroke="#0ea5e9" strokeWidth={2} fill="url(#gUmid)" dot={false} connectNulls />
          </AreaChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* ECG trend (medie per interval) */}
      <ChartCard title="ECG — medie per interval" subtitle="raw units" color="#6366f1"
        badge={latest?.ecg != null ? `${latest.ecg}` : null}
        badgeColor={getBadgeColor(latest?.ecg, lim.ecgMin, lim.ecgMax)}>
        <ResponsiveContainer width="100%" height={200}>
          <LineChart data={data} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <YAxis domain={['auto', 'auto']} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip content={<ChartTooltip unit="" />} />
            {lim.ecgMin && <ReferenceLine y={lim.ecgMin} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Min', fontSize: 10, fill: '#fbbf24' }} />}
            {lim.ecgMax && <ReferenceLine y={lim.ecgMax} stroke="#fbbf24" strokeDasharray="4 2" label={{ value: 'Max', fontSize: 10, fill: '#fbbf24' }} />}
            <Line type="linear" dataKey="ecg" stroke="#6366f1" strokeWidth={1.5} dot={false} connectNulls />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Formă de undă ECG din ultima citire (string CSV → waveform) */}
      {latest?.ecgWaveform?.length > 0 && !latest.leadsOff && (
        <EcgWaveform samples={latest.ecgWaveform} />
      )}

      {/* Info dispozitiv */}
      <div className="flex items-center gap-3 text-xs text-slate-400 bg-slate-50 rounded-xl px-4 py-2.5">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse flex-shrink-0" />
        <span>Dispozitiv: <strong className="text-slate-600">{deviceId}</strong></span>
        {latest?.uptime != null && (
          <span className="ml-2">· Uptime: <strong className="text-slate-600">{(latest.uptime / 1000).toFixed(0)}s</strong></span>
        )}
        <span className="ml-auto">{data.length} citiri afișate</span>
      </div>
    </div>
  );
}

/* ── Formă de undă ECG ─────────────────────────────────────────────────────── */
function EcgWaveform({ samples }) {
  const waveData = samples.map((v, i) => ({ i, v }));
  return (
    <div className="bg-white rounded-xl border border-indigo-100 p-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="w-2 h-2 rounded-full bg-indigo-500 animate-pulse" />
        <p className="text-sm font-semibold text-slate-700">Formă de Undă ECG — Ultima Citire</p>
        <span className="ml-auto text-xs text-slate-400 font-mono">{samples.length} eșantioane · ecg_data CSV</span>
      </div>
      <ResponsiveContainer width="100%" height={140}>
        <LineChart data={waveData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
          <XAxis dataKey="i" hide />
          <YAxis domain={['auto', 'auto']} tick={{ fontSize: 10, fill: '#94a3b8' }} />
          <Tooltip formatter={(v) => [`${v}`, 'ECG']} labelFormatter={() => ''} />
          <Line type="linear" dataKey="v" stroke="#6366f1" strokeWidth={1.5} dot={false} />
        </LineChart>
      </ResponsiveContainer>
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

/* ── Componente utilitare ───────────────────────────────────────────────────── */
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
