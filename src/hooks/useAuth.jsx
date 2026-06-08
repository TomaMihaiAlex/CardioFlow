import React, { createContext, useContext, useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { ref, onValue } from 'firebase/database';
import { auth, db } from '../firebase';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,     setUser]     = useState(null);
  const [role,     setRole]     = useState(null);
  const [userData, setUserData] = useState(null);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    let unsubUserDoc = null;

    const unsubAuth = onAuthStateChanged(auth, (firebaseUser) => {
      // Curățăm orice listener anterior pe documentul utilizatorului
      if (unsubUserDoc) { unsubUserDoc(); unsubUserDoc = null; }

      if (!firebaseUser) {
        setUser(null);
        setRole(null);
        setUserData(null);
        setLoading(false);
        return;
      }

      setUser(firebaseUser);

      // Listener LIVE pe /users/{uid}. Un get() unic rata cursa cu scrierea
      // rolului din Register (createUser loghează instant, înainte ca set-ul
      // documentului să se termine) → rolul rămânea null pentru totdeauna.
      // onValue prinde scrierea reactiv, fără reload.
      const userRef = ref(db, `users/${firebaseUser.uid}`);
      unsubUserDoc = onValue(
        userRef,
        (snap) => {
          if (snap.exists()) {
            const data = snap.val();
            setRole(data.role ?? null);
            setUserData(data);
          } else {
            setRole(null);
            setUserData(null);
          }
          setLoading(false);
        },
        (err) => {
          // Niciodată spinner infinit: rezolvăm loading-ul și pe eroare.
          console.error('Eroare la citirea profilului utilizatorului:', err);
          setRole(null);
          setUserData(null);
          setLoading(false);
        }
      );
    });

    return () => {
      if (unsubUserDoc) unsubUserDoc();
      unsubAuth();
    };
  }, []);

  return (
    <AuthContext.Provider value={{ user, role, userData, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth trebuie folosit în interiorul <AuthProvider>');
  return ctx;
}
