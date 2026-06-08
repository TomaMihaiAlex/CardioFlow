import React, { useState, useEffect, useRef, useCallback } from 'react';
import { getAuth } from 'firebase/auth';
import { ResponsiveContainer, LineChart, Line, YAxis, CartesianGrid, XAxis } from 'recharts';

const WS_URL     = import.meta.env.VITE_WS_SERVER_URL || 'http://localhost:3001';
const BUFFER_LEN = 500; // ~2s la 250 Hz

/**
 * Grafic ECG în timp real via WebSocket (Socket.io).
 * Se conectează DOAR când medicul apasă "Pornește Stream".
 * La unmount sau stop, deconectează și eliberează resursele.
 */
export default function EcgStreamChart({ deviceId }) {
  const [samples,  setSamples]  = useState([]); // { i, v }[]
  const [status,   setStatus]   = useState('idle');  // idle|connecting|streaming|error
  const [errorMsg, setErrorMsg] = useState('');
  const [fps,      setFps]      = useState(0);
  const socketRef  = useRef(null);
  const fpsRef     = useRef({ count: 0, last: Date.now() });

  const stopStream = useCallback(() => {
    if (socketRef.current) {
      socketRef.current.emit('stop_ecg_stream');
      socketRef.current.disconnect();
      socketRef.current = null;
    }
    setStatus('idle');
    setSamples([]);
    setFps(0);
  }, []);

  const startStream = useCallback(async () => {
    setStatus('connecting');
    setErrorMsg('');
    try {
      const idToken = await getAuth().currentUser?.getIdToken(true);
      if (!idToken) throw new Error('Utilizatorul nu este autentificat.');

      // Import dinamic pentru a nu crește bundle-ul dacă nu e necesar
      const { io } = await import('socket.io-client');
      const socket  = io(WS_URL, { transports: ['websocket'], reconnection: false });
      socketRef.current = socket;

      socket.on('connect', () => {
        socket.emit('request_ecg_stream', { deviceId, idToken });
      });

      socket.on('ecg_stream_ready', () => {
        setStatus('streaming');
      });

      socket.on('ecg_data', ({ samples: raw }) => {
        // Acceptăm array numeric, Float32Array, sau CSV string
        const arr = Array.isArray(raw)
          ? raw
          : typeof raw === 'string'
            ? raw.split(',').map(Number).filter(n => !isNaN(n))
            : [];

        setSamples(prev => {
          const next = [...prev, ...arr.map((v, i) => ({ i: prev.length + i, v }))];
          return next.length > BUFFER_LEN ? next.slice(next.length - BUFFER_LEN) : next;
        });

        // Calcul FPS aproximativ
        fpsRef.current.count += arr.length;
        const now = Date.now();
        if (now - fpsRef.current.last >= 1000) {
          setFps(fpsRef.current.count);
          fpsRef.current = { count: 0, last: now };
        }
      });

      socket.on('auth_error', (msg) => {
        setStatus('error');
        setErrorMsg(msg);
        socket.disconnect();
      });

      socket.on('connect_error', () => {
        setStatus('error');
        setErrorMsg(`Nu s-a putut conecta la serverul WS (${WS_URL}). Verificați că serverul rulează.`);
      });

      socket.on('disconnect', () => {
        if (status === 'streaming') setStatus('idle');
      });
    } catch (err) {
      setStatus('error');
      setErrorMsg(err.message);
    }
  }, [deviceId]);

  // Curățăm conexiunea la unmount — CRITIC pentru a nu lăsa stream-uri deschise
  useEffect(() => () => { if (socketRef.current) stopStream(); }, [stopStream]);

  return (
    <div className="bg-white rounded-xl border border-indigo-100 p-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2.5">
          <span className={`w-2.5 h-2.5 rounded-full flex-shrink-0
            ${status === 'streaming' ? 'bg-indigo-500 animate-pulse' : 'bg-slate-300'}`} />
          <span className="text-sm font-semibold text-slate-700">ECG Real-Time</span>
          {status === 'streaming' && (
            <span className="text-xs font-bold text-indigo-700 bg-indigo-100 px-2 py-0.5 rounded-full">
              ● LIVE
            </span>
          )}
          {status === 'streaming' && fps > 0 && (
            <span className="text-xs text-slate-400">{fps} samples/s</span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {(status === 'idle' || status === 'error') && (
            <button onClick={startStream}
              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg transition-colors">
              ▶ Pornește Stream
            </button>
          )}
          {status === 'connecting' && (
            <button disabled className="px-3 py-1.5 bg-slate-200 text-slate-400 text-xs font-semibold rounded-lg">
              <svg className="inline animate-spin h-3 w-3 mr-1" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              Se conectează...
            </button>
          )}
          {status === 'streaming' && (
            <button onClick={stopStream}
              className="px-3 py-1.5 bg-red-100 hover:bg-red-200 text-red-700 text-xs font-semibold rounded-lg transition-colors">
              ⏹ Oprește
            </button>
          )}
        </div>
      </div>

      {/* Error */}
      {status === 'error' && (
        <div className="text-red-700 text-sm bg-red-50 border border-red-200 rounded-lg px-3 py-2.5 mb-3">
          {errorMsg}
        </div>
      )}

      {/* Waveform */}
      {status === 'streaming' && samples.length > 0 && (
        <>
          <ResponsiveContainer width="100%" height={180}>
            <LineChart data={samples} margin={{ top: 5, right: 5, left: -25, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eef2ff" />
              <XAxis dataKey="i" hide />
              <YAxis domain={['auto', 'auto']} tick={{ fontSize: 10, fill: '#94a3b8' }} />
              <Line
                type="linear" dataKey="v"
                stroke="#6366f1" strokeWidth={1.5}
                dot={false} isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
          <div className="flex items-center justify-between mt-2 text-xs text-slate-400">
            <span className="font-mono">{deviceId}</span>
            <span>{samples.length} sample-uri în buffer ({BUFFER_LEN} max)</span>
          </div>
        </>
      )}

      {/* Idle / waiting */}
      {(status === 'idle' || (status === 'streaming' && samples.length === 0)) && (
        <div className="text-center py-8 text-slate-400 text-sm">
          {status === 'idle' ? (
            <>
              <p>Activați stream-ul pentru a vizualiza forma de undă ECG în timp real.</p>
              <p className="text-xs mt-1 text-slate-300">
                Conexiunea directă este menținută doar cât fereastra este deschisă.
              </p>
            </>
          ) : (
            <p>Așteptare date de la dispozitiv <span className="font-mono">{deviceId}</span>...</p>
          )}
        </div>
      )}
    </div>
  );
}
