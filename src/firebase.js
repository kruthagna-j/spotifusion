// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";

// Your web app's Firebase configuration using environment variables for security
const firebaseConfig = {
  apiKey: "AIzaSyBdUbewfkAubnIJvYZsI5Pyl5jlE_W03Hw",
  authDomain: "spotifusion-a82a9.firebaseapp.com",
  projectId: "spotifusion-a82a9",
  storageBucket: "spotifusion-a82a9.firebasestorage.app",
  messagingSenderId: "774945500379",
  appId: "1:774945500379:web:3cac58ab226244145630fe"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Export Firebase services to use across your app
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);