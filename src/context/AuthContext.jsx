import { createContext, useContext, useEffect, useState } from "react";
import { getAuth, onAuthStateChanged } from "firebase/auth";
import { app } from "../lib/firebase"; // adjust path to your firebase config

const AuthContext = createContext();
const auth = getAuth(app);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true); // 1. Start with loading true

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false); // 2. Stop loading once Firebase responds
    });
    return () => unsubscribe();
  }, []);

  // 3. Prevent rendering children until loading is finished (stops the flicker!)
  if (loading) {
    return <div className="loading-screen">Loading Spotifusion...</div>; 
  }

  return (
    <AuthContext.Provider value={{ user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);