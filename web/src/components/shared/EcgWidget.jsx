import React, { useEffect, useRef, useState, useCallback } from 'react';
import { ref, query, limitToLast, onValue } from 'firebase/database';
import { db } from '../../firebase';

// ── Dimensiuni și constante ───────────────────────────────────────────────────
const W           = 700;   // lățime canvas (px)
const H           = 180;   // înălțime canvas (px)
const SPEED       = 180;   // pixeli / secundă (25mm/s paper speed)
const ERASER_W    = 18;    // lățime eraser în fața capului de scriere
const TAIL_PX     = 30;    // număr pixeli cu glow intensificat
const AMPLITUDE   = H * 0.38; // scala amplitudine ECG
const MID_Y       = H * 0.50; // linia de bază
const STATS_WIN   = 60;    // rolling window pentru statistici BPM

// ── Funcție gaussiană ─────────────────────────────────────────────────────────
const gauss = (t, mu, sigma, amp) =>
  amp * Math.exp(-0.5 * Math.pow((t - mu) / sigma, 2));

// ── Generare undă PQRST sintetică realistă ────────────────────────────────────
// Faza t ∈ [0, 1) reprezintă un ciclu cardiac complet
function pqrst(t) {
  let v = 0;
  // Baseline wander ușor (segment PR)
  if (t < 0.09) v += 0.015 * Math.sin((t / 0.09) * Math.PI);
  // Undă P: depolarizare atrială
  v += gauss(t, 0.12, 0.018, 0.08) + gauss(t, 0.16, 0.012, -0.12);
  // Complex QRS: depolarizare ventriculară
  v += gauss(t, 0.20, 0.030,  1.00)   // R — vârf principal
     + gauss(t, 0.26, 0.014, -0.30)   // S — deflexie negativă
     + gauss(t, 0.30, 0.018,  0.12);  // revenire la linia de bază
  // Undă T: repolarizare ventriculară
  v += gauss(t, 0.46, 0.040, 0.15)
     + gauss(t, 0.50, 0.055, 0.18)
     + gauss(t, 0.54, 0.040, 0.12);
  return v;
}

// ── Grid ECG paper ────────────────────────────────────────────────────────────
function drawGrid(ctx) {
  // Linii minore (la 50px X, 30px Y) — opacitate 6%
  ctx.lineWidth = 0.5;
  ctx.strokeStyle = 'rgba(0,255,65,0.06)';
  for (let x = 0; x <= W; x += 50) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
  }
  for (let y = 0; y <= H; y += 30) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
  }
  // Linii majore (la 250px X, 150px Y) — opacitate 12%
  ctx.strokeStyle = 'rgba(0,255,65,0.12)';
  for (let x = 0; x <= W; x += 250) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
  }
  for (let y = 0; y <= H; y += 150) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
  }
}

// ── Statistici (median rapid pentru array mic) ────────────────────────────────
function computeStats(history, smoothBpm) {
  if (!history.length) return { bpm: Math.round(smoothBpm), min: Math.round(smoothBpm), med: Math.round(smoothBpm), max: Math.round(smoothBpm), rr: Math.round(60000 / smoothBpm) };
  const sorted = [...history].sort((a, b) => a - b);
  return {
    bpm: Math.round(smoothBpm),
    min: sorted[0],
    med: sorted[Math.floor(sorted.length / 2)],
    max: sorted[sorted.length - 1],
    rr:  Math.round(60000 / smoothBpm),
  };
}

// ── Widget ECG ────────────────────────────────────────────────────────────────
export default function EcgWidget({ deviceId }) {
  const canvasRef = useRef(null);
  const animRef   = useRef(null);

  const SIGNAL_TIMEOUT_MS = 10_000; // 10s fără date → linie plată

  const S = useRef({
    bpm:          72,
    targetBpm:    72,
    phase:        0,
    head:         0,
    buffer:       new Float32Array(W).fill(NaN),
    bpmHistory:   [],
    lastTime:     null,
    lastDataTime: 0,   // timestamp ultima citire Firebase
  });

  const [stats,   setStats]   = useState({ bpm: 72, min: 72, med: 72, max: 72, rr: 833 });
  const [isLive,  setIsLive]  = useState(false);
  const [noSignal, setNoSignal] = useState(false);

  // ── Punct de intrare pentru BPM din Firebase ──────────────────────────────
  const setNewBpm = useCallback((bpm) => {
    const s = S.current;
    if (bpm > 30 && bpm < 220) {
      s.targetBpm    = bpm;
      s.lastDataTime = Date.now(); // marcăm momentul ultimei citiri valide
      s.bpmHistory.push(bpm);
      if (s.bpmHistory.length > STATS_WIN) s.bpmHistory.shift();
      setIsLive(true);
      setNoSignal(false);
    }
  }, []);

  // ── Listener Firebase: limitToLast(1) pentru BPM real-time ───────────────
  useEffect(() => {
    if (!deviceId) return;
    const q = query(ref(db, `device_data/${deviceId}/readings`), limitToLast(1));
    const unsub = onValue(q, (snap) => {
      snap.forEach(child => {
        const d = child.val();
        // Acceptăm toate formatele posibile de câmp
        const bpm = d.sensors?.heart_rate_bpm ?? d.heartRate ?? d.bpm ?? null;
        if (bpm) setNewBpm(Math.round(bpm));
      });
    });
    return () => unsub();
  }, [deviceId, setNewBpm]);

  // ── Actualizare statistici la fiecare 500ms ───────────────────────────────
  useEffect(() => {
    const id = setInterval(() => {
      const s = S.current;
      setStats(computeStats(s.bpmHistory, s.bpm));
    }, 500);
    return () => clearInterval(id);
  }, []);

  // ── Loop de animație pe Canvas ────────────────────────────────────────────
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const s   = S.current;

    const frame = (timestamp) => {
      if (!s.lastTime) s.lastTime = timestamp;
      const dt = Math.min((timestamp - s.lastTime) / 1000, 0.05); // max 50ms
      s.lastTime = timestamp;

      // Easing BPM smooth (15% per cadru → ~15fps time constant)
      s.bpm += (s.targetBpm - s.bpm) * 0.15;

      const cycleTime   = 60 / s.bpm;
      const phPerPx     = 1.0 / (SPEED * cycleTime);
      const pxThisFrame = SPEED * dt;

      // Verificăm dacă au trecut mai mult de SIGNAL_TIMEOUT_MS fără date noi
      const signalLost = s.lastDataTime > 0 &&
                         (Date.now() - s.lastDataTime) > SIGNAL_TIMEOUT_MS;

      // ── Scriem sample-uri noi în buffer circular ───────────────────────
      for (let i = 0; i < Math.ceil(pxThisFrame); i++) {
        if (signalLost) {
          // Linie plată cu zgomot minim (sensor conectat dar fără puls)
          s.buffer[s.head] = (Math.random() - 0.5) * 0.008;
        } else {
          s.buffer[s.head] = pqrst(s.phase);
          s.phase = (s.phase + phPerPx) % 1;
        }
        s.head = (s.head + 1) % W;
      }

      // Actualizăm indicatorul de semnal (React deduplică dacă valoarea nu s-a schimbat)
      setNoSignal(signalLost);

      // ── Render ─────────────────────────────────────────────────────────
      ctx.fillStyle = '#0a0a0a';
      ctx.fillRect(0, 0, W, H);
      drawGrid(ctx);

      // Mapare screen → buffer: screen[x] = buffer[(head + x) % W]
      // screen x=0 = cel mai vechi, screen x=W-1 = cel mai nou
      const getY = (sx) => {
        const v = s.buffer[(s.head + sx) % W];
        return isNaN(v) ? MID_Y : MID_Y - v * AMPLITUDE;
      };

      const lineEnd   = W - ERASER_W;
      const tailStart = lineEnd - TAIL_PX;

      // Culoarea liniei: verde normal, galben-portocaliu când semnal pierdut
      const lineColor  = signalLost ? '#ff9500' : '#00ff41';
      ctx.strokeStyle  = lineColor;
      ctx.shadowColor  = lineColor;

      // Linia principală
      ctx.lineWidth = 1.8;
      ctx.shadowBlur = signalLost ? 3 : 6;
      ctx.beginPath();
      for (let sx = 0; sx < tailStart; sx++) {
        const y = getY(sx);
        sx === 0 ? ctx.moveTo(sx + 0.5, y) : ctx.lineTo(sx + 0.5, y);
      }
      ctx.stroke();

      // Tail glow (redus când semnal pierdut — nu are sens glow pe linie plată)
      ctx.lineWidth = signalLost ? 1.8 : 3.5;
      ctx.shadowBlur = signalLost ? 3 : 14;
      ctx.beginPath();
      for (let sx = tailStart; sx < lineEnd; sx++) {
        const y = getY(sx);
        sx === tailStart ? ctx.moveTo(sx + 0.5, y) : ctx.lineTo(sx + 0.5, y);
      }
      ctx.stroke();

      ctx.shadowBlur = 0; // reset important pentru performanță

      animRef.current = requestAnimationFrame(frame);
    };

    animRef.current = requestAnimationFrame(frame);
    return () => { if (animRef.current) cancelAnimationFrame(animRef.current); };
  }, []);

  // ── Render React ─────────────────────────────────────────────────────────
  return (
    <div style={{
      background: '#0a0a0a',
      fontFamily: "'Courier New', Courier, monospace",
      borderRadius: 12,
      overflow: 'hidden',
      border: '1px solid #0d1f0d',
      boxShadow: '0 0 30px rgba(0,255,65,0.05)',
    }}>
      {/* Header: canal + BPM */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 16px 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{
            width: 8, height: 8, borderRadius: '50%',
            background: noSignal ? '#ff9500' : isLive ? '#00ff41' : '#1a3a1a',
            boxShadow: noSignal ? '0 0 8px #ff9500' : isLive ? '0 0 8px #00ff41' : 'none',
            display: 'inline-block',
            animation: noSignal ? 'ecgPulse 0.5s infinite' : isLive ? 'ecgPulse 1s infinite' : 'none',
          }} />
          <span style={{ color: noSignal ? '#ff9500' : '#00ff41', fontSize: 10, letterSpacing: 3, opacity: 0.6 }}>
            LEAD II · ECG
          </span>
          {noSignal && (
            <span style={{
              color: '#ff9500', fontSize: 9, letterSpacing: 2, fontWeight: 700,
              animation: 'ecgPulse 0.8s infinite',
            }}>
              ⚠ NO SIGNAL
            </span>
          )}
          {!isLive && !noSignal && deviceId && (
            <span style={{ color: '#555', fontSize: 9, letterSpacing: 1 }}>
              AȘTEPTARE DATE
            </span>
          )}
          {!deviceId && (
            <span style={{ color: '#ff9500', fontSize: 9, opacity: 0.6 }}>
              DEVICE NECONFIGURAT
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 5 }}>
          <span style={{
            color: noSignal ? '#ff9500' : '#00ff41',
            fontSize: 48, fontWeight: 700, lineHeight: 1,
            textShadow: noSignal
              ? '0 0 20px rgba(255,149,0,0.6)'
              : '0 0 20px rgba(0,255,65,0.8)',
          }}>
            {noSignal ? '---' : stats.bpm}
          </span>
          <span style={{ color: noSignal ? '#ff9500' : '#00ff41', fontSize: 11, opacity: 0.5, marginBottom: 4 }}>
            BPM
          </span>
        </div>
      </div>

      {/* Canvas ECG */}
      <canvas
        ref={canvasRef}
        width={W}
        height={H}
        style={{ display: 'block', width: '100%', height: 'auto' }}
      />

      {/* Footer: statistici */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)' }}>
        {[
          { label: 'MIN BPM', value: stats.min },
          { label: 'MED BPM', value: stats.med },
          { label: 'MAX BPM', value: stats.max },
          { label: 'RR (ms)', value: stats.rr  },
        ].map((item, i) => (
          <div key={item.label} style={{
            padding: '6px 0',
            borderTop: '1px solid #0d1f0d',
            borderRight: i < 3 ? '1px solid #0d1f0d' : 'none',
            textAlign: 'center',
          }}>
            <div style={{ color: '#00ff41', opacity: 0.3, fontSize: 8, letterSpacing: 2, marginBottom: 2 }}>
              {item.label}
            </div>
            <div style={{
              color: '#00ff41', fontSize: 17, fontWeight: 600,
              textShadow: '0 0 10px rgba(0,255,65,0.6)',
            }}>
              {item.value}
            </div>
          </div>
        ))}
      </div>

      <style>{`
        @keyframes ecgPulse {
          0%, 100% { opacity: 1; box-shadow: 0 0 8px #00ff41; }
          50%       { opacity: 0.2; box-shadow: 0 0 2px #00ff41; }
        }
      `}</style>
    </div>
  );
}
