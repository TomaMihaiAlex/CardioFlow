import { ref, update, push } from 'firebase/database';
import { db } from '../firebase';

/**
 * Activează un pacient în sistem printr-un singur multi-path update atomic.
 *
 * Scrie simultan în:
 *   medicalRecords/{newKey}       ← fișa oficială (noua arhitectură)
 *   devicePairings/{deviceId}     ← asociere hardware ↔ pacient
 *   sensorThresholds/{patientUid} ← praguri citite rapid de Android
 *   users/{patientUid}            ← activeRecordId + deviceId
 *   appointmentRequests/{id}      ← status → 'active' (dacă vine din cerere)
 *   pacienti/{newKey}             ← backwards compat cu PacientDashboard
 */
export async function activatePatient({
  patientUid,
  medicId,
  requestId = null,
  patientData,   // { prenume, nume, varsta, telefon, email }
  medicalData,   // { anamneza, alergii, diagnosticCurent }
  deviceId,
  thresholds,    // { pulsMin, pulsMax, spo2Min, spo2Max, tempMin, tempMax, ecgMin, ecgMax, umidMin, umidMax }
}) {
  const now        = Date.now();
  const recordRef  = push(ref(db, 'medicalRecords'));
  const pacientRef = push(ref(db, 'pacienti'));

  const updates = {};

  // 1. Medical Record
  updates[`medicalRecords/${recordRef.key}`] = {
    patientUid,
    medicId,
    requestId,
    deviceId,
    prenume:          patientData.prenume  || '',
    nume:             patientData.nume     || '',
    varsta:           parseInt(patientData.varsta) || null,
    telefon:          patientData.telefon  || '',
    email:            (patientData.email   || '').toLowerCase(),
    anamneza:         medicalData.anamneza          || '',
    alergii:          medicalData.alergii            || '',
    diagnosticCurent: medicalData.diagnosticCurent  || '',
    recomandari:      {},
    status:           'active',
    creatLa:          now,
    actualizatLa:     now,
  };

  // 2. Device Pairing
  updates[`devicePairings/${deviceId}`] = {
    deviceId,
    patientUid,
    medicId,
    recordId:  recordRef.key,
    pacientId: pacientRef.key,
    model:     'ESP32 + AD8232 + MAX30102 + DHT11',
    status:    'paired',
    pairedAt:  now,
    lastSeen:  now,
  };

  // 3. Sensor Thresholds — nod separat pentru acces O(1) de pe Android
  updates[`sensorThresholds/${patientUid}`] = {
    ...thresholds,
    setByMedicId: medicId,
    actualizatLa: now,
  };

  // 4. User — link rapid la fișă și dispozitiv
  updates[`users/${patientUid}/activeRecordId`] = recordRef.key;
  updates[`users/${patientUid}/deviceId`]       = deviceId;

  // 5. Appointment Request → active
  if (requestId) {
    updates[`appointmentRequests/${requestId}/status`]      = 'active';
    updates[`appointmentRequests/${requestId}/activatedAt`] = now;
    updates[`appointmentRequests/${requestId}/medicId`]     = medicId;
  }

  // 6. Pacienti doc — backwards compat cu PacientDashboard existent
  updates[`pacienti/${pacientRef.key}`] = {
    medicId,
    prenume:           patientData.prenume  || '',
    nume:              patientData.nume     || '',
    varsta:            parseInt(patientData.varsta) || null,
    telefon:           patientData.telefon  || '',
    email:             (patientData.email   || '').toLowerCase(),
    istoricMedical:    medicalData.anamneza          || '',
    alergii:           medicalData.alergii            || '',
    consultatiiCardio: medicalData.diagnosticCurent  || '',
    deviceId,
    limiteSenzori:     thresholds,
    recomandari:       {},
    creatLa:           now,
  };

  // Scriere atomică multi-path
  await update(ref(db), updates);

  return { recordId: recordRef.key, pacientId: pacientRef.key };
}
