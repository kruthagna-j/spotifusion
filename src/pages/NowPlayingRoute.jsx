import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Music2 } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import NowPlaying from '@/components/NowPlaying'

export default function NowPlayingRoute() {
  const player = usePlayer()

  useEffect(() => {
    if (player.currentTrack) player.openNowPlaying()
    return () => player.closeNowPlaying()
  }, [player.currentTrack?.id])

  if (!player.currentTrack) {
    return <div className="min-h-full grid place-items-center p-8 text-center"><div><Music2 size={52} className="mx-auto text-text-subdued mb-4"/><h1 className="text-3xl font-black">Nothing is playing</h1><p className="text-text-muted mt-2">Choose a song to open the full player.</p><Link to="/search" className="sf-primary-button mt-6">Find music</Link></div></div>
  }

  return <NowPlaying />
}
