import { memo } from 'react'
import { Play, Pause, Heart, BadgeCheck, ListPlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { usePlayerRow } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongIds } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/timeFormat'
import AddToPlaylistMenu from '@/components/AddToPlaylistMenu'
import { getArtwork } from '@/lib/artwork'

function TrackRow({track,index,contextTracks}){
 const player=usePlayerRow(); const {user}=useAuth(); const likedIds=useLikedSongIds(user?.uid)
 const isCurrent=player.currentTrackId===track.id; const isLiked=likedIds.has(track.id)
 return <div onDoubleClick={()=>player.playTrack(track,contextTracks)} className={`group grid grid-cols-[24px_minmax(0,1fr)_auto] items-center gap-3 md:gap-4 px-2 md:px-4 py-2.5 rounded-xl hover:bg-surface-hover ${isCurrent?'text-brand':'text-text-muted'}`}>
  <div className="flex items-center justify-center w-6"><span className="group-hover:hidden text-sm" aria-hidden="true">{index+1}</span><button className="hidden group-hover:flex" onClick={()=>isCurrent?player.togglePlay():player.playTrack(track,contextTracks)} aria-label={isCurrent&&player.isPlaying?`Pause ${track.title}`:`Play ${track.title}`}>{isCurrent&&player.isPlaying?<Pause size={16} className="text-text"/>:<Play size={16} className="text-text"/>}</button></div>
  <div className="flex items-center gap-3 min-w-0"><img src={getArtwork(track, 'small')} alt="" loading="lazy" decoding="async" className="w-10 h-10 rounded object-cover shrink-0"/><div className="min-w-0"><button type="button" onClick={()=>{player.playTrack(track,contextTracks);player.openNowPlaying()}} className={`text-sm truncate text-left hover:underline ${isCurrent?'text-brand':'text-text'}`} title="Open Now Playing">{track.title}</button><div className="text-xs text-text-subdued truncate flex items-center gap-1">{track.artist?<Link to={`/artist/${encodeURIComponent(track.artist)}`} state={{tracks:contextTracks,name:track.artist}} onClick={e=>e.stopPropagation()} className="truncate hover:text-white hover:underline">{track.artist}</Link>:'Unknown artist'}{track.album&&<><span className="text-text-subdued">·</span><Link to={`/album/${encodeURIComponent(track.album)}`} state={{tracks:contextTracks,name:track.album}} onClick={e=>e.stopPropagation()} className="truncate hover:text-white hover:underline">{track.album}</Link></>}{track.trusted&&<BadgeCheck size={12} className="text-brand shrink-0" aria-label="Verified source"/>}</div></div></div>
  <div className="flex items-center gap-4"><button className="opacity-0 group-hover:opacity-100 transition-opacity" onClick={()=>user?(isLiked?unlikeSong(user.uid,track.id):likeSong(user.uid,track)):null} aria-label={isLiked?`Unlike ${track.title}`:`Like ${track.title}`} aria-pressed={isLiked}><Heart size={16} className={isLiked?'fill-brand text-brand opacity-100':''}/></button><button className="opacity-0 group-hover:opacity-100 transition-opacity" onClick={()=>player.playNextInQueue(track)} aria-label={`Play ${track.title} next`} title="Play next"><ListPlus size={16}/></button><span className="text-sm w-10 text-right">{track.duration?formatTime(parseDurationLocal(track.duration)):'--:--'}</span><AddToPlaylistMenu track={track}/></div>
 </div>
}
function parseDurationLocal(duration){if(typeof duration!=='string')return 0;if(duration.startsWith('PT')){const m=duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);if(!m)return 0;const[,h,min,s]=m;return(Number(h)||0)*3600+(Number(min)||0)*60+(Number(s)||0)}const parts=duration.split(':').map(Number);if(parts.some(Number.isNaN))return 0;return parts.reduce((total,part)=>total*60+part,0)}
export default memo(TrackRow)
