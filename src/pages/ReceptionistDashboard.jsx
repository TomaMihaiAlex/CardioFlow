import React, { useState, useEffect, useMemo } from 'react';
import { ref, query, orderByChild, onValue, update, push, set } from 'firebase/database';
import { db } from '../firebase';
import { useAuth } from '../hooks/useAuth';
import Navbar               from '../components/shared/Navbar';
import LoadingSpinner       from '../components/shared/LoadingSpinner';
import DoctorCalendarPicker from '../components/receptionist/DoctorCalendarPicker';

const STATUS_CFG = {
  pending:   { label: 'În așteptare',      bg: 'bg-amber-100',   text: 'text-amber-700',   border: 'border-amber-200'   },
  scheduled: { label: 'Programat',         bg: 'bg-blue-100',    text: 'text-blue-700',    border: 'border-blue-200'    },
  active:    { label: 'Activ',             bg: 'bg-emerald-100', text: 'text-emerald-700', border: 'border-emerald-200' },
  closed:    { label: 'Închis',            bg: 'bg-slate-100',   text: 'text-slate-500',   border: 'border-slate-200'   },
};

export default function ReceptionistDashboard() {
  const { user } = useAuth();
  const [requests,  setRequests]  = useState([]);
  const [medici,    setMedici]    = useState([]);
  const [selected,  setSelected]  = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [filterTab, setFilterTab] = useState('pending');
  const [search,    setSearch]    = useState('');

  useEffect(() => {
    const q = query(ref(db, 'appointmentRequests'), orderByChild('creatLa'));
    const unsub = onValue(q, (snap) => {
      const list = [];
      snap.forEach(child => list.push({ id: child.key, ...child.val() }));
      list.reverse();
      setRequests(list);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  useEffect(() => {
    const q = query(ref(db, 'users'), orderByChild('role'));
    const unsub = onValue(q, (snap) => {
      const list = [];
      snap.forEach(child => {
        const d = child.val();
        if (d.role === 'medic') list.push({ uid: child.key, ...d });
      });
      setMedici(list);
    });
    return () => unsub();
  }, []);

  const filtered = useMemo(() => {
    let list = filterTab === 'toate'
      ? requests.filter(r => r.status !== 'closed')
      : requests.filter(r => r.status === filterTab);

    if (search.trim()) {
      const s = search.toLowerCase();
      list = list.filter(r =>
        `${r.prenume} ${r.nume}`.toLowerCase().includes(s) ||
        r.email?.toLowerCase().includes(s) ||
        r.symptoms?.toLowerCase().includes(s)
      );
    }
    return list;
  }, [requests, filterTab, search]);

  const stats = useMemo(() => ({
    total:     requests.filter(r => r.status !== 'closed').length,
    pending:   requests.filter(r => r.status === 'pending').length,
    scheduled: requests.filter(r => r.status === 'scheduled').length,
    active:    requests.filter(r => r.status === 'active').length,
  }), [requests]);

  // Sincronizăm cererea selectată cu datele live
  const currentSelected = selected ? requests.find(r => r.id === selected.id) || selected : null;

  if (loading) return <LoadingSpinner />;

  return (
    <div className="flex flex-col h-screen bg-slate-50">
      <Navbar title="Recepție" />

      {/* Stats bar */}
      <div className="grid grid-cols-4 gap-px bg-slate-200 border-b border-slate-200 flex-shrink-0">
        {[
          { l: 'Total activi',  v: stats.total,     c: 'text-slate-700' },
          { l: 'În așteptare',  v: stats.pending,   c: 'text-amber-600' },
          { l: 'Programate',    v: stats.scheduled, c: 'text-blue-600'  },
          { l: 'Activate',      v: stats.active,    c: 'text-emerald-600'},
        ].map(s => (
          <div key={s.l} className="bg-white px-5 py-3 text-center">
            <p className={`text-2xl font-bold ${s.c}`}>{s.v}</p>
            <p className="text-xs text-slate-400 font-medium mt-0.5">{s.l}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* ── Lista cereri ─────────────────────────────────────────────── */}
        <aside className="w-80 flex flex-col border-r border-slate-200 bg-white flex-shrink-0">
          <div className="flex border-b border-slate-100">
            {[
              { key: 'pending',   label: 'Noi',       badge: stats.pending },
              { key: 'scheduled', label: 'Programate' },
              { key: 'toate',     label: 'Toate' },
            ].map(t => (
              <button key={t.key} onClick={() => setFilterTab(t.key)}
                className={`flex-1 py-2.5 text-xs font-semibold transition-colors border-b-2
                  ${filterTab === t.key ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
                {t.label}
                {t.badge > 0 && (
                  <span className="ml-1 inline-flex items-center justify-center w-4 h-4 bg-amber-500 text-white text-xs rounded-full">
                    {t.badge}
                  </span>
                )}
              </button>
            ))}
          </div>

          <div className="px-3 py-2.5">
            <input type="text" value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Caută pacient, simptom..."
              className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white" />
          </div>

          <div className="flex-1 overflow-y-auto">
            {filtered.length === 0
              ? <p className="p-6 text-center text-slate-400 text-sm">Nicio cerere.</p>
              : filtered.map(r => (
                  <RequestCard key={r.id} r={r}
                    selected={currentSelected?.id === r.id}
                    onClick={() => setSelected(r)} />
                ))
            }
          </div>
        </aside>

        {/* ── Panou detaliu + alocare ──────────────────────────────────── */}
        <main className="flex-1 overflow-y-auto">
          {currentSelected
            ? <RequestDetail
                request={currentSelected}
                medici={medici}
                receptionistUid={user.uid}
                onClose={() => setSelected(null)}
              />
            : <EmptyDetail pending={stats.pending} />
          }
        </main>
      </div>
    </div>
  );
}

/* ── Card cerere în sidebar ──────────────────────────────────────────────── */
function RequestCard({ r, selected, onClick }) {
  const s    = STATUS_CFG[r.status] || STATUS_CFG.pending;
  const time = r.creatLa ? new Date(r.creatLa).toLocaleString('ro-RO', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit' }) : '—';
  return (
    <button onClick={onClick}
      className={`w-full text-left px-4 py-3 border-l-2 border-b border-slate-50 transition-colors
        ${selected ? 'bg-blue-50 border-l-blue-600' : 'border-l-transparent hover:bg-slate-50'}`}>
      <div className="flex items-start justify-between gap-2">
        <p className={`text-sm font-semibold ${selected ? 'text-blue-700' : 'text-slate-700'}`}>
          {r.prenume} {r.nume}
        </p>
        <span className={`text-xs px-2 py-0.5 rounded-full border font-medium flex-shrink-0 ${s.bg} ${s.text} ${s.border}`}>
          {s.label}
        </span>
      </div>
      <p className="text-xs text-slate-500 mt-1 line-clamp-2">{r.symptoms || r.descriereProblema}</p>
      <p className="text-xs text-slate-300 mt-1.5">{time}</p>
    </button>
  );
}

/* ── Detaliu cerere + alocare medic + calendar ───────────────────────────── */
function RequestDetail({ request: r, medici, receptionistUid, onClose }) {
  const [selectedMedic, setSelectedMedic] = useState(r.medicId || '');
  const [selectedSlot,  setSelectedSlot]  = useState(null); // { date, time }
  const [assigning,     setAssigning]     = useState(false);
  const [success,       setSuccess]       = useState(false);
  const [error,         setError]         = useState('');

  useEffect(() => {
    setSelectedMedic(r.medicId || '');
    setSelectedSlot(null);
    setSuccess(false);
    setError('');
  }, [r.id]);

  const handleAssign = async () => {
    if (!selectedMedic || !selectedSlot) {
      setError('Selectați un medic și un slot orar.');
      return;
    }
    const medic = medici.find(m => m.uid === selectedMedic);
    if (!medic) return;

    setAssigning(true);
    setError('');
    try {
      // 1. Actualizăm cererea
      await update(ref(db, `appointmentRequests/${r.id}`), {
        status:          'scheduled',
        medicId:         medic.uid,
        medicNume:       `${medic.prenume || ''} ${medic.nume || ''}`.trim() || medic.email,
        scheduledSlot:   selectedSlot,
        receptionistUid: receptionistUid,
        scheduledAt:     Date.now(),
      });

      // 2. Marcăm slot-ul ca rezervat în calendarul medicului
      const slotKey = `${selectedSlot.date}_${selectedSlot.time}`.replace(/:/g, '');
      await set(ref(db, `doctorSchedule/${medic.uid}/slots/${selectedSlot.date}/${slotKey}`), {
        time:       selectedSlot.time,
        status:     'booked',
        patientUid: r.patientUid,
        requestId:  r.id,
      });

      setSuccess(true);
    } catch (e) {
      setError('Eroare la alocare. Reîncercați.');
      console.error(e);
    } finally {
      setAssigning(false);
    }
  };

  const s = STATUS_CFG[r.status] || STATUS_CFG.pending;

  return (
    <div className="p-6 max-w-2xl mx-auto space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">{r.prenume} {r.nume}</h2>
          <div className="flex items-center gap-2 mt-1">
            <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${s.bg} ${s.text} ${s.border}`}>
              {s.label}
            </span>
            {r.creatLa && <span className="text-xs text-slate-400">{new Date(r.creatLa).toLocaleString('ro-RO')}</span>}
          </div>
        </div>
        <button onClick={onClose} className="text-slate-400 hover:text-slate-600 text-xl">×</button>
      </div>

      {/* Date pacient */}
      <div className="bg-white rounded-xl border border-slate-100 p-5">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Date Pacient</p>
        <div className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
          {[['Email', r.email], ['Telefon', r.telefon], ['Vârstă', r.varsta ? `${r.varsta} ani` : null]]
            .filter(([, v]) => v)
            .map(([l, v]) => (
              <div key={l}><span className="text-slate-400">{l}:</span> <span className="font-medium text-slate-700">{v}</span></div>
          ))}
        </div>
      </div>

      {/* Simptome */}
      <div className="bg-white rounded-xl border border-slate-100 p-5">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Simptome / Motiv</p>
        <p className="text-slate-700 text-sm leading-relaxed whitespace-pre-wrap">{r.symptoms || r.descriereProblema}</p>
      </div>

      {/* Alocare medic + calendar */}
      {r.status !== 'active' && r.status !== 'closed' && (
        <div className="bg-white rounded-xl border border-slate-100 p-5 space-y-5">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            {r.status === 'scheduled' ? 'Modifică Programarea' : 'Alocă Medic & Slot Orar'}
          </p>

          {r.medicNume && (
            <div className="flex items-center gap-3 bg-blue-50 border border-blue-200 rounded-xl p-3">
              <span className="text-2xl">👨‍⚕️</span>
              <div>
                <p className="font-semibold text-blue-800">{r.medicNume}</p>
                {r.scheduledSlot && (
                  <p className="text-xs text-blue-500 mt-0.5">
                    {r.scheduledSlot.date} la {r.scheduledSlot.time}
                  </p>
                )}
              </div>
            </div>
          )}

          {/* Selectare medic */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              {r.medicId ? 'Schimbă medicul' : 'Selectați medicul'}
            </label>
            <select value={selectedMedic} onChange={e => { setSelectedMedic(e.target.value); setSelectedSlot(null); }}
              className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
              <option value="">— Selectați un medic —</option>
              {medici.length === 0 && <option disabled>Niciun medic înregistrat</option>}
              {medici.map(m => (
                <option key={m.uid} value={m.uid}>
                  {m.prenume} {m.nume}{m.specialization ? ` — ${m.specialization}` : ''} ({m.email})
                </option>
              ))}
            </select>
          </div>

          {/* Calendar */}
          {selectedMedic && (
            <div>
              <p className="text-sm font-medium text-slate-700 mb-3">Selectați un slot orar disponibil</p>
              <DoctorCalendarPicker
                medicId={selectedMedic}
                onSelect={slot => setSelectedSlot(slot)}
              />
            </div>
          )}

          {error   && <p className="text-red-600 text-sm">{error}</p>}
          {success && <p className="text-emerald-600 text-sm font-medium">✓ Programarea a fost creată cu succes!</p>}

          <button
            onClick={handleAssign}
            disabled={!selectedMedic || !selectedSlot || assigning}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold py-2.5 rounded-xl transition-colors disabled:opacity-50"
          >
            {assigning
              ? 'Se procesează...'
              : r.status === 'scheduled' ? '🔄 Actualizează Programarea' : '✅ Confirmă Programarea'}
          </button>
        </div>
      )}

      {(r.status === 'active' || r.status === 'closed') && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-sm text-emerald-700 font-medium">
          {r.status === 'active' ? '✅ Pacient activat în sistem de către medic.' : '✓ Caz închis.'}
        </div>
      )}
    </div>
  );
}

/* ── Stare goală ────────────────────────────────────────────────────────── */
function EmptyDetail({ pending }) {
  return (
    <div className="h-full flex flex-col items-center justify-center text-center p-8">
      <div className="w-20 h-20 bg-amber-50 rounded-2xl flex items-center justify-center mb-4">
        <span className="text-4xl">{pending > 0 ? '📋' : '✅'}</span>
      </div>
      <h3 className="text-lg font-semibold text-slate-700 mb-1">
        {pending > 0 ? `${pending} cerere${pending > 1 ? 'ri' : ''} în așteptare` : 'Totul la zi!'}
      </h3>
      <p className="text-slate-400 text-sm max-w-xs">
        {pending > 0
          ? 'Selectați o cerere din stânga pentru a o procesa și aloca unui medic.'
          : 'Nu există cereri noi în acest moment.'}
      </p>
    </div>
  );
}
