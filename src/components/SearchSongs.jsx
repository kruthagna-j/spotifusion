import React, { useState } from 'react';

export default function SearchSongs({ songs, onSelectSong }) {
  const [searchTerm, setSearchTerm] = useState('');

  // Filter songs based on title or artist
  const filteredSongs = songs.filter(song => 
    song.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    song.artist.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Search</h1>
      
      {/* Search Input Box */}
      <div className="relative max-w-md">
        <input 
          type="text" 
          placeholder="Search for songs or artists..." 
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full bg-[#242424] text-white px-4 py-3 pl-10 rounded-full text-sm font-medium focus:outline-none focus:ring-2 focus:ring-white transition"
        />
        <span className="absolute left-3.5 top-3.5 text-[#b3b3b3]">🔍</span>
      </div>

      {/* Filtered Results Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 mt-6">
        {filteredSongs.length > 0 ? (
          filteredSongs.map(song => (
            <div 
              key={song.id} 
              onClick={() => onSelectSong(song)}
              className="bg-[#181818] p-4 rounded-md hover:bg-[#282828] transition cursor-pointer group"
            >
              <img 
                src={song.coverUrl || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&h=300&fit=crop"} 
                alt={song.title} 
                className="w-full aspect-square object-cover rounded-md mb-3 shadow-md" 
              />
              <h3 className="font-bold truncate text-sm">{song.title}</h3>
              <p className="text-xs text-[#b3b3b3] truncate mt-1">{song.artist}</p>
            </div>
          ))
        ) : (
          <p className="text-sm text-[#b3b3b3]">No tracks found matching "{searchTerm}".</p>
        )}
      </div>
    </div>
  );
}