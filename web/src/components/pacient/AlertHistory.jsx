import React, { useState, useEffect } from 'react';
import { collection, query, where, orderBy, limit, onSnapshot } from 'firebase/firestore';
import { db } from '../../firebase';

const STATUS_STYLES = {
  activa:    { badge: 'bg-red-100 text-red-700 border-red-200',    row: 'bg-red-50 border-red-100'  },
  rezolvata: { badge: 'bg-slate-100 text-slate-500 border-slate-200', row: 'bg-white border-slate-100' },
};

const ALERT_ICONS = {
  puls:        '💓',
  temperatura: '🌡️',
  ecg:         '📈',
  umiditate:   '💧',
};

export default function AlertHistory({ pacientId }) {
  const [alerts,  setAlerts]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter,  setFilter]  = useState('toate'); // 'toate' | 'activa' | 'rezolvata'

  useEffect(() => {
    if (!pacientId) return;
    const q = query(
      collection(db, 'alerme'),
      where('pacientId', '==', pacientId),
      orderBy('timestamp', 'desc'),
      limit(100)
    );
    const unsub = onSnapshot(q, snap => {
      setAlerts(snap.docs.map(d => ({ id: d.id, ...d.data() })));
      setLoading(false);
    });
    return unsub;
  }, [pacientId]);

  const active   = alerts.filter(a => a.status === 'activa').length;
  const filtered = filter === 'toate' ? alerts : alerts.filter(a => a.status === filter);

  if (loading) return <div className="text-slate-400 text-sm p-4">Se încarcă alertele...</div>;

  return (
    <div>
      {/* Sumar */}
      <div className="grid grid-cols-3 gap-3 mb-5">
        <SumCard label="Total Alerte"    value={alerts.length} color="slate" />
        <SumCard label="Active"          value={active}        color="red"   />
        <SumCard label="Rezolvate"       value={alerts.length - active} color="emerald" />
      </div>

      {/* Filtre */}
      <div className="flex gap-2 mb-4">
        {['toate', 'activa', 'rezolvata'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors capitalize
              ${filter === f ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
          >
            {f}
          </button>
        ))}
      </div>

      {/* Lista */}
      {filtered.length === 0 ? (
        <div className="text-center py-10 text-slate-400">
          <span className="text-3xl block mb-2">🔔</span>
          <p className="text-sm">Nicio alertă {filter !== 'toate' ? filter : ''} înregistrată.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map(a => {
            const s  = STATUS_STYLES[a.status] || STATUS_STYLES.rezolvata;
            const ts = a.timestamp?.toDate ? a.timestamp.toDate() : null;
            const tipLower = (a.tipAlerta || '').toLowerCase();
            const icon = Object.entries(ALERT_ICONS).find(([k]) => tipLower.includes(k))?.[1] || '⚠️';

            return (
              <div key={a.id} className={`flex items-start gap-3 p-3.5 rounded-xl border ${s.row}`}>
                <span className="text-xl mt-0.5">{icon}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-sm text-slate-800">{a.tipAlerta}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${s.badge}`}>
                      {a.status}
                    </span>
                    {a.valoareCitita !== undefined && (
                      <span className="text-xs text-slate-500">val: {a.valoareCitita}</span>
                    )}
                  </div>
                  <p className="text-sm text-slate-600 mt-1">{a.textAsociat}</p>
                  {ts && (
                    <p className="text-xs text-slate-400 mt-1">
                      {ts.toLocaleDateString('ro-RO', { day: '2-digit', month: 'long', year: 'numeric' })}
                      {' · '}
                      {ts.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function SumCard({ label, value, color }) {
  const styles = {
    slate:   'bg-slate-50 text-slate-700',
    red:     'bg-red-50 text-red-700',
    emerald: 'bg-emerald-50 text-emerald-700',
  };
  return (
    <div className={`rounded-xl p-3 ${styles[color]}`}>
      <p className="text-2xl font-bold">{value}</p>
      <p className="text-xs font-medium opacity-70 mt-0.5">{label}</p>
    </div>
  );
}
