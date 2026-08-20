import { createContext, useContext, useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
import {
  auth,
  signInWithGoogle,
  signOut as fbSignOut,
  ensureUserProfile,
} from "../lib/firebase";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true); // 1. Start with loading true

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        try {
          await ensureUserProfile(currentUser);
        } catch (err) {
          console.error("Error ensuring user profile: ", err);
        }
      }
      setLoading(false); // 2. Stop loading once Firebase responds
    });
    return () => unsubscribe();
  }, []);

  async function signIn() {
    await signInWithGoogle();
  }

  async function signOut() {
    await fbSignOut();
  }

  // 3. Prevent rendering children until loading is finished (stops the flicker!)
  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-bg text-text-muted text-sm">
        Loading Spotifusion…
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ user, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);