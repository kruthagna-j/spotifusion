import React, { useState, useEffect, useRef } from 'react';
import { collection, getDocs, doc, setDoc, deleteDoc, addDoc } from 'firebase/firestore';
import { onAuthStateChanged } from 'firebase/auth';
import { db, auth } from './firebase';
import AuthModal from './components/AuthModal';

export default function App() {
  const [songs, setSongs] = useState([]);
  const [currentSong, setCurrentSong] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [view, setView] = useState('library'); // 'library', 'playlists'
  const [user, setUser] = useState(null);
  
  // Audio Player Progress States
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);

  // Custom Playlist States
  const [playlists, setPlaylists] = useState([]);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  
  const audioRef = useRef(null);

  // Monitor Auth State
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
    });
    return () => unsubscribe();
  }, []);

  // Fetch songs and playlists from Firestore
  useEffect(() => {
    const fetchData = async () => {
      try {
        const querySnapshot = await getDocs(collection(db, "songs"));
        const songList = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        setSongs(songList);
        if (songList.length > 0) setCurrentSong(songList[0]);

        if (user) {
          const playlistSnapshot = await getDocs(collection(db, `users/${user.uid}/playlists`));
          const userPlaylists = playlistSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
          setPlaylists(userPlaylists);
        }
      } catch (error) {
        console.error("Error fetching Firestore data: ", error);
      }
    };
    fetchData();
  }, [user]);

  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.play();
    }
    setIsPlaying(!isPlaying);
  };

  // Handle Timeline Updates
  const handleTimeUpdate = () => {
    setCurrentTime(audioRef.current.currentTime);
    setDuration(audioRef.current.duration || 0);
  };

  const handleSeek = (e) => {
    const progressBar = e.currentTarget;
    const rect = progressBar.getBoundingClientRect();
    const clickPosition = (e.clientX - rect.left) / rect.width;
    const newTime = clickPosition * duration;
    audioRef.current.currentTime = newTime;
    setCurrentTime(newTime);
  };

  const formatTime = (time) => {
    if (isNaN(time)) return "0:00";
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  };

  // Create Playlist Handler
  const handleCreatePlaylist = async (e) => {
    e.preventDefault();
    if (!user || !newPlaylistName.trim()) return;

    try {
      const docRef = await addDoc(collection(db, `users/${user.uid}/playlists`), {
        name: newPlaylistName,
        createdAt: new Date()
      });
      setPlaylists([...playlists, { id: docRef.id, name: newPlaylistName }]);
      setNewPlaylistName('');
    } catch (err) {
      console.error("Error creating playlist: ", err);
    }
  };

  return (
    <div className="flex flex-col h-screen bg-[#121212] text-white font-sans overflow-hidden select-none">
      
      {/* HEADER */}
      <header className="h-16 bg-black flex items-center justify-between px-6 border-b border-[#282828] shrink-0">
        <div className="text-lg font-bold tracking-wider cursor-pointer" onClick={() => setView('library')}>
          🎵 Harmonize
        </div>
        <AuthModal user={user} />
      </header>

      {/* MAIN CONTENT AREA */}
      <div className="flex flex-1 overflow-hidden">
        <aside className="hidden md:flex flex-col w-60 bg-black p-6 space-y-6">
          <nav className="space-y-4 text-sm text-[#b3b3b3] font-semibold">
            <button onClick={() => setView('library')} className={`block hover:text-white ${view === 'library' ? 'text-white' : ''}`}>My Library</button>
            <button onClick={() => setView('playlists')} className={`block hover:text-white ${view === 'playlists' ? 'text-white' : ''}`}>Playlists</button>
          </nav>
        </aside>

        <main className="flex-1 bg-gradient-to-b from-[#222222] to-[#121212] overflow-y-auto p-6 pb-28">
          {view === 'library' && (
            <div className="space-y-6">
              <h1 className="text-3xl font-bold">Discover Tracks</h1>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                {songs.map(song => (
                  <div
                    key={song.id}
                    onClick={() => { setCurrentSong(song); setIsPlaying(true); }}
                    className="bg-[#181818] p-4 rounded-md hover:bg-[#282828] transition cursor-pointer group"
                  >
                    <img src={song.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&h=300&fit=crop"} alt={song.title} className="w-full aspect-square object-cover rounded-md mb-3 shadow-md" />
                    <h3 className="font-bold truncate text-sm">{song.title}</h3>
                    <p className="text-xs text-[#b3b3b3] truncate mt-1">{song.artist}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {view === 'playlists' && (
            <div className="space-y-6">
              <h1 className="text-3xl font-bold">Your Playlists</h1>
              {user ? (
                <form onSubmit={handleCreatePlaylist} className="flex gap-2 max-w-md">
                  <input
                    type="text"
                    placeholder="New Playlist Name"
                    value={newPlaylistName}
                    onChange={(e) => setNewPlaylistName(e.target.value)}
                    className="bg-[#282828] text-white px-4 py-2 rounded-md flex-1 text-sm focus:outline-none"
                  />
                  <button type="submit" className="bg-white text-black font-bold px-4 py-2 rounded-md text-sm">Create</button>
                </form>
              ) : (
                <p className="text-sm text-[#b3b3b3]">Sign in to create and save custom playlists.</p>
              )}

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-4">
                {playlists.map(pl => (
                  <div key={pl.id} className="bg-[#181818] p-4 rounded-md border border-[#282828]">
                    <h3 className="font-bold">{pl.name}</h3>
                  </div>
                ))}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* PERSISTENT BOTTOM PLAYER BAR WITH TIMELINE SCRUBBING */}
      <div className="h-24 bg-black border-t border-[#282828] px-4 flex items-center justify-between shrink-0 z-50">
        
        {/* Track Info */}
        <div className="flex items-center space-x-3 w-1/3">
          <img src={currentSong?.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=100&h=100&fit=crop"} className="w-14 h-14 rounded object-cover" alt="Art" />
          <div className="truncate">
            <h4 className="text-sm font-semibold truncate">{currentSong?.title || "Select a track"}</h4>
            <p className="text-xs text-[#b3b3b3] truncate">{currentSong?.artist || "Artist"}</p>
          </div>
        </div>

        {/* Center Controls & Interactive Progress Bar */}
        <div className="flex flex-col items-center w-1/3 space-y-2">
          <audio
            ref={audioRef}
            src={currentSong?.audioUrl}
            onTimeUpdate={handleTimeUpdate}
            onEnded={() => setIsPlaying(false)}
          />
          <button onClick={togglePlay} className="w-10 h-10 bg-white text-black rounded-full flex items-center justify-center hover:scale-105 transition">
            {isPlaying ? <span className="font-bold">❚❚</span> : <span className="font-bold ml-0.5">▶</span>}
          </button>

          {/* Timeline Bar */}
          <div className="flex items-center space-x-2 w-full max-w-md text-xs text-[#b3b3b3]">
            <span>{formatTime(currentTime)}</span>
            <div
              onClick={handleSeek}
              className="flex-1 h-1.5 bg-[#4d4d4d] rounded-full cursor-pointer relative overflow-hidden"
            >
              <div
                className="absolute top-0 left-0 h-full bg-white rounded-full pointer-events-none" 
                style={{ width: `${(currentTime / (duration || 1)) * 100}%` }}
              ></div>
            </div>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        <div className="hidden md:flex justify-end w-1/3 text-[#b3b3b3] text-sm">
          <span>🔊 Ready</span>
        </div>
      </div>
    </div>
  );
}