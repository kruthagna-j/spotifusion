import React from 'react';
import { signInWithPopup, GoogleAuthProvider, signOut } from 'firebase/auth';
import { auth } from '../firebase';

export default function AuthModal({ user }) {
  const handleGoogleLogin = async () => {
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error("Authentication error: ", error);
    }
  };

  const handleLogout = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      console.error("Logout error: ", error);
    }
  };

  return (
    <div className="flex items-center space-x-4">
      {user ? (
        <div className="flex items-center space-x-3 bg-[#282828] px-3 py-1.5 rounded-full">
          <img
            src={user.photoURL || "https://via.placeholder.com/30"}
            alt="Profile"
            className="w-7 h-7 rounded-full object-cover"
          />
          <span className="text-xs font-semibold hidden sm:inline">{user.displayName}</span>
          <button
            onClick={handleLogout}
            className="text-xs text-[#b3b3b3] hover:text-white transition ml-2"
          >
            Logout
          </button>
        </div>
      ) : (
        <button
          onClick={handleGoogleLogin}
          className="bg-white text-black text-xs font-bold px-4 py-2 rounded-full hover:scale-105 transition"
        >
          Sign In with Google
        </button>
      )}
    </div>
  );
}