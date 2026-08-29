import { useState } from 'react'
import { Play, Pause, Heart, Cast, ListMusic } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { getArtwork } from '@/lib/artwork'
import QueuePanel from '@/components/QueuePanel'
import NowPlaying from '@/components/NowPlaying'

export default function PlayerBar(){
 const player=usePlayer(); const {user}=useAuth(); const liked=useLikedSongs(user?.uid); const [queueOpen,setQueueOpen]=useState(false); const {currentTrack}=player
 if(!currentTrack)return null
 const isLiked=liked.some(t=>t.id===currentTrack.id)
 const toggleLike=()=>{if(!user)return;isLiked?unlikeSong(user.uid,currentTrack.id):likeSong(user.uid,currentTrack)}
 const art=getArtwork(currentTrack,'medium')
 return <>
  <div className="mobile-mini-player md:hidden"><div className="mobile-mini-player-card neon-mini">
   <button onClick={()=>player.openNowPlaying()} className="flex items-center gap-3 min-w-0 flex-1 text-left"><img src={art} alt="" className="w-10 h-10 rounded-[10px] object-cover shrink-0"/><span className="min-w-0 flex-1"><span className="block text-xs font-extrabold truncate">{currentTrack.title}</span><span className="block text-[10px] text-[#77817c] truncate mt-1">{currentTrack.artist}</span></span></button>
   <button onClick={toggleLike} aria-label="Like" className="w-9 h-9 grid place-items-center shrink-0"><Heart size={18} className={isLiked?'fill-[#1ed760] text-[#1ed760]':'text-[#89938e]'}/></button>
   <button onClick={player.togglePlay} aria-label={player.isPlaying?'Pause':'Play'} className="neon-play !w-9 !h-9 shrink-0">{player.isPlaying?<Pause size={16}/>:<Play size={16} fill="currentColor"/>}</button>
  </div></div>
  <NowPlaying/>
  <div className="hidden md:flex items-center justify-between px-5 h-[86px] bg-surface border-t border-border"><div className="flex items-center gap-3 min-w-0"><img src={art} alt="" className="w-14 h-14 rounded object-cover"/><div className="min-w-0"><p className="text-sm truncate">{currentTrack.title}</p><p className="text-xs text-text-subdued truncate">{currentTrack.artist}</p></div><button onClick={toggleLike}><Heart size={18} className={isLiked?'fill-brand text-brand':'text-text-muted'}/></button></div><div className="flex items-center gap-4"><button className="control-button" onClick={player.playPrevious}>‹</button><button onClick={player.togglePlay} className="control-button control-button-play">{player.isPlaying?<Pause size={17}/>:<Play size={17}/>}</button><button className="control-button" onClick={player.playNext}>›</button></div><div className="flex items-center gap-4"><button onClick={()=>player.outputSupported&&(player.outputDeviceId?player.resetOutputDevice:player.chooseOutputDevice)}><Cast size={18} className={player.outputDeviceId?'text-brand':'text-text-muted'}/></button><button onClick={()=>setQueueOpen(v=>!v)}><ListMusic size={18}/></button>{queueOpen&&<QueuePanel onClose={()=>setQueueOpen(false)}/>}</div></div>
 </>
}
