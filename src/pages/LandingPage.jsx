import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const FEATURES = [
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    ),
    title: 'Monitorizare în Timp Real',
    desc: 'Grafice live pentru ECG, puls, SpO₂, temperatură și umiditate. Date transmise automat de dispozitivul ESP32 la fiecare 30 de secunde.',
    color: 'blue',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
    ),
    title: 'Alerte Inteligente Instant',
    desc: 'Alarme asincrone trimise imediat când senzorii depășesc pragurile personalizate de medic. Zero întârziere în situații critice.',
    color: 'red',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
      </svg>
    ),
    title: 'ECG Live prin WebSocket',
    desc: 'Stream direct de la dispozitiv la browserul medicului. Forma de undă ECG în timp real, activată doar la cerere pentru eficiență maximă.',
    color: 'indigo',
  },
  {
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    ),
    title: 'Sistem Complet de Triaj',
    desc: 'Flux integrat: pacientul rezervă → recepționerul alocă medic și slot orar → medicul creează fișa și asociază dispozitivul.',
    color: 'emerald',
  },
];

const STATS = [
  { value: '30s',  label: 'interval de citire senzori' },
  { value: '4',    label: 'senzori monitorizați' },
  { value: '3',    label: 'roluri de utilizator' },
  { value: '250Hz', label: 'frecvența ECG live' },
];

const COLOR_MAP = {
  blue:    { bg: 'bg-blue-50',    icon: 'bg-blue-100 text-blue-600',    title: 'text-blue-900' },
  red:     { bg: 'bg-red-50',     icon: 'bg-red-100 text-red-600',      title: 'text-red-900'  },
  indigo:  { bg: 'bg-indigo-50',  icon: 'bg-indigo-100 text-indigo-600', title: 'text-indigo-900' },
  emerald: { bg: 'bg-emerald-50', icon: 'bg-emerald-100 text-emerald-600', title: 'text-emerald-900' },
};

// Animăm un ECG simplu în hero
function EcgLine() {
  const path = "M0,50 L20,50 L25,20 L30,80 L35,50 L55,50 L60,10 L65,90 L70,50 L90,50 L95,30 L100,70 L105,50 L130,50";
  return (
    <svg viewBox="0 0 130 100" className="w-full h-12 opacity-30" preserveAspectRatio="none">
      <path d={path} fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function LandingPage() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <div className="min-h-screen bg-white font-sans">

      {/* ── Navbar ───────────────────────────────────────────────────────── */}
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-200
        ${scrolled ? 'bg-white/95 backdrop-blur shadow-sm' : 'bg-transparent'}`}>
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg className="w-4.5 h-4.5 text-white w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
            <span className={`font-bold text-lg tracking-tight ${scrolled ? 'text-slate-900' : 'text-white'}`}>
              CardioFlow
            </span>
          </div>

          <div className="flex items-center gap-3">
            <Link to="/login"
              className={`text-sm font-medium px-4 py-2 rounded-lg transition-colors
                ${scrolled
                  ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  : 'text-white/80 hover:text-white hover:bg-white/10'}`}>
              Autentificare
            </Link>
            <Link to="/register"
              className="text-sm font-semibold px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors shadow-sm">
              Înregistrare
            </Link>
          </div>
        </div>
      </nav>

      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className="relative min-h-screen flex items-center bg-gradient-to-br from-blue-950 via-blue-900 to-slate-900 overflow-hidden">
        {/* Decorative background elements */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute -top-40 -right-40 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />
          <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />
          {/* Grid pattern */}
          <div className="absolute inset-0 opacity-5"
            style={{ backgroundImage: 'linear-gradient(#ffffff 1px, transparent 1px), linear-gradient(90deg, #ffffff 1px, transparent 1px)', backgroundSize: '60px 60px' }} />
        </div>

        <div className="relative max-w-6xl mx-auto px-5 pt-24 pb-20 w-full">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            {/* Text */}
            <div>
              <div className="inline-flex items-center gap-2 bg-blue-500/20 border border-blue-400/30 text-blue-300 text-xs font-semibold px-3 py-1.5 rounded-full mb-6">
                <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse" />
                Sistem de Monitorizare Medicală IoT
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white leading-tight tracking-tight">
                Monitorizare<br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400">
                  cardiacă
                </span>{' '}
                în timp real
              </h1>

              <p className="text-blue-200/80 text-lg mt-6 leading-relaxed max-w-lg">
                Platformă medicală completă pentru pacienți purtabili — ECG, puls, SpO₂, temperatură monitorizate continuu și transmise instant medicului dumneavoastră.
              </p>

              <div className="flex flex-col sm:flex-row gap-3 mt-8">
                <Link to="/register"
                  className="inline-flex items-center justify-center gap-2 bg-blue-500 hover:bg-blue-400 text-white font-semibold px-6 py-3 rounded-xl transition-colors text-sm shadow-lg shadow-blue-500/25">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  Creează Cont Gratuit
                </Link>
                <Link to="/login"
                  className="inline-flex items-center justify-center gap-2 bg-white/10 hover:bg-white/15 border border-white/20 text-white font-semibold px-6 py-3 rounded-xl transition-colors text-sm">
                  Intră în Cont
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </Link>
              </div>

              {/* Mini stats */}
              <div className="flex flex-wrap gap-6 mt-10 pt-8 border-t border-white/10">
                {STATS.map(s => (
                  <div key={s.label}>
                    <p className="text-2xl font-extrabold text-white">{s.value}</p>
                    <p className="text-blue-300/70 text-xs mt-0.5">{s.label}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Visual card */}
            <div className="hidden lg:block">
              <div className="bg-white/5 backdrop-blur border border-white/10 rounded-2xl p-6 space-y-4">
                {/* Live indicator */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
                    <span className="text-white text-sm font-medium">Monitor Live</span>
                  </div>
                  <span className="text-blue-300/60 text-xs">esp32_001</span>
                </div>

                {/* Metrics */}
                <div className="grid grid-cols-2 gap-3">
                  {[
                    { l: 'Puls',        v: '72', u: 'bpm',  c: 'text-red-400',     bg: 'bg-red-500/10' },
                    { l: 'SpO₂',        v: '98', u: '%',    c: 'text-violet-400',  bg: 'bg-violet-500/10' },
                    { l: 'Temperatură', v: '36.6', u: '°C', c: 'text-orange-400',  bg: 'bg-orange-500/10' },
                    { l: 'Umiditate',   v: '45', u: '%',    c: 'text-sky-400',     bg: 'bg-sky-500/10' },
                  ].map(m => (
                    <div key={m.l} className={`${m.bg} rounded-xl p-3`}>
                      <p className="text-white/50 text-xs">{m.l}</p>
                      <p className={`text-2xl font-bold ${m.c} mt-1`}>{m.v}</p>
                      <p className="text-white/30 text-xs">{m.u}</p>
                    </div>
                  ))}
                </div>

                {/* ECG strip */}
                <div className="bg-white/5 rounded-xl p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-white/60 text-xs font-medium">ECG</span>
                    <span className="flex items-center gap-1 text-indigo-400 text-xs font-semibold">
                      <span className="w-1.5 h-1.5 bg-indigo-400 rounded-full animate-pulse" />
                      LIVE
                    </span>
                  </div>
                  <div className="text-indigo-400">
                    <EcgLine />
                  </div>
                </div>

                {/* Alert */}
                <div className="flex items-center gap-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl px-3 py-2.5">
                  <span className="text-emerald-400 text-lg">✓</span>
                  <div>
                    <p className="text-emerald-400 text-xs font-semibold">Toți parametrii în limite normale</p>
                    <p className="text-white/30 text-xs">Ultima actualizare: acum 12s</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Scroll indicator */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 text-white/30 animate-bounce">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </section>

      {/* ── Features ─────────────────────────────────────────────────────── */}
      <section className="py-20 bg-slate-50">
        <div className="max-w-6xl mx-auto px-5">
          <div className="text-center mb-14">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Tot ce aveți nevoie<br />
              <span className="text-blue-600">într-o singură platformă</span>
            </h2>
            <p className="text-slate-500 mt-4 text-lg max-w-xl mx-auto">
              De la dispozitivul ESP32 purtabil până la browserul medicului — un flux continuu și securizat.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {FEATURES.map(f => {
              const c = COLOR_MAP[f.color];
              return (
                <div key={f.title} className={`${c.bg} rounded-2xl p-6 border border-transparent hover:shadow-md transition-shadow`}>
                  <div className={`w-11 h-11 ${c.icon} rounded-xl flex items-center justify-center mb-4`}>
                    {f.icon}
                  </div>
                  <h3 className={`font-bold text-base ${c.title} mb-2`}>{f.title}</h3>
                  <p className="text-slate-600 text-sm leading-relaxed">{f.desc}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* ── Roluri ───────────────────────────────────────────────────────── */}
      <section className="py-20 bg-white">
        <div className="max-w-5xl mx-auto px-5">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight">
              Un sistem pentru fiecare rol
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              {
                icon: '🏃',
                role: 'Pacient',
                color: 'border-blue-200 bg-blue-50/50',
                badge: 'bg-blue-100 text-blue-700',
                items: [
                  'Rezervare programare online',
                  'Grafice real-time ale senzorilor',
                  'Recomandări de la medic',
                  'Istoric alerte personale',
                ],
              },
              {
                icon: '📋',
                role: 'Recepționer',
                color: 'border-amber-200 bg-amber-50/50',
                badge: 'bg-amber-100 text-amber-700',
                items: [
                  'Inbox cu cereri de programare',
                  'Calendar disponibilitate medici',
                  'Alocare automată slot orar',
                  'Statistici flux pacienți',
                ],
              },
              {
                icon: '🩺',
                role: 'Medic',
                color: 'border-emerald-200 bg-emerald-50/50',
                badge: 'bg-emerald-100 text-emerald-700',
                items: [
                  'Crearea fișei medicale',
                  'Asociere dispozitiv ESP32',
                  'Praguri alarmă personalizate',
                  'ECG Live prin WebSocket',
                ],
              },
            ].map(r => (
              <div key={r.role} className={`rounded-2xl border-2 ${r.color} p-6`}>
                <div className="flex items-center gap-3 mb-5">
                  <span className="text-3xl">{r.icon}</span>
                  <span className={`text-sm font-bold px-3 py-1 rounded-full ${r.badge}`}>{r.role}</span>
                </div>
                <ul className="space-y-2.5">
                  {r.items.map(item => (
                    <li key={item} className="flex items-start gap-2.5 text-sm text-slate-700">
                      <svg className="w-4 h-4 text-emerald-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                      </svg>
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA final ────────────────────────────────────────────────────── */}
      <section className="py-20 bg-gradient-to-br from-blue-900 to-slate-900">
        <div className="max-w-2xl mx-auto px-5 text-center">
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Gata să începeți?
          </h2>
          <p className="text-blue-200/70 mt-4 text-lg">
            Creați un cont gratuit și conectați primul dispozitiv ESP32 în câteva minute.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center mt-8">
            <Link to="/register"
              className="inline-flex items-center justify-center gap-2 bg-blue-500 hover:bg-blue-400 text-white font-semibold px-8 py-3.5 rounded-xl transition-colors text-sm shadow-lg shadow-blue-500/25">
              Înregistrare Gratuită
            </Link>
            <Link to="/login"
              className="inline-flex items-center justify-center gap-2 bg-white/10 hover:bg-white/15 border border-white/20 text-white font-semibold px-8 py-3.5 rounded-xl transition-colors text-sm">
              Am deja cont
            </Link>
          </div>
        </div>
      </section>

      {/* ── Footer ───────────────────────────────────────────────────────── */}
      <footer className="bg-slate-900 py-8 border-t border-white/5">
        <div className="max-w-6xl mx-auto px-5 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 bg-blue-600 rounded-md flex items-center justify-center">
              <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
            <span className="text-white/60 text-sm font-medium">CardioFlow</span>
          </div>
          <p className="text-slate-500 text-xs">
            Sistem purtabil de supraveghere a sănătății · ESP32 + AD8232 + MAX30102 + DHT11
          </p>
        </div>
      </footer>
    </div>
  );
}
