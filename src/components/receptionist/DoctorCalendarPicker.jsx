import React, { useState, useEffect, useMemo } from 'react';
import { ref, onValue, update } from 'firebase/database';
import { db } from '../../firebase';

// Generează sloturile orare standard 09:00–16:30 la fiecare 30 de minute
function generateSlots() {
  const slots = [];
  for (let h = 9; h <= 16; h++) {
    for (const m of [0, 30]) {
      if (h === 16 && m === 30) break;
      slots.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
    }
  }
  return slots;
}
const ALL_SLOTS = generateSlots();

// Formatează Date ca YYYY-MM-DD în timezone local
function toLocalDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const DAYS_RO = ['Dum', 'Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sâm'];
const MONTHS_RO = ['Ian', 'Feb', 'Mar', 'Apr', 'Mai', 'Iun', 'Iul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * Calendar săptămânal cu slot-uri orare pentru un medic dat.
 * Afișează săptămâna curentă + urmtoarea.
 * Slot-urile deja rezervate apar dezactivate.
 * La selectare, apelează onSelect({ date, time }).
 */
export default function DoctorCalendarPicker({ medicId, onSelect }) {
  const [weekOffset, setWeekOffset] = useState(0); // 0 = săptămâna curentă
  const [selectedDate, setSelectedDate] = useState(null);
  const [bookedSlots,  setBookedSlots]  = useState({}); // { 'YYYY-MM-DD_HH:MM': true }
  const [selectedSlot, setSelectedSlot] = useState(null);

  // Calculăm zilele săptămânii vizibile (Lun–Dum)
  const weekDays = useMemo(() => {
    const today = new Date();
    const day   = today.getDay(); // 0=dum
    const monday = new Date(today);
    monday.setDate(today.getDate() - ((day + 6) % 7) + weekOffset * 7);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return d;
    });
  }, [weekOffset]);

  // Listener pentru slot-urile rezervate ale medicului
  useEffect(() => {
    if (!medicId) return;
    const unsubList = weekDays.map(day => {
      const dateStr = toLocalDate(day);
      return onValue(ref(db, `doctorSchedule/${medicId}/slots/${dateStr}`), (snap) => {
        if (!snap.exists()) return;
        const updates = {};
        snap.forEach(child => {
          const slot = child.val();
          if (slot.status === 'booked') {
            updates[`${dateStr}_${slot.time}`] = true;
          }
        });
        setBookedSlots(prev => ({ ...prev, ...updates }));
      });
    });
    return () => unsubList.forEach(u => u());
  }, [medicId, weekOffset]);

  const handleSelectSlot = (time) => {
    setSelectedSlot(time);
    onSelect?.({ date: selectedDate, time });
  };

  const today = toLocalDate(new Date());

  return (
    <div className="space-y-4">
      {/* Navigare săptămână */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => { setWeekOffset(w => w - 1); setSelectedDate(null); setSelectedSlot(null); }}
          disabled={weekOffset <= 0}
          className="p-2 rounded-lg hover:bg-slate-100 disabled:opacity-30 transition-colors"
        >
          ←
        </button>
        <span className="text-sm font-semibold text-slate-700">
          {toLocalDate(weekDays[0])} – {toLocalDate(weekDays[6])}
        </span>
        <button
          onClick={() => { setWeekOffset(w => w + 1); setSelectedDate(null); setSelectedSlot(null); }}
          disabled={weekOffset >= 4}
          className="p-2 rounded-lg hover:bg-slate-100 disabled:opacity-30 transition-colors"
        >
          →
        </button>
      </div>

      {/* Grid săptămânal */}
      <div className="grid grid-cols-7 gap-1">
        {weekDays.map(day => {
          const dateStr  = toLocalDate(day);
          const isPast   = dateStr < today;
          const isToday  = dateStr === today;
          const isSel    = dateStr === selectedDate;
          const isWeekend = day.getDay() === 0 || day.getDay() === 6;

          return (
            <button
              key={dateStr}
              onClick={() => { if (!isPast) { setSelectedDate(dateStr); setSelectedSlot(null); }}}
              disabled={isPast}
              className={`flex flex-col items-center py-2 rounded-xl text-xs transition-colors
                ${isPast     ? 'opacity-30 cursor-default'
                : isSel      ? 'bg-blue-600 text-white'
                : isToday    ? 'bg-blue-50 text-blue-700 border border-blue-300'
                : isWeekend  ? 'text-slate-400 hover:bg-slate-100'
                :              'text-slate-700 hover:bg-slate-100'}`}
            >
              <span className="font-medium">{DAYS_RO[day.getDay()]}</span>
              <span className={`text-base font-bold mt-0.5 ${isSel ? 'text-white' : ''}`}>
                {day.getDate()}
              </span>
              <span className={`${isSel ? 'text-blue-200' : 'text-slate-400'}`}>
                {MONTHS_RO[day.getMonth()]}
              </span>
            </button>
          );
        })}
      </div>

      {/* Slot-uri orare pentru ziua selectată */}
      {selectedDate && (
        <div>
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            Slot-uri disponibile — {selectedDate}
          </p>
          <div className="grid grid-cols-4 gap-2">
            {ALL_SLOTS.map(time => {
              const key    = `${selectedDate}_${time}`;
              const booked = bookedSlots[key];
              const isSel  = selectedSlot === time;

              return (
                <button
                  key={time}
                  disabled={booked}
                  onClick={() => handleSelectSlot(time)}
                  className={`py-2 rounded-lg text-sm font-medium transition-colors border
                    ${booked
                      ? 'bg-slate-100 text-slate-300 border-slate-100 cursor-not-allowed line-through'
                      : isSel
                        ? 'bg-blue-600 text-white border-blue-600'
                        : 'bg-white text-slate-700 border-slate-200 hover:border-blue-400 hover:text-blue-700'}`}
                >
                  {time}
                  {booked && <span className="block text-xs mt-0.5">ocupat</span>}
                </button>
              );
            })}
          </div>

          {selectedSlot && (
            <div className="mt-3 flex items-center gap-2 bg-blue-50 border border-blue-200 rounded-xl px-4 py-2.5">
              <span className="text-blue-600 text-lg">📅</span>
              <div>
                <p className="text-sm font-semibold text-blue-800">
                  Slot selectat: {selectedDate} la {selectedSlot}
                </p>
                <p className="text-xs text-blue-500">Confirmați alocarea mai jos.</p>
              </div>
            </div>
          )}
        </div>
      )}

      {!selectedDate && (
        <p className="text-center text-slate-400 text-sm py-4">
          Selectați o zi din calendar pentru a vedea slot-urile disponibile.
        </p>
      )}
    </div>
  );
}
