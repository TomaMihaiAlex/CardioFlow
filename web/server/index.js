require('dotenv').config();
const express  = require('express');
const http     = require('http');
const { Server } = require('socket.io');
const admin    = require('firebase-admin');

// ─── Firebase Admin Init ────────────────────────────────────────────────────
function initFirebase() {
  const b64 = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
  const cred = b64
    ? admin.credential.cert(JSON.parse(Buffer.from(b64, 'base64').toString('utf8')))
    : admin.credential.applicationDefault(); // Cloud Run Workload Identity sau GOOGLE_APPLICATION_CREDENTIALS

  admin.initializeApp({
    credential:  cred,
    databaseURL: process.env.FIREBASE_DATABASE_URL,
  });
}
initFirebase();

const db = admin.database();

// ─── Express + Socket.io ────────────────────────────────────────────────────
const app    = express();
const server = http.createServer(app);
const io     = new Server(server, {
  cors: {
    origin:  (process.env.ALLOWED_ORIGINS || 'http://localhost:5173').split(','),
    methods: ['GET', 'POST'],
  },
});

app.get('/health', (_, res) => res.json({ status: 'ok', uptime: process.uptime() }));

// ─── State ───────────────────────────────────────────────────────────────────
// deviceId → Set<socketId>  (consumers = medici care urmăresc)
const activeStreams = new Map();
// socketId → { role: 'provider'|'consumer', deviceId, uid }
const socketMeta   = new Map();

// ─── Auth helpers ────────────────────────────────────────────────────────────
async function verifyToken(idToken) {
  try   { return await admin.auth().verifyIdToken(idToken); }
  catch { return null; }
}

async function medicHasAccessToDevice(medicUid, deviceId) {
  const snap = await db.ref(`devicePairings/${deviceId}`).get();
  if (!snap.exists()) return false;
  return snap.val().medicId === medicUid;
}

// ─── Cleanup helper ──────────────────────────────────────────────────────────
function removeConsumer(socketId, deviceId) {
  if (!deviceId) return;
  const consumers = activeStreams.get(deviceId);
  if (!consumers) return;
  consumers.delete(socketId);
  if (consumers.size === 0) {
    // Niciun medic nu mai urmărește → comandăm Android-ul să oprească stream-ul
    io.to(`device:${deviceId}`).emit('stop_ecg_stream');
    activeStreams.delete(deviceId);
    console.log(`[ECG] Stream oprit — dispozitiv ${deviceId} (niciun consumer)`);
  }
}

// ─── Socket.io events ────────────────────────────────────────────────────────
io.on('connection', (socket) => {
  console.log(`[WS] Connect: ${socket.id}`);

  /**
   * Android / dispozitiv se înregistrează ca sursă ECG.
   * Payload: { deviceId: string, idToken: string }
   */
  socket.on('join_as_provider', async ({ deviceId, idToken }) => {
    const decoded = await verifyToken(idToken);
    if (!decoded) return socket.emit('auth_error', 'Token Firebase invalid.');

    // Verificăm că utilizatorul este asociat cu deviceId (via users.patientId)
    const userSnap = await db.ref(`users/${decoded.uid}/patientId`).get();
    if (userSnap.exists() && userSnap.val() !== deviceId) {
      // Tolerăm și service accounts / admin fără verificare strictă (IoT gateway)
      console.warn(`[WS] Provider uid=${decoded.uid} nu deține ${deviceId}`);
    }

    socket.join(`device:${deviceId}`);
    socketMeta.set(socket.id, { role: 'provider', deviceId, uid: decoded.uid });

    // Dacă există deja consumatori care așteaptă, pornim stream-ul imediat
    if (activeStreams.has(deviceId) && activeStreams.get(deviceId).size > 0) {
      socket.emit('start_ecg_stream');
    }

    socket.emit('provider_ready', { deviceId });
    console.log(`[ECG] Provider conectat: ${deviceId}`);
  });

  /**
   * Medicul din browser cere stream ECG pentru un dispozitiv.
   * Payload: { deviceId: string, idToken: string }
   */
  socket.on('request_ecg_stream', async ({ deviceId, idToken }) => {
    const decoded = await verifyToken(idToken);
    if (!decoded) return socket.emit('auth_error', 'Token Firebase invalid.');

    const hasAccess = await medicHasAccessToDevice(decoded.uid, deviceId);
    if (!hasAccess) return socket.emit('auth_error', 'Nu aveți acces la acest dispozitiv.');

    socket.join(`device:${deviceId}`);
    socketMeta.set(socket.id, { role: 'consumer', deviceId, uid: decoded.uid });

    if (!activeStreams.has(deviceId)) activeStreams.set(deviceId, new Set());
    activeStreams.get(deviceId).add(socket.id);

    // Notificăm provider-ul să înceapă transmisia
    socket.to(`device:${deviceId}`).emit('start_ecg_stream');
    socket.emit('ecg_stream_ready', { deviceId });
    console.log(`[ECG] Consumer adăugat pentru ${deviceId} (total: ${activeStreams.get(deviceId).size})`);
  });

  /**
   * Android trimite un batch de sample-uri ECG.
   * Payload: { samples: number[] | string, timestamp: number }
   *   samples poate fi Float32Array serialized, array numeric, sau CSV string
   */
  socket.on('ecg_samples', (payload) => {
    const meta = socketMeta.get(socket.id);
    if (!meta || meta.role !== 'provider') return;

    const consumers = activeStreams.get(meta.deviceId);
    if (!consumers || consumers.size === 0) return; // nimeni nu ascultă — nu relay

    socket.to(`device:${meta.deviceId}`).emit('ecg_data', {
      samples:   payload.samples,
      timestamp: payload.timestamp || Date.now(),
      deviceId:  meta.deviceId,
    });
  });

  /**
   * Medicul oprește stream-ul (închide fereastra / apasă Stop).
   */
  socket.on('stop_ecg_stream', () => {
    const meta = socketMeta.get(socket.id);
    if (meta?.role === 'consumer') {
      removeConsumer(socket.id, meta.deviceId);
      socketMeta.delete(socket.id);
    }
  });

  socket.on('disconnect', () => {
    const meta = socketMeta.get(socket.id);
    if (meta) {
      if (meta.role === 'consumer') removeConsumer(socket.id, meta.deviceId);
      socketMeta.delete(socket.id);
    }
    console.log(`[WS] Disconnect: ${socket.id}`);
  });
});

// ─── Start ───────────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
  console.log(`CardioFlow ECG WS Relay — port ${PORT}`);
  console.log(`Health: http://localhost:${PORT}/health`);
});
