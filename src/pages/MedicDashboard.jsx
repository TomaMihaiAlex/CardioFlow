import React, { useState, useEffect, useMemo } from 'react';
import { ref, query, orderByChild, equalTo, onValue, update } from 'firebase/database';
import { db } from '../firebase';
import { useAuth } from '../hooks/useAuth';
import Navbar                 from '../components/shared/Navbar';
import LoadingSpinner         from '../components/shared/LoadingSpinner';
import AddPatientModal        from '../components/medic/AddPatientModal';
import FirstConsultationModal from '../components/medic/FirstConsultationModal';
import PatientDetail          from '../components/medic/PatientDetail';

export default function MedicDashboard() {
  const { user } = useAuth();
  const [patients,         setPatients]         = useState([]);
  const [appointments,     setAppointments]      = useState([]); // appointmentRequests asignate acestui medic
  const [selectedPatient,  setSelectedPatient]   = useState(null);
  const [loading,          setLoading]           = useState(true);
  const [search,           setSearch]            = useState('');
  const [sidebarTab,       setSidebarTab]        = useState('pacienti');
  const [showAddModal,     setShowAddModal]       = useState(false);
  const [showFirstConsult, setShowFirstConsult]  = useState(false);
  const [consultRequest,   setConsultRequest]    = useState(null); // cererea selectată

  // Pacienți ai medicului
  useEffect(() => {
    if (!user) return;
    const q = query(ref(db, 'pacienti'), orderByChild('medicId'), equalTo(user.uid));
    const unsub = onValue(q, (snap) => {
      const list = [];
      snap.forEach(child => {
        const data = child.val();
        if (data.recomandari && !Array.isArray(data.recomandari))
          data.recomandari = Object.values(data.recomandari);
        list.push({ id: child.key, ...data });
      });
      list.sort((a, b) => (b.creatLa || 0) - (a.creatLa || 0));
      setPatients(list);
      setSelectedPatient(prev => prev ? (list.find(p => p.id === prev.id) || prev) : null);
      setLoading(false);
    });
    return () => unsub();
  }, [user]);

  // Cereri de programare asignate acestui medic
  useEffect(() => {
    if (!user) return;
    const q = query(ref(db, 'appointmentRequests'), orderByChild('medicId'), equalTo(user.uid));
    const unsub = onValue(q, (snap) => {
      const list = [];
      snap.forEach(child => {
        const d = child.val();
        if (d.status !== 'closed' && d.status !== 'active') // 'active' = deja creat fișă
          list.push({ id: child.key, ...d });
      });
      list.sort((a, b) => (b.creatLa || 0) - (a.creatLa || 0));
      setAppointments(list);
    });
    return () => unsub();
  }, [user]);

  const filteredPatients = useMemo(() => {
    if (!search.trim()) return patients;
    const s = search.toLowerCase();
    return patients.filter(p =>
      `${p.prenume} ${p.nume}`.toLowerCase().includes(s) ||
      p.email?.toLowerCase().includes(s) ||
      p.cnp?.includes(s)
    );
  }, [patients, search]);

  const pendingAppointments = appointments.filter(a => a.status === 'scheduled');

  const openFirstConsult = (req) => {
    setConsultRequest(req);
    setShowFirstConsult(true);
  };

  const handleMarkClosed = async (id) => {
    try { await update(ref(db, `appointmentRequests/${id}`), { status: 'closed' }); }
    catch (e) { console.error(e); }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="flex flex-col h-screen bg-slate-50">
      <Navbar title="Panou Medic" />

      <div className="flex flex-1 overflow-hidden">
        {/* ── Sidebar ───────────────────────────────────────────────────── */}
        <aside className="w-72 flex flex-col border-r border-slate-200 bg-white flex-shrink-0">
          <div className="p-4 border-b border-slate-100">
            <button
              onClick={() => setShowAddModal(true)}
              className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold py-2.5 rounded-xl transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Adaugă Pacient Direct
            </button>
          </div>

          {/* Tab-uri sidebar */}
          <div className="flex border-b border-slate-100">
            {[
              { key: 'pacienti',     label: `Pacienți (${patients.length})` },
              { key: 'programari',   label: 'Programări', badge: pendingAppointments.length },
            ].map(t => (
              <button key={t.key} onClick={() => setSidebarTab(t.key)}
                className={`flex-1 py-2.5 text-xs font-semibold transition-colors border-b-2
                  ${sidebarTab === t.key ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500'}`}>
                {t.label}
                {t.badge > 0 && (
                  <span className="ml-1 inline-flex items-center justify-center w-4 h-4 bg-amber-500 text-white text-xs rounded-full">
                    {t.badge}
                  </span>
                )}
              </button>
            ))}
          </div>

          {sidebarTab === 'pacienti' ? (
            <>
              <div className="px-3 py-3">
                <input type="text" value={search} onChange={e => setSearch(e.target.value)}
                  placeholder="Caută pacient..."
                  className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white" />
              </div>
              <div className="flex-1 overflow-y-auto">
                {filteredPatients.length === 0
                  ? <p className="px-4 py-8 text-center text-slate-400 text-sm">
                      {patients.length === 0 ? 'Niciun pacient.' : 'Niciun rezultat.'}
                    </p>
                  : filteredPatients.map(p => (
                      <PatientCard key={p.id} patient={p}
                        selected={selectedPatient?.id === p.id}
                        onClick={() => setSelectedPatient(p)} />
                    ))
                }
              </div>
              <p className="px-4 py-3 border-t border-slate-100 text-xs text-slate-400 text-center">
                {patients.length} pacienți
              </p>
            </>
          ) : (
            <div className="flex-1 overflow-y-auto">
              {appointments.length === 0
                ? <p className="px-4 py-8 text-center text-slate-400 text-sm">Nicio programare asignată.</p>
                : appointments.map(a => (
                    <AppointmentCard key={a.id} appt={a}
                      onCreateFile={() => openFirstConsult(a)}
                      onClose={() => handleMarkClosed(a.id)} />
                  ))
              }
            </div>
          )}
        </aside>

        {/* ── Panou principal ───────────────────────────────────────────── */}
        <main className="flex-1 overflow-hidden">
          {selectedPatient
            ? <PatientDetail patient={selectedPatient} onDeleted={() => setSelectedPatient(null)} />
            : <EmptyState onAdd={() => setShowAddModal(true)} count={patients.length} />
          }
        </main>
      </div>

      <AddPatientModal
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
      />
      <FirstConsultationModal
        open={showFirstConsult}
        onClose={() => { setShowFirstConsult(false); setConsultRequest(null); }}
        appointmentRequest={consultRequest}
      />
    </div>
  );
}

/* ── Sub-componente ───────────────────────────────────────────────────────── */
function PatientCard({ patient, selected, onClick }) {
  const initials = `${patient.prenume?.[0]??''}${patient.nume?.[0]??''}`.toUpperCase();
  return (
    <button onClick={onClick}
      className={`w-full text-left px-4 py-3 flex items-center gap-3 transition-colors border-l-2
        ${selected ? 'bg-blue-50 border-l-blue-600' : 'border-l-transparent hover:bg-slate-50'}`}>
      <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0
        ${selected ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'}`}>
        {initials || '?'}
      </div>
      <div className="min-w-0">
        <p className={`text-sm font-semibold truncate ${selected ? 'text-blue-700' : 'text-slate-700'}`}>
          {patient.prenume} {patient.nume}
        </p>
        <p className="text-xs text-slate-400 truncate">
          {patient.varsta ? `${patient.varsta} ani` : ''}
          {patient.deviceId ? ` · 📡 ${patient.deviceId}` : ''}
        </p>
      </div>
    </button>
  );
}

function AppointmentCard({ appt, onCreateFile, onClose }) {
  const isNew = appt.status === 'scheduled';
  return (
    <div className={`px-4 py-3 border-b border-slate-50 ${isNew ? 'bg-amber-50/60' : ''}`}>
      <div className="flex items-start justify-between gap-2 mb-1">
        <p className="text-sm font-semibold text-slate-800">{appt.prenume} {appt.nume}</p>
        <span className={`text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0
          ${isNew ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-500'}`}>
          {isNew ? '📅 Confirmat' : appt.status}
        </span>
      </div>
      {appt.scheduledSlot && (
        <p className="text-xs text-blue-600 font-medium mb-1">
          {appt.scheduledSlot.date} la {appt.scheduledSlot.time}
        </p>
      )}
      <p className="text-xs text-slate-500 line-clamp-2 mb-2">{appt.symptoms || appt.descriereProblema}</p>
      {isNew && (
        <div className="flex gap-2">
          <button onClick={onCreateFile}
            className="flex-1 text-xs py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors">
            + Crează Fișă Medicală
          </button>
          <button onClick={onClose}
            className="text-xs px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-lg transition-colors">
            Închide
          </button>
        </div>
      )}
    </div>
  );
}

function EmptyState({ onAdd, count }) {
  return (
    <div className="h-full flex flex-col items-center justify-center text-center p-8">
      <div className="w-20 h-20 bg-blue-50 rounded-2xl flex items-center justify-center mb-4">
        <svg className="w-10 h-10 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <h3 className="text-lg font-semibold text-slate-700 mb-1">
        {count === 0 ? 'Niciun pacient' : 'Selectați un pacient'}
      </h3>
      <p className="text-slate-400 text-sm max-w-xs">
        {count === 0 ? 'Adăugați un pacient direct sau prin tab-ul Programări.' : 'Click pe un pacient din lista din stânga.'}
      </p>
      {count === 0 && (
        <button onClick={onAdd}
          className="mt-4 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-xl transition-colors">
          + Adaugă Pacient
        </button>
      )}
    </div>
  );
}
