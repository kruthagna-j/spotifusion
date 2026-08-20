import React, { useState, useEffect, useRef } from 'react';
import { collection, getDocs, doc, setDoc, deleteDoc, addDoc } from 'firebase/firestore';
import { onAuthStateChanged } from 'firebase/auth';
import { db, auth } from './firebase';
import AuthModal from './components/AuthModal';

export default function App() {
  const [songs, setSongs] = useState([]);
  const [currentSong, setCurrentSong] = useState(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [view, setView] = useState('library'); // 'library', 'search', 'playlists'
  const [user, setUser] = useState(null);
  
  // Search state
  const [searchTerm, setSearchTerm] = useState('');

  // Audio Player Progress & Volume States
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1); // 1 = 100% volume

  // Custom Playlist States
  const [playlists, setPlaylists] = useState([]);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  
  // Liked songs map
  const [likedSongs, setLikedSongs] = useState({});

  const audioRef = useRef(null);

  // Monitor Auth State
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
    });
    return () => unsubscribe();
  }, []);

  // Fetch songs and user playlists/favorites from Firestore
  useEffect(() => {
    const fetchData = async () => {
      try {
        const querySnapshot = await getDocs(collection(db, "songs"));
        const songList = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        setSongs(songList);
        if (songList.length > 0 && !currentSong) {
          setCurrentSong(songList[0]);
          setCurrentIndex(0);
        }

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

  // Handle Auto-Advance to Next Track
  const handleNext = () => {
    if (songs.length === 0) return;
    const nextIndex = (currentIndex + 1) % songs.length;
    setCurrentIndex(nextIndex);
    setCurrentSong(songs[nextIndex]);
    setIsPlaying(true);
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

  const handleVolumeChange = (e) => {
    const newVol = parseFloat(e.target.value);
    setVolume(newVol);
    if (audioRef.current) {
      audioRef.current.volume = newVol;
    }
  };

  const formatTime = (time) => {
    if (isNaN(time)) return "0:00";
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  };

  // Like Song Handler
  const toggleLike = async (e, songId) => {
    e.stopPropagation();
    if (!user) {
      alert("Please sign in to like songs!");
      return;
    }

    const isLiked = likedSongs[songId];
    const likeRef = doc(db, `users/${user.uid}/favorites`, songId);

    try {
      if (isLiked) {
        await deleteDoc(likeRef);
        setLikedSongs(prev => ({ ...prev, [songId]: false }));
      } else {
        await setDoc(likeRef, { songId, timestamp: new Date() });
        setLikedSongs(prev => ({ ...prev, [songId]: true }));
      }
    } catch (err) {
      console.error("Error updating favorite: ", err);
    }
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

  // Filter songs for search view
  const filteredSongs = songs.filter(song => 
    song.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    song.artist?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex flex-col h-screen bg-[#121212] text-white font-sans overflow-hidden select-none">
      
      {/* HEADER */}
      <header className="h-16 bg-black flex items-center justify-between px-6 border-b border-[#282828] shrink-0">
        <div className="text-lg font-bold tracking-wider cursor-pointer flex items-center space-x-2" onClick={() => setView('library')}>
          <span>🎵</span>
          <span>Spotifusion</span>
        </div>
        <AuthModal user={user} />
      </header>

      {/* MAIN CONTENT AREA */}
      <div className="flex flex-1 overflow-hidden">
        
        {/* Desktop Sidebar Navigation */}
        <aside className="hidden md:flex flex-col w-60 bg-black p-6 space-y-6">
          <nav className="space-y-4 text-sm text-[#b3b3b3] font-semibold">
            <button onClick={() => setView('library')} className={`block hover:text-white transition ${view === 'library' ? 'text-white' : ''}`}>My Library</button>
            <button onClick={() => setView('search')} className={`block hover:text-white transition ${view === 'search' ? 'text-white' : ''}`}>Search</button>
            <button onClick={() => setView('playlists')} className={`block hover:text-white transition ${view === 'playlists' ? 'text-white' : ''}`}>Playlists</button>
          </nav>
        </aside>

        {/* Dynamic Views */}
        <main className="flex-1 bg-gradient-to-b from-[#222222] to-[#121212] overflow-y-auto p-6 pb-28">
          
          {/* LIBRARY VIEW */}
          {view === 'library' && (
            <div className="space-y-6">
              <h1 className="text-3xl font-bold">Discover Tracks</h1>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                {songs.map((song, index) => (
                  <div 
                    key={song.id} 
                    onClick={() => { 
                      setCurrentIndex(index);
                      setCurrentSong(song); 
                      setIsPlaying(true); 
                    }}
                    className="bg-[#181818] p-4 rounded-md hover:bg-[#282828] transition cursor-pointer group relative shadow-md"
                  >
                    <img src={song.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&h=300&fit=crop"} alt={song.title} className="w-full aspect-square object-cover rounded-md mb-3" />
                    <h3 className="font-bold truncate text-sm">{song.title}</h3>
                    <p className="text-xs text-[#b3b3b3] truncate mt-1">{song.artist}</p>
                    
                    <button 
                      onClick={(e) => toggleLike(e, song.id)}
                      className="absolute bottom-4 right-4 text-xl opacity-0 group-hover:opacity-100 transition"
                    >
                      {likedSongs[song.id] ? '❤️' : '🤍'}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SEARCH VIEW */}
          {view === 'search' && (
            <div className="space-y-6">
              <h1 className="text-3xl font-bold">Search</h1>
              <div className="relative max-w-md">
                <input 
                  type="text" 
                  placeholder="Artists, songs, or podcasts" 
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full bg-[#242424] text-white px-4 py-3 pl-10 rounded-full text-sm font-medium focus:outline-none focus:ring-2 focus:ring-white transition"
                />
                <span className="absolute left-3.5 top-3.5 text-[#b3b3b3]">🔍</span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 mt-6">
                {filteredSongs.length > 0 ? (
                  filteredSongs.map(song => {
                    const originalIndex = songs.findIndex(s => s.id === song.id);
                    return (
                      <div 
                        key={song.id} 
                        onClick={() => { 
                          setCurrentIndex(originalIndex !== -1 ? originalIndex : 0);
                          setCurrentSong(song); 
                          setIsPlaying(true); 
                        }}
                        className="bg-[#181818] p-4 rounded-md hover:bg-[#282828] transition cursor-pointer group shadow-md"
                      >
                        <img src={song.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&h=300&fit=crop"} alt={song.title} className="w-full aspect-square object-cover rounded-md mb-3" />
                        <h3 className="font-bold truncate text-sm">{song.title}</h3>
                        <p className="text-xs text-[#b3b3b3] truncate mt-1">{song.artist}</p>
                      </div>
                    );
                  })
                ) : (
                  <p className="text-sm text-[#b3b3b3]">No tracks found matching "{searchTerm}".</p>
                )}
              </div>
            </div>
          )}

          {/* PLAYLISTS VIEW */}
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
                  <button type="submit" className="bg-white text-black font-bold px-4 py-2 rounded-md text-sm hover:scale-105 transition">Create</button>
                </form>
              ) : (
                <p className="text-sm text-[#b3b3b3]">Sign in to create and save custom playlists.</p>
              )}

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-4">
                {playlists.map(pl => (
                  <div key={pl.id} className="bg-[#181818] p-4 rounded-md border border-[#282828] shadow-md">
                    <h3 className="font-bold text-base">{pl.name}</h3>
                    <p className="text-xs text-[#b3b3b3] mt-1">Custom Playlist</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* PERSISTENT BOTTOM PLAYER BAR */}
      <div className="h-24 bg-black border-t border-[#282828] px-4 flex items-center justify-between shrink-0 z-50">
        
        {/* Track Details */}
        <div className="flex items-center space-x-3 w-1/3">
          <img src={currentSong?.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=100&h=100&fit=crop"} className="w-14 h-14 rounded object-cover shadow" alt="Art" />
          <div className="truncate">
            <h4 className="text-sm font-semibold truncate">{currentSong?.title || "Select a track"}</h4>
            <p className="text-xs text-[#b3b3b3] truncate">{currentSong?.artist || "Artist"}</p>
          </div>
        </div>

        {/* Center Controls & Timeline Bar */}
        <div className="flex flex-col items-center w-1/3 space-y-2">
          <audio
            ref={audioRef}
            src={currentSong?.audioUrl}
            onTimeUpdate={handleTimeUpdate}
            onEnded={handleNext}
          />
          <div className="flex items-center space-x-4">
            <button onClick={togglePlay} className="w-10 h-10 bg-white text-black rounded-full flex items-center justify-center hover:scale-105 transition shadow">
              {isPlaying ? <span className="font-bold">❚❚</span> : <span className="font-bold ml-0.5">▶</span>}
            </button>
            <button onClick={handleNext} className="text-[#b3b3b3] hover:text-white text-lg transition" title="Next Track">
              ⏭
            </button>
          </div>

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

        {/* Right Utility Bar with Volume Slider */}
        <div className="hidden md:flex items-center justify-end space-x-3 w-1/3 text-[#b3b3b3] text-sm">
          <span>🔊</span>
          <input
            type="range"
            min="0"
            max="1"
            step="0.01"
            value={volume}
            onChange={handleVolumeChange}
            className="w-24 h-1 bg-[#4d4d4d] rounded-lg appearance-none cursor-pointer accent-white"
          />
        </div>
      </div>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="md:hidden flex justify-around items-center h-16 bg-black border-t border-[#282828] text-xs text-[#b3b3b3] shrink-0">
        <button onClick={() => setView('library')} className={`flex flex-col items-center ${view === 'library' ? 'text-white' : ''}`}><span>📚 Library</span></button>
        <button onClick={() => setView('search')} className={`flex flex-col items-center ${view === 'search' ? 'text-white' : ''}`}><span>🔍 Search</span></button>
        <button onClick={() => setView('playlists')} className={`flex flex-col items-center ${view === 'playlists' ? 'text-white' : ''}`}><span>🎵 Playlists</span></button>
      </nav>

    </div>
  );
}