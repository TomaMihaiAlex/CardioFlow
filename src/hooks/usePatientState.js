import { useState, useEffect, useMemo } from 'react';
import { ref, query, orderByChild, equalTo, onValue, get } from 'firebase/database';
import { db } from '../firebase';
import { useAuth } from './useAuth';

/**
 * Stările posibile ale interfeței pacientului.
 * Derivate PUR din date — niciun useState manual pentru navigare.
 */
export const PS = {
  LOADING:           'loading',
  NO_RECORD:         'no_record',         // fără fișă și fără cerere activă
  REQUEST_PENDING:   'request_pending',   // cerere trimisă, recepționer n-a alocat încă
  REQUEST_SCHEDULED: 'request_scheduled', // recepționer a alocat medic + slot
  MONITORING:        'monitoring',        // fișă activă — dashboard complet
};

/**
 * Hook central — ascultă în paralel medicalRecords, pacienti (backwards compat)
 * și appointmentRequests, derivând starea curectă a pacientului.
 */
export function usePatientState() {
  const { user, userData } = useAuth();
  const [medicalRecord,      setMedicalRecord]      = useState(undefined);
  const [legacyPatientDoc,   setLegacyPatientDoc]   = useState(undefined);
  const [appointmentRequest, setAppointmentRequest] = useState(undefined);

  useEffect(() => {
    if (!user) {
      setMedicalRecord(null);
      setLegacyPatientDoc(null);
      setAppointmentRequest(null);
      return;
    }

    // ── 1. medicalRecords (noul flux de activare) ──────────────────────────
    const recQ = query(
      ref(db, 'medicalRecords'),
      orderByChild('patientUid'),
      equalTo(user.uid)
    );
    const unsubRec = onValue(recQ, (snap) => {
      let found = null;
      snap.forEach(child => {
        const d = child.val();
        if (d.status === 'active') found = { id: child.key, ...d };
      });
      setMedicalRecord(found);
    });

    // ── 2. pacienti (flux vechi — medic a adăugat direct fișa) ────────────
    const legQ = query(
      ref(db, 'pacienti'),
      orderByChild('email'),
      equalTo(user.email?.toLowerCase() || '')
    );
    const unsubLeg = onValue(legQ, (snap) => {
      let found = null;
      snap.forEach(child => {
        const data = child.val();
        if (data.recomandari && !Array.isArray(data.recomandari))
          data.recomandari = Object.values(data.recomandari);
        found = { id: child.key, ...data };
      });
      setLegacyPatientDoc(found);
    });

    // ── 3. appointmentRequests (cereri de programare) ──────────────────────
    const reqQ = query(
      ref(db, 'appointmentRequests'),
      orderByChild('patientUid'),
      equalTo(user.uid)
    );
    const unsubReq = onValue(reqQ, (snap) => {
      let found = null;
      snap.forEach(child => {
        const d = child.val();
        if (d.status !== 'closed') {
          if (!found || (d.creatLa || 0) > (found.creatLa || 0))
            found = { id: child.key, ...d };
        }
      });
      setAppointmentRequest(found);
    });

    return () => { unsubRec(); unsubLeg(); unsubReq(); };
  }, [user?.uid, user?.email]);

  const loading = medicalRecord === undefined || legacyPatientDoc === undefined || appointmentRequest === undefined;

  const state = useMemo(() => {
    if (loading)                                            return PS.LOADING;
    if (medicalRecord?.status === 'active')                 return PS.MONITORING;
    if (legacyPatientDoc)                                   return PS.MONITORING;
    if (appointmentRequest?.status === 'active')            return PS.MONITORING;
    if (appointmentRequest?.status === 'scheduled')         return PS.REQUEST_SCHEDULED;
    if (appointmentRequest?.status === 'pending')           return PS.REQUEST_PENDING;
    return PS.NO_RECORD;
  }, [loading, medicalRecord, legacyPatientDoc, appointmentRequest]);

  // activeRecord: sursa de date pentru dashboard-ul de monitorizare
  const activeRecord = medicalRecord || legacyPatientDoc || null;

  return { state, activeRecord, appointmentRequest, userData, user };
}
